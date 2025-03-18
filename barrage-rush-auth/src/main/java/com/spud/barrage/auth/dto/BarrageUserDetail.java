package com.spud.barrage.auth.dto;

import com.spud.barrage.common.data.entity.User;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import lombok.Data;
import org.springframework.data.util.Pair;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * @author Spud
 * @date 2025/3/16
 */
@Data
public class BarrageUserDetail implements UserDetails, Serializable {

  private final User user;
  
  // 被禁言的房间id: 禁言时间
  private Collection<Pair<Long, Long>> bannedRooms;

  // 被禁言的房间的权限
  private Collection<? extends GrantedAuthority> bannedRoomsAuthorities;
  
  public BarrageUserDetail(User user) {
    this.user = user;
    bannedRooms = new ArrayList<>();
    bannedRoomsAuthorities = new ArrayList<>();
  }

  @Override
  public boolean isEnabled() {
    return !user.isDelete() && !user.isBanned();
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return bannedRoomsAuthorities;
  }

  @Override
  public String getPassword() {
    return user.getPassword();
  }

  @Override
  public String getUsername() {
    return user.getUsername();
  }
}
