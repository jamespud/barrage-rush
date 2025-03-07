package com.spud.barrage.endpoint;

import com.spud.barrage.model.entity.DanmakuMessage;
import jakarta.websocket.CloseReason;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Controller;

/**
 * @author Spud
 * @date 2025/3/4
 */
@Slf4j
@ServerEndpoint(value = "/barrage")
@Controller
public class Demo {

  private Session session;

  // 收到消息
  @OnMessage
  public void onMessage(DanmakuMessage message) throws IOException {
    
  }

  // 连接打开
  @OnOpen
  public void onOpen(Session session, EndpointConfig endpointConfig) {
    // 保存 session 到对象
    this.session = session;
    log.info("[websocket] 新的连接：id={}", this.session.getId());
  }

  // 连接关闭
  @OnClose
  public void onClose(CloseReason closeReason) {
    log.info("[websocket] 连接断开：id={}，reason={}", this.session.getId(), closeReason);
  }

  // 连接异常
  @OnError
  public void onError(Throwable throwable) throws IOException {

    log.info("[websocket] 连接异常：id={}，throwable={}", this.session.getId(),
        throwable.getMessage());

    // 关闭连接。状态码为 UNEXPECTED_CONDITION（意料之外的异常）
    this.session.close(
        new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, throwable.getMessage()));
  }

}
