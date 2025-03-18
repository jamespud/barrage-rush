package com.spud.barrage.push.service.impl;

import com.spud.barrage.common.data.dto.DanmakuMessage;
import com.spud.barrage.push.config.ConnectionProperties;
import com.spud.barrage.push.service.DanmakuService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 弹幕服务实现
 *
 * @author Spud
 * @date 2025/3/15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DanmakuServiceImpl implements DanmakuService {

  private final RedisTemplate<String, Object> redisTemplate;
  private final ConnectionProperties properties;

  @Override
  public List<DanmakuMessage> getLatestMessages(Long roomId, int count) {
    String key = getDanmakuKey(roomId);
    long size = redisTemplate.opsForList().size(key);

    if (size <= 0) {
      return Collections.emptyList();
    }

    // 获取最新的count条消息
    long startIndex = Math.max(0, size - count);
    return getMessages(roomId, startIndex, count);
  }

  @Override
  public List<DanmakuMessage> getMessages(Long roomId, long startIndex, int count) {
    String key = getDanmakuKey(roomId);
    List<Object> objects = redisTemplate.opsForList()
        .range(key, startIndex, startIndex + count - 1);

    if (objects == null || objects.isEmpty()) {
      return Collections.emptyList();
    }

    List<DanmakuMessage> messages = new ArrayList<>(objects.size());
    for (Object obj : objects) {
      if (obj instanceof DanmakuMessage) {
        messages.add((DanmakuMessage) obj);
      }
    }

    return messages;
  }

  @Override
  public long getMessageCount(Long roomId) {
    String key = getDanmakuKey(roomId);
    Long size = redisTemplate.opsForList().size(key);
    return size != null ? size : 0;
  }

  @Override
  public boolean saveMessage(DanmakuMessage message) {
    if (message == null || message.getRoomId() == null) {
      return false;
    }

    try {
      String key = getDanmakuKey(message.getRoomId());
      redisTemplate.opsForList().rightPush(key, message);
      redisTemplate.expire(key, properties.getRedis().getDanmakuExpire(), TimeUnit.SECONDS);
      return true;
    } catch (Exception e) {
      log.error("保存弹幕消息失败: {}", e.getMessage(), e);
      return false;
    }
  }

  @Override
  public int saveMessages(List<DanmakuMessage> messages) {
    if (messages == null || messages.isEmpty()) {
      return 0;
    }

    int successCount = 0;
    for (DanmakuMessage message : messages) {
      if (saveMessage(message)) {
        successCount++;
      }
    }

    return successCount;
  }

  @Override
  public boolean deleteRoomMessages(Long roomId) {
    String key = getDanmakuKey(roomId);
    return Boolean.TRUE.equals(redisTemplate.delete(key));
  }

  @Override
  public boolean deleteMessage(Long roomId, String messageId) {
    String key = getDanmakuKey(roomId);
    long size = redisTemplate.opsForList().size(key);

    if (size <= 0) {
      return false;
    }

    // 遍历查找并删除指定消息
    for (long i = 0; i < size; i++) {
      Object obj = redisTemplate.opsForList().index(key, i);
      if (obj instanceof DanmakuMessage) {
        DanmakuMessage message = (DanmakuMessage) obj;
        if (messageId.equals(message.getId())) {
          // 使用临时值替换要删除的消息
          redisTemplate.opsForList().set(key, i, "DELETED");
          // 删除临时值
          redisTemplate.opsForList().remove(key, 1, "DELETED");
          return true;
        }
      }
    }

    return false;
  }

  /**
   * 获取弹幕消息键
   */
  private String getDanmakuKey(Long roomId) {
    return properties.getRedis().getDanmakuKeyPrefix() + roomId;
  }
} 