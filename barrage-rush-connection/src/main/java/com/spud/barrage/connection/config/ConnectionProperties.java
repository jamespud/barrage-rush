package com.spud.barrage.connection.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 连接服务配置
 *
 * @author Spud
 * @date 2025/3/15
 */
@Configuration
@ConfigurationProperties(prefix = "connection")
@Data
public class ConnectionProperties {

  /**
   * 服务器ID
   */
  private int serverId;

  /**
   * 服务器地址
   */
  private String serverAddress;
    
  /**
   * 服务器端口
   */
  private int serverPort;

  /**
   * 服务器区域
   */
  private String region;

  /**
   * 最大连接数
   */
  private int maxConnections = 10000;

  /**
   * 心跳超时时间（毫秒）
   */
  private int heartbeatTimeout = 30000;

  /**
   * 心跳间隔（毫秒）
   */
  private int heartbeatInterval = 15000;

  /**
   * 连接超时时间（毫秒）
   */
  private int connectionTimeout = 5000;

  /**
   * 消息缓冲区大小
   */
  private int messageBufferSize = 100;

  /**
   * 消息发送间隔（毫秒）
   */
  private int messageSendInterval = 50;

  /**
   * Redis配置
   */
  private RedisConfig redis = new RedisConfig();

  /**
   * Redis配置
   */
  @Data
  public static class RedisConfig {

    /**
     * 弹幕消息键前缀
     */
    private String danmakuKeyPrefix = "danmaku:room:";

    /**
     * 弹幕消息过期时间（秒）
     */
    private int danmakuExpire = 3600;

    /**
     * 房间在线人数键前缀
     */
    private String roomOnlineKeyPrefix = "room:online:";

    /**
     * 用户会话键前缀
     */
    private String userSessionKeyPrefix = "user:session:";

    /**
     * 用户会话过期时间（秒）
     */
    private int userSessionExpire = 3600;

    /**
     * 心跳键前缀
     */
    private String heartbeatKeyPrefix = "heartbeat:";

    /**
     * 心跳过期时间（秒）
     */
    private int heartbeatExpire = 60;
  }
} 