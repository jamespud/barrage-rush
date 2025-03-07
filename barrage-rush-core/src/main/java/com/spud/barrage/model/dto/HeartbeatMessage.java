package com.spud.barrage.model.dto;

import lombok.Builder;
import lombok.Data;

/**
 * @author Spud
 * @date 2025/3/6
 */
@Data
@Builder
public class HeartbeatMessage {

  private Long timestamp;    // 时间戳
  private String sessionId;  // 会话ID
  private Integer type;      // 消息类型(1:ping, 2:pong)
}