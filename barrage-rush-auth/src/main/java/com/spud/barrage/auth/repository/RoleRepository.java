package com.spud.barrage.auth.repository;

import com.spud.barrage.auth.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 角色仓库接口
 * 
 * @author Spud
 * @date 2025/3/27
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * 根据角色名称查询角色
     *
     * @param name 角色名称
     * @return 角色可选对象
     */
    Optional<Role> findByName(String name);

    /**
     * 检查角色名称是否存在
     *
     * @param name 角色名称
     * @return 是否存在
     */
    boolean existsByName(String name);
}