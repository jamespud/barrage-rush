package com.spud.barrage.auth.util;

import com.spud.barrage.auth.model.User;
import com.spud.barrage.common.auth.util.JwtTokenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT工具类，封装common-auth模块的JwtTokenUtil
 *
 * @author Spud
 * @date 2025/3/27
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtTokenUtil jwtTokenUtil;

    /**
     * 获取用户名
     */
    public String getUsernameFromToken(String token) {
        return jwtTokenUtil.getUsernameFromToken(token);
    }

    /**
     * 获取过期时间
     */
    public Date getExpirationDateFromToken(String token) {
        return jwtTokenUtil.getExpirationDateFromToken(token);
    }

    /**
     * 从token中获取token版本号
     */
    public Long getTokenVersionFromToken(String token) {
        return jwtTokenUtil.getTokenVersionFromToken(token);
    }

    /**
     * 生成token
     */
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        if (userDetails instanceof User user) {
            claims.put("userId", user.getId());
        }
        return jwtTokenUtil.generateAccessToken(userDetails, claims);
    }

    /**
     * 生成刷新token
     */
    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        if (userDetails instanceof User user) {
            claims.put("userId", user.getId());
            claims.put("tokenVersion", user.getTokenVersion());
        }
        return jwtTokenUtil.generateRefreshToken(userDetails, claims);
    }

    /**
     * 验证令牌
     */
    public Boolean validateToken(String token, UserDetails userDetails) {
        return jwtTokenUtil.validateToken(token, userDetails);
    }

    /**
     * 验证令牌和版本号
     * 用于敏感操作时验证refresh token
     */
    public Boolean validateTokenAndVersion(String token, UserDetails userDetails) {
        if (userDetails instanceof User user) {
            return jwtTokenUtil.validateTokenAndVersion(token, userDetails, user.getTokenVersion());
        }
        return false;
    }

    /**
     * 从授权头中提取token
     */
    public String getTokenFromHeader(String header) {
        return jwtTokenUtil.getTokenFromHeader(header);
    }
}