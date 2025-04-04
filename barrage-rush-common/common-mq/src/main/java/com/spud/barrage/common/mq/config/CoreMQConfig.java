package com.spud.barrage.common.mq.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spud.barrage.common.mq.constant.MqConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.util.Pair;

/**
 * 核心MQ配置类
 * 负责管理消息队列的配置和房间的交换机/队列绑定
 *
 * @author Spud
 * @date 2025/4/01
 */
@Slf4j
@Configuration
public class CoreMQConfig {

  private final Random random = new Random();

  @Autowired
  protected RedisTemplate<String, String> redisTemplate;

  @Autowired
  protected RoomManager roomManager;

  @Autowired
  private RedisConnectionFactory redisConnectionFactory;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private CacheManager cacheManager;

  /**
   * 根据房间ID获取其对应的交换机和队列
   * 实现负载均衡策略，而不是简单返回第一个
   *
   * @param roomId 房间ID
   * @return 交换机和队列对
   */
  public Pair<String, String> getExchangeAndQueue(Long roomId) {
    // 从缓存获取房间交换机和队列
    Set<String> exchanges = cacheManager.getRoomExchange(roomId);
    Set<String> queues = cacheManager.getRoomQueue(roomId);

    // 如果Redis中交换机和队列为空，则创建新的交换机和队列
    if (exchanges.isEmpty() || queues.isEmpty()) {
      log.info("No exchange or queue found for roomId={}, creating new ones", roomId);
      roomManager.processRoomStatus(roomId);

      // 重新获取创建的交换机和队列
      exchanges = cacheManager.getRoomExchange(roomId);
      queues = cacheManager.getRoomQueue(roomId);
    }

    if (exchanges.isEmpty() || queues.isEmpty()) {
      log.warn("Failed to create exchange or queue for roomId={}", roomId);
      return Pair.of("", "");
    }

    // 负载均衡策略实现
    String selectedExchange = selectResource(new ArrayList<>(exchanges));
    String selectedQueue = selectResource(new ArrayList<>(queues));

    log.debug("Selected exchange={} and queue={} for roomId={}", selectedExchange, selectedQueue,
        roomId);
    return Pair.of(selectedExchange, selectedQueue);
  }

  /**
   * 资源选择策略
   * 1. 如果只有一个资源，直接返回
   * 2. 如果有多个资源，随机选择一个实现简单的负载均衡
   */
  private String selectResource(List<String> resources) {
    if (resources.isEmpty()) {
      return "";
    } else if (resources.size() == 1) {
      return resources.getFirst();
    } else {
      // 随机选择，实现简单的负载均衡
      return resources.get(random.nextInt(resources.size()));
    }
  }

  /**
   * 设置Redis消息监听器，监听房间MQ配置变化
   */
  @Bean
  public RedisMessageListenerContainer redisMessageListenerContainer() {
    RedisMessageListenerContainer container = new RedisMessageListenerContainer();
    container.setConnectionFactory(redisConnectionFactory);

    // 监听房间MQ配置变化的频道
    MessageListenerAdapter listenerAdapter = new MessageListenerAdapter(new RoomMqChangeListener());
    container.addMessageListener(listenerAdapter,
        new ChannelTopic(MqConstants.RedisTopic.ROOM_MQ_CHANGE));

    log.info("Redis message listener for room MQ changes initialized");
    return container;
  }

  /**
   * 更新房间绑定
   *
   * @param roomId 房间ID
   * @return 是否更新成功
   */
  private boolean updateRoomBinding(Long roomId) {
    try {
      cacheManager.updateLocalCache(roomId);
      log.info("Room binding updated for roomId={}", roomId);
      return true;
    } catch (Exception e) {
      log.error("Failed to update room binding for roomId={}: {}", roomId, e.getMessage(), e);
      return false;
    }
  }

  /**
   * 内部类：Redis消息监听器
   * 监听房间MQ配置变化事件
   */
  private class RoomMqChangeListener implements MessageListener {

    @Override
    public void onMessage(org.springframework.data.redis.connection.Message message,
        byte[] pattern) {
      try {
        // 解析消息内容
        String content = new String(message.getBody());
        log.debug("Received room MQ change event: {}", content);

        Long roomId;
        try {
          roomId = objectMapper.readValue(content, Long.class);
        } catch (Exception e) {
          // 尝试直接解析数字
          roomId = Long.parseLong(content);
        }

        // 更新房间绑定
        if (updateRoomBinding(roomId)) {
          log.info("Successfully processed room MQ change event for roomId={}", roomId);
        }
      } catch (Exception e) {
        log.error("Failed to process room MQ change event: {}", e.getMessage(), e);
      }
    }
  }
}
