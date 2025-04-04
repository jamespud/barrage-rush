package com.spud.barrage.common.auth.config;

import com.spud.barrage.common.auth.util.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * JWT配置类
 *
 * @author Spud
 * @date 2023/4/1
 */
@Configuration
public class JwtConfig {

  @Value("${jwt.secret:barrage-rush-default-secret-key}")
  private String secret;

  @Value("${jwt.access-token-expiration:1800000}")
  private long accessTokenExpiration;

  @Value("${jwt.refresh-token-expiration:604800000}")
  private long refreshTokenExpiration;

  @Value("${jwt.remember-me-expiration:2592000000}")
  private long rememberMeExpiration;

  /**
   * 配置JWT工具类
   */
  @Bean
  @ConditionalOnMissingBean
  public JwtTokenUtil jwtTokenUtil() {
    JwtTokenUtil jwtTokenUtil = new JwtTokenUtil();
    jwtTokenUtil.setSecret(secret);
    jwtTokenUtil.setAccessTokenExpiration(accessTokenExpiration);
    jwtTokenUtil.setRefreshTokenExpiration(refreshTokenExpiration);
    jwtTokenUtil.setRememberMeExpiration(rememberMeExpiration);
    return jwtTokenUtil;
  }
}