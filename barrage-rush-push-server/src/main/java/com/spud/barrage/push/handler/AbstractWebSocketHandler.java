package com.spud.barrage.push.handler;

import com.spud.barrage.push.manager.WebSocketSessionManager;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * WebSocket处理器基类
 * 提供公共的WebSocket会话处理逻辑
 *
 * @author Spud
 * @date 2025/3/30
 */
@Slf4j
public abstract class AbstractWebSocketHandler extends TextWebSocketHandler {

  @Autowired
  protected WebSocketSessionManager sessionManager;

  @Override
  public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    Long roomId = getRoomId(session);
    Long userId = getUserId(session);
    String sessionId = session.getId();

    log.debug("[{}] 新的WebSocket连接已建立: sessionId={}, roomId={}, userId={}",
        getHandlerName(), sessionId, roomId, userId);

    // 注册会话
    sessionManager.registerSession(getHandlerType(), roomId, userId, session);

    // 处理连接建立
    handleConnectionEstablished(session);
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
    Long roomId = getRoomId(session);
    Long userId = getUserId(session);
    String sessionId = session.getId();

    log.debug("[{}] WebSocket连接已关闭: sessionId={}, roomId={}, userId={}, status={}",
        getHandlerName(), sessionId, roomId, userId, status);

    // 取消注册会话
    sessionManager.unregisterSession(getHandlerType(), roomId, userId, session);

    // 处理连接关闭
    handleConnectionClosed(session, status);
  }

  @Override
  public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
    Long roomId = getRoomId(session);
    Long userId = getUserId(session);
    String sessionId = session.getId();

    log.error("[{}] WebSocket传输错误: sessionId={}, roomId={}, userId={}",
        getHandlerName(), sessionId, roomId, userId, exception);

    // 处理错误
    handleError(session, exception);
  }

  /**
   * 向会话发送消息
   */
  protected void sendMessage(WebSocketSession session, String message) {
    try {
      if (session.isOpen()) {
        session.sendMessage(new TextMessage(message));
      }
    } catch (IOException e) {
      log.error("[{}] 发送消息失败: sessionId={}", getHandlerName(), session.getId(), e);
    }
  }

  /**
   * 获取房间ID
   */
  protected Long getRoomId(WebSocketSession session) {
    return (Long) session.getAttributes().get("roomId");
  }

  /**
   * 获取用户ID
   */
  protected Long getUserId(WebSocketSession session) {
    return (Long) session.getAttributes().get("userId");
  }

  /**
   * 获取处理器名称
   */
  protected abstract String getHandlerName();

  /**
   * 获取处理器类型
   */
  protected abstract String getHandlerType();

  /**
   * 处理连接建立
   */
  protected abstract void handleConnectionEstablished(WebSocketSession session) throws Exception;

  /**
   * 处理连接关闭
   */
  protected abstract void handleConnectionClosed(WebSocketSession session, CloseStatus status)
      throws Exception;

  /**
   * 处理传输错误
   */
  protected abstract void handleError(WebSocketSession session, Throwable exception)
      throws Exception;
}