package com.spud.barrage.common.data.config;

/**
 * @author Spud
 * @date 2025/3/11
 */
public class RedisConfig {
  
  // 房间类型改变时间
  public static String ROOM_TYPE_CHANGE = "room:change:%d";

  // 观众
  public static String ROOM_VIEWER = "room:%s:viewers";
  public static String ACTIVE_ROOM = "room:*:viewers";

  // 直播间ws连接数
  public static String ROOM_CONNECTION = "room:%s:connections";
  public static String ALL_ROOM_CONNECTION = "room:*:connections";
  
  // exchange名字 {type}:{exId}
  public static String EXCHANGE_NAME = "exchange:%s:%d";
  // 直播间队列名 {type}:{exId}:{queueId}
  public static String QUEUE_NAME = "queue:%s:%d:%d";

  // 直播间和mq的绑定关系
  // room -> set(exchange)
  public static String ROOM_EXCHANGE = "room:%d:exchange";
  // queue -> set(queue)
  public static String ROOM_QUEUE = "room:%s:queue";
  // exchange -> set(queue) 
  // 最多10个
  public static String EXCHANGE_QUEUE = "exchange:%s:queue";
  // queue -> set(routing key)
  public static String EXCHANGE_ROUTING_KEY = "queue:%s:routingKey";

  // 直播间类型变化事件 {roomId}
  public static String ROOM_MQ_EVENT = "mq:event:%d";
  
  // 所有直播间流量变化锁
  public static String ALL_ROOM_STATE_LOCK = "room:state:lock";
  // 直播间流量变化锁
  public static String PER_ROOM_STATE = "room:state:lock:%d";
  
  // 所有队列相关 {type} -> set(queue) exchange名
  public static String EXCHANGE_TYPE = "exchange:type:*";
  
  
}
