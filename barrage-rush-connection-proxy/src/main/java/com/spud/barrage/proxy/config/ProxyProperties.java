package com.spud.barrage.proxy.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author Spud
 * @date 2025/3/13
 */
@Configuration
@ConfigurationProperties(prefix = "proxy")
@Data
public class ProxyProperties {

  // 本地服务器ID
  private int serverId;

  // 本地服务器地址
  private String serverAddress;

  // 本地服务器端口
  private int serverPort;

  // 本地服务器区域
  private String region;

  // 最大连接数
  private int maxConnections = 10000;

  // 连接超时时间（毫秒）
  private int connectionTimeout = 5000;

  // 心跳间隔（秒）
  private int heartbeatInterval = 30;

  // 健康检查间隔（秒）
  private int healthCheckInterval = 10;

  // 负载均衡策略（ROUND_ROBIN/LEAST_CONNECTIONS/LATENCY）
  private String loadBalanceStrategy = "ROUND_ROBIN";

  // 地理位置服务配置
  private GeoLocationConfig geoLocation = new GeoLocationConfig();

  @Data
  public static class GeoLocationConfig {

    // 是否启用地理位置路由
    private boolean enabled = true;

    // GeoIP数据库路径
    private String databasePath;

    // 默认区域
    private String defaultRegion = "CENTRAL";

    // 是否使用中国区域划分
    private boolean useChinaRegions = true;

    // 是否记录详细地理位置信息
    private boolean logDetailedLocation = true;

    // 是否将地理位置信息添加到会话属性
    private boolean addLocationToSession = true;
  }
} 