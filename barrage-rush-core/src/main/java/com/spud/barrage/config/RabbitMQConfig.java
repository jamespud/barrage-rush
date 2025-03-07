package com.spud.barrage.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Spud
 * @date 2025/3/4
 */
@Configuration
public class RabbitMQConfig {

  // 弹幕交换机
  public static final String DANMAKU_EXCHANGE = "danmaku.exchange";
  // 弹幕队列
  public static final String DANMAKU_QUEUE = "danmaku.queue";
  // 弹幕路由键
  public static final String DANMAKU_ROUTING_KEY = "danmaku.routing.key";

  @Bean
  public Exchange danmakuExchange() {
    return ExchangeBuilder.directExchange(DANMAKU_EXCHANGE)
        .durable(true)
        .build();
  }

  @Bean
  public Queue danmakuQueue() {
    return QueueBuilder.durable(DANMAKU_QUEUE)
        .build();
  }

  @Bean
  public Binding danmakuBinding() {
    return BindingBuilder.bind(danmakuQueue())
        .to(danmakuExchange())
        .with(DANMAKU_ROUTING_KEY)
        .noargs();
  }

  @Bean
  public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
    RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
    rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
    return rabbitTemplate;
  }
}