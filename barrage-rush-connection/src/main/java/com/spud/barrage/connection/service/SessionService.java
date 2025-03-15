package com.spud.barrage.connection.service;

import com.spud.barrage.connection.model.UserSession;

/**
 * 会话服务接口
 * 
 * @author Spud
 * @date 2025/3/15
 */
public interface SessionService {
    
    /**
     * 创建用户会话
     *
     * @param userId 用户ID
     * @param roomId 房间ID
     * @param nickname 用户昵称
     * @param avatar 用户头像
     * @param ip 用户IP
     * @param location 用户位置
     * @return 用户会话
     */
    UserSession createSession(Long userId, Long roomId, String nickname, String avatar, String ip, String location);
    
    /**
     * 获取用户会话
     *
     * @param sessionId 会话ID
     * @return 用户会话
     */
    UserSession getSession(String sessionId);
    
    /**
     * 更新用户会话
     *
     * @param session 用户会话
     * @return 是否成功
     */
    boolean updateSession(UserSession session);
    
    /**
     * 删除用户会话
     *
     * @param sessionId 会话ID
     * @return 是否成功
     */
    boolean deleteSession(String sessionId);
    
    /**
     * 设置数据连接会话ID
     *
     * @param sessionId 会话ID
     * @param dataSessionId 数据连接会话ID
     * @return 是否成功
     */
    boolean setDataSessionId(String sessionId, String dataSessionId);
    
    /**
     * 设置心跳连接会话ID
     *
     * @param sessionId 会话ID
     * @param heartbeatSessionId 心跳连接会话ID
     * @return 是否成功
     */
    boolean setHeartbeatSessionId(String sessionId, String heartbeatSessionId);
    
    /**
     * 更新用户最后活跃时间
     *
     * @param sessionId 会话ID
     * @return 是否成功
     */
    boolean updateLastActiveTime(String sessionId);
    
    /**
     * 设置用户在线状态
     *
     * @param sessionId 会话ID
     * @param online 是否在线
     * @return 是否成功
     */
    boolean setOnlineStatus(String sessionId, boolean online);
    
    /**
     * 获取房间在线用户数
     *
     * @param roomId 房间ID
     * @return 在线用户数
     */
    long getRoomOnlineCount(Long roomId);
    
    /**
     * 增加房间在线用户数
     *
     * @param roomId 房间ID
     * @return 增加后的在线用户数
     */
    long incrementRoomOnlineCount(Long roomId);
    
    /**
     * 减少房间在线用户数
     *
     * @param roomId 房间ID
     * @return 减少后的在线用户数
     */
    long decrementRoomOnlineCount(Long roomId);
} 