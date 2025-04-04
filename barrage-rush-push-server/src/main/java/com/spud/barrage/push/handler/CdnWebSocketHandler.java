package com.spud.barrage.push.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.spud.barrage.push.constant.WebSocketType;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * CDN信息WebSocket处理器
 * @author Spud
 * @date 2025/3/30
 */
@Slf4j
@Component
public class CdnWebSocketHandler extends AbstractWebSocketHandler {

  private static final String HANDLER_NAME = "CDN处理器";
  private static final String HANDLER_TYPE = WebSocketType.CDN;

  // 消息类型
  private static final String TYPE_CDN_INFO = "CDN_INFO";
  private static final String TYPE_ERROR = "ERROR";
  private static final String TYPE_SUCCESS = "SUCCESS";

  @Autowired
  private RedisTemplate<String, String> redisTemplate;

  @Autowired
  private ObjectMapper objectMapper;

  @Value("${push.cdn.redis-key-prefix:cdn:room:}")
  private String cdnKeyPrefix;

  @Value("${push.cdn.redis-ttl:3600}")
  private int cdnRedisTtl;

  @Override
  public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
    String payload = message.getPayload();
    Long roomId = getRoomId(session);
    Long userId = getUserId(session);

    log.debug("[{}] 收到消息: roomId={}, userId={}, payload={}",
        HANDLER_NAME, roomId, userId, payload);

    try {
      // 解析消息
      JsonNode jsonNode = objectMapper.readTree(payload);
      String type = jsonNode.has("type") ? jsonNode.get("type").asText() : "";

      // 处理不同类型的消息
      if (TYPE_CDN_INFO.equals(type)) {
        // 处理CDN信息更新
        handleCdnInfoUpdate(session, jsonNode);
      } else {
        sendErrorResponse(session, "不支持的消息类型: " + type);
      }
    } catch (JsonProcessingException e) {
      log.error("[{}] 解析消息失败: {}", HANDLER_NAME, e.getMessage(), e);
      sendErrorResponse(session, "无效的消息格式");
    }
  }

  /**
   * 处理CDN信息更新
   */
  private void handleCdnInfoUpdate(WebSocketSession session, JsonNode jsonNode) throws Exception {
    Long roomId = getRoomId(session);
    Long userId = getUserId(session);

    // 检查是否有权限更新CDN信息
    // TODO: 实现权限检查逻辑

    // 保存CDN信息到Redis
    String cdnKey = cdnKeyPrefix + roomId;
    String cdnInfo = jsonNode.toString();

    redisTemplate.opsForValue().set(cdnKey, cdnInfo, cdnRedisTtl, TimeUnit.SECONDS);

    log.info("[{}] CDN信息已更新: roomId={}, userId={}", HANDLER_NAME, roomId, userId);

    // 广播CDN信息更新给房间内所有用户
    sessionManager.broadcastToRoom(HANDLER_TYPE, roomId, cdnInfo);

    // 发送成功响应
    sendSuccessResponse(session, "CDN信息已更新");
  }

  /**
   * 发送错误响应
   */
  private void sendErrorResponse(WebSocketSession session, String message) throws Exception {
    ObjectNode response = objectMapper.createObjectNode();
    response.put("type", TYPE_ERROR);
    response.put("data", message);

    sendMessage(session, response.toString());
  }

  /**
   * 发送成功响应
   */
  private void sendSuccessResponse(WebSocketSession session, String message) throws Exception {
    ObjectNode response = objectMapper.createObjectNode();
    response.put("type", TYPE_SUCCESS);
    response.put("data", message);

    sendMessage(session, response.toString());
  }

  @Override
  protected String getHandlerName() {
    return HANDLER_NAME;
  }

  @Override
  protected String getHandlerType() {
    return HANDLER_TYPE;
  }

  @Override
  protected void handleConnectionEstablished(WebSocketSession session) throws Exception {
    Long roomId = getRoomId(session);

    log.debug("[{}] CDN连接已建立: roomId={}, userId={}",
        HANDLER_NAME, roomId, getUserId(session));

    // 发送当前CDN信息给客户端
    String cdnKey = cdnKeyPrefix + roomId;
    String cdnInfo = redisTemplate.opsForValue().get(cdnKey);

    if (cdnInfo != null) {
      sendMessage(session, cdnInfo);
      log.debug("[{}] 发送现有CDN信息: roomId={}", HANDLER_NAME, roomId);
    }
  }

  @Override
  protected void handleConnectionClosed(WebSocketSession session, CloseStatus status)
      throws Exception {
    log.debug("[{}] CDN连接已关闭: roomId={}, userId={}, status={}",
        HANDLER_NAME, getRoomId(session), getUserId(session), status);
  }

  @Override
  protected void handleError(WebSocketSession session, Throwable exception) throws Exception {
    log.error("[{}] CDN连接错误: roomId={}, userId={}",
        HANDLER_NAME, getRoomId(session), getUserId(session), exception);
  }
}