# Barrage Rush Connection Proxy

弹幕系统连接代理服务，用于将客户端 WebSocket 连接路由到最合适的 Push 服务器。

## 主要功能

1. **智能路由**: 根据客户端 IP、地理位置、房间 ID 等信息，将客户端引导至最合适的 Push 服务器
2. **减少延迟**: 通过重定向方式，避免代理中转消息，最小化客户端与服务器间的通信延迟
3. **地理位置感知**: 利用 GeoIP 数据库识别客户端所在区域，优先路由到同区域的服务器
4. **一致性哈希分配**: 与集群管理模块集成，使用一致性哈希算法确保同一房间的连接路由到相同的服务器
5. **健康检查**: 定期检查 Push 服务器的健康状态，避免路由到不可用的服务器
6. **临时令牌生成**: 为客户端生成连接认证令牌，提高连接安全性

## 技术架构

### 架构图

```
                  +------------------------+
                  |      API Gateway      |
                  +------------+-----------+
                               |
                               v
  +----------------+     +---------------+
  |  客户端应用     +---->+ Connection Proxy|
  +------+---------+     +-------+-------+
         ^                       |
         |                       v
         |                +------+------+
         +----------------+ Push Server |
                         +-------------+
```

### 主要组件

- **GeoIpService**: 根据 IP 地址解析地理位置
- **TokenService**: 生成临时连接令牌
- **RoutingService**: 实现 WebSocket 连接的智能路由逻辑
- **HealthCheckService**: 定期检查 Push 服务器健康状态
- **ConnectionProxyController**: 提供 HTTP API 接口

## 使用方法

### API 接口

#### 1. 获取连接信息

```
GET /api/connect?roomId={roomId}&region={region}
```

参数说明:

- `roomId`: 房间 ID，必填
- `region`: 客户端区域，选填，不提供时自动根据 IP 识别

返回示例:

```json
{
  "serverUrl": "ws://push-server-1.example.com/ws/room/12345",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expireAt": 1648138569000,
  "region": "CN",
  "serverId": "push-CN-1",
  "code": 0,
  "message": "success"
}
```

状态码说明:

- `0`: 成功，客户端可直接连接返回的 URL
- `1`: 重定向，客户端应连接到返回的 URL（可能是跨区域服务器）
- `2`: 错误，查看 message 字段了解详情

#### 2. 获取服务器状态

```
GET /api/server-status
```

返回示例:

```json
{
  "servers": {
    "push-CN-1": {
      "id": "push-CN-1",
      "host": "push-1.cn.example.com",
      "healthy": true,
      "region": "CN",
      "load": 0.3,
      "connections": 1250
    },
    "push-US-1": {
      "id": "push-US-1",
      "host": "push-1.us.example.com",
      "healthy": true,
      "region": "US",
      "load": 0.5,
      "connections": 2100
    }
  },
  "totalServers": 2,
  "healthyServers": 2
}
```

### 客户端集成流程

1. 客户端向 Connection Proxy 发送请求获取连接信息
2. 根据响应中的`serverUrl`和`token`建立 WebSocket 连接
3. 如果连接失败，重新请求 Connection Proxy 获取新的连接信息

## 配置说明

主要配置项（application.yml）:

```yaml
# GeoIP2数据库配置
geoip:
  database:
    path: ${user.home}/geoip/GeoLite2-City.mmdb
  cache:
    size: 10000
    expiration: 12
  default-region: CN

# 令牌配置
token:
  secret: ${TOKEN_SECRET:barrage-rush-secret-key}
  expiration: 300 # 5分钟

# 推送服务器配置
push-server:
  url-pattern: ws://%s/ws/room/%d
  route-by-region: true
  instance-prefix: push-
  use-consistent-hash: true

# 健康检查配置
health-check:
  enabled: true
  interval: 60000 # 60秒
  timeout: 5000 # 5秒
  url-pattern: http://%s/actuator/health
```

## 部署说明

### 前置条件

- Java 17 或更高版本
- Redis 服务器
- GeoLite2 数据库文件（可选，用于地理位置识别）

### 运行

```bash
java -jar barrage-rush-connection-proxy.jar
```

### 环境变量

- `TOKEN_SECRET`: 令牌加密密钥
- `REDIS_HOST`: Redis 服务器地址
- `REDIS_PASSWORD`: Redis 密码

## 注意事项

1. 为提高地理位置识别准确性，建议定期更新 GeoIP 数据库
2. 生产环境建议配置 HTTPS
3. 令牌密钥应妥善保管，建议通过环境变量注入而非硬编码在配置文件中
4. 对于高并发场景，建议部署多个 Connection Proxy 实例并通过负载均衡分发请求
