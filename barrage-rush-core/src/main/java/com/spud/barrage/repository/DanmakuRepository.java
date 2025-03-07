package com.spud.barrage.repository;

import com.spud.barrage.model.entity.DanmakuMessage;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * @author Spud
 * @date 2025/3/7
 */
@Repository
public interface DanmakuRepository extends JpaRepository<DanmakuMessage, Long> {

  @Query("SELECT d FROM DanmakuMessage d WHERE d.roomId = :roomId ORDER BY d.timestamp DESC")
  List<DanmakuMessage> findRecentByRoomId(String roomId, Pageable pageable);
}