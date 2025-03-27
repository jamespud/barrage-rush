package com.spud.barrage.common.data.dto;

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
  @Size(min = 64, max = 64, message = "房间ID长度必须为64位")
  private Long roomId;

  @NotNull(message = "消息内容不能为空")
  @Size(min = 1, max = 128, message = "弹幕内容长度必须在1-128之间")
  private String content;

  @NotNull(message = "消息类型不能为空")
  private DanmakuType type;

  private String style;

}