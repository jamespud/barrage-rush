package com.spud.barrage.common.data.entity;

import java.io.Serial;
import java.io.Serializable;
import java.sql.Date;

/**
 * @author Spud
 * @date 2025/3/10
 */
public class BaseEntity implements Serializable {
  
  @Serial
  private static final long serialVersionUID = 1L;

  private Date createTime;
  
  private Date updateTime;

  private int isDelete;

  public Date getCreateTime() {
    return createTime;
  }

  public void setCreateTime(Date createTime) {
    this.createTime = createTime;
  }

  public Date getUpdateTime() {
    return updateTime;
  }

  public void setUpdateTime(Date updateTime) {
    this.updateTime = updateTime;
  }

  public int getIsDelete() {
    return isDelete;
  }

  public void setIsDelete(int isDelete) {
    this.isDelete = isDelete;
  }
}
