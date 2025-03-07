package com.spud.barrage;

import com.spud.barrage.config.DanmakuProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * @author Spud
 * @date 2025/3/3
 */
@SpringBootApplication
@EnableConfigurationProperties(DanmakuProperties.class)
public class BarrageRushApplication {

  public static void main(String[] args) {
    SpringApplication.run(BarrageRushApplication.class, args);
  }
}