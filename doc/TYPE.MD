# 弹幕系统接入层设计文档

## 一、数据模型定义

### 1. 弹幕消息模型
```protobuf
syntax = "proto3";

message DanmakuMessage {
    int64 id = 1;                    // 弹幕唯一ID
    int64 room_id = 2;              // 房间ID
    int64 user_id = 3;              // 发送者ID
    string content = 4;              // 弹幕内容
    int64 timestamp = 5;            // 发送时间戳
    DanmakuType type = 6;           // 弹幕类型
    DanmakuStyle style = 7;         // 弹幕样式
    string extra = 8;               // 扩展字段(JSON)
}

enum DanmakuType {
    NORMAL = 0;     // 普通弹幕
    TOP = 1;        // 顶部固定
    BOTTOM = 2;     // 底部固定
    REVERSE = 3;    // 逆向弹幕
    SPECIAL = 4;    // 特殊弹幕
}

message DanmakuStyle {
    string color = 1;       // 颜色(16进制)
    int32 font_size = 2;    // 字体大小
    int32 position = 3;     // 显示位置
    bool bold = 4;         // 是否加粗
    float speed = 5;       // 移动速度
}
```

### 2. Redis 数据结构设计

```plaintext
# 1. 房间在线用户集合 (Set)
key: room:${roomId}:users
value: Set<userId>
TTL: 24小时

# 2. 用户连接信息 (Hash)
key: user:${userId}:connection
value: Hash
    - sessionId: 会话ID
    - roomId: 当前房间ID
    - connectTime: 连接时间
    - clientInfo: 客户端信息
TTL: 24小时

# 3. 房间最近弹幕列表 (Sorted Set)
key: room:${roomId}:messages
value: Sorted Set
    - score: 时间戳
    - member: 弹幕消息ID
capacity: 1000条
TTL: 5分钟

# 4. 用户发送频率限制 (String)
key: limit:user:${userId}:count
value: 发送次数
TTL: 60秒

# 5. 房间弹幕配置 (Hash)
key: room:${roomId}:config
value: Hash
    - status: 房间状态
    - maxLength: 弹幕最大长度
    - interval: 发送间隔
    - filters: 过滤规则
TTL: 永久
```

### 3. Kafka 主题设计

```plaintext
# 1. 弹幕消息主题
Topic: danmaku-message-{分片ID}
Partitions: 32
Replication: 3
Retention: 7天

# 2. 用户行为日志主题
Topic: danmaku-user-behavior
Partitions: 16
Replication: 3
Retention: 30天

# 3. 系统事件主题
Topic: danmaku-system-event
Partitions: 8
Replication: 3
Retention: 7天
```

## 二、接口定义

### 1. WebSocket 接口

```plaintext
# 1. 连接地址
ws://{domain}/danmaku/ws/{roomId}?token={token}

# 2. 消息格式
{
    "type": "MESSAGE",        // 消息类型
    "data": {                 // 消息数据
        "id": "xxx",
        "content": "xxx",
        ...
    },
    "timestamp": 1234567890
}

# 3. 消息类型
- CONNECT: 连接消息
- MESSAGE: 弹幕消息
- HEARTBEAT: 心跳消息
- COMMAND: 控制消息
- ERROR: 错误消息
```

### 2. HTTP 降级接口

```plaintext
# 1. 发送弹幕
POST /api/v1/danmaku/send
Request:
{
    "roomId": 12345,
    "content": "弹幕内容",
    "type": 0,
    "style": {...}
}

# 2. 获取最近弹幕
GET /api/v1/danmaku/recent?roomId=12345&limit=100

# 3. 房间状态查询
GET /api/v1/danmaku/room/status?roomId=12345
```

## 三、限流策略

### 1. 用户级限流
- 单用户每分钟发送上限: 60条
- 单条弹幕内容长度: 100字
- 单用户并发连接数: 3个

### 2. 房间级限流
- 房间每秒弹幕上限: 1000条
- 房间在线用户上限: 100万
- 房间消息队列缓冲: 10000条

### 3. 全局限流
- 系统总连接数: 1000万
- 系统消息吞吐: 10万QPS
- 系统带宽限制: 10Gbps

## 四、连接管理

### 1. 会话状态机
```plaintext
初始化 -> 认证中 -> 已连接 -> 心跳中 -> 断开连接
                  |-> 认证失败
                  |-> 心跳超时
                  |-> 主动断开
```

### 2. 心跳机制
- 心跳间隔: 30秒
- 超时时间: 90秒
- 重连策略: 指数退避

### 3. 断线重连
- 重连次数: 最多3次
- 重连间隔: 1s, 2s, 4s
- 会话保持: 60秒