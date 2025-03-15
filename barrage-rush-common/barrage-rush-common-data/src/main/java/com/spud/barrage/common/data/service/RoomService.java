package com.spud.barrage.common.data.service;

import com.spud.barrage.common.data.entity.RoomConfig;

/**
 * @author Spud
 * @date 2025/3/6
 */
public interface RoomService {

  /**
   * 获取房间配置
   */
  RoomConfig getRoomConfig(Long roomId);
}