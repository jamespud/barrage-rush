package com.spud.barrage.damaku.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.spud.barrage.common.data.dto.DanmakuMessage;
import com.spud.barrage.common.data.entity.AnchorRoomConfig;
import com.spud.barrage.common.data.service.RoomService;
import com.spud.barrage.constant.ApiConstants;
import com.spud.barrage.constant.Constants;
import com.spud.barrage.damaku.mq.DanmakuProducer;
import com.spud.barrage.damaku.service.DanmakuService;
import com.spud.barrage.util.SnowflakeIdWorker;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * @author Spud
 * @date 2025/3/4
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DanmakuServiceImpl implements DanmakuService {

  private final DanmakuProducer danmakuProducer;
  private final RoomService roomService;
  private static final LoadingCache<Long, Cache<Long, DanmakuMessage>> messageCache = Caffeine.newBuilder()
      .maximumSize(1000)
      .expireAfterWrite(1, TimeUnit.MINUTES)
      .build(roomId -> Caffeine.newBuilder()
          .maximumSize(1000)
          .expireAfterWrite(1, TimeUnit.MINUTES)
          .build()
      );

  @Autowired
  private RedisTemplate<String, Object> redisTemplate;

  @Autowired
  private SnowflakeIdWorker snowflakeIdWorker;

  @Override
  public void processDanmaku(DanmakuMessage message) {
    // 1. 消息验证
    if (validateMessage(message)) {
      // 2. 发送到消息队列
      boolean sent = danmakuProducer.sendDanmaku(message);
      if (sent) {
        // 3. 更新本地缓存
        messageCache.get(message.getRoomId()).put(message.getId(), message);
        log.info("Process danmaku success: {}", message);
      }
    }

  }

  private boolean validateMessage(DanmakuMessage message) {
    boolean valid = true;
    // 1. 检查房间状态: 房间是否允许弹幕
    AnchorRoomConfig roomConfig = roomService.getRoomConfig(message.getRoomId());
    if (roomConfig != null && !roomConfig.getAllowDanmaku()) {
      valid = false;
      log.error("Room is closed");
    }
    // 2. 检查发送权限: 
    // 是否被禁言，是否超过发送频率限制
    if (valid && !checkSendPermission(message.getUserId().toString(),
        message.getRoomId().toString())) {
      valid = false;
      log.error("User is banned");
    }

    return valid;
  }

  @Override
  public boolean checkSendPermission(String userId, String roomId) {
    // 1. 检查用户是否被禁言
    String banKey = String.format(ApiConstants.REDIS_USER_BAN, userId);
    if (Boolean.TRUE.equals(redisTemplate.hasKey(banKey))) {
      return false;
    }
    // 2. 检查发送频率
    String limitKey = String.format(ApiConstants.REDIS_USER_LIMIT, userId);
    Long count = redisTemplate.opsForValue().increment(limitKey);
    if (count == 1L) {
      redisTemplate.expire(limitKey, Constants.RATE_LIMIT_EXPIRE, TimeUnit.SECONDS);
    }
    return count <= Constants.MAX_SEND_RATE;
  }

  @Override
  public Collection<DanmakuMessage> getRecentDanmaku(Long roomId, int limit) {
    return messageCache.get(roomId).asMap().values();
  }

}