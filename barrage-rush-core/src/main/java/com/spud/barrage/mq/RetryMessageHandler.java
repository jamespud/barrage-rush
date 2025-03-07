package com.spud.barrage.mq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * @author Spud
 * @date 2025/3/6
 */
@Slf4j
@Component
public class RetryMessageHandler implements MessageRecoverer {

  private static final String RETRY_COUNT_KEY = "mq:retry:count:";
  private static final int MAX_RETRY_COUNT = 3;

  @Autowired
  private RedisTemplate<String, Object> redisTemplate;

  @Override
  public void recover(Message message, Throwable cause) {
    String messageId = message.getMessageProperties().getMessageId();
    String retryKey = RETRY_COUNT_KEY + messageId;

    // 获取重试次数
    Long retryCount = redisTemplate.opsForValue().increment(retryKey);

    if (retryCount <= MAX_RETRY_COUNT) {
      log.warn("Message retry {} times, messageId: {}", retryCount, messageId);
      // TODO: 实现重试逻辑
    } else {
      log.error("Message retry exceed max count, messageId: {}", messageId);
      // TODO: 发送告警，存储失败消息
    }
  }
}