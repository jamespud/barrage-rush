package com.spud.barrage.common.data.entity;

import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * @author Spud
 * @date 2025/3/16
 */
@Table(name = "user_token")
public class UserToken {

  @Id
  private Long userId;
  
  private int tokenVersion;
  
}
