package com.spud.barrage.common.mq.consumer;

import com.spud.barrage.common.cluster.manager.InstanceManager;
import com.spud.barrage.common.data.config.RedisConfig;
import com.spud.barrage.common.mq.config.CacheManager;
import com.spud.barrage.common.mq.config.DynamicConsumerConfig;
import com.spud.barrage.constant.RoomType;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.stereotype.Component;

/**
 * @author Spud
 * @date 2025/3/23
 */
@Component
@RequiredArgsConstructor
public class InstanceChangeListener {
  private final InstanceManager instanceManager;
  private final DynamicConsumerConfig consumerConfig;
  private final RedisTemplate<String, Object> redisTemplate;
  private final CacheManager cacheManager;

  // Redis订阅频道名称
  private static final String INSTANCE_CHANGE_CHANNEL = "mq:instance:change";

  @PostConstruct
  public void init() {
    // 监听实例变化事件
    MessageListenerAdapter adapter = new MessageListenerAdapter((MessageListener) (message, pattern) -> {
      rebalanceRooms();
    });

    RedisMessageListenerContainer container = new RedisMessageListenerContainer();
    container.setConnectionFactory(redisTemplate.getConnectionFactory());
    container.addMessageListener(adapter, new ChannelTopic(INSTANCE_CHANGE_CHANNEL));
    container.start();
  }

  // 实例发生变化时，重新平衡房间分配
  private void rebalanceRooms() {
    Set<String> activeRooms = redisTemplate.keys(RedisConfig.ACTIVE_ROOM);
    if (activeRooms.isEmpty()) {
      return;
    }

    // 解绑不再负责的房间
    for (String queueName : new ArrayList<>(consumerConfig.getBoundQueues())) {
      try {
        Long roomId = extractRoomId(queueName);
        if (roomId != null && !instanceManager.isResponsibleFor(roomId)) {
          consumerConfig.unbindConsumerFromQueue(queueName);
        }
      } catch (Exception e) {
        // 忽略解析错误
      }
    }

    // 绑定新负责的房间
    for (String roomKey : activeRooms) {
      try {
        Long roomId = Long.parseLong(roomKey.split(":")[1]);
        if (instanceManager.isResponsibleFor(roomId)) {
          RoomType roomType = cacheManager.getRoomType(roomId);
          consumerConfig.bindConsumerToRoom(roomId, roomType);
        }
      } catch (Exception e) {
        // 忽略绑定错误
      }
    }
  }

  // 从队列名提取房间ID
  private Long extractRoomId(String queueName) {
    try {
      String[] parts = queueName.split("\\.");
      if (parts.length >= 3) {
        return Long.parseLong(parts[parts.length - 2]);
      }
    } catch (NumberFormatException e) {
      // 忽略解析错误
    }
    return null;
  }
}