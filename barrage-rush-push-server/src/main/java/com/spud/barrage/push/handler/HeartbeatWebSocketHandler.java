package com.spud.barrage.push.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.spud.barrage.push.constant.WebSocketType;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * 心跳WebSocket处理器
 * @author Spud
 * @date 2025/3/30
 */
@Slf4j
@Component
public class HeartbeatWebSocketHandler extends AbstractWebSocketHandler {

  private static final String HANDLER_NAME = "心跳处理器";
  private static final String HANDLER_TYPE = WebSocketType.HEARTBEAT;

  // 保存每个会话的最后心跳时间
  private final Map<String, AtomicLong> sessionLastHeartbeatMap = new ConcurrentHashMap<>();

  @Autowired
  private RedisTemplate<String, String> redisTemplate;

  @Autowired
  private ObjectMapper objectMapper;

  @Value("${push.heartbeat.timeout:30000}")
  private long heartbeatTimeout;

  @Value("${push.heartbeat.redis-key-prefix:heartbeat:}")
  private String heartbeatKeyPrefix;

  @Value("${push.heartbeat.redis-ttl:60}")
  private int heartbeatRedisTtl;

  @Override
  public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
    String sessionId = session.getId();
    Long roomId = getRoomId(session);
    Long userId = getUserId(session);

    // 更新心跳时间
    updateHeartbeat(session);

    // 构建响应
    ObjectNode response = objectMapper.createObjectNode();
    response.put("type", "HEARTBEAT");

    ObjectNode data = response.putObject("data");
    data.put("timestamp", System.currentTimeMillis());

    // 发送响应
    sendMessage(session, response.toString());

    log.debug("[{}] 处理心跳消息: sessionId={}, roomId={}, userId={}",
        HANDLER_NAME, sessionId, roomId, userId);
  }

  /**
   * 更新心跳时间
   */
  private void updateHeartbeat(WebSocketSession session) {
    String sessionId = session.getId();
    Long roomId = getRoomId(session);
    Long userId = getUserId(session);
    long timestamp = System.currentTimeMillis();

    // 更新内存中的心跳时间
    sessionLastHeartbeatMap.computeIfAbsent(sessionId, k -> new AtomicLong(0))
        .set(timestamp);

    // 更新Redis中的心跳记录
    String heartbeatKey = heartbeatKeyPrefix + sessionId;
    String heartbeatValue = String.format("%s:%s:%d", userId, roomId, timestamp);

    redisTemplate.opsForValue()
        .set(heartbeatKey, heartbeatValue, heartbeatRedisTtl, TimeUnit.SECONDS);
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
    // 初始心跳
    updateHeartbeat(session);

    log.debug("[{}] 心跳连接已建立: sessionId={}, roomId={}, userId={}",
        HANDLER_NAME, session.getId(), getRoomId(session), getUserId(session));
  }

  @Override
  protected void handleConnectionClosed(WebSocketSession session, CloseStatus status)
      throws Exception {
    String sessionId = session.getId();

    // 清理心跳数据
    sessionLastHeartbeatMap.remove(sessionId);

    // 清理Redis中的心跳记录
    String heartbeatKey = heartbeatKeyPrefix + sessionId;
    redisTemplate.delete(heartbeatKey);

    log.debug("[{}] 心跳连接已关闭: sessionId={}, status={}",
        HANDLER_NAME, sessionId, status);
  }

  @Override
  protected void handleError(WebSocketSession session, Throwable exception) throws Exception {
    log.error("[{}] 心跳连接错误: sessionId={}", HANDLER_NAME, session.getId(), exception);
  }
}