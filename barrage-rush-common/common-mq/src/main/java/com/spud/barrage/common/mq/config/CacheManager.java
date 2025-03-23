package com.spud.barrage.common.mq.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.spud.barrage.common.data.config.RedisConfig;
import com.spud.barrage.common.mq.util.MqUtils;
import com.spud.barrage.constant.RoomType;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * @author Spud
 * @date 2025/3/22
 */
@Slf4j
@Component
public class CacheManager {

  // 房间分类Map缓存，避免频繁查询Redis
  static final Cache<Long, RoomType> ROOM_TYPE_CACHE = Caffeine.newBuilder()
      .expireAfterAccess(3, TimeUnit.MINUTES).build();
  // 房间ex Map，记录当前房间的ex
  static final Cache<Long, Set<String>> ROOM_EXCHANGE_CACHE = Caffeine.newBuilder()
      .expireAfterAccess(3, TimeUnit.MINUTES).build();
  // 房间队列Map，记录当前房间的队列
  static final Cache<Long, Set<String>> ROOM_QUEUE_CACHE = Caffeine.newBuilder()
      .expireAfterAccess(3, TimeUnit.MINUTES).build();
  // 房间流量计数器
  static final Cache<Long, Integer> ROOM_VIEWER_CACHE = Caffeine.newBuilder()
      .expireAfterAccess(3, TimeUnit.MINUTES).build();

  @Autowired
  private ResourceManager resourceManager;

  @Autowired
  protected RedisTemplate<String, String> redisTemplate;

  private void clearLocalRoomCache(Long roomId) {
    ROOM_TYPE_CACHE.invalidate(roomId);
    ROOM_EXCHANGE_CACHE.invalidate(roomId);
    ROOM_QUEUE_CACHE.invalidate(roomId);
    ROOM_VIEWER_CACHE.invalidate(roomId);
  }

  public void updateLocalCache(Long roomId) {
    clearLocalRoomCache(roomId);
    getRoomType(roomId);
    getRoomExchange(roomId);
    getRoomQueue(roomId);
    getRoomViewer(roomId);
  }

  /**
   * 清除房间的交换机和队列绑定
   * 必须持房间分布式锁
   */
  public boolean clearExchangeAndQueue(Long roomId, RoomType oldType) {
    try {
      if (clearQueue(roomId, oldType) && clearExchange(roomId, oldType)) {
        log.info("Cleared exchange and queue bindings for room {}", roomId);
        return true;
      }
    } catch (Exception e) {
      log.error("Failed to clear exchange and queue for room {}: {}", roomId, e.getMessage(), e);
    }
    return false;
  }

  /**
   * 只清除房间的队列绑定
   * 必须持房间分布式锁
   */
  public boolean clearQueue(Long roomId,  RoomType oldType) {
    boolean success = true;
    try {
      Set<String> roomQueue = getRoomQueue(roomId);
      // 清除本地缓存
      ROOM_QUEUE_CACHE.invalidate(roomId);
      // 清除Redis中的绑定
      for (String queue : roomQueue) {
        success &= resourceManager.releaseQueueId(oldType, queue);
      }
      log.info("Cleared queue bindings for room {}", roomId);
    } catch (Exception e) {
      log.error("Failed to clear queue for room {}: {}", roomId, e.getMessage(), e);
      success = false;
    }
    return success;
  }

  /**
   * 只清除房间的交换机绑定
   * 必须持房间分布式锁
   */
  public boolean clearExchange(Long roomId, RoomType oldType) {
    boolean success = true;
    try {
      Set<String> roomExchange = getRoomExchange(roomId);
      ROOM_EXCHANGE_CACHE.invalidate(roomId);
      // 清除Redis中的绑定
      for (String exchange : roomExchange) {
        success &= resourceManager.releaseExchangeId(oldType, exchange);
      }
      log.info("Cleared exchange bindings for room {}", roomId);
    } catch (Exception e) {
      log.error("Failed to clear exchange for room {}: {}", roomId, e.getMessage(), e);
      success = false;
    }
    return success;
  }

