package com.spud.barrage.proxy.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置类
 *
 * @author Spud
 * @date 2025/3/30
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

  /**
   * 配置跨域
   */
  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/**")
        .allowedOrigins("*")
        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .maxAge(3600);
  }
}