package com.spud.barrage.common.cluster.manager;

import com.spud.barrage.common.cluster.hash.ConsistentHash;
import jakarta.annotation.PreDestroy;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

/**
 * 实例管理器
 * 用于管理集群中的节点实例，提供实例注册、心跳检测和发现功能
 *
 * @author Spud
 * @date 2025/3/23
 */
@Slf4j
public class InstanceManager {

  // TTL倍数
  private static final int HEARTBEAT_TTL_MULTIPLIER = 2;
  // 心跳间隔除数
  private static final int HEARTBEAT_INTERVAL_DIVISOR = 2;
  // 心跳初始延迟除数
  private static final int HEARTBEAT_INITIAL_DELAY_DIVISOR = 4;
  // 实例键格式
  private static final String INSTANCE_KEY_FORMAT = "cluster:%s:instances";
  // 实例变更通道格式
  private static final String INSTANCE_CHANGE_CHANNEL_FORMAT = "cluster:%s:instance-change";
  // 关闭等待超时（秒）
  private static final int SHUTDOWN_WAIT_TIMEOUT = 5;

  // 使用HSET + HEXPIRE组合命令（需要Redis 4.0+）
  private static final String HEARTBEAT_SCRIPT_STR = """
      redis.call('HSET', KEYS[1], ARGV[1], ARGV[2])
      redis.call('HEXPIRE', KEYS[1], ARGV[3], 'FIELDS', 1, ARGV[1])
      return 1""";

  private static final DefaultRedisScript<Long> HEARTBEAT_SCRIPT = new DefaultRedisScript<>(
      HEARTBEAT_SCRIPT_STR,
      Long.class);

  private final RedisTemplate<String, Object> redisTemplate;

  // 一致性哈希环
  private final ConsistentHash<String> consistentHash;

  // 当前实例ID
  @Getter
  private final String instanceId;

  // 实例类型（服务类型）
  private final String instanceType;

  // Redis中实例列表的键
  private final String instancesKey;

  // 实例变更事件通道
  private final String instanceChangeChannel;

  // 心跳执行器
  private final ScheduledExecutorService heartbeatScheduler;
  // 实例心跳过期时间（秒）
  private final int heartbeatTtl;
  // Redis消息监听容器
  private RedisMessageListenerContainer redisListenerContainer;
  // 实例变更回调
  private Consumer<ChangeEvent> changeCallback;

  /**
   * 创建实例管理器
   *
   * @param redisTemplate Redis模板
   * @param instanceType  实例类型
   */
  public InstanceManager(RedisTemplate<String, Object> redisTemplate, String instanceType) {
    this(redisTemplate, instanceType, 25);
  }

