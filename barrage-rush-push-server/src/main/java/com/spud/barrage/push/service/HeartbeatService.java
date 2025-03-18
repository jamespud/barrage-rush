package com.spud.barrage.push.service;

import com.spud.barrage.common.data.dto.HeartbeatMessage;

/**
 * 心跳服务接口
 *
 * @author Spud
 * @date 2025/3/15
 */
public interface HeartbeatService {

  /**
   * 更新心跳
   *
   * @param sessionId 会话ID
   * @param message   心跳消息
   * @return 是否成功
   */
  boolean updateHeartbeat(String sessionId, HeartbeatMessage message);

  /**
   * 检查心跳是否有效
   *
   * @param sessionId 会话ID
   * @return 是否有效
   */
  boolean isHeartbeatValid(String sessionId);

  /**
   * 删除心跳
   *
   * @param sessionId 会话ID
   * @return 是否成功
   */
  boolean deleteHeartbeat(String sessionId);

  /**
   * 获取最后一次心跳消息
   *
   * @param sessionId 会话ID
   * @return 心跳消息
   */
  HeartbeatMessage getLastHeartbeat(String sessionId);

  /**
   * 处理心跳消息
   *
   * @param sessionId 会话ID
   * @param message   心跳消息
   * @return 响应消息
   */
  HeartbeatMessage processHeartbeat(String sessionId, HeartbeatMessage message);
} 