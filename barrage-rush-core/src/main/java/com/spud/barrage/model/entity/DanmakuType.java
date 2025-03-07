package com.spud.barrage.model.entity;

/**
 * @author Spud
 * @date 2025/3/4
 */
public enum DanmakuType {
  NORMAL(0, "普通弹幕"),
  TOP(1, "顶部固定"),
  BOTTOM(2, "底部固定"),
  REVERSE(3, "逆向弹幕"),
  SPECIAL(4, "特殊弹幕");

  private final int code;
  private final String desc;

  DanmakuType(int code, String desc) {
    this.code = code;
    this.desc = desc;
  }

  public int getCode() {
    return code;
  }

  public String getDesc() {
    return desc;
  }
}