package com.spud.barrage.connection.controller;

import com.spud.barrage.connection.config.ConnectionProperties;
import com.spud.barrage.connection.service.ConnectionMonitorService;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 监控控制器
 *
 * @author Spud
 * @date 2025/3/15
 */
@Slf4j
@RestController
@RequestMapping("/api/monitor")
@RequiredArgsConstructor
public class MonitorController {

  private final ConnectionMonitorService monitorService;
  private final ConnectionProperties properties;

  /**
   * 获取连接统计信息
   */
  @GetMapping("/connections")
  public ResponseEntity<Map<String, Object>> getConnectionStats() {
    Map<String, Object> result = new HashMap<>();

    // 服务器信息
    Map<String, Object> serverInfo = new HashMap<>();
    serverInfo.put("id", properties.getServerId());
    serverInfo.put("address", properties.getServerAddress());
    serverInfo.put("port", properties.getServerPort());
    serverInfo.put("region", properties.getRegion());
    serverInfo.put("maxConnections", properties.getMaxConnections());
    result.put("server", serverInfo);

    // 连接统计信息
    result.put("stats", monitorService.getConnectionStats());

    // 配置信息
    Map<String, Object> configInfo = new HashMap<>();
    configInfo.put("heartbeatTimeout", properties.getHeartbeatTimeout());
    configInfo.put("heartbeatInterval", properties.getHeartbeatInterval());
    configInfo.put("connectionTimeout", properties.getConnectionTimeout());
    configInfo.put("messageBufferSize", properties.getMessageBufferSize());
    configInfo.put("messageSendInterval", properties.getMessageSendInterval());
    result.put("config", configInfo);

    return ResponseEntity.ok(result);
  }

  /**
   * 获取系统信息
   */
  @GetMapping("/system")
  public ResponseEntity<Map<String, Object>> getSystemInfo() {
    Map<String, Object> result = new HashMap<>();

    // JVM信息
    Map<String, Object> jvmInfo = new HashMap<>();
    Runtime runtime = Runtime.getRuntime();
    jvmInfo.put("maxMemory", runtime.maxMemory());
    jvmInfo.put("totalMemory", runtime.totalMemory());
    jvmInfo.put("freeMemory", runtime.freeMemory());
    jvmInfo.put("usedMemory", runtime.totalMemory() - runtime.freeMemory());
    jvmInfo.put("availableProcessors", runtime.availableProcessors());
    result.put("jvm", jvmInfo);

    // 系统信息
    Map<String, Object> systemInfo = new HashMap<>();
    systemInfo.put("javaVersion", System.getProperty("java.version"));
    systemInfo.put("javaVendor", System.getProperty("java.vendor"));
    systemInfo.put("osName", System.getProperty("os.name"));
    systemInfo.put("osVersion", System.getProperty("os.version"));
    systemInfo.put("osArch", System.getProperty("os.arch"));
    systemInfo.put("userDir", System.getProperty("user.dir"));
    result.put("system", systemInfo);

    return ResponseEntity.ok(result);
  }
} 