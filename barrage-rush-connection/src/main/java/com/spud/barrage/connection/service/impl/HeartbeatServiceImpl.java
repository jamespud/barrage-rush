package com.spud.barrage.connection.service.impl;

import com.spud.barrage.common.data.dto.HeartbeatMessage;
import com.spud.barrage.connection.config.ConnectionProperties;
import com.spud.barrage.connection.model.UserSession;
import com.spud.barrage.connection.service.HeartbeatService;
import com.spud.barrage.connection.service.SessionService;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 心跳服务实现
 *
 * @author Spud
 * @date 2025/3/15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HeartbeatServiceImpl implements HeartbeatService {

  private final RedisTemplate<String, Object> redisTemplate;
  private final ConnectionProperties properties;
  private final SessionService sessionService;

  @Override
  public boolean updateHeartbeat(String sessionId, HeartbeatMessage message) {
    if (sessionId == null || message == null) {
      return false;
    }

    try {
      String key = getHeartbeatKey(sessionId);
      redisTemplate.opsForValue()
          .set(key, message, properties.getRedis().getHeartbeatExpire(), TimeUnit.SECONDS);

      // 更新会话最后活跃时间
      sessionService.updateLastActiveTime(sessionId);

      return true;
    } catch (Exception e) {
      log.error("更新心跳失败: {}", e.getMessage(), e);
      return false;
    }
  }

  @Override
  public boolean isHeartbeatValid(String sessionId) {
    HeartbeatMessage heartbeat = getLastHeartbeat(sessionId);
    if (heartbeat == null) {
      return false;
    }

    // 检查心跳是否超时
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime timestamp = heartbeat.getTimestamp();
    long millisElapsed = ChronoUnit.MILLIS.between(timestamp, now);

    return millisElapsed < properties.getHeartbeatTimeout();
  }

  @Override
  public boolean deleteHeartbeat(String sessionId) {
    String key = getHeartbeatKey(sessionId);
    return Boolean.TRUE.equals(redisTemplate.delete(key));
  }

  @Override
  public HeartbeatMessage getLastHeartbeat(String sessionId) {
    String key = getHeartbeatKey(sessionId);
    return (HeartbeatMessage) redisTemplate.opsForValue().get(key);
  }

  @Override
  public HeartbeatMessage processHeartbeat(String sessionId, HeartbeatMessage message) {
    if (message == null) {
      return null;
    }

    // 更新心跳
    updateHeartbeat(sessionId, message);

    // 获取会话信息
    UserSession session = sessionService.getSession(sessionId);
    if (session == null) {
      log.warn("处理心跳消息失败: 会话不存在, sessionId={}", sessionId);
      return HeartbeatMessage.reconnect(message.getRoomId(), message.getUserId(), sessionId,
          "会话不存在");
    }

    // 检查心跳类型
    if ("PING".equals(message.getType())) {
      // 响应PONG
      return HeartbeatMessage.pong(
          session.getRoomId(),
          session.getUserId(),
          sessionId,
          properties.getServerId()
      );
    }

    return null;
  }

  /**
   * 获取心跳键
   */
  private String getHeartbeatKey(String sessionId) {
    return properties.getRedis().getHeartbeatKeyPrefix() + sessionId;
  }
} 