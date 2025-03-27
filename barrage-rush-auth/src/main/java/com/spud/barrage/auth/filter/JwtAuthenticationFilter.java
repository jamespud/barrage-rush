package com.spud.barrage.auth.filter;


import com.spud.barrage.auth.repository.UserRepository;
import com.spud.barrage.auth.service.UserDetailsServiceImpl;
import com.spud.barrage.auth.util.JwtUtil;
import com.spud.barrage.common.core.io.Constants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * JWT认证过滤器
 *
 * @author Spud
 * @date 2025/3/27
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtUtil jwtUtil;
  private final UserDetailsServiceImpl userDetailsService;
  private final RedisTemplate<String, Object> redisTemplate;
  private final UserRepository userRepository;

  @Override
  protected void doFilterInternal(@NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain) throws ServletException, IOException {
    try {
      // 检查是否为白名单路径
      String requestURI = request.getRequestURI();
      if (isWhiteListPath(requestURI)) {
        filterChain.doFilter(request, response);
        return;
      }

      // 从请求头中获取Authorization
      String authorizationHeader = request.getHeader(Constants.Security.AUTHORIZATION_HEADER);
      if (!StringUtils.hasText(authorizationHeader)
          || !authorizationHeader.startsWith(Constants.Security.TOKEN_PREFIX)) {
        filterChain.doFilter(request, response);
        return;
      }

      // 从Authorization头中提取JWT令牌
      String token = jwtUtil.getTokenFromHeader(authorizationHeader);
      if (token == null) {
        filterChain.doFilter(request, response);
        return;
      }

      // 检查令牌是否在黑名单中（已登出）
      String blacklistKey = Constants.Security.BLACKLIST_TOKEN_PREFIX + token;
      if (Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey))) {
        log.warn("Token is in blacklist: {}", token);
        filterChain.doFilter(request, response);
        return;
      }

      // 获取用户名并检查认证上下文
      String username = jwtUtil.getUsernameFromToken(token);
      if (StringUtils.hasText(username)
          && SecurityContextHolder.getContext().getAuthentication() == null) {
        // 加载用户详情
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        // 验证令牌
        if (jwtUtil.validateToken(token, userDetails)) {
          // 创建认证对象
          UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
              userDetails, null, userDetails.getAuthorities());
          authenticationToken.setDetails(
              new WebAuthenticationDetailsSource().buildDetails(request));

          // 设置认证上下文
          SecurityContextHolder.getContext().setAuthentication(authenticationToken);

          // 更新用户最后活跃时间
          updateUserLastActiveTime(username);
        }
      }
    } catch (Exception e) {
      log.error("Cannot set user authentication: {}", e.getMessage(), e);
    }

    filterChain.doFilter(request, response);
  }

  /**
   * 检查请求路径是否在白名单中
   */
  private boolean isWhiteListPath(String requestURI) {
    return Arrays.stream(Constants.Security.WHITE_LIST)
        .anyMatch(pattern -> {
          if (pattern.endsWith("/**")) {
            String prefix = pattern.substring(0, pattern.length() - 3);
            return requestURI.startsWith(prefix);
          }
          return pattern.equals(requestURI);
        });
  }

  /**
   * 更新用户最后活跃时间
   */
  private void updateUserLastActiveTime(String username) {
    try {
      userRepository.findByUsername(username).ifPresent(user -> {
        user.setLastActiveTime(LocalDateTime.now());
        userRepository.save(user);
      });
    } catch (Exception e) {
      log.error("Error updating user last active time: {}", e.getMessage(), e);
    }
  }
}