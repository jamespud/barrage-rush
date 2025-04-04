package com.spud.barrage.proxy.service;

import com.spud.barrage.proxy.model.ConnectionInfo;
import com.spud.barrage.proxy.model.ServerInstance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 连接路由服务
 * 负责为客户端选择合适的WebSocket服务器
 *
 * @author Spud
 * @date 2025/3/30
 */
@Slf4j
@Service
public class ConnectionRoutingService {

  @Autowired
  private ServerDiscoveryService serverDiscoveryService;

  @Autowired
  private TokenService tokenService;

  @Autowired
  private RegionService regionService;

  @Value("${push-server.heartbeat-url-pattern:ws://{host}:{port}/ws/heartbeat/{roomId}?token={token}}")
  private String heartbeatUrlPattern;

  @Value("${push-server.danmaku-url-pattern:ws://{host}:{port}/ws/danmaku/{roomId}?token={token}}")
  private String danmakuUrlPattern;

  @Value("${push-server.cdn-url-pattern:ws://{host}:{port}/ws/cdn/{roomId}?token={token}}")
  private String cdnUrlPattern;

  /**
   * 创建连接信息
   *
   * @param roomId     房间ID
   * @param userId     用户ID
   * @param regionCode 区域代码（可选）
   * @param clientIp   客户端IP
   * @return 连接信息
   */
  public ConnectionInfo createConnectionInfo(String roomId, String userId, String regionCode,
      String clientIp) {
    // 1. 确定客户端区域
    String region = determineRegion(regionCode, clientIp);

    // 2. 选择合适的WebSocket服务器
    ServerInstance selectedInstance = selectBestInstance(roomId, region);
    if (selectedInstance == null) {
      log.error("无法找到可用的WebSocket服务器：roomId={}, region={}", roomId, region);
      throw new RuntimeException("无法找到可用的WebSocket服务器");
    }

    // 3. 生成令牌
    String token = tokenService.generateToken(roomId, userId);
    long expireAt = System.currentTimeMillis() + tokenService.getTokenTtl() * 1000;

    // 4. 构建服务器URL
    String serverHost = selectedInstance.getHost();
    int serverPort = selectedInstance.getPort();

    // 构建连接URL，替换占位符
    String heartbeatUrl = heartbeatUrlPattern
        .replace("{host}", serverHost)
        .replace("{port}", String.valueOf(serverPort))
        .replace("{roomId}", roomId)
        .replace("{token}", token);

    String danmakuUrl = danmakuUrlPattern
        .replace("{host}", serverHost)
        .replace("{port}", String.valueOf(serverPort))
        .replace("{roomId}", roomId)
        .replace("{token}", token);

    String cdnUrl = cdnUrlPattern
        .replace("{host}", serverHost)
        .replace("{port}", String.valueOf(serverPort))
        .replace("{roomId}", roomId)
        .replace("{token}", token);

    // 5. 构建并返回连接信息
    ConnectionInfo.ServerInfo serverInfo = ConnectionInfo.ServerInfo.builder()
        .heartbeatUrl(heartbeatUrl)
        .danmakuUrl(danmakuUrl)
        .cdnUrl(cdnUrl)
        .build();

    return ConnectionInfo.builder()
        .roomId(roomId)
        .userId(userId)
        .serverInfo(serverInfo)
        .token(token)
        .expireAt(expireAt)
        .region(region)
        .build();
  }

  /**
   * 确定客户端区域
   */
  private String determineRegion(String requestedRegion, String clientIp) {
    // 如果客户端指定了区域，优先使用
    if (requestedRegion != null && !requestedRegion.isEmpty()) {
      // 验证区域是否有效
      if (regionService.isValidRegion(requestedRegion)) {
        return requestedRegion;
      }
    }

    // 根据IP地址判断区域
    String regionByIp = regionService.getRegionByIp(clientIp);
    if (regionByIp != null) {
      return regionByIp;
    }

    // 默认区域
    return regionService.getDefaultRegion();
  }

  /**
   * 选择最佳WebSocket服务器实例
   */
  private ServerInstance selectBestInstance(String roomId, String region) {
    // 选择区域内最佳实例
    return serverDiscoveryService.selectBestInstance(region);
  }
}