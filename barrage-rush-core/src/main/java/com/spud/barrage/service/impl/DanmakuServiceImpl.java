package com.spud.barrage.service.impl;

import com.spud.barrage.common.constant.ApiConstants;
import com.spud.barrage.common.constant.Constants;
import com.spud.barrage.common.exception.BusinessException;
import com.spud.barrage.common.util.SnowflakeIdWorker;
import com.spud.barrage.model.dto.DanmakuRequest;
import com.spud.barrage.model.entity.DanmakuMessage;
import com.spud.barrage.model.entity.RoomConfig;
import com.spud.barrage.mq.producer.DanmakuProducer;
import com.spud.barrage.service.DanmakuService;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * @author Spud
 * @date 2025/3/4
 */
@Service
@Slf4j
public class DanmakuServiceImpl implements DanmakuService {

  @Autowired
  private DanmakuProducer danmakuProducer;

  @Autowired
  private RedisTemplate<String, Object> redisTemplate;

  @Autowired
  private SnowflakeIdWorker snowflakeIdWorker;

  @Override
  public String sendDanmaku(DanmakuRequest request, Long userId, String username) {
    return "";
  }

  @Override
  public void processDanmaku(DanmakuMessage message) {
    // 1. 生成消息ID
    message.setId(snowflakeIdWorker.nextId());
    message.setTimestamp(System.currentTimeMillis());

    // 2. 消息验证
    validateMessage(message);

    // 3. 发送到MQ
    danmakuProducer.sendDanmaku(message);

    // 4. 存储到Redis
    String key = String.format(ApiConstants.REDIS_ROOM_MESSAGES, message.getRoomId());
    redisTemplate.opsForZSet().add(key, message, message.getTimestamp());
    redisTemplate.expire(key, Constants.ROOM_MESSAGES_EXPIRE, TimeUnit.SECONDS);

    // 5. 清理过期消息
    redisTemplate.opsForZSet().removeRange(key, 0, -Constants.MAX_ROOM_MESSAGES - 1);

    log.info("Process danmaku success: {}", message);
  }

  @Override
  public List<DanmakuMessage> getRecentDanmaku(String roomId, int limit) {
    String key = String.format(ApiConstants.REDIS_ROOM_MESSAGES, roomId);
    Set<Object> messages = redisTemplate.opsForZSet().reverseRange(key, 0, limit - 1);
    return messages.stream()
        .map(msg -> (DanmakuMessage) msg)
        .collect(Collectors.toList());
  }

  @Override
  public boolean checkSendPermission(String userId, String roomId) {
    // 1. 检查用户是否被禁言
    String banKey = String.format("ban:user:%s", userId);
    if (Boolean.TRUE.equals(redisTemplate.hasKey(banKey))) {
      return false;
    }

    // 2. 检查发送频率
    String limitKey = String.format(ApiConstants.REDIS_USER_LIMIT, userId);
    Long count = redisTemplate.opsForValue().increment(limitKey);
    if (count == 1) {
      redisTemplate.expire(limitKey, Constants.RATE_LIMIT_EXPIRE, TimeUnit.SECONDS);
    }
    return count <= Constants.MAX_SEND_RATE;
  }

  @Override
  public RoomConfig getRoomConfig(String roomId) {
    String key = String.format(ApiConstants.REDIS_ROOM_CONFIG, roomId);
    return (RoomConfig) redisTemplate.opsForValue().get(key);
  }

  @Override
  public void updateRoomConfig(RoomConfig config) {
    String key = String.format(ApiConstants.REDIS_ROOM_CONFIG, config.getRoomId());
    config.setUpdateTime(System.currentTimeMillis());
    redisTemplate.opsForValue().set(key, config);
  }

  private void validateMessage(DanmakuMessage message) {
    // 1. 检查必填字段
    if (message.getRoomId() == null || message.getUserId() == null
        || StringUtils.hasLength(message.getContent())) {
      throw new BusinessException("必填字段不能为空");
    }

    // 2. 检查内容长度
    if (message.getContent().length() > Constants.MAX_CONTENT_LENGTH) {
      throw new BusinessException("弹幕内容过长");
    }

    // 3. 检查发送权限
    if (!checkSendPermission(message.getUserId().toString(), message.getRoomId().toString())) {
      throw new BusinessException("发送太频繁，请稍后再试");
    }

    // 4. 检查房间状态
    RoomConfig roomConfig = getRoomConfig(message.getRoomId().toString());
    if (roomConfig != null && !roomConfig.getAllowDanmaku()) {
      throw new BusinessException("房间已关闭弹幕");
    }
  }
}