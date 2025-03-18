package com.spud.barrage.auth.service.impl;

import com.spud.barrage.auth.dto.BarrageUserDetail;
import com.spud.barrage.auth.service.AnchorUserDetailService;
import com.spud.barrage.common.data.entity.AnchorRoomSilence;
import com.spud.barrage.common.data.entity.User;
import com.spud.barrage.common.data.repository.AnchorRoomSilenceRepository;
import com.spud.barrage.common.data.service.UserService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * @author Spud
 * @date 2025/3/16
 */
@Service
public class AnchorUserDetailServiceImpl implements AnchorUserDetailService {
  
  @Autowired
  private UserService userService;
  
  @Autowired
  private AnchorRoomSilenceRepository roomSilenceRepository;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    User user = userService.getUserByUsername(username);
    List<AnchorRoomSilence> roomSilences = roomSilenceRepository.getAnchorRoomSilenceByUserId(
        user.getUserId());
    BarrageUserDetail userDetail = new BarrageUserDetail(user);
    for (AnchorRoomSilence roomSilence : roomSilences) {
      userDetail.getBannedRooms().add(Pair.of(roomSilence.getRoomId(), roomSilence.getEndTime()));
    }
    return userDetail;
  }
}
