package com.spud.barrage.common.data.entity;

import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * @author Spud
 * @date 2025/3/16
 */
@Data
@Table(name = "anchor_room_silence")
public class AnchorRoomSilence {
  
  @Id
  private Long id;
  
  private Long userId;
  
  private Long roomId;
  
  private Long endTime;
  
}
