package com.spud.barrage.push.config;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 服务器注册配置
 *
 * @author Spud
 * @date 2025/3/15
 */
@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class ServerRegistrationConfig {

  // Redis键常量
  private static final String SERVER_INFO_KEY = "ws:server:%d:info";
  private static final String SERVER_HEALTH_KEY = "ws:server:%d:health";
  private static final String SERVER_REGION_KEY = "ws:region:%s:servers";
  private static final String SERVER_EXPIRE_SECONDS = "30";
  // 健康状态常量
  private static final String HEALTH_UP = "UP";
  private final RedisTemplate<String, Object> redisTemplate;
  private final ConnectionProperties properties;

  @PostConstruct
  public void init() {
    registerServer();
  }

  /**
   * 注册服务器
   */
  public void registerServer() {
    try {
      int serverId = properties.getServerId();
      String region = properties.getRegion();

      // 更新服务器信息
      String infoKey = String.format(SERVER_INFO_KEY, serverId);
      redisTemplate.opsForHash().put(infoKey, "id", serverId);
      redisTemplate.opsForHash().put(infoKey, "address", properties.getServerAddress());
      redisTemplate.opsForHash().put(infoKey, "port", properties.getServerPort());
      redisTemplate.opsForHash().put(infoKey, "region", region);
      redisTemplate.opsForHash().put(infoKey, "maxConnections", properties.getMaxConnections());
      redisTemplate.opsForHash().put(infoKey, "registerTime", System.currentTimeMillis());

      // 更新健康状态
      String healthKey = String.format(SERVER_HEALTH_KEY, serverId);
      redisTemplate.opsForValue().set(healthKey, HEALTH_UP);

      // 添加到区域服务器集合
      String regionKey = String.format(SERVER_REGION_KEY, region);
      redisTemplate.opsForSet().add(regionKey, serverId);

      // 设置过期时间
      redisTemplate.expire(infoKey, Integer.parseInt(SERVER_EXPIRE_SECONDS), TimeUnit.SECONDS);
      redisTemplate.expire(healthKey, Integer.parseInt(SERVER_EXPIRE_SECONDS), TimeUnit.SECONDS);
      redisTemplate.expire(regionKey, Integer.parseInt(SERVER_EXPIRE_SECONDS), TimeUnit.SECONDS);

      log.info("注册服务器: id={}, 区域={}, 地址={}:{}",
          serverId, region, properties.getServerAddress(), properties.getServerPort());
    } catch (Exception e) {
      log.error("注册服务器失败: {}", e.getMessage(), e);
    }
  }

  /**
   * 定期更新服务器状态
   */
  @Scheduled(fixedRate = 10000) // 每10秒更新一次
  public void updateServerStatus() {
    try {
      int serverId = properties.getServerId();

      // 更新健康状态
      String healthKey = String.format(SERVER_HEALTH_KEY, serverId);
      redisTemplate.opsForValue().set(healthKey, HEALTH_UP);
      redisTemplate.expire(healthKey, Integer.parseInt(SERVER_EXPIRE_SECONDS), TimeUnit.SECONDS);

      // 更新服务器信息过期时间
      String infoKey = String.format(SERVER_INFO_KEY, serverId);
      redisTemplate.expire(infoKey, Integer.parseInt(SERVER_EXPIRE_SECONDS), TimeUnit.SECONDS);

      // 更新区域服务器集合过期时间
      String regionKey = String.format(SERVER_REGION_KEY, properties.getRegion());
      redisTemplate.expire(regionKey, Integer.parseInt(SERVER_EXPIRE_SECONDS), TimeUnit.SECONDS);

      log.debug("更新服务器状态: id={}, 区域={}", serverId, properties.getRegion());
    } catch (Exception e) {
      log.error("更新服务器状态失败: {}", e.getMessage(), e);
    }
  }
} 