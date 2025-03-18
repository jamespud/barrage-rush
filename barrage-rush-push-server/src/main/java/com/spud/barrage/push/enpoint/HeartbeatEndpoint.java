package com.spud.barrage.push.enpoint;

import com.spud.barrage.common.data.dto.DanmakuMessage;
import com.spud.barrage.constant.ApiConstants;
import jakarta.websocket.CloseReason;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author Spud
 * @date 2025/3/10
 */
@Slf4j
@Component
@ServerEndpoint(value = ApiConstants.HEARTBEAT_ENDPOINT)
public class HeartbeatEndpoint {

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
    log.debug("[websocket] 新的连接：id={}", this.session.getId());
  }

  // 连接关闭
  @OnClose
  public void onClose(CloseReason closeReason) {
    log.debug("[websocket] 连接断开：id={}，reason={}", this.session.getId(), closeReason);
  }

  // 连接异常
  @OnError
  public void onError(Throwable throwable) throws IOException {

    log.error("[websocket] 连接异常：id={}，throwable={}", this.session.getId(),
        throwable.getMessage());

    // 关闭连接。状态码为 UNEXPECTED_CONDITION（意料之外的异常）
    this.session.close(
        new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, throwable.getMessage()));
  }
}
