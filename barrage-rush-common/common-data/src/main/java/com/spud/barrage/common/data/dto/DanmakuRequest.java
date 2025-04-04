package com.spud.barrage.common.data.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import lombok.Data;

/**
 * 弹幕请求对象
 * 用于接收客户端发送的弹幕请求
 *
 * @author Spud
 * @date 2025/4/10
 */
@Data
public class DanmakuRequest implements Serializable {

  private static final long serialVersionUID = 1L;

  /**
   * 房间ID
   */
  @NotNull(message = "房间ID不能为空")
  private Long roomId;

  /**
   * 弹幕内容
   */
  @NotBlank(message = "弹幕内容不能为空")
  @Size(min = 1, max = 200, message = "弹幕内容长度必须在1-200之间")
  private String content;

  /**
   * 弹幕颜色（十六进制颜色代码）
   * 默认白色：#FFFFFF
   */
  private String color = "#FFFFFF";

  /**
   * 弹幕大小
   * 默认25，范围15-50
   */
  private Integer size = 25;

  /**
   * 弹幕位置
   * 0：滚动弹幕（默认）
   * 1：顶部固定弹幕
   * 2：底部固定弹幕
   */
  private Integer position = 0;

  /**
   * 时间戳（毫秒）
   */
  private Long timestamp;

  public DanmakuMessage createDanmakuMessage(Long messageId, Long userId) {
    return new DanmakuMessage(messageId, userId, this);
  }
}