package com.spud.barrage.common.mq.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spud.barrage.common.cluster.manager.InstanceManager;
import com.spud.barrage.common.data.config.RedisConfig;
import com.spud.barrage.common.mq.util.MqUtils;
import com.spud.barrage.constant.RoomType;
import jakarta.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

/**
 * 动态消费者配置
 * 处理消费者与队列的动态绑定
 *
 * @author Spud
 * @date 2025/3/10
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DynamicConsumerConfig {

  private final AmqpAdmin amqpAdmin;
  private final RabbitTemplate rabbitTemplate;
  private final RedisTemplate<String, Object> redisTemplate;
  private final RedisConnectionFactory redisConnectionFactory;
  private final ObjectMapper objectMapper;
  private final RabbitListenerEndpointRegistry registry;
  private final ResourceManager resourceManager;
  private final InstanceManager instanceManager;

  // 记录已绑定的队列
  private final Set<String> boundQueues = ConcurrentHashMap.newKeySet();
  
  @Autowired
  private CacheManager cacheManager;

  /**
   * 暴露boundQueues集合作为Bean
   */
  @Bean
  public Set<String> getBoundQueues() {
    return boundQueues;
  }

  /**
   * 初始化方法，用于系统启动时加载所有活跃房间的绑定
   */
  @PostConstruct
  public void init() {
    try {
      // 获取所有活跃房间
      Set<String> activeRooms = redisTemplate.keys(RedisConfig.ACTIVE_ROOM);
      if (activeRooms.isEmpty()) {
        return;
      }

      // 对房间ID进行排序，确保多实例时处理顺序一致
      List<Long> roomIds = activeRooms.stream()
          .map(MqUtils::extractRoomId)
          .filter(Objects::nonNull)
          .sorted()
          .toList();

      int boundCount = 0;
      // 遍历所有活跃房间，但只绑定由当前实例负责的房间
      for (Long roomId : roomIds) {
        // 通过一致性哈希判断该房间是否由当前实例负责
        if (instanceManager.isResponsibleFor(roomId)) {
          try {
            RoomType roomType = cacheManager.getRoomType(roomId);
            bindConsumerToRoom(roomId, roomType);
            boundCount++;
          } catch (Exception e) {
            log.error("Failed to bind consumer for room {}: {}", roomId, e.getMessage(), e);
          }
        }
      }

      log.info("Initialized consumer bindings for {} active rooms out of {} total",
          boundCount, roomIds.size());

      // 设置Redis事件监听器
      setupRedisEventListeners();

    } catch (Exception e) {
      log.error("Failed to initialize consumer bindings: {}", e.getMessage(), e);
    }
  }

  // 设置Redis事件监听器
  private void setupRedisEventListeners() {
    RedisMessageListenerContainer container = new RedisMessageListenerContainer();
    container.setConnectionFactory(redisConnectionFactory);

    // 监听房间状态变化事件
    MessageListenerAdapter roomChangeListener = new MessageListenerAdapter(new RoomStatusChangeHandler());
    container.addMessageListener(roomChangeListener, new ChannelTopic(RabbitMQConfig.ROOM_MQ_CHANGE_TOPIC));

    // 监听实例变化事件
    MessageListenerAdapter instanceChangeListener = new MessageListenerAdapter(new InstanceChangeHandler());
    container.addMessageListener(instanceChangeListener, new ChannelTopic("mq:instance:change"));

    container.start();
  }

  // 添加实例变化处理器
  private class InstanceChangeHandler implements MessageListener {
    @Override
    public void onMessage(Message message, byte[] pattern) {
      try {
        String change = new String(message.getBody());

        // 处理实例离线事件
        if (change.startsWith("offline:")) {
          String offlineInstanceId = change.substring("offline:".length());
          log.info("Detected instance offline: {}", offlineInstanceId);
          // 需要重新平衡房间分配
          rebalanceRooms();
        } else {
          // 实例上线或其他变更
          log.info("Instance change detected: {}", change);
          rebalanceRooms();
        }
      } catch (Exception e) {
        log.error("Failed to process instance change event: {}", e.getMessage(), e);
      }
    }
  }

  // 重新平衡房间分配
  private void rebalanceRooms() {
    try {
      // 获取所有活跃房间
      Set<String> activeRooms = redisTemplate.keys(RedisConfig.ACTIVE_ROOM);
      if (activeRooms == null || activeRooms.isEmpty()) {
        return;
      }

      // 提取房间ID
      List<Long> roomIds = activeRooms.stream()
          .map(MqUtils::extractRoomId)
          .filter(Objects::nonNull)
          .toList();

      // 处理不再负责的房间
      List<Long> currentlyBoundRooms = boundQueues.stream()
          .map(MqUtils::extractRoomIdFromQueue)
          .filter(Objects::nonNull)
          .distinct()
          .toList();

      for (Long roomId : currentlyBoundRooms) {
        if (!instanceManager.isResponsibleFor(roomId)) {
          // 解绑不再负责的房间队列
          List<String> roomQueues = boundQueues.stream()
              .filter(q -> q.contains("." + roomId + "."))
              .toList();

          for (String queue : roomQueues) {
            unbindConsumerFromQueue(queue);
          }

          log.info("Unbound queues for room {} - no longer responsible", roomId);
        }
      }

      // 处理新负责的房间
      int newlyBound = 0;
      for (Long roomId : roomIds) {
        if (instanceManager.isResponsibleFor(roomId)) {
          RoomType roomType = cacheManager.getRoomType(roomId);
          bindConsumerToRoom(roomId, roomType);
          newlyBound++;
        }
      }

      if (newlyBound > 0) {
        log.info("Newly bound {} rooms during rebalancing", newlyBound);
      }
    } catch (Exception e) {
      log.error("Failed to rebalance rooms: {}", e.getMessage(), e);
    }
  }

  /**
   * 监听房间状态变化的Redis事件
   */
  @Bean
  public MessageListenerAdapter roomStatusChangeListener() {
    return new MessageListenerAdapter(new RoomStatusChangeHandler());
  }

  // Redis消息监听器处理器
  private class RoomStatusChangeHandler implements MessageListener {
    @Override
    public void onMessage(Message message, byte[] pattern) {
      try {
        String body = new String(message.getBody());
        Long roomId = objectMapper.readValue(body, Long.class);

        // 只处理由当前实例负责的房间
        if (instanceManager.isResponsibleFor(roomId)) {
          RoomType roomType = cacheManager.getRoomType(roomId);

          // 先解绑旧的队列
          Set<String> oldQueues = boundQueues.stream()
              .filter(q -> q.contains("." + roomId + "."))
              .collect(Collectors.toSet());

          for (String queueName : oldQueues) {
            unbindConsumerFromQueue(queueName);
          }

          // 绑定新的队列
          bindConsumerToRoom(roomId, roomType);
        }
      } catch (Exception e) {
        log.error("Failed to process room status change event: {}", e.getMessage(), e);
      }
    }
  }

  /**
   * 为房间状态变化绑定消费者
   */
  public void bindConsumerToRoom(Long roomId, RoomType roomType) {
    try {
      // 使用分布式锁防止并发绑定
      String lockKey = "consumer:bind:lock:" + roomId;
      String lockValue = UUID.randomUUID().toString();
      boolean locked = acquireLock(lockKey, lockValue, 10000);

      if (!locked) {
        log.warn("Failed to acquire lock for binding room {}, skipping", roomId);
        return;
      }

      try {
        // 获取队列名称
        String queueName = getQueueNameForRoom(roomId, roomType);

        // 检查队列是否已绑定
        if (boundQueues.contains(queueName)) {
          log.info("Consumer already bound to queue: {}", queueName);
          return;
        }

        // 确保队列存在
        Queue queue = new Queue(queueName, true, false, false);
        Object declareResult = amqpAdmin.declareQueue(queue);

        if (declareResult == null) {
          log.error("Failed to declare queue {} for room {}", queueName, roomId);
          return;
        }

        // 获取交换机名称
        String exchangeName = getExchangeNameForQueue(queueName);

        // 确保交换机和绑定关系存在
        String routingKey = String.format(RabbitMQConfig.DANMAKU_ROUTING_KEY, queueName);
        Binding binding = new Binding(queueName, Binding.DestinationType.QUEUE, exchangeName,
            routingKey, null);

        try {
          amqpAdmin.declareBinding(binding);
        } catch (Exception e) {
          log.error("Failed to declare binding for queue {}: {}", queueName, e.getMessage());
          return;
        }

        // 刷新消费者
        RabbitListenerEndpointRegistry registry = this.registry;
        if (registry != null && registry.getListenerContainer("danmakuConsumer") != null) {
          registry.getListenerContainer("danmakuConsumer").start();
        } else {
          log.warn("Consumer container not found, queue binding may not be active");
        }

        boundQueues.add(queueName);

        log.info("Successfully bound consumer to queue: {} for room: {} with type: {}",
            queueName, roomId, roomType);
      } finally {
        // 释放锁
        releaseLock(lockKey, lockValue);
      }
    } catch (Exception e) {
      log.error("Failed to bind consumer to queue for room {}: {}", roomId, e.getMessage(), e);
    }
  }

  /**
   * 解绑消费者
   */
  public void unbindConsumerFromQueue(String queueName) {
    try {
      if (!boundQueues.contains(queueName)) {
        return;
      }

      // 解绑队列
      String exchangeName = getExchangeNameForQueue(queueName);
      amqpAdmin.removeBinding(new Binding(queueName, Binding.DestinationType.QUEUE, exchangeName,
          String.format(RabbitMQConfig.DANMAKU_ROUTING_KEY, queueName), null));

      boundQueues.remove(queueName);
      log.info("Successfully unbound consumer from queue: {}", queueName);
    } catch (Exception e) {
      log.error("Failed to unbind consumer from queue {}: {}", queueName, e.getMessage(), e);
    }
  }

  /**
   * 根据队列名获取交换机名
   */
  private String getExchangeNameForQueue(String queueName) {
    return queueName.replace(RabbitMQConfig.DANMAKU_QUEUE_PREFIX,
        RabbitMQConfig.DANMAKU_EXCHANGE_PREFIX);
  }

  /**
   * 根据房间ID和类型获取队列名称
   */
  private String getQueueNameForRoom(Long roomId, RoomType roomType) {
    return resourceManager.buildQueueName(roomType, roomId,
        resourceManager.buildExchangeName(roomType, roomId));
  }

  /**
   * 房间MQ配置变化事件
   */
  @Data
  public static class RoomMqChangeEvent {
    private Long roomId;
  }

  // 获取分布式锁
  private boolean acquireLock(String lockKey, String lockValue, long timeout) {
    try {
      String script = "if redis.call('setnx', KEYS[1], ARGV[1]) == 1 then " +
          "redis.call('pexpire', KEYS[1], ARGV[2]) " +
          "return 1 " +
          "else " +
          "return 0 " +
          "end";

      Long result = redisTemplate.execute(
          new DefaultRedisScript<>(script, Long.class),
          Collections.singletonList(lockKey),
          lockValue, String.valueOf(timeout));

      return Boolean.TRUE.equals(result == 1L);
    } catch (Exception e) {
      log.error("Error acquiring lock {}: {}", lockKey, e.getMessage());
      return false;
    }
  }

  // 释放分布式锁
  private boolean releaseLock(String lockKey, String lockValue) {
    try {
      String script = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
          "return redis.call('del', KEYS[1]) " +
          "else " +
          "return 0 " +
          "end";

      Long result = redisTemplate.execute(
          new DefaultRedisScript<>(script, Long.class),
          Collections.singletonList(lockKey),
          lockValue);

      return Boolean.TRUE.equals(result == 1L);
    } catch (Exception e) {
      log.error("Error releasing lock {}: {}", lockKey, e.getMessage());
      return false;
    }
  }
}