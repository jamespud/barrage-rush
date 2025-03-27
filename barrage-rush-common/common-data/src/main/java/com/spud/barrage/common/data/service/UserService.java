package com.spud.barrage.common.data.service;

import com.spud.barrage.common.data.entity.User;

/**
 * @author Spud
 * @date 2025/3/10
 */
public interface UserService {

  boolean saveUser(User user);

  boolean deleteUser(Long userId);

  boolean updateUser(User user);

  User getUserById(Long userId);

  User getUserByUsername(String username);

}
