package com.spud.barrage.proxy.controller;

import com.spud.barrage.common.core.io.Result;
import com.spud.barrage.proxy.model.ConnectionInfo;
import com.spud.barrage.proxy.service.ConnectionRoutingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * WebSocket连接控制器
 * 提供获取WebSocket连接信息的API
 *
 * @author Spud
 * @date 2025/3/30
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
public class ConnectionController {

  @Autowired
  private ConnectionRoutingService routingService;

  /**
   * 获取WebSocket连接信息
   * 客户端通过此接口获取WebSocket连接的相关信息，包括服务器地址、认证令牌等
   *
   * @param roomId  房间ID
   * @param userId  用户ID (可选)
   * @param region  区域 (可选)
   * @param request HTTP请求
   * @return 连接信息
   */
  @GetMapping("/connect")
  public Result<ConnectionInfo> getConnectionInfo(
      @RequestParam("roomId") String roomId,
      @RequestParam(value = "userId", required = false) String userId,
      @RequestParam(value = "region", required = false) String region,
      HttpServletRequest request) {

    log.info("获取连接信息请求: roomId={}, userId={}, region={}", roomId, userId, region);

    // 获取客户端IP地址
    String clientIp = getClientIp(request);

    // 如果未提供用户ID，使用guest前缀的临时ID
    if (userId == null || userId.trim().isEmpty()) {
      userId = "guest_" + System.currentTimeMillis();
      log.debug("使用临时用户ID: {}", userId);
    }

    try {
      // 获取连接信息
      ConnectionInfo connectionInfo = routingService
          .createConnectionInfo(roomId, userId, region, clientIp);
      log.debug("已创建连接信息: roomId={}, userId={}, server={}",
          roomId, userId, connectionInfo.getServerInfo());

      return Result.success(connectionInfo);
    } catch (Exception e) {
      log.error("创建连接信息失败: roomId={}, userId={}", roomId, userId, e);
      return Result.fail("无法创建连接信息: " + e.getMessage());
    }
  }

  /**
   * 获取客户端真实IP
   */
  private String getClientIp(HttpServletRequest request) {
    String ip = request.getHeader("X-Forwarded-For");
    if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getHeader("Proxy-Client-IP");
    }
    if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getHeader("WL-Proxy-Client-IP");
    }
    if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getHeader("X-Real-IP");
    }
    if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getRemoteAddr();
    }

    // 多个代理的情况，第一个IP为客户端真实IP
    if (ip != null && ip.indexOf(",") > 0) {
      ip = ip.substring(0, ip.indexOf(","));
    }

    return ip;
  }

  /**
   * 健康检查接口
   */
  @GetMapping("/health")
  public Result<String> health() {
    return Result.success("OK");
  }
}