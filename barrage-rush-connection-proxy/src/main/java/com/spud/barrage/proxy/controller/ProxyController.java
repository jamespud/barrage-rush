package com.spud.barrage.proxy.controller;

import com.spud.barrage.proxy.config.ProxyProperties;
import com.spud.barrage.proxy.config.WSServerConnection;
import com.spud.barrage.proxy.service.GeoLocationService;
import com.spud.barrage.proxy.service.LoadBalancer;
import com.spud.barrage.proxy.service.WebSocketProxyHandler;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 连接代理控制器
 * 
 * @author Spud
 * @date 2025/3/15
 */
@Slf4j
@RestController
@RequestMapping("/api/proxy")
@RequiredArgsConstructor
public class ProxyController {

  private final WebSocketProxyHandler proxyHandler;
  private final LoadBalancer loadBalancer;
  private final GeoLocationService geoLocationService;
  private final ProxyProperties properties;
  private final RedisTemplate<String, Object> redisTemplate;

  /**
   * 获取代理服务器状态
   */
  @GetMapping("/status")
  public ResponseEntity<Map<String, Object>> getStatus() {
    try {
      Map<String, Object> status = new HashMap<>();
      
      // 服务器信息
      Map<String, Object> serverInfo = new HashMap<>();
      serverInfo.put("id", properties.getServerId());
      serverInfo.put("address", properties.getServerAddress());
      serverInfo.put("port", properties.getServerPort());
      serverInfo.put("region", properties.getRegion());
      serverInfo.put("maxConnections", properties.getMaxConnections());
      serverInfo.put("loadBalanceStrategy", properties.getLoadBalanceStrategy());
      status.put("server", serverInfo);
      
      // 连接统计
      status.put("connections", proxyHandler.getConnectionStats());
      
      // 区域服务器信息
      Map<String, Object> regionServers = new HashMap<>();
      Set<String> regions = redisTemplate.keys(String.format(WSServerConnection.WS_REGION, "*"));
      if (regions != null) {
        for (String regionKey : regions) {
          String region = regionKey.split(":")[2];
          Set<Object> servers = redisTemplate.opsForSet().members(regionKey);
          regionServers.put(region, servers);
        }
      }
      status.put("regionServers", regionServers);
      
      return ResponseEntity.ok(status);
    } catch (Exception e) {
      log.error("获取代理服务器状态失败: {}", e.getMessage(), e);
      return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
    }
  }
  
  /**
   * 测试IP地理位置
   */
  @GetMapping("/test-ip")
  public ResponseEntity<Map<String, Object>> testIp(@RequestParam String ip) {
    Map<String, Object> result = new HashMap<>();
    
    try {
      Map<String, String> locationDetail = geoLocationService.getLocationDetail(ip);
      String region = geoLocationService.getRegion(ip);
      
      result.put("ip", ip);
      result.put("location", locationDetail);
      result.put("region", region);
      
      // 获取推荐的服务器
      Integer recommendedServer = loadBalancer.selectTargetServer(region);
      result.put("recommendedServer", recommendedServer);
      
      // 获取服务器信息
      if (recommendedServer != null) {
        String infoKey = String.format(WSServerConnection.WS_INFO, recommendedServer);
        Map<Object, Object> serverInfo = redisTemplate.opsForHash().entries(infoKey);
        result.put("serverInfo", serverInfo);
      }
      
      log.info("IP测试: {}, 位置: {}, 区域: {}, 推荐服务器: {}", 
          ip, 
          locationDetail, 
          region, 
          recommendedServer);
      
      return ResponseEntity.ok(result);
    } catch (Exception e) {
      log.error("IP测试失败: {}", e.getMessage(), e);
      result.put("error", e.getMessage());
      return ResponseEntity.badRequest().body(result);
    }
  }
  
