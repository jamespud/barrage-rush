package com.spud.barrage.push.config;

import com.spud.barrage.push.websocket.DataWebSocketHandler;
import com.spud.barrage.push.websocket.HeartbeatWebSocketHandler;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * WebSocket初始化器
 *
 * @author Spud
 * @date 2025/3/15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketInitializer {

  private final DataWebSocketHandler dataWebSocketHandler;
  private final HeartbeatWebSocketHandler heartbeatWebSocketHandler;

  @PostConstruct
  public void init() {
    log.info("初始化WebSocket处理器");

    // 初始化数据WebSocket处理器
    dataWebSocketHandler.init();

    // 初始化心跳WebSocket处理器
    heartbeatWebSocketHandler.init();

    log.info("WebSocket处理器初始化完成");
  }
} 