package com.spud.barrage.common.mq.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author Spud
 * @date 2025/3/13
 */
@Configuration
@ConfigurationProperties(prefix = "room.traffic")
@Data
public class RoomTrafficProperties {

  // 热门房间阈值（观众数）
  private int hotThreshold = 1000;

  // 超热门房间阈值
  private int superHotThreshold = 10000;

  // 冷门房间阈值
  private int coldThreshold = 10;

  // 观众数刷新间隔（秒）
  private int refreshInterval = 5;

  // 房间类型缓存过期时间（秒）
  private int typeCacheExpire = 300;
  
  // 房间观众数缓存过期时间（秒）
  private int viewerCacheExpire = 300;
  
  // 房间队列缓存过期时间（秒）
  private int queueCacheExpire = 300;
  
  // 房间交换机缓存过期时间（秒）
  private int exchangeCacheExpire = 300;
  
}
