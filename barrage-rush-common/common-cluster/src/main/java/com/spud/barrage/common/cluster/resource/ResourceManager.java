package com.spud.barrage.common.cluster.resource;

import com.spud.barrage.common.cluster.manager.InstanceManager;
import com.spud.barrage.common.cluster.manager.InstanceManager.ChangeEvent;
import com.spud.barrage.common.cluster.manager.InstanceManager.ChangeEventType;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

/**
 * 资源管理器
 * 管理和协调集群中的资源分配，处理资源的自动重新分配
 *
 * @author Spud
 * @date 2025/3/23
 */
@Slf4j
public class ResourceManager {

  // 锁TTL（毫秒）
  private static final long LOCK_TTL = 10000;
  // 资源键格式
  private static final String RESOURCE_KEY_FORMAT = "cluster:%s:resources";
  // 重平衡锁键格式
  private static final String REBALANCE_LOCK_KEY_FORMAT = "cluster:%s:rebalance:lock";
  // instanceId:timestamp
  private static final String RESOURCE_LOCK_VALUE_FORMAT = "%s:%d";

  // 实例管理器
  private final InstanceManager instanceManager;

  // Redis模板
  private final RedisTemplate<String, Object> redisTemplate;

  // 资源类型
  private final String resourceType;

  // Redis中资源列表的键
  private final String resourcesKey;

  // 本地缓存的资源集合
  private final Set<String> assignedResources = ConcurrentHashMap.newKeySet();

  // 资源分配回调
  private Consumer<ResourceEvent> resourceCallback;

  /**
   * 创建资源管理器
   *
   * @param instanceManager 实例管理器
   * @param redisTemplate   Redis模板
   * @param resourceType    资源类型
   */
  public ResourceManager(InstanceManager instanceManager,
      RedisTemplate<String, Object> redisTemplate,
      String resourceType) {
    this.instanceManager = instanceManager;
    this.redisTemplate = redisTemplate;
    this.resourceType = resourceType;
    this.resourcesKey = String.format(RESOURCE_KEY_FORMAT, resourceType);

    // 设置实例变更监听器
    instanceManager.setChangeCallback(this::handleInstanceChange);
  }

  /**
   * 启动资源管理器
   */
  public void start() {
    log.info("Starting ResourceManager for resource type: {}", resourceType);
    rebalanceResources();
  }

  /**
   * 设置资源分配回调
   */
  public void setResourceCallback(Consumer<ResourceEvent> callback) {
    this.resourceCallback = callback;
  }

  /**
   * 处理实例变更事件
   */
  private void handleInstanceChange(ChangeEvent event) {
    if (event.getType() == ChangeEventType.RING_REBUILT) {
      // 哈希环重建，需要重新平衡资源
      log.info("Hash ring rebuilt, rebalancing resources for type: {}", resourceType);
      rebalanceResources();
    }
  }

  /**
   * 重新平衡资源
   */
  public void rebalanceResources() {
    // 获取分布式锁防止并发重平衡
    String lockKey = String.format(REBALANCE_LOCK_KEY_FORMAT, resourceType);
    String lockValue = instanceManager.getInstanceId() + ":" + System.currentTimeMillis();

    boolean locked = acquireLock(lockKey, lockValue, LOCK_TTL);
    if (!locked) {
      log.info("Another instance is already rebalancing resources, skipping");
      return;
    }

    try {
      // 获取所有资源
      Set<Object> resources = redisTemplate.opsForSet().members(resourcesKey);
      if (resources == null || resources.isEmpty()) {
        log.info("No resources found for type: {}", resourceType);
        return;
      }

      Set<String> previouslyAssigned = new HashSet<>(assignedResources);
      assignedResources.clear();

      // 按照一致性哈希策略分配资源
      for (Object resourceObj : resources) {
        String resourceId = resourceObj.toString();
        if (instanceManager.isResponsibleFor(resourceId)) {
          assignedResources.add(resourceId);

          // 如果是新分配的资源，触发回调
          if (!previouslyAssigned.contains(resourceId)) {
            log.info("Resource assigned to this instance: {}", resourceId);
            if (resourceCallback != null) {
              resourceCallback.accept(
                  new ResourceEvent(ResourceEventType.RESOURCE_ASSIGNED, resourceId));
            }
          }
        } else if (previouslyAssigned.contains(resourceId)) {
          // 如果是取消分配的资源，触发回调
          log.info("Resource unassigned from this instance: {}", resourceId);
          if (resourceCallback != null) {
            resourceCallback.accept(
                new ResourceEvent(ResourceEventType.RESOURCE_UNASSIGNED, resourceId));
          }
        }
      }

      log.info("Rebalanced resources for type {}: {} assigned to this instance",
          resourceType, assignedResources.size());
    } finally {
      // 释放分布式锁
      releaseLock(lockKey, lockValue);
    }
  }

