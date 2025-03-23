package com.spud.barrage.common.mq.config;

import com.spud.barrage.common.data.config.RedisConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ配置
 * <p>
 * exchange分为共享和独享两种类型，共享类型的exchange可以被多个queue绑定，独享类型的exchange只能被一个queue绑定
 * 共享类型的exchange为topic，独享类型的exchange为direct
 * <p>
 * queue分为共享和独享两种类型，共享类型的queue可以换绑exchange，独享类型的queue仅能绑定exchange一次，且不能换绑
 * <p>
 * routing key用于区分共享exchange不同的queue
 * <p>
 * 直播间与exchange、queue、routing key的关系见 {@link RedisConfig}
 *
 * @author Spud
 * @date 2025/3/11
 */
@Component
public class RabbitMQConfig {

  // 前缀定义
  public static final String DANMAKU_EXCHANGE_PREFIX = "danmaku:exchange";
  public static final String DANMAKU_QUEUE_PREFIX = "danmaku:queue";
  public static final String DANMAKU_ROUTING_KEY_PREFIX = "danmaku:routing";

  // 管理所有交换机 - {ex类型}
  public static final String ALL_EXCHANGE_KEY = DANMAKU_EXCHANGE_PREFIX + ":%s:*";
  // 管理交换机的状态 - {ex类型}:{状态}
  public static final String EXCHANGE_IDLE = DANMAKU_EXCHANGE_PREFIX + ":%s:idle";
  public static final String EXCHANGE_USED = DANMAKU_EXCHANGE_PREFIX + ":%s:used";

  // 管理所有队列 - {queue类型}
  public static final String ALL_QUEUE_KEY = DANMAKU_QUEUE_PREFIX + ":%s:*";
  // 管理队列的状态 - {queue类型}:{状态}
  public static final String QUEUE_IDLE = DANMAKU_QUEUE_PREFIX + ":%s:idle";
  public static final String QUEUE_USED = DANMAKU_QUEUE_PREFIX + ":%s:used";
  
  // 共享队列房间映射
  public static final String SHARED_QUEUE_USED_ROOM = DANMAKU_QUEUE_PREFIX + ":shared:used:%d";

  // 弹幕路由键模式
  public static final String DANMAKU_ROUTING_KEY = "danmaku:routing:%s";

  // Redis频道主题，用于监听房间MQ配置变化
  public static final String ROOM_MQ_CHANGE_TOPIC = "room:mq:change";
  
  // 静态引用
  public static int hotViewersThreshold;
  public static int coldViewersThreshold;
  
  /**
   * 初始化静态字段
   */
  @Value("${rabbitmq.room.hot-viewers-threshold:30000}")
  public void setHotViewersThreshold(int threshold) {
    RabbitMQConfig.hotViewersThreshold = threshold;
  }
  
  @Value("${rabbitmq.room.cold-viewers-threshold:1000}")
  public void setColdViewersThreshold(int threshold) {
    RabbitMQConfig.coldViewersThreshold = threshold;
  }
}
