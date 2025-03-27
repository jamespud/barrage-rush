package com.spud.barrage.common.core.constant;

/**
 * @author Spud
 * @date 2025/3/6
 */
public class Constants {

  // 业务相关常量
  public static final int MAX_CONTENT_LENGTH = 100;
  public static final int MAX_SEND_RATE = 60;  // 每分钟最大发送次数
  public static final int MAX_CONNECTIONS_PER_USER = 3;
  public static final int MAX_ROOM_MESSAGES = 1000;

  // 缓存相关常量
  public static final int ROOM_MESSAGES_EXPIRE = 5 * 60;  // 5分钟
  public static final int USER_SESSION_EXPIRE = 24 * 60 * 60;  // 24小时
  public static final int RATE_LIMIT_EXPIRE = 60;  // 1分钟

  // WebSocket相关常量
  public static final int HEARTBEAT_INTERVAL = 30;  // 30秒
  public static final int HEARTBEAT_TIMEOUT = 90;   // 90秒


}