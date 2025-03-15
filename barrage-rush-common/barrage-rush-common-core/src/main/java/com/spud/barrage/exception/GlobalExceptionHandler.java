package com.spud.barrage.exception;

import com.spud.barrage.common.data.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * @author Spud
 * @date 2025/3/6
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(BusinessException.class)
  public Result<Void> handleBusinessException(BusinessException e) {
    log.warn("Business exception: {}", e.getMessage());
    return Result.fail(e.getCode(), e.getMessage());
  }

  @ExceptionHandler(Exception.class)
  public Result<Void> handleException(Exception e) {
    log.error("System error", e);
    return Result.fail(500, "系统错误");
  }
}