package com.spud.barrage.common.mq.config;

import com.spud.barrage.common.data.config.RedisLockUtils;
import com.spud.barrage.common.data.mq.enums.RoomType;
import com.spud.barrage.common.mq.constant.MqConstants;
import com.spud.barrage.common.mq.properties.RoomTrafficProperties;
import com.spud.barrage.common.mq.util.MqUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 房间管理器
 * <p>
 * 负责管理房间MQ配置和状态，包括以下功能：
 * 1. 周期性检查房间热度，更新房间类型
 * 2. 根据房间类型分配相应的MQ资源（交换机和队列）
 * 3. 管理房间资源绑定关系，包括交换机和队列的绑定
 * 4. 处理房间MQ配置变更事件，确保消息正确路由
 * </p>
 *
 * @author Spud
 * @date 2025/4/01
 */
@Slf4j
@Component
public class RoomManager {

  @Resource
  private RedisLockUtils redisLockUtils;

  @Resource
  private RedisTemplate<String, Object> redisTemplate;

  /**
   * 用于异步处理房间状态更新的线程池
   */
  private ThreadPoolExecutor roomStatusExecutor;

  /**
   * 用于定时扫描房间状态的调度线程池
   */
  private ScheduledExecutorService scheduledExecutor;

  @Autowired
  private CacheManager cacheManager;

  @Autowired
  private RoomTrafficProperties trafficProperties;

  /**
   * 初始化房间管理器
   */
  @PostConstruct
  public void init() {
    // 创建房间状态处理线程池
    roomStatusExecutor = new ThreadPoolExecutor(trafficProperties.getThreadPool().getCoreSize(),
        trafficProperties.getThreadPool().getMaxSize(),
        trafficProperties.getThreadPool().getKeepAliveSeconds(), TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(trafficProperties.getThreadPool().getQueueCapacity()),
        r -> new Thread(r, "room-status-handler-" + r.hashCode()),
        new ThreadPoolExecutor.CallerRunsPolicy());

    // 创建定时调度线程池
    scheduledExecutor = Executors.newScheduledThreadPool(1);

    // 启动定时扫描任务
    scheduledExecutor.scheduleAtFixedRate(this::checkRoomStatuses,
        trafficProperties.getSchedule().getInitialDelaySeconds(),
        trafficProperties.getSchedule().getPeriodSeconds(), TimeUnit.SECONDS);

    log.info("房间管理器初始化完成，定时检查间隔: {}秒",
        trafficProperties.getSchedule().getPeriodSeconds());
  }

  /**
   * 检查所有活跃房间状态
   */
  private void checkRoomStatuses() {
    log.info("Starting room status refresh task");
    try {
      // 获取所有活跃房间ID
      Set<Object> roomIds = redisTemplate.opsForSet().members(MqConstants.RedisKey.ACTIVE_ROOMS);
      if (roomIds == null || roomIds.isEmpty()) {
        return;
      }
      log.info("Found {} active rooms to process", roomIds.size());

      // 异步处理每个房间的状态
      for (Object roomIdObj : roomIds) {
        try {
          Long roomId = Long.valueOf(roomIdObj.toString());
          roomStatusExecutor.execute(() -> processRoomStatus(roomId));
        } catch (Exception e) {
          log.error("处理房间ID异常: {}", roomIdObj, e);
        }
      }
    } catch (Exception e) {
      log.error("检查房间状态异常", e);
    }
  }

  /**
   * 处理房间状态
   *
   * @param roomId 房间ID
   */
  public void processRoomStatus(Long roomId) {
    if (roomId == null) {
      log.warn("无效的房间ID: null");
      return;
    }
    // 验证是否可以更新房间状态
    boolean canUpdateRoom = validateRoomEvent(roomId);
    if (!canUpdateRoom) {
      // 不能更改房间状态，只更新本地缓存
      log.debug("Skipping room status update for roomId={}, updated too recently", roomId);
      cacheManager.updateLocalCache(roomId);
      return;
    }
    log.debug("start processing room status: roomId={}", roomId);
    try {
      // 尝试获取分布式锁，防止多个节点同时更新
      String lockKey = String.format(MqConstants.RedisKey.ROOM_STATUS_LOCK, roomId);
      boolean locked = redisLockUtils.tryLock(lockKey,
          trafficProperties.getLockTimeoutMillis());

      if (!locked) {
        log.info("Failed to acquire lock for room {}, another process is updating it", roomId);
        return;
      }

      try {
        // 获取当前房间类型和观众数
        RoomType oldType = cacheManager.getRoomType(roomId);
        cacheManager.invalidRoomType(roomId);
        RoomType newType = cacheManager.getRoomType(roomId);

        log.info("Room {} type changing from {} to {}", roomId, oldType, newType);
        boolean updated = handleRoomTypeChange(roomId, oldType, newType);

        if (updated) {
          // 更新状态变更时间
          redisTemplate.opsForValue()
              .set(String.format(MqConstants.RedisKey.ROOM_TYPE_CHANGE, roomId),
                  System.currentTimeMillis());

          // 发布房间状态变化事件，通知其他节点
          publishRoomStatusChangeEvent(roomId);

          log.info("Successfully updated room {} type from {} to {}", roomId, oldType, newType);
        } else {
          log.warn("Failed to update status for room {}", roomId);
        }

      } finally {
        // 释放锁
        redisLockUtils.unlock(lockKey);
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
        redisTemplate.opsForSet()
            .add(String.format(MqConstants.RedisKey.ROOM_QUEUE, roomId), queueName);
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
      case SUPER_HOT -> trafficProperties.getShard().getSuperHotShardCount();
      case HOT -> trafficProperties.getShard().getHotShardCount();
      case NORMAL -> trafficProperties.getShard().getNormalShardCount();
      case COLD -> trafficProperties.getShard().getColdShardCount();
    };
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
    long interval = trafficProperties.getShard().getTypeChangeInterval();

    boolean canUpdate = (currentTime - lastEventTimestamp) >= interval;

    if (!canUpdate) {
      log.debug("Room {} event interval check failed: last={}, current={}, required={}ms", roomId,
          lastEventTimestamp, currentTime, interval);
    }

    return canUpdate;
  }
}
