package com.spud.barrage.consumer.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spud.barrage.common.cluster.manager.InstanceManager;
import com.spud.barrage.common.data.config.RedisLockUtils;
import com.spud.barrage.common.mq.config.RoomResourceManager;
import com.spud.barrage.common.mq.consumer.DefaultConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 弹幕消费者
 * 负责监听和消费RabbitMQ中的弹幕消息
 *
 * @author Spud
 * @date 2025/3/11
 */
@Slf4j
@Component
public class DanmakuConsumer extends DefaultConsumer {

  public DanmakuConsumer(AmqpAdmin amqpAdmin,
      RedisTemplate<String, Object> redisTemplate,
      RedisConnectionFactory redisConnectionFactory,
      ObjectMapper objectMapper,
      RabbitListenerEndpointRegistry registry,
      RoomResourceManager roomResourceManager,
      InstanceManager instanceManager,
      RedisLockUtils redisLockUtils) {
    super(amqpAdmin, redisTemplate, redisConnectionFactory, objectMapper, registry,
        roomResourceManager, instanceManager, redisLockUtils);
  }
}