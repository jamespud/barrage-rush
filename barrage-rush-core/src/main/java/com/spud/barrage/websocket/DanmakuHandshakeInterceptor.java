package com.spud.barrage.websocket;

import com.spud.barrage.service.DanmakuService;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

/**
 * @author Spud
 * @date 2025/3/4
 */
@Slf4j
@Component
public class DanmakuHandshakeInterceptor implements HandshakeInterceptor {

  @Autowired
  private DanmakuService danmakuService;

  @Override
  public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
      WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
    try {
      // 1. 获取路径参数
      String path = request.getURI().getPath();
      String roomId = path.substring(path.lastIndexOf('/') + 1);

      // 2. 获取token参数
      String token = request.getURI().getQuery().split("token=")[1];

      // 3. 验证token并获取用户信息
      String userId = validateToken(token);
      if (userId == null) {
        return false;
      }

      // 4. 检查发送权限
      if (!danmakuService.checkSendPermission(userId, roomId)) {
        return false;
      }

      // 5. 存储会话信息
      attributes.put("userId", userId);
      attributes.put("roomId", roomId);
      attributes.put("connectTime", System.currentTimeMillis());

      return true;
    } catch (Exception e) {
      log.error("Handshake failed", e);
      return false;
    }
  }

  @Override
  public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
      WebSocketHandler wsHandler, Exception exception) {
    // 握手后的处理
  }

  private String validateToken(String token) {
    // TODO: 实现真实的token验证逻辑
    return token != null ? "test_user_id" : null;
  }
}