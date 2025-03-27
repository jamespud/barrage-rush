package com.spud.barrage.common.core.constant;

/**
 * 房间类型枚举
 * @author Spud
 * @date 2025/3/19
 */
public enum RoomType {
  // 冷门房间，共享队列
  COLD,
  // 普通房间，自己的队列
  NORMAL,
  // 热门房间，多分片队列
  HOT,
  // 超热门房间，多分片队列
  SUPER_HOT;

  public String getExchangeType() {
    return switch (this) {
      case COLD -> "shared";
      case NORMAL, HOT, SUPER_HOT -> "unique";
    };
  }

  public String getQueueType() {
    return switch (this) {
      case COLD -> "shared";
      case NORMAL, HOT, SUPER_HOT -> "unique";
    };
  }
}