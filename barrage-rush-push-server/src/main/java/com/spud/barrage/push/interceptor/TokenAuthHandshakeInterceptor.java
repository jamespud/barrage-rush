package com.spud.barrage.push.interceptor;

import com.spud.barrage.push.service.TokenService;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * WebSocket握手拦截器，用于验证令牌
 * @author Spud
 * @date 2025/3/30
 */
@Slf4j
@Component
public class TokenAuthHandshakeInterceptor implements HandshakeInterceptor {

  private static final String TOKEN_PARAM = "token";
  private static final String ROOM_ID_ATTR = "roomId";
  private static final String USER_ID_ATTR = "userId";

  // 路径正则表达式，用于提取roomId
  private static final Pattern ROOM_ID_PATTERN = Pattern.compile("/ws/\\w+/(\\w+)");

  @Autowired
  private TokenService tokenService;

  @Override
  public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
      WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
    // 获取请求URI
    String path = request.getURI().getPath();
    log.debug("WebSocket握手请求: {}", path);

    // 从URI路径中提取roomId
    String roomId = extractRoomId(path);
    if (roomId == null) {
      log.warn("无法从路径中提取房间ID: {}", path);
      return false;
    }

    // 从URI查询参数中获取token
    Map<String, String> params = UriComponentsBuilder.fromUri(request.getURI()).build()
        .getQueryParams()
        .toSingleValueMap();
    String token = params.get(TOKEN_PARAM);

    // 验证token参数
    if (!StringUtils.hasText(token)) {
      log.warn("缺少token参数: {}", path);
      return false;
    }

    // 验证令牌有效性
    String userId = tokenService.verifyToken(token, roomId);
    if (userId == null) {
      log.warn("无效的令牌: token={}, roomId={}", token, roomId);
      return false;
    }

    // 存储信息到会话属性
    attributes.put(ROOM_ID_ATTR, roomId);
    attributes.put(USER_ID_ATTR, userId);

    log.debug("WebSocket握手验证成功: roomId={}, userId={}", roomId, userId);
    return true;
  }

  @Override
  public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
      WebSocketHandler wsHandler, Exception exception) {
    if (exception != null) {
      log.error("WebSocket握手异常", exception);
    }
  }

  /**
   * 从路径中提取房间ID
   */
  private String extractRoomId(String path) {
    Matcher matcher = ROOM_ID_PATTERN.matcher(path);
    if (matcher.find()) {
      return matcher.group(1);
    }
    return null;
  }
}