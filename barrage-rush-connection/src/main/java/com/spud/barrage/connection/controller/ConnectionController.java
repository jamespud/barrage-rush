package com.spud.barrage.connection.controller;

import com.spud.barrage.connection.config.ConnectionProperties;
import com.spud.barrage.connection.model.UserSession;
import com.spud.barrage.connection.service.DanmakuService;
import com.spud.barrage.connection.service.SessionService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 连接服务控制器
 *
 * @author Spud
 * @date 2025/3/15
 */
@Slf4j
@RestController
@RequestMapping("/api/connection")
@RequiredArgsConstructor
public class ConnectionController {

  private final SessionService sessionService;
  private final DanmakuService danmakuService;
  private final ConnectionProperties properties;

  /**
   * 获取服务器状态
   */
  @GetMapping("/status")
  public ResponseEntity<Map<String, Object>> getStatus() {
    Map<String, Object> status = new HashMap<>();

    // 服务器信息
    Map<String, Object> serverInfo = new HashMap<>();
    serverInfo.put("id", properties.getServerId());
    serverInfo.put("address", properties.getServerAddress());
    serverInfo.put("port", properties.getServerPort());
    serverInfo.put("region", properties.getRegion());
    serverInfo.put("maxConnections", properties.getMaxConnections());
    status.put("server", serverInfo);

    return ResponseEntity.ok(status);
  }

  /**
   * 获取房间在线人数
   */
  @GetMapping("/room/{roomId}/online")
  public ResponseEntity<Map<String, Object>> getRoomOnlineCount(@PathVariable Long roomId) {
    Map<String, Object> result = new HashMap<>();

    long onlineCount = sessionService.getRoomOnlineCount(roomId);
    result.put("roomId", roomId);
    result.put("onlineCount", onlineCount);

    return ResponseEntity.ok(result);
  }

  /**
   * 获取房间最近消息
   */
  @GetMapping("/room/{roomId}/messages")
  public ResponseEntity<Map<String, Object>> getRoomMessages(
      @PathVariable Long roomId,
      @RequestParam(defaultValue = "10") int count) {
    Map<String, Object> result = new HashMap<>();

    List<DanmakuMessage> messages = danmakuService.getLatestMessages(roomId, count);
    long messageCount = danmakuService.getMessageCount(roomId);

    result.put("roomId", roomId);
    result.put("messages", messages);
    result.put("totalCount", messageCount);

    return ResponseEntity.ok(result);
  }

  /**
   * 发送弹幕消息
   */
  @PostMapping("/room/{roomId}/message")
  public ResponseEntity<Map<String, Object>> sendMessage(
      @PathVariable Long roomId,
      @RequestBody DanmakuMessage message) {
    Map<String, Object> result = new HashMap<>();

    // 设置房间ID
    message.setRoomId(roomId);

    // 保存消息
    boolean success = danmakuService.saveMessage(message);

    result.put("success", success);
    result.put("message", message);

    return ResponseEntity.ok(result);
  }

  /**
   * 获取用户会话信息
   */
  @GetMapping("/session/{sessionId}")
  public ResponseEntity<Map<String, Object>> getSessionInfo(@PathVariable String sessionId) {
    Map<String, Object> result = new HashMap<>();

    UserSession session = sessionService.getSession(sessionId);
    if (session != null) {
      result.put("session", session);
      result.put("found", true);
    } else {
      result.put("found", false);
    }

    return ResponseEntity.ok(result);
  }

  /**
   * 健康检查
   */
  @GetMapping("/health")
  public ResponseEntity<Map<String, Object>> health() {
    Map<String, Object> health = new HashMap<>();
    health.put("status", "UP");
    health.put("serverId", properties.getServerId());
    health.put("region", properties.getRegion());
    health.put("timestamp", System.currentTimeMillis());

    return ResponseEntity.ok(health);
  }
} 