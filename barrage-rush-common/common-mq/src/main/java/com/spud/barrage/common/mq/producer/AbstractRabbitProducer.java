package com.spud.barrage.common.mq.producer;

import com.spud.barrage.common.data.dto.DanmakuMessage;
import com.spud.barrage.common.mq.config.CoreMQConfig;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;

/**
 * @author Spud
 * @date 2025/3/12
 */
@Slf4j
public abstract class AbstractRabbitProducer {

  @Autowired
  private RabbitTemplate rabbitTemplate;

  @Autowired
  private CoreMQConfig coreMQConfig;

  /**
   * 发送弹幕
   *
   * @param message 弹幕消息
   * @return 是否发送成功
   */
  public abstract boolean sendDanmaku(DanmakuMessage message);

  /**
   * 发送消息到消息队列
   *
   * @param roomId  房间ID
   * @param userId  用户ID
   * @param message 消息内容
   * @return 是否发送成功
   */
  protected boolean sendMessage(Long roomId, Long userId, DanmakuMessage message) {
    try {
      // 获取房间对应的Exchange和Queue
      Pair<Object, Object> exchangeAndQueue = coreMQConfig.getExchangeAndQueue(roomId);
      String exchange = exchangeAndQueue.getFirst().toString();
      String routingKey = exchangeAndQueue.getSecond().toString();

      if (exchange.isEmpty() || routingKey.isEmpty()) {
        log.error("Failed to get exchange or routing key for room {}", roomId);
        return false;
      }

      // 创建关联数据用于跟踪消息发送状态
      CorrelationData correlationData = new CorrelationData(UUID.randomUUID().toString());

      // 发送消息
      rabbitTemplate.convertAndSend(exchange, routingKey, message, correlationData);

      // 等待确认结果
      try {
        CorrelationData.Confirm confirm = correlationData.getFuture().get(2, TimeUnit.SECONDS);
        if (confirm != null && confirm.isAck()) {
          log.debug("Message sent successfully: id={}", correlationData.getId());
          return true;
        } else {
          String reason = confirm != null ? confirm.getReason() : "unknown";
          log.error("Message not acknowledged: id={}, reason={}", correlationData.getId(), reason);
          return false;
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        log.error("Interrupted while waiting for message confirmation: {}", e.getMessage());
      } catch (ExecutionException e) {
        log.error("Error getting message confirmation: {}", e.getMessage());
      } catch (TimeoutException e) {
        log.error("Timeout waiting for message confirmation: {}", e.getMessage());
      }

      return false;
    } catch (Exception e) {
      log.error("Failed to send message to room {}: {}", roomId, e.getMessage(), e);
      return false;
    }
  }
}
