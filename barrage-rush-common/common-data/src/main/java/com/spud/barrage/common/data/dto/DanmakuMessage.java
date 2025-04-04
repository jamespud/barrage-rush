package com.spud.barrage.common.data.dto;

import com.spud.barrage.common.data.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.io.Serializable;
import lombok.Builder;
import lombok.Getter;
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
public class DanmakuMessage extends BaseEntity implements Serializable {

  private static final long serialVersionUID = 1L;

  /**
   * 消息ID
   */
  @Id
  private Long id;

  /**
   * 房间ID
   */
  private Long roomId;

  /**
   * 用户ID
   */
  private Long userId;

  /**
   * 弹幕内容
   */
  private String content;

  /**
   * 弹幕位置
   * 0：滚动弹幕（默认）
   * 1：顶部固定弹幕
   * 2：底部固定弹幕
   */
  private Integer position = 0;

  /**
   * 弹幕颜色
   */
  private String color;

  /**
   * 弹幕字体大小
   */
  private Integer size;

  /**
   * 时间戳（毫秒）
   */
  private Long timestamp;

  public DanmakuMessage() {

  }

  public DanmakuMessage(Long id, Long roomId, Long userId, String content,
      String color, Integer size, Integer position, Long timestamp) {
    this.id = id;
    this.roomId = roomId;
    this.userId = userId;
    this.content = content;
    this.color = color;
    this.size = size;
    this.position = position;
    this.timestamp = timestamp;
  }

  public DanmakuMessage(Long id, Long userId, DanmakuRequest request) {
    this(id, request.getRoomId(), userId, request.getContent(), request.getColor(),
        request.getSize(), request.getPosition(), request.getTimestamp());
  }
}