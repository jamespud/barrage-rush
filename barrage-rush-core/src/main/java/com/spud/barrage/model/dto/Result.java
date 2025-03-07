package com.spud.barrage.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author Spud
 * @date 2025/3/6
 */
@Data
@AllArgsConstructor
public class Result<T> {

  private Integer code;
  private String message;
  private T data;

  public static <T> Result<T> success(T data) {
    return new Result<>(200, "success", data);
  }

  public static <T> Result<T> success() {
    return success(null);
  }

  public static <T> Result<T> fail(String message) {
    return new Result<>(500, message, null);
  }

  public static <T> Result<T> fail(int code, String message) {
    return new Result<>(code, message, null);
  }
}