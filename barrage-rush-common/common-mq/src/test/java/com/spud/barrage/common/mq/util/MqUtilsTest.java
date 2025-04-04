package com.spud.barrage.common.mq.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.spud.barrage.common.data.mq.enums.RoomType;
import com.spud.barrage.common.mq.config.RabbitMQConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * MqUtils工具类单元测试
 *
 * @author Spud
 * @date 2025/4/01
 */
class MqUtilsTest {

  @BeforeEach
  void setUp() {
    // 设置测试用的房间类型阈值
    RabbitMQConfig.superHotViewersThreshold = 10000;
    RabbitMQConfig.hotViewersThreshold = 1000;
    RabbitMQConfig.coldViewersThreshold = 10;
  }

  @Test
  void determineRoomTypeWhenNull() {
    // 当观众数为null时应返回NORMAL
    RoomType type = MqUtils.determineRoomType(null);
    assertEquals(RoomType.NORMAL, type, "Null viewer count should default to NORMAL room type");
  }

  @Test
  void determineRoomTypeForSuperHot() {
    // 超热门直播间测试 (>=10000观众)
    RoomType type = MqUtils.determineRoomType(12000);
    assertEquals(RoomType.SUPER_HOT, type, "12000 viewers should be classified as SUPER_HOT");

    type = MqUtils.determineRoomType(10000);
    assertEquals(RoomType.SUPER_HOT, type, "10000 viewers should be classified as SUPER_HOT");
  }

  @Test
  void determineRoomTypeForHot() {
    // 热门直播间测试 (>=1000 且 <10000观众)
    RoomType type = MqUtils.determineRoomType(5000);
    assertEquals(RoomType.HOT, type, "5000 viewers should be classified as HOT");

    type = MqUtils.determineRoomType(1000);
    assertEquals(RoomType.HOT, type, "1000 viewers should be classified as HOT");

    type = MqUtils.determineRoomType(9999);
    assertEquals(RoomType.HOT, type, "9999 viewers should be classified as HOT");
  }

  @Test
  void determineRoomTypeForNormal() {
    // 普通直播间测试 (>10 且 <1000观众)
    RoomType type = MqUtils.determineRoomType(500);
    assertEquals(RoomType.NORMAL, type, "500 viewers should be classified as NORMAL");

    type = MqUtils.determineRoomType(11);
    assertEquals(RoomType.NORMAL, type, "11 viewers should be classified as NORMAL");

    type = MqUtils.determineRoomType(999);
    assertEquals(RoomType.NORMAL, type, "999 viewers should be classified as NORMAL");
  }

  @Test
  void determineRoomTypeForCold() {
    // 冷门直播间测试 (<=10观众)
    RoomType type = MqUtils.determineRoomType(10);
    assertEquals(RoomType.COLD, type, "10 viewers should be classified as COLD");

    type = MqUtils.determineRoomType(0);
    assertEquals(RoomType.COLD, type, "0 viewers should be classified as COLD");

    type = MqUtils.determineRoomType(5);
    assertEquals(RoomType.COLD, type, "5 viewers should be classified as COLD");
  }

  @Test
  void extractRoomId() {
    // 测试从房间key中提取房间ID
    Long roomId = MqUtils.extractRoomId("room:12345:viewers");
    assertEquals(12345L, roomId, "Should extract room ID 12345 from key");

    roomId = MqUtils.extractRoomId("prefix:98765:suffix:extra");
    assertEquals(98765L, roomId, "Should extract room ID 98765 from complex key");

    roomId = MqUtils.extractRoomId("invalid_key");
    assertNull(roomId, "Should return null for invalid key format");

    roomId = MqUtils.extractRoomId("prefix:not_a_number:suffix");
    assertNull(roomId, "Should return null when ID is not a number");
  }

  @Test
  void extractRoomIdFromQueue() {
    // 测试从队列名称中提取房间ID
    Long roomId = MqUtils.extractRoomIdFromQueue("danmaku.queue.12345.0");
    assertEquals(12345L, roomId, "Should extract room ID 12345 from queue name");

    roomId = MqUtils.extractRoomIdFromQueue("prefix.98765");
    assertEquals(98765L, roomId, "Should extract room ID 98765 from simple queue name");

    roomId = MqUtils.extractRoomIdFromQueue("invalid_queue");
    assertNull(roomId, "Should return null for invalid queue format");

    roomId = MqUtils.extractRoomIdFromQueue("prefix.not_a_number.suffix");
    assertNull(roomId, "Should return null when ID is not a number");
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 2, 3, 4})
  void generateQueueNameForDifferentTypes(int shardIndex) {
    // 测试为不同房间类型生成队列名称
    Long roomId = 12345L;

    // 冷门房间 - 应该返回共享队列
    String coldQueue = MqUtils.generateQueueName(roomId, RoomType.COLD, shardIndex);
    assertEquals("danmaku.queue.shared.cold", coldQueue,
        "Cold rooms should use shared queue regardless of shard index");

    // 普通房间 - 应该返回独立队列
    String normalQueue = MqUtils.generateQueueName(roomId, RoomType.NORMAL, shardIndex);
    assertEquals("danmaku.queue.12345", normalQueue,
        "Normal rooms should use dedicated queue without shard");

    // 热门房间 - 应该返回带分片的队列
    String hotQueue = MqUtils.generateQueueName(roomId, RoomType.HOT, shardIndex);
    assertEquals("danmaku.queue.12345." + shardIndex, hotQueue,
        "Hot rooms should use sharded queue");

    // 超热门房间 - 应该返回带分片的队列
    String superHotQueue = MqUtils.generateQueueName(roomId, RoomType.SUPER_HOT, shardIndex);
    assertEquals("danmaku.queue.12345." + shardIndex, superHotQueue,
        "Super hot rooms should use sharded queue");
  }

  @Test
  void generateExchangeName() {
    // 测试为不同房间类型生成交换机名称
    Long roomId = 12345L;

    // 冷门房间 - 应该返回共享交换机
    String coldExchange = MqUtils.generateExchangeName(roomId, RoomType.COLD);
    assertEquals("danmaku.exchange.shared", coldExchange,
        "Cold rooms should use shared exchange");

    // 普通房间 - 应该返回独立交换机
    String normalExchange = MqUtils.generateExchangeName(roomId, RoomType.NORMAL);
    assertEquals("danmaku.exchange.12345", normalExchange,
        "Normal rooms should use dedicated exchange");

    // 热门房间 - 应该返回独立交换机
    String hotExchange = MqUtils.generateExchangeName(roomId, RoomType.HOT);
    assertEquals("danmaku.exchange.12345", hotExchange,
        "Hot rooms should use dedicated exchange");

    // 超热门房间 - 应该返回独立交换机
    String superHotExchange = MqUtils.generateExchangeName(roomId, RoomType.SUPER_HOT);
    assertEquals("danmaku.exchange.12345", superHotExchange,
        "Super hot rooms should use dedicated exchange");
  }
}