package com.spud.barrage.common.mq.config;

import com.spud.barrage.common.data.config.RedisConfig;
import org.springframework.beans.factory.annotation.Value;

/**
 * RabbitMQ配置
 * <p>
 * exchange分为共享和独享两种类型，共享类型的exchange可以被多个queue绑定，独享类型的exchange只能被一个queue绑定
 * 共享类型的exchange为topic，独享类型的exchange为direct
 * <p>
 * queue分为共享和独享两种类型，共享类型的queue可以换绑exchange，独享类型的queue仅能绑定exchange一次，且不能换绑
 * <p>
 * routing key暂时没有实际含义，只是用于区分共享exchange不同的queue
 * <p>
 * 直播间与exchange、queue、routing key的关系见 {@link RedisConfig}
 * @author Spud
 * @date 2025/3/11
 */
public class RabbitMQConfig {

  public static final String DANMAKU_EXCHANGE_PREFIX = "danmaku.exchange";
  public static final String DANMAKU_QUEUE_PREFIX = "danmaku.queue";
  public static final String DANMAKU_ROUTING_KEY_PREFIX = "danmaku.routing";

  // {ex类型}.{ex编号}
  public static final String DANMAKU_EXCHANGE_SHARED = DANMAKU_EXCHANGE_PREFIX + ".share.%s";
  public static final String DANMAKU_EXCHANGE_UNIQUE = DANMAKU_EXCHANGE_PREFIX + ".unique.%s";

  // {ex编号}.{队列编号}
  public static final String DANMAKU_QUEUE_SHARED = DANMAKU_QUEUE_PREFIX + ".shared.%s.%s";
  public static final String DANMAKU_QUEUE_UNIQUE = DANMAKU_QUEUE_PREFIX + ".unique.%s.%s";

  // {房间id}
  public static final String DANMAKU_ROUTING_KEY = "danmaku.routing.%s";


  // 热门房间观看人数阈值
  @Value("${rabbitmq.room.hot-viewers-threshold:30000}")
  public static int hotViewersThreshold;

  // 冷门房间观看人数阈值
  @Value("${rabbitmq.room.cold-viewers-threshold:1000}")
  public static int coldViewersThreshold;

}
