package com.spud.barrage.push.config;

/**
 * @author Spud
 * @date 2025/3/30
 */

import com.spud.barrage.push.handler.CdnWebSocketHandler;
import com.spud.barrage.push.handler.DanmakuWebSocketHandler;
import com.spud.barrage.push.handler.HeartbeatWebSocketHandler;
import com.spud.barrage.push.interceptor.WebSocketInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

/**
 * WebSocket配置类
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

  @Autowired
  private DanmakuWebSocketHandler danmakuWebSocketHandler;

  @Autowired
  private WebSocketInterceptor webSocketInterceptor;

  @Autowired
  private HeartbeatWebSocketHandler heartbeatWebSocketHandler;

  @Autowired
  private CdnWebSocketHandler cdnWebSocketHandler;

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    // 注册弹幕处理器
    registry.addHandler(danmakuWebSocketHandler, "/ws/danmaku/{roomId}")
        .addInterceptors(webSocketInterceptor)
        .setAllowedOriginPatterns("*");
    // 注册心跳处理器
    registry.addHandler(heartbeatWebSocketHandler, "/ws/heartbeat")
        .addInterceptors(webSocketInterceptor)
        .setAllowedOriginPatterns("*");

    // 注册CDN处理器
    registry.addHandler(cdnWebSocketHandler, "/ws/cdn")
        .addInterceptors(webSocketInterceptor)
        .setAllowedOriginPatterns("*");

  }

  /**
   * 配置WebSocket服务器容器
   * 设置消息缓冲区大小、空闲超时时间和最大消息大小
   */
  @Bean
  public ServletServerContainerFactoryBean createWebSocketContainer() {
    ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
    // 设置消息缓冲区大小 8MB
    container.setMaxTextMessageBufferSize(8 * 1024 * 1024);
    // 设置二进制消息缓冲区大小 16MB
    container.setMaxBinaryMessageBufferSize(16 * 1024 * 1024);
    // 设置空闲超时时间 60秒
    container.setMaxSessionIdleTimeout(60000L);
    return container;
  }
}