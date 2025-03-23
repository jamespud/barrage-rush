package com.spud.barrage.common.mq.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spud.barrage.common.data.config.RedisConfig;
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
 * @author Spud
 * @date 2025/3/22
 */
@Slf4j
@Configuration
public class CoreMQConfig {

  // 使用Redis存储和读取直播间信息
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

  public Pair<Object, Object> getExchangeAndQueue(Long roomId) {
    // 从缓存获取房间ex和队列
    Set<String> exchanges = CacheManager.ROOM_EXCHANGE_CACHE.get(roomId,
        k -> redisTemplate.opsForSet().members(String.format(RedisConfig.ROOM_EXCHANGE, roomId)));
    Set<String> queues = CacheManager.ROOM_QUEUE_CACHE.get(roomId,
        k -> redisTemplate.opsForSet().members(String.format(RedisConfig.ROOM_QUEUE, roomId)));
    // 如果redis中ex和队列为空，则创建新的ex和队列
    if (exchanges.isEmpty() || queues.isEmpty()) {
      roomManager.processRoomStatus(roomId);
      exchanges = CacheManager.ROOM_EXCHANGE_CACHE.get(roomId,
          k -> redisTemplate.opsForSet().members(String.format(RedisConfig.ROOM_EXCHANGE, roomId)));
      queues = CacheManager.ROOM_QUEUE_CACHE.get(roomId,
          k -> redisTemplate.opsForSet().members(String.format(RedisConfig.ROOM_QUEUE, roomId)));
    }
    if (exchanges.isEmpty() || queues.isEmpty()) {
      log.warn("No exchange or queue found for roomId={}", roomId);
    }
    //  TODO: 负载均衡 - 目前简单返回第一个
    return Pair.of(
        exchanges.stream().findFirst().orElse(""),
        queues.stream().findFirst().orElse(""));
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
        new ChannelTopic(RabbitMQConfig.ROOM_MQ_CHANGE_TOPIC));

    return container;
  }

  /**
   * 内部类：Redis消息监听器
   */
  private class RoomMqChangeListener implements MessageListener {

    @Override
    public void onMessage(org.springframework.data.redis.connection.Message message,
        byte[] pattern) {
      try {
        // 解析消息内容
        String content = new String(message.getBody());
        Long roomId = objectMapper.readValue(content, Long.class);

        // 更新房间绑定
        updateRoomBinding(roomId);

        log.info("Processed room MQ change event: {}", roomId);
      } catch (Exception e) {
        log.error("Failed to process room MQ change event: {}", e.getMessage(), e);
      }
    }
  }

  private boolean updateRoomBinding(Long roomId) {
    cacheManager.updateLocalCache(roomId);
    return false;
  }


}
