package com.spud.barrage.common.data.mq;

import com.spud.barrage.common.data.mq.enums.RoomType;

/**
 * 房间事件信息
 * 用于通知房间状态变更
 *
 * @author Spud
 * @date 2025/4/01
 */
public class RoomEvent {

  /**
   * 房间ID
   */
  private Long roomId;

  /**
   * 旧房间类型
   */
  private RoomType oldType;

  /**
   * 新房间类型
   */
  private RoomType newType;

  /**
   * 事件时间戳
   */
  private Long timestamp;

  public Long getRoomId() {
    return roomId;
  }

  public void setRoomId(Long roomId) {
    this.roomId = roomId;
  }

  public RoomType getOldType() {
    return oldType;
  }

  public void setOldType(RoomType oldType) {
    this.oldType = oldType;
  }

  public RoomType getNewType() {
    return newType;
  }

  public void setNewType(RoomType newType) {
    this.newType = newType;
  }

  public Long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Long timestamp) {
    this.timestamp = timestamp;
  }

  @Override
  public String toString() {
    return "RoomEvent{" +
        "roomId=" + roomId +
        ", oldType=" + oldType +
        ", newType=" + newType +
        ", timestamp=" + timestamp +
        '}';
  }
}