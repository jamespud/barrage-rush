package com.spud.barrage.common.mq.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author Spud
 * @date 2025/3/13
 */
@Configuration
@ConfigurationProperties(prefix = "dynamic.mq")
@Data
public class DynamicMQProperties {

  // 普通房间分片数
  private int normalShardCount = 3;

  // 热门房间分片数
  private int hotShardCount = 10;

  // 超热门房间分片数
  private int superHotShardCount = 20;

  // 队列最大长度
  private int maxQueueLength = 50000;

  // 消息TTL（毫秒）
  private int messageTtl = 30000;

  // 队列清理间隔（秒）
  private int cleanupInterval = 300;
} 