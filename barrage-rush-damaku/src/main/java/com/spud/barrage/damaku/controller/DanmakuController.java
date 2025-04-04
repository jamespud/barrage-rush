package com.spud.barrage.damaku.controller;

import com.spud.barrage.common.core.constant.ApiConstants;
import com.spud.barrage.common.core.io.Result;
import com.spud.barrage.common.data.dto.DanmakuMessage;
import com.spud.barrage.common.data.dto.DanmakuRequest;
import com.spud.barrage.damaku.service.DanmakuService;
import jakarta.validation.Valid;
import java.util.Collection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 弹幕控制器
 * 处理弹幕相关的HTTP请求
 *
 * @author Spud
 * @date 2025/4/10
 */
@Slf4j
@RestController
@RequestMapping(ApiConstants.DANMAKU_PREFIX)
public class DanmakuController {

  @Autowired
  private DanmakuService danmakuService;

  /**
   * 发送弹幕
   * 通过HTTP发送弹幕，并将弹幕推送到消息队列
   *
   * @param request 弹幕请求体
   * @param roomId  房间ID
   * @return 处理结果
   */
  @PreAuthorize("isAuthenticated()")
  @PostMapping(ApiConstants.SEND_DANMAKU)
  public Result<DanmakuMessage> sendDanmaku(@Valid @RequestBody DanmakuRequest request,
      @PathVariable Long roomId) {

    try {
      // 处理弹幕
      DanmakuMessage message = danmakuService.processDanmaku(request);
      if (message != null) {
        return Result.success(message);
      } else {
        return Result.fail("弹幕发送失败，请稍后重试");
      }
    } catch (Exception e) {
      log.error("发送弹幕异常", e);
      return Result.fail("系统错误");
    }
  }

  /**
   * 获取最近弹幕
   *
   * @param roomId 房间ID
   * @param limit  获取数量限制，默认为20条
   * @return 最近的弹幕消息
   */
  @GetMapping(ApiConstants.GET_RECENT)
  public Result<Collection<DanmakuMessage>> getRecentDanmaku(
      @PathVariable Long roomId,
      @PathVariable(required = false) Integer limit) {
    try {
      // 默认获取20条
      int messageLimit = limit != null ? limit : 20;
      Collection<DanmakuMessage> messages = danmakuService.getRecentDanmaku(roomId, messageLimit);
      return Result.success(messages);
    } catch (Exception e) {
      log.error("获取最近弹幕异常", e);
      return Result.fail("系统错误");
    }
  }
}