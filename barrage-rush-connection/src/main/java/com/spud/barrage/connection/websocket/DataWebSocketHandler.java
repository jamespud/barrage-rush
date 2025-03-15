package com.spud.barrage.connection.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spud.barrage.connection.config.ConnectionProperties;
import com.spud.barrage.connection.model.UserSession;
import com.spud.barrage.connection.service.DanmakuService;
import com.spud.barrage.connection.service.SessionService;
import java.io.IOException;
import java.util.List;
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
 * 数据WebSocket处理器
 * 
 * @author Spud
 * @date 2025/3/15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataWebSocketHandler extends TextWebSocketHandler {
    
    private final SessionService sessionService;
    private final DanmakuService danmakuService;
    private final ObjectMapper objectMapper;
    private final ConnectionProperties properties;
    
    // 存储活跃连接
    private final Map<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();
    
    // 存储会话ID与WebSocket会话的映射
    private final Map<String, String> sessionIdMapping = new ConcurrentHashMap<>();
    
    // 存储房间ID与最后消息索引的映射
    private final Map<Long, Long> roomLastIndexMap = new ConcurrentHashMap<>();
    
    // 消息发送调度器
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    /**
     * 初始化
     */
    public void init() {
        // 启动消息发送任务
        scheduler.scheduleAtFixedRate(
                this::sendMessagesTask,
                0,
                properties.getMessageSendInterval(),
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
        
        // 设置数据连接会话ID
        sessionService.setDataSessionId(userSession.getSessionId(), session.getId());
        
        // 存储WebSocket会话
        activeSessions.put(session.getId(), session);
        sessionIdMapping.put(session.getId(), userSession.getSessionId());
        
        log.info("数据连接已建立: sessionId={}, roomId={}, userId={}, ip={}", 
                session.getId(), roomId, userId, clientIp);
        
        // 发送历史消息
        sendHistoryMessages(session, roomId);
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 数据连接不处理客户端消息
        log.debug("收到客户端消息: {}", message.getPayload());
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String wsSessionId = session.getId();
        String userSessionId = sessionIdMapping.remove(wsSessionId);
        activeSessions.remove(wsSessionId);
        
        if (userSessionId != null) {
            UserSession userSession = sessionService.getSession(userSessionId);
            if (userSession != null) {
                log.info("数据连接已关闭: sessionId={}, roomId={}, userId={}, status={}", 
                        wsSessionId, userSession.getRoomId(), userSession.getUserId(), status);
                
                // 清除数据连接会话ID
                if (wsSessionId.equals(userSession.getDataSessionId())) {
                    sessionService.setDataSessionId(userSessionId, null);
                }
                
                // 如果心跳连接也已关闭，则删除会话
                if (userSession.getHeartbeatSessionId() == null) {
                    sessionService.deleteSession(userSessionId);
                }
            }
        } else {
            log.info("数据连接已关闭: sessionId={}, status={}", wsSessionId, status);
        }
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("数据连接传输错误: sessionId={}, error={}", session.getId(), exception.getMessage(), exception);
        session.close(CloseStatus.SERVER_ERROR);
    }
    
    /**
     * 发送历史消息
     */
    private void sendHistoryMessages(WebSocketSession session, Long roomId) {
        try {
            // 获取最新的消息
            List<DanmakuMessage> messages = danmakuService.getLatestMessages(roomId, properties.getMessageBufferSize());
            
            if (!messages.isEmpty()) {
                for (DanmakuMessage message : messages) {
                    sendMessage(session, message);
                }
                
                // 更新最后消息索引
                roomLastIndexMap.put(roomId, danmakuService.getMessageCount(roomId));
            } else {
                roomLastIndexMap.put(roomId, 0L);
            }
        } catch (Exception e) {
            log.error("发送历史消息失败: roomId={}, error={}", roomId, e.getMessage(), e);
        }
    }
    
    /**
     * 发送消息
     */
    private void sendMessage(WebSocketSession session, DanmakuMessage message) {
        if (session == null || !session.isOpen() || message == null) {
            return;
        }
        
        try {
            String json = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            log.error("发送消息失败: sessionId={}, error={}", session.getId(), e.getMessage(), e);
        }
    }
    
    /**
     * 消息发送任务
     */
    private void sendMessagesTask() {
        try {
            // 获取所有房间的新消息
            for (Map.Entry<Long, Long> entry : roomLastIndexMap.entrySet()) {
                Long roomId = entry.getKey();
                Long lastIndex = entry.getValue();
                
                // 获取新消息
                long currentCount = danmakuService.getMessageCount(roomId);
                if (currentCount > lastIndex) {
                    List<DanmakuMessage> newMessages = danmakuService.getMessages(roomId, lastIndex, (int) (currentCount - lastIndex));
                    
                    // 发送新消息给该房间的所有连接
                    for (Map.Entry<String, String> sessionEntry : sessionIdMapping.entrySet()) {
                        String wsSessionId = sessionEntry.getKey();
                        String userSessionId = sessionEntry.getValue();
                        
                        UserSession userSession = sessionService.getSession(userSessionId);
                        if (userSession != null && roomId.equals(userSession.getRoomId())) {
                            WebSocketSession wsSession = activeSessions.get(wsSessionId);
                            if (wsSession != null && wsSession.isOpen()) {
                                for (DanmakuMessage message : newMessages) {
                                    sendMessage(wsSession, message);
                                }
                            }
                        }
                    }
                    
                    // 更新最后消息索引
                    roomLastIndexMap.put(roomId, currentCount);
                }
            }
        } catch (Exception e) {
            log.error("消息发送任务异常: {}", e.getMessage(), e);
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