package com.spud.barrage.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author Spud
 * @date 2025/3/6
 */
@Configuration
@EnableScheduling
public class ApplicationConfig {

  @Bean
  public ObjectMapper objectMapper() {
    return new ObjectMapper();
  }
}