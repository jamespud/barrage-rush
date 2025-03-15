package com.spud.barrage.common.mq.mange;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spud.barrage.common.data.dto.DanmakuMessage;
import com.spud.barrage.common.data.dto.DanmakuRequest;
import com.spud.barrage.common.data.dto.HeartbeatMessage;
import com.spud.barrage.common.data.dto.WebSocketRequest;
import com.spud.barrage.common.data.dto.WebSocketResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * @author Spud
 * @date 2025/3/4
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DanmakuWebSocketHandler extends TextWebSocketHandler {

  @Autowired
  private DanmakuService danmakuService;

  @Autowired
  private WebSocketManager webSocketManager;

  @Autowired
  private ObjectMapper objectMapper;

  @Override
  public void afterConnectionEstablished(WebSocketSession session) {
    try {
      String roomId = getRoomIdFromSession(session);
      String userId = getUserIdFromSession(session);

      // 加入房间
      webSocketManager.addSession(roomId, userId, session);

      // 发送最近消息
      sendRecentMessages(session, roomId);

      log.info("WebSocket connected, roomId: {}, userId: {}", roomId, userId);

      WebSocketResponse<String> response = new WebSocketResponse<>("CONNECTED", "连接成功");
      session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
    } catch (Exception e) {
      log.error("WebSocket connection failed", e);
      closeSession(session, "连接失败");
    }
  }

  @Override
  protected void handleTextMessage(WebSocketSession session, TextMessage message) {
    try {
      // 1. 解析消息类型
      WebSocketRequest request = objectMapper.readValue(message.getPayload(),
          WebSocketRequest.class);

      switch (request.getType()) {
        case "DANMAKU":
          handleDanmakuMessage(session, request.getData());
          break;
        case "HEARTBEAT":
          handleHeartbeat(session, request.getData());
          break;
        default:
          log.warn("Unknown message type: {}", request.getType());
      }
    } catch (Exception e) {
      log.error("Handle message error", e);
      sendErrorMessage(session, "消息处理失败");
    }
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
    Long roomId = getRoomIdFromSession(session);
    Long userId = getUserIdFromSession(session);

    webSocketManager.removeSession(roomId, userId);
    log.info("WebSocket closed, roomId: {}, userId: {}, status: {}", roomId, userId, status);
  }

  private void handleDanmakuMessage(WebSocketSession session, String data) throws Exception {
    // 1. 解析弹幕请求
    DanmakuRequest request = objectMapper.readValue(data, DanmakuRequest.class);

    // 2. 获取用户信息
    String userId = getUserIdFromSession(session);

    // 3. 构建弹幕消息
    DanmakuMessage message = buildDanmakuMessage(request, userId);

    // 4. 处理弹幕
    danmakuService.processDanmaku(message);
  }

  private void handleHeartbeat(WebSocketSession session, String data) throws Exception {
    HeartbeatMessage heartbeat = objectMapper.readValue(data, HeartbeatMessage.class);
    webSocketManager.updateHeartbeat(session.getId(), heartbeat.getTimestamp());
  }

  private DanmakuMessage buildDanmakuMessage(DanmakuRequest request, String userId) {
    return DanmakuMessage.builder()
        .roomId(request.getRoomId())
        .userId(Long.valueOf(userId))
        .content(request.getContent())
        .type(request.getType())
        .style(request.getStyle().toString())
        .timestamp(System.currentTimeMillis())
        .build();
  }

  private void sendRecentMessages(WebSocketSession session, Long roomId) {
    try {
      List<DanmakuMessage> recentDanmaku = danmakuService.getRecentDanmaku(roomId, 0L, 50);
      if (!CollectionUtils.isEmpty(recentDanmaku)) {
        WebSocketResponse<Object> response = new WebSocketResponse<>("HISTORY", recentDanmaku);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
      }
    } catch (Exception e) {
      log.error("Send recent messages failed", e);
    }
  }

  private void sendErrorMessage(WebSocketSession session, String message) {
    try {
      WebSocketResponse<String> response = new WebSocketResponse<>("ERROR", message);
      session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
    } catch (Exception e) {
      log.error("Send error message failed", e);
    }
  }

  private Long getUserIdFromSession(WebSocketSession session) {
    return (Long) session.getAttributes().get("userId");
  }

  private Long getRoomIdFromSession(WebSocketSession session) {
    return (Long) session.getAttributes().get("roomId");
  }

  private void closeSession(WebSocketSession session, String reason) {
    try {
      session.close(CloseStatus.SERVER_ERROR);
    } catch (Exception e) {
      log.error("Close session failed", e);
    }
  }
}