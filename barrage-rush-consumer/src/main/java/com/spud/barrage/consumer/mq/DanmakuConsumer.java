package com.spud.barrage.consumer.mq;

import com.spud.barrage.common.data.dto.DanmakuMessage;
import com.spud.barrage.common.mq.config.RabbitMQConfig;
import com.spud.barrage.consumer.service.DanmakuProcessService;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.DirectMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author Spud
 * @date 2025/3/11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DanmakuConsumer implements DisposableBean {

    private final ConnectionFactory connectionFactory;
    private final DanmakuProcessService danmakuProcessService;
    private final Jackson2JsonMessageConverter messageConverter;
    
    @Value("${rabbitmq.sharding.count:3}")
    private int defaultShardingCount;
    
    @Value("${consumer.prefetch.count:100}")
    private int prefetchCount;
    
    // 存储创建的监听容器，用于优雅关闭
    private final List<MessageListenerContainer> listenerContainers = new ArrayList<>();
    
    // 存储热门房间的监听容器，用于动态管理
    private final Map<String, List<MessageListenerContainer>> hotRoomContainers = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void init() {
        // 初始化标准分片的消费者
        setupNormalQueueConsumers();
        
        // 初始化冷门房间共享队列的消费者
        setupColdQueueConsumer();
        
        log.info("DanmakuConsumer initialized with {} normal shards", defaultShardingCount);
    }
    
    /**
     * 设置标准分片队列的消费者
     */
    private void setupNormalQueueConsumers() {
        for (int i = 0; i < defaultShardingCount; i++) {
            String queueNamePattern = String.format(RabbitMQConfig.DANMAKU_QUEUE_TEMPLATE, "*", i);
            DirectMessageListenerContainer container = createMessageListenerContainer(queueNamePattern);
            listenerContainers.add(container);
            container.start();
        }
    }
    
    /**
     * 设置冷门房间共享队列的消费者
     */
    private void setupColdQueueConsumer() {
        DirectMessageListenerContainer container = createMessageListenerContainer(RabbitMQConfig.DANMAKU_SHARED_COLD_QUEUE);
        listenerContainers.add(container);
        container.start();
    }
    
    /**
     * 为热门房间创建专门的消费者
     */
    public void setupHotRoomConsumer(String roomId, int shardCount) {
        List<MessageListenerContainer> containers = new ArrayList<>();
        
        for (int i = 0; i < shardCount; i++) {
            String queueName = String.format("danmaku.hot.queue.%s.%d", roomId, i);
            DirectMessageListenerContainer container = createMessageListenerContainer(queueName);
            containers.add(container);
            container.start();
        }
        
        hotRoomContainers.put(roomId, containers);
        log.info("Created {} consumers for hot room {}", shardCount, roomId);
    }
    
    /**
     * 移除热门房间的消费者
     */
    public void removeHotRoomConsumer(String roomId) {
        List<MessageListenerContainer> containers = hotRoomContainers.remove(roomId);
        if (containers != null) {
            for (MessageListenerContainer container : containers) {
                container.stop();
            }
            log.info("Removed consumers for hot room {}", roomId);
        }
    }
    
    /**
     * 创建消息监听容器
     */
    private DirectMessageListenerContainer createMessageListenerContainer(String queueName) {
        DirectMessageListenerContainer container = new DirectMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(queueName);
        container.setPrefetchCount(prefetchCount);
        container.setAcknowledgeMode(AcknowledgeMode.AUTO);
        
        // 设置消息监听器
        MessageListenerAdapter adapter = new MessageListenerAdapter(new DanmakuMessageHandler());
        adapter.setMessageConverter(messageConverter);
        adapter.setDefaultListenerMethod("handleMessage");
        container.setMessageListener(adapter);
        
        return container;
    }
    
    /**
     * 消息处理内部类
     */
    private class DanmakuMessageHandler implements MessageListener {
        @Override
        public void onMessage(Message message) {
            try {
                // 转换消息
                DanmakuMessage danmakuMessage = (DanmakuMessage) messageConverter.fromMessage(message);
                
                // 处理消息
                danmakuProcessService.processDanmaku(danmakuMessage);
            } catch (Exception e) {
                log.error("Failed to process danmaku message: {}", e.getMessage(), e);
            }
        }
    }
    
    @Override
    public void destroy() {
        // 停止所有监听容器
        for (MessageListenerContainer container : listenerContainers) {
            container.stop();
        }
        
        // 停止所有热门房间的监听容器
        for (List<MessageListenerContainer> containers : hotRoomContainers.values()) {
            for (MessageListenerContainer container : containers) {
                container.stop();
            }
        }
    }
} 