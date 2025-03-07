package com.spud.barrage.service.impl;

import com.spud.barrage.common.constant.ApiConstants;
import com.spud.barrage.model.entity.RoomConfig;
import com.spud.barrage.service.RoomService;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * @author Spud
 * @date 2025/3/6
 */
@Service
@Slf4j
public class RoomServiceImpl implements RoomService {

  @Autowired
  private RedisTemplate<String, Object> redisTemplate;

  @Override
  public Set<String> getOnlineUsers(String roomId) {
    String key = String.format(ApiConstants.REDIS_ROOM_USERS, roomId);
    Set<Object> members = redisTemplate.opsForSet().members(key);
    if (!CollectionUtils.isEmpty(members)) {
      return members.stream().map(Object::toString).collect(Collectors.toSet());
    }
    return null;
  }

  @Override
  public String getUserRoom(String userId) {
    String key = String.format(ApiConstants.REDIS_USER_CONNECTION, userId);
    return (String) redisTemplate.opsForHash().get(key, "roomId");
  }

  @Override
  public void joinRoom(String userId, String roomId) {
    // 1. 更新房间用户列表
    String roomKey = String.format(ApiConstants.REDIS_ROOM_USERS, roomId);
    redisTemplate.opsForSet().add(roomKey, userId);
    redisTemplate.expire(roomKey, 24, TimeUnit.HOURS);

    // 2. 更新用户连接信息
    String userKey = String.format(ApiConstants.REDIS_USER_CONNECTION, userId);
    redisTemplate.opsForHash().put(userKey, "roomId", roomId);
    redisTemplate.opsForHash().put(userKey, "joinTime", System.currentTimeMillis());
    redisTemplate.expire(userKey, 24, TimeUnit.HOURS);

    log.info("User {} joined room {}", userId, roomId);
  }

  @Override
  public void leaveRoom(String userId, String roomId) {
    // 1. 从房间用户列表移除
    String roomKey = String.format(ApiConstants.REDIS_ROOM_USERS, roomId);
    redisTemplate.opsForSet().remove(roomKey, userId);

    // 2. 清除用户连接信息
    String userKey = String.format(ApiConstants.REDIS_USER_CONNECTION, userId);
    redisTemplate.delete(userKey);

    log.info("User {} left room {}", userId, roomId);
  }

  @Override
  public RoomConfig getRoomConfig(String roomId) {
    String key = String.format(ApiConstants.REDIS_ROOM_CONFIG, roomId);
    return (RoomConfig) redisTemplate.opsForValue().get(key);
  }
}