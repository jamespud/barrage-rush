package com.spud.barrage.damaku.mq;

import com.spud.barrage.common.data.dto.DanmakuMessage;
import com.spud.barrage.common.mq.config.DynamicQueueConfig;
import com.spud.barrage.common.mq.producer.AbstractRabbitProducer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpAdmin;
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
public class DanmakuProducer extends AbstractRabbitProducer {
  
  public boolean sendDanmaku(DanmakuMessage message) {
    // TODO: 完善逻辑
    boolean sent = false;
    try {
      Long roomId = message.getRoomId();

      sent = sendMessage(roomId, message.getUserId(), message);
      for (int i = 0; !sent && i < 3; i++) {
        sent = sendMessage(roomId, message.getUserId(), message);
      }
    } catch (Exception e) {
      log.error("Failed to send danmaku: {}", e.getMessage(), e);
    }
    return sent;
  }
}
