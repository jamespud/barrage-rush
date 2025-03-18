package com.spud.barrage.push.enpoint;

import com.spud.barrage.constant.ApiConstants;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author Spud
 * @date 2025/3/10
 */
@Slf4j
@Component
@ServerEndpoint(value = ApiConstants.DANMAKU_ENDPOINT)
public class DanmakuMessageEndpoint {

}
