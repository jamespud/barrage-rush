package com.spud.barrage.proxy.service;

import com.spud.barrage.proxy.config.ProxyProperties;
import com.spud.barrage.proxy.config.WSServerConnection;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

/**
 * WebSocket代理处理器
 *
 * @author Spud
 * @date 2025/3/15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketProxyHandler extends AbstractWebSocketHandler {

  private final GeoLocationService geoLocationService;
  private final LoadBalancer loadBalancer;
  private final ProxyProperties properties;
  private final RedisTemplate<String, Object> redisTemplate;

  // 存储活跃连接
  private final Map<String, WebSocketSession> activeConnections = new ConcurrentHashMap<>();

  // 存储连接的地理位置信息
  private final Map<String, Map<String, String>> connectionLocations = new ConcurrentHashMap<>();

  // 存储连接的目标服务器
  private final Map<String, Integer> connectionTargets = new ConcurrentHashMap<>();

  // 存储连接的建立时间
  private final Map<String, Long> connectionTimes = new ConcurrentHashMap<>();

  // 存储各区域连接数统计
  private final Map<String, AtomicInteger> regionConnectionCounts = new ConcurrentHashMap<>();

  // 存储各省份连接数统计
  private final Map<String, AtomicInteger> provinceConnectionCounts = new ConcurrentHashMap<>();

  // 存储各城市连接数统计
  private final Map<String, AtomicInteger> cityConnectionCounts = new ConcurrentHashMap<>();

  @Override
  public void afterConnectionEstablished(WebSocketSession session) {
    try {
      // 获取客户端IP
      String clientIp = getClientIp(session);

      // 获取详细位置信息
      Map<String, String> locationDetail = geoLocationService.getLocationDetail(clientIp);
      String region = geoLocationService.getRegion(clientIp);

      // 记录连接信息
      if (properties.getGeoLocation().isLogDetailedLocation()) {
        log.info("新连接: IP={}, 国家={}, 省份={}, 城市={}, 区域={}",
            clientIp,
            locationDetail.get("country"),
            locationDetail.get("province"),
            locationDetail.get("city"),
            region);
      } else {
        log.info("新连接: IP={}, 区域={}", clientIp, region);
      }

      // 更新区域连接数统计
      regionConnectionCounts.computeIfAbsent(region, k -> new AtomicInteger(0))
          .incrementAndGet();

      // 更新省份连接数统计
      String province = locationDetail.get("province");
      if (province != null && !province.equals("未知")) {
        provinceConnectionCounts.computeIfAbsent(province, k -> new AtomicInteger(0))
            .incrementAndGet();
      }

      // 更新城市连接数统计
      String city = locationDetail.get("city");
      if (city != null && !city.equals("未知")) {
        cityConnectionCounts.computeIfAbsent(city, k -> new AtomicInteger(0))
            .incrementAndGet();
      }

      // 选择目标服务器
      Integer targetServerId = loadBalancer.selectTargetServer(region);
      if (targetServerId == null) {
        log.warn("无法找到可用的目标服务器，区域: {}", region);
        session.close(CloseStatus.NOT_ACCEPTABLE);
        return;
      }

      // 获取目标服务器信息
      String serverInfoKey = String.format(WSServerConnection.WS_INFO, targetServerId);
      Map<Object, Object> serverInfo = redisTemplate.opsForHash().entries(serverInfoKey);

      if (serverInfo == null || serverInfo.isEmpty()) {
        log.warn("无法获取目标服务器信息，服务器ID: {}", targetServerId);
        session.close(CloseStatus.NOT_ACCEPTABLE);
        return;
      }

      String targetAddress = serverInfo.get("address").toString();
      int targetPort = Integer.parseInt(serverInfo.get("port").toString());

      // 构建目标WebSocket URL
      String targetUrl = String.format(WSServerConnection.WS_URL_TEMPLATE, targetAddress,
          targetPort);

      // 建立到目标服务器的连接
      WebSocketClient targetClient = new StandardWebSocketClient();
      WebSocketSession targetSession = targetClient.execute(
          new TargetWebSocketHandler(session),
          String.valueOf(session.getHandshakeHeaders()),
          URI.create(targetUrl)
      ).get();

      // 存储连接映射
      activeConnections.put(session.getId(), targetSession);

      // 存储连接的地理位置信息
      connectionLocations.put(session.getId(), locationDetail);

      // 存储连接的目标服务器
      connectionTargets.put(session.getId(), targetServerId);

      // 存储连接的建立时间
      connectionTimes.put(session.getId(), System.currentTimeMillis());

      // 更新服务器状态
      loadBalancer.updateServerStatus(
          properties.getServerId(),
          activeConnections.size(),
          measureLatency(targetAddress, targetPort)
      );

      // 添加连接信息到会话属性
      if (properties.getGeoLocation().isAddLocationToSession()) {
        session.getAttributes().put("country", locationDetail.get("country"));
        session.getAttributes().put("province", locationDetail.get("province"));
        session.getAttributes().put("city", locationDetail.get("city"));
        session.getAttributes().put("region", region);
        session.getAttributes().put("ip", clientIp);
        session.getAttributes().put("targetServer", targetServerId);
      }

      log.info("代理连接已建立: {} -> {}:{} ({})",
          clientIp,
          targetAddress,
          targetPort,
          formatLocation(locationDetail));

    } catch (Exception e) {
      log.error("建立代理连接失败: {}", e.getMessage(), e);
      try {
        session.close(CloseStatus.SERVER_ERROR);
      } catch (IOException ex) {
        log.error("关闭连接失败", ex);
      }
    }
  }

  @Override
  public void handleTextMessage(WebSocketSession session, TextMessage message) {
    try {
      WebSocketSession targetSession = activeConnections.get(session.getId());
      if (targetSession != null && targetSession.isOpen()) {
        targetSession.sendMessage(message);
      } else {
        log.warn("目标会话不存在或已关闭，无法转发消息");
        session.close(CloseStatus.SERVER_ERROR);
      }
    } catch (Exception e) {
      log.error("转发文本消息失败: {}", e.getMessage(), e);
      try {
        session.close(CloseStatus.SERVER_ERROR);
      } catch (IOException ex) {
        log.error("关闭连接失败", ex);
      }
    }
  }

  @Override
  public void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
    try {
      WebSocketSession targetSession = activeConnections.get(session.getId());
      if (targetSession != null && targetSession.isOpen()) {
        targetSession.sendMessage(message);
      } else {
        log.warn("目标会话不存在或已关闭，无法转发二进制消息");
        session.close(CloseStatus.SERVER_ERROR);
      }
    } catch (Exception e) {
      log.error("转发二进制消息失败: {}", e.getMessage(), e);
      try {
        session.close(CloseStatus.SERVER_ERROR);
      } catch (IOException ex) {
        log.error("关闭连接失败", ex);
      }
    }
  }

  @Override
  public void handlePongMessage(WebSocketSession session, PongMessage message) {
    try {
      WebSocketSession targetSession = activeConnections.get(session.getId());
      if (targetSession != null && targetSession.isOpen()) {
        targetSession.sendMessage(message);
      }
    } catch (Exception e) {
      log.error("转发Pong消息失败: {}", e.getMessage(), e);
    }
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
    try {
      // 获取并关闭目标会话
      WebSocketSession targetSession = activeConnections.remove(session.getId());
      if (targetSession != null && targetSession.isOpen()) {
        targetSession.close(status);
      }

      // 获取连接信息
      Map<String, String> locationDetail = connectionLocations.remove(session.getId());
      Integer targetServerId = connectionTargets.remove(session.getId());
      Long connectionTime = connectionTimes.remove(session.getId());

      // 计算连接持续时间
      long duration = 0;
      if (connectionTime != null) {
        duration = System.currentTimeMillis() - connectionTime;
      }

      // 更新统计信息
      if (locationDetail != null) {
        // 更新区域连接数统计
        String region = locationDetail.get("region");
        if (region != null) {
          AtomicInteger count = regionConnectionCounts.get(region);
          if (count != null) {
            count.decrementAndGet();
          }
        }

        // 更新省份连接数统计
        String province = locationDetail.get("province");
        if (province != null && !province.equals("未知")) {
          AtomicInteger count = provinceConnectionCounts.get(province);
          if (count != null) {
            count.decrementAndGet();
          }
        }

        // 更新城市连接数统计
        String city = locationDetail.get("city");
        if (city != null && !city.equals("未知")) {
          AtomicInteger count = cityConnectionCounts.get(city);
          if (count != null) {
            count.decrementAndGet();
          }
        }
      }

      // 更新服务器状态
      loadBalancer.updateServerStatus(
          properties.getServerId(),
          activeConnections.size(),
          0 // 连接关闭时不需要测量延迟
      );

      // 记录连接关闭信息
      if (locationDetail != null) {
        log.info("连接关闭: {} -> {}, 持续时间: {}秒, 状态: {}",
            formatLocation(locationDetail),
            targetServerId,
            duration / 1000,
            status);
      } else {
        log.info("连接关闭: {}, 持续时间: {}秒, 状态: {}",
            session.getId(),
            duration / 1000,
            status);
      }

    } catch (Exception e) {
      log.error("关闭代理连接失败: {}", e.getMessage(), e);
    }
  }

  @Override
  public void handleTransportError(WebSocketSession session, Throwable exception) {
    log.error("WebSocket传输错误: {}", exception.getMessage(), exception);
    try {
      session.close(CloseStatus.SERVER_ERROR);
    } catch (IOException e) {
      log.error("关闭连接失败", e);
    }
  }

  /**
   * 获取客户端IP
   */
  private String getClientIp(WebSocketSession session) {
    HttpHeaders headers = session.getHandshakeHeaders();
    String forwardedFor = headers.getFirst("X-Forwarded-For");
    if (forwardedFor != null) {
      return forwardedFor.split(",")[0].trim();
    }
    return session.getRemoteAddress().getAddress().getHostAddress();
  }

  /**
   * 测量服务器延迟
   */
  private int measureLatency(String server, int port) {
    try {
      long start = System.currentTimeMillis();
      URL url = new URL(String.format(WSServerConnection.WS_HEALTH_URL_TEMPLATE, server, port));
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setConnectTimeout(1000);
      conn.setReadTimeout(1000);
      conn.getResponseCode();
      return (int) (System.currentTimeMillis() - start);
    } catch (Exception e) {
      log.warn("测量服务器延迟失败: {}", e.getMessage());
      return Integer.MAX_VALUE;
    }
  }

  /**
   * 格式化位置信息
   */
  private String formatLocation(Map<String, String> locationDetail) {
    if (locationDetail == null) {
      return "未知位置";
    }

    String country = locationDetail.get("country");
    String province = locationDetail.get("province");
    String city = locationDetail.get("city");

    if ("中国".equals(country)) {
      if (city != null && !city.equals("未知")) {
        if (province != null && !province.equals("未知") && !city.equals(province)) {
          return province + "-" + city;
        }
        return city;
      }
      return province != null ? province : country;
    }

    return country + (city != null && !city.equals("未知") ? "-" + city : "");
  }

  /**
   * 获取当前连接统计信息
   */
  public Map<String, Object> getConnectionStats() {
    Map<String, Object> stats = new HashMap<>();

    // 总连接数
    stats.put("totalConnections", activeConnections.size());

    // 各区域连接数
    Map<String, Integer> regionStats = new HashMap<>();
    for (Map.Entry<String, AtomicInteger> entry : regionConnectionCounts.entrySet()) {
      regionStats.put(entry.getKey(), entry.getValue().get());
    }
    stats.put("regionConnections", regionStats);

    // 各省份连接数
    Map<String, Integer> provinceStats = new HashMap<>();
    for (Map.Entry<String, AtomicInteger> entry : provinceConnectionCounts.entrySet()) {
      provinceStats.put(entry.getKey(), entry.getValue().get());
    }
    stats.put("provinceConnections", provinceStats);

    // 各城市连接数
    Map<String, Integer> cityStats = new HashMap<>();
    for (Map.Entry<String, AtomicInteger> entry : cityConnectionCounts.entrySet()) {
      cityStats.put(entry.getKey(), entry.getValue().get());
    }
    stats.put("cityConnections", cityStats);

    return stats;
  }

  /**
   * 目标WebSocket处理器
   */
  private static class TargetWebSocketHandler extends AbstractWebSocketHandler {

    private final WebSocketSession clientSession;

    public TargetWebSocketHandler(WebSocketSession clientSession) {
      this.clientSession = clientSession;
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message)
        throws IOException {
      if (clientSession.isOpen()) {
        clientSession.sendMessage(message);
      }
    }

    @Override
    public void handleBinaryMessage(WebSocketSession session, BinaryMessage message)
        throws IOException {
      if (clientSession.isOpen()) {
        clientSession.sendMessage(message);
      }
    }

    @Override
    public void handlePongMessage(WebSocketSession session, PongMessage message)
        throws IOException {
      if (clientSession.isOpen()) {
        clientSession.sendMessage(message);
      }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status)
        throws Exception {
      if (clientSession.isOpen()) {
        clientSession.close(status);
      }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception)
        throws Exception {
      if (clientSession.isOpen()) {
        clientSession.close(CloseStatus.SERVER_ERROR);
      }
    }
  }
} 