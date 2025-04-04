package com.spud.barrage.common.mq.config;

import com.spud.barrage.common.mq.constant.MqConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ配置
 * <p>
 * 基于房间流量和热度，管理消息队列资源分配的配置类
 * </p>
 * <p>
 * exchange分为共享和独享两种类型:
 * - 共享类型: 可被多个queue绑定，类型为topic
 * - 独享类型: 一般只绑定一个queue，类型为direct
 * </p>
 * <p>
 * queue分为共享和独享两种类型:
 * - 共享类型: 用于冷门房间，多个房间共享同一队列
 * - 独享类型: 用于热门房间，每个房间独立队列，热度高的可以有多个分片
 * </p>
 * <p>
 * 房间类型与资源分配关系:
 * - 超热门: 独享exchange，多分片队列 (5+)
 * - 热门: 独享exchange，多分片队列 (2-4)
 * - 普通: 独享exchange，独享队列
 * - 冷门: 共享exchange，共享队列
 * </p>
 *
 * @author Spud
 * @date 2025/4/01
 */
@Component
public class RabbitMQConfig {

  // 前缀定义 - 通过常量类引用
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

  // Redis频道主题，用于监听房间MQ配置变化 - 使用常量类
  public static final String ROOM_MQ_CHANGE_TOPIC = MqConstants.RedisTopic.ROOM_MQ_CHANGE;

  // 房间类型阈值配置
  public static int superHotViewersThreshold; // 超热门阈值
  public static int hotViewersThreshold; // 热门阈值
  public static int coldViewersThreshold; // 冷门阈值

  /**
   * 初始化超热门阈值
   * 默认10000观众为超热门直播间
   */
  @Value("${barrage.room.viewers.super-hot-threshold:10000}")
  public void setSuperHotViewersThreshold(int threshold) {
    RabbitMQConfig.superHotViewersThreshold = threshold;
  }

  /**
   * 初始化热门阈值
   * 默认1000观众为热门直播间
   */
  @Value("${barrage.room.viewers.hot-threshold:1000}")
  public void setHotViewersThreshold(int threshold) {
    RabbitMQConfig.hotViewersThreshold = threshold;
  }

  /**
   * 初始化冷门阈值
   * 默认10观众以下为冷门直播间
   */
  @Value("${barrage.room.viewers.cold-threshold:10}")
  public void setColdViewersThreshold(int threshold) {
    RabbitMQConfig.coldViewersThreshold = threshold;
  }
}
