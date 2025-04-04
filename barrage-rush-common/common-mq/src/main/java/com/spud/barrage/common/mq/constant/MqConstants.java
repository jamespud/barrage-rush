package com.spud.barrage.common.mq.constant;

/**
 * MQ相关常量
 * 统一管理消息队列中使用的常量值
 *
 * @author Spud
 * @date 2025/4/01
 */
public class MqConstants {

  /**
   * 队列名称格式
   */
  public static class QueueFormat {

    /** 冷门房间共享队列名称 */
    public static final String COLD_SHARED = "danmaku.queue.shared.cold";

    /** 普通房间队列名称格式 - 需要格式化roomId */
    public static final String NORMAL = "danmaku.queue.%d";

    /** 热门房间队列名称格式 - 需要格式化roomId和分片索引 */
    public static final String HOT = "danmaku.queue.%d.%d";
  }

  /**
   * 交换机名称格式
   */
  public static class ExchangeFormat {

    /** 共享交换机名称 */
    public static final String SHARED = "danmaku.exchange.shared";

    /** 独立交换机名称格式 - 需要格式化roomId */
    public static final String DEDICATED = "danmaku.exchange.%d";
  }

  /**
   * 路由键格式
   */
  public static class RoutingKeyFormat {

    /** 弹幕路由键格式 - 需要格式化roomId */
    public static final String DANMAKU = "danmaku.routing.%d";

    /** 系统消息路由键 */
    public static final String SYSTEM = "danmaku.routing.system";
  }

  /**
   * Redis主题
   */
  public static class RedisTopic {

    /** 房间MQ配置变更主题 */
    public static final String ROOM_MQ_CHANGE = "room:mq:change";

    /** 房间热度变更主题 */
    public static final String ROOM_HEAT_CHANGE = "room:heat:change";

    /** 实例变更主题 */
    public static final String INSTANCE_CHANGE = "mq:instance:change";
  }

  /**
   * Redis键格式
   */
  public static class RedisKey {

    /** 房间状态锁键格式 - 需要格式化roomId */
    public static final String ROOM_STATUS_LOCK = "lock:room:status:%d";

    public static final String ROOM_BINDING_LOCK = "bind:room:binding:%d";

    /** 房间资源信息键格式 - 需要格式化roomId */
    public static final String ROOM_RESOURCE = "room:resource:%d";

    /** 房间观众数键格式 - 需要格式化roomId */
    public static final String ROOM_VIEWERS = "room:viewers:%d";

    /** 活跃房间集合键 */
    public static final String ACTIVE_ROOMS = "room:active";

    /** 交换机ID生成器键 */
    public static final String EXCHANGE_ID_GENERATOR = "danmaku:exchange:id";

    /** 队列ID生成器键 */
    public static final String QUEUE_ID_GENERATOR = "danmaku:queue:id";

    /** 绑定关系键格式 - 需要格式化交换机名称和队列名称 */
    public static final String BINDING = "danmaku:binding:%s:%s";

    /** 房间弹幕速率键格式 - 需要格式化roomId */
    public static final String ROOM_DANMAKU_RATE = "room:%d:danmaku:rate";

    /** 空闲交换机列表键格式 - 需要格式化交换机类型 */
    public static final String EXCHANGE_IDLE = "danmaku:exchange:idle:%s";

    /** 已使用交换机列表键格式 - 需要格式化交换机类型 */
    public static final String EXCHANGE_USED = "danmaku:exchange:used:%s";

    /** 空闲队列列表键格式 - 需要格式化队列类型 */
    public static final String QUEUE_IDLE = "danmaku:queue:idle:%s";

    /** 已使用队列列表键格式 - 需要格式化队列类型 */
    public static final String QUEUE_USED = "danmaku:queue:used:%s";

    /** 房间类型变更键格式 - 需要格式化roomId */
    public static final String ROOM_TYPE_CHANGE = "room:type:change:%d";

    /** 房间交换机缓存键格式 - 需要格式化roomId */
    public static final String ROOM_EXCHANGE = "room:exchange:%d";

    /** 房间队列缓存键格式 - 需要格式化roomId */
    public static final String ROOM_QUEUE = "room:queue:%d";

    /** 房间MQ事件键格式 - 需要格式化roomId */
    public static final String ROOM_MQ_EVENT = "room:mq:event:%d";

    /** 房间用户列表键格式 - 需要格式化roomId */
    public static final String ROOM_USERS = "room:%d:users";

    /** 用户连接信息键格式 - 需要格式化userId */
    public static final String USER_CONNECTION = "user:%d:connection";

    /** 房间消息列表键格式 - 需要格式化roomId */
    public static final String ROOM_MESSAGES = "room:%d:messages";

    /** 用户发送频率限制键格式 - 需要格式化userId */
    public static final String USER_LIMIT = "limit:user:%d:count";

    /** 房间配置键格式 - 需要格式化roomId */
    public static final String ROOM_CONFIG = "room:%d:config";

    /** 房间禁言状态键格式 - 需要格式化roomId */
    public static final String ROOM_BAN = "ban:user:%d";

    /** 用户禁言状态键格式 - 需要格式化userId */
    public static final String USER_BAN = "ban:user:%d";

    /** 房间热度级别键格式 - 需要格式化roomId */
    public static final String ROOM_HEAT_LEVEL = "room:%d:heat:level";

    public static final String REDIS_ROOM_MESSAGES = "room:%s:messages";
  }

  /**
   * Redis缓存过期时间(秒)
   */
  public static class ExpirationTime {

    /** 房间类型缓存过期时间 */
    public static final int ROOM_TYPE = 180; // 3分钟

    /** 房间队列缓存过期时间 */
    public static final int ROOM_QUEUE = 180; // 3分钟

    /** 房间交换机缓存过期时间 */
    public static final int ROOM_EXCHANGE = 180; // 3分钟

    /** 速率限制过期时间 */
    public static final int RATE_LIMIT = 60; // 1分钟

    /** 用户连接信息过期时间 */
    public static final int USER_CONNECTION = 300; // 5分钟

    /** 用户禁言过期时间(默认) */
    public static final int USER_BAN_DEFAULT = 1800; // 30分钟

    /** 房间消息过期时间 */
    public static final int ROOM_MESSAGES = 300; // 5分钟
  }
}