package com.spud.barrage.common.mq.mange;

import com.spud.barrage.common.data.dto.DanmakuMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageBatchProcessor {
    private final BlockingQueue<DanmakuMessage> messageQueue = new LinkedBlockingQueue<>();
    private final WebSocketManager webSocketManager;
    
    public boolean addMessage(DanmakuMessage message) {
        return messageQueue.offer(message);
    }
    
    @Scheduled(fixedRate = 100)
    public void processBatch() {
        List<DanmakuMessage> batch = new ArrayList<>();
        messageQueue.drainTo(batch, 100);
        
        if (!batch.isEmpty()) {
            long startTime = System.currentTimeMillis();
            
            // 按房间分组
            Map<String, List<DanmakuMessage>> roomMessages = batch.stream()
                .collect(Collectors.groupingBy(DanmakuMessage::getRoomId));
                
            // 批量推送到各个房间
            roomMessages.forEach((roomId, messages) -> {
                try {
                    webSocketManager.broadcastToRoom(roomId, messages);
                } catch (Exception e) {
                    log.error("Failed to broadcast messages to room {}", roomId, e);
                }
            });
            
            // 记录推送延迟
            metrics.recordPushLatency(System.currentTimeMillis() - startTime);
        }
    }
} 