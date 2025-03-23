package com.spud.barrage.common.mq.config;

import com.spud.barrage.common.data.config.RedisConfig;
import com.spud.barrage.common.mq.config.properties.DynamicMQProperties;
import com.spud.barrage.common.mq.config.properties.RoomTrafficProperties;
import com.spud.barrage.common.mq.util.MqUtils;
import com.spud.barrage.constant.RoomType;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @author Spud
 * @date 2025/3/22
 */
@Slf4j
@Component
public class RoomManager {

  @Autowired
  private DynamicMQProperties dynamicMQProperties;

  @Autowired
  private RedisTemplate<String, Object> redisTemplate;

  @Autowired
  private CacheManager cacheManager;

  private final ThreadPoolExecutor pool;
  
  static final long LOCK_TIMEOUT = 6000;

  // 分布式锁脚本 - 获取锁
  private static final DefaultRedisScript<Long> LOCK_SCRIPT = new DefaultRedisScript<>(
      "if redis.call('setnx', KEYS[1], ARGV[1]) == 1 then " +
          "  redis.call('pexpire', KEYS[1], ARGV[2]) " +
          "  return 1 " +
          "else " +
          "  return 0 " +
          "end",
      Long.class);

  // 分布式锁脚本 - 释放锁
  private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
      "if redis.call('get', KEYS[1]) == ARGV[1] then " +
          "  redis.call('del', KEYS[1]) " +
          "  return 1 " +
          "else " +
          "  return 0 " +
          "end",
      Long.class);

  public RoomManager(RoomTrafficProperties properties) {
    pool = new ThreadPoolExecutor(properties.getPool().getCoreSize(),
        properties.getPool().getMaxSize(),
        properties.getPool().getKeepAlive(),
        TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(1000),
        new ThreadPoolExecutor.CallerRunsPolicy());
  }

  @Scheduled(fixedRate = 60000)
  public void schedule() {
    log.info("Starting room status refresh task");

    try {
      // 获取所有活跃房间
      Set<String> activeRooms = redisTemplate.keys(RedisConfig.ACTIVE_ROOM);
      if (activeRooms == null || activeRooms.isEmpty()) {
        log.info("No active rooms found");
        return;
      }

      log.info("Found {} active rooms", activeRooms.size());

      // 批量获取所有房间的观众数量
      for (String roomKey : activeRooms) {
        try {
          // 提取房间ID
          Long roomId = extractRoomId(roomKey);
          if (roomId == null) {
            continue;
          }

          // 提交任务到线程池处理房间状态
          CompletableFuture.runAsync(() -> processRoomStatus(roomId), pool)
              .exceptionally(e -> {
                log.error("Error processing room status for room {}: {}", roomId, e.getMessage(),
                    e);
                return null;
              });
        } catch (Exception e) {
          log.error("Failed to process room key {}: {}", roomKey, e.getMessage(), e);
        }
      }
    } catch (Exception e) {
      log.error("Failed to execute room status refresh task: {}", e.getMessage(), e);
    }
  }

  /**
   * 处理房间状态
   * 更新Redis中的房间ex和队列
   * 更新Caffeine中的房间ex和队列
   * @param roomId 房间ID
   */
  public void processRoomStatus(Long roomId) {
    try {

      boolean eventFlag = validateRoomEvent(roomId);
      if (!eventFlag) {
        // 不能更改房间状态就从redis获取房间类型
        cacheManager.updateLocalCache(roomId);
        return;
      }

      // 尝试获取分布式锁
      String lockKey = String.format(RedisConfig.PER_ROOM_STATE, roomId);
      String lockValue = String.valueOf(Thread.currentThread().threadId());
      boolean locked = acquireLock(lockKey, lockValue, LOCK_TIMEOUT);
      if (!locked) {
        log.info("Failed to acquire lock for room {}, another process is updating it", roomId);
        return;
      }
      // 获取旧的房间类型
      RoomType oldType = cacheManager.getRoomType(roomId);
      Integer viewerCount = cacheManager.getViewerCount(roomId);
      // 根据观众数判断房间类型
      RoomType newType = MqUtils.determineRoomType(viewerCount);

      // 拿到分布式锁后，无论是否发生类型变化，都要进行房间状态更新
      try {
        log.info("Room {} type changing from {} to {}", roomId, oldType, newType);
        // 更新本地缓存
        CacheManager.ROOM_TYPE_CACHE.put(roomId, newType);
        // 根据房间类型变化规则进行处理
        handleRoomTypeChange(roomId, oldType, newType);
        // 更新更新时间
        redisTemplate.opsForValue()
            .set(String.format(RedisConfig.ROOM_TYPE_CHANGE, roomId), System.currentTimeMillis());
        // 发布房间状态变化事件
        publishRoomStatusChangeEvent(roomId);

        log.info("Successfully updated room {} type from {} to {}", roomId, oldType, newType);
      } finally {
        // 释放锁
        releaseLock(lockKey, lockValue);
      }
    } catch (Exception e) {
      log.error("Error processing room status for room {}: {}", roomId, e.getMessage(), e);
    }
  }

  /**
   * 发布房间状态变化事件
   */
  private void publishRoomStatusChangeEvent(Long roomId) {
    // 发布Redis事件
    redisTemplate.convertAndSend(RabbitMQConfig.ROOM_MQ_CHANGE_TOPIC, roomId.toString());
  }

  /**
   * 处理房间类型变化
   */
  private void handleRoomTypeChange(Long roomId, RoomType oldType, RoomType newType) {
    // TODO: 暂时使用: 清除现有绑定，根据新状态创建新的绑定
    cacheManager.clearExchangeAndQueue(roomId, oldType);
    cacheManager.createExchangeAndQueue(roomId, newType);
  }
  


  /**
   * 从房间键中提取房间ID
   */
  private Long extractRoomId(String roomKey) {
    try {
      return Long.parseLong(roomKey.split(":")[1]);
    } catch (Exception e) {
      log.error("Failed to extract room ID from key {}: {}", roomKey, e.getMessage(), e);
      return null;
    }
  }

  /**
   * 获取分布式锁
   */
  private boolean acquireLock(String lockKey, String lockValue, long timeout) {
    try {
      Long result = redisTemplate.execute(
          LOCK_SCRIPT,
          java.util.Collections.singletonList(lockKey),
          lockValue,
          String.valueOf(timeout));
      return Objects.equals(result, 1L);
    } catch (Exception e) {
      log.error("Failed to acquire lock {}: {}", lockKey, e.getMessage(), e);
      return false;
    }
  }

  /**
   * 释放分布式锁
   */
  private boolean releaseLock(String lockKey, String lockValue) {
    try {
      Long result = redisTemplate.execute(
          UNLOCK_SCRIPT,
          java.util.Collections.singletonList(lockKey),
          lockValue);
      return Objects.equals(result, 1L);
    } catch (Exception e) {
      log.error("Failed to release lock {}: {}", lockKey, e.getMessage(), e);
      return false;
    }
  }
  
  /**
   * 验证房间事件更新间隔
   * @return 是否可以更新
   */
  public boolean validateRoomEvent(Long roomId) {
    long lastEventTimestamp = getLatestRoomEvent(roomId);
    return System.currentTimeMillis() - lastEventTimestamp
        >= dynamicMQProperties.getRoomEventChangeInterval();
  }

  /**
   * 获取房间最新事件时间
   * @return 事件时间戳
   */
  protected long getLatestRoomEvent(Long roomId) {
    // 本地不缓存更新时间
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
}
