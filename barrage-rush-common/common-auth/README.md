# Common Auth Module

弹幕系统共享认证模块，提供 JWT 认证相关的工具类和 DTO。

## 功能

- JWT 令牌的生成、解析和验证
- 认证相关的常量
- 用户认证相关的 DTO
- JWT 配置类

## 使用方法

### 1. 添加依赖

在需要使用认证功能的模块中，添加以下依赖：

```xml
<dependency>
    <groupId>com.spud.barrage</groupId>
    <artifactId>common-auth</artifactId>
    <version>${project.version}</version>
</dependency>
```

### 2. 使用 JWT 工具类

```java
@Autowired
private JwtTokenUtil jwtTokenUtil;

// 生成访问令牌
Map<String, Object> claims = new HashMap<>();
claims.put("userId", 123L);
String accessToken = jwtTokenUtil.generateAccessToken(userDetails, claims);

// 生成刷新令牌
String refreshToken = jwtTokenUtil.generateRefreshToken(userDetails, claims);

// 从令牌中获取用户名
String username = jwtTokenUtil.getUsernameFromToken(token);

// 验证令牌
boolean isValid = jwtTokenUtil.validateToken(token, userDetails);
```

### 3. 使用认证常量

```java
// 获取认证请求头
request.getHeader(AuthConstants.AUTHORIZATION_HEADER);

// 使用Token前缀
String fullToken = AuthConstants.TOKEN_PREFIX + token;

// 使用白名单路径
http.authorizeRequests()
    .requestMatchers(AuthConstants.WHITE_LIST).permitAll();
```

### 4. 自定义配置

在`application.yml`或`application.properties`中配置：

```yaml
jwt:
  secret: your-custom-secret-key
  access-token-expiration: 1800000 # 30分钟
  refresh-token-expiration: 604800000 # 7天
  remember-me-expiration: 2592000000 # 30天
```

## DTO 类

- `AuthResponse` - 认证响应 DTO
- `LoginRequest` - 登录请求 DTO
- `RefreshTokenRequest` - 刷新令牌请求 DTO
- `RegisterRequest` - 注册请求 DTO
- `ChangePasswordRequest` - 修改密码请求 DTO
- `BarrageUserDetail` - 用户详情 DTO
