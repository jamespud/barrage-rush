package com.spud.barrage.push.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spud.barrage.common.data.dto.DanmakuMessage;
import com.spud.barrage.push.constant.WebSocketType;
import com.spud.barrage.push.service.MessageService;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * 弹幕WebSocket处理器
 * 处理弹幕相关的WebSocket消息
 *
 * @author Spud
 * @date 2025/3/30
 */
@Slf4j
@Component
public class DanmakuWebSocketHandler extends AbstractWebSocketHandler {

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private MessageService messageService;

  @Override
  protected String getHandlerName() {
    return "弹幕处理器";
  }

  @Override
  protected String getHandlerType() {
    return WebSocketType.DANMAKU;
  }

  @Override
  public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
    Long roomId = getRoomId(session);
    Long userId = getUserId(session);
    String payload = message.getPayload();

    log.debug("[弹幕] 收到消息: roomId={}, userId={}, payload={}", roomId, userId, payload);

    try {
      // 解析消息
      Map<String, Object> msgData = objectMapper.readValue(payload, Map.class);
      String type = (String) msgData.getOrDefault("type", "");

      // 处理消息
      switch (type) {
        case "PING":
          // 响应心跳检测
          sendPong(session);
          break;
        case "DANMAKU":
          // 处理弹幕消息，这里实际上通常通过HTTP接口发送弹幕
          // 这里主要用于客户端紧急情况下通过WebSocket发送弹幕
          handleDanmakuMessage(session, msgData);
          break;
        default:
          log.warn("[弹幕] 未知消息类型: type={}, roomId={}, userId={}", type, roomId, userId);
          break;
      }
    } catch (Exception e) {
      log.error("[弹幕] 处理消息异常: roomId={}, userId={}", roomId, userId, e);
      sendError(session, "消息格式错误: " + e.getMessage());
    }
  }

  /**
   * 处理弹幕消息
   */
  private void handleDanmakuMessage(WebSocketSession session, Map<String, Object> msgData) {
    Long roomId = getRoomId(session);
    Long userId = getUserId(session);

    try {
      // 提取弹幕数据
      Map<String, Object> data = (Map<String, Object>) msgData.get("data");
      if (data == null) {
        sendError(session, "消息缺少data字段");
        return;
      }

      // 构建弹幕消息
      DanmakuMessage danmakuMessage = messageService.createDanmakuMessage(roomId, userId, data);

      // 发布消息
      messageService.publishDanmakuMessage(danmakuMessage);

      // 发送确认消息
      sendAck(session, danmakuMessage.getId());
    } catch (Exception e) {
      log.error("[弹幕] 处理弹幕消息异常: roomId={}, userId={}", roomId, userId, e);
      sendError(session, "处理弹幕失败: " + e.getMessage());
    }
  }

  /**
   * 发送PONG响应
   */
  private void sendPong(WebSocketSession session) throws IOException {
    Map<String, Object> pong = new HashMap<>();
    pong.put("type", "PONG");
    pong.put("timestamp", System.currentTimeMillis());

    String pongMessage = objectMapper.writeValueAsString(pong);
    sendMessage(session, pongMessage);
  }

  /**
   * 发送确认消息
   */
  private void sendAck(WebSocketSession session, Long messageId) throws IOException {
    Map<String, Object> ack = new HashMap<>();
    ack.put("type", "ACK");
    ack.put("messageId", messageId);
    ack.put("timestamp", System.currentTimeMillis());

    String ackMessage = objectMapper.writeValueAsString(ack);
    sendMessage(session, ackMessage);
  }

  /**
   * 发送错误消息
   */
  private void sendError(WebSocketSession session, String errorMessage) {
    try {
      Map<String, Object> error = new HashMap<>();
      error.put("type", "ERROR");
      error.put("message", errorMessage);
      error.put("timestamp", System.currentTimeMillis());

      String errorJson = objectMapper.writeValueAsString(error);
      sendMessage(session, errorJson);
    } catch (IOException e) {
      log.error("[弹幕] 发送错误消息失败: sessionId={}, error={}", session.getId(), errorMessage,
          e);
    }
  }

  @Override
  protected void handleConnectionEstablished(WebSocketSession session) throws Exception {
    Long roomId = getRoomId(session);

    // 发送最近的弹幕历史记录
    sendRecentMessages(session, roomId);
  }

  /**
   * 发送最近的弹幕历史记录
   */
  private void sendRecentMessages(WebSocketSession session, Long roomId) {
    try {
      // 获取最近的弹幕
      List<DanmakuMessage> recentMessages = messageService.getRecentMessages(roomId, 50);

      if (recentMessages != null && !recentMessages.isEmpty()) {
        // 构建历史消息
        Map<String, Object> history = new HashMap<>();
        history.put("type", "HISTORY");
        history.put("messages", recentMessages);
        history.put("timestamp", System.currentTimeMillis());

        String historyJson = objectMapper.writeValueAsString(history);
        sendMessage(session, historyJson);
      }
    } catch (Exception e) {
      log.error("[弹幕] 发送历史消息失败: roomId={}, sessionId={}", roomId, session.getId(), e);
    }
  }

  @Override
  protected void handleConnectionClosed(WebSocketSession session, CloseStatus status)
      throws Exception {
    // 连接关闭时的处理逻辑
  }

  @Override
  protected void handleError(WebSocketSession session, Throwable exception) throws Exception {
    // 错误处理逻辑
  }
}