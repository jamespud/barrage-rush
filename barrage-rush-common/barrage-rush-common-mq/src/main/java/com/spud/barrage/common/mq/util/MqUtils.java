package com.spud.barrage.common.mq.util;

import com.spud.barrage.common.mq.config.DynamicQueueConfig.RoomType;
import com.spud.barrage.common.mq.config.RabbitMQConfig;

/**
 * @author Spud
 * @date 2025/3/12
 */
public class MqUtils {

  public static RoomType checkRoomType(Integer viewers) {
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
}
