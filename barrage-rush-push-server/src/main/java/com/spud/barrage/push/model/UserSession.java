package com.spud.barrage.push.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户会话
 *
 * @author Spud
 * @date 2025/3/15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSession implements Serializable {

  private static final long serialVersionUID = 1L;

  /**
   * 会话ID
   */
  private String sessionId;

  /**
   * 用户ID
   */
  private Long userId;

  /**
   * 房间ID
   */
  private Long roomId;

  /**
   * 用户昵称
   */
  private String nickname;

  /**
   * 用户头像
   */
  private String avatar;

  /**
   * 用户IP
   */
  private String ip;

  /**
   * 用户位置
   */
  private String location;

  /**
   * 连接时间
   */
  private LocalDateTime connectTime;

  /**
   * 最后活跃时间
   */
  private LocalDateTime lastActiveTime;

  /**
   * 数据连接会话ID
   */
  private String dataSessionId;

  /**
   * 心跳连接会话ID
   */
  private String heartbeatSessionId;

  /**
   * 服务器ID
   */
  private Integer serverId;

  /**
   * 是否在线
   */
  private Boolean online;

  /**
   * 更新最后活跃时间
   */
  public void updateLastActiveTime() {
    this.lastActiveTime = LocalDateTime.now();
  }
} 