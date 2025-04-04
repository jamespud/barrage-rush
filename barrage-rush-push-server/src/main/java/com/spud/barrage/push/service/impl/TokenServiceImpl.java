package com.spud.barrage.push.service.impl;

import com.spud.barrage.push.service.TokenService;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 令牌服务实现类
 *
 * @author Spud
 * @date 2025/3/30
 */
@Slf4j
@Service
public class TokenServiceImpl implements TokenService {

  private static final String TOKEN_KEY_PREFIX = "token:";

  @Autowired
  private RedisTemplate<String, Object> redisTemplate;

  @Value("${push.token.default-expire-minutes:60}")
  private int defaultExpireMinutes;

  @Value("${push.token.max-expire-minutes:1440}")
  private int maxExpireMinutes;

  @Override
  public Map<String, Object> verifyToken(String token) {
    if (token == null || token.isEmpty()) {
      log.warn("[令牌] 令牌为空");
      return null;
    }

    String redisKey = TOKEN_KEY_PREFIX + token;
    Object tokenData = redisTemplate.opsForValue().get(redisKey);

    if (tokenData == null) {
      log.warn("[令牌] 令牌不存在或已过期: {}", token);
      return null;
    }

    if (!(tokenData instanceof Map)) {
      log.error("[令牌] 令牌数据格式错误: {}", token);
      return null;
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> tokenInfo = (Map<String, Object>) tokenData;

    // 检查是否过期（双重检查，Redis已有过期机制，这里是额外检查）
    Long expireAt = (Long) tokenInfo.get("expireAt");
    if (expireAt != null && expireAt < System.currentTimeMillis()) {
      log.warn("[令牌] 令牌已过期: token={}, expireAt={}", token, expireAt);
      redisTemplate.delete(redisKey);
      return null;
    }

    log.debug("[令牌] 令牌验证成功: token={}, userId={}",
        token, tokenInfo.get("userId"));

    return tokenInfo;
  }

  @Override
  public Map<String, Object> generateToken(String userId, String roomId, int expireMinutes) {
    // 验证过期时间
    if (expireMinutes <= 0) {
      expireMinutes = defaultExpireMinutes;
    } else if (expireMinutes > maxExpireMinutes) {
      expireMinutes = maxExpireMinutes;
    }

    // 生成唯一令牌
    String token = generateUniqueToken();

    // 计算过期时间
    long expireAt = System.currentTimeMillis() + expireMinutes * 60 * 1000L;

    // 创建令牌信息
    Map<String, Object> tokenInfo = new HashMap<>();
    tokenInfo.put("token", token);
    tokenInfo.put("userId", userId);
    tokenInfo.put("roomId", roomId);
    tokenInfo.put("createdAt", System.currentTimeMillis());
    tokenInfo.put("expireAt", expireAt);

    // 存储令牌
    String redisKey = TOKEN_KEY_PREFIX + token;
    redisTemplate.opsForValue().set(redisKey, tokenInfo, expireMinutes, TimeUnit.MINUTES);

    log.info("[令牌] 生成新令牌: token={}, userId={}, roomId={}, expireMinutes={}",
        token, userId, roomId, expireMinutes);

    return tokenInfo;
  }

  @Override
  public boolean revokeToken(String token) {
    if (token == null || token.isEmpty()) {
      return false;
    }

    String redisKey = TOKEN_KEY_PREFIX + token;
    Boolean deleted = redisTemplate.delete(redisKey);

    if (Boolean.TRUE.equals(deleted)) {
      log.info("[令牌] 令牌已吊销: {}", token);
      return true;
    } else {
      log.warn("[令牌] 令牌吊销失败，令牌不存在: {}", token);
      return false;
    }
  }

  /**
   * 生成唯一令牌
   */
  private String generateUniqueToken() {
    return UUID.randomUUID().toString().replace("-", "");
  }
}