  /**
   * 创建实例管理器
   *
   * @param redisTemplate Redis模板
   * @param instanceType  实例类型
   * @param heartbeatTtl  心跳过期时间
   */
  public InstanceManager(RedisTemplate<String, Object> redisTemplate, String instanceType,
      int heartbeatTtl) {
    this.redisTemplate = redisTemplate;
    this.instanceType = instanceType;
    this.instanceId = generateInstanceId();
    this.consistentHash = new ConsistentHash<>();
    this.heartbeatTtl = heartbeatTtl;
    this.instancesKey = String.format(INSTANCE_KEY_FORMAT, instanceType);
    this.instanceChangeChannel = String.format(INSTANCE_CHANGE_CHANNEL_FORMAT, instanceType);
    this.heartbeatScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
      Thread t = new Thread(r, "instance-heartbeat-" + instanceType);
      t.setDaemon(true);
      return t;
    });
  }

  /**
   * 启动实例管理器
   */
  public void start() {
    registerInstance();
    startHeartbeat();
    setupRedisListeners();
    updateHashRing();
    log.info("InstanceManager started for {} with instance ID: {}", instanceType, instanceId);
  }

  /**
   * 生成唯一实例ID
   */
  private String generateInstanceId() {
    return instanceType + "-" + UUID.randomUUID();
  }

  /**
   * 设置实例变更回调
   */
  public void setChangeCallback(Consumer<ChangeEvent> callback) {
    this.changeCallback = callback;
  }

  /**
   * 注册实例
   */
  private void registerInstance() {
    // 使用原子操作添加实例
    redisTemplate.execute(
        HEARTBEAT_SCRIPT,
        Collections.singletonList(instancesKey),
        HEARTBEAT_SCRIPT,
        Collections.singletonList(instancesKey),
        instanceId,
        String.valueOf(System.currentTimeMillis()),
        String.valueOf(heartbeatTtl * 2));
    redisTemplate.convertAndSend(instanceChangeChannel, "add:" + instanceId);
  }

  /**
   * 启动心跳任务
   */
  private void startHeartbeat() {
    heartbeatScheduler.scheduleAtFixedRate(() -> {
      try {
        sendHeartbeat();
        updateHashRing();
      } catch (Exception e) {
        log.error("Error in heartbeat: {}", e.getMessage(), e);
      }
    }, heartbeatTtl / 4, heartbeatTtl / 2, TimeUnit.SECONDS);
  }

  /**
   * 发送心跳
   */
  private void sendHeartbeat() {
    redisTemplate.execute(
        HEARTBEAT_SCRIPT,
        Collections.singletonList(instancesKey),
        HEARTBEAT_SCRIPT,
        Collections.singletonList(instancesKey),
        instanceId,
        String.valueOf(System.currentTimeMillis()),
        String.valueOf(heartbeatTtl * 2));
  }

  /**
   * 设置Redis监听器
   */
  private void setupRedisListeners() {
    // 创建消息监听器
    MessageListener listener = new MessageListener() {
      @Override
      public void onMessage(Message message, byte[] pattern) {
        String body = new String(message.getBody());
        handleInstanceChangeEvent(body);
      }
    };

    MessageListenerAdapter adapter = new MessageListenerAdapter(listener);

    RedisMessageListenerContainer container = new RedisMessageListenerContainer();
    container.setConnectionFactory(redisTemplate.getConnectionFactory());
    container.addMessageListener(adapter, new ChannelTopic(instanceChangeChannel));
    container.afterPropertiesSet();
    container.start();

    this.redisListenerContainer = container;
  }

  /**
   * 处理实例变更事件
   */
  private void handleInstanceChangeEvent(String event) {
    try {
      // 解析事件
      String[] parts = event.split(":", 2);
      if (parts.length != 2) {
        log.warn("Invalid instance change event format: {}", event);
        return;
      }

      String operation = parts[0];
      String targetInstanceId = parts[1];

      // 更新哈希环
      boolean updated = false;

      if ("add".equals(operation)) {
        // 实例添加事件
        log.info("Instance added: {}", targetInstanceId);
        updateHashRing();
        updated = true;

        if (changeCallback != null) {
          changeCallback.accept(new ChangeEvent(ChangeEventType.INSTANCE_ADDED, targetInstanceId));
        }
      } else if ("remove".equals(operation) || "offline".equals(operation)) {
        // 实例移除事件
        log.info("Instance removed/offline: {}", targetInstanceId);
        updateHashRing();
        updated = true;

        if (changeCallback != null) {
          changeCallback.accept(
              new ChangeEvent(ChangeEventType.INSTANCE_REMOVED, targetInstanceId));
        }
      } else if ("rebuild".equals(operation)) {
        // 哈希环重建事件
        log.info("Hash ring rebuild requested by: {}", targetInstanceId);
        updateHashRing();
        updated = true;

        if (changeCallback != null) {
          changeCallback.accept(new ChangeEvent(ChangeEventType.RING_REBUILT, targetInstanceId));
        }
      }

      // 如果哈希环有更新，可能需要重新分配资源
      if (updated && changeCallback != null) {
        changeCallback.accept(new ChangeEvent(ChangeEventType.RING_REBUILT, null));
      }
    } catch (Exception e) {
      log.error("Error handling instance change event: {}", e.getMessage(), e);
    }
  }

  /**
   * 更新哈希环
   */
  private void updateHashRing() {
    // 获取所有活跃实例
    Map<Object, Object> instances = redisTemplate.opsForHash().entries(instancesKey);

    // 重建哈希环
    consistentHash.clear();

    // 检查实例是否活跃（超过2个TTL未更新则认为不活跃）
    long now = System.currentTimeMillis();
    long expirationThreshold = now - (heartbeatTtl * HEARTBEAT_TTL_MULTIPLIER * 1000L);

    for (Map.Entry<Object, Object> entry : instances.entrySet()) {
      String id = entry.getKey().toString();
      long timestamp = Long.parseLong(entry.getValue().toString());

      if (timestamp >= expirationThreshold) {
        // 活跃实例，添加到哈希环
        consistentHash.addNode(id);
      } else {
        // 过期实例，从Redis中移除
        redisTemplate.opsForHash().delete(instancesKey, id);

        // 通知其他实例
        redisTemplate.convertAndSend(instanceChangeChannel, "remove:" + id);

        log.info("Removed expired instance: {}", id);
      }
    }
  }

  /**
   * 检查指定资源是否由当前实例负责
   *
   * @param resourceId 资源ID
   * @return 如果由当前实例负责则返回true，否则返回false
   */
  public boolean isResponsibleFor(String resourceId) {
    return consistentHash.isNodeResponsible(instanceId, resourceId);
  }

  /**
   * 检查指定资源是否由当前实例负责
   *
   * @param resourceId 资源ID
   * @return 如果由当前实例负责则返回true，否则返回false
   */
  public boolean isResponsibleFor(Number resourceId) {
    return isResponsibleFor(resourceId.toString());
  }

  /**
   * 获取负责指定资源的实例ID
   *
   * @param resourceId 资源ID
   * @return 负责该资源的实例ID，如果没有则返回null
   */
  public String getResponsibleInstance(String resourceId) {
    return consistentHash.getNode(resourceId);
  }

  /**
   * 获取负责指定资源的实例ID
   *
   * @param resourceId 资源ID
   * @return 负责该资源的实例ID，如果没有则返回null
   */
  public String getResponsibleInstance(Number resourceId) {
    return getResponsibleInstance(resourceId.toString());
  }

  /**
   * 获取当前所有实例ID
   *
   * @return 所有实例ID的集合
   */
  public Set<String> getAllInstances() {
    return consistentHash.getNodes();
  }

  /**
   * 获取实例数量
   *
   * @return 实例数量
   */
  public int getInstanceCount() {
    return consistentHash.getNodeCount();
  }

  /**
   * 获取一致性哈希实例
   *
   * @return 一致性哈希实例
   */
  public ConsistentHash<String> getConsistentHash() {
    return consistentHash;
  }

  /**
   * 主动请求重建哈希环
   */
  public void requestRingRebuild() {
    redisTemplate.convertAndSend(instanceChangeChannel, "rebuild:" + instanceId);
  }

  /**
   * 优雅关闭
   */
  @PreDestroy
  public void shutdown() {
    try {
      // 发送实例下线通知
      redisTemplate.convertAndSend(instanceChangeChannel, "offline:" + instanceId);

      // 从Redis中移除实例
      redisTemplate.opsForHash().delete(instancesKey, instanceId);

      // 关闭心跳线程
      heartbeatScheduler.shutdown();
      try {
        if (!heartbeatScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
          heartbeatScheduler.shutdownNow();
        }
      } catch (InterruptedException e) {
        heartbeatScheduler.shutdownNow();
        Thread.currentThread().interrupt();
      }

      // 关闭Redis监听容器
      if (redisListenerContainer != null) {
        redisListenerContainer.stop();
      }

      log.info("InstanceManager for {} shutdown complete", instanceType);
    } catch (Exception e) {
      log.error("Error shutting down instance manager: {}", e.getMessage(), e);
    }
  }

  /**
   * 实例变更事件类型
   */
  public enum ChangeEventType {
    // 实例添加
    INSTANCE_ADDED,
    // 实例移除
    INSTANCE_REMOVED,
    // 哈希环重建
    RING_REBUILT
  }

  /**
   * 实例变更事件
   */
  public static class ChangeEvent {

    // 事件类型
    @Getter
    private final ChangeEventType type;

    // 变更的实例ID
    @Getter
    private final String instanceId;

    public ChangeEvent(ChangeEventType type, String instanceId) {
      this.type = type;
      this.instanceId = instanceId;
    }
  }
}
  