package com.spud.barrage.consumer.mq;

import com.spud.barrage.common.data.dto.DanmakuMessage;
import com.spud.barrage.common.mq.config.DynamicConsumerConfig;
import com.spud.barrage.consumer.service.DanmakuProcessService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.DirectMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 弹幕消费者
 * 负责监听和消费RabbitMQ中的弹幕消息
 * 
 * @author Spud
 * @date 2025/3/11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DanmakuConsumer implements DisposableBean {

  private final ConnectionFactory connectionFactory;
  private final DanmakuProcessService danmakuProcessService;
  private final Jackson2JsonMessageConverter messageConverter;
  private final RedisTemplate<String, Object> redisTemplate;

  @Autowired(required = false)
  private DynamicConsumerConfig dynamicConsumerConfig;

  // 存储创建的监听容器，用于优雅关闭
  private final List<MessageListenerContainer> listenerContainers = new ArrayList<>();
  // 存储热门房间的监听容器，用于动态管理
  private final Map<String, List<MessageListenerContainer>> hotRoomContainers = new ConcurrentHashMap<>();

  // Redis消息监听容器
  private RedisMessageListenerContainer redisListenerContainer;

  @Value("${rabbitmq.sharding.count:3}")
  private int defaultShardingCount;

  @Value("${consumer.prefetch.count:100}")
  private int prefetchCount;

  @Value("${consumer.hot-room.max-shards:5}")
  private int maxHotRoomShards;

  @PostConstruct
  public void init() {
    // 初始化标准分片的消费者
    setupNormalQueueConsumers();

    // 初始化冷门房间共享队列的消费者
    setupColdQueueConsumer();

    // 初始化Redis监听器
    setupRedisListeners();

    log.info("DanmakuConsumer initialized with {} normal shards", defaultShardingCount);
  }

  /**
   * 设置标准分片队列的消费者
   */
  private void setupNormalQueueConsumers() {
    for (int i = 0; i < defaultShardingCount; i++) {
      String queueNamePattern = String.format("danmaku.queue.*." + i);
      DirectMessageListenerContainer container = createMessageListenerContainer(queueNamePattern);
      listenerContainers.add(container);
      container.start();
    }
  }

  /**
   * 设置冷门房间共享队列的消费者
   */
  private void setupColdQueueConsumer() {
    DirectMessageListenerContainer container = createMessageListenerContainer(
        "danmaku.queue.shared.cold");
    listenerContainers.add(container);
    container.start();
  }

  /**
   * 设置Redis事件监听器
   */
  private void setupRedisListeners() {
    try {
      RedisMessageListenerContainer container = new RedisMessageListenerContainer();
      container.setConnectionFactory(redisTemplate.getConnectionFactory());

      // 监听房间热度变化事件
      RoomHeatChangeListener heatChangeListener = new RoomHeatChangeListener();
      container.addMessageListener(heatChangeListener,
          new ChannelTopic("room:heat:change"));

      // 监听房间MQ配置变化事件
      container.addMessageListener(heatChangeListener,
          new ChannelTopic("room:mq:change"));

      container.start();
      this.redisListenerContainer = container;

      log.info("Redis event listeners initialized");
    } catch (Exception e) {
      log.error("Failed to initialize Redis listeners: {}", e.getMessage(), e);
    }
  }

  /**
   * 为热门房间创建专门的消费者
   */
  public void setupHotRoomConsumer(String roomId, int shardCount) {
    // 限制最大分片数
    int actualShards = Math.min(shardCount, maxHotRoomShards);

    // 如果该房间已有消费者，先移除
    removeHotRoomConsumer(roomId);

    List<MessageListenerContainer> containers = new ArrayList<>();

    for (int i = 0; i < actualShards; i++) {
      String queueName = String.format("danmaku.hot.queue.%s.%d", roomId, i);
      DirectMessageListenerContainer container = createMessageListenerContainer(queueName);
      containers.add(container);
      container.start();
    }

    hotRoomContainers.put(roomId, containers);
    log.info("Created {} consumers for hot room {}", actualShards, roomId);

    // 通知动态消费者配置
    if (dynamicConsumerConfig != null) {
      try {
        dynamicConsumerConfig.bindConsumerToRoom(Long.parseLong(roomId),
            com.spud.barrage.constant.RoomType.HOT);
      } catch (Exception e) {
        log.error("Failed to notify dynamic config for hot room {}: {}",
            roomId, e.getMessage(), e);
      }
    }
  }

  /**
   * 移除热门房间的消费者
   */
  public void removeHotRoomConsumer(String roomId) {
    List<MessageListenerContainer> containers = hotRoomContainers.remove(roomId);
    if (containers != null) {
      for (MessageListenerContainer container : containers) {
        try {
          container.stop();
        } catch (Exception e) {
          log.error("Error stopping container for room {}: {}", roomId, e.getMessage());
        }
      }
      log.info("Removed consumers for hot room {}", roomId);
    }
  }

  /**
   * 创建消息监听容器
   */
  private DirectMessageListenerContainer createMessageListenerContainer(String queueName) {
    DirectMessageListenerContainer container = new DirectMessageListenerContainer();
    container.setConnectionFactory(connectionFactory);
    container.setQueueNames(queueName);
    container.setPrefetchCount(prefetchCount);
    container.setAcknowledgeMode(AcknowledgeMode.AUTO);

    // 设置消息监听器
    MessageListenerAdapter adapter = new MessageListenerAdapter(new DanmakuMessageHandler());
    adapter.setMessageConverter(messageConverter);
    adapter.setDefaultListenerMethod("handleMessage");
    container.setMessageListener(adapter);

    return container;
  }

  /**
   * 房间热度变化监听器
   */
  private class RoomHeatChangeListener implements org.springframework.data.redis.connection.MessageListener {
    @Override
    public void onMessage(org.springframework.data.redis.connection.Message message, byte[] pattern) {
      try {
        String roomIdStr = new String(message.getBody());
        log.info("Received room heat change event for room: {}", roomIdStr);

        // 获取房间热度级别并适当调整消费者
        String heatKey = String.format("room:%s:heat:level", roomIdStr);
        Object heatLevel = redisTemplate.opsForValue().get(heatKey);

        if (heatLevel != null) {
          int level = Integer.parseInt(heatLevel.toString());
          if (level >= 3) { // 高热度
            setupHotRoomConsumer(roomIdStr, 4);
          } else if (level >= 2) { // 中等热度
            setupHotRoomConsumer(roomIdStr, 2);
          } else {
            // 低热度，移除专用消费者
            removeHotRoomConsumer(roomIdStr);
          }
        }
      } catch (Exception e) {
        log.error("Error processing room heat change: {}", e.getMessage(), e);
      }
    }
  }

  @Override
  public void destroy() {
    // 停止所有监听容器
    for (MessageListenerContainer container : listenerContainers) {
      try {
        container.stop();
      } catch (Exception e) {
        log.error("Error stopping listener container: {}", e.getMessage());
      }
    }

    // 停止所有热门房间的监听容器
    for (List<MessageListenerContainer> containers : hotRoomContainers.values()) {
      for (MessageListenerContainer container : containers) {
        try {
          container.stop();
        } catch (Exception e) {
          log.error("Error stopping hot room container: {}", e.getMessage());
        }
      }
    }

    // 停止Redis监听容器
    if (redisListenerContainer != null) {
      try {
        redisListenerContainer.stop();
      } catch (Exception e) {
        log.error("Error stopping Redis listener container: {}", e.getMessage());
      }
    }

    log.info("DanmakuConsumer destroyed successfully");
  }

  /**
   * 消息处理内部类
   */
  private class DanmakuMessageHandler implements MessageListener {

    @Override
    public void onMessage(Message message) {
      try {
        // 转换消息
        DanmakuMessage danmakuMessage = (DanmakuMessage) messageConverter.fromMessage(message);

        // 更新处理速率计数
        String rateKey = String.format("room:%s:danmaku:rate", danmakuMessage.getRoomId());
        redisTemplate.opsForValue().increment(rateKey);
        // 设置60秒过期，用于每分钟计算速率
        redisTemplate.expire(rateKey, 60, TimeUnit.SECONDS);

        // 处理消息
        danmakuProcessService.processDanmaku(danmakuMessage);
      } catch (Exception e) {
        log.error("Failed to process danmaku message: {}", e.getMessage(), e);
      }
    }
  }
}