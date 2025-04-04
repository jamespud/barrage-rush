package com.spud.barrage.push.interceptor;

import com.spud.barrage.push.service.TokenService;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * WebSocket拦截器
 * 负责身份验证和解析URI参数
 *
 * @author Spud
 * @date 2025/3/30
 */
@Slf4j
@Component
public class WebSocketInterceptor implements HandshakeInterceptor {

  @Autowired
  private TokenService tokenService;

  @Override
  public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
      WebSocketHandler wsHandler, Map<String, Object> attributes) {
    // 获取请求路径
    String path = request.getURI().getPath();
    log.debug("[WebSocket] 收到连接请求: {}", path);

    try {
      // 解析房间ID
      String roomId = extractRoomId(request);
      if (!StringUtils.hasText(roomId)) {
        log.warn("[WebSocket] 缺少房间ID: {}", path);
        response.setStatusCode(HttpStatus.BAD_REQUEST);
        return false;
      }
      attributes.put("roomId", roomId);

      // 获取token
      String token = extractToken(request);
      if (!StringUtils.hasText(token)) {
        log.warn("[WebSocket] 缺少token: {}", path);
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return false;
      }

      // 验证token
      Map<String, Object> tokenInfo = tokenService.verifyToken(token);
      if (tokenInfo == null) {
        log.warn("[WebSocket] 无效的token: {}", path);
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return false;
      }

      // 将用户ID添加到attributes中
      String userId = (String) tokenInfo.get("userId");
      attributes.put("userId", userId);
      attributes.put("token", token);

      log.info("[WebSocket] 验证通过: roomId={}, userId={}", roomId, userId);
      return true;
    } catch (Exception e) {
      log.error("[WebSocket] 握手验证异常: {}", path, e);
      response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
      return false;
    }
  }

  @Override
  public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
      WebSocketHandler wsHandler, Exception exception) {
    // 握手后不做额外处理
  }

  /**
   * 从URI中提取房间ID
   */
  private String extractRoomId(ServerHttpRequest request) {
    String path = request.getURI().getPath();
    String[] segments = path.split("/");
    if (segments.length > 0) {
      return segments[segments.length - 1];
    }
    return null;
  }

  /**
   * 从请求参数中提取token
   */
  private String extractToken(ServerHttpRequest request) {
    // 从查询参数中获取token
    String query = request.getURI().getQuery();
    if (query != null) {
      Map<String, String> queryParams = UriComponentsBuilder.newInstance()
          .query(query)
          .build()
          .getQueryParams()
          .toSingleValueMap();
      String token = queryParams.get("token");
      if (StringUtils.hasText(token)) {
        return token;
      }
    }

    // 从header中获取token
    if (request.getHeaders().containsKey("Authorization")) {
      String authHeader = request.getHeaders().getFirst("Authorization");
      if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
        return authHeader.substring(7);
      }
    }

    return null;
  }
}