package com.spud.barrage.model.entity;

import lombok.Builder;
import lombok.Data;

/**
 * @author Spud
 * @date 2025/3/6
 */
@Data
@Builder
public class RoomConfig {

  private Long roomId;           // 房间ID
  private Boolean allowDanmaku;  // 是否允许发送弹幕
  private Integer maxLength;     // 弹幕最大长度
  private Integer interval;      // 发送间隔(秒)
  private String filters;        // 过滤规则(JSON)
  private Integer status;        // 房间状态(0:关闭,1:开启)
  private Long updateTime;       // 更新时间
}