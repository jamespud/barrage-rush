package com.spud.barrage.common.data.dto;

import lombok.Builder;
import lombok.Data;

/**
 * @author Spud
 * @date 2025/3/6
 */
@Data
@Builder
public class UserSession {

  private String sessionId;      // 会话ID
  private Long userId;           // 用户ID
  private Long roomId;           // 房间ID
  private Long connectTime;      // 连接时间
  private Long lastHeartbeat;    // 最后心跳时间
  private String clientInfo;     // 客户端信息
  private Integer status;        // 会话状态(0:初始化,1:已连接,2:心跳中)
}