package com.spud.barrage.damaku.service;

import com.spud.barrage.common.data.dto.DanmakuMessage;
import com.spud.barrage.common.data.dto.DanmakuRequest;
import java.util.Collection;

/**
 * @author Spud
 * @date 2025/3/4
 */
public interface DanmakuService {

  /**
   * 处理弹幕消息
   *
   * @param request 弹幕请求
   * @return 处理后的弹幕消息，如果处理失败返回null
   */
  DanmakuMessage processDanmaku(DanmakuRequest request);

  /**
   * 获取房间最近弹幕
   *
   * @param roomId 房间ID
   * @param limit  获取数量限制
   * @return 房间最近的弹幕消息集合
   */
  Collection<DanmakuMessage> getRecentDanmaku(Long roomId, int limit);

  /**
   * 检查用户发送权限
   *
   * @param userId 用户ID
   * @param roomId 房间ID
   * @return 是否有发送权限
   */
  boolean checkSendPermission(Long userId, Long roomId);
}