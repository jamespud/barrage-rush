package com.spud.barrage.push.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spud.barrage.push.constant.WebSocketType;
import com.spud.barrage.push.manager.WebSocketSessionManager;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 弹幕服务
 * @author Spud
 * @date 2025/3/30
 */
@Slf4j
@Service
public class DanmakuService {

  @Autowired
  private RedisTemplate<String, String> redisTemplate;

  @Autowired
  private RabbitTemplate rabbitTemplate;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private WebSocketSessionManager sessionManager;

  @Value("${push.danmaku.redis-key-prefix:danmaku:room:}")
  private String danmakuKeyPrefix;

  @Value("${push.danmaku.redis-ttl:3600}")
  private int danmakuRedisTtl;

  @Value("${push.danmaku.max-message-count:200}")
  private int maxMessageCount;

  @Value("${barrage.mq.danmaku-exchange:danmaku.exchange}")
  private String danmakuExchange;

  @Value("${barrage.mq.danmaku-routing-key:danmaku.routing}")
  private String danmakuRoutingKey;

  // 存储每个房间已处理的消息索引
  private final Map<Long, Long> roomMessageIndexMap = new ConcurrentHashMap<>();

  /**
   * 处理弹幕消息
   */
  public void processDanmaku(String message) {
    try {
      JsonNode jsonNode = objectMapper.readTree(message);
      String roomId = jsonNode.path("roomId").asText();

      // 发送到消息队列
      rabbitTemplate.convertAndSend(danmakuExchange, danmakuRoutingKey, message);

      log.debug("弹幕消息已发送到队列: roomId={}", roomId);
    } catch (JsonProcessingException e) {
      log.error("处理弹幕消息失败", e);
    }
  }

  /**
   * 获取最近的弹幕消息
   */
  public List<String> getRecentDanmaku(String roomId) {
    String key = danmakuKeyPrefix + roomId;
    List<String> messages = redisTemplate.opsForList().range(key, 0, maxMessageCount - 1);
    return messages != null ? messages : Collections.emptyList();
  }

  /**
   * 监听RabbitMQ中的弹幕消息
   */
  @RabbitListener(queues = "${barrage.mq.danmaku-queue:danmaku.queue}")
  public void onDanmakuMessage(String message) {
    try {
      JsonNode jsonNode = objectMapper.readTree(message);
      if (!jsonNode.has("roomId")) {
        log.error("弹幕消息缺少roomId字段: {}", message);
        return;
      }

      Long roomId = jsonNode.get("roomId").asLong();
      String type = jsonNode.path("type").asText();

      // 只处理弹幕类型的消息
      if (!"DANMAKU".equals(type)) {
        return;
      }

      log.debug("收到弹幕消息: roomId={}", roomId);

      // 将消息广播给房间内的客户端
      sessionManager.broadcastToRoom(WebSocketType.DANMAKU, roomId, message);

    } catch (JsonProcessingException e) {
      log.error("解析弹幕消息失败", e);
    }
  }

  /**
   * 定期轮询Redis获取最新弹幕
   * 注: 这是除RabbitMQ外的备用方案，保证消息不丢失
   */
  @Scheduled(fixedRate = 1000) // 每秒检查一次
  public void pollDanmakuFromRedis() {
    // 获取所有活跃房间
    Set<Long> roomIds = sessionManager.getRoomIds(WebSocketType.DANMAKU);

    for (Long roomId : roomIds) {
      String key = danmakuKeyPrefix + roomId;
      Long listSize = redisTemplate.opsForList().size(key);

      if (listSize == null || listSize == 0) {
        continue;
      }

      // 获取当前索引，默认为0
      long currentIndex = roomMessageIndexMap.getOrDefault(roomId, 0L);

      // 如果有新消息
      if (listSize > currentIndex) {
        // 获取新消息
        List<String> newMessages = redisTemplate.opsForList()
            .range(key, currentIndex, listSize - 1);

        if (newMessages != null && !newMessages.isEmpty()) {
          // 推送新消息
          for (String message : newMessages) {
            try {
              JsonNode jsonNode = objectMapper.readTree(message);
              String type = jsonNode.path("type").asText();

              // 只推送弹幕类型的消息
              if ("DANMAKU".equals(type)) {
                sessionManager.broadcastToRoom(WebSocketType.DANMAKU, roomId, message);
              }
            } catch (JsonProcessingException e) {
              log.error("解析Redis弹幕消息失败", e);
            }
          }

          // 更新索引
          roomMessageIndexMap.put(roomId, listSize);
        }
      }
    }
  }
}