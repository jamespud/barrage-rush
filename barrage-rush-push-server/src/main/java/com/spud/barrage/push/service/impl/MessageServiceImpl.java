package com.spud.barrage.push.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spud.barrage.common.data.dto.DanmakuMessage;
import com.spud.barrage.push.constant.WebSocketType;
import com.spud.barrage.push.manager.WebSocketSessionManager;
import com.spud.barrage.push.service.MessageService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 消息服务实现类
 *
 * @author Spud
 * @date 2025/3/30
 */
@Slf4j
@Service
public class MessageServiceImpl implements MessageService {

  private static final String ROOM_MESSAGE_KEY = "room:message:";
  private static final String MESSAGE_ACK_KEY = "message:ack:";
  private static final int MESSAGE_EXPIRE_DAYS = 7;

  @Autowired
  private RedisTemplate<String, Object> redisTemplate;

  @Autowired
  private WebSocketSessionManager webSocketSessionManager;

  @Autowired
  private ObjectMapper objectMapper;

  @Override
  public DanmakuMessage createDanmakuMessage(Long roomId, Long userId, Map<String, Object> data) {
    DanmakuMessage message = new DanmakuMessage();
    message.setId(generateMessageId());
    message.setRoomId(roomId);
    message.setUserId(userId);
    message.setContent((String) data.getOrDefault("content", ""));
    message.setColor((String) data.getOrDefault("color", "#FFFFFF"));
    message.setSize((Integer) data.getOrDefault("size", 25));
    message.setPosition((Integer) data.getOrDefault("position", 0));
    message.setTimestamp(System.currentTimeMillis());

    log.debug("[消息] 创建弹幕消息: roomId={}, userId={}, messageId={}",
        roomId, userId, message.getId());

    return message;
  }

  @Override
  public void publishDanmakuMessage(DanmakuMessage message) {
    try {
      Long roomId = message.getRoomId();

      // 1. 保存消息到Redis
      String messageKey = ROOM_MESSAGE_KEY + roomId;
      redisTemplate.opsForList().leftPush(messageKey, message);
      redisTemplate.expire(messageKey, MESSAGE_EXPIRE_DAYS, TimeUnit.DAYS);

      // 2. 转换为JSON
      Map<String, Object> messageMap = new HashMap<>();
      messageMap.put("type", "DANMAKU");
      messageMap.put("data", message);
      messageMap.put("timestamp", System.currentTimeMillis());

      String messageJson = objectMapper.writeValueAsString(messageMap);

      // 3. 广播给房间内的所有用户
      webSocketSessionManager.broadcastToRoom(WebSocketType.DANMAKU, roomId, messageJson);

      log.info("[消息] 发布弹幕消息: roomId={}, messageId={}",
          roomId, message.getId());
    } catch (Exception e) {
      log.error("[消息] 发布弹幕消息失败: messageId={}", message.getId(), e);
    }
  }

  @Override
  public List<DanmakuMessage> getRecentMessages(Long roomId, int limit) {
    try {
      String messageKey = ROOM_MESSAGE_KEY + roomId;
      List<Object> messages = redisTemplate.opsForList().range(messageKey, 0, limit - 1);

      List<DanmakuMessage> result = new ArrayList<>();
      if (messages != null) {
        for (Object msg : messages) {
          if (msg instanceof DanmakuMessage) {
            result.add((DanmakuMessage) msg);
          }
        }
      }

      log.debug("[消息] 获取最近消息: roomId={}, count={}", roomId, result.size());
      return result;
    } catch (Exception e) {
      log.error("[消息] 获取最近消息失败: roomId={}", roomId, e);
      return new ArrayList<>();
    }
  }

  @Override
  public void markMessageAcknowledged(Long messageId, Long userId) {
    try {
      String ackKey = MESSAGE_ACK_KEY + messageId;
      redisTemplate.opsForSet().add(ackKey, userId);
      redisTemplate.expire(ackKey, MESSAGE_EXPIRE_DAYS, TimeUnit.DAYS);

      log.debug("[消息] 标记消息已确认: messageId={}, userId={}", messageId, userId);
    } catch (Exception e) {
      log.error("[消息] 标记消息已确认失败: messageId={}, userId={}", messageId, userId, e);
    }
  }

  @Override
  public Map<String, Object> getSystemStatus(Long roomId) {
    try {
      Map<String, Object> status = new HashMap<>();

      // 房间信息
      status.put("roomId", roomId);

      // 在线人数
      int onlineCount = webSocketSessionManager.getRoomSessionCount(WebSocketType.SYSTEM, roomId) +
          webSocketSessionManager.getRoomSessionCount(WebSocketType.DANMAKU, roomId);
      status.put("onlineCount", onlineCount);

      // 当前时间
      status.put("serverTime", System.currentTimeMillis());

      // 最近消息数量
      String messageKey = ROOM_MESSAGE_KEY + roomId;
      Long messageCount = redisTemplate.opsForList().size(messageKey);
      status.put("messageCount", messageCount != null ? messageCount : 0);

      log.debug("[消息] 获取系统状态: roomId={}, onlineCount={}", roomId, onlineCount);
      return status;
    } catch (Exception e) {
      log.error("[消息] 获取系统状态失败: roomId={}", roomId, e);
      return new HashMap<>();
    }
  }

  /**
   * 生成消息ID
   * 使用时间戳和随机数组合
   */
  private Long generateMessageId() {
    // 使用时间戳后10位 + 5位随机数，组成15位消息ID
    long timestamp = System.currentTimeMillis() % 10000000000L;
    long random = (long) (Math.random() * 100000);
    return timestamp * 100000 + random;
  }
}