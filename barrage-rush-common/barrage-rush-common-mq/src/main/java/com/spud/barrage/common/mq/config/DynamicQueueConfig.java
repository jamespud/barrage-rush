package com.spud.barrage.common.mq.config;

import static com.spud.barrage.common.mq.util.MqUtils.checkRoomType;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.spud.barrage.common.data.config.RedisConfig;
import com.spud.barrage.common.mq.util.MqUtils;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.util.Pair;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * TODO: 根据ws连接数判断
 *
 * @author Spud
 * @date 2025/3/11
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DynamicQueueConfig {
  
  // 房间队列变化缓存
  private static final Cache<Long, Long> ROOM_EVENT_CACHE = Caffeine.newBuilder().build();
  // 房间分类Map缓存，避免频繁查询Redis
  private static final Cache<Long, RoomType> ROOM_TYPE_CACHE = Caffeine.newBuilder().build();
  // 房间流量计数器
  private static final Cache<Long, Integer> ROOM_VIEWER_CACHE = Caffeine.newBuilder().build();
  // 房间ex Map，记录当前房间的队列
  private static final Cache<Long, Set<Object>> ROOM_EXCHANGE_CACHE = Caffeine.newBuilder().build();
  // 房间队列Map，记录当前房间的队列
  private static final Cache<Long, Set<Object>> ROOM_QUEUE_CACHE = Caffeine.newBuilder().build();
  // 使用Redis存储和读取直播间信息
  private static RedisTemplate<String, Object> redisTemplate;
  
  private static DynamicMQProperties dynamicMQProperties;

  public static Pair<Object, Object> getExchangeAndQueue(Long roomId) {
    Integer views = ROOM_VIEWER_CACHE.get(roomId, k -> {
      Object object = redisTemplate.opsForValue()
          .get(String.format(RedisConfig.ROOM_VIEWER, roomId));
      if (Objects.isNull(object) || !(object instanceof String)) {
        return 0;
      }
      return Integer.parseInt(object.toString());
    });
    RoomType oldType = ROOM_TYPE_CACHE.get(roomId, k -> RoomType.COLD);
    RoomType type = MqUtils.checkRoomType(views);
    if (oldType != type && validateRoomEvent(roomId)) {
      // 清除现有exchange、queue绑定
      clearExchangeAndQueue(roomId);
      // 创建新的exchange、queue绑定
      createExchangeAndQueue(roomId, type);
    }
    // 从Redis获取房间ex和队列
    Set<Object> exchanges = ROOM_EXCHANGE_CACHE.get(roomId,
        k -> redisTemplate.opsForSet().members(String.format(RedisConfig.ROOM_EXCHANGE, roomId)));
    Set<Object> queues = ROOM_QUEUE_CACHE.get(roomId,
        k -> redisTemplate.opsForSet().members(String.format(RedisConfig.ROOM_QUEUE, roomId)));
    // TODO: 负载均衡
    return Pair.of(exchanges.stream().findFirst().get(), queues.stream().findFirst().get());
  }

  private static void clearExchangeAndQueue(Long roomId) {
    try {
      ROOM_EXCHANGE_CACHE.invalidate(roomId);
      ROOM_QUEUE_CACHE.invalidate(roomId);
      redisTemplate.opsForSet().remove(String.format(RedisConfig.ROOM_EXCHANGE, roomId));
      redisTemplate.opsForSet().remove(String.format(RedisConfig.ROOM_QUEUE, roomId));
    } catch (Exception e) {
      log.error("Failed to clear exchange and queue for room {}: {}", roomId, e.getMessage(), e);
    }
  }

  private static void createExchangeAndQueue(Long roomId, RoomType type) {
    // 更新
    long changeTime = System.currentTimeMillis();
    redisTemplate.opsForValue()
        .set(String.format(RedisConfig.ROOM_MQ_EVENT, roomId), changeTime);
    ROOM_EVENT_CACHE.put(roomId, changeTime);
    // TODO: 原子性操作
    // TODO: 创建ex和queue

  }

  /**
   * 获取房间类型，优先从本地缓存获取，没有则查询Redis
   */
  public RoomType getRoomType(Long roomId) {
    return ROOM_TYPE_CACHE.get(roomId, this::evaluateRoomTypeByViewers);
  }

  /**
   * 根据观看人数评估房间类型
   */
  private RoomType evaluateRoomTypeByViewers(Long roomId) {
    // 从Redis获取房间观看人数
    String key = String.format(RedisConfig.ROOM_VIEWER, roomId);
    Object viewersObj = redisTemplate.opsForValue().get(key);
    int viewers = 0;

    if (viewersObj != null) {
      try {
        viewers = Integer.parseInt(viewersObj.toString());
      } catch (NumberFormatException e) {
        log.error("Invalid viewers count in Redis for room {}: {}", roomId, viewersObj);
      }
    }

    // 根据观看人数判断房间类型
    return MqUtils.checkRoomType(viewers);
  }

  /**
   * 预热机制：定时更新本地缓存，确保系统能及时响应热门直播间
   */
  @Scheduled(fixedRate = 10000) // 每10秒更新一次
  public void refreshRoomTypeCache() {
    try {
      // 查询所有活跃房间
      Set<String> activeRooms = redisTemplate.keys(RedisConfig.ACTIVE_ROOM);
      if (activeRooms == null || activeRooms.isEmpty()) {
        return;
      }

      // 批量获取所有房间的观众数量
      List<Object> viewersList = redisTemplate.opsForValue().multiGet(activeRooms);
      if (viewersList == null || viewersList.isEmpty()) {
        return;
      }

      // 更新本地缓存
      int i = 0;
      for (String key : activeRooms) {
        try {
          long roomId = Long.parseLong(key.split(":")[1]);
          int viewers = Integer.parseInt(viewersList.get(i++).toString());

          RoomType type = MqUtils.checkRoomType(viewers);

          // 更新本地缓存
          RoomType oldType = ROOM_TYPE_CACHE.get(roomId, k -> RoomType.NORMAL);
          ROOM_TYPE_CACHE.put(roomId, type);

          // 热门直播间状态变化时预热资源
          if (type == RoomType.HOT && oldType != RoomType.HOT) {
            preWarmHotRoom(roomId);
          }
        } catch (Exception e) {
          log.error("Failed to process room viewer data: {}", e.getMessage(), e);
        }
      }
    } catch (Exception e) {
      log.error("Failed to refresh room type cache: {}", e.getMessage(), e);
    }
  }

  /**
   * 对热门直播间进行预热
   */
  private void preWarmHotRoom(long roomId) {
    log.info("Pre-warming resources for hot room: {}", roomId);
    // TODO: 这里可以添加预热逻辑，比如提前创建交换机和队列
  }
  
  private static boolean validateRoomEvent(Long roomId) {
    long lastEventTimestamp = getLatestRoomEvent(roomId);
    return System.currentTimeMillis() - lastEventTimestamp
        < dynamicMQProperties.getRoomEventChangeInterval();
  }
  
  private static long getLatestRoomEvent(Long roomId) {
    return ROOM_EVENT_CACHE.get(roomId, k -> {
      Object object = redisTemplate.opsForValue()
          .get(String.format(RedisConfig.ROOM_MQ_EVENT, roomId));
      if (Objects.isNull(object) || !(object instanceof String)) {
        return 0L;
      }
      return Long.parseLong(object.toString());
    });
  }

  /**
   * 房间类型枚举
   */
  public enum RoomType {
    // 冷门房间，共享队列
    COLD,
    // 普通房间，自己的队列
    NORMAL,
    // 热门房间，多分片队列
    HOT,
    // 超热门房间，多分片队列
    SUPER_HOT
  }
} 