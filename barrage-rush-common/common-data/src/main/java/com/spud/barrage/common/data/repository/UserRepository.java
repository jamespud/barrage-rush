package com.spud.barrage.common.data.repository;

import com.spud.barrage.common.data.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author Spud
 * @date 2025/3/10
 */
public interface UserRepository extends JpaRepository<User, Long> {
  
  User findByUsername(String username);

}
