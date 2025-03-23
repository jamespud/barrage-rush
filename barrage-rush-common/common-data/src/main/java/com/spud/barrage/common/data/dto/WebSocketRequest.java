package com.spud.barrage.common.data.dto;

import lombok.Data;

/**
 * @author Spud
 * @date 2025/3/6
 */
@Data
public class WebSocketRequest<T> {

  // 消息类型
  private String type;

  // 消息数据
  private T data;
}