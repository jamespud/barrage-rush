package com.spud.barrage.common.mq.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 房间流量相关配置属性
 *
 * @author Spud
 * @date 2025/4/01
 */
@Data
@Component
@ConfigurationProperties(prefix = "barrage.room.traffic")
public class RoomTrafficProperties {

  /**
   * 线程池配置
   */
  private ThreadPool threadPool = new ThreadPool();

  /**
   * 分片配置
   */
  private Shard shard = new Shard();

  /**
   * 调度配置
   */
  private Schedule schedule = new Schedule();

  /**
   * 锁超时时间(毫秒)
   */
  private long lockTimeoutMillis = 10000;

  /**
   * 流量阈值配置
   */
  private Threshold threshold = new Threshold();

  /**
   * 线程池配置
   */
  @Data
  public static class ThreadPool {

    /**
     * 核心线程数
     */
    private int coreSize = 4;

    /**
     * 最大线程数
     */
    private int maxSize = 8;

    /**
     * 线程保持活跃时间(秒)
     */
    private int keepAliveSeconds = 60;

    /**
     * 队列容量
     */
    private int queueCapacity = 1000;
  }

  /**
   * 分片配置
   */
  @Data
  public static class Shard {

    /**
     * 超热门房间分片数
     */
    private int superHotShardCount = 5;

    /**
     * 热门房间分片数
     */
    private int hotShardCount = 3;

    /**
     * 普通房间分片数
     */
    private int normalShardCount = 1;

    /**
     * 冷门房间分片数
     */
    private int coldShardCount = 1;

    /**
     * 房间类型改变间隔(秒)
     */
    private int typeChangeInterval = 60;
  }

  /**
   * 调度配置
   */
  @Data
  public static class Schedule {

    /**
     * 初始延迟时间(秒)
     */
    private int initialDelaySeconds = 10;

    /**
     * 周期时间(秒)
     */
    private int periodSeconds = 60;
  }

  /**
   * 流量阈值配置
   */
  @Data
  public static class Threshold {

    private int cold = 1000;

    private int normal = 10000;

    private int hot = 50000;

    private int superHot = 100000;
  }
}
