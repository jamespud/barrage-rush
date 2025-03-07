package com.spud.barrage.mq.producer;

import com.spud.barrage.model.dto.HeartbeatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Spud
 * @date 2025/3/6
 */
@Slf4j
@Component
public class HeartbeatProducer {

  private static final String HEARTBEAT_EXCHANGE = "heartbeat.exchange";
  private static final String HEARTBEAT_ROUTING_KEY = "heartbeat.routing.key";

  @Autowired
  private RabbitTemplate rabbitTemplate;

  public void sendHeartbeat(HeartbeatMessage message) {
    try {
      rabbitTemplate.convertAndSend(
          HEARTBEAT_EXCHANGE,
          HEARTBEAT_ROUTING_KEY,
          message
      );
      log.debug("Send heartbeat message success: {}", message);
    } catch (Exception e) {
      log.error("Send heartbeat message failed: {}", message, e);
    }
  }
}