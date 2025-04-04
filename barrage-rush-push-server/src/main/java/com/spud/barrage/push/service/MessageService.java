package com.spud.barrage.push.service;

import com.spud.barrage.common.data.dto.DanmakuMessage;
import java.util.List;
import java.util.Map;

/**
 * 消息服务接口
 * 用于创建、发布和管理消息
 *
 * @author Spud
 * @date 2025/3/30
 */
public interface MessageService {

  /**
   * 创建弹幕消息
   *
   * @param roomId 房间ID
   * @param userId 用户ID
   * @param data   弹幕数据
   * @return 弹幕消息
   */
  DanmakuMessage createDanmakuMessage(Long roomId, Long userId, Map<String, Object> data);

  /**
   * 发布弹幕消息
   *
   * @param message 弹幕消息
   */
  void publishDanmakuMessage(DanmakuMessage message);

  /**
   * 获取最近的弹幕消息
   *
   * @param roomId 房间ID
   * @param limit  限制数量
   * @return 弹幕消息列表
   */
  List<DanmakuMessage> getRecentMessages(Long roomId, int limit);

  /**
   * 标记消息为已确认
   *
   * @param messageId 消息ID
   * @param userId    用户ID
   */
  void markMessageAcknowledged(Long messageId, Long userId);

  /**
   * 获取系统状态信息
   *
   * @param roomId 房间ID
   * @return 系统状态信息
   */
  Map<String, Object> getSystemStatus(Long roomId);
}