package com.spud.barrage.proxy.config;

import com.spud.barrage.proxy.service.WebSocketProxyHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * @author Spud
 * @date 2025/3/15
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

  private final WebSocketProxyHandler proxyHandler;

  public WebSocketConfig(WebSocketProxyHandler proxyHandler) {
    this.proxyHandler = proxyHandler;
  }

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry.addHandler(proxyHandler, "/ws")
        .setAllowedOrigins("*");
  }
} 