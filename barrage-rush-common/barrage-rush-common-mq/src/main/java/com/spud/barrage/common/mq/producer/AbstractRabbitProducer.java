package com.spud.barrage.common.mq.producer;

import com.spud.barrage.common.data.dto.DanmakuMessage;
import com.spud.barrage.common.mq.config.DynamicQueueConfig;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.util.Pair;

/**
 * @author Spud
 * @date 2025/3/12
 */
public abstract class AbstractRabbitProducer {

  private RabbitTemplate rabbitTemplate;

  protected boolean sendMessage(Long roomId, Long userId, DanmakuMessage message) {
    Pair<Object, Object> exchangeAndQueue = DynamicQueueConfig.getExchangeAndQueue(roomId);
    Object exchange = exchangeAndQueue.getFirst();
    Object queue = exchangeAndQueue.getSecond();
    // TODO: 消息确认
    rabbitTemplate.send(exchange.toString(), queue.toString(), new Message(message.toString().getBytes()));
    // 发送消息
    return false;
  }
}
