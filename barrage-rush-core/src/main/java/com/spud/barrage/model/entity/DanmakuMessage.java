package com.spud.barrage.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Spud
 * @date 2025/3/4
 */
@Entity
@Table(name = "danmaku_message", indexes = {
    @Index(name = "idx_room_time", columnList = "roomId,createTime")
})
@Getter
@Setter
@Builder
@ToString
@RequiredArgsConstructor
@AllArgsConstructor
public class DanmakuMessage {

  @Id
  private Long id;

  private String roomId;
  private Long userId;
  private String username;
  private String content;
  private DanmakuType type;
  private String style;
  private Long timestamp;
  private LocalDateTime createTime;
  private String region;
}