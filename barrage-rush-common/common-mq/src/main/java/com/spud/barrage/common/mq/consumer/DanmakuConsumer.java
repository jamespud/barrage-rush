package com.spud.barrage.common.mq.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spud.barrage.common.data.dto.DanmakuMessage;
import com.spud.barrage.common.data.repository.DanmakuRepository;
import com.spud.barrage.common.mq.config.DynamicConsumerConfig;
import com.spud.barrage.constant.ApiConstants;
import com.spud.barrage.constant.Constants;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 弹幕消费者
 * 支持动态监听不同房间的队列
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DanmakuConsumer {

  private final DanmakuRepository danmakuRepository;
  private final RedisTemplate<String, Object> redisTemplate;
  private final DynamicConsumerConfig dynamicConsumerConfig;

  @Autowired
  private ObjectMapper objectMapper;

  /**
   * 监听所有动态队列
   * 通过Redis事件通知实现队列的动态绑定和解绑
   */
  @RabbitListener(queues = "#{dynamicConsumerConfig.boundQueues}")
  public void handleDanmaku(Message message) {
    try {
      // 从消息中获取房间ID
      String queueName = message.getMessageProperties().getConsumerQueue();
      Long roomId = extractRoomIdFromQueue(queueName);

      if (roomId == null) {
        log.error("Failed to extract room ID from queue name: {}", queueName);
        return;
      }

      // 解析消息
      DanmakuMessage danmakuMessage = objectMapper.readValue(message.getBody(),
          DanmakuMessage.class);
      danmakuMessage.setRoomId(roomId);

      // 1. 保存到数据库
      danmakuRepository.save(danmakuMessage);

      // 2. 同步到Redis
      String key = String.format(ApiConstants.REDIS_ROOM_MESSAGES, roomId);
      redisTemplate.opsForZSet().add(key, danmakuMessage, danmakuMessage.getSendTime());
      redisTemplate.expire(key, Constants.ROOM_MESSAGES_EXPIRE, TimeUnit.SECONDS);

      log.debug("Consumed danmaku message for room {}: {}", roomId, danmakuMessage);
    } catch (Exception e) {
      log.error("Failed to consume danmaku message: {}", message, e);
    }
  }

  /**
   * 从队列名称中提取房间ID
   */
  private Long extractRoomIdFromQueue(String queueName) {
    try {
      String[] parts = queueName.split("\\.");
      if (parts.length < 4) {
        return null;
      }
      return Long.parseLong(parts[parts.length - 1]);
    } catch (Exception e) {
      log.error("Failed to extract room ID from queue name {}: {}", queueName, e.getMessage(), e);
      return null;
    }
  }
}