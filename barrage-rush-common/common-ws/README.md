# Barrage Rush WebSocket 公共模块

这个模块提供了 WebSocket 相关的基础功能，包括连接处理、会话管理、消息发送等，主要用于弹幕系统的实时通信功能。

## 主要功能

1. **WebSocket 会话管理**：管理 WebSocket 连接，支持房间分组和用户标识
2. **令牌认证**：通过令牌验证 WebSocket 连接，确保连接安全
3. **消息处理**：处理不同类型的 WebSocket 消息，如弹幕、心跳等
4. **弹幕服务**：提供弹幕相关的服务接口和基础实现
5. **自动配置**：Spring Boot 自动配置，简化使用

## 使用方法

### 添加依赖

在你的项目`pom.xml`中添加依赖：

```xml
<dependency>
  <groupId>com.spud.barrage</groupId>
  <artifactId>common-ws</artifactId>
  <version>${project.version}</version>
</dependency>
```

### 配置属性

在`application.yml`配置文件中添加以下配置：

```yaml
barrage:
  websocket:
    enabled: true
    heartbeat-timeout: 60
    session-idle-timeout: 1800000
    max-text-message-buffer-size: 8192
    max-binary-message-buffer-size: 8192
    async-send-timeout: 10000
```

### 自定义 DanmakuService 实现

创建自己的 DanmakuService 实现类，继承 AbstractDanmakuServiceImpl：

```java

@Service
public class MyDanmakuService extends AbstractDanmakuServiceImpl {

  @Override
  protected void doProcessDanmaku(DanmakuMessage danmakuMessage) {
    // 自定义弹幕处理逻辑
  }

  @Override
  protected void doOnUserJoinRoom(Long userId, Long roomId) {
    // 自定义用户加入房间处理逻辑
  }

  @Override
  protected void doOnUserLeaveRoom(Long userId, Long roomId) {
    // 自定义用户离开房间处理逻辑
  }
}
```

### 前端连接示例

```javascript
// 获取连接信息
async function getConnectionInfo(roomId) {
  const response = await fetch(`/api/connect?roomId=${roomId}`);
  return await response.json();
}

// 建立WebSocket连接
async function connectWebSocket(roomId) {
  const connectionInfo = await getConnectionInfo(roomId);

  if (connectionInfo.code !== 0 && connectionInfo.code !== 1) {
    console.error("连接失败:", connectionInfo.message);
    return null;
  }

  const ws = new WebSocket(
      `${connectionInfo.serverUrl}?token=${connectionInfo.token}&roomId=${roomId}`
  );

  ws.onopen = function () {
    console.log("WebSocket连接已建立");
  };

  ws.onmessage = function (event) {
    const message = JSON.parse(event.data);
    handleWebSocketMessage(message);
  };

  ws.onclose = function () {
    console.log("WebSocket连接已关闭");
  };

  // 启动心跳
  startHeartbeat(ws);

  return ws;
}

// 心跳处理
function startHeartbeat(ws) {
  const heartbeatInterval = setInterval(() => {
    if (ws.readyState === WebSocket.OPEN) {
      ws.send(
          JSON.stringify({
            type: "HEARTBEAT",
            data: null,
            timestamp: Date.now(),
          })
      );
    } else {
      clearInterval(heartbeatInterval);
    }
  }, 30000);
}

// 发送弹幕
function sendDanmaku(ws, content) {
  if (ws.readyState !== WebSocket.OPEN) {
    return false;
  }

  ws.send(
      JSON.stringify({
        type: "DANMAKU",
        data: content,
        timestamp: Date.now(),
      })
  );

  return true;
}

// 处理接收到的消息
function handleWebSocketMessage(message) {
  switch (message.type) {
    case "DANMAKU":
      // 处理弹幕消息
      displayDanmaku(message.data);
      break;
    case "HEARTBEAT":
      // 处理心跳响应
      console.log("Heartbeat received:", message.data);
      break;
    case "SUCCESS":
      // 处理成功消息
      console.log("Success:", message.data);
      break;
    case "ERROR":
      // 处理错误消息
      console.error("Error:", message.data);
      break;
    default:
      console.log("Unknown message type:", message.type);
  }
}
```

## 核心类说明

### WebSocketSessionManager

管理 WebSocket 会话，支持房间分组和用户标识。提供方法进行会话添加、移除、查询和消息广播。

### TokenAuthHandshakeInterceptor

WebSocket 握手拦截器，负责验证连接时的令牌，确保连接安全和合法。

### AbstractWebSocketHandler

WebSocket 处理器基类，处理连接建立、关闭和错误等通用逻辑。

### DanmakuWebSocketHandler

弹幕 WebSocket 处理器，处理弹幕相关的消息，如弹幕发送、心跳等。

### DanmakuService

弹幕服务接口，定义弹幕处理的核心方法。

### AbstractDanmakuServiceImpl

弹幕服务抽象实现类，提供基础功能实现，子类可以扩展自定义逻辑。

### DanmakuController

弹幕 HTTP API 控制器，提供弹幕相关的 HTTP 接口，如发送弹幕、获取历史弹幕等。

## 自定义扩展

### 自定义消息类型

扩展 RequestType 枚举：

```java
public enum CustomRequestType {
    GIFT("GIFT"),
    FOLLOW("FOLLOW");

    private final String type;

    CustomRequestType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
```

### 自定义 WebSocket 处理器

创建自定义 WebSocket 处理器：

```java

@Component
public class CustomWebSocketHandler extends AbstractWebSocketHandler {
  // 实现自定义逻辑
}
```

注册自定义 WebSocket 处理器：

```java

@Configuration
public class CustomWebSocketConfigurer extends WebSocketConfig.AbstractWebSocketConfigurer {

  @Autowired
  private CustomWebSocketHandler customWebSocketHandler;

  @Override
  protected void configureWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry.addHandler(customWebSocketHandler, "/ws/custom/{param}")
        .addInterceptors(tokenAuthHandshakeInterceptor)
        .setAllowedOrigins("*");
  }
}
```

## 注意事项

1. 生产环境中应根据需要调整会话超时和心跳超时等参数
2. 生产环境应限制 WebSocket 连接的来源域名，防止跨站点 WebSocket 劫持
3. 使用集群部署时，应考虑会话同步和消息广播的问题，可以集成 Redis 进行会话共享
4. 对于高并发场景，应考虑消息的批量处理和缓冲策略
