package com.spud.barrage.common.constant;

/**
 * @author Spud
 * @date 2025/3/5
 */
public class ApiConstants {
  // WebSocket相关
  public static final String WS_ENDPOINT = "/ws/danmaku/{roomId}";

  // HTTP API相关
  public static final String API_PREFIX = "/api/v1/danmaku";
  public static final String SEND_DANMAKU = "/send";
  public static final String GET_RECENT = "/recent";
  public static final String ROOM_STATUS = "/room/status";

  // Redis Key前缀
  public static final String REDIS_ROOM_USERS = "room:%s:users";
  public static final String REDIS_USER_CONNECTION = "user:%s:connection";
  public static final String REDIS_ROOM_MESSAGES = "room:%s:messages";
  public static final String REDIS_USER_LIMIT = "limit:user:%s:count";
  public static final String REDIS_ROOM_CONFIG = "room:%s:config";
}