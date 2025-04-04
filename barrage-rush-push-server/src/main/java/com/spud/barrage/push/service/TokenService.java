package com.spud.barrage.push.service;

import java.util.Map;

/**
 * 令牌服务接口
 * 用于验证和管理令牌
 *
 * @author Spud
 * @date 2025/3/30
 */
public interface TokenService {

  /**
   * 验证令牌有效性
   *
   * @param token 待验证的令牌
   * @return 如果令牌有效，返回包含用户信息的Map，否则返回null
   */
  Map<String, Object> verifyToken(String token);

  /**
   * 生成新的令牌
   *
   * @param userId        用户ID
   * @param roomId        房间ID
   * @param expireMinutes 过期时间（分钟）
   * @return 生成的令牌信息
   */
  Map<String, Object> generateToken(String userId, String roomId, int expireMinutes);

  /**
   * 吊销令牌
   *
   * @param token 待吊销的令牌
   * @return 操作是否成功
   */
  boolean revokeToken(String token);
}