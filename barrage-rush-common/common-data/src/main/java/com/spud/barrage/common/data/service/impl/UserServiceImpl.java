package com.spud.barrage.common.data.service.impl;

import com.spud.barrage.common.data.entity.User;
import com.spud.barrage.common.data.repository.UserRepository;
import com.spud.barrage.common.data.service.UserService;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * @author Spud
 * @date 2025/3/10
 */
@Service
public class UserServiceImpl implements UserService {

  private UserRepository userRepository;

  @Override
  public boolean saveUser(User user) {
    userRepository.save(user);
    return true;
  }

  @Override
  public boolean deleteUser(Long userId) {
    userRepository.deleteById(userId);
    return true;
  }

  @Override
  public boolean updateUser(User user) {
    userRepository.save(user);
    return true;
  }

  @Override
  public User getUserById(Long userId) {
    Optional<User> user = userRepository.findById(userId);
    return user.orElse(null);
  }

  @Override
  public User getUserByUsername(String username) {
    return userRepository.findByUsername(username);
  }
}
