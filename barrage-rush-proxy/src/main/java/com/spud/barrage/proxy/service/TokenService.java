package com.spud.barrage.proxy.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 令牌服务
 * 负责生成和验证WebSocket连接的认证令牌
 *
 * @author Spud
 * @date 2025/3/30
 */
@Slf4j
@Service
public class TokenService {

  @Value("${barrage.token.secret:defaultSecretKeyForDevelopmentEnvironmentOnly}")
  private String secret;

  @Value("${barrage.token.ttl:3600}")
  private long tokenTtl;

  /**
   * 签名密钥
   */
  private Key key;

  /**
   * 初始化签名密钥
   */
  public void init() {
    this.key = Keys.hmacShaKeyFor(secret.getBytes());
  }

  /**
   * 生成令牌
   *
   * @param roomId 房间ID
   * @param userId 用户ID
   * @return 令牌
   */
  public String generateToken(String roomId, String userId) {
    if (key == null) {
      init();
    }

    Date now = new Date();
    Date expiration = new Date(now.getTime() + tokenTtl * 1000);

    Map<String, Object> claims = new HashMap<>();
    claims.put("roomId", roomId);
    claims.put("userId", userId);

    return Jwts.builder()
        .setClaims(claims)
        .setSubject(userId)
        .setIssuedAt(now)
        .setExpiration(expiration)
        .signWith(key, SignatureAlgorithm.HS256)
        .compact();
  }

  /**
   * 验证令牌
   *
   * @param token  令牌
   * @param roomId 房间ID（用于验证）
   * @return 用户ID，如果令牌无效则返回null
   */
  public String validateToken(String token, String roomId) {
    if (key == null) {
      init();
    }

    try {
      Claims claims = Jwts.parserBuilder()
          .setSigningKey(key)
          .build()
          .parseClaimsJws(token)
          .getBody();

      // 验证房间ID
      String tokenRoomId = claims.get("roomId", String.class);
      if (!roomId.equals(tokenRoomId)) {
        log.warn("令牌房间ID不匹配: expected={}, actual={}", roomId, tokenRoomId);
        return null;
      }

      return claims.getSubject();
    } catch (Exception e) {
      log.warn("令牌验证失败: {}", e.getMessage());
      return null;
    }
  }

  /**
   * 获取令牌有效期（秒）
   */
  public long getTokenTtl() {
    return tokenTtl;
  }
}