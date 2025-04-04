package com.spud.barrage.damaku.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.spud.barrage.common.core.util.SnowflakeIdWorker;
import com.spud.barrage.common.data.dto.DanmakuMessage;
import com.spud.barrage.common.data.dto.DanmakuRequest;
import com.spud.barrage.common.data.entity.AnchorRoomConfig;
import com.spud.barrage.common.data.repository.AnchorRoomConfigRepository;
import com.spud.barrage.common.data.repository.AnchorRoomSilenceRepository;
import com.spud.barrage.damaku.mq.DanmakuProducer;
import com.spud.barrage.damaku.service.DanmakuService;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 弹幕服务实现类
 * 负责处理弹幕消息的验证、发送和缓存
 *
 * @author Spud
 * @date 2025/3/4
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DanmakuServiceImpl implements DanmakuService {

  // 房间是否允许发言缓存
  // 房间 -> 是否允许发言
  static final Cache<Long, Boolean> ROOM_BAN_CACHE = Caffeine.newBuilder()
      .expireAfterAccess(10, TimeUnit.MINUTES).build();

  // 房间用户封禁计数器
  // 房间 -> 用户 -> 封禁时长
  static final Cache<Long, Cache<Long, Long>> USER_BAN_CACHE = Caffeine.newBuilder()
      .expireAfterAccess(10, TimeUnit.MINUTES).build();

  private static final LoadingCache<Long, Cache<Long, DanmakuMessage>> messageCache = Caffeine.newBuilder()
      .maximumSize(1000)
      .expireAfterWrite(5, TimeUnit.MINUTES)
      .build(roomId -> Caffeine.newBuilder()
          .maximumSize(500)
          .expireAfterWrite(5, TimeUnit.MINUTES)
          .build());

  @Autowired
  private RedisTemplate<String, Object> redisTemplate;

  private final DanmakuProducer danmakuProducer;

  @Autowired
  private SnowflakeIdWorker snowflakeIdWorker;

  @Autowired
  private AnchorRoomSilenceRepository roomSilenceRepository;

  @Autowired
  private AnchorRoomConfigRepository roomConfigRepository;

  @Override
  public DanmakuMessage processDanmaku(DanmakuRequest request) {
    // 1. 创建弹幕消息
    DanmakuMessage message = request.createDanmakuMessage(snowflakeIdWorker.nextId(),
        getUserIdFromContext());

    // 2. 消息验证
    if (validateMessage(message)) {
      // 3. 发送到消息队列
      boolean sent = danmakuProducer.sendDanmaku(message);
      if (sent) {
        // 4. 更新本地缓存
        messageCache.get(message.getRoomId()).put(message.getId(), message);
        log.info("弹幕处理成功: roomId={}, messageId={}", message.getRoomId(), message.getId());
        return message;
      }
    }
    return null;
  }

  /**
   * 从认证上下文中获取用户ID
   */
  private Long getUserIdFromContext() {
    // TODO: 从Spring Security上下文中获取用户ID
    return 10000L; // 临时返回测试ID
  }

  /**
   * 验证弹幕消息
   */
  private boolean validateMessage(DanmakuMessage message) {
    boolean valid = checkSendPermission(message.getUserId(), message.getRoomId());
    // TODO: 验证弹幕格式，消息过滤
    return valid;
  }

  @Override
  public boolean checkSendPermission(Long userId, Long roomId) {
    // 1. 检查房间是否允许发弹幕
    boolean allow = checkRoomAllowDanmaku(roomId);
    if (!allow) {
      return false;
    }
    // 2. 检查用户在房间是否被禁言
    allow &= checkUserAllowDanmaku(roomId, userId);
    if (!allow) {
      return false;
    }

    // TODO:  3. 检查发送频率
    return allow;
  }

  @Override
  public Collection<DanmakuMessage> getRecentDanmaku(Long roomId, int limit) {
    return messageCache.get(roomId).asMap().values();
  }

  public boolean checkRoomAllowDanmaku(Long roomId) {
    boolean allow;
    Boolean present = ROOM_BAN_CACHE.getIfPresent(roomId);
    if (present == null) {
      // 缓存不存在, 查询数据库
      AnchorRoomConfig roomConfig = roomConfigRepository.findAnchorRoomConfigByRoomId(
          roomId);
      // 更新本地缓存
      allow = roomConfig != null && roomConfig.getAllowDanmaku();
      ROOM_BAN_CACHE.put(roomId, allow);
      // TODO: 是否需要redis缓存
    } else {
      allow = present;
    }
    return allow;
  }

  /**
   * 检查用户在该房间是否允许发言
   * @param roomId 房间ID
   * @param userId 用户ID
   * @return true: 允许发言; false: 禁止发言
   */
  public boolean checkUserAllowDanmaku(Long roomId, Long userId) {
    boolean allow = true;
    Cache<Long, Long> present = USER_BAN_CACHE.getIfPresent(roomId);
    if (present == null) {
      Map<Long, Long> silencedUsers = roomSilenceRepository.findUserIdAndEndTimeByRoomId(roomId);
      if (!silencedUsers.isEmpty() && silencedUsers.containsKey(userId)) {
        allow = false;
      }
      Cache<Long, Long> cache = Caffeine.newBuilder()
          .expireAfterAccess(10, TimeUnit.MINUTES)
          .build();
      cache.putAll(silencedUsers);
      USER_BAN_CACHE.put(roomId, cache);
      // TODO: 是否需要redis缓存
    } else {
      Long cacheAllow = present.getIfPresent(userId);
      if (cacheAllow != null) {
        allow = false;
      }
    }
    return allow;
  }

}