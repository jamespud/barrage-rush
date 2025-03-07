package com.spud.barrage.config;

import com.spud.barrage.common.constant.ApiConstants;
import com.spud.barrage.websocket.DanmakuHandshakeInterceptor;
import com.spud.barrage.websocket.DanmakuWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

/**
 * @author Spud
 * @date 2025/3/4
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry.addHandler(danmakuWebSocketHandler(), ApiConstants.WS_ENDPOINT)
        .addInterceptors(new DanmakuHandshakeInterceptor())
        // 生产环境需要限制允许的域名
        .setAllowedOrigins("*");
  }

  @Bean
  public DanmakuWebSocketHandler danmakuWebSocketHandler() {
    return new DanmakuWebSocketHandler();
  }

  @Bean
  public ServletServerContainerFactoryBean createWebSocketContainer() {
    ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
    container.setMaxTextMessageBufferSize(8192);
    container.setMaxBinaryMessageBufferSize(8192);
    // 15分钟超时
    container.setMaxSessionIdleTimeout(15 * 60 * 1000L);
    return container;
  }
}