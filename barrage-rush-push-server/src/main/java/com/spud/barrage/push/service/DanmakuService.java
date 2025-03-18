package com.spud.barrage.push.service;

import com.spud.barrage.common.data.dto.DanmakuMessage;
import java.util.List;

/**
 * 弹幕服务接口
 *
 * @author Spud
 * @date 2025/3/15
 */
public interface DanmakuService {

  /**
   * 获取房间最新弹幕消息
   *
   * @param roomId 房间ID
   * @param count  消息数量
   * @return 弹幕消息列表
   */
  List<DanmakuMessage> getLatestMessages(Long roomId, int count);

  /**
   * 获取房间弹幕消息（从指定索引开始）
   *
   * @param roomId     房间ID
   * @param startIndex 起始索引
   * @param count      消息数量
   * @return 弹幕消息列表
   */
  List<DanmakuMessage> getMessages(Long roomId, long startIndex, int count);

  /**
   * 获取房间弹幕消息数量
   *
   * @param roomId 房间ID
   * @return 消息数量
   */
  long getMessageCount(Long roomId);

  /**
   * 保存弹幕消息
   *
   * @param message 弹幕消息
   * @return 是否成功
   */
  boolean saveMessage(DanmakuMessage message);

  /**
   * 批量保存弹幕消息
   *
   * @param messages 弹幕消息列表
   * @return 成功保存的消息数量
   */
  int saveMessages(List<DanmakuMessage> messages);

  /**
   * 删除房间弹幕消息
   *
   * @param roomId 房间ID
   * @return 是否成功
   */
  boolean deleteRoomMessages(Long roomId);

  /**
   * 删除指定消息
   *
   * @param roomId    房间ID
   * @param messageId 消息ID
   * @return 是否成功
   */
  boolean deleteMessage(Long roomId, String messageId);
} 