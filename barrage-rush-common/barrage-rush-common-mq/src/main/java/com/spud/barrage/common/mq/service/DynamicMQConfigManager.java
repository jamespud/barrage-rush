package com.spud.barrage.common.mq.service;

import com.spud.barrage.common.mq.config.DynamicMQProperties;
import com.spud.barrage.common.mq.config.DynamicQueueConfig.RoomType;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
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
public class DynamicMQConfigManager {

  private final AmqpAdmin amqpAdmin;
  private final RedisTemplate<String, Object> redisTemplate;
  private final DynamicMQProperties config;
  private final RoomTrafficManager trafficManager;

  /**
   * 确保房间的MQ资源存在
   */
  public void ensureRoomResources(String roomId) {
    RoomType roomType = trafficManager.getRoomType(roomId);
    int shardCount = getShardCount(roomType);

    // 检查Redis中是否已初始化
    String initKey = String.format("mq:room:%s:initialized", roomId);
    Boolean initialized = (Boolean) redisTemplate.opsForValue().get(initKey);

    if (Boolean.TRUE.equals(initialized)) {
      return;
    }

    try {
      // 创建exchange和queue
      createRoomResources(roomId, roomType, shardCount);

      // 标记为已初始化
      redisTemplate.opsForValue().set(initKey, true);
      redisTemplate.expire(initKey, 24, TimeUnit.HOURS);

      log.info("Created MQ resources for room {}: type={}, shards={}",
          roomId, roomType, shardCount);

    } catch (Exception e) {
      log.error("Failed to create MQ resources for room {}: {}", roomId, e.getMessage());
    }
  }

  /**
   * 创建房间的MQ资源
   */
  private void createRoomResources(String roomId, RoomType roomType, int shardCount) {
    for (int i = 0; i < shardCount; i++) {
      String exchangeName = getExchangeName(roomId, roomType, i);
      String queueName = getQueueName(roomId, roomType, i);
      String routingKey = getRoutingKey(roomId, roomType, i);

      // 创建exchange
      Exchange exchange = ExchangeBuilder.topicExchange(exchangeName)
          .durable(true)
          .build();

      // 创建queue
      Queue queue = QueueBuilder.durable(queueName)
          .withArgument("x-max-length", config.getMaxQueueLength())
          .withArgument("x-message-ttl", config.getMessageTtl())
          .withArgument("x-overflow", "drop-head")
          .build();

      // 创建binding
      Binding binding = BindingBuilder.bind(queue)
          .to(exchange)
          .with(routingKey)
          .noargs();

      // 声明资源
      amqpAdmin.declareExchange(exchange);
      amqpAdmin.declareQueue(queue);
      amqpAdmin.declareBinding(binding);

      // 记录到Redis
      recordResourceBinding(roomId, roomType, exchangeName, queueName, routingKey);
    }
  }

  /**
   * 记录资源绑定关系
   */
  private void recordResourceBinding(String roomId, RoomType roomType,
      String exchange, String queue, String routingKey) {
    String key = String.format("mq:room:%s:bindings", roomId);
    Map<String, Object> binding = Map.of(
        "exchange", exchange,
        "queue", queue,
        "routingKey", routingKey,
        "type", roomType.name(),
        "timestamp", System.currentTimeMillis()
    );

    redisTemplate.opsForHash().put(key, queue, binding);
    redisTemplate.expire(key, 24, TimeUnit.HOURS);
  }

  /**
   * 获取分片数
   */
  private int getShardCount(RoomType roomType) {
    return switch (roomType) {
      case SUPER_HOT -> config.getSuperHotShardCount();
      case HOT -> config.getHotShardCount();
      case NORMAL -> config.getNormalShardCount();
      case COLD -> 1;
    };
  }

  /**
   * 获取exchange名称
   */
  private String getExchangeName(String roomId, RoomType roomType, int shardIndex) {
    return switch (roomType) {
      case COLD -> "danmaku.exchange.cold";
      case NORMAL -> String.format("danmaku.exchange.%d", shardIndex);
      case HOT, SUPER_HOT -> String.format("danmaku.hot.exchange.%s.%d", roomId, shardIndex);
    };
  }

  /**
   * 获取queue名称
   */
  private String getQueueName(String roomId, RoomType roomType, int shardIndex) {
    return switch (roomType) {
      case COLD -> "danmaku.queue.cold";
      case NORMAL -> String.format("danmaku.queue.%d", shardIndex);
      case HOT, SUPER_HOT -> String.format("danmaku.hot.queue.%s.%d", roomId, shardIndex);
    };
  }

  /**
   * 获取routing key
   */
  private String getRoutingKey(String roomId, RoomType roomType, int shardIndex) {
    return switch (roomType) {
      case COLD -> String.format("danmaku.cold.%s", roomId);
      case NORMAL -> String.format("danmaku.room.%s.%d", roomId, shardIndex);
      case HOT, SUPER_HOT -> String.format("danmaku.hot.room.%s.%d", roomId, shardIndex);
    };
  }

  /**
   * 清理过期资源
   */
  @Scheduled(fixedRateString = "${dynamic.mq.cleanup-interval:300}000")
  public void cleanupExpiredResources() {
    try {
      Set<String> bindingKeys = redisTemplate.keys("mq:room:*:bindings");

      for (String key : bindingKeys) {
        String roomId = key.split(":")[2];

        // 检查房间是否还有观众
        String viewerKey = String.format("room:%s:viewers", roomId);
        Object viewersObj = redisTemplate.opsForValue().get(viewerKey);

        if (viewersObj == null || Integer.parseInt(viewersObj.toString()) == 0) {
          // 删除资源
          deleteRoomResources(roomId);
        }
      }
    } catch (Exception e) {
      log.error("Failed to cleanup expired resources: {}", e.getMessage());
    }
  }

  /**
   * 删除房间资源
   */
  private void deleteRoomResources(String roomId) {
    try {
      String bindingKey = String.format("mq:room:%s:bindings", roomId);
      Map<Object, Object> bindings = redisTemplate.opsForHash().entries(bindingKey);

      for (Object bindingObj : bindings.values()) {
        Map<String, Object> binding = (Map<String, Object>) bindingObj;
        String queue = (String) binding.get("queue");
        String exchange = (String) binding.get("exchange");

        // 删除queue和binding
        amqpAdmin.deleteQueue(queue);
        amqpAdmin.deleteExchange(exchange);
      }

      // 删除Redis记录
      redisTemplate.delete(bindingKey);
      redisTemplate.delete(String.format("mq:room:%s:initialized", roomId));

      log.info("Deleted MQ resources for room {}", roomId);
    } catch (Exception e) {
      log.error("Failed to delete resources for room {}: {}", roomId, e.getMessage());
    }
  }
} 