package com.spud.barrage.push.manager;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * WebSocket会话管理器
 * 负责管理所有WebSocket连接
 *
 * @author Spud
 * @date 2025/3/30
 */
@Slf4j
@Component
public class WebSocketSessionManager {

  // 按类型、房间和会话ID组织的会话存储
  // Map<类型, Map<房间ID, Map<会话ID, 会话>>>
  private final Map<String, Map<Long, Map<String, WebSocketSession>>> sessionStore =
      new ConcurrentHashMap<>();

  // 用户会话映射: 用户ID -> 会话ID集合
  private final Map<Long, Set<String>> userSessionsMap =
      new ConcurrentHashMap<>();

  // 会话用户映射: 会话ID -> 用户ID
  private final Map<String, Long> sessionUserMap =
      new ConcurrentHashMap<>();

  // 会话房间映射: 会话ID -> 房间ID
  private final Map<String, Long> sessionRoomMap =
      new ConcurrentHashMap<>();

  // 会话类型映射: 会话ID -> 类型
  private final Map<String, String> sessionTypeMap =
      new ConcurrentHashMap<>();

  /**
   * 注册会话
   */
  public void registerSession(String type, Long roomId, Long userId, WebSocketSession session) {
    String sessionId = session.getId();

    // 添加到会话存储
    sessionStore.getOrDefault(type, new ConcurrentHashMap<>())
        .getOrDefault(roomId, new ConcurrentHashMap<>())
        .put(sessionId, session);

    // 添加到用户会话映射
    userSessionsMap.compute(userId, (k, v) -> {
      Set<String> sessions = (v != null) ? v : ConcurrentHashMap.newKeySet();
      sessions.add(sessionId);
      return sessions;
    });

    // 添加到反向映射
    sessionUserMap.put(sessionId, userId);
    sessionRoomMap.put(sessionId, roomId);
    sessionTypeMap.put(sessionId, type);

    log.debug("注册会话: type={}, roomId={}, userId={}, sessionId={}", type, roomId, userId,
        sessionId);
  }

  /**
   * 取消注册会话
   */
  public void unregisterSession(String type, Long roomId, Long userId, WebSocketSession session) {
    String sessionId = session.getId();

    // 从会话存储中移除
    Map<Long, Map<String, WebSocketSession>> typeMap = sessionStore.get(type);
    if (typeMap != null) {
      Map<String, WebSocketSession> roomMap = typeMap.get(roomId);
      if (roomMap != null) {
        roomMap.remove(sessionId);

        // 如果房间没有会话了，移除房间
        if (roomMap.isEmpty()) {
          typeMap.remove(roomId);

          // 如果类型没有房间了，移除类型
          if (typeMap.isEmpty()) {
            sessionStore.remove(type);
          }
        }
      }
    }

    // 从用户会话映射中移除
    Set<String> userSessions = userSessionsMap.getOrDefault(userId, ConcurrentHashMap.newKeySet());
    if (userSessions != null) {
      userSessions.remove(sessionId);

      // 如果用户没有会话了，移除用户
      if (userSessions.isEmpty()) {
        userSessionsMap.remove(userId);
      }
    }

    // 从反向映射中移除
    sessionUserMap.remove(sessionId);
    sessionRoomMap.remove(sessionId);
    sessionTypeMap.remove(sessionId);

    log.debug("取消注册会话: type={}, roomId={}, userId={}, sessionId={}", type, roomId, userId,
        sessionId);
  }

  /**
   * 向指定类型的房间内所有会话广播消息
   */
  public void broadcastToRoom(String type, Long roomId, String message) {
    Map<Long, Map<String, WebSocketSession>> typeMap = sessionStore.getOrDefault(type,
        new ConcurrentHashMap<>());
    if (typeMap == null) {
      return;
    }

    Map<String, WebSocketSession> roomMap = typeMap.get(roomId);
    if (roomMap == null || roomMap.isEmpty()) {
      return;
    }

    TextMessage textMessage = new TextMessage(message);

    // 广播消息
    for (WebSocketSession session : roomMap.values()) {
      try {
        if (session.isOpen()) {
          session.sendMessage(textMessage);
        }
      } catch (IOException e) {
        log.error("发送消息失败: sessionId={}, roomId={}, type={}", session.getId(), roomId, type,
            e);
      }
    }
  }

  /**
   * 向指定用户的所有会话发送消息
   */
  public void sendToUser(String userId, String message) {
    Set<String> sessionIds = userSessionsMap.getOrDefault(userId, Collections.emptySet());

    if (sessionIds.isEmpty()) {
      return;
    }

    TextMessage textMessage = new TextMessage(message);

    for (String sessionId : sessionIds) {
      String type = sessionTypeMap.get(sessionId);
      Long roomId = sessionRoomMap.get(sessionId);

      if (type == null || roomId == null) {
        continue;
      }

      Map<String, WebSocketSession> roomMap = sessionStore
          .getOrDefault(type, Collections.emptyMap())
          .getOrDefault(roomId, Collections.emptyMap());

      WebSocketSession session = roomMap.get(sessionId);

      if (session != null && session.isOpen()) {
        try {
          session.sendMessage(textMessage);
        } catch (IOException e) {
          log.error("发送消息给用户失败: userId={}, sessionId={}", userId, sessionId, e);
        }
      }
    }
  }

  /**
   * 获取房间的会话数量
   */
  public int getRoomSessionCount(String type, Long roomId) {
    return sessionStore
        .getOrDefault(type, Collections.emptyMap())
        .getOrDefault(roomId, Collections.emptyMap())
        .size();
  }

  /**
   * 获取用户的会话数量
   */
  public int getUserSessionCount(String userId) {
    return userSessionsMap.getOrDefault(userId, Collections.emptySet()).size();
  }

  /**
   * 获取所有房间ID
   */
  public Set<Long> getRoomIds(String type) {
    return sessionStore
        .getOrDefault(type, Collections.emptyMap())
        .keySet();
  }

  /**
   * 获取房间内的所有用户ID
   */
  public Set<Long> getRoomUsers(String type, String roomId) {
    Map<String, WebSocketSession> roomMap = sessionStore
        .getOrDefault(type, Collections.emptyMap())
        .getOrDefault(roomId, Collections.emptyMap());

    Set<Long> userIds = ConcurrentHashMap.newKeySet();
    for (String sessionId : roomMap.keySet()) {
      Long userId = sessionUserMap.get(sessionId);
      if (userId != null) {
        userIds.add(userId);
      }
    }

    return userIds;
  }
}