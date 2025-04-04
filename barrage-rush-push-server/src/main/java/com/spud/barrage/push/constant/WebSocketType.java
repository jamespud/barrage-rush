package com.spud.barrage.push.constant;

/**
 * WebSocket类型常量
 *
 * @author Spud
 * @date 2025/3/30
 */
public interface WebSocketType {

  /**
   * 系统消息
   */
  String SYSTEM = "SYSTEM";

  /**
   * 弹幕消息
   */
  String DANMAKU = "DANMAKU";

  /**
   * 游戏状态消息
   */
  String GAME_STATE = "GAME_STATE";

  /**
   * 直播状态消息
   */
  String LIVE_STATE = "LIVE_STATE";

  /**
   * 心跳WebSocket
   */
  String HEARTBEAT = "heartbeat";

  /**
   * CDN信息WebSocket
   */
  String CDN = "cdn";
}