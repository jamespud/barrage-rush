package com.spud.barrage.proxy.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket连接信息，包含三个连接的URL
 *
 * @author Spud
 * @date 2025/3/30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionInfo {

  /**
   * 房间ID
   */
  private String roomId;

  /**
   * 服务器连接信息
   */
  private ServerInfo serverInfo;

  /**
   * 连接令牌
   */
  private String token;

  /**
   * 过期时间戳
   */
  private long expireAt;

  /**
   * 区域代码
   */
  private String region;

  /**
   * 用户ID
   */
  private String userId;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ServerInfo {

    /**
     * 心跳WebSocket URL
     */
    private String heartbeatUrl;

    /**
     * 弹幕WebSocket URL
     */
    private String danmakuUrl;

    /**
     * CDN信息WebSocket URL
     */
    private String cdnUrl;
  }
}