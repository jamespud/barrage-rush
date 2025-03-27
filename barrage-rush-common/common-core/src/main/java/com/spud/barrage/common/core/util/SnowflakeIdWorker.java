package com.spud.barrage.common.core.util;

import org.springframework.stereotype.Component;

/**
 * Twitter Snowflake 分布式ID生成器实现
 *
 * @author Spud
 * @date 2025/3/6
 *
 * @desc 64位ID结构（从高位到低位）：
 * <pre>
 * 1位  - 保留位（未使用，始终为0）
 * 41位 - 时间戳差值（当前时间 - 纪元开始时间），约可使用69年
 * 5位  - 数据中心ID（支持32个数据中心）
 * 5位  - 工作节点ID（每个数据中心支持32个节点）
 * 12位 - 序列号（每节点每毫秒可生成4096个ID）
 * </pre>
 *
 * 时间戳纪元起点：2010-11-04 09:42:54.657（对应twepoch值1288834974657L）<br>
 * 线程安全实现：通过synchronized关键字保证多线程环境下的正确性
 */
@Component
public class SnowflakeIdWorker {

  private final long workerId;
  private final long datacenterId;
  // 起始时间戳（2010-11-04 09:42:54 GMT）
  private final long twepoch = 1288834974657L;
  // 工作节点ID位数
  private final long workerIdBits = 5L;
  // 数据中心ID位数
  private final long datacenterIdBits = 5L;
  // 最大工作节点ID（31）
  private final long maxWorkerId = -1L ^ (-1L << workerIdBits);
  // 最大数据中心ID（31）
  private final long maxDatacenterId = -1L ^ (-1L << datacenterIdBits);
  // 序列号位数
  private final long sequenceBits = 12L;
  // 工作节点ID左移位数（12）
  private final long workerIdShift = sequenceBits;
  // 数据中心ID左移位数（17）
  private final long datacenterIdShift = sequenceBits + workerIdBits;
  // 时间戳左移位数（22）
  private final long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;
  // 序列号掩码（4095）
  private final long sequenceMask = -1L ^ (-1L << sequenceBits);
  private long sequence = 0L;
  private long lastTimestamp = -1L;

  public SnowflakeIdWorker() {
    this(0L, 0L);
  }

  /**
   * 参数化构造函数
   *
   * @param workerId     工作节点ID（范围：0-31）
   * @param datacenterId 数据中心ID（范围：0-31）
   * @throws IllegalArgumentException 当参数超出范围时抛出
   */
  public SnowflakeIdWorker(long workerId, long datacenterId) {
    // 参数有效性校验
    if (workerId > maxWorkerId || workerId < 0) {
      throw new IllegalArgumentException(
          "Worker ID can't be greater than " + maxWorkerId + " or less than 0");
    }
    if (datacenterId > maxDatacenterId || datacenterId < 0) {
      throw new IllegalArgumentException(
          "Datacenter ID can't be greater than " + maxDatacenterId + " or less than 0");
    }
    this.workerId = workerId;
    this.datacenterId = datacenterId;
  }

  /**
   * 生成全局唯一ID（线程安全）
   *
   * 算法流程：
   * 1. 获取当前时间戳，检测时钟回拨
   * 2. 同一毫秒内递增序列号
   * 3. 序列号溢出时等待至下一毫秒
   * 4. 组合各字段生成最终ID
   *
   * @return 64位long型唯一ID
   * @throws RuntimeException 检测到时钟回拨时抛出
   */
  public synchronized long nextId() {
    long timestamp = timeGen();

    // 时钟回拨检测（允许相等，不允许小于）
    if (timestamp < lastTimestamp) {
      throw new RuntimeException("Clock moved backwards. Refusing to generate id");
    }

    // 同一毫秒内生成
    if (lastTimestamp == timestamp) {
      sequence = (sequence + 1) & sequenceMask;
      // 序列号溢出处理
      if (sequence == 0) {
        timestamp = tilNextMillis(lastTimestamp);
      }
    } else {
      // 新时间戳重置序列号
      sequence = 0L;
    }

    lastTimestamp = timestamp;

    // 组装ID各组成部分
    return ((timestamp - twepoch) << timestampLeftShift) | // 时间戳部分
        (datacenterId << datacenterIdShift) |               // 数据中心部分
        (workerId << workerIdShift) |                       // 工作节点部分
        sequence;                                           // 序列号部分
  }

  /**
   * 等待至下一毫秒（当序列号溢出时调用）
   *
   * @param lastTimestamp 上次生成ID的时间戳
   * @return 当前可用的最新时间戳
   */
  private long tilNextMillis(long lastTimestamp) {
    long timestamp = timeGen();
    // 自旋等待直到下一毫秒
    while (timestamp <= lastTimestamp) {
      timestamp = timeGen();
    }
    return timestamp;
  }

  /**
   * 获取当前系统时间戳
   *
   * @return 当前时间（毫秒）
   */
  protected long timeGen() {
    return System.currentTimeMillis();
  }
}
