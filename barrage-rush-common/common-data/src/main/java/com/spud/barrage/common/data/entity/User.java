package com.spud.barrage.common.data.entity;

import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Date;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author Spud
 * @date 2025/3/9
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Table(name = "user")
public class User extends BaseEntity {

  @Id
  private Long userId;

  // 用户名
  private String username;

  // 昵称
  private String nickname;

  // 密码
  private String password;

  // 用户头像URL
  private String avatar;

  // 手机号
  private String mobile;

  // 邮箱
  private String email;

  // 性别
  private int gender;

  // 生日
  private Date birthday;

  // 注册时间
  private Date registerTime;

  // 最后登录时间
  private Date lastLoginTime;

  // 状态 0: 启用 1: 禁用
  private boolean banned;
}

