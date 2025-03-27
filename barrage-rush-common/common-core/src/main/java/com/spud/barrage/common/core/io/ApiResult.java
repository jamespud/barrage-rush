package com.spud.barrage.common.core.io;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通用API结果响应类
 *
 * @author Spud
 * @date 2025/3/27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResult<T> implements Serializable {

  private static final long serialVersionUID = 1L;

  /**
   * 状态码
   */
  private Integer code;

  /**
   * 消息
   */
  private String message;

  /**
   * 数据
   */
  private T data;

  /**
   * 请求成功
   */
  public static <T> ApiResult<T> success() {
    return ApiResult.<T>builder()
        .code(ResultCode.SUCCESS.getCode())
        .message(ResultCode.SUCCESS.getMessage())
        .build();
  }

  /**
   * 请求成功，带数据
   */
  public static <T> ApiResult<T> success(T data) {
    return ApiResult.<T>builder()
        .code(ResultCode.SUCCESS.getCode())
        .message(ResultCode.SUCCESS.getMessage())
        .data(data)
        .build();
  }

  /**
   * 请求成功，自定义消息和数据
   */
  public static <T> ApiResult<T> success(String message, T data) {
    return ApiResult.<T>builder()
        .code(ResultCode.SUCCESS.getCode())
        .message(message)
        .data(data)
        .build();
  }

  /**
   * 请求失败
   */
  public static <T> ApiResult<T> failed() {
    return ApiResult.<T>builder()
        .code(ResultCode.FAILED.getCode())
        .message(ResultCode.FAILED.getMessage())
        .build();
  }

  /**
   * 请求失败，自定义消息
   */
  public static <T> ApiResult<T> failed(String message) {
    return ApiResult.<T>builder()
        .code(ResultCode.FAILED.getCode())
        .message(message)
        .build();
  }

  /**
   * 请求失败，自定义状态码和消息
   */
  public static <T> ApiResult<T> failed(ResultCode resultCode) {
    return ApiResult.<T>builder()
        .code(resultCode.getCode())
        .message(resultCode.getMessage())
        .build();
  }

  /**
   * 请求失败，自定义状态码、消息和数据
   */
  public static <T> ApiResult<T> failed(ResultCode resultCode, T data) {
    return ApiResult.<T>builder()
        .code(resultCode.getCode())
        .message(resultCode.getMessage())
        .data(data)
        .build();
  }

  /**
   * 请求失败，自定义状态码和消息
   */
  public static <T> ApiResult<T> failed(Integer code, String message) {
    return ApiResult.<T>builder()
        .code(code)
        .message(message)
        .build();
  }

  /**
   * 参数验证失败
   */
  public static <T> ApiResult<T> validateFailed() {
    return ApiResult.<T>builder()
        .code(ResultCode.VALIDATE_FAILED.getCode())
        .message(ResultCode.VALIDATE_FAILED.getMessage())
        .build();
  }

  /**
   * 参数验证失败，自定义消息
   */
  public static <T> ApiResult<T> validateFailed(String message) {
    return ApiResult.<T>builder()
        .code(ResultCode.VALIDATE_FAILED.getCode())
        .message(message)
        .build();
  }

  /**
   * 未登录
   */
  public static <T> ApiResult<T> unauthorized() {
    return ApiResult.<T>builder()
        .code(ResultCode.UNAUTHORIZED.getCode())
        .message(ResultCode.UNAUTHORIZED.getMessage())
        .build();
  }

  /**
   * 未授权
   */
  public static <T> ApiResult<T> forbidden() {
    return ApiResult.<T>builder()
        .code(ResultCode.FORBIDDEN.getCode())
        .message(ResultCode.FORBIDDEN.getMessage())
        .build();
  }
}