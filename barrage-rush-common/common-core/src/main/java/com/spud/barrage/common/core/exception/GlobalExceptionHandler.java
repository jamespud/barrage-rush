package com.spud.barrage.common.core.exception;

import com.spud.barrage.common.core.io.Result;
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

  @ExceptionHandler(BarrageException.class)
  public Result<Void> handleBusinessException(BarrageException e) {
    log.warn("Business exception: {}", e.getMessage());
    return Result.fail(e.getCode(), e.getMessage());
  }

  @ExceptionHandler(Exception.class)
  public Result<Void> handleException(Exception e) {
    log.error("System error", e);
    return Result.fail(500, "系统错误");
  }
}