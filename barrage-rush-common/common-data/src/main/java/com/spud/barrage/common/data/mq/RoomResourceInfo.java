package com.spud.barrage.common.data.mq;

import com.spud.barrage.common.data.mq.enums.RoomType;

/**
 * 房间资源信息
 * 存储房间相关的MQ资源配置
 *
 * @author Spud
 * @date 2025/4/01
 */
public class RoomResourceInfo {

  /**
   * 房间ID
   */
  private Long roomId;

  /**
   * 房间类型
   */
  private RoomType roomType;

  /**
   * 交换机名称
   */
  private String exchangeName;

  /**
   * 队列名称(主队列)
   */
  private String queueName;

  /**
   * 分片队列名称列表，用逗号分隔
   */
  private String shardQueues;

  /**
   * 创建时间
   */
  private Long createTime;

  /**
   * 最后更新时间
   */
  private Long updateTime;

  public Long getRoomId() {
    return roomId;
  }

  public void setRoomId(Long roomId) {
    this.roomId = roomId;
  }

  public RoomType getRoomType() {
    return roomType;
  }

  public void setRoomType(RoomType roomType) {
    this.roomType = roomType;
  }

  public String getExchangeName() {
    return exchangeName;
  }

  public void setExchangeName(String exchangeName) {
    this.exchangeName = exchangeName;
  }

  public String getQueueName() {
    return queueName;
  }

  public void setQueueName(String queueName) {
    this.queueName = queueName;
  }

  public String getShardQueues() {
    return shardQueues;
  }

  public void setShardQueues(String shardQueues) {
    this.shardQueues = shardQueues;
  }

  public Long getCreateTime() {
    return createTime;
  }

  public void setCreateTime(Long createTime) {
    this.createTime = createTime;
  }

  public Long getUpdateTime() {
    return updateTime;
  }

  public void setUpdateTime(Long updateTime) {
    this.updateTime = updateTime;
  }

  @Override
  public String toString() {
    return "RoomResourceInfo{" +
        "roomId=" + roomId +
        ", roomType=" + roomType +
        ", exchangeName='" + exchangeName + '\'' +
        ", queueName='" + queueName + '\'' +
        ", shardQueues='" + shardQueues + '\'' +
        ", createTime=" + createTime +
        ", updateTime=" + updateTime +
        '}';
  }
}