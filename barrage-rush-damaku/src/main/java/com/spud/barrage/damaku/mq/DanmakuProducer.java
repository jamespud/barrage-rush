package com.spud.barrage.damaku.mq;

import com.spud.barrage.common.data.dto.DanmakuMessage;
import com.spud.barrage.common.mq.producer.AbstractRabbitProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 弹幕生产者
 * 负责将弹幕消息发送到消息队列
 * 
 * @author Spud
 * @date 2025/3/4
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DanmakuProducer extends AbstractRabbitProducer {

  // 最大重试次数
  private static final int MAX_RETRY = 3;
  // 重试间隔基数（毫秒）
  private static final long RETRY_INTERVAL_BASE = 50;

  @Override
  public boolean sendDanmaku(DanmakuMessage message) {
    if (message == null || message.getRoomId() == null || message.getUserId() == null) {
      log.error("Invalid danmaku message: {}", message);
      return false;
    }

    Long roomId = message.getRoomId();
    Long userId = message.getUserId();

    // 重试计数
    int retryCount = 0;
    boolean sent = false;

    // 发送消息，失败后重试
    while (!sent && retryCount < MAX_RETRY) {
      try {
        sent = super.sendMessage(roomId, userId, message);

        if (!sent) {
          retryCount++;
          if (retryCount < MAX_RETRY) {
            // 指数退避策略
            long sleepTime = RETRY_INTERVAL_BASE * (1L << retryCount);
            log.warn("Failed to send danmaku for room {}, retry {}/{} after {} ms",
                roomId, retryCount, MAX_RETRY, sleepTime);
            Thread.sleep(sleepTime);
          }
        }
      } catch (Exception e) {
        retryCount++;
        if (retryCount < MAX_RETRY) {
          log.error("Error sending danmaku for room {}, retry {}/{}: {}",
              roomId, retryCount, MAX_RETRY, e.getMessage());
          try {
            // 指数退避策略
            Thread.sleep(RETRY_INTERVAL_BASE * (1L << retryCount));
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.error("Interrupted during retry wait", ie);
            break;
          }
        } else {
          log.error("Failed to send danmaku after {} retries: {}", MAX_RETRY, e.getMessage(), e);
        }
      }
    }

    if (sent) {
      log.info("Successfully sent danmaku: roomId={}, userId={}, content='{}'",
          roomId, userId, message.getContent());
    } else {
      log.error("Failed to send danmaku after {} attempts: roomId={}, userId={}",
          MAX_RETRY, roomId, userId);
    }

    return sent;
  }
}
