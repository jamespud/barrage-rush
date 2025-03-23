package com.spud.barrage.common.mq.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * MQ动态配置属性
 *
 * @author Spud
 * @date 2025/3/24
 */
@Data
@Component
@ConfigurationProperties(prefix = "barrage.mq")
public class DynamicMQProperties {

    /**
     * 房间事件变更间隔（毫秒）
     * 防止频繁变更造成资源浪费
     */
    private long roomEventChangeInterval = 60000; // 默认1分钟
    
    /**
     * 观众数量缓存TTL（秒）
     */
    private long viewerCacheTtl = 300; // 默认5分钟
    
    /**
     * 房间类型相关阈值配置
     */
    private int superHotViewersThreshold = 100000; // 超热门阈值
    private int hotViewersThreshold = 30000; // 热门阈值
    private int coldViewersThreshold = 1000; // 冷门阈值
    
    /**
     * 队列配置
     */
    private QueueProperties queue = new QueueProperties();
    
    /**
     * 队列相关配置
     */
    @Data
    public static class QueueProperties {
        /**
         * 队列TTL（秒）
         */
        private long ttl = 86400; // 默认1天
        
        /**
         * 队列预热数量
         */
        private int warmupCount = 10;
        
        /**
         * 自动扩容阈值
         */
        private int autoScaleThreshold = 80; // 利用率达到80%时自动扩容
    }
    
    /**
     * 交换机配置
     */
    private ExchangeProperties exchange = new ExchangeProperties();
    
    /**
     * 交换机相关配置
     */
    @Data
    public static class ExchangeProperties {
        /**
         * 交换机TTL（秒）
         */
        private long ttl = 86400; // 默认1天
        
        /**
         * 交换机预热数量
         */
        private int warmupCount = 5;
    }
    
    /**
     * 资源池配置
     */
    private PoolProperties pool = new PoolProperties();
    
    /**
     * 资源池相关配置
     */
    @Data
    public static class PoolProperties {
        /**
         * 资源池清理间隔（分钟）
         */
        private long cleanupInterval = 60; // 默认1小时清理一次
        
        /**
         * 资源池最大空闲数量
         */
        private int maxIdleSize = 100; // 每种类型最多保留100个空闲资源
    }
} 