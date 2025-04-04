package com.spud.barrage.proxy.service;

import com.spud.barrage.proxy.model.ServerInstance;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 服务发现服务
 * 负责发现和管理WebSocket服务器实例
 *
 * @author Spud
 * @date 2025/3/30
 */
@Slf4j
@Service
public class ServerDiscoveryService {

  @Autowired
  private RedisTemplate<String, Object> redisTemplate;

  @Value("${barrage.discovery.active-servers-key:ws:active-servers}")
  private String activeServersKey;

  @Value("${barrage.discovery.server-prefix:ws:server:}")
  private String serverPrefix;

  @Value("${barrage.discovery.refresh-interval:10000}")
  private long refreshInterval;

  /**
   * 服务器实例缓存
   * key: 实例ID, value: 服务器实例
   */
  private final Map<String, ServerInstance> serverInstances = new ConcurrentHashMap<>();

  /**
   * 按区域组织的服务器实例
   * key: 区域, value: 实例ID列表
   */
  private final Map<String, List<String>> regionServers = new ConcurrentHashMap<>();

  @PostConstruct
  public void init() {
    // 初始化时刷新服务器列表
    refreshServerInstances();
    log.info("服务发现服务初始化完成");
  }

  @PreDestroy
  public void destroy() {
    log.info("服务发现服务关闭");
  }

  /**
   * 定期刷新服务器实例列表
   */
  @Scheduled(fixedDelayString = "${barrage.discovery.refresh-interval:10000}")
  public void refreshServerInstances() {
    try {
      // 获取所有活跃服务器ID
      Set<Object> activeServerIds = redisTemplate.opsForSet().members(activeServersKey);
      if (activeServerIds == null || activeServerIds.isEmpty()) {
        log.warn("没有发现活跃的WebSocket服务器");
        return;
      }

      // 标记当前所有已知实例为非活跃
      serverInstances.values().forEach(instance -> instance.setActive(false));

      // 更新区域服务器映射
      regionServers.clear();

      // 处理每个活跃服务器
      for (Object serverId : activeServerIds) {
        String id = serverId.toString();
        String key = serverPrefix + id;

        // 获取服务器信息
        Map<Object, Object> serverInfo = redisTemplate.opsForHash().entries(key);
        if (serverInfo.isEmpty()) {
          continue;
        }

        // 获取服务器指标
        Map<Object, Object> metricsInfo = redisTemplate.opsForHash().entries(key + ":metrics");

        // 创建或更新服务器实例
        updateServerInstance(id, serverInfo, metricsInfo);
      }

      // 移除非活跃实例
      List<String> inactiveIds = serverInstances.entrySet().stream()
          .filter(entry -> !entry.getValue().isActive())
          .map(Map.Entry::getKey)
          .collect(Collectors.toList());

      inactiveIds.forEach(serverInstances::remove);

      log.debug("刷新服务器实例完成，活跃实例: {}", serverInstances.size());
    } catch (Exception e) {
      log.error("刷新服务器实例失败", e);
    }
  }

  /**
   * 更新服务器实例
   */
  private void updateServerInstance(String id, Map<Object, Object> serverInfo,
      Map<Object, Object> metricsInfo) {
    String host = serverInfo.getOrDefault("host", "").toString();
    String type = serverInfo.getOrDefault("type", "").toString();
    String region = serverInfo.getOrDefault("region", "").toString();

    // 仅处理WebSocket推送服务器
    if (!"push-server".equals(type)) {
      return;
    }

    int port = 0;
    try {
      port = Integer.parseInt(serverInfo.getOrDefault("port", "0").toString());
    } catch (NumberFormatException e) {
      log.warn("服务器端口格式错误: {}", serverInfo.get("port"));
    }

    long startTime = 0;
    try {
      startTime = Long.parseLong(serverInfo.getOrDefault("startTime", "0").toString());
    } catch (NumberFormatException e) {
      log.warn("服务器启动时间格式错误: {}", serverInfo.get("startTime"));
    }

    long lastHeartbeat = 0;
    try {
      lastHeartbeat = Long.parseLong(serverInfo.getOrDefault("lastHeartbeat", "0").toString());
    } catch (NumberFormatException e) {
      log.warn("服务器心跳时间格式错误: {}", serverInfo.get("lastHeartbeat"));
    }

    // 转换指标信息
    Map<String, Integer> metrics = new ConcurrentHashMap<>();
    for (Map.Entry<Object, Object> entry : metricsInfo.entrySet()) {
      String metricKey = entry.getKey().toString();
      try {
        Integer metricValue = Integer.parseInt(entry.getValue().toString());
        metrics.put(metricKey, metricValue);
      } catch (NumberFormatException e) {
        log.warn("指标值格式错误: key={}, value={}", metricKey, entry.getValue());
      }
    }

    // 创建或更新服务器实例
    ServerInstance instance = serverInstances.getOrDefault(id, new ServerInstance());
    instance.setId(id);
    instance.setHost(host);
    instance.setPort(port);
    instance.setRegion(region);
    instance.setType(type);
    instance.setStartTime(startTime);
    instance.setLastHeartbeat(lastHeartbeat);
    instance.setActive(true);
    instance.setMetrics(metrics);

    // 保存到缓存
    serverInstances.put(id, instance);

    // 添加到区域映射
    regionServers.computeIfAbsent(region, k -> new ArrayList<>()).add(id);
  }

  /**
   * 获取所有可用的服务器实例
   */
  public List<ServerInstance> getAllInstances() {
    return serverInstances.values().stream()
        .filter(ServerInstance::isHealthy)
        .collect(Collectors.toList());
  }

  /**
   * 获取指定区域的服务器实例
   */
  public List<ServerInstance> getInstancesByRegion(String region) {
    List<String> instanceIds = regionServers.getOrDefault(region, new ArrayList<>());
    return instanceIds.stream()
        .map(serverInstances::get)
        .filter(instance -> instance != null && instance.isHealthy())
        .collect(Collectors.toList());
  }

  /**
   * 获取指定ID的服务器实例
   */
  public ServerInstance getInstance(String id) {
    return serverInstances.get(id);
  }

  /**
   * 选择合适的服务器实例
   * 优先选择指定区域的实例，如果没有则选择全局最佳实例
   */
  public ServerInstance selectBestInstance(String region) {
    // 获取指定区域的实例
    List<ServerInstance> regionInstances = getInstancesByRegion(region);

    // 如果区域内没有可用实例，获取所有可用实例
    if (regionInstances.isEmpty()) {
      regionInstances = getAllInstances();
    }

    // 如果仍然没有可用实例，返回null
    if (regionInstances.isEmpty()) {
      return null;
    }

    // 根据加权负载分数选择最佳实例
    return regionInstances.stream()
        .min((a, b) -> Double.compare(a.getWeightedLoadScore(), b.getWeightedLoadScore()))
        .orElse(regionInstances.get(0));
  }
}