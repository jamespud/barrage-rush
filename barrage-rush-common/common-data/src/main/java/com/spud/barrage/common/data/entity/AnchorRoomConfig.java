package com.spud.barrage.common.data.entity;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author Spud
 * @date 2025/3/6
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Builder
public class AnchorRoomConfig extends BaseEntity {

  // 房间ID
  private Long roomId;

  // 是否允许发送弹幕
  private Boolean allowDanmaku;

  // 弹幕最大长度
  private Integer maxLength;

  // 发送间隔(秒)
  private Integer interval;

  // 过滤规则(JSON)
  private String filters;

  // 房间状态(0:关闭,1:开启)
  private Integer status;

}