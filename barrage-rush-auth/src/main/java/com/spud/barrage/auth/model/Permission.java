package com.spud.barrage.auth.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 权限实体类
 *
 * @author Spud
 * @date 2025/3/27
 */
@Data
@Entity
@Table(name = "auth_permission")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Permission {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /**
   * 权限名称（如：user:create，对应Spring Security的authority）
   */
  @Column(unique = true, nullable = false, length = 100)
  private String name;

  /**
   * 权限描述
   */
  @Column(length = 200)
  private String description;

  /**
   * 权限类型（MENU:菜单, BUTTON:按钮, API:接口）
   */
  @Column(length = 20)
  private String type;

  /**
   * 权限路径（如URL或前端路由）
   */
  @Column(length = 200)
  private String path;

  /**
   * HTTP方法（GET,POST,PUT,DELETE等）
   */
  @Column(length = 20)
  private String method;

  /**
   * 权限状态（0:禁用，1:启用）
   */
  private Integer status = 1;

  /**
   * 权限排序（值越小优先级越高）
   */
  private Integer sort;

  /**
   * 父权限ID
   */
  private Long parentId;

  /**
   * 创建时间
   */
  private LocalDateTime createTime;

  /**
   * 更新时间
   */
  private LocalDateTime updateTime;

  /**
   * 权限-角色关联
   */
  @ManyToMany(mappedBy = "permissions")
  private Set<Role> roles = new HashSet<>();

  @PrePersist
  protected void onCreate() {
    createTime = LocalDateTime.now();
    updateTime = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updateTime = LocalDateTime.now();
  }
}