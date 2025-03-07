package com.spud.barrage.controller;

import com.spud.barrage.common.constant.ApiConstants;
import com.spud.barrage.common.exception.BusinessException;
import com.spud.barrage.model.dto.DanmakuRequest;
import com.spud.barrage.model.dto.Result;
import com.spud.barrage.model.entity.DanmakuMessage;
import com.spud.barrage.service.DanmakuService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Spud
 * @date 2025/3/6
 */
@RestController
@RequestMapping(ApiConstants.API_PREFIX)
@Slf4j
public class DanmakuController {

  @Autowired
  private DanmakuService danmakuService;

  /**
   * 发送弹幕（HTTP备用接口）
   */
  @PostMapping(ApiConstants.SEND_DANMAKU)
  public Result<Void> sendDanmaku(@Valid @RequestBody DanmakuRequest request,
      @RequestHeader("Authorization") String token) {
    try {
      // 1. 验证token
      String userId = validateToken(token);

      // 2. 构建消息
      DanmakuMessage message = buildDanmakuMessage(request, userId);

      // 3. 处理弹幕
      danmakuService.processDanmaku(message);

      return Result.success();
    } catch (BusinessException e) {
      log.warn("Send danmaku failed: {}", e.getMessage());
      return Result.fail(e.getMessage());
    } catch (Exception e) {
      log.error("Send danmaku error", e);
      return Result.fail("系统错误");
    }
  }

  private DanmakuMessage buildDanmakuMessage(@Valid DanmakuRequest request, String userId) {
    return null;
  }

  private String validateToken(String token) {
    
    return "1";
  }

  /**
   * 获取最近弹幕
   */
  @GetMapping(ApiConstants.GET_RECENT)
  public Result<List<DanmakuMessage>> getRecentDanmaku(
      @RequestParam Long roomId,
      @RequestParam(defaultValue = "100") Integer limit) {
    try {
      List<DanmakuMessage> messages = danmakuService.getRecentDanmaku(
          String.valueOf(roomId), limit);
      return Result.success(messages);
    } catch (Exception e) {
      log.error("Get recent danmaku error", e);
      return Result.fail("系统错误");
    }
  }
}