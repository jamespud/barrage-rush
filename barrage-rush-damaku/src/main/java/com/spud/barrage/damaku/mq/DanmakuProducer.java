package com.spud.barrage.damaku.mq;

import com.spud.barrage.common.data.dto.DanmakuMessage;
import com.spud.barrage.common.mq.config.DynamicQueueConfig;
import com.spud.barrage.common.mq.config.RabbitMQConfig;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author Spud
 * @date 2025/3/4
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DanmakuProducer {

  private final RabbitTemplate rabbitTemplate;
  private final DynamicQueueConfig queueConfig;
  private final AmqpAdmin amqpAdmin;

  @Value("${rabbitmq.sharding.count:3}")
  private int defaultShardingCount;

  @Value("${rabbitmq.hot.sharding.count:10}")
  private int hotShardingCount;

  // 热门房间的交换机和队列缓存
  private final ConcurrentHashMap<String, Boolean> hotRoomQueueInitialized = new ConcurrentHashMap<>();

  // 热门房间的分片计数
  private final ConcurrentHashMap<String, AtomicInteger> hotRoomShardCounter = new ConcurrentHashMap<>();

  public boolean sendDanmaku(DanmakuMessage message) {
    // TODO: 完善逻辑
    try {
      Long roomId = message.getRoomId();

      // 根据房间类型决定发送策略
      DynamicQueueConfig.RoomType roomType = queueConfig.getRoomType(roomId);

      switch (roomType) {
        case COLD:
          // 冷门房间使用共享队列
          sendToColdQueue(message);
          break;

        case NORMAL:
          // 普通房间使用固定分片
          sendToNormalQueue(message);
          break;

        case HOT:
          // 热门房间使用更多分片和负载均衡
          // 确保热门房间的所有队列已初始化
          ensureAllHotRoomQueuesExist(roomId);
          sendToHotQueue(message);
          break;
      }

      return true;
    } catch (Exception e) {
      log.error("Failed to send danmaku: {}", e.getMessage(), e);
      return false;
    }
  }

  /**
   * 确保热门房间的所有队列都已创建
   */
  private void ensureAllHotRoomQueuesExist(String roomId) {
    // 已初始化过则跳过
    if (hotRoomQueueInitialized.containsKey(roomId)) {
      return;
    }

    synchronized (this) {
      if (hotRoomQueueInitialized.containsKey(roomId)) {
        return;
      }

      log.info("Initializing queues for hot room: {}", roomId);

      // 为热门房间初始化所有分片的队列
      for (int i = 0; i < hotShardingCount; i++) {
        ensureHotExchangeAndQueueExist(roomId, i);
      }

      hotRoomQueueInitialized.put(roomId, true);
    }
  }

  private void sendToColdQueue(DanmakuMessage message) {
    // 所有冷门房间共享一个队列，但使用不同的routing key
    String routingKey = "danmaku.cold." + message.getRoomId();
    rabbitTemplate.convertAndSend("danmaku.exchange.cold", routingKey, message);
  }

  private void sendToNormalQueue(DanmakuMessage message) {
    int shardIndex = getShardingIndex(message.getUserId(), defaultShardingCount);
    String exchange = String.format(RabbitMQConfig.DANMAKU_EXCHANGE_TEMPLATE, shardIndex);
    String routingKey = String.format(RabbitMQConfig.DANMAKU_ROUTING_KEY_TEMPLATE,
        message.getRoomId(), shardIndex);

    rabbitTemplate.convertAndSend(exchange, routingKey, message);
  }

  private void sendToHotQueue(DanmakuMessage message) {
    // 热门房间使用更多分片和轮询策略
    String roomId = message.getRoomId().toString();

    // 获取当前计数并增加，实现简单的轮询负载均衡
    int currentCount = hotRoomShardCounter
        .computeIfAbsent(roomId, k -> new AtomicInteger(0))
        .getAndIncrement() % hotShardingCount;

    String exchange = String.format("danmaku.hot.exchange.%s.%d", roomId, currentCount);
    String routingKey = String.format("danmaku.hot.room.%s.%d", roomId, currentCount);

    rabbitTemplate.convertAndSend(exchange, routingKey, message);
  }

  private void ensureHotExchangeAndQueueExist(String roomId, int shardIndex) {
    String exchangeName = String.format("danmaku.hot.exchange.%s.%d", roomId, shardIndex);
    String queueName = String.format("danmaku.hot.queue.%s.%d", roomId, shardIndex);
    String routingKey = String.format("danmaku.hot.room.%s.%d", roomId, shardIndex);

    // 声明交换机、队列和绑定
    Exchange exchange = ExchangeBuilder.topicExchange(exchangeName).durable(true).build();
    Queue queue = QueueBuilder.durable(queueName)
        .withArgument("x-max-length", 50000)
        .withArgument("x-message-ttl", 30000)
        .withArgument("x-overflow", "drop-head")
        .build();

    Binding binding = BindingBuilder.bind(queue)
        .to(exchange)
        .with(routingKey)
        .noargs();

    try {
      amqpAdmin.declareExchange(exchange);
      amqpAdmin.declareQueue(queue);
      amqpAdmin.declareBinding(binding);
    } catch (Exception e) {
      log.error("Failed to ensure hot exchange and queue: {}", e.getMessage(), e);
    }
  }

  private int getShardingIndex(Long userId, int shardingCount) {
    return Math.abs(userId.hashCode() % shardingCount);
  }
}
