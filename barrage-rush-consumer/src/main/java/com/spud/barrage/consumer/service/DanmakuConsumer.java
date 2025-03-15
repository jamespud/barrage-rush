package com.spud.barrage.consumer.service;

import com.spud.barrage.common.data.dto.DanmakuMessage;
import com.spud.barrage.common.mq.config.RabbitMQConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * @author Spud
 * @date 2025/3/10
 */
@Slf4j
@Component
public class DanmakuConsumer {

  @Autowired
  private RedisTemplate<String, String> redisTemplate;

  @RabbitListener(queues = RabbitMQConfig.DANMAKU_QUEUE_TEMPLATE)
  public void receiveMessage(DanmakuMessage message) {
    log.debug("Received message: {}", message);
    // TODO: 消息过滤
    // TODO: mysql 保存弹幕行为
    redisTemplate.opsForZSet()
        .add(message.getRoomId().toString(), message.getContent(), message.getTimestamp());
  }
}
