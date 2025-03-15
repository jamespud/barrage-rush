package com.spud.barrage.damaku.controller;

import com.spud.barrage.common.data.dto.DanmakuMessage;
import com.spud.barrage.common.data.dto.DanmakuRequest;
import com.spud.barrage.common.data.dto.Result;
import com.spud.barrage.constant.ApiConstants;
import com.spud.barrage.damaku.service.DanmakuService;
import com.spud.barrage.util.SnowflakeIdWorker;
import jakarta.validation.Valid;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Spud
 * @date 2025/3/6
 */
@Slf4j
@RestController
@RequestMapping(ApiConstants.DANMAKU_PREFIX)
public class DanmakuController {

  @Autowired
  private DanmakuService danmakuService;
  
  @Autowired
  private SnowflakeIdWorker snowflakeIdWorker;

  /**
   * 通过http发送弹幕，并将弹幕推送到mq
   */
  @PostMapping(ApiConstants.SEND_DANMAKU)
  public Result<Void> sendDanmaku(@Valid @RequestBody DanmakuRequest request) {
    // TODO: 从token获取用户id
    Long userId = 1L;
    DanmakuMessage danmakuMessage = buildDanmakuMessage(request, userId);

    danmakuService.processDanmaku(danmakuMessage);

    return Result.success();
  }

  private DanmakuMessage buildDanmakuMessage(DanmakuRequest request, Long userId) {

    return DanmakuMessage.builder()
        .id(snowflakeIdWorker.nextId())
        .userId(userId)
        .roomId(request.getRoomId())
        .content(request.getContent())
        .type(request.getType())
        .timestamp(System.currentTimeMillis())
        .build();

  }

  /**
   * 获取最近弹幕
   */
  @GetMapping(ApiConstants.GET_RECENT)
  public Result<List<DanmakuMessage>> getRecentDanmaku(
      @RequestParam Long roomId,
      @RequestParam Long userId) {
    try {
      List<DanmakuMessage> messages = danmakuService.getRecentDanmaku(roomId, userId, 10);
      return Result.success(messages);
    } catch (Exception e) {
      log.error("Get recent danmaku error", e);
      return Result.fail("系统错误");
    }
  }


}