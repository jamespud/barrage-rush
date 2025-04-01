package com.spud.barrage.common.mq.config;

import com.spud.barrage.common.core.constant.RoomType;
import com.spud.barrage.common.data.config.RedisConfig;
import com.spud.barrage.common.mq.config.properties.DynamicMQProperties;
import com.spud.barrage.common.mq.config.properties.RoomTrafficProperties;
import com.spud.barrage.common.mq.util.MqUtils;
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
 * 房间管理器
 * 负责管理房间的MQ配置和状态
 * 定期检查房间热度并更新房间类型和MQ绑定
 * 
 * @author Spud
 * @date 2025/3/22
 */
@Slf4j
@Component
public class RoomManager {

  static final long LOCK_TIMEOUT = 10000; // 增加锁超时时间为10秒

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

  private final ThreadPoolExecutor pool;

  @Autowired
  private DynamicMQProperties dynamicMQProperties;

  @Autowired
  private RedisTemplate<String, Object> redisTemplate;

  @Autowired
  private CacheManager cacheManager;

  public RoomManager(RoomTrafficProperties properties) {
    // 初始化处理线程池
    pool = new ThreadPoolExecutor(
        properties.getPool().getCoreSize(),
        properties.getPool().getMaxSize(),
        properties.getPool().getKeepAlive(),
        TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(1000),
        new ThreadPoolExecutor.CallerRunsPolicy());
    log.info("RoomManager initialized with thread pool: core={}, max={}, keepAlive={}s",
        properties.getPool().getCoreSize(),
        properties.getPool().getMaxSize(),
        properties.getPool().getKeepAlive());
  }

  /**
   * 定时任务: 每60秒检查活跃房间状态
   * 异步处理每个房间的状态更新
   */
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

      log.info("Found {} active rooms to process", activeRooms.size());

