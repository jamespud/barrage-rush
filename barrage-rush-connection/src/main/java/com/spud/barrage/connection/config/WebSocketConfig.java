package com.spud.barrage.connection.config;

import com.spud.barrage.connection.websocket.DataWebSocketHandler;
import com.spud.barrage.connection.websocket.HeartbeatWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket配置
 *
 * @author Spud
 * @date 2025/3/15
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

  private final DataWebSocketHandler dataWebSocketHandler;
  private final HeartbeatWebSocketHandler heartbeatWebSocketHandler;

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    // 数据连接
    registry.addHandler(dataWebSocketHandler, "/ws/data/{roomId}/{userId}")
        .setAllowedOrigins("*");

    // 心跳连接
    registry.addHandler(heartbeatWebSocketHandler, "/ws/heartbeat/{roomId}/{userId}")
        .setAllowedOrigins("*");
  }
} 