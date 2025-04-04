package com.spud.barrage.common.data.config;

import java.util.Collections;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

/**
 * Redis分布式锁配置
 *
 * @author Spud
 * @date 2025/4/01
 */
@Component
@Slf4j
public class RedisLockUtils {

  private static final String LOCK_SUCCESS = "OK";
  private static final Long RELEASE_SUCCESS = 1L;
  private static final String LOCK_LUA_SCRIPT = "if redis.call('setnx', KEYS[1], ARGV[1]) == 1 then redis.call('pexpire', KEYS[1], ARGV[2]) return 'OK' else return redis.call('get', KEYS[1]) end";
  private static final String UNLOCK_LUA_SCRIPT = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";

  @Autowired
  private RedisTemplate<String, Object> redisTemplate;

  private final ThreadLocal<String> lockFlag = new ThreadLocal<>();

  /**
   * 尝试获取分布式锁
   *
   * @param lockKey       锁键
   * @param timeoutMillis 超时时间（毫秒）
   * @return 是否成功获取锁
   */
  public boolean tryLock(String lockKey, long timeoutMillis) {
    String requestId = UUID.randomUUID().toString();
    lockFlag.set(requestId);

    try {
      DefaultRedisScript<String> lockScript = new DefaultRedisScript<>(LOCK_LUA_SCRIPT,
          String.class);
      String result = redisTemplate.execute(
          lockScript,
          Collections.singletonList(lockKey),
          requestId,
          String.valueOf(timeoutMillis));

      boolean locked = LOCK_SUCCESS.equals(result);
      if (locked) {
        log.debug("获取锁成功: {}, 线程: {}", lockKey, Thread.currentThread().getName());
      } else {
        log.debug("获取锁失败: {}, 锁已被其他线程持有", lockKey);
      }
      return locked;
    } catch (Exception e) {
      log.error("获取锁异常: " + lockKey, e);
      return false;
    }
  }

  /**
   * 释放分布式锁
   *
   * @param lockKey 锁键
   * @return 是否成功释放锁
   */
  public boolean unlock(String lockKey) {
    String requestId = lockFlag.get();
    if (requestId == null) {
      log.warn("未找到锁标识，无法释放锁: {}", lockKey);
      return false;
    }

    try {
      DefaultRedisScript<Long> unlockScript = new DefaultRedisScript<>(UNLOCK_LUA_SCRIPT,
          Long.class);
      Long result = redisTemplate.execute(
          unlockScript,
          Collections.singletonList(lockKey),
          requestId);

      boolean unlocked = RELEASE_SUCCESS.equals(result);
      if (unlocked) {
        log.debug("释放锁成功: {}, 线程: {}", lockKey, Thread.currentThread().getName());
      } else {
        log.warn("释放锁失败: {}, 可能已超时或被其他线程释放", lockKey);
      }
      lockFlag.remove();
      return unlocked;
    } catch (Exception e) {
      log.error("释放锁异常: " + lockKey, e);
      return false;
    }
  }
}