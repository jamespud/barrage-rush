package com.spud.barrage.exception;

import lombok.Getter;

/**
 * @author Spud
 * @date 2025/3/6
 */
@Getter
public class BarrageException extends RuntimeException {

  private final int code;

  public BarrageException(String message) {
    super(message);
    this.code = 500;
  }

  public BarrageException(int code, String message) {
    super(message);
    this.code = code;
  }
}