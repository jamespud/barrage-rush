package com.spud.barrage.common.core.io;

/**
 * API结果状态码枚举
 *
 * @author Spud
 * @date 2025/3/27
 */
public enum ResultCode {
  /**
   * 成功
   */
  SUCCESS(200, "操作成功"),

  /**
   * 失败
   */
  FAILED(500, "操作失败"),

  /**
   * 参数验证失败
   */
  VALIDATE_FAILED(400, "参数检验失败"),

  /**
   * 未登录或token已过期
   */
  UNAUTHORIZED(401, "暂未登录或token已过期"),

  /**
   * 没有相关权限
   */
  FORBIDDEN(403, "没有相关权限"),

  /**
   * 资源不存在
   */
  NOT_FOUND(404, "资源不存在"),

  /**
   * 服务器运行异常
   */
  SERVER_ERROR(500, "服务器运行异常"),

  /**
   * 用户名或密码错误
   */
  USER_PASSWORD_ERROR(1001, "用户名或密码错误"),

  /**
   * 验证码错误
   */
  CAPTCHA_ERROR(1002, "验证码错误或已过期"),

  /**
   * 账号已被禁用
   */
  ACCOUNT_DISABLED(1003, "账号已被禁用"),

  /**
   * 账号已锁定
   */
  ACCOUNT_LOCKED(1004, "账号已锁定"),

  /**
   * 令牌失效
   */
  TOKEN_INVALID(1005, "令牌无效或已过期"),

  /**
   * 用户名已存在
   */
  USERNAME_EXISTS(1006, "用户名已存在"),

  /**
   * 邮箱已存在
   */
  EMAIL_EXISTS(1007, "邮箱已被注册"),

  /**
   * 手机号已存在
   */
  PHONE_EXISTS(1008, "手机号已被注册"),

  /**
   * 密码错误
   */
  PASSWORD_ERROR(1009, "密码错误"),

  /**
   * 两次密码不一致
   */
  PASSWORD_NOT_MATCH(1010, "两次密码输入不一致");

  private final Integer code;
  private final String message;

  ResultCode(Integer code, String message) {
    this.code = code;
    this.message = message;
  }

  public Integer getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }
}