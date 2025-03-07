package com.spud.barrage.model.dto;

import lombok.Data;

/**
 * @author Spud
 * @date 2025/3/6
 */
@Data
public class WebSocketRequest {
  private String type;    // 消息类型
  private String data;    // 消息数据
}