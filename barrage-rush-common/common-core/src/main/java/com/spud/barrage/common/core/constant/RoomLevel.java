package com.spud.barrage.common.core.constant;

import lombok.Getter;

/**
 * @author Spud
 * @date 2025/3/12
 */
@Getter
public enum RoomLevel {
  // 冷门房间：共享一个队列，按roomId路由
  COLD(1, "shared", "cold"),

  // 普通房间：独立队列，几个固定分片
  NORMAL(3, "dedicated", "normal"),

  // 热门房间：独立队列，更多分片，更高优先级
  HOT(10, "dedicated", "hot"),

  // 超热门房间：独立队列，最多分片，最高优先级
  SUPER_HOT(20, "dedicated", "super_hot");

  private final int defaultShards;
  private final String queueType;
  private final String priorityLevel;

  // 构造函数、getter等

  RoomLevel(int defaultShards, String queueType, String priorityLevel) {
    this.defaultShards = defaultShards;
    this.queueType = queueType;
    this.priorityLevel = priorityLevel;
  }
}