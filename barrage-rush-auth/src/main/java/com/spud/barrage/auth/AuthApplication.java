package com.spud.barrage.auth;

import com.spud.barrage.auth.config.AuthProperties;
import com.spud.barrage.auth.model.Permission;
import com.spud.barrage.auth.model.Role;
import com.spud.barrage.auth.model.User;
import com.spud.barrage.auth.repository.PermissionRepository;
import com.spud.barrage.auth.repository.RoleRepository;
import com.spud.barrage.auth.repository.UserRepository;
import com.spud.barrage.common.core.io.AuthConstants;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 认证服务启动类
 *
 * @author Spud
 * @date 2025/3/26
 */
@Slf4j
@RequiredArgsConstructor
@EnableConfigurationProperties({AuthProperties.class})
@SpringBootApplication(scanBasePackages = "com.spud.barrage")
public class AuthApplication {

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final PermissionRepository permissionRepository;
  private final PasswordEncoder passwordEncoder;

  public static void main(String[] args) {
    SpringApplication.run(AuthApplication.class, args);
  }

  /**
   * 初始化测试数据
   */
  @Bean
  public ApplicationRunner initDatabase() {
    return (ApplicationArguments args) -> {
      log.info("初始化测试数据...");

      // 检查是否已初始化
      if (roleRepository.count() > 0) {
        log.info("数据库已初始化，跳过初始化步骤");
        return;
      }

      // 创建权限
      Permission viewPermission = Permission.builder()
          .name("content:view")
          .description("查看内容权限")
          .type("MENU")
          .path("/content/view")
          .method("GET")
          .status(1)
          .sort(1)
          .build();

      Permission createPermission = Permission.builder()
          .name("content:create")
          .description("创建内容权限")
          .type("BUTTON")
          .path("/content/create")
          .method("POST")
          .status(1)
          .sort(2)
          .build();

      Permission updatePermission = Permission.builder()
          .name("content:update")
          .description("更新内容权限")
          .type("BUTTON")
          .path("/content/update")
          .method("PUT")
          .status(1)
          .sort(3)
          .build();

      Permission deletePermission = Permission.builder()
          .name("content:delete")
          .description("删除内容权限")
          .type("BUTTON")
          .path("/content/delete")
          .method("DELETE")
          .status(1)
          .sort(4)
          .build();

      Permission adminPermission = Permission.builder()
          .name("system:admin")
          .description("系统管理权限")
          .type("MENU")
          .path("/system/admin")
          .method("GET")
          .status(1)
          .sort(5)
          .build();

      List<Permission> permissions = permissionRepository.saveAll(
          Arrays.asList(viewPermission, createPermission, updatePermission, deletePermission,
              adminPermission));

      // 创建角色
      Role userRole = Role.builder()
          .name(AuthConstants.Security.ROLE_USER)
          .description("普通用户角色")
          .type("SYSTEM")
          .sort(2)
          .permissions(
              new HashSet<>(Arrays.asList(viewPermission, createPermission, updatePermission)))
          .build();

      Role adminRole = Role.builder()
          .name(AuthConstants.Security.ROLE_ADMIN)
          .description("管理员角色")
          .type("SYSTEM")
          .sort(1)
          .permissions(new HashSet<>(permissions))
          .build();

      List<Role> roles = roleRepository.saveAll(Arrays.asList(userRole, adminRole));

      // 创建测试用户
      User adminUser = User.builder()
          .username("admin")
          .password(passwordEncoder.encode("admin123"))
          .email("admin@example.com")
          .nickname("管理员")
          .avatarUrl("/avatar/admin.png")
          .enabled(true)
          .accountNonExpired(true)
          .accountNonLocked(true)
          .credentialsNonExpired(true)
          .lastLoginTime(LocalDateTime.now())
          .lastActiveTime(LocalDateTime.now())
          .roles(Collections.singleton(adminRole))
          .build();

      User normalUser = User.builder()
          .username("user")
          .password(passwordEncoder.encode("user123"))
          .email("user@example.com")
          .nickname("普通用户")
          .avatarUrl("/avatar/user.png")
          .enabled(true)
          .accountNonExpired(true)
          .accountNonLocked(true)
          .credentialsNonExpired(true)
          .lastLoginTime(LocalDateTime.now())
          .lastActiveTime(LocalDateTime.now())
          .roles(Collections.singleton(userRole))
          .build();

      userRepository.saveAll(Arrays.asList(adminUser, normalUser));

      log.info("测试数据初始化完成");
    };
  }
}
