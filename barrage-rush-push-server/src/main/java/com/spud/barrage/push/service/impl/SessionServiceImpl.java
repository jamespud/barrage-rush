package com.spud.barrage.push.service.impl;

import com.spud.barrage.push.config.ConnectionProperties;
import com.spud.barrage.push.model.UserSession;
import com.spud.barrage.push.service.SessionService;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 会话服务实现
 *
 * @author Spud
 * @date 2025/3/15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

  private final RedisTemplate<String, Object> redisTemplate;
  private final ConnectionProperties properties;

  @Override
  public UserSession createSession(Long userId, Long roomId, String nickname, String avatar,
      String ip, String location) {
    String sessionId = generateSessionId();

    UserSession session = UserSession.builder()
        .sessionId(sessionId)
        .userId(userId)
        .roomId(roomId)
        .nickname(nickname)
        .avatar(avatar)
        .ip(ip)
        .location(location)
        .connectTime(LocalDateTime.now())
        .lastActiveTime(LocalDateTime.now())
        .serverId(properties.getServerId())
        .online(true)
        .build();

    String key = getUserSessionKey(sessionId);
    redisTemplate.opsForValue()
        .set(key, session, properties.getRedis().getUserSessionExpire(), TimeUnit.SECONDS);

    // 增加房间在线人数
    incrementRoomOnlineCount(roomId);

    log.info("创建用户会话: sessionId={}, userId={}, roomId={}, nickname={}", sessionId, userId,
        roomId, nickname);
    return session;
  }

  @Override
  public UserSession getSession(String sessionId) {
    String key = getUserSessionKey(sessionId);
    return (UserSession) redisTemplate.opsForValue().get(key);
  }

  @Override
  public boolean updateSession(UserSession session) {
    String key = getUserSessionKey(session.getSessionId());
    session.updateLastActiveTime();
    redisTemplate.opsForValue()
        .set(key, session, properties.getRedis().getUserSessionExpire(), TimeUnit.SECONDS);
    return true;
  }

  @Override
  public boolean deleteSession(String sessionId) {
    UserSession session = getSession(sessionId);
    if (session != null) {
      String key = getUserSessionKey(sessionId);
      redisTemplate.delete(key);

      // 减少房间在线人数
      decrementRoomOnlineCount(session.getRoomId());

      log.info("删除用户会话: sessionId={}, userId={}, roomId={}", sessionId, session.getUserId(),
          session.getRoomId());
      return true;
    }
    return false;
  }

  @Override
  public boolean setDataSessionId(String sessionId, String dataSessionId) {
    UserSession session = getSession(sessionId);
    if (session != null) {
      session.setDataSessionId(dataSessionId);
      return updateSession(session);
    }
    return false;
  }

  @Override
  public boolean setHeartbeatSessionId(String sessionId, String heartbeatSessionId) {
    UserSession session = getSession(sessionId);
    if (session != null) {
      session.setHeartbeatSessionId(heartbeatSessionId);
      return updateSession(session);
    }
    return false;
  }

  @Override
  public boolean updateLastActiveTime(String sessionId) {
    UserSession session = getSession(sessionId);
    if (session != null) {
      session.updateLastActiveTime();
      return updateSession(session);
    }
    return false;
  }

  @Override
  public boolean setOnlineStatus(String sessionId, boolean online) {
    UserSession session = getSession(sessionId);
    if (session != null) {
      session.setOnline(online);
      return updateSession(session);
    }
    return false;
  }

  @Override
  public long getRoomOnlineCount(Long roomId) {
    String key = getRoomOnlineKey(roomId);
    Object count = redisTemplate.opsForValue().get(key);
    return count != null ? Long.parseLong(count.toString()) : 0;
  }

  @Override
  public long incrementRoomOnlineCount(Long roomId) {
    String key = getRoomOnlineKey(roomId);
    Long count = redisTemplate.opsForValue().increment(key);
    return count != null ? count : 0;
  }

  @Override
  public long decrementRoomOnlineCount(Long roomId) {
    String key = getRoomOnlineKey(roomId);
    Long count = redisTemplate.opsForValue().decrement(key);
    if (count != null && count < 0) {
      redisTemplate.opsForValue().set(key, 0);
      return 0;
    }
    return count != null ? count : 0;
  }

  /**
   * 生成会话ID
   */
  private String generateSessionId() {
    return UUID.randomUUID().toString().replace("-", "");
  }

  /**
   * 获取用户会话键
   */
  private String getUserSessionKey(String sessionId) {
    return properties.getRedis().getUserSessionKeyPrefix() + sessionId;
  }

  /**
   * 获取房间在线人数键
   */
  private String getRoomOnlineKey(Long roomId) {
    return properties.getRedis().getRoomOnlineKeyPrefix() + roomId;
  }
} 