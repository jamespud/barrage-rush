package com.spud.barrage.common.core.exception;

import lombok.Getter;

/**
 * @author Spud
 * @date 2025/3/6
 */
@Getter
public class BarrageException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /**
   * 错误代码
   */
  private final Integer code;

  /**
   * 错误消息
   */
  private final String message;

  /**
   * 构造函数
   *
   * @param message 错误消息
   */
  public BarrageException(String message) {
    this(500, message);
  }

  /**
   * 构造函数
   *
   * @param code    错误代码
   * @param message 错误消息
   */
  public BarrageException(Integer code, String message) {
    super(message);
    this.code = code;
    this.message = message;
  }
}