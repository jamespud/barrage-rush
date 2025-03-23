package com.spud.barrage.common.mq.util;

import com.spud.barrage.common.mq.config.RabbitMQConfig;
import com.spud.barrage.constant.RoomType;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Spud
 * @date 2025/3/12
 */
@Slf4j
public class MqUtils {

  /**
   * 根据观众数确定房间类型
   */
  public static RoomType determineRoomType(Integer viewers) {
    RoomType type;
    if (viewers >= RabbitMQConfig.hotViewersThreshold) {
      type = RoomType.HOT;
    } else if (viewers <= RabbitMQConfig.coldViewersThreshold) {
      type = RoomType.COLD;
    } else {
      type = RoomType.NORMAL;
    }
    return type;
  }

  public static Long extractRoomId(String roomKey) {
    try {
      String[] parts = roomKey.split(":");
      if (parts.length >= 2) {
        return Long.parseLong(parts[1]);
      }
    } catch (NumberFormatException e) {
      log.warn("Failed to parse room ID from key: {}", roomKey);
    }
    return null;
  }

  // 从队列名称提取房间ID
  public static Long extractRoomIdFromQueue(String queueName) {
    try {
      String[] parts = queueName.split("\\.");
      if (parts.length >= 3) {
        return Long.parseLong(parts[parts.length - 2]);
      }
    } catch (NumberFormatException e) {
      // 忽略解析错误
    }
    return null;
  }
}
