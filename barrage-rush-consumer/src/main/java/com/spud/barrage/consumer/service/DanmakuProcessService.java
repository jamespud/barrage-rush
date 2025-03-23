package com.spud.barrage.consumer.service;

import com.spud.barrage.common.data.dto.DanmakuMessage;
import com.spud.barrage.common.data.repository.DanmakuRepository;
import com.spud.barrage.constant.ApiConstants;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DanmakuProcessService {

  private final RedisTemplate<String, Object> redisTemplate;
  private final DanmakuRepository danmakuRepository;
  private final PushNotificationService pushNotificationService;

  @Value("${danmaku.storage.ttl:3600}")
  private long storageTtl;

  @Value("${danmaku.storage.sample-rate:0.2}")
  private double sampleRate;

  @Value("${danmaku.storage.hot-room-threshold:1000}")
  private int hotRoomThreshold;

  /**
   * 处理弹幕消息
   */
  public void processDanmaku(DanmakuMessage message) {
    try {
      // 1. 保存到Redis
      saveToRedis(message);

      // 2. 决定是否保存到MySQL
      if (shouldSaveToDatabase(message)) {
        saveToDatabase(message);
      }

      // 3. 推送通知
      pushNotificationService.pushDanmakuMessage(message);

      log.debug("Processed danmaku: room={}, user={}", message.getRoomId(), message.getUserId());
    } catch (Exception e) {
      log.error("Failed to process danmaku: {}", e.getMessage(), e);
    }
  }

  /**
   * 保存消息到Redis
   */
  private void saveToRedis(DanmakuMessage message) {
    String key = String.format(ApiConstants.REDIS_ROOM_MESSAGES, message.getRoomId());

    redisTemplate.opsForZSet().add(key, message, message.getTimestamp());
    redisTemplate.expire(key, storageTtl, TimeUnit.SECONDS);

    // 更新房间最后活跃时间
    String lastActiveKey = String.format("room:%s:lastActive", message.getRoomId());
    redisTemplate.opsForValue().set(lastActiveKey, System.currentTimeMillis());
  }

  /**
   * 保存消息到数据库
   */
  private void saveToDatabase(DanmakuMessage message) {
    try {
      danmakuRepository.save(message);
    } catch (Exception e) {
      log.error("Failed to save danmaku to database: {}", e.getMessage(), e);
    }
  }

  /**
   * 决定是否保存到数据库 根据不同的策略： 1. 热门房间：使用抽样保存 2. 普通/冷门房间：全量保存
   */
  private boolean shouldSaveToDatabase(DanmakuMessage message) {
    // 获取房间当前观众数
    String viewerKey = String.format("room:%s:viewers", message.getRoomId());
    Object viewersObj = redisTemplate.opsForValue().get(viewerKey);

    int viewers = 0;
    if (viewersObj != null) {
      try {
        viewers = Integer.parseInt(viewersObj.toString());
      } catch (NumberFormatException e) {
        log.error("Invalid viewers count: {}", viewersObj);
      }
    }

    // 热门房间使用抽样策略
    if (viewers >= hotRoomThreshold) {
      return Math.random() <= sampleRate;
    }

    // 普通/冷门房间全量保存
    return true;
  }
} 