package com.spud.barrage.common.auth.dto;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录请求DTO
 *
 * @author Spud
 * @date 2025/3/27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest implements Serializable {

  private static final long serialVersionUID = 1L;

  /**
   * 用户名/邮箱/手机号
   */
  @NotBlank(message = "用户名不能为空")
  private String username;

  /**
   * 密码
   */
  @NotBlank(message = "密码不能为空")
  private String password;

  /**
   * 验证码
   */
  private String captcha;

  /**
   * 验证码ID
   */
  private String captchaId;

  /**
   * 记住我（7天有效期）
   */
  private Boolean rememberMe = false;
}