package com.spud.barrage.auth.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
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
 * 角色实体类
 *
 * @author Spud
 * @date 2025/3/27
 */
@Data
@Entity
@Table(name = "auth_role")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Role {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /**
   * 角色名称
   */
  @Column(unique = true, nullable = false, length = 50)
  private String name;

  /**
   * 角色描述
   */
  @Column(length = 200)
  private String description;

  /**
   * 角色类型（SYSTEM:系统角色, CUSTOM:自定义角色）
   */
  @Column(length = 20)
  private String type;

  /**
   * 角色排序（值越小优先级越高）
   */
  private Integer sort;

  /**
   * 创建时间
   */
  private LocalDateTime createTime;

  /**
   * 更新时间
   */
  private LocalDateTime updateTime;

  /**
   * 角色-权限关联
   */
  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(name = "auth_role_permission", joinColumns = @JoinColumn(name = "role_id"), inverseJoinColumns = @JoinColumn(name = "permission_id"))
  private Set<Permission> permissions = new HashSet<>();

  /**
   * 用户-角色关联
   */
  @ManyToMany(mappedBy = "roles")
  private Set<User> users = new HashSet<>();

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