      // 批量处理所有活跃房间
      for (String roomKey : activeRooms) {
        try {
          // 提取房间ID
          Long roomId = extractRoomId(roomKey);
          if (roomId == null) {
            continue;
          }

          // 提交任务到线程池异步处理房间状态
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
   * 基于房间热度更新MQ资源分配
   * 
   * @param roomId 房间ID
   */
  public void processRoomStatus(Long roomId) {
    try {
      // 验证是否可以更新房间状态
      boolean canUpdateRoom = validateRoomEvent(roomId);
      if (!canUpdateRoom) {
        // 不能更改房间状态，只更新本地缓存
        log.debug("Skipping room status update for roomId={}, updated too recently", roomId);
        cacheManager.updateLocalCache(roomId);
        return;
      }

      // 尝试获取分布式锁，防止多个节点同时更新
      String lockKey = String.format(RedisConfig.PER_ROOM_STATE, roomId);
      String lockValue = String.valueOf(Thread.currentThread().threadId());
      boolean locked = acquireLock(lockKey, lockValue, LOCK_TIMEOUT);

      if (!locked) {
        log.info("Failed to acquire lock for room {}, another process is updating it", roomId);
        return;
      }

      try {
        // 获取当前房间类型和观众数
        RoomType oldType = cacheManager.getRoomType(roomId);
        Integer viewerCount = cacheManager.getViewerCount(roomId);

        // 根据观众数确定新的房间类型
        RoomType newType = MqUtils.determineRoomType(viewerCount);

        // 无论是否发生类型变化，都要进行房间状态更新
        log.info("Room {} status: viewers={}, type changing from {} to {}",
            roomId, viewerCount, oldType, newType);

        // 更新本地缓存
        CacheManager.ROOM_TYPE_CACHE.put(roomId, newType);

        // 根据房间类型变化规则更新MQ资源
        boolean updated = handleRoomTypeChange(roomId, oldType, newType);

        if (updated) {
          // 更新状态变更时间
          redisTemplate.opsForValue()
              .set(String.format(RedisConfig.ROOM_TYPE_CHANGE, roomId), System.currentTimeMillis());

          // 发布房间状态变化事件，通知其他节点
          publishRoomStatusChangeEvent(roomId);

          log.info("Successfully updated room {} type from {} to {}", roomId, oldType, newType);
        } else {
          log.warn("Failed to update MQ resources for room {}", roomId);
        }
      } finally {
        // 无论处理结果如何，都要释放锁
        releaseLock(lockKey, lockValue);
      }
    } catch (Exception e) {
      log.error("Error processing room status for room {}: {}", roomId, e.getMessage(), e);
    }
  }

  /**
   * 发布房间状态变化事件，通知系统中其他组件
   * 
   * @param roomId 房间ID
   */
  private void publishRoomStatusChangeEvent(Long roomId) {
    try {
      redisTemplate.convertAndSend(RabbitMQConfig.ROOM_MQ_CHANGE_TOPIC, roomId.toString());
      log.debug("Published room MQ change event for roomId={}", roomId);
    } catch (Exception e) {
      log.error("Failed to publish room status change event: {}", e.getMessage(), e);
    }
  }

  /**
   * 处理房间类型变化
   * 根据新旧类型，更新交换机和队列的绑定关系
   * 
   * @param roomId  房间ID
   * @param oldType 旧房间类型
   * @param newType 新房间类型
   * @return 是否成功更新
   */
  private boolean handleRoomTypeChange(Long roomId, RoomType oldType, RoomType newType) {
    try {
      // 如果房间类型没有变化，只在首次分配时更新资源
      if (oldType == newType) {
        // 检查是否已有资源分配
        Set<String> exchanges = cacheManager.getRoomExchange(roomId);
        Set<String> queues = cacheManager.getRoomQueue(roomId);

        // 如果已有资源分配，无需再更新
        if (!exchanges.isEmpty() && !queues.isEmpty()) {
          log.debug("Room {} type unchanged and resources already allocated", roomId);
          return true;
        }
      }

      // 清除旧的资源绑定
      log.debug("Clearing old resources for room {}", roomId);
      cacheManager.clearExchangeAndQueue(roomId, oldType);

      // 根据新类型创建新的资源绑定
      log.debug("Creating new resources for room {} with type {}", roomId, newType);
      cacheManager.createExchangeAndQueue(roomId, newType);

      // 针对热门和超热门房间，根据需要创建额外分片
      if (newType == RoomType.HOT || newType == RoomType.SUPER_HOT) {
        int shardCount = getShardCountForType(newType);
        createAdditionalShards(roomId, newType, shardCount);
      }

      return true;
    } catch (Exception e) {
      log.error("Error handling room type change for room {}: {}", roomId, e.getMessage(), e);
      return false;
    }
  }

  /**
   * 为热门房间创建额外的分片队列
   * 
   * @param roomId     房间ID
   * @param type       房间类型
   * @param shardCount 分片数量
   */
  private void createAdditionalShards(Long roomId, RoomType type, int shardCount) {
    try {
      // 从第1个分片开始创建（0号分片由createExchangeAndQueue创建）
      for (int i = 1; i < shardCount; i++) {
        String queueName = MqUtils.generateQueueName(roomId, type, i);
        redisTemplate.opsForSet().add(String.format(RedisConfig.ROOM_QUEUE, roomId), queueName);
        log.debug("Created additional shard queue {} for room {}", queueName, roomId);
      }
    } catch (Exception e) {
      log.error("Failed to create additional shards for room {}: {}", roomId, e.getMessage(), e);
    }
  }

  /**
   * 根据房间类型确定分片数量
   * 
   * @param type 房间类型
   * @return 分片数量
   */
  private int getShardCountForType(RoomType type) {
    return switch (type) {
      case SUPER_HOT -> 5; // 超热门房间使用5个分片
      case HOT -> 3; // 热门房间使用3个分片
      default -> 1; // 其他类型使用1个分片
    };
  }

  /**
   * 从房间键中提取房间ID
   * 
   * @param roomKey 房间键
   * @return 房间ID
   */
  private Long extractRoomId(String roomKey) {
    return MqUtils.extractRoomId(roomKey);
  }

  /**
   * 获取分布式锁
   * 
   * @param lockKey   锁的键
   * @param lockValue 锁的值（用于标识锁的持有者）
   * @param timeout   锁的超时时间（毫秒）
   * @return 是否成功获取锁
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
   * 
   * @param lockKey   锁的键
   * @param lockValue 锁的值（用于验证锁的持有者）
   * @return 是否成功释放锁
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
   * 防止频繁更新同一房间的状态
   * 
   * @param roomId 房间ID
   * @return 是否可以更新
   */
  public boolean validateRoomEvent(Long roomId) {
    long lastEventTimestamp = cacheManager.getLatestRoomEvent(roomId);
    long currentTime = System.currentTimeMillis();
    long interval = dynamicMQProperties.getRoomEventChangeInterval();

    boolean canUpdate = (currentTime - lastEventTimestamp) >= interval;

    if (!canUpdate) {
      log.debug("Room {} event interval check failed: last={}, current={}, required={}ms",
          roomId, lastEventTimestamp, currentTime, interval);
    }

    return canUpdate;
  }
}
