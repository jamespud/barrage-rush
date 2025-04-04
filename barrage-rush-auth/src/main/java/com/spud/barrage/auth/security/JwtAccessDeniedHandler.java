package com.spud.barrage.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * JWT访问拒绝处理器
 * 处理已认证的用户访问未授权资源时的异常
 *
 * @author Spud
 * @date 2025/3/27
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

  private final ObjectMapper objectMapper;

  @Override
  public void handle(HttpServletRequest request, HttpServletResponse response,
      AccessDeniedException accessDeniedException) throws IOException {
    log.error("访问拒绝: {}, 异常: {}", request.getRequestURI(),
        accessDeniedException.getMessage());

    response.setStatus(HttpStatus.FORBIDDEN.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());

    Map<String, Object> responseBody = new HashMap<>();
    responseBody.put("status", HttpStatus.FORBIDDEN.value());
    responseBody.put("error", "Forbidden");
    responseBody.put("message", "没有权限访问该资源");
    responseBody.put("path", request.getRequestURI());

    try (OutputStream outputStream = response.getOutputStream()) {
      objectMapper.writeValue(outputStream, responseBody);
      outputStream.flush();
    }
  }
}