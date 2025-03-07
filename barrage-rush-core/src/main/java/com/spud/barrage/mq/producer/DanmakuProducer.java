package com.spud.barrage.mq.producer;

import com.spud.barrage.config.RabbitMQConfig;
import com.spud.barrage.model.entity.DanmakuMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Spud
 * @date 2025/3/4
 */
@Slf4j
@Component
public class DanmakuProducer {

  @Autowired
  private RabbitTemplate rabbitTemplate;

  public void sendDanmaku(DanmakuMessage message) {
    try {
      rabbitTemplate.convertAndSend(
          RabbitMQConfig.DANMAKU_EXCHANGE,
          RabbitMQConfig.DANMAKU_ROUTING_KEY,
          message
      );
      log.info("Send danmaku message success: {}", message);
    } catch (Exception e) {
      log.error("Send danmaku message failed: {}", message, e);
      throw new RuntimeException("发送弹幕消息失败", e);
    }
  }
}
