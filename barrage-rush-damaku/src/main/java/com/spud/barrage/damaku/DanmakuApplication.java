package com.spud.barrage.damaku;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author Spud
 * @date 2025/3/23
 */
@SpringBootApplication(scanBasePackages = "com.spud.barrage")
public class DanmakuApplication {

  public static void main(String[] args) {
    SpringApplication.run(DanmakuApplication.class, args);
  }

}
