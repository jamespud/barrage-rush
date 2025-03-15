package com.spud.barrage.connection.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spud.barrage.connection.config.ConnectionProperties;
import com.spud.barrage.connection.model.UserSession;
import com.spud.barrage.connection.service.HeartbeatService;
import com.spud.barrage.connection.service.SessionService;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 心跳WebSocket处理器
 * 
 * @author Spud
 * @date 2025/3/15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HeartbeatWebSocketHandler extends TextWebSocketHandler {
    
    private final SessionService sessionService;
    private final HeartbeatService heartbeatService;
    private final ObjectMapper objectMapper;
    private final ConnectionProperties properties;
    
    // 存储活跃连接
    private final Map<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();
    
    // 存储会话ID与WebSocket会话的映射
    private final Map<String, String> sessionIdMapping = new ConcurrentHashMap<>();
    
    // 心跳检查调度器
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    /**
     * 初始化
     */
    public void init() {
        // 启动心跳检查任务
        scheduler.scheduleAtFixedRate(
                this::checkHeartbeatsTask,
                properties.getHeartbeatInterval(),
                properties.getHeartbeatInterval(),
                TimeUnit.MILLISECONDS
        );
    }
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 解析路径参数
        UriComponents uriComponents = UriComponentsBuilder.fromUri(session.getUri()).build();
        String roomIdStr = uriComponents.getPathSegment(2);
        String userIdStr = uriComponents.getPathSegment(3);
        
        if (!StringUtils.hasText(roomIdStr) || !StringUtils.hasText(userIdStr)) {
            log.warn("连接参数无效: roomId={}, userId={}", roomIdStr, userIdStr);
            session.close(CloseStatus.BAD_DATA);
            return;
        }
        
        Long roomId = Long.parseLong(roomIdStr);
        Long userId = Long.parseLong(userIdStr);
        
        // 获取客户端IP
        String clientIp = getClientIp(session);
        
        // 创建或获取会话
        UserSession userSession = sessionService.createSession(userId, roomId, "用户" + userId, "", clientIp, "");
        
        // 设置心跳连接会话ID
        sessionService.setHeartbeatSessionId(userSession.getSessionId(), session.getId());
        
        // 存储WebSocket会话
        activeSessions.put(session.getId(), session);
        sessionIdMapping.put(session.getId(), userSession.getSessionId());
        
        log.info("心跳连接已建立: sessionId={}, roomId={}, userId={}, ip={}", 
                session.getId(), roomId, userId, clientIp);
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            String payload = message.getPayload();
            HeartbeatMessage heartbeatMessage = objectMapper.readValue(payload, HeartbeatMessage.class);
            
            String wsSessionId = session.getId();
            String userSessionId = sessionIdMapping.get(wsSessionId);
            
            if (userSessionId != null) {
                // 处理心跳消息
                HeartbeatMessage response = heartbeatService.processHeartbeat(userSessionId, heartbeatMessage);
                
                // 发送响应
                if (response != null) {
                    sendMessage(session, response);
                }
            } else {
                log.warn("收到未知会话的心跳消息: sessionId={}", wsSessionId);
            }
        } catch (Exception e) {
            log.error("处理心跳消息失败: {}", e.getMessage(), e);
        }
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String wsSessionId = session.getId();
        String userSessionId = sessionIdMapping.remove(wsSessionId);
        activeSessions.remove(wsSessionId);
        
        if (userSessionId != null) {
            UserSession userSession = sessionService.getSession(userSessionId);
            if (userSession != null) {
                log.info("心跳连接已关闭: sessionId={}, roomId={}, userId={}, status={}", 
                        wsSessionId, userSession.getRoomId(), userSession.getUserId(), status);
                
                // 清除心跳连接会话ID
                if (wsSessionId.equals(userSession.getHeartbeatSessionId())) {
                    sessionService.setHeartbeatSessionId(userSessionId, null);
                }
                
                // 删除心跳记录
                heartbeatService.deleteHeartbeat(userSessionId);
                
                // 如果数据连接也已关闭，则删除会话
                if (userSession.getDataSessionId() == null) {
                    sessionService.deleteSession(userSessionId);
                }
            }
        } else {
            log.info("心跳连接已关闭: sessionId={}, status={}", wsSessionId, status);
        }
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("心跳连接传输错误: sessionId={}, error={}", session.getId(), exception.getMessage(), exception);
        session.close(CloseStatus.SERVER_ERROR);
    }
    
    /**
     * 发送消息
     */
    private void sendMessage(WebSocketSession session, HeartbeatMessage message) {
        if (session == null || !session.isOpen() || message == null) {
            return;
        }
        
        try {
            String json = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            log.error("发送心跳消息失败: sessionId={}, error={}", session.getId(), e.getMessage(), e);
        }
    }
    
    /**
     * 心跳检查任务
     */
    private void checkHeartbeatsTask() {
        try {
            for (Map.Entry<String, String> entry : sessionIdMapping.entrySet()) {
                String wsSessionId = entry.getKey();
                String userSessionId = entry.getValue();
                
                // 检查心跳是否有效
                if (!heartbeatService.isHeartbeatValid(userSessionId)) {
                    WebSocketSession session = activeSessions.get(wsSessionId);
                    if (session != null && session.isOpen()) {
                        UserSession userSession = sessionService.getSession(userSessionId);
                        if (userSession != null) {
                            // 发送重连消息
                            HeartbeatMessage reconnectMessage = HeartbeatMessage.reconnect(
                                    userSession.getRoomId(),
                                    userSession.getUserId(),
                                    userSessionId,
                                    "心跳超时"
                            );
                            sendMessage(session, reconnectMessage);
                            
                            // 关闭连接
                            session.close(CloseStatus.NORMAL);
                            log.info("心跳超时，关闭连接: sessionId={}, roomId={}, userId={}", 
                                    wsSessionId, userSession.getRoomId(), userSession.getUserId());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("心跳检查任务异常: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 获取客户端IP
     */
    private String getClientIp(WebSocketSession session) {
        HttpHeaders headers = session.getHandshakeHeaders();
        String forwardedFor = headers.getFirst("X-Forwarded-For");
        if (forwardedFor != null) {
            return forwardedFor.split(",")[0].trim();
        }
        return session.getRemoteAddress().getAddress().getHostAddress();
    }
} 