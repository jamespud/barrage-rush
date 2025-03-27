# 弹幕推送服务器 (Barrage Push Server)

弹幕推送服务器是弹幕系统的核心组件，负责接收和分发实时弹幕消息。本服务器基于 WebSocket 技术，提供高性能、低延迟的弹幕消息推送功能，支持大规模分布式部署和自动故障转移。

## 功能特点

- **实时弹幕推送**：基于 WebSocket 技术实现的低延迟弹幕消息推送
- **集群化部署**：支持多实例部署，通过一致性哈希算法进行负载均衡
- **资源自动分配**：根据服务器负载和网络状态，自动分配房间资源
- **自动故障转移**：实例失效时自动重新分配房间资源，确保服务可用性
- **消息持久化**：支持弹幕消息的持久化存储和历史记录查询
- **智能消息路由**：根据用户地理位置和服务器负载进行智能路由
- **健康检查和监控**：提供丰富的健康检查和监控指标

## 技术架构

### 核心组件

- **WebSocket 服务**：处理客户端连接和消息推送
- **消息队列**：使用 RabbitMQ 实现消息的异步处理和跨实例广播
- **缓存层**：使用 Redis 存储会话信息、消息历史和集群状态
- **集群管理**：基于 Redis 实现的分布式集群管理和资源分配
- **监控系统**：提供健康检查和性能指标的监控 API

### 架构图

```
+----------------+      +----------------+
|   客户端       |      |   客户端       |
+-------+--------+      +-------+--------+
        |                       |
        | WebSocket             | WebSocket
        |                       |
+-------v-----------------------v--------+
|             API网关/负载均衡           |
+------------------+--------------------+
                   |
     +-------------+-------------+
     |                           |
+----v--------+        +---------v----+
| 推送服务器#1 |        | 推送服务器#2 |
|             |        |              |
+------+------+        +------+-------+
       |                      |
       +----------+----------+
                  |
       +----------v----------+
       |      消息队列       |
       |    (RabbitMQ)      |
       +----------+----------+
                  |
       +----------v----------+
       |       Redis         |
       | (集群管理/缓存)     |
       +---------------------+
```

## 配置说明

### 核心配置参数

```yaml
# 服务器配置
server:
  port: 8083 # 服务器端口

# 弹幕系统集群配置
barrage:
  cluster:
    enabled: true # 是否启用集群功能
    instance-type: push-server # 实例类型
    resource-type: room # 资源类型
    heartbeat-ttl: 30 # 心跳TTL（秒）
    virtual-nodes: 128 # 虚拟节点数

# 推送服务器配置
push:
  server:
    host: localhost # 服务器主机名
    port: 8083 # 服务器端口
    region: default # 服务器地区
    url-pattern: http://{host}:{port}/ws/room/{roomId} # URL模式

  websocket:
    allowed-origins: "*" # 允许的源
    path: /ws/room/{roomId} # WebSocket路径
    cluster-check-enabled: true # 启用集群检查

  danmaku:
    queue-ttl: 3600 # 弹幕队列TTL（秒）
    max-queue-size: 200 # 最大队列大小
    broadcast-to-mq: true # 是否向MQ广播弹幕
```

## API 接口

### WebSocket 接口

- **连接端点**: `/ws/room/{roomId}?token={token}`
  - `roomId`: 房间 ID
  - `token`: 连接令牌（由连接代理服务生成）

### HTTP 接口

- **GET /api/push/status**: 获取推送服务器状态信息
- **POST /api/push/broadcast/{roomId}**: 广播系统消息到指定房间
- **GET /api/push/check/{roomId}**: 检查房间是否由当前服务器管理
- **GET /api/push/room/{roomId}**: 查询房间连接信息
- **DELETE /api/push/room/{roomId}**: 解除房间与服务器的链接

## 部署指南

### 环境要求

- Java 21+
- Redis 6.0+
- RabbitMQ 3.8+

### 打包部署

```bash
# 构建
mvn clean package

# 运行
java -jar target/barrage-rush-push-server-1.0-SNAPSHOT.jar
```

### 容器部署

```bash
# 构建Docker镜像
docker build -t barrage-push-server .

# 运行容器
docker run -d --name push-server -p 8083:8083 \
  -e REDIS_HOST=redis \
  -e RABBITMQ_HOST=rabbitmq \
  -e SERVER_HOST=push-server.example.com \
  barrage-push-server
```

## 性能和扩展性

- 单实例支持 10,000+并发 WebSocket 连接
- 支持水平扩展，理论上可无限扩展容量
- 资源自动重平衡，确保负载均衡
- 根据地理位置进行智能路由，降低延迟

## 高可用性

- 支持多区域部署
- 自动故障转移
- 健康检查和自动恢复
- 灵活的会话恢复机制

## 与其他模块的集成

- **连接代理服务**: 处理初始连接请求，生成令牌并指引客户端连接到合适的推送服务器
- **消息服务**: 接收和处理用户发送的弹幕消息
- **存储服务**: 持久化存储弹幕消息和用户数据

## 问题排查

### 常见问题

1. **WebSocket 连接失败**: 检查令牌有效性和房间资源分配情况
2. **消息发送失败**: 检查 RabbitMQ 连接和队列状态
3. **集群同步问题**: 检查 Redis 连接和集群配置

### 日志分析

日志位于`logs/barrage-rush-push-server.log`，包含详细的运行信息和错误信息。

## 开发指南

### 扩展点

- 自定义握手拦截器
- 自定义消息处理器
- 自定义资源分配策略

### 添加新功能

开发者可以通过继承或实现以下接口来扩展功能：

- `DanmakuService`: 自定义弹幕处理逻辑
- `HandshakeInterceptor`: 自定义握手拦截逻辑
- `WebSocketHandler`: 自定义 WebSocket 消息处理

## 许可证

MIT License
