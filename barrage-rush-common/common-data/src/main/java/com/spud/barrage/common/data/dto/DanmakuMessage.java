package com.spud.barrage.common.data.dto;

import com.spud.barrage.common.data.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
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
public class DanmakuMessage extends BaseEntity {

  
  /**
   * 消息ID
   */
  @Id
  private Long messageId;

  /**
   * 房间ID
   */
  private Long roomId;

  /**
   * 用户ID
   */
  private Long userId;

  /**
   * 用户名称
   */
  private String username;

  /**
   * 用户头像
   */
  private String avatar;

  /**
   * 弹幕内容
   */
  private String content;

  /**
   * 弹幕类型
   */
  private DanmakuType type;

  /**
   * 弹幕样式
   */
  private String style;

  /**
   * 弹幕颜色
   */
  private String color;

  /**
   * 弹幕字体大小
   */
  private Integer fontSize;

  /**
   * 时间戳（毫秒）
   */
  private Long timestamp;

  /**
   * 额外数据
   */
  private Object extra;

  public DanmakuMessage() {

  }

  public DanmakuMessage(Long id, Long roomId, Long userId, String content, DanmakuType type,
      String style) {
    // TODO: 唯一ID生成
    this.messageId = System.currentTimeMillis();
    this.roomId = roomId;
    this.userId = userId;
    this.content = content;
    this.type = type;
    this.style = style;
    this.timestamp = System.currentTimeMillis();
  }

  public DanmakuMessage(Long id, Long userId, DanmakuRequest request) {
    this(id, request.getRoomId(), userId, request.getContent(), request.getType(),
        request.getStyle());
  }
}