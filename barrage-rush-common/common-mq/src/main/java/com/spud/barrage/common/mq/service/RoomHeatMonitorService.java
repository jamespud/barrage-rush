package com.spud.barrage.common.mq.service;

import com.spud.barrage.common.data.config.RedisConfig;
import com.spud.barrage.common.mq.config.RabbitMQConfig;
import com.spud.barrage.common.mq.config.RoomManager;
import com.spud.barrage.common.mq.util.MqUtils;
import com.spud.barrage.constant.RoomType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 房间热度监控服务
 * 检测房间热度变化并触发房间类型变更
 *
 * @author Spud
 * @date 2025/3/24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoomHeatMonitorService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RoomManager roomManager;

    // 缓存房间热度数据，避免频繁触发房间类型变更
    private final Map<Long, RoomHeatData> roomHeatCache = new ConcurrentHashMap<>();

    /**
     * 定时检查房间热度变化
     * 每分钟执行一次
     */
    @Scheduled(fixedRate = 60000)
    public void checkRoomHeat() {
        try {
            // 获取所有活跃房间
            Set<String> activeRoomKeys = redisTemplate.keys(RedisConfig.ACTIVE_ROOM);
            if (activeRoomKeys == null || activeRoomKeys.isEmpty()) {
                return;
            }

            log.debug("Checking heat for {} active rooms", activeRoomKeys.size());

            // 处理每个房间
            for (String roomKey : activeRoomKeys) {
                try {
                    Long roomId = MqUtils.extractRoomId(roomKey);
                    if (roomId == null) {
                        continue;
                    }

                    // 获取房间观众数量
                    String viewerKey = String.format(RedisConfig.ROOM_VIEWER, roomId);
                    Object viewersObj = redisTemplate.opsForValue().get(viewerKey);
                    int viewers = 0;
                    if (viewersObj != null) {
                        try {
                            viewers = Integer.parseInt(viewersObj.toString());
                        } catch (NumberFormatException e) {
                            log.error("Invalid viewers count for room {}: {}", roomId, viewersObj);
                        }
                    }

                    // 获取弹幕速率
                    String danmakuRateKey = String.format("room:%s:danmaku:rate", roomId);
                    Object rateObj = redisTemplate.opsForValue().get(danmakuRateKey);
                    int danmakuRate = 0;
                    if (rateObj != null) {
                        try {
                            danmakuRate = Integer.parseInt(rateObj.toString());
                        } catch (NumberFormatException e) {
                            log.error("Invalid danmaku rate for room {}: {}", roomId, rateObj);
                        }
                    }

                    // 获取当前房间类型
                    RoomType currentType = getRoomType(roomId);

                    // 判断新的房间类型
                    RoomType newType = determineRoomTypeByHeat(viewers, danmakuRate);

                    // 如果房间类型发生变化，更新房间类型
                    if (currentType != newType) {
                        updateRoomType(roomId, currentType, newType);
                    }

                    // 更新缓存
                    roomHeatCache.put(roomId, new RoomHeatData(viewers, danmakuRate, newType));

                } catch (Exception e) {
                    log.error("Error processing room heat for room key {}: {}", roomKey, e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            log.error("Error checking room heat: {}", e.getMessage(), e);
        }
    }

    /**
     * 获取房间当前类型
     */
    private RoomType getRoomType(Long roomId) {
        // 首先尝试从缓存获取
        RoomHeatData cachedData = roomHeatCache.get(roomId);
        if (cachedData != null) {
            return cachedData.roomType();
        }

        // 从Redis获取
        String typeKey = String.format(RedisConfig.ROOM_TYPE_CHANGE, roomId);
        Object typeObj = redisTemplate.opsForValue().get(typeKey);
        if (typeObj != null) {
            try {
                return RoomType.valueOf(typeObj.toString());
            } catch (IllegalArgumentException e) {
                log.error("Invalid room type for room {}: {}", roomId, typeObj);
            }
        }

        // 默认为普通房间
        return RoomType.NORMAL;
    }

    /**
     * 根据热度因素确定房间类型
     */
    private RoomType determineRoomTypeByHeat(int viewers, int danmakuRate) {
        // 首先根据观众数判断
        RoomType typeByViewers = MqUtils.determineRoomType(viewers);

        // 超热门房间 - 弹幕速率大于500/分钟
        if (danmakuRate > 3000000) {
            return RoomType.SUPER_HOT;
        }

        // 热门房间 - 弹幕速率大于200/分钟
        if (danmakuRate > 600000 && typeByViewers != RoomType.COLD) {
            return RoomType.HOT;
        }

        // 其他情况按观众数判断
        return typeByViewers;
    }

    /**
     * 更新房间类型
     */
    private void updateRoomType(Long roomId, RoomType oldType, RoomType newType) {
        try {
            log.info("Room {} type changing from {} to {}", roomId, oldType, newType);

            // 更新Redis中的房间类型
            redisTemplate.opsForValue().set(
                    String.format(RedisConfig.ROOM_TYPE_CHANGE, roomId),
                    newType.name());

            // 处理房间类型变化
            roomManager.processRoomStatus(roomId);

            // 发布房间状态变化事件
            redisTemplate.convertAndSend(RabbitMQConfig.ROOM_MQ_CHANGE_TOPIC, roomId.toString());

        } catch (Exception e) {
            log.error("Failed to update room type for room {}: {}", roomId, e.getMessage(), e);
        }
    }

    /**
     * 房间热度数据记录
     */
    private record RoomHeatData(
            int viewers,
            int danmakuRate,
            RoomType roomType) {
    }
}