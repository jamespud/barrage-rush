package com.spud.barrage.common.core.constant;

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
  public static final String SEND_DANMAKU = "/send/{roomId}";
  public static final String GET_RECENT = "/recent/{roomId}";

  // 房间相关
  public static final String ROOM_PREFIX = "/api/v1/rooms";
  public static final String ROOM_CONFIG = "/{roomId}/config";
  public static final String ROOM_ACTION = "/{roomId}/action";
}