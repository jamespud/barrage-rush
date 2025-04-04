package com.spud.barrage.proxy.model;

import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket服务器实例信息
 *
 * @author Spud
 * @date 2025/3/30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServerInstance {

  /**
   * 实例ID
   */
  private String id;

  /**
   * 主机地址
   */
  private String host;

  /**
   * 端口
   */
  private int port;

  /**
   * 区域
   */
  private String region;

  /**
   * 是否活跃
   */
  private boolean active;

  /**
   * 最后心跳时间
   */
  private long lastHeartbeat;

  /**
   * 启动时间
   */
  private long startTime;

  /**
   * 服务类型
   */
  private String type;

  /**
   * 实例指标
   */
  @Builder.Default
  private Map<String, Integer> metrics = new HashMap<>();

  /**
   * 获取连接数
   */
  public int getConnections() {
    return metrics.getOrDefault("totalConnections", 0);
  }

  /**
   * 获取CPU负载
   */
  public double getCpuLoad() {
    return metrics.getOrDefault("cpuLoad", 0) / 100.0;
  }

  /**
   * 获取内存使用率
   */
  public double getMemoryUsage() {
    return metrics.getOrDefault("memoryUsage", 0) / 100.0;
  }

  /**
   * 是否健康
   */
  public boolean isHealthy() {
    return active && (System.currentTimeMillis() - lastHeartbeat < 30000);
  }

  /**
   * 获取加权负载分数（越低越好）
   */
  public double getWeightedLoadScore() {
    // 根据连接数、CPU负载和内存使用率计算加权分数
    double connectionWeight = 0.5;
    double cpuWeight = 0.3;
    double memoryWeight = 0.2;

    double connectionScore = getConnections() / 1000.0; // 假设1000连接为满载
    double cpuScore = getCpuLoad();
    double memoryScore = getMemoryUsage();

    return connectionWeight * connectionScore +
        cpuWeight * cpuScore +
        memoryWeight * memoryScore;
  }
}