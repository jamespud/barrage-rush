package com.spud.barrage.common.cluster.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

import com.spud.barrage.common.cluster.hash.ConsistentHash;
import com.spud.barrage.common.cluster.manager.InstanceManager;
import com.spud.barrage.common.cluster.resource.ResourceManager;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * 集群自动配置类
 * 提供集群管理相关的Bean
 * 
 * @author Spud
 * @date 2025/3/23
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "barrage.cluster", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ClusterAutoConfiguration {

    /**
     * 集群配置属性
     */
    @Data
    @ConfigurationProperties(prefix = "barrage.cluster")
    public static class ClusterProperties {
        // 是否启用集群功能
        private boolean enabled = true;

        // 实例类型
        private String instanceType = "default";

        // 心跳间隔（秒）
        private int heartbeatInterval = 30;

        // 资源类型
        private String resourceType = "room";

        // 一致性哈希虚拟节点数量
        private int virtualNodeCount = 160;
    }

    /**
     * 创建集群配置属性Bean
     */
    @Bean
    public ClusterProperties clusterProperties() {
        return new ClusterProperties();
    }

    /**
     * 创建一致性哈希Bean
     */
    @Bean
    @ConditionalOnMissingBean
    public ConsistentHash<String> consistentHash(ClusterProperties properties) {
        return new ConsistentHash<>(properties.getVirtualNodeCount());
    }

    /**
     * 创建实例管理器Bean
     */
    @Bean(initMethod = "start", destroyMethod = "shutdown")
    @ConditionalOnMissingBean
    public InstanceManager instanceManager(RedisTemplate<String, Object> redisTemplate, ClusterProperties properties) {
        InstanceManager manager = new InstanceManager(
                redisTemplate,
                properties.getInstanceType(),
                properties.getHeartbeatInterval());

        log.info("Created InstanceManager for type: {}", properties.getInstanceType());
        return manager;
    }

    /**
     * 创建资源管理器Bean
     */
    @Bean(initMethod = "start")
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "barrage.cluster", name = "resource-type")
    public ResourceManager resourceManager(
            InstanceManager instanceManager,
            RedisTemplate<String, Object> redisTemplate,
            ClusterProperties properties) {
        ResourceManager manager = new ResourceManager(
                instanceManager,
                redisTemplate,
                properties.getResourceType());

        log.info("Created ResourceManager for resource type: {}", properties.getResourceType());
        return manager;
    }
}