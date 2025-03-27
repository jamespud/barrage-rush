package com.spud.barrage.damaku.controller;

import com.spud.barrage.common.data.dto.DanmakuMessage;
import com.spud.barrage.common.data.dto.DanmakuRequest;
import com.spud.barrage.common.data.dto.Result;
import com.spud.barrage.common.core.constant.ApiConstants;
import com.spud.barrage.damaku.service.DanmakuService;
import com.spud.barrage.common.core.util.SnowflakeIdWorker;
import jakarta.validation.Valid;
import java.util.Collection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
  @PreAuthorize("!hasAnyRole(roomId)")
  @PostMapping(ApiConstants.SEND_DANMAKU)
  public Result<Void> sendDanmaku(@Valid @RequestBody DanmakuRequest request,
      @PathVariable Long roomId) {
    SecurityContext context = SecurityContextHolder.getContext();
    Long userId = Long.parseLong(context.getAuthentication().getName());
    DanmakuMessage danmakuMessage = new DanmakuMessage(snowflakeIdWorker.nextId(), userId, request);
    danmakuService.processDanmaku(danmakuMessage);
    return Result.success();
  }

  /**
   * 获取最近弹幕
   */
  @GetMapping(ApiConstants.GET_RECENT)
  public Result<Collection<DanmakuMessage>> getRecentDanmaku(
      @PathVariable Long roomId) {
    try {
      Collection<DanmakuMessage> messages = danmakuService.getRecentDanmaku(roomId, 10);
      return Result.success(messages);
    } catch (Exception e) {
      log.error("Get recent danmaku error", e);
      return Result.fail("系统错误");
    }
  }


}