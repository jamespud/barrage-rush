package com.spud.barrage.auth.security;

import com.spud.barrage.common.core.io.Constants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * JWT工具类
 *
 * @author Spud
 * @date 2025/3/27
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration}")
    private Long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private Long refreshTokenExpiration;

    @Value("${jwt.remember-me-expiration}")
    private Long rememberMeExpiration;

    private final RedisTemplate<String, Object> redisTemplate;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成访问Token
     */
    public String generateAccessToken(UserDetails userDetails, boolean rememberMe) {
        long expiration = rememberMe ? rememberMeExpiration : accessTokenExpiration;
        return generateToken(userDetails, expiration);
    }

    /**
     * 生成刷新Token
     */
    public String generateRefreshToken(UserDetails userDetails) {
        return generateToken(userDetails, refreshTokenExpiration);
    }

    /**
     * 生成Token
     */
    private String generateToken(UserDetails userDetails, long expiration) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", userDetails.getUsername());
        return generateToken(claims, userDetails.getUsername(), expiration);
    }

    /**
     * 根据荷载和过期时间生成Token
     */
    private String generateToken(Map<String, Object> claims, String subject, long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * 解析Token获取载荷
     */
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 从Token中获取用户名
     */
    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    /**
     * 从Token中获取过期时间
     */
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    /**
     * 获取Token中指定的载荷信息
     */
    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    /**
     * 验证Token是否过期
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
     * 验证Token是否在黑名单中
     */
    private Boolean isTokenInBlacklist(String token) {
        String key = Constants.Redis.KEY_TOKEN_BLACKLIST_PREFIX + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 将Token加入黑名单
     */
    public void addTokenToBlacklist(String token) {
        try {
            String key = Constants.Redis.KEY_TOKEN_BLACKLIST_PREFIX + token;
            Date expiration = getExpirationDateFromToken(token);
            long ttl = Math.max((expiration.getTime() - System.currentTimeMillis()) / 1000, 0);
            redisTemplate.opsForValue().set(key, "1", ttl, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("将Token加入黑名单出错", e);
        }
    }

    /**
     * 验证Token是否有效
     */
    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = getUsernameFromToken(token);
        return (username.equals(userDetails.getUsername())
                && !isTokenExpired(token)
                && !isTokenInBlacklist(token));
    }

    /**
     * 验证Token是否有效（不需要UserDetails）
     */
    public Boolean validateToken(String token) {
        try {
            return (!isTokenExpired(token) && !isTokenInBlacklist(token));
        } catch (Exception e) {
            log.error("验证Token出错", e);
            return false;
        }
    }
}