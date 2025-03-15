package com.spud.barrage.constant;

/**
 * @author Spud
 * @date 2025/3/5
 */
public class ApiConstants {
  // WebSocket相关
  public static final String DANMAKU_ENDPOINT = "/ws/danmaku/{roomId}";
  public static final String HEARTBEAT_ENDPOINT = "/ws/danmaku/{roomId}";

  // 弹幕相关
  public static final String DANMAKU_PREFIX = "/api/v1/danmaku";
  public static final String SEND_DANMAKU = "/send";
  public static final String GET_RECENT = "/recent";
  // 房间相关
  public static final String ROOM_PREFIX = "/api/v1/danmaku";
  public static final String ROOM_CONFIG = "/rooms/{roomId}/config";
  public static final String ROOM_ACTION = "/rooms/{roomId}/action";

  // Redis Key前缀
  public static final String REDIS_ROOM_USERS = "room:%s:users";
  public static final String REDIS_USER_CONNECTION = "user:%s:connection";
  public static final String REDIS_ROOM_MESSAGES = "room:%s:messages";
  public static final String REDIS_USER_LIMIT = "limit:user:%s:count";
  public static final String REDIS_ROOM_CONFIG = "room:%s:config";
  public static final String REDIS_USER_BAN = "room:%s:config";
}