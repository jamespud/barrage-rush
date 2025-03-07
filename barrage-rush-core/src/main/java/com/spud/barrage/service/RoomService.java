package com.spud.barrage.service;

import com.spud.barrage.model.entity.RoomConfig;
import java.util.Set;

/**
 * @author Spud
 * @date 2025/3/6
 */
public interface RoomService {
  /**
   * 获取房间在线用户
   */
  Set<String> getOnlineUsers(String roomId);

  /**
   * 获取用户所在房间
   */
  String getUserRoom(String userId);

  /**
   * 用户进入房间
   */
  void joinRoom(String userId, String roomId);

  /**
   * 用户离开房间
   */
  void leaveRoom(String userId, String roomId);

  /**
   * 获取房间配置
   */
  RoomConfig getRoomConfig(String roomId);
}