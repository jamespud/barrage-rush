package com.spud.barrage.ws.config;

/**
 * @author Spud
 * @date 2025/3/23
 */
public enum RequestType {
  DANMAKU("DANMAKU"),
  HEARTBEAT("HEARTBEAT");
  
  
  private final String type;

  RequestType(String type) {
    this.type = type;
  }

  public String getType() {
    return type;
  }
}
