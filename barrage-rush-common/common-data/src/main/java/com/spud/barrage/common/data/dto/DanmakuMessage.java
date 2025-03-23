package com.spud.barrage.common.data.dto;

import com.spud.barrage.common.data.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.sql.Date;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.cache.annotation.CachePut;

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
public class DanmakuMessage extends BaseEntity {

  @Id
  private Long id;
  private Long roomId;
  private Long userId;
  private String content;
  private DanmakuType type;
  private String style;
  private Long sendTime;

  public DanmakuMessage() {

  }

  public DanmakuMessage(Long id, Long roomId, Long userId, String content, DanmakuType type,
      String style) {
    // TODO: 唯一ID生成
    this.id = System.currentTimeMillis();
    this.roomId = roomId;
    this.userId = userId;
    this.content = content;
    this.type = type;
    this.style = style;
    this.sendTime = System.currentTimeMillis();
  }

  public DanmakuMessage(Long id, Long userId, DanmakuRequest request) {
  this(id, request.getRoomId(), userId, request.getContent(), request.getType(), request.getStyle());
  }
}