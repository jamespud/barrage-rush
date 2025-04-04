package com.spud.barrage.auth.repository;

import com.spud.barrage.auth.model.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * 用户仓库接口
 *
 * @author Spud
 * @date 2025/3/27
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

  /**
   * 根据用户名查询用户
   *
   * @param username 用户名
   * @return 用户可选对象
   */
  Optional<User> findByUsername(String username);

  /**
   * 根据邮箱查询用户
   *
   * @param email 邮箱
   * @return 用户可选对象
   */
  Optional<User> findByEmail(String email);

  /**
   * 根据手机号查询用户
   *
   * @param phoneNumber 手机号
   * @return 用户可选对象
   */
  Optional<User> findByPhoneNumber(String phoneNumber);

  /**
   * 检查用户名是否存在
   *
   * @param username 用户名
   * @return 是否存在
   */
  boolean existsByUsername(String username);

  /**
   * 检查邮箱是否存在
   *
   * @param email 邮箱
   * @return 是否存在
   */
  boolean existsByEmail(String email);

  /**
   * 检查手机号是否存在
   *
   * @param phoneNumber 手机号
   * @return 是否存在
   */
  boolean existsByPhoneNumber(String phoneNumber);

  /**
   * 统计在线用户数量
   *
   * @return 在线用户数量
   */
  @Query("SELECT COUNT(u) FROM User u WHERE u.lastActiveTime > CURRENT_TIMESTAMP - 1800000")
  long countOnlineUsers();
}