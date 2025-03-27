package com.spud.barrage.push;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 推送服务器应用程序入口
 *
 * @author Spud
 * @date 2025/3/25
 */
@SpringBootApplication(scanBasePackages = "com.spud.barrage")
@EnableScheduling
public class PushServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(PushServerApplication.class, args);
    }
}