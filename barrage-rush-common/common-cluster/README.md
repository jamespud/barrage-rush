# Barrage Rush 集群管理模块

这个模块提供了基于一致性哈希算法的集群管理功能，包括实例注册与发现、资源分配与负载均衡等。

## 主要功能

1. **一致性哈希算法**：高效的哈希环实现，支持虚拟节点和权重配置
2. **实例管理**：基于 Redis 的实例注册、心跳检测和实例发现
3. **资源分配**：基于一致性哈希的资源分配和重平衡
4. **自动配置**：集成 Spring Boot 的自动配置

## 如何使用

### 1. 添加依赖

在你的模块的`pom.xml`中添加依赖:

```xml
<dependency>
  <groupId>com.spud.barrage</groupId>
  <artifactId>common-cluster</artifactId>
  <version>${project.version}</version>
</dependency>
```

### 2. 配置属性

在`application.yml`或`application.properties`中配置:

```yaml
barrage:
  cluster:
    enabled: true
    instance-type: damaku-server # 或 mq-consumer, ws-server 等
    heartbeat-interval: 30
    resource-type: room
    virtual-node-count: 160
```

### 3. 自动注入相关 Bean

```java

@Autowired
private InstanceManager instanceManager;

@Autowired
private ResourceManager resourceManager;
```

### 4. 使用示例

#### 弹幕服务中使用

```java

@Service
public class DanmakuService {

  private final ResourceManager resourceManager;

  public DanmakuService(ResourceManager resourceManager) {
    this.resourceManager = resourceManager;

    // 设置资源分配回调处理
    resourceManager.setResourceCallback(this::handleResourceEvent);
  }

  // 处理资源分配事件
  private void handleResourceEvent(ResourceManager.ResourceEvent event) {
    String roomId = event.getResourceId();

    switch (event.getType()) {
      case RESOURCE_ASSIGNED:
        // 房间分配给当前实例
        log.info("Room {} assigned to this instance", roomId);
        initRoom(roomId);
        break;

      case RESOURCE_UNASSIGNED:
        // 房间从当前实例移除
        log.info("Room {} unassigned from this instance", roomId);
        cleanupRoom(roomId);
        break;

      case RESOURCE_STATE_CHANGED:
        // 房间状态变更
        log.info("Room {} state changed to {}", roomId, event.getState());
        updateRoomState(roomId, event.getState());
        break;
    }
  }

  // 处理弹幕消息
  public void processDanmaku(DanmakuMessage message) {
    Long roomId = message.getRoomId();

    // 检查房间是否由当前实例负责
    if (resourceManager.isResponsibleForResource(roomId)) {
      // 处理弹幕...
    } else {
      // 转发到负责该房间的实例
      String responsibleInstance = resourceManager.getResponsibleInstance(roomId);
      // ...
    }
  }
}
```

#### 消息队列消费者中使用

```java

@Component
public class DynamicConsumerConfig {

  private final InstanceManager instanceManager;
  private final ResourceManager resourceManager;

  public DynamicConsumerConfig(InstanceManager instanceManager, ResourceManager resourceManager) {
    this.instanceManager = instanceManager;
    this.resourceManager = resourceManager;

    // 设置资源分配回调
    resourceManager.setResourceCallback(this::handleRoomAssignment);
  }

  private void handleRoomAssignment(ResourceManager.ResourceEvent event) {
    Long roomId = Long.parseLong(event.getResourceId());

    if (event.getType() == ResourceManager.ResourceEventType.RESOURCE_ASSIGNED) {
      // 为房间绑定消费者
      bindConsumerToRoom(roomId);
    } else if (event.getType() == ResourceManager.ResourceEventType.RESOURCE_UNASSIGNED) {
      // 解绑消费者
      unbindConsumerFromRoom(roomId);
    }
  }

  // 检查是否应该处理某个房间的消息
  public boolean shouldProcessForRoom(Long roomId) {
    return resourceManager.isResponsibleForResource(roomId);
  }
}
```

#### WebSocket 服务中使用

```java

@Component
public class WebSocketManager {

  private final InstanceManager instanceManager;

  public WebSocketManager(InstanceManager instanceManager) {
    this.instanceManager = instanceManager;
  }

  // 客户端连接时，检查是否应该重定向到其他实例
  public String checkRedirect(Long roomId) {
    String responsible = instanceManager.getResponsibleInstance(roomId.toString());

    if (!instanceManager.getInstanceId().equals(responsible)) {
      // 返回重定向信息
      return getServerUrl(responsible);
    }

    return null; // 不需要重定向
  }
}
```

## 相关类说明

### `ConsistentHash<T>`

一致性哈希算法的核心实现，支持泛型节点类型。

### `InstanceManager`

实例管理器，处理集群中的节点实例，提供实例注册、心跳检测和发现功能。

### `ResourceManager`

资源管理器，管理和协调集群中的资源分配，处理资源的自动重新分配。

### `ClusterAutoConfiguration`

Spring Boot 自动配置类，提供集群管理相关的 Bean。
