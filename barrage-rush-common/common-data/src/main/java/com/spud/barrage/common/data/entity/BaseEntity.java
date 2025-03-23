package com.spud.barrage.common.data.entity;

import java.io.Serial;
import java.io.Serializable;
import java.sql.Date;
import lombok.Data;

/**
 * @author Spud
 * @date 2025/3/10
 */
@Data
public class BaseEntity implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private Date createTime;

  private Date updateTime;

  private boolean isDelete;
}
