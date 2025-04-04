package com.spud.barrage.auth.repository;

import com.spud.barrage.auth.model.Permission;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * 权限仓库接口
 *
 * @author Spud
 * @date 2025/3/27
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

  /**
   * 根据权限名称查询权限
   *
   * @param name 权限名称
   * @return 权限可选对象
   */
  Optional<Permission> findByName(String name);

  /**
   * 根据权限路径和请求方法查询权限
   *
   * @param path   权限路径
   * @param method 请求方法
   * @return 权限可选对象
   */
  Optional<Permission> findByPathAndMethod(String path, String method);

  /**
   * 查询所有有效的权限
   *
   * @return 有效权限列表
   */
  @Query("SELECT p FROM Permission p WHERE p.status = 1")
  List<Permission> findAllActivePermissions();

  /**
   * 根据父ID查询权限列表
   *
   * @param parentId 父ID
   * @return 权限列表
   */
  List<Permission> findByParentId(Long parentId);

  /**
   * 查询所有顶级权限
   *
   * @return 顶级权限列表
   */
  @Query("SELECT p FROM Permission p WHERE p.parentId IS NULL OR p.parentId = 0")
  List<Permission> findAllTopLevelPermissions();
}