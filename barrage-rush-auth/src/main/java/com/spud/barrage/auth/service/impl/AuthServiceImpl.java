package com.spud.barrage.auth.service.impl;

import com.spud.barrage.auth.dto.AuthResponse;
import com.spud.barrage.auth.dto.LoginRequest;
import com.spud.barrage.auth.dto.RefreshTokenRequest;
import com.spud.barrage.auth.dto.RegisterRequest;
import com.spud.barrage.auth.model.Role;
import com.spud.barrage.auth.model.User;
import com.spud.barrage.auth.repository.RoleRepository;
import com.spud.barrage.auth.repository.UserRepository;
import com.spud.barrage.auth.service.AuthService;
import com.spud.barrage.auth.util.JwtUtil;
import com.spud.barrage.common.core.exception.BarrageException;
import com.spud.barrage.common.core.io.Constants;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 认证服务实现类
 * 
 * @author Spud
 * @date 2025/3/27
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public AuthResponse login(LoginRequest loginRequest) {
        // 验证验证码
        if (loginRequest.getCaptcha() != null && loginRequest.getCaptchaId() != null) {
            boolean captchaValid = verifyCaptcha(loginRequest.getCaptchaId(), loginRequest.getCaptcha());
            if (!captchaValid) {
                throw new BarrageException("验证码错误");
            }
        }

        try {
            // 创建认证token
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                    loginRequest.getUsername(), loginRequest.getPassword());

            // 认证
            Authentication authentication = authenticationManager.authenticate(authenticationToken);

            // 设置认证信息
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 生成JWT令牌
            User userDetails = (User) authentication.getPrincipal();
            String accessToken = jwtUtil.generateToken(userDetails);
            String refreshToken = jwtUtil.generateRefreshToken(userDetails);

            // 更新用户最后登录时间
            userDetails.setLastLoginTime(LocalDateTime.now());
            userDetails.setLastActiveTime(LocalDateTime.now());
            userRepository.save(userDetails);

            // 记录在线用户
            String onlineUserKey = Constants.Redis.ONLINE_USER_PREFIX + userDetails.getId();
            redisTemplate.opsForValue().set(onlineUserKey, "1", 30, TimeUnit.MINUTES);

            // 返回认证响应
            return buildAuthResponse(userDetails, accessToken, refreshToken);
        } catch (BadCredentialsException e) {
            // 记录登录失败次数
            incrementLoginFailCount(loginRequest.getUsername());
            throw new BarrageException("用户名或密码错误");
        }
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest registerRequest) {
        // 验证验证码
        boolean captchaValid = verifyCaptcha(registerRequest.getCaptchaId(), registerRequest.getCaptcha());
        if (!captchaValid) {
            throw new BarrageException("验证码错误");
        }

        // 验证密码是否匹配
        if (!registerRequest.getPassword().equals(registerRequest.getConfirmPassword())) {
            throw new BarrageException("两次输入的密码不一致");
        }

        // 验证用户名是否已存在
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new BarrageException("用户名已存在");
        }

        // 验证邮箱是否已存在
        if (registerRequest.getEmail() != null && userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new BarrageException("邮箱已被注册");
        }

        // 验证手机号是否已存在
        if (registerRequest.getPhoneNumber() != null
                && userRepository.existsByPhoneNumber(registerRequest.getPhoneNumber())) {
            throw new BarrageException("手机号已被注册");
        }

        // 获取用户角色
        Role userRole = roleRepository.findByName(Constants.Security.ROLE_USER)
                .orElseThrow(() -> new BarrageException("用户角色不存在"));

        // 创建用户实体
        User user = User.builder()
                .username(registerRequest.getUsername())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .email(registerRequest.getEmail())
                .phoneNumber(registerRequest.getPhoneNumber())
                .nickname(registerRequest.getNickname() != null ? registerRequest.getNickname()
                        : registerRequest.getUsername())
                .avatarUrl("/avatar/default.png")
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .lastLoginTime(LocalDateTime.now())
                .lastActiveTime(LocalDateTime.now())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .roles(Collections.singleton(userRole))
                .build();

        // 保存用户
        userRepository.save(user);

        // 生成JWT令牌
        String accessToken = jwtUtil.generateToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);

        // 返回认证响应
        return buildAuthResponse(user, accessToken, refreshToken);
    }

    @Override
    public AuthResponse refreshToken(RefreshTokenRequest refreshTokenRequest) {
        // 验证刷新令牌
        String refreshToken = refreshTokenRequest.getRefreshToken();
        try {
            // 从刷新令牌中获取用户名
            String username = jwtUtil.getUsernameFromToken(refreshToken);

            // 加载用户
            User userDetails = userRepository.findByUsername(username)
                    .orElseThrow(() -> new BarrageException("用户不存在"));

            // 验证刷新令牌
            if (!jwtUtil.validateToken(refreshToken, userDetails)) {
                throw new BarrageException("刷新令牌无效");
            }

            // 生成新的访问令牌
            String newAccessToken = jwtUtil.generateToken(userDetails);

            // 更新用户最后活跃时间
            userDetails.setLastActiveTime(LocalDateTime.now());
            userRepository.save(userDetails);

            // 返回新的认证响应
            return buildAuthResponse(userDetails, newAccessToken, refreshToken);
        } catch (Exception e) {
            throw new BarrageException("刷新令牌无效或已过期");
        }
    }

    @Override
    public void logout(String token) {
        // 将令牌加入黑名单
        if (token != null && !token.isEmpty()) {
            String jwtToken = token;
            if (token.startsWith(Constants.Security.TOKEN_PREFIX)) {
                jwtToken = token.substring(Constants.Security.TOKEN_PREFIX.length());
            }

            try {
                // 获取用户名
                String username = jwtUtil.getUsernameFromToken(jwtToken);

                // 将令牌加入黑名单
                String blacklistKey = Constants.Security.BLACKLIST_TOKEN_PREFIX + jwtToken;
                redisTemplate.opsForValue().set(blacklistKey, "1", 24, TimeUnit.HOURS);

                // 移除在线用户记录
                userRepository.findByUsername(username).ifPresent(user -> {
                    String onlineUserKey = Constants.Redis.ONLINE_USER_PREFIX + user.getId();
                    redisTemplate.delete(onlineUserKey);
                });

                log.debug("用户 [{}] 已登出", username);
            } catch (Exception e) {
                log.error("注销过程中发生错误", e);
            }
        }
    }

    @Override
    public boolean verifyCaptcha(String captchaId, String captcha) {
        if (captchaId == null || captcha == null) {
            return false;
        }

        String captchaKey = Constants.Security.CAPTCHA_PREFIX + captchaId;
        String storedCaptcha = (String) redisTemplate.opsForValue().get(captchaKey);

        if (storedCaptcha != null && storedCaptcha.equalsIgnoreCase(captcha)) {
            // 验证成功后删除验证码
            redisTemplate.delete(captchaKey);
            return true;
        }
        return false;
    }

    @Override
    public Object generateCaptcha() {
        // 简化版本，实际应用中应使用图形验证码
        String captchaId = UUID.randomUUID().toString();
        String captchaCode = generateRandomCode(6);

        // 存储验证码到Redis，5分钟过期
        String captchaKey = Constants.Security.CAPTCHA_PREFIX + captchaId;
        redisTemplate.opsForValue().set(captchaKey, captchaCode, 5, TimeUnit.MINUTES);

        Map<String, String> captchaMap = new HashMap<>();
        captchaMap.put("captchaId", captchaId);
        captchaMap.put("captchaImage", "模拟的验证码图片: " + captchaCode);

        return captchaMap;
    }

    /**
     * 构建认证响应
     */
    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        // 获取用户角色和权限
        List<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toList());

        List<String> permissions = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(permission -> permission.getName())
                .distinct()
                .collect(Collectors.toList());

        // 构建认证响应
        return AuthResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType(Constants.Security.TOKEN_PREFIX.trim())
                .expiresIn(jwtUtil.getExpirationDateFromToken(accessToken).getTime() - System.currentTimeMillis())
                .roles(roles)
                .permissions(permissions)
                .loginTime(Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()))
                .build();
    }

    /**
     * 增加登录失败次数
     */
    private void incrementLoginFailCount(String username) {
        String loginErrorKey = Constants.Redis.USER_INFO_PREFIX + "login:error:" + username;
        Long count = redisTemplate.opsForValue().increment(loginErrorKey);
        if (count != null && count == 1) {
            redisTemplate.expire(loginErrorKey, 1, TimeUnit.HOURS);
        }

        if (count != null && count >= 5) {
            // 锁定账户
            userRepository.findByUsername(username).ifPresent(user -> {
                user.setAccountNonLocked(false);
                userRepository.save(user);

                // 记录锁定时间，15分钟后自动解锁
                String lockKey = Constants.Redis.USER_INFO_PREFIX + "lock:" + username;
                redisTemplate.opsForValue().set(lockKey, "1", 15, TimeUnit.MINUTES);

                log.warn("用户 [{}] 登录失败次数过多，账户已锁定15分钟", username);
            });
        }
    }

    /**
     * 生成随机验证码
     */
    private String generateRandomCode(int length) {
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}