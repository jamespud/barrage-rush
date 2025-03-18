package com.spud.barrage.common.data.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import java.util.concurrent.TimeUnit;

/**
 * @author Spud
 * @date 2025/3/18
 */
public class ElementCache {
  // 定义缓存：Key 为元素唯一标识，Value 为元素值
  private final Cache<String, String> cache;

  public ElementCache() {
    this.cache = Caffeine.newBuilder()
        .expireAfter(new Expiry<String, String>() {
          // 动态设置每个元素的过期时间
          @Override
          public long expireAfterCreate(String key, String value, long currentTime) {
            long expirationMinutes = getExpirationTimeForElement(value);
            return TimeUnit.MINUTES.toNanos(expirationMinutes);
          }

          @Override
          public long expireAfterUpdate(String key, String value, long currentTime, long currentDuration) {
            return currentDuration; // 更新时不改变过期时间
          }

          @Override
          public long expireAfterRead(String key, String value, long currentTime, long currentDuration) {
            return currentDuration; // 读取时不刷新过期时间
          }

          private long getExpirationTimeForElement(String value) {
            // 根据元素内容返回不同的过期时间（示例逻辑）
            if (value.contains("urgent")) {
              return 1; // 1分钟过期
            } else {
              return 5; // 5分钟过期
            }
          }
        })
        .build();
  }
}