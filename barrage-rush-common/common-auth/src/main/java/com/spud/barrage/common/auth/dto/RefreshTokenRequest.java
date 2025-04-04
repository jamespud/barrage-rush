package com.spud.barrage.common.auth.dto;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 刷新令牌请求DTO
 *
 * @author Spud
 * @date 2023/3/27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenRequest implements Serializable {

  private static final long serialVersionUID = 1L;

  /**
   * 刷新令牌
   */
  @NotBlank(message = "刷新令牌不能为空")
  private String refreshToken;

  /**
   * 是否为敏感操作
   * 敏感操作需要验证token版本号，确保在用户修改密码后，旧的refresh token无法用于敏感操作
   */
  private boolean sensitiveOperation = false;
}