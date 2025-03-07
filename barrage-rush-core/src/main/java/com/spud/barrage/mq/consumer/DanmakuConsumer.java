package com.spud.barrage.mq.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spud.barrage.config.RabbitMQConfig;
import com.spud.barrage.model.dto.WebSocketResponse;
import com.spud.barrage.model.entity.DanmakuMessage;
import com.spud.barrage.websocket.WebSocketManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Spud
 * @date 2025/3/4
 */
@Slf4j
@Component
public class DanmakuConsumer {

  @Autowired
  private WebSocketManager webSocketManager;

  @Autowired
  private ObjectMapper objectMapper;

  @RabbitListener(queues = RabbitMQConfig.DANMAKU_QUEUE)
  public void handleDanmaku(DanmakuMessage message) {
    try {
      // 1. 构建WebSocket响应
      WebSocketResponse<DanmakuMessage> response = new WebSocketResponse<>("DANMAKU", message);

      // 2. 转换为JSON
      String messageJson = objectMapper.writeValueAsString(response);

      // 3. 广播到房间
      String roomId = String.valueOf(message.getRoomId());
      webSocketManager.broadcastToRoom(roomId, messageJson);

      log.info("Process danmaku message success: {}", message);
    } catch (Exception e) {
      log.error("Process danmaku message failed: {}", message, e);
      // TODO: 考虑重试机制
    }
  }
}