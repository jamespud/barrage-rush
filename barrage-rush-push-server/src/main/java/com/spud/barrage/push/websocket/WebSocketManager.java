package com.spud.barrage.push.websocket;

import com.spud.barrage.constant.ApiConstants;
import com.spud.barrage.constant.Constants;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * @author Spud
 * @date 2025/3/4
 */
@Slf4j
@Component
public class WebSocketManager {

  private final Map<String, ConcurrentHashMap<String, WebSocketSession>> roomSessions = new ConcurrentHashMap<>();
  private final Map<String, Long> sessionHeartbeats = new ConcurrentHashMap<>();

  @Autowired
  private RedisTemplate<String, Object> redisTemplate;

  public void addSession(String roomId, String userId, WebSocketSession session) {
    // 1. 添加到内存
    roomSessions.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>())
        .put(userId, session);

    // 2. 添加到Redis
    String roomKey = String.format(ApiConstants.REDIS_ROOM_USERS, roomId);
    redisTemplate.opsForSet().add(roomKey, userId);
    redisTemplate.expire(roomKey, Constants.USER_SESSION_EXPIRE, TimeUnit.SECONDS);

    // 3. 初始化心跳时间
    sessionHeartbeats.put(session.getId(), System.currentTimeMillis());
  }

  public void removeSession(String roomId, String userId) {
    // 1. 从内存移除
    ConcurrentHashMap<String, WebSocketSession> sessions = roomSessions.get(roomId);
    if (sessions != null) {
      WebSocketSession session = sessions.remove(userId);
      if (session != null) {
        sessionHeartbeats.remove(session.getId());
      }
      if (sessions.isEmpty()) {
        roomSessions.remove(roomId);
      }
    }

    // 2. 从Redis移除
    String roomKey = String.format(ApiConstants.REDIS_ROOM_USERS, roomId);
    redisTemplate.opsForSet().remove(roomKey, userId);
  }

  public void broadcastToRoom(String roomId, String message) {
    ConcurrentHashMap<String, WebSocketSession> sessions = roomSessions.get(roomId);
    if (sessions != null) {
      sessions.values().forEach(session -> {
        try {
          if (session.isOpen()) {
            session.sendMessage(new TextMessage(message));
          }
        } catch (IOException e) {
          log.error("Broadcast message error", e);
        }
      });
    }
  }

  public void updateHeartbeat(String sessionId, Long timestamp) {
    sessionHeartbeats.put(sessionId, timestamp);
  }

  @Scheduled(fixedRate = 30000)  // 每30秒检查一次
  public void checkHeartbeats() {
    long now = System.currentTimeMillis();
    sessionHeartbeats.forEach((sessionId, lastHeartbeat) -> {
      if (now - lastHeartbeat > Constants.HEARTBEAT_TIMEOUT * 1000) {
        closeExpiredSession(sessionId);
      }
    });
  }

  private void closeExpiredSession(String sessionId) {
    roomSessions.values().forEach(sessions ->
        sessions.forEach((userId, session) -> {
          if (session.getId().equals(sessionId)) {
            try {
              session.close();
            } catch (IOException e) {
              log.error("Close expired session error", e);
            }
          }
        })
    );
  }
}