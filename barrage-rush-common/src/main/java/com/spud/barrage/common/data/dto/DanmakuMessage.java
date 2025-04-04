package com.spud.barrage.common.data.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 弹幕消息DTO
 *
 * @author Spud
 * @date 2025/3/30
 */
@Data
public class DanmakuMessage implements Serializable {

  private static final long serialVersionUID = 1L;

  /**
   * 消息ID
   */
  private Long messageId;

  /**
   * 房间ID
   */
  private String roomId;

  /**
   * 用户ID
   */
  private String userId;

  /**
   * 发送时间戳
   */
  private Long timestamp;

  /**
   * 弹幕内容
   */
  private String content;

  /**
   * 弹幕颜色（十六进制颜色代码）
   */
  private String color;

  /**
   * 弹幕大小
   * 默认25，范围15-50
   */
  private Integer size;

  /**
   * 弹幕位置
   * 0：滚动弹幕
   * 1：顶部固定弹幕
   * 2：底部固定弹幕
   */
  private Integer position;

  /**
   * 附加数据
   */
  private Map<String, Object> extra;
} 