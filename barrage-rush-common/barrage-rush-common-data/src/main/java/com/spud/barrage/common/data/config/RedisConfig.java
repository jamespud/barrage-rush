package com.spud.barrage.common.data.config;

/**
 * @author Spud
 * @date 2025/3/11
 */
public class RedisConfig {
  // 观众
  public static String ROOM_VIEWER = "room:%s:viewers";
  public static String ACTIVE_ROOM = "room:*:viewers";
  
  // 直播间ws连接数
  public static String ROOM_CONNECTION = "room:%s:connections";
  public static String ALL_ROOM_CONNECTION = "room:*:connections";
  
  // 直播间和mq的绑定关系
  // room -> set(exchange)
  public static String ROOM_EXCHANGE = "room:%s:exchange";
  // queue -> set(queue)
  public static String ROOM_QUEUE = "room:%s:queue";
  // exchange -> set(queue)
  public static String EXCHANGE_QUEUE ="exchange:%s:queue";
  // queue -> set(routing key)
  public static String EXCHANGE_ROUTING_KEY = "queue:%s:routingKey";
  

}
