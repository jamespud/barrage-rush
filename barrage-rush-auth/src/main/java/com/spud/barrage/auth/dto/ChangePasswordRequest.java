package com.spud.barrage.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 修改密码请求DTO
 *
 * @author Spud
 * @date 2025/3/27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequest implements Serializable {

  private static final long serialVersionUID = 1L;

  /**
   * 旧密码
   */
  @NotBlank(message = "旧密码不能为空")
  private String oldPassword;

  /**
   * 新密码
   */
  @NotBlank(message = "新密码不能为空")
  @Size(min = 6, max = 20, message = "密码长度应在6-20之间")
  private String newPassword;

  /**
   * 确认新密码
   */
  @NotBlank(message = "确认新密码不能为空")
  private String confirmPassword;
}