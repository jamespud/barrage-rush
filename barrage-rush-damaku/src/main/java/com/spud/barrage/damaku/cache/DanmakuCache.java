package com.spud.barrage.damaku.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;

/**
 * @author Spud
 * @date 2025/3/11
 */
public class DanmakuCache<T> {

  private final Cache<String, T> recentDanmakuCache = Caffeine.newBuilder()
      .expireAfterWrite(10, TimeUnit.SECONDS)
      .maximumSize(10000)
      .build();
}
