package com.spud.barrage.common.exception;

import lombok.Getter;

/**
 * @author Spud
 * @date 2025/3/6
 */
@Getter
public class BusinessException extends RuntimeException {
  private final int code;

  public BusinessException(String message) {
    super(message);
    this.code = 500;
  }

  public BusinessException(int code, String message) {
    super(message);
    this.code = code;
  }
}