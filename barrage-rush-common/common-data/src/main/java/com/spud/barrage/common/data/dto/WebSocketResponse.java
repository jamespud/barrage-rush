package com.spud.barrage.common.data.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Spud
 * @date 2025/3/7
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketResponse<T> {

  private String type;
  private T data;
}