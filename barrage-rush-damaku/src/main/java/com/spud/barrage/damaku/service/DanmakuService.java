package com.spud.barrage.damaku.service;

import com.spud.barrage.common.data.dto.DanmakuMessage;
import java.util.List;

/**
 * @author Spud
 * @date 2025/3/4
 */
public interface DanmakuService {

  /**
   * 处理弹幕消息
   */
  void processDanmaku(DanmakuMessage message);

  /**
   * 获取房间最近弹幕
   *
   * @param roomId 房间ID
   * @param userId 用户ID
   * @param limit  时间线限制 当前时间多少毫秒以前
   */
  List<DanmakuMessage> getRecentDanmaku(Long roomId, Long userId, int limit);

  /**
   * 检查用户发送权限
   */
  boolean checkSendPermission(String userId, String roomId);
}