package com.spud.barrage.common.mq.config;

import com.spud.barrage.constant.RoomType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * MQ资源管理器
 * 负责管理交换机和队列的ID分配和释放
 *
 * @author Spud
 * @date 2025/3/24
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResourceManager {

    private final RedisTemplate<String, String> redisTemplate;

    // Lua脚本：从idle集合获取一个ID并移动到used集合
    private static final String GET_ID_SCRIPT =
            "local id = redis.call('SPOP', KEYS[1]) " +
            "if id then " +
            "  redis.call('SADD', KEYS[2], id) " +
            "  return id " +
            "else " +
            "  return nil " +
            "end";

    // Lua脚本：生成新ID，确保不在used集合中
    private static final String GENERATE_ID_SCRIPT =
            "local counter = tonumber(redis.call('GET', KEYS[1])) or 0 " +
            "counter = counter + 1 " +
            "while redis.call('SISMEMBER', KEYS[2], tostring(counter)) == 1 do " +
            "  counter = counter + 1 " +
            "end " +
            "redis.call('SET', KEYS[1], tostring(counter)) " +
            "redis.call('SADD', KEYS[2], tostring(counter)) " +
            "return tostring(counter)";

    // Lua脚本：从used集合移动ID到idle集合
    private static final String RELEASE_ID_SCRIPT =
            "if redis.call('SISMEMBER', KEYS[1], ARGV[1]) == 1 then " +
            "  redis.call('SREM', KEYS[1], ARGV[1]) " +
            "  redis.call('SADD', KEYS[2], ARGV[1]) " +
            "  return 1 " +
            "else " +
            "  return 0 " +
            "end";

    /**
     * 获取交换机ID
     * @param type 房间类型
     * @return 交换机ID
     */
    public String getExchangeId(RoomType type) {
        String typeKey = type.getExchangeType();
        String idleKey = String.format(RabbitMQConfig.EXCHANGE_IDLE, typeKey);
        String usedKey = String.format(RabbitMQConfig.EXCHANGE_USED, typeKey);
        String counterKey = String.format("%s:counter", idleKey);

        // 尝试从idle集合获取ID
        String id = executeScript(GET_ID_SCRIPT, idleKey, usedKey);
        
        // 如果没有空闲ID，生成新ID
        if (id == null) {
            id = executeScript(GENERATE_ID_SCRIPT, counterKey, usedKey);
            log.info("Generated new exchange ID: {} for type {}", id, type);
        } else {
            log.info("Reused idle exchange ID: {} for type {}", id, type);
        }
        
        return id;
    }

    /**
     * 获取队列ID
     * @param exchangeId 交换机ID
     * @param type 房间类型
     * @return 队列ID
     */
    public String getQueueId(String exchangeId, RoomType type) {
        String typeKey = type.getQueueType();
        String idleKey = String.format(RabbitMQConfig.QUEUE_IDLE, typeKey);
        String usedKey = String.format(RabbitMQConfig.QUEUE_USED, typeKey);
        String counterKey = String.format("%s:counter", idleKey);

        // 尝试从idle集合获取ID
        String id = executeScript(GET_ID_SCRIPT, idleKey, usedKey);
        
        // 如果没有空闲ID，生成新ID
        if (id == null) {
            // TODO: exchange与queue的关系
            id = executeScript(GENERATE_ID_SCRIPT, counterKey, usedKey);
            log.info("Generated new queue ID: {} for exchange {} and type {}", id, exchangeId, type);
        } else {
            log.info("Reused idle queue ID: {} for exchange {} and type {}", id, exchangeId, type);
        }
        
        return id;
    }

    /**
     * 释放交换机ID
     * @param type 房间类型
     * @param exchangeId 交换机ID
     * @return 是否成功释放
     */
    public boolean releaseExchangeId(RoomType type, String exchangeId) {
        try {
            String typeKey = type.getExchangeType();
            String usedKey = String.format(RabbitMQConfig.EXCHANGE_USED, typeKey);
            String idleKey = String.format(RabbitMQConfig.EXCHANGE_IDLE, typeKey);
            
            boolean result = "1".equals(executeScript(RELEASE_ID_SCRIPT, usedKey, idleKey, exchangeId));
            
            if (result) {
                log.info("Released exchange ID: {} for type {}", exchangeId, type);
            } else {
                log.warn("Failed to release exchange ID: {}, not found in used set for type {}", exchangeId, type);
            }
            
            return result;
        } catch (Exception e) {
            log.error("Error releasing exchange ID: {} for type {}: {}", exchangeId, type, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 释放队列ID
     * @param type 房间类型
     * @param queueId 队列ID
     * @return 是否成功释放
     */
    public boolean releaseQueueId(RoomType type, String queueId) {
        try {
            String typeKey = type.getQueueType();
            String usedKey = String.format(RabbitMQConfig.QUEUE_USED, typeKey);
            String idleKey = String.format(RabbitMQConfig.QUEUE_IDLE, typeKey);
            
            boolean result = "1".equals(executeScript(RELEASE_ID_SCRIPT, usedKey, idleKey, queueId));
            
            if (result) {
                log.info("Released queue ID: {} for type {}", queueId, type);
            } else {
                log.warn("Failed to release queue ID: {}, not found in used set for type {}", queueId, type);
            }
            
            return result;
        } catch (Exception e) {
            log.error("Error releasing queue ID: {} for type {}: {}", queueId, type, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 执行Lua脚本 (两个键)
     */
    private String executeScript(String script, String key1, String key2) {
        try {
            Object result = redisTemplate.execute(
                (RedisCallback<Object>) connection -> connection.eval(
                    script.getBytes(StandardCharsets.UTF_8),
                    ReturnType.VALUE,
                    2,
                    key1.getBytes(StandardCharsets.UTF_8),
                    key2.getBytes(StandardCharsets.UTF_8)
                )
            );
            
            if (result == null) {
                return null;
            }
            
            return result instanceof byte[] ? new String((byte[]) result, StandardCharsets.UTF_8) : result.toString();
        } catch (Exception e) {
            log.error("Error executing Redis script: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 执行Lua脚本 (两个键，一个参数)
     */
    private String executeScript(String script, String key1, String key2, String arg1) {
        try {
            Object result = redisTemplate.execute(
                (RedisCallback<Object>) connection -> connection.eval(
                    script.getBytes(StandardCharsets.UTF_8),
                    ReturnType.VALUE,
                    2,
                    key1.getBytes(StandardCharsets.UTF_8),
                    key2.getBytes(StandardCharsets.UTF_8),
                    arg1.getBytes(StandardCharsets.UTF_8)
                )
            );
            
            if (result == null) {
                return null;
            }
            
            return result instanceof byte[] ? new String((byte[]) result, StandardCharsets.UTF_8) : result.toString();
        } catch (Exception e) {
            log.error("Error executing Redis script: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 构建交换机名称
     */
    public String buildExchangeName(RoomType type, Long roomId) {
        String exchangeId = getExchangeId(type);

        String exchangeType = type.getExchangeType();
        return String.format(RabbitMQConfig.EXCHANGE_USED, exchangeType);
    }

    /**
     * 构建队列名称
     */
    public String buildQueueName(RoomType type, Long roomId, String exchangeName) {
        String queueId = getQueueId(exchangeName, type);
        String queueType = type.getQueueType();
        return String.format(RabbitMQConfig.QUEUE_USED, queueType);
    }
} 