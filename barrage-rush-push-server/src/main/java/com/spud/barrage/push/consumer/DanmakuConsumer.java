package com.spud.barrage.push.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spud.barrage.common.data.dto.DanmakuMessage;
import com.spud.barrage.common.data.repository.DanmakuRepository;
import com.spud.barrage.common.mq.mange.WebSocketManager;
import com.spud.barrage.constant.ApiConstants;
import com.spud.barrage.constant.Constants;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * @author Spud
 * @date 2025/3/4
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DanmakuConsumer {

  private final DanmakuRepository danmakuRepository;
  private final RedisTemplate<String, Object> redisTemplate;

  @Autowired
  private WebSocketManager webSocketManager;

  @Autowired
  private ObjectMapper objectMapper;

  @RabbitListener(queues = "#{@dynamicDanmakuQueues}")
  public void handleDanmaku(DanmakuMessage message) {
    try {
      // 1. 保存到数据库
      danmakuRepository.save(message);

      // 2. 同步到Redis
      String key = String.format(ApiConstants.REDIS_ROOM_MESSAGES, message.getRoomId());
      redisTemplate.opsForZSet().add(key, message, message.getTimestamp());
      redisTemplate.expire(key, Constants.ROOM_MESSAGES_EXPIRE, TimeUnit.SECONDS);

      log.debug("Consumed danmaku message: {}", message);
    } catch (Exception e) {
      log.error("Failed to consume danmaku message: {}", message, e);
      throw e;
    }
  }
}