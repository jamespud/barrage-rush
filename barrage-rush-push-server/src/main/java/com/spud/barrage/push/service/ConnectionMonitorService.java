package com.spud.barrage.push.service;

import com.spud.barrage.push.config.ConnectionProperties;
import com.spud.barrage.push.model.UserSession;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 连接监控服务
 *
 * @author Spud
 * @date 2025/3/15
 */
@Slf4j
@Service
@EnableScheduling
@RequiredArgsConstructor
public class ConnectionMonitorService {

  private final SessionService sessionService;
  private final HeartbeatService heartbeatService;
  private final RedisTemplate<String, Object> redisTemplate;
  private final ConnectionProperties properties;
  private final Map<Long, Integer> roomConnectionCounts = new HashMap<>();
  // 连接统计信息
  private int totalConnections = 0;
  private int activeConnections = 0;
  private int inactiveConnections = 0;

  /**
   * 定期清理过期会话
   */
  @Scheduled(fixedRate = 60000) // 每分钟执行一次
  public void cleanupExpiredSessions() {
    log.info("开始清理过期会话");

    try {
      // 获取所有会话键
      String sessionKeyPattern = properties.getRedis().getUserSessionKeyPrefix() + "*";
      var keys = redisTemplate.keys(sessionKeyPattern);

      if (keys == null || keys.isEmpty()) {
        log.info("没有找到会话");
        return;
      }

      int expiredCount = 0;
      int totalCount = keys.size();

      for (String key : keys) {
        String sessionId = key.substring(properties.getRedis().getUserSessionKeyPrefix().length());
        UserSession session = sessionService.getSession(sessionId);

        if (session != null) {
          // 检查会话是否过期
          LocalDateTime now = LocalDateTime.now();
          long inactiveSeconds = ChronoUnit.SECONDS.between(session.getLastActiveTime(), now);

          // 如果超过会话过期时间的一半且心跳无效，则认为会话过期
          if (inactiveSeconds > properties.getRedis().getUserSessionExpire() / 2
              && !heartbeatService.isHeartbeatValid(sessionId)) {
            log.info("删除过期会话: sessionId={}, userId={}, roomId={}, 不活跃时间={}秒",
                sessionId, session.getUserId(), session.getRoomId(), inactiveSeconds);

            // 删除会话
            sessionService.deleteSession(sessionId);
            expiredCount++;
          }
        }
      }

      log.info("清理过期会话完成: 总会话数={}, 过期会话数={}", totalCount, expiredCount);
    } catch (Exception e) {
      log.error("清理过期会话失败: {}", e.getMessage(), e);
    }
  }

  /**
   * 定期更新连接统计信息
   */
  @Scheduled(fixedRate = 30000) // 每30秒执行一次
  public void updateConnectionStats() {
    try {
      // 获取所有会话键
      String sessionKeyPattern = properties.getRedis().getUserSessionKeyPrefix() + "*";
      var keys = redisTemplate.keys(sessionKeyPattern);

      if (keys == null) {
        totalConnections = 0;
        activeConnections = 0;
        inactiveConnections = 0;
        roomConnectionCounts.clear();
        return;
      }

      totalConnections = keys.size();
      activeConnections = 0;
      inactiveConnections = 0;
      roomConnectionCounts.clear();

      for (String key : keys) {
        String sessionId = key.substring(properties.getRedis().getUserSessionKeyPrefix().length());
        UserSession session = sessionService.getSession(sessionId);

        if (session != null) {
          // 检查会话是否活跃
          if (session.getOnline() != null && session.getOnline()) {
            activeConnections++;
          } else {
            inactiveConnections++;
          }

          // 更新房间连接数
          Long roomId = session.getRoomId();
          if (roomId != null) {
            roomConnectionCounts.put(roomId, roomConnectionCounts.getOrDefault(roomId, 0) + 1);
          }
        }
      }

      log.info("连接统计: 总连接数={}, 活跃连接数={}, 不活跃连接数={}, 房间数={}",
          totalConnections, activeConnections, inactiveConnections, roomConnectionCounts.size());
    } catch (Exception e) {
      log.error("更新连接统计信息失败: {}", e.getMessage(), e);
    }
  }

  /**
   * 获取连接统计信息
   */
  public Map<String, Object> getConnectionStats() {
    Map<String, Object> stats = new HashMap<>();

    stats.put("totalConnections", totalConnections);
    stats.put("activeConnections", activeConnections);
    stats.put("inactiveConnections", inactiveConnections);
    stats.put("roomCount", roomConnectionCounts.size());
    stats.put("roomConnections", roomConnectionCounts);

    return stats;
  }
} 