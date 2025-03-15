package com.spud.barrage.proxy.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spud.barrage.proxy.service.LoadBalancer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestTemplate;

/**
 * 代理服务器配置
 * 
 * @author Spud
 * @date 2025/3/15
 */
@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class ProxyConfig {
    
    private final LoadBalancer loadBalancer;
    private final ProxyProperties properties;
    
    /**
     * 初始化时注册服务器
     */
    @PostConstruct
    public void init() {
        log.info("初始化代理服务器: id={}, 区域={}, 地址={}:{}",
            properties.getServerId(),
            properties.getRegion(),
            properties.getServerAddress(),
            properties.getServerPort());
        
        loadBalancer.registerServer();
    }
    
    /**
     * 定期更新服务器状态
     */
    @Scheduled(fixedRateString = "${proxy.health-check-interval:10}000")
    public void updateServerStatus() {
        loadBalancer.updateServerStatus(
            properties.getServerId(),
            0, // 连接数由WebSocketProxyHandler更新
            0  // 延迟由WebSocketProxyHandler更新
        );
    }
    
    /**
     * 配置RedisTemplate
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // 使用StringRedisSerializer来序列化和反序列化redis的key值
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // 使用Jackson2JsonRedisSerializer来序列化和反序列化redis的value值
        GenericJackson2JsonRedisSerializer jsonRedisSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);
        template.setValueSerializer(jsonRedisSerializer);
        template.setHashValueSerializer(jsonRedisSerializer);
        
        template.afterPropertiesSet();
        return template;
    }
    
    /**
     * 配置RestTemplate
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
    
    /**
     * 配置ObjectMapper
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
} 