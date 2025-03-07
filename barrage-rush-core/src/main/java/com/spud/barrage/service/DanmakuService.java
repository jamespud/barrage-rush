package com.spud.barrage.service;

import com.spud.barrage.model.dto.DanmakuRequest;
import com.spud.barrage.model.entity.DanmakuMessage;
import com.spud.barrage.model.entity.RoomConfig;
import java.util.List;

/**
 * @author Spud
 * @date 2025/3/4
 */
public interface DanmakuService {


  /**
   * 发送弹幕
   * @param request 弹幕请求
   * @param userId 用户ID
   * @param username 用户名
   * @return 消息ID
   */
  String sendDanmaku(DanmakuRequest request, Long userId, String username);
  
  /**
   * 处理弹幕消息
   */
  void processDanmaku(DanmakuMessage message);

  /**
   * 获取房间最近弹幕
   */
  List<DanmakuMessage> getRecentDanmaku(String roomId, int limit);

  /**
   * 检查用户发送权限
   */
  boolean checkSendPermission(String userId, String roomId);

  /**
   * 获取房间配置
   */
  RoomConfig getRoomConfig(String roomId);

  /**
   * 更新房间配置
   */
  void updateRoomConfig(RoomConfig config);
}