package com.spud.barrage.proxy.service;

import com.spud.barrage.proxy.config.ProxyProperties;
import com.spud.barrage.proxy.config.WSServerConnection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 负载均衡服务
 *
 * @author Spud
 * @date 2025/3/13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoadBalancer {

  private static final ConcurrentHashMap<String, AtomicInteger> regionCounterMap = new ConcurrentHashMap<>();
  private final RedisTemplate<String, Object> redisTemplate;
  private final ProxyProperties properties;

  /**
   * 选择目标服务器
   *
   * @param region 区域
   * @return 服务器ID，如果没有可用服务器则返回null
   */
  public Integer selectTargetServer(String region) {
    try {
      return switch (properties.getLoadBalanceStrategy()) {
        case "ROUND_ROBIN" -> selectByRoundRobin(region);
        case "LEAST_CONNECTIONS" -> selectByLeastConnections(region);
        case "LATENCY" -> selectByLatency(region);
        default -> {
          log.warn("未知的负载均衡策略: {}, 使用轮询策略", properties.getLoadBalanceStrategy());
          yield selectByRoundRobin(region);
        }
      };
    } catch (Exception e) {
      log.error("选择目标服务器失败: {}", e.getMessage(), e);
      return null;
    }
  }

  /**
   * 轮询选择服务器
   */
  private Integer selectByRoundRobin(String region) {
    Set<Integer> servers = getHealthyServers(region);
    if (servers == null || servers.isEmpty()) {
      log.warn("区域 {} 没有可用的服务器", region);
      return null;
    }

    List<Integer> serverList = new ArrayList<>(servers);
    return serverList.get(Math.abs(
        regionCounterMap.getOrDefault(region, new AtomicInteger(0)).incrementAndGet()
            % serverList.size()));
  }

  /**
   * 最少连接数选择服务器
   */
  private Integer selectByLeastConnections(String region) {
    Set<Integer> servers = getHealthyServers(region);
    if (servers == null || servers.isEmpty()) {
      log.warn("区域 {} 没有可用的服务器", region);
      return null;
    }

    return servers.stream()
        .min(Comparator.comparingInt(this::getConnectionCount))
        .orElse(null);
  }

  /**
   * 延迟选择服务器
   */
  private Integer selectByLatency(String region) {
    Set<Integer> servers = getHealthyServers(region);
    if (servers == null || servers.isEmpty()) {
      log.warn("区域 {} 没有可用的服务器", region);
      return null;
    }

    return servers.stream()
        .min(Comparator.comparingInt(this::getLatency))
        .orElse(null);
  }

  /**
   * 获取健康的服务器列表
   */
  private Set<Integer> getHealthyServers(String region) {
    String key = String.format(WSServerConnection.WS_REGION, region);
    Set<Object> members = redisTemplate.opsForSet().members(key);

    if (members == null || members.isEmpty()) {
      // 如果指定区域没有服务器，尝试使用默认区域
      if (!region.equals(properties.getGeoLocation().getDefaultRegion())) {
        log.info("区域 {} 没有可用服务器，尝试使用默认区域 {}",
            region, properties.getGeoLocation().getDefaultRegion());
        return getHealthyServers(properties.getGeoLocation().getDefaultRegion());
      }
      return null;
    }

    return members.stream()
        .map(obj -> {
          try {
            return Integer.parseInt(obj.toString());
          } catch (NumberFormatException e) {
            log.error("服务器ID格式错误: {}", obj);
            return null;
          }
        })
        .filter(id -> id != null && isServerHealthy(id))
        .collect(Collectors.toSet());
  }

  /**
   * 检查服务器是否健康
   */
  private boolean isServerHealthy(Integer serverId) {
    String healthKey = String.format(WSServerConnection.WS_HEALTH, serverId);
    Object health = redisTemplate.opsForValue().get(healthKey);

    if (health == null) {
      return false;
    }

    return WSServerConnection.HEALTH_UP.equals(health.toString());
  }

  /**
   * 获取服务器连接数
   */
  private int getConnectionCount(Integer serverId) {
    String key = String.format(WSServerConnection.WS_CONNECTION, serverId);
    Object count = redisTemplate.opsForValue().get(key);
    return count != null ? Integer.parseInt(count.toString()) : Integer.MAX_VALUE;
  }

  /**
   * 获取服务器延迟
   */
  private int getLatency(Integer serverId) {
    String key = String.format(WSServerConnection.WS_LATENCY, serverId);
    Object latency = redisTemplate.opsForValue().get(key);
    return latency != null ? Integer.parseInt(latency.toString()) : Integer.MAX_VALUE;
  }

  /**
   * 更新服务器状态
   */
  public void updateServerStatus(int serverId, int connections, int latency) {
    try {
      String region = properties.getRegion();

      // 更新连接数
      String connKey = String.format(WSServerConnection.WS_CONNECTION, serverId);
      redisTemplate.opsForValue().set(connKey, connections);

      // 更新延迟
      String latKey = String.format(WSServerConnection.WS_LATENCY, serverId);
      redisTemplate.opsForValue().set(latKey, latency);

      // 更新健康状态
      String healthKey = String.format(WSServerConnection.WS_HEALTH, serverId);
      redisTemplate.opsForValue().set(healthKey, WSServerConnection.HEALTH_UP);

      // 添加到区域服务器集合
      String regionKey = String.format(WSServerConnection.WS_REGION, region);
      redisTemplate.opsForSet().add(regionKey, serverId);

      // 设置过期时间
      redisTemplate.expire(connKey, WSServerConnection.SERVER_EXPIRE_SECONDS, TimeUnit.SECONDS);
      redisTemplate.expire(latKey, WSServerConnection.SERVER_EXPIRE_SECONDS, TimeUnit.SECONDS);
      redisTemplate.expire(healthKey, WSServerConnection.SERVER_EXPIRE_SECONDS, TimeUnit.SECONDS);
      redisTemplate.expire(regionKey, WSServerConnection.SERVER_EXPIRE_SECONDS, TimeUnit.SECONDS);

      log.debug("更新服务器状态: id={}, 区域={}, 连接数={}, 延迟={}ms",
          serverId, region, connections, latency);
    } catch (Exception e) {
      log.error("更新服务器状态失败: {}", e.getMessage(), e);
    }
  }

  /**
   * 注册当前服务器
   */
  public void registerServer() {
    try {
      int serverId = properties.getServerId();
      String region = properties.getRegion();

      // 更新服务器信息
      String infoKey = String.format(WSServerConnection.WS_INFO, serverId);
      redisTemplate.opsForHash().put(infoKey, "id", serverId);
      redisTemplate.opsForHash().put(infoKey, "address", properties.getServerAddress());
      redisTemplate.opsForHash().put(infoKey, "port", properties.getServerPort());
      redisTemplate.opsForHash().put(infoKey, "region", region);
      redisTemplate.opsForHash().put(infoKey, "maxConnections", properties.getMaxConnections());
      redisTemplate.opsForHash().put(infoKey, "registerTime", System.currentTimeMillis());

      // 添加到区域服务器集合
      String regionKey = String.format(WSServerConnection.WS_REGION, region);
      redisTemplate.opsForSet().add(regionKey, serverId);

      // 设置过期时间
      redisTemplate.expire(infoKey, WSServerConnection.SERVER_EXPIRE_SECONDS, TimeUnit.SECONDS);
      redisTemplate.expire(regionKey, WSServerConnection.SERVER_EXPIRE_SECONDS, TimeUnit.SECONDS);

      log.info("注册服务器: id={}, 区域={}, 地址={}:{}",
          serverId, region, properties.getServerAddress(), properties.getServerPort());
    } catch (Exception e) {
      log.error("注册服务器失败: {}", e.getMessage(), e);
    }
  }
} 