  /**
   * 添加资源
   *
   * @param resourceId 资源ID
   * @return 如果添加成功且由当前实例负责则返回true，否则返回false
   */
  public boolean addResource(String resourceId) {
    redisTemplate.opsForSet().add(resourcesKey, resourceId);

    boolean responsible = instanceManager.isResponsibleFor(resourceId);
    if (responsible) {
      assignedResources.add(resourceId);

      if (resourceCallback != null) {
        resourceCallback.accept(new ResourceEvent(ResourceEventType.RESOURCE_ASSIGNED, resourceId));
      }
    }

    return responsible;
  }

  /**
   * 添加资源
   *
   * @param resourceId 资源ID
   * @return 如果添加成功且由当前实例负责则返回true，否则返回false
   */
  public boolean addResource(Number resourceId) {
    return addResource(resourceId.toString());
  }

  /**
   * 移除资源
   *
   * @param resourceId 资源ID
   */
  public void removeResource(String resourceId) {
    redisTemplate.opsForSet().remove(resourcesKey, resourceId);

    boolean wasAssigned = assignedResources.remove(resourceId);
    if (wasAssigned && resourceCallback != null) {
      resourceCallback.accept(new ResourceEvent(ResourceEventType.RESOURCE_UNASSIGNED, resourceId));
    }
  }

  /**
   * 移除资源
   *
   * @param resourceId 资源ID
   */
  public void removeResource(Number resourceId) {
    removeResource(resourceId.toString());
  }

  /**
   * 检查资源是否由当前实例负责
   *
   * @param resourceId 资源ID
   * @return 如果由当前实例负责则返回true，否则返回false
   */
  public boolean isResponsibleForResource(String resourceId) {
    return assignedResources.contains(resourceId) ||
        (redisTemplate.opsForSet().isMember(resourcesKey, resourceId) &&
            instanceManager.isResponsibleFor(resourceId));
  }

  /**
   * 检查资源是否由当前实例负责
   *
   * @param resourceId 资源ID
   * @return 如果由当前实例负责则返回true，否则返回false
   */
  public boolean isResponsibleForResource(Number resourceId) {
    return isResponsibleForResource(resourceId.toString());
  }

  /**
   * 获取分配给当前实例的所有资源
   *
   * @return 资源ID集合
   */
  public Set<String> getAssignedResources() {
    return Collections.unmodifiableSet(assignedResources);
  }

  /**
   * 获取负责指定资源的实例ID
   *
   * @param resourceId 资源ID
   * @return 负责该资源的实例ID，如果没有则返回null
   */
  public String getResponsibleInstance(String resourceId) {
    return instanceManager.getResponsibleInstance(resourceId);
  }

  /**
   * 获取负责指定资源的实例ID
   *
   * @param resourceId 资源ID
   * @return 负责该资源的实例ID，如果没有则返回null
   */
  public String getResponsibleInstance(Number resourceId) {
    return getResponsibleInstance(resourceId.toString());
  }

  /**
   * 发布资源状态变更事件
   *
   * @param resourceId 资源ID
   * @param state      状态
   */
  public void publishResourceStateChange(String resourceId, String state) {
    if (resourceCallback != null && isResponsibleForResource(resourceId)) {
      resourceCallback.accept(
          new ResourceEvent(ResourceEventType.RESOURCE_STATE_CHANGED, resourceId, state));
    }
  }

  /**
   * 发布资源状态变更事件
   *
   * @param resourceId 资源ID
   * @param state      状态
   */
  public void publishResourceStateChange(Number resourceId, String state) {
    publishResourceStateChange(resourceId.toString(), state);
  }

  /**
   * 获取分布式锁
   */
  private boolean acquireLock(String lockKey, String lockValue, long ttl) {
    Boolean result = redisTemplate.opsForValue()
        .setIfAbsent(lockKey, lockValue, ttl, TimeUnit.MILLISECONDS);
    return Boolean.TRUE.equals(result);
  }

  /**
   * 释放分布式锁
   */
  private void releaseLock(String lockKey, String expectedValue) {
    String script = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
        "  return redis.call('del', KEYS[1]) " +
        "else " +
        "  return 0 " +
        "end";

    redisTemplate.execute(
        new DefaultRedisScript<>(script, Long.class),
        Collections.singletonList(lockKey),
        expectedValue);
  }

  /**
   * 资源事件类型
   */
  public enum ResourceEventType {
    // 资源分配
    RESOURCE_ASSIGNED,
    // 资源取消分配
    RESOURCE_UNASSIGNED,
    // 资源状态变更
    RESOURCE_STATE_CHANGED
  }

  /**
   * 资源事件
   */
  public static class ResourceEvent {

    // 事件类型
    private final ResourceEventType type;

    // 资源ID
    private final String resourceId;

    // 资源状态（可选）
    private final String state;

    public ResourceEvent(ResourceEventType type, String resourceId) {
      this(type, resourceId, null);
    }

    public ResourceEvent(ResourceEventType type, String resourceId, String state) {
      this.type = type;
      this.resourceId = resourceId;
      this.state = state;
    }

    public ResourceEventType getType() {
      return type;
    }

    public String getResourceId() {
      return resourceId;
    }

    public String getState() {
      return state;
    }
  }
}