package com.spud.barrage.common.data.service;

import com.spud.barrage.common.data.entity.AnchorRoomConfig;

/**
 * @author Spud
 * @date 2025/3/6
 */
public interface RoomService {

  /**
   * 获取房间配置
   */
  AnchorRoomConfig getRoomConfig(Long roomId);
}