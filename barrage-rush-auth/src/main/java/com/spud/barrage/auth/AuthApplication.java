package com.spud.barrage.auth;

import com.spud.barrage.auth.config.AuthProperties;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * @author Spud
 * @date 2025/3/7
 */
@EnableConfigurationProperties({AuthProperties.class})
@SpringBootApplication
public class AuthApplication {

  public static void main(String[] args) {

  }
}
