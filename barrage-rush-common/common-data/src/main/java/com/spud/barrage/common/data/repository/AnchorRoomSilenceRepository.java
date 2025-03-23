package com.spud.barrage.common.data.repository;

import com.spud.barrage.common.data.entity.AnchorRoomSilence;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author Spud
 * @date 2025/3/16
 */
@Repository
public interface AnchorRoomSilenceRepository extends JpaRepository<AnchorRoomSilence, Long> {
  
  List<AnchorRoomSilence> getAnchorRoomSilenceByUserId(Long userId);

}
