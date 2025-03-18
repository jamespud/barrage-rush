package com.spud.barrage.common.mq.service;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.spud.barrage.common.data.config.RedisConfig;
import com.spud.barrage.common.mq.config.DynamicQueueConfig.RoomType;
import com.spud.barrage.common.mq.config.RoomTrafficProperties;
import com.spud.barrage.common.mq.util.MqUtils;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * @author Spud
 * @date 2025/3/13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoomTrafficManager {

  private final RedisTemplate<String, Object> redisTemplate;
  private final RoomTrafficProperties config;

  // 本地缓存房间类型，减少Redis访问
  private final LoadingCache<String, RoomType> roomTypeCache = Caffeine.newBuilder()
      .expireAfterWrite(config.getTypeCacheExpire(), TimeUnit.SECONDS)
      .build(this::evaluateRoomType);

  // 本地缓存房间观众数，减少Redis访问
  private final LoadingCache<String, Integer> roomViewerCache = Caffeine.newBuilder()
      .expireAfterWrite(config.getViewerCacheExpire(), TimeUnit.SECONDS)
      .build(this::getRoomViewerCount);

  // 本地缓存房间交换机，减少Redis访问
  private final LoadingCache<String, Set<Object>> roomExchangeCache = Caffeine.newBuilder()
      .expireAfterWrite(config.getExchangeCacheExpire(), TimeUnit.SECONDS)
      .build(this::getRoomExchange);

  // 本地缓存房间队列，减少Redis访问
  private final LoadingCache<String, Set<Object>> roomQueueCache = Caffeine.newBuilder()
      .expireAfterWrite(config.getQueueCacheExpire(), TimeUnit.SECONDS)
      .build(this::getRoomQueue);

  /**
   * 获取房间类型
   */
  public RoomType getRoomType(String roomId) {
    try {
      return roomTypeCache.get(roomId);
    } catch (Exception e) {
      log.error("Failed to get room type for {}: {}", roomId, e.getMessage());
      return RoomType.NORMAL;
    }
  }

  /**
   * 评估房间类型
   */
  private RoomType evaluateRoomType(String roomId) {
    // 获取当前观众数
    int viewers = getRoomViewerCount(roomId);
    // 根据观众数判断房间类型
    return MqUtils.checkRoomType(viewers);
  }

  /**
   * 获取房间交换机
   */
  public Set<Object> getRoomExchange(String roomId) {
    try {
      return redisTemplate.opsForSet()
          .members(String.format(RedisConfig.ROOM_EXCHANGE, roomId));

    } catch (Exception e) {
      log.error("Failed to get room exchange for {}: {}", roomId, e.getMessage());
      return null;
    }
  }

  /**
   * 获取房间队列
   */
  public Set<Object> getRoomQueue(String roomId) {
    try {
      return redisTemplate.opsForSet()
          .members(String.format(RedisConfig.ROOM_QUEUE, roomId));
    } catch (Exception e) {
      log.error("Failed to get room queue for {}: {}", roomId, e.getMessage());
      return null;
    }
  }

  /**
   * 获取房间观众数
   */
  private Integer getRoomViewerCount(String roomId) {
    String viewerKey = String.format(RedisConfig.ROOM_VIEWER, roomId);
    Object viewersObj = redisTemplate.opsForValue().get(viewerKey);

    int viewers = 0;
    if (viewersObj != null) {
      try {
        viewers = Integer.parseInt(viewersObj.toString());
      } catch (NumberFormatException e) {
        log.error("Invalid viewers count for room {}: {}", roomId, viewersObj);
      }
    }
    return viewers;
  }

  /**
   * 更新房间类型
   */
  public void updateRoomType(String roomId) {
    RoomType newType = evaluateRoomType(roomId);
    RoomType oldType = getRoomType(roomId);

    if (newType != oldType) {
      // 更新本地缓存
      roomTypeCache.put(roomId, newType);

      // 更新Redis中的房间类型
      String typeKey = String.format("room:%s:type", roomId);
      redisTemplate.opsForValue().set(typeKey, newType.name());
      redisTemplate.expire(typeKey, config.getTypeCacheExpire(), TimeUnit.SECONDS);

      // 触发房间类型变更事件
      publishRoomTypeChangeEvent(roomId, oldType, newType);
    }
  }

  /**
   * 发布房间类型变更事件
   */
  private void publishRoomTypeChangeEvent(String roomId, RoomType oldType, RoomType newType) {
    Map<String, Object> event = Map.of(
        "roomId", roomId,
        "oldType", oldType.name(),
        "newType", newType.name(),
        "timestamp", System.currentTimeMillis()
    );

    redisTemplate.convertAndSend("room:type:change", event);
  }

  /**
   * 定期刷新房间类型
   */
  @Scheduled(fixedRateString = "${room.traffic.refresh-interval:5}000")
  public void refreshRoomTypes() {
    try {
      // 获取所有房间
      Set<String> roomKeys = redisTemplate.keys("room:*:viewers");
      if (roomKeys == null) {
        return;
      }

      for (String key : roomKeys) {
        String roomId = key.split(":")[1];
        updateRoomType(roomId);
      }
    } catch (Exception e) {
      log.error("Failed to refresh room types: {}", e.getMessage());
    }
  }
}