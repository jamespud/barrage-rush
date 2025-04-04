package com.spud.barrage.common.auth.dto;

import java.util.Collection;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * 弹幕系统用户详情DTO
 *
 * @author Spud
 * @date 2023/3/27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BarrageUserDetail implements UserDetails {

  private static final long serialVersionUID = 1L;

  /**
   * 用户ID
   */
  private Long userId;

  /**
   * 用户名
   */
  private String username;

  /**
   * 密码
   */
  private String password;

  /**
   * 权限列表
   */
  private Collection<? extends GrantedAuthority> authorities;

  /**
   * 是否启用
   */
  private boolean enabled;

  /**
   * 账号是否未过期
   */
  private boolean accountNonExpired;

  /**
   * 账号是否未锁定
   */
  private boolean accountNonLocked;

  /**
   * 密码是否未过期
   */
  private boolean credentialsNonExpired;

  /**
   * 权限名称列表（非Spring Security标准字段，用于业务逻辑）
   */
  private List<String> permissionNames;

  /**
   * 角色名称列表（非Spring Security标准字段，用于业务逻辑）
   */
  private List<String> roleNames;
}