  /**
   * 创建房间的交换机和队列绑定
   */
  public void createExchangeAndQueue(Long roomId, RoomType type) {
    try {
      // 更新房间mq事件时间
      long changeTime = System.currentTimeMillis();

      // 根据房间类型创建不同的交换机和队列
      String exchangeName = resourceManager.getExchangeId(type);
      String queueName = resourceManager.getQueueId(exchangeName, type);

      // 将交换机和队列绑定保存到Redis
      redisTemplate.opsForSet().add(String.format(RedisConfig.ROOM_EXCHANGE, roomId), exchangeName);
      redisTemplate.opsForSet().add(String.format(RedisConfig.ROOM_QUEUE, roomId), queueName);

      redisTemplate.opsForValue()
          .set(String.format(RedisConfig.ROOM_MQ_EVENT, roomId), String.valueOf(changeTime));

      // 更新本地缓存
      ROOM_EXCHANGE_CACHE.put(roomId, Collections.singleton(exchangeName));
      ROOM_QUEUE_CACHE.put(roomId, Collections.singleton(queueName));

      log.info("Created exchange {} and queue {} for room {}", exchangeName, queueName, roomId);
    } catch (Exception e) {
      log.error("Failed to create exchange and queue for room {}: {}", roomId, e.getMessage(), e);
    }
  }

  public Set<String> getActiveRooms() {
    return ROOM_VIEWER_CACHE.asMap().keySet().stream()
        .map((id) -> String.format(RedisConfig.ROOM_VIEWER, id)).collect(Collectors.toSet());
  }

  public long getLatestRoomEvent(Long roomId) {
    Object object = redisTemplate.opsForValue()
        .get(String.format(RedisConfig.ROOM_MQ_EVENT, roomId));
    if (Objects.isNull(object)) {
      return -1L;
    }
    try {
      return Long.parseLong(object.toString());
    } catch (NumberFormatException e) {
      log.error("Invalid event timestamp for room {}: {}", roomId, object);
      return -1L;
    }
  }

  /**
   * 获取房间类型，优先从本地缓存获取，没有则查询Redis
   */
  public RoomType getRoomType(Long roomId) {
    return CacheManager.ROOM_TYPE_CACHE.get(roomId, k -> {
      Integer viewers = getViewerCount(roomId);
      return MqUtils.determineRoomType(viewers);
    });
  }

  /**
   * 获取房间观众数量
   */
  public Integer getViewerCount(Long roomId) {
    return CacheManager.ROOM_VIEWER_CACHE.get(roomId, k -> {
      Object object = redisTemplate.opsForValue()
          .get(String.format(RedisConfig.ROOM_VIEWER, roomId));
      if (Objects.isNull(object)) {
        return 0;
      }
      try {
        return Integer.parseInt(object.toString());
      } catch (NumberFormatException e) {
        log.error("Invalid viewer count for room {}: {}", roomId, object);
        return 0;
      }
    });
  }

  public Set<String> getRoomExchange(Long roomId) {
    return ROOM_EXCHANGE_CACHE.get(roomId,
        k -> redisTemplate.opsForSet().members(String.format(RedisConfig.ROOM_EXCHANGE, roomId)));
  }

  public Set<String> getRoomQueue(Long roomId) {
    return ROOM_QUEUE_CACHE.get(roomId,
        k -> redisTemplate.opsForSet().members(String.format(RedisConfig.ROOM_QUEUE, roomId)));
  }

  public Integer getRoomViewer(Long roomId) {
    return ROOM_VIEWER_CACHE.get(roomId, k -> {
      Object object = redisTemplate.opsForValue()
          .get(String.format(RedisConfig.ROOM_VIEWER, roomId));
      if (Objects.isNull(object)) {
        return 0;
      }
      try {
        return Integer.parseInt(object.toString());
      } catch (NumberFormatException e) {
        log.error("Invalid viewer count for room {}: {}", roomId, object);
      }
      return 0;
    });
  }
}
