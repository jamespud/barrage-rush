package com.spud.barrage.proxy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 弹幕系统连接代理服务启动类
 *
 * @author Spud
 * @date 2025/3/30
 */
@SpringBootApplication(scanBasePackages = {"com.spud.barrage"})
@EnableScheduling
public class ProxyApplication {

  public static void main(String[] args) {
    SpringApplication.run(ProxyApplication.class, args);
  }
}