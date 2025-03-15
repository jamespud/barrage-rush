package com.spud.barrage.common.data.entity;

import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * @author Spud
 * @date 2025/3/10
 */
@Table(name = "room_user_role")
public class RoomUserRole extends BaseEntity {
  
  @Id
  private Long roomId;
  
  private Long userId;
  
  // 用户角色 1: 主播 2: 管理员 3: 观众
  private int role;
}
