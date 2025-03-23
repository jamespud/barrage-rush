package com.spud.barrage.consumer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spud.barrage.common.data.dto.DanmakuMessage;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class PushNotificationService {

  private final RedisTemplate<String, Object> redisTemplate;
  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;

  @Value("${push.notification.batch-size:100}")
  private int batchSize;

  @Value("${push.notification.enabled:true}")
  private boolean pushEnabled;

  /**
   * 推送弹幕消息
   */
  public void pushDanmakuMessage(DanmakuMessage message) {
    if (!pushEnabled) {
      return;
    }

    try {
      // 获取房间对应的所有Push Server
      Set<Object> pushServers = getPushServersForRoom(message.getRoomId().toString());

      if (pushServers == null || pushServers.isEmpty()) {
        // 如果没有找到Push Server，存储到Redis以供Push Server轮询
        storeDanmakuForPushServer(message);
        return;
      }

      // 直接推送到所有Push Server
      for (Object serverObj : pushServers) {
        String serverUrl = serverObj.toString();
        try {
          sendToPushServer(serverUrl, message);
        } catch (Exception e) {
          log.error("Failed to push to server {}: {}", serverUrl, e.getMessage());
        }
      }
    } catch (Exception e) {
      log.error("Failed to push notification: {}", e.getMessage(), e);
    }
  }

  /**
   * 获取房间对应的Push Server列表
   */
  private Set<Object> getPushServersForRoom(String roomId) {
    String key = String.format("room:%s:pushservers", roomId);
    return redisTemplate.opsForSet().members(key);
  }

  /**
   * 存储弹幕消息供Push Server轮询
   */
  private void storeDanmakuForPushServer(DanmakuMessage message) {
    String key = String.format("push:queue:%s", message.getRoomId());
    redisTemplate.opsForList().leftPush(key, message);

    // 限制队列大小
    Long size = redisTemplate.opsForList().size(key);
    if (size != null && size > batchSize * 10) {
      redisTemplate.opsForList().trim(key, 0, batchSize * 10 - 1);
    }
  }

  /**
   * 发送消息到Push Server
   */
  private void sendToPushServer(String serverUrl, DanmakuMessage message) {
    try {
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);

      Map<String, Object> payload = Map.of(
          "type", "DANMAKU",
          "roomId", message.getRoomId(),
          "message", message);

      HttpEntity<String> request = new HttpEntity<>(
          objectMapper.writeValueAsString(payload), headers);

      restTemplate.postForEntity(serverUrl + "/api/push", request, Void.class);
    } catch (Exception e) {
      log.error("Failed to send to push server: {}", e.getMessage(), e);
    }
  }
}