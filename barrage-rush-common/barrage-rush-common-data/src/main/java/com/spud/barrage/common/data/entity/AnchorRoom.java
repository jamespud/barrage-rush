package com.spud.barrage.common.data.entity;

import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Date;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author Spud
 * @date 2025/3/10
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Table(name = "anchor_room")
public class AnchorRoom extends BaseEntity {

  @Id
  private Long roomId;

  private String roomName;

  // 等于 User.userId
  private Long anchorId;

  private Date lastPlayTime;

  // 房间状态 0: 关闭 1: 开启 2: 直播中 3: 已结束
  private int roomStatus;
}