  /**
   * 获取服务器信息
   */
  @GetMapping("/servers")
  public ResponseEntity<Map<String, Object>> getServers() {
    try {
      Map<String, Object> result = new HashMap<>();
      
      // 获取所有服务器信息
      Set<String> infoKeys = redisTemplate.keys(String.format(WSServerConnection.WS_INFO, "*"));
      if (infoKeys != null) {
        Map<String, Object> servers = new HashMap<>();
        for (String key : infoKeys) {
          String serverId = key.split(":")[2];
          Map<Object, Object> serverInfo = redisTemplate.opsForHash().entries(key);
          
          // 获取服务器连接数
          String connKey = String.format(WSServerConnection.WS_CONNECTION, serverId);
          Object connections = redisTemplate.opsForValue().get(connKey);
          if (connections != null) {
            serverInfo.put("connections", connections);
          }
          
          // 获取服务器延迟
          String latKey = String.format(WSServerConnection.WS_LATENCY, serverId);
          Object latency = redisTemplate.opsForValue().get(latKey);
          if (latency != null) {
            serverInfo.put("latency", latency);
          }
          
          // 获取服务器健康状态
          String healthKey = String.format(WSServerConnection.WS_HEALTH, serverId);
          Object health = redisTemplate.opsForValue().get(healthKey);
          if (health != null) {
            serverInfo.put("health", health);
          } else {
            serverInfo.put("health", WSServerConnection.HEALTH_UNKNOWN);
          }
          
          servers.put(serverId, serverInfo);
        }
        result.put("servers", servers);
      }
      
      return ResponseEntity.ok(result);
    } catch (Exception e) {
      log.error("获取服务器信息失败: {}", e.getMessage(), e);
      return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
    }
  }
  
  /**
   * 获取特定服务器信息
   */
  @GetMapping("/servers/{serverId}")
  public ResponseEntity<Map<String, Object>> getServerInfo(@PathVariable int serverId) {
    try {
      Map<String, Object> result = new HashMap<>();
      
      // 获取服务器信息
      String infoKey = String.format(WSServerConnection.WS_INFO, serverId);
      Map<Object, Object> serverInfo = redisTemplate.opsForHash().entries(infoKey);
      
      if (serverInfo == null || serverInfo.isEmpty()) {
        return ResponseEntity.notFound().build();
      }
      
      result.putAll(serverInfo.entrySet().stream()
          .collect(HashMap::new, (m, e) -> m.put(e.getKey().toString(), e.getValue()), HashMap::putAll));
      
      // 获取服务器连接数
      String connKey = String.format(WSServerConnection.WS_CONNECTION, serverId);
      Object connections = redisTemplate.opsForValue().get(connKey);
      if (connections != null) {
        result.put("connections", connections);
      }
      
      // 获取服务器延迟
      String latKey = String.format(WSServerConnection.WS_LATENCY, serverId);
      Object latency = redisTemplate.opsForValue().get(latKey);
      if (latency != null) {
        result.put("latency", latency);
      }
      
      // 获取服务器健康状态
      String healthKey = String.format(WSServerConnection.WS_HEALTH, serverId);
      Object health = redisTemplate.opsForValue().get(healthKey);
      if (health != null) {
        result.put("health", health);
      } else {
        result.put("health", WSServerConnection.HEALTH_UNKNOWN);
      }
      
      return ResponseEntity.ok(result);
    } catch (Exception e) {
      log.error("获取服务器信息失败: {}", e.getMessage(), e);
      return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
    }
  }
  
  /**
   * 注册当前服务器
   */
  @PostMapping("/register")
  public ResponseEntity<Map<String, Object>> registerServer() {
    try {
      loadBalancer.registerServer();
      
      Map<String, Object> result = new HashMap<>();
      result.put("status", "success");
      result.put("serverId", properties.getServerId());
      result.put("region", properties.getRegion());
      
      return ResponseEntity.ok(result);
    } catch (Exception e) {
      log.error("注册服务器失败: {}", e.getMessage(), e);
      return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
    }
  }
  
  /**
   * 健康检查
   */
  @GetMapping("/health")
  public ResponseEntity<Map<String, Object>> health() {
    Map<String, Object> health = new HashMap<>();
    health.put("status", WSServerConnection.HEALTH_UP);
    health.put("serverId", properties.getServerId());
    health.put("region", properties.getRegion());
    health.put("connections", proxyHandler.getConnectionStats().get("totalConnections"));
    health.put("timestamp", System.currentTimeMillis());
    
    return ResponseEntity.ok(health);
  }
} 