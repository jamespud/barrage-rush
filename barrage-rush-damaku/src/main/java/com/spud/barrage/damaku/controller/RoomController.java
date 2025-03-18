package com.spud.barrage.damaku.controller;

import com.spud.barrage.common.data.dto.Result;
import com.spud.barrage.common.data.entity.AnchorRoomConfig;
import com.spud.barrage.common.data.service.RoomService;
import com.spud.barrage.constant.ApiConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Spud
 * @date 2025/3/10
 */
@Slf4j
@RestController
@RequestMapping(ApiConstants.ROOM_PREFIX)
public class RoomController {

  @Autowired
  private RoomService roomService;

  @GetMapping(ApiConstants.ROOM_CONFIG)
  public Result<AnchorRoomConfig> roomConfig(@PathVariable("roomId") Long roomId) {
    AnchorRoomConfig roomConfig = roomService.getRoomConfig(roomId);
    // TODO: 处理房间配置
    return Result.success(roomConfig);
  }

  @GetMapping(ApiConstants.ROOM_ACTION)
  public Result<String> roomAction(@PathVariable("roomId") Long roomId) {
    // TODO: 处理房间操作
    return Result.success("success");
  }
}
