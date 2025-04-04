package com.spud.barrage.auth.service;

import com.spud.barrage.common.auth.dto.AuthResponse;
import com.spud.barrage.common.auth.dto.LoginRequest;
import com.spud.barrage.common.auth.dto.RefreshTokenRequest;
import com.spud.barrage.common.auth.dto.RegisterRequest;

/**
 * 认证服务接口
 *
 * @author Spud
 * @date 2025/3/27
 */
public interface AuthService {

  /**
   * 用户登录
   *
   * @param loginRequest 登录请求
   * @return 认证响应
   */
  AuthResponse login(LoginRequest loginRequest);

  /**
   * 用户注册
   *
   * @param registerRequest 注册请求
   * @return 认证响应
   */
  AuthResponse register(RegisterRequest registerRequest);

  /**
   * 刷新令牌
   *
   * @param refreshTokenRequest 刷新令牌请求
   * @return 认证响应
   */
  AuthResponse refreshToken(RefreshTokenRequest refreshTokenRequest);

  /**
   * 用户登出
   *
   * @param token 访问令牌
   */
  void logout(String token);

  /**
   * 验证验证码
   *
   * @param captchaId 验证码ID
   * @param captcha   验证码
   * @return 是否验证成功
   */
  boolean verifyCaptcha(String captchaId, String captcha);

  /**
   * 生成验证码
   *
   * @return 验证码信息，包含验证码ID和验证码图片Base64
   */
  Object generateCaptcha();

  /**
   * 修改密码
   *
   * @param username    用户名
   * @param oldPassword 旧密码
   * @param newPassword 新密码
   * @return 是否修改成功
   */
  boolean changePassword(String username, String oldPassword, String newPassword);
}