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
   * 创建一个基础结果对象
   */
  private static <T> ApiResult<T> create(Integer code, String message, T data) {
    return ApiResult.<T>builder()
        .code(code)
        .message(message)
        .data(data)
        .build();
  }

  /**
   * 创建一个基于ResultCode的结果对象
   */
  private static <T> ApiResult<T> create(ResultCode resultCode, T data) {
    return create(resultCode.getCode(), resultCode.getMessage(), data);
  }

  /**
   * 请求成功
   */
  public static <T> ApiResult<T> success() {
    return create(ResultCode.SUCCESS, null);
  }

  /**
   * 请求成功，带数据
   */
  public static <T> ApiResult<T> success(T data) {
    return create(ResultCode.SUCCESS, data);
  }

  /**
   * 请求成功，自定义消息和数据
   */
  public static <T> ApiResult<T> success(String message, T data) {
    return create(ResultCode.SUCCESS.getCode(), message, data);
  }

  /**
   * 请求失败
   */
  public static <T> ApiResult<T> failed() {
    return create(ResultCode.FAILED, null);
  }

  /**
   * 请求失败，自定义消息
   */
  public static <T> ApiResult<T> failed(String message) {
    return create(ResultCode.FAILED.getCode(), message, null);
  }

  /**
   * 请求失败，自定义状态码和消息
   */
  public static <T> ApiResult<T> failed(ResultCode resultCode) {
    return create(resultCode, null);
  }

  /**
   * 请求失败，自定义状态码、消息和数据
   */
  public static <T> ApiResult<T> failed(ResultCode resultCode, T data) {
    return create(resultCode, data);
  }

  /**
   * 请求失败，自定义状态码和消息
   */
  public static <T> ApiResult<T> failed(Integer code, String message) {
    return create(code, message, null);
  }

  /**
   * 参数验证失败
   */
  public static <T> ApiResult<T> validateFailed() {
    return create(ResultCode.VALIDATE_FAILED, null);
  }

  /**
   * 参数验证失败，自定义消息
   */
  public static <T> ApiResult<T> validateFailed(String message) {
    return create(ResultCode.VALIDATE_FAILED.getCode(), message, null);
  }

  /**
   * 未登录
   */
  public static <T> ApiResult<T> unauthorized() {
    return create(ResultCode.UNAUTHORIZED, null);
  }

  /**
   * 未授权
   */
  public static <T> ApiResult<T> forbidden() {
    return create(ResultCode.FORBIDDEN, null);
  }
}