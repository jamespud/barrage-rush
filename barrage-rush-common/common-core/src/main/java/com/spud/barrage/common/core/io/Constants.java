package com.spud.barrage.common.core.io;

/**
 * 系统常量
 *
 * @author Spud
 * @date 2025/3/27
 */
public class Constants {

  /**
   * 认证相关常量
   */
  public static class Auth {

    /**
     * 认证请求头名称
     */
    public static final String HEADER_AUTH = "Authorization";

    /**
     * Token前缀
     */
    public static final String TOKEN_PREFIX = "Bearer ";

    /**
     * 访问Token名称
     */
    public static final String ACCESS_TOKEN_NAME = "accessToken";

    /**
     * 刷新Token名称
     */
    public static final String REFRESH_TOKEN_NAME = "refreshToken";

    /**
     * JWT主题
     */
    public static final String JWT_SUBJECT = "barrage-rush-auth";

    /**
     * JWT访问Token过期时间（毫秒）：1小时
     */
    public static final long JWT_ACCESS_TOKEN_EXPIRATION = 60 * 60 * 1000;

    /**
     * JWT刷新Token过期时间（毫秒）：7天
     */
    public static final long JWT_REFRESH_TOKEN_EXPIRATION = 7 * 24 * 60 * 60 * 1000;

    /**
     * 记住我时JWT访问Token过期时间（毫秒）：7天
     */
    public static final long JWT_REMEMBER_ME_EXPIRATION = 7 * 24 * 60 * 60 * 1000;

    /**
     * 用户角色前缀
     */
    public static final String ROLE_PREFIX = "ROLE_";

    /**
     * 管理员角色
     */
    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    /**
     * 用户角色
     */
    public static final String ROLE_USER = "ROLE_USER";

    /**
     * 匿名用户角色
     */
    public static final String ROLE_ANONYMOUS = "ROLE_ANONYMOUS";
  }

  /**
   * Redis相关常量
   */
  public static class Redis {

    /**
     * 验证码前缀
     */
    public static final String KEY_CAPTCHA_PREFIX = "barrage:captcha:";

    /**
     * 验证码过期时间（秒）：5分钟
     */
    public static final long CAPTCHA_EXPIRATION = 5 * 60;

    /**
     * 用户Token前缀
     */
    public static final String KEY_TOKEN_PREFIX = "barrage:token:";

    /**
     * 用户黑名单Token前缀
     */
    public static final String KEY_TOKEN_BLACKLIST_PREFIX = "barrage:token:blacklist:";

    /**
     * 在线用户前缀
     */
    public static final String KEY_ONLINE_USER_PREFIX = "barrage:online:user:";

    /**
     * 用户登录限制前缀
     */
    public static final String KEY_LOGIN_LIMIT_PREFIX = "barrage:login:limit:";

    /**
     * 用户登录错误次数前缀
     */
    public static final String KEY_LOGIN_ERROR_COUNT_PREFIX = "barrage:login:error:count:";

    /**
     * 用户登录错误锁定前缀
     */
    public static final String KEY_LOGIN_ERROR_LOCK_PREFIX = "barrage:login:error:lock:";

    /**
     * 用户登录错误锁定时间（秒）：15分钟
     */
    public static final long LOGIN_ERROR_LOCK_EXPIRATION = 15 * 60;

    /**
     * 用户登录错误最大次数：5次
     */
    public static final int LOGIN_ERROR_MAX_COUNT = 5;

    /**
     * 用户信息缓存前缀
     */
    public static final String USER_INFO_PREFIX = "auth:user:info:";

    /**
     * 用户Token缓存前缀
     */
    public static final String USER_TOKEN_PREFIX = "auth:user:token:";

    /**
     * 在线用户前缀
     */
    public static final String ONLINE_USER_PREFIX = "auth:online:user:";
  }

  /**
   * 安全配置相关常量
   */
  public static class Security {

    /**
     * 白名单路径，不需要认证即可访问
     */
    public static final String[] WHITE_LIST = {
        "/auth/login",
        "/auth/register",
        "/auth/refresh",
        "/auth/captcha",
        "/swagger-ui/**",
        "/v3/api-docs/**",
        "/swagger-ui.html",
        "/actuator/**",
        "/error"
    };

    /**
     * Token前缀
     */
    public static final String TOKEN_PREFIX = "Bearer ";

    /**
     * 授权头
     */
    public static final String AUTHORIZATION_HEADER = "Authorization";

    /**
     * 默认角色前缀
     */
    public static final String ROLE_PREFIX = "ROLE_";

    /**
     * 管理员角色
     */
    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    /**
     * 用户角色
     */
    public static final String ROLE_USER = "ROLE_USER";

    /**
     * 验证码Redis键前缀
     */
    public static final String CAPTCHA_PREFIX = "auth:captcha:";

    /**
     * 黑名单Token Redis键前缀
     */
    public static final String BLACKLIST_TOKEN_PREFIX = "auth:token:blacklist:";
  }

  /**
   * 系统用户相关常量
   */
  public static class User {

    /**
     * 用户状态：启用
     */
    public static final Integer STATUS_ENABLED = 1;

    /**
     * 用户状态：禁用
     */
    public static final Integer STATUS_DISABLED = 0;

    /**
     * 默认头像
     */
    public static final String DEFAULT_AVATAR = "https://s2.loli.net/2022/04/07/gw1L2Z5sPtS8RIj.gif";
  }
}