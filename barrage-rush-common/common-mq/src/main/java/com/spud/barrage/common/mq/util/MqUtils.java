package com.spud.barrage.common.mq.util;

import com.spud.barrage.common.core.constant.RoomType;
import com.spud.barrage.common.mq.config.RabbitMQConfig;
import lombok.extern.slf4j.Slf4j;

/**
 * MQ工具类
 * 提供与消息队列相关的工具方法
 * 
 * @author Spud
 * @date 2025/4/01
 */
@Slf4j
public class MqUtils {

  /**
   * 根据观众数确定房间类型
   * 规则:
   * - 超热门: 观众数 >= 超热门阈值 (默认 10000)
   * - 热门: 观众数 >= 热门阈值 (默认 1000)
   * - 普通: 冷门阈值 < 观众数 < 热门阈值
   * - 冷门: 观众数 <= 冷门阈值 (默认 10)
   *
   * @param viewers 房间观众数
   * @return 房间类型枚举
   */
  public static RoomType determineRoomType(Integer viewers) {
    if (viewers == null) {
      log.warn("Viewer count is null, defaulting to NORMAL room type");
      return RoomType.NORMAL;
    }

    RoomType type;
    if (viewers >= RabbitMQConfig.superHotViewersThreshold) {
      type = RoomType.SUPER_HOT;
      log.debug("Room classified as SUPER_HOT with {} viewers", viewers);
    } else if (viewers >= RabbitMQConfig.hotViewersThreshold) {
      type = RoomType.HOT;
      log.debug("Room classified as HOT with {} viewers", viewers);
    } else if (viewers <= RabbitMQConfig.coldViewersThreshold) {
      type = RoomType.COLD;
      log.debug("Room classified as COLD with {} viewers", viewers);
    } else {
      type = RoomType.NORMAL;
      log.debug("Room classified as NORMAL with {} viewers", viewers);
    }
    return type;
  }

  /**
   * 从房间Key中提取房间ID
   *
   * @param roomKey 房间键名，格式为 "prefix:roomId:suffix"
   * @return 房间ID，解析失败返回null
   */
  public static Long extractRoomId(String roomKey) {
    try {
      String[] parts = roomKey.split(":");
      if (parts.length >= 2) {
        return Long.parseLong(parts[1]);
      }
      log.warn("Invalid room key format: {}", roomKey);
    } catch (NumberFormatException e) {
      log.warn("Failed to parse room ID from key: {}", roomKey);
    }
    return null;
  }

  /**
   * 从队列名称提取房间ID
   * 队列名称格式: "prefix.roomId.suffix" 或 "prefix.roomId"
   *
   * @param queueName 队列名称
   * @return 房间ID，解析失败返回null
   */
  public static Long extractRoomIdFromQueue(String queueName) {
    try {
      String[] parts = queueName.split("\\.");
      if (parts.length >= 3) {
        // 标准格式: prefix.roomId.suffix
        return Long.parseLong(parts[parts.length - 2]);
      } else if (parts.length == 2) {
        // 简化格式: prefix.roomId
        return Long.parseLong(parts[1]);
      }
      log.warn("Unable to extract room ID from queue name: {}", queueName);
    } catch (NumberFormatException e) {
      log.warn("Failed to parse room ID from queue name: {}", queueName);
    }
    return null;
  }

  /**
   * 生成房间的队列名称
   *
   * @param roomId     房间ID
   * @param type       房间类型
   * @param shardIndex 分片索引 (对于热门房间的多分片队列)
   * @return 队列名称
   */
  public static String generateQueueName(Long roomId, RoomType type, int shardIndex) {
    return switch (type) {
      case COLD -> "danmaku.queue.shared.cold";
      case NORMAL -> String.format("danmaku.queue.%d", roomId);
      case HOT, SUPER_HOT -> String.format("danmaku.queue.%d.%d", roomId, shardIndex);
      default -> {
        log.error("Unknown room type: {}", type);
        yield String.format("danmaku.queue.%d", roomId);
      }
    };
  }

  /**
   * 生成房间的交换机名称
   *
   * @param roomId 房间ID
   * @param type   房间类型
   * @return 交换机名称
   */
  public static String generateExchangeName(Long roomId, RoomType type) {
    switch (type) {
      case COLD:
        return "danmaku.exchange.shared";
      case NORMAL:
      case HOT:
      case SUPER_HOT:
        return String.format("danmaku.exchange.%d", roomId);
      default:
        log.error("Unknown room type: {}", type);
        return String.format("danmaku.exchange.%d", roomId);
    }
  }
}
