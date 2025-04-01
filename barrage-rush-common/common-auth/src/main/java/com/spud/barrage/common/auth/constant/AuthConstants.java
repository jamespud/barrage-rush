package com.spud.barrage.common.auth.constant;

/**
 * 认证常量类
 *
 * @author Spud
 * @date 2023/4/1
 */
public class AuthConstants {

    /**
     * 认证请求头
     */
    public static final String AUTHORIZATION_HEADER = "Authorization";

    /**
     * Token前缀
     */
    public static final String TOKEN_PREFIX = "Bearer ";

    /**
     * 黑名单Token前缀
     */
    public static final String BLACKLIST_TOKEN_PREFIX = "blacklist:token:";

    /**
     * 角色前缀
     */
    public static final String ROLE_PREFIX = "ROLE_";

    /**
     * 安全上下文中的用户ID键
     */
    public static final String SECURITY_USER_ID = "userId";

    /**
     * 安全上下文中的用户名键
     */
    public static final String SECURITY_USERNAME = "username";

    /**
     * 安全上下文中的权限键
     */
    public static final String SECURITY_PERMISSIONS = "permissions";

    /**
     * 安全上下文中的角色键
     */
    public static final String SECURITY_ROLES = "roles";

    /**
     * 白名单路径
     */
    public static final String[] WHITE_LIST = {
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/refresh-token",
            "/api/v1/auth/captcha",
            "/api/v1/health",
            "/swagger-ui/**",
            "/swagger-resources/**",
            "/v3/api-docs/**",
            "/error"
    };

    private AuthConstants() {
        // 私有构造方法，防止实例化
    }
}