package com.spud.barrage.common.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 注册请求DTO
 *
 * @author Spud
 * @date 2025/3/27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest implements Serializable {

  private static final long serialVersionUID = 1L;

  /**
   * 用户名
   */
  @NotBlank(message = "用户名不能为空")
  @Size(min = 4, max = 20, message = "用户名长度应在4-20之间")
  @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "用户名仅支持字母、数字、下划线和连字符")
  private String username;

  /**
   * 密码
   */
  @NotBlank(message = "密码不能为空")
  @Size(min = 6, max = 20, message = "密码长度应在6-20之间")
  private String password;

  /**
   * 确认密码
   */
  @NotBlank(message = "确认密码不能为空")
  private String confirmPassword;

  /**
   * 邮箱
   */
  @NotBlank(message = "邮箱不能为空")
  @Email(message = "邮箱格式不正确")
  private String email;

  /**
   * 手机号
   */
  @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
  private String phoneNumber;

  /**
   * 昵称
   */
  @Size(max = 20, message = "昵称长度不能超过20")
  private String nickname;

  /**
   * 验证码
   */
  @NotBlank(message = "验证码不能为空")
  private String captcha;

  /**
   * 验证码ID
   */
  @NotBlank(message = "验证码ID不能为空")
  private String captchaId;
}