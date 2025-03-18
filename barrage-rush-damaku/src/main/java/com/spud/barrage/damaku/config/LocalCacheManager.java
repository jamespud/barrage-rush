package com.spud.barrage.damaku.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

/**
 * @author Spud
 * @date 2025/3/17
 */
@Configuration
@EnableCaching
public class LocalCacheManager {
  @Bean
  public RedisConnectionFactory redisConnectionFactory() {
    return new LettuceConnectionFactory();
  }

  @Bean
  @Primary  // 设为默认缓存管理器
  public CacheManager cacheManager() {
    CaffeineCacheManager cacheManager = new CaffeineCacheManager();
    cacheManager.setCaffeine(Caffeine.newBuilder()
        // 初始容量
        .initialCapacity(100)
        // 最大缓存条目数
        .maximumSize(500)
        // 写入后过期时间
        .expireAfterWrite(1, TimeUnit.MINUTES));
    return cacheManager;
  }

}