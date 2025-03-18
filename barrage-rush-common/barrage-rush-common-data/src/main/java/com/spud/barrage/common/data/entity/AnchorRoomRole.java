package com.spud.barrage.common.data.entity;

import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author Spud
 * @date 2025/3/10
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Table(name = "anchor_room_role")
public class AnchorRoomRole extends BaseEntity {

  @Id
  private Long roomId;

  private Long userId;

  // 用户角色 1: 主播 2: 管理员
  private int role;
}
