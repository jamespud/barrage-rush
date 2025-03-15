package com.spud.barrage.common.data.dto;

import lombok.Builder;
import lombok.Data;

/**
 * @author Spud
 * @date 2025/3/6
 */
@Data
@Builder
public class HeartbeatMessage {

  // 时间戳
  private Long timestamp;
  // 会话ID
  private String sessionId;
  // 消息类型(1:ping, 2:pong)
  private Integer type;
}