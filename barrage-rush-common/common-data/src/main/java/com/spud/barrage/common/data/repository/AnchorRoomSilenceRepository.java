package com.spud.barrage.common.data.repository;

import com.spud.barrage.common.data.entity.AnchorRoomSilence;
import java.util.Map;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * @author Spud
 * @date 2025/3/16
 */
@Repository
public interface AnchorRoomSilenceRepository extends JpaRepository<AnchorRoomSilence, Long> {

  /**
   * 根据用户ID和房间ID查询
   *
   * @param roomId 房间ID
   * @param userId 用户ID
   * @return 结果
   */
  AnchorRoomSilence findAnchorRoomSilenceByRoomIdAndUserId(Long roomId, Long userId);

  @Query("SELECT new map(r.userId as userId, r.endTime as endTime) FROM AnchorRoomSilence r WHERE r.roomId = :roomId")
  Map<Long, Long> findUserIdAndEndTimeByRoomId(Long roomId);
}
