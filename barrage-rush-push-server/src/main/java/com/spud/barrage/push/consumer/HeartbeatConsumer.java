package com.spud.barrage.push.consumer;


import com.spud.barrage.common.data.dto.HeartbeatMessage;
import com.spud.barrage.common.mq.mange.WebSocketManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Spud
 * @date 2025/3/6
 */
@Slf4j
@Component
public class HeartbeatConsumer {

  @Autowired
  private WebSocketManager webSocketManager;

  @RabbitListener(queues = "heartbeat.queue")
  public void handleHeartbeat(HeartbeatMessage message) {
    try {
      // 更新会话最后心跳时间
      webSocketManager.updateHeartbeat(message.getSessionId(), message.getTimestamp());
      log.debug("Process heartbeat message success: {}", message);
    } catch (Exception e) {
      log.error("Process heartbeat message failed: {}", message, e);
    }
  }
}