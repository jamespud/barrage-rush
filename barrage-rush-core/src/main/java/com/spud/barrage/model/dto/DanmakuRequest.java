package com.spud.barrage.model.dto;

import com.spud.barrage.model.entity.DanmakuType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

/**
 * @author Spud
 * @date 2025/3/6
 */
@Data
@Builder
public class DanmakuRequest {

  @NotBlank(message = "房间ID不能为空")
  private String roomId;

  @NotNull(message = "消息内容不能为空")
  @Size(min = 1, max = 100, message = "弹幕内容长度必须在1-100之间")
  private String content;
  private DanmakuType type;
  private DanmakuStyle style;

  @Data
  @Builder
  public static class DanmakuStyle {

    private String color;    // 颜色
    private Integer fontSize;// 字体大小
    private Integer position;// 位置
    private Boolean bold;    // 是否加粗
    private Float speed;     // 速度
  }
}