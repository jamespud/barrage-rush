package com.spud.barrage.push;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 弹幕连接服务启动类
 *
 * @author Spud
 * @date 2025/3/15
 */
@SpringBootApplication
@EnableScheduling
public class ConnectionApplication {

  public static void main(String[] args) {
    SpringApplication.run(ConnectionApplication.class, args);
  }
}