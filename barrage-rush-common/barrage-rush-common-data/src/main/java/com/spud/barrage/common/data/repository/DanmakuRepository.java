package com.spud.barrage.common.data.repository;

import com.spud.barrage.common.data.dto.DanmakuMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DanmakuRepository extends JpaRepository<DanmakuMessage, Long> {

    /**
     * 查询房间最近的弹幕
     */
    @Query("SELECT d FROM DanmakuMessage d WHERE d.roomId = :roomId AND d.timestamp > :timestamp ORDER BY d.timestamp DESC")
    List<DanmakuMessage> findRecentByRoomId(@Param("roomId") Long roomId, @Param("timestamp") Long timestamp);
    
    /**
     * 查询用户在房间的所有弹幕
     */
    @Query("SELECT d FROM DanmakuMessage d WHERE d.roomId = :roomId AND d.userId = :userId ORDER BY d.timestamp DESC")
    List<DanmakuMessage> findByRoomIdAndUserId(@Param("roomId") Long roomId, @Param("userId") Long userId);
    
    /**
     * 查询房间弹幕总数
     */
    @Query("SELECT COUNT(d) FROM DanmakuMessage d WHERE d.roomId = :roomId")
    Long countByRoomId(@Param("roomId") Long roomId);
} 