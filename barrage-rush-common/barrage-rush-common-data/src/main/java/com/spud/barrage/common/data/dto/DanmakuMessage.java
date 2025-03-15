package com.spud.barrage.common.data.dto;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.amqp.core.Message;

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
@ToString
@AllArgsConstructor
public class DanmakuMessage    {

  @Id
  private Long id;

  private Long roomId;
  private Long userId;
  private String username;
  private String content;
  private DanmakuType type;
  private String style;
  private Long timestamp;
  private String region;

  public DanmakuMessage() {
    
  }
}