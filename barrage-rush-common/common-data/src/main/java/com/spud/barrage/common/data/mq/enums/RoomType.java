package com.spud.barrage.common.data.mq.enums;

/**
 * 房间类型枚举
 * 定义不同热度等级的房间类型
 *
 * @author Spud
 * @date 2025/4/01
 */
public enum RoomType {

  /**
   * 超热门房间
   * 有大量观众，需要多个分片队列
   */
  SUPER_HOT(3, "超热门"),

  /**
   * 热门房间
   * 有较多观众，需要几个分片队列
   */
  HOT(2, "热门"),

  /**
   * 普通房间
   * 观众数适中，使用单个独享队列
   */
  NORMAL(1, "普通"),

  /**
   * 冷门房间
   * 观众很少，使用共享队列
   */
  COLD(0, "冷门");

  /**
   * 类型编码
   */
  private final int code;

  /**
   * 类型描述
   */
  private final String desc;

  RoomType(int code, String desc) {
    this.code = code;
    this.desc = desc;
  }

  public int getCode() {
    return code;
  }

  public String getDesc() {
    return desc;
  }

  /**
   * 根据编码获取房间类型
   *
   * @param code 类型编码
   * @return 房间类型枚举，默认返回NORMAL
   */
  public static RoomType getByCode(int code) {
    for (RoomType type : RoomType.values()) {
      if (type.getCode() == code) {
        return type;
      }
    }
    return NORMAL;
  }

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