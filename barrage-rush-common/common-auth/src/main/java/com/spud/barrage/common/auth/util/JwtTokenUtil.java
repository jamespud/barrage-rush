package com.spud.barrage.common.auth.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * JWT工具类
 *
 * @author Spud
 * @date 2023/4/1
 */
@Slf4j
public class JwtTokenUtil {

  /**
   * 密钥
   */
  private String secret;

  /**
   * 访问令牌过期时间
   */
  private long accessTokenExpiration;

  /**
   * 刷新令牌过期时间
   */
  private long refreshTokenExpiration;

  /**
   * 记住我过期时间
   */
  private long rememberMeExpiration;

  /**
   * 签名密钥
   */
  private Key key;

  /**
   * Token前缀
   */
  private static final String TOKEN_PREFIX = "Bearer ";

  /**
   * 初始化
   */
  @PostConstruct
  public void init() {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * 设置密钥
   */
  public void setSecret(String secret) {
    this.secret = secret;
    init();
  }

  /**
   * 设置访问令牌过期时间
   */
  public void setAccessTokenExpiration(long accessTokenExpiration) {
    this.accessTokenExpiration = accessTokenExpiration;
  }

  /**
   * 设置刷新令牌过期时间
   */
  public void setRefreshTokenExpiration(long refreshTokenExpiration) {
    this.refreshTokenExpiration = refreshTokenExpiration;
  }

  /**
   * 设置记住我过期时间
   */
  public void setRememberMeExpiration(long rememberMeExpiration) {
    this.rememberMeExpiration = rememberMeExpiration;
  }

  /**
   * 获取用户名
   */
  public String getUsernameFromToken(String token) {
    return getClaimFromToken(token, Claims::getSubject);
  }

  /**
   * 获取过期时间
   */
  public Date getExpirationDateFromToken(String token) {
    return getClaimFromToken(token, Claims::getExpiration);
  }

  /**
   * 获取用户ID
   */
  public Long getUserIdFromToken(String token) {
    return getClaimFromToken(token, claims -> {
      Integer userId = claims.get("userId", Integer.class);
      return userId != null ? userId.longValue() : null;
    });
  }

  /**
   * 获取令牌版本
   */
  public Long getTokenVersionFromToken(String token) {
    return getClaimFromToken(token, claims -> {
      Integer tokenVersion = claims.get("tokenVersion", Integer.class);
      return tokenVersion != null ? tokenVersion.longValue() : 0L;
    });
  }

  /**
   * 从token中获取指定类型的信息
   */
  public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = getAllClaimsFromToken(token);
    return claimsResolver.apply(claims);
  }

  /**
   * 解析token获取所有claims
   */
  private Claims getAllClaimsFromToken(String token) {
    return Jwts.parserBuilder()
        .setSigningKey(key)
        .build()
        .parseClaimsJws(token)
        .getBody();
  }

  /**
   * 检查token是否过期
   */
  private Boolean isTokenExpired(String token) {
    try {
      final Date expiration = getExpirationDateFromToken(token);
      return expiration.before(new Date());
    } catch (ExpiredJwtException e) {
      return true;
    }
  }

  /**
   * 生成访问令牌
   */
  public String generateAccessToken(UserDetails userDetails, Map<String, Object> claims) {
    return generateToken(claims, userDetails.getUsername(), accessTokenExpiration);
  }

  /**
   * 生成访问令牌（记住我）
   */
  public String generateAccessTokenWithRememberMe(UserDetails userDetails,
      Map<String, Object> claims) {
    return generateToken(claims, userDetails.getUsername(), rememberMeExpiration);
  }

  /**
   * 生成刷新令牌
   */
  public String generateRefreshToken(UserDetails userDetails, Map<String, Object> claims) {
    return generateToken(claims, userDetails.getUsername(), refreshTokenExpiration);
  }

  /**
   * 生成令牌
   */
  private String generateToken(Map<String, Object> claims, String subject, long expiration) {
    return Jwts.builder()
        .setClaims(claims)
        .setSubject(subject)
        .setIssuedAt(new Date())
        .setExpiration(new Date(System.currentTimeMillis() + expiration))
        .signWith(key, SignatureAlgorithm.HS512)
        .compact();
  }

  /**
   * 验证令牌
   */
  public Boolean validateToken(String token, UserDetails userDetails) {
    try {
      final String username = getUsernameFromToken(token);
      return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    } catch (SignatureException e) {
      log.error("无效的JWT签名: {}", e.getMessage());
    } catch (MalformedJwtException e) {
      log.error("无效的JWT令牌: {}", e.getMessage());
    } catch (ExpiredJwtException e) {
      log.error("JWT令牌已过期: {}", e.getMessage());
    } catch (UnsupportedJwtException e) {
      log.error("不支持的JWT令牌: {}", e.getMessage());
    } catch (IllegalArgumentException e) {
      log.error("JWT声明为空: {}", e.getMessage());
    }
    return false;
  }

  /**
   * 验证令牌和版本号
   */
  public Boolean validateTokenAndVersion(String token, UserDetails userDetails, long tokenVersion) {
    try {
      final String username = getUsernameFromToken(token);
      final Long version = getTokenVersionFromToken(token);
      return (username.equals(userDetails.getUsername())
          && !isTokenExpired(token)
          && version.equals(tokenVersion));
    } catch (Exception e) {
      log.error("验证令牌和版本号失败: {}", e.getMessage());
      return false;
    }
  }

  /**
   * 从请求头中获取令牌
   */
  public String getTokenFromHeader(String header) {
    if (header != null && header.startsWith(TOKEN_PREFIX)) {
      return header.replace(TOKEN_PREFIX, "");
    }
    return null;
  }

  /**
   * 创建包含用户ID的Claims
   */
  public Map<String, Object> createClaims(Long userId) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("userId", userId);
    return claims;
  }
}