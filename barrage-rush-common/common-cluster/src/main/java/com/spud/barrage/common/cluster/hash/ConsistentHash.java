package com.spud.barrage.common.cluster.hash;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.zip.CRC32;
import lombok.extern.slf4j.Slf4j;

/**
 * 一致性哈希算法的通用实现，参照Nginx实现
 * 支持虚拟节点、节点权重和自定义哈希函数
 * 使用读写锁保证线程安全
 * 采用Nginx风格的双哈希种子技术，每个节点默认生成160个虚拟节点（40*4）
 *
 * @author Spud
 * @date 2025/3/23
 */
@Slf4j
public class ConsistentHash<T> {

  /**
   * 节点名称格式 - 用于生成虚拟节点的名称
   * 格式：{节点名称}#{虚拟节点序号}@哈希种子
   */
  private static final String NODE_NAME_PATTERN = "%s#%d@%d";

  /**
   * 默认虚拟节点数量 - 每个实际节点生成的虚拟节点数量
   */
  private static final int COUNT_PER_NODE = 160;

  /**
   * 虚拟节点数 - 每个哈希种子生成的虚拟节点数量
   */
  private static final int COUNT_PER_HASH = 40;

  /**
   * 哈希种子数量 - 每个实际节点生成的哈希种子数量
   */
  private static final int SEED_PER_NODE = 4;

  /**
   * 哈希环 - 有序映射结构 (非并发安全，通过读写锁保证)
   */
  private final SortedMap<Long, T> hashRing = new TreeMap<>();

  /**
   * 节点哈希映射 - 用于快速查找节点
   */
  private final Map<T, Set<Long>> nodeToHashes = new HashMap<>();

  /**
   * 节点集合 - 用于快速查找节点是否存在
   */
  private final Set<T> nodes = ConcurrentHashMap.newKeySet();

  /**
   * 节点权重映射
   */
  private final Map<T, Integer> nodeWeights = new HashMap<>();

  /**
   * 虚拟节点数量
   */
  private final int countPerHash;

  /**
   * 每个种子生成的虚拟节点数量
   */
  private final int seedPerNode;

  /**
   * 哈希种子数量
   */
  private final int seedCount;

  /**
   * 哈希函数
   */
  private final HashFunction hashFunction;

  /**
   * 读写锁 - 保证线程安全
   */
  private final ReadWriteLock lock = new ReentrantReadWriteLock();
  private final Lock readLock = lock.readLock();
  private final Lock writeLock = lock.writeLock();

  /**
   * 使用默认配置创建一致性哈希实例
   */
  public ConsistentHash() {
    this(COUNT_PER_HASH, new Crc32Hash());
  }

  /**
   * 使用指定的虚拟节点数量创建一致性哈希实例
   *
   * @param countPerHash 虚拟节点数量
   */
  public ConsistentHash(int countPerHash) {
    this(countPerHash, new Crc32Hash());
  }

  /**
   * 使用指定的虚拟节点数量和哈希函数创建一致性哈希实例
   *
   * @param countPerHash 虚拟节点数量
   * @param hashFunction     哈希函数
   */
  public ConsistentHash(int countPerHash, HashFunction hashFunction) {
    this(countPerHash, hashFunction, SEED_PER_NODE);
  }

  /**
   * 使用指定的虚拟节点数量、哈希函数和每个种子点数创建一致性哈希实例
   *
   * @param countPerHash 虚拟节点数量
   * @param hashFunction     哈希函数
   * @param seedPerNode    每个哈希种子生成的虚拟节点数量
   */
  public ConsistentHash(int countPerHash, HashFunction hashFunction, int seedPerNode) {
    this.countPerHash = countPerHash;
    this.hashFunction = hashFunction;
    this.seedPerNode = seedPerNode;
    this.seedCount = (int) Math.ceil((double) countPerHash / seedPerNode);
  }

  /**
   * 添加节点
   *
   * @param node 节点
   */
  public void addNode(T node) {
    addNode(node, 1);
  }

  /**
   * 添加带权重的节点
   *
   * @param node   节点
   * @param weight 权重（影响虚拟节点数量）
   */
  public void addNode(T node, int weight) {
    validateNode(node, weight);

    try {
      writeLock.lock();

      if (nodes.contains(node)) {
        log.debug("Node {} already exists, updating weight from {} to {}", node,
            nodeWeights.getOrDefault(node, 1), weight);
        // 如果节点已存在，先移除再添加以更新权重
        doRemoveNode(node);
      }

      nodes.add(node);
      nodeWeights.put(node, weight);

      addVirtualNodes(node);

      log.debug("Added node {} with weight {}, total virtual nodes: {}", node, weight,
          hashRing.size());
    } finally {
      writeLock.unlock();
    }
  }

  private void validateNode(T node, int weight) {
    if (node == null) {
      throw new IllegalArgumentException("Node cannot be null");
    }
    if (weight <= 0) {
      throw new IllegalArgumentException("Weight must be positive: " + weight);
    }
  }

  protected String getVirtualNodeName(T node, int sequence, int seed) {
    return String.format(NODE_NAME_PATTERN, node, sequence, seed);
  }

  /**
   * 移除节点
   *
   * @param node 节点
   */
  public void removeNode(T node) {
    if (node == null || !nodes.contains(node)) {
      return;
    }

    try {
      writeLock.lock();
      doRemoveNode(node);
    } finally {
      writeLock.unlock();
    }
  }

  /**
   * 执行节点删除操作 (内部方法，需要在写锁保护下调用)
   */
  private void doRemoveNode(T node) {
    if (!nodes.contains(node)) {
      return;
    }

    nodes.remove(node);
    int weight = nodeWeights.remove(node);

    Set<Long> hashes = nodeToHashes.remove(node);
    if (hashes != null) {
      hashes.forEach(hashRing::remove);
      log.debug("Removed node {} with weight {}, total virtual nodes removed: {}", node, weight,
          hashRing.size());
    }

  }

  /**
   * 动态更新节点权重
   * @param node 目标节点
   * @param newWeight 新权重（必须大于0）
   */
  public void updateWeight(T node, int newWeight) {
    validateNode(node, newWeight);

    writeLock.lock();
    try {
      if (!nodes.contains(node)) {
        throw new IllegalArgumentException("Node not exists: " + node);
      }

      int oldWeight = nodeWeights.get(node);
      if (oldWeight == newWeight) return;

      // 计算需要增减的虚拟节点数
      int delta = newWeight - oldWeight;
      if (delta > 0) {
        expandVirtualNodes(node, delta);
      } else {
        shrinkVirtualNodes(node, -delta * seedPerNode * seedCount);
      }

      nodeWeights.put(node, newWeight);
      log.info("Updated node {} weight: {} -> {}", node, oldWeight, newWeight);
    } finally {
      writeLock.unlock();
    }
  }

  /**
   * 扩展节点的虚拟节点（内部方法）
   * @param node 目标节点
   * @param delta 需要增加的权重值
   */
  private void expandVirtualNodes(T node, int delta) {
    Set<Long> hashes = nodeToHashes.get(node);
    // 从当前权重开始扩展
    int startSeq = seedPerNode * nodeWeights.get(node);

    for (int s = 0; s < seedCount; s++) {
      for (int i = 0; i < seedPerNode * delta; i++) {
        String vnode = getVirtualNodeName(node, startSeq + i, s);
        long hash = hashFunction.hash(vnode);
        if (hashRing.putIfAbsent(hash, node) == null) {
          hashes.add(hash);
        }
      }
    }
  }

  /**
   * 收缩节点的虚拟节点（内部方法）
   * @param node 目标节点
   * @param removeCount 需要移除的虚拟节点数量
   */
  private void shrinkVirtualNodes(T node, int removeCount) {
    Set<Long> hashes = nodeToHashes.get(node);
    if (hashes == null || hashes.isEmpty()) return;

    // 按哈希值排序后均匀移除（保持分布均衡）
    List<Long> sortedHashes = new ArrayList<>(hashes);
    Collections.sort(sortedHashes);

    int step = Math.max(1, sortedHashes.size() / removeCount);
    Iterator<Long> iterator = sortedHashes.iterator();

    int removed = 0;
    while (iterator.hasNext() && removed < removeCount) {
      Long hash = iterator.next();
      // 均匀采样移除
      if ((hash % step) == 0) {
        hashRing.remove(hash);
        iterator.remove();
        removed++;
      }
    }

    log.debug("Shrink {} virtual nodes from {}", removed, node);
  }



  /**
   * 获取负责指定键的节点
   *
   * @param key 键
   * @return 负责该键的节点，如果环为空则返回null
   */
  public T getNode(String key) {
    try {
      readLock.lock();

      if (hashRing.isEmpty()) {
        return null;
      }

      long hash = hashFunction.hash(key);

      // 找到大于等于当前哈希值的第一个节点
      SortedMap<Long, T> tailMap = hashRing.tailMap(hash);
      Long nodeHash = tailMap.isEmpty() ? hashRing.firstKey() : tailMap.firstKey();

      return hashRing.get(nodeHash);
    } finally {
      readLock.unlock();
    }
  }

  /**
   * 获取负责指定键的节点
   *
   * @param key 键对象
   * @return 负责该键的节点，如果环为空则返回null
   */
  public T getNode(Object key) {
    return getNode(String.valueOf(key));
  }

  /**
   * 获取指定键的前N个节点（可用于副本存储）
   *
   * @param key   键
   * @param count 需要的节点数量
   * @return 节点列表
   */
  public Set<T> getNodes(String key, int count) {
    if (count <= 0) {
      throw new IllegalArgumentException("Count must be positive");
    }

    Set<T> result = ConcurrentHashMap.newKeySet();

    try {
      readLock.lock();

      if (hashRing.isEmpty() || count > nodes.size()) {
        // 返回所有可用节点
        return Set.copyOf(nodes);
      }

      long hash = hashFunction.hash(key);
      SortedMap<Long, T> tailMap = new TreeMap<>(hashRing.tailMap(hash));
      tailMap.putAll(hashRing.headMap(hash));

      // 选择不重复的前N个节点
      for (Map.Entry<Long, T> entry : tailMap.entrySet()) {
        T node = entry.getValue();
        result.add(node);
        if (result.size() >= count) {
          break;
        }
      }

      return Set.copyOf(result);
    } finally {
      readLock.unlock();
    }
  }

  /**
   * 判断指定节点是否负责某个键
   *
   * @param node 节点
   * @param key  键
   * @return 如果该节点负责该键则返回true，否则返回false
   */
  public boolean isNodeResponsible(T node, String key) {
    T responsibleNode = getNode(key);
    return node.equals(responsibleNode);
  }

  /**
   * 判断指定节点是否负责某个键
   *
   * @param node 节点
   * @param key  键对象
   * @return 如果该节点负责该键则返回true，否则返回false
   */
  public boolean isNodeResponsible(T node, Object key) {
    return isNodeResponsible(node, String.valueOf(key));
  }

  /**
   * 获取所有节点
   *
   * @return 所有节点的集合
   */
  public Set<T> getNodes() {
    return Set.copyOf(nodes);
  }

  /**
   * 获取节点数量
   *
   * @return 节点数量
   */
  public int getNodeCount() {
    return nodes.size();
  }

  /**
   * 清空哈希环
   */
  public void clear() {
    try {
      writeLock.lock();
      hashRing.clear();
      nodes.clear();
      nodeWeights.clear();
    } finally {
      writeLock.unlock();
    }
  }

  /**
   * 批量添加节点
   *
   * @param nodes 节点集合
   */
  public void addNodes(Collection<T> nodes) {
    if (nodes == null || nodes.isEmpty()) {
      return;
    }

    try {
      writeLock.lock();
      for (T node : nodes) {
        // 直接在锁内部添加，避免多次获取锁
        if (node == null) {
          continue;
        }

        if (this.nodes.contains(node)) {
          // 如果节点已存在，先移除再添加
          doRemoveNode(node);
        }

        this.nodes.add(node);
        nodeWeights.put(node, 1);

        addVirtualNodes(node);
      }
    } finally {
      writeLock.unlock();
    }
  }

  /**
   * 批量添加带权重的节点
   *
   * @param nodeWeights 节点权重映射
   */
  public void addWeightedNodes(Map<T, Integer> nodeWeights) {
    if (nodeWeights == null || nodeWeights.isEmpty()) {
      return;
    }

    try {
      writeLock.lock();
      for (Map.Entry<T, Integer> entry : nodeWeights.entrySet()) {
        T node = entry.getKey();
        int weight = entry.getValue();

        if (node == null || weight <= 0) {
          continue;
        }

        if (this.nodes.contains(node)) {
          // 如果节点已存在，先移除再添加
          doRemoveNode(node);
        }

        this.nodes.add(node);
        this.nodeWeights.put(node, weight);

        addVirtualNodes(node);
      }
    } finally {
      writeLock.unlock();
    }
  }

  private void addVirtualNodes(T node) {
    int weight = nodeWeights.get(node);
    Set<Long> hashes = nodeToHashes.computeIfAbsent(node, k -> ConcurrentHashMap.newKeySet());

    for (int seed = 0; seed < seedCount; seed++) {
      // 权重影响实际生成数量
      for (int i = 0; i < seedPerNode * weight; i++) {
        String vnode = getVirtualNodeName(node, i, seed);
        long hash = hashFunction.hash(vnode);
        if (hashRing.putIfAbsent(hash, node) == null) {
          hashes.add(hash);
        }
      }
    }
  }

  /**
   * 计算负载均衡度量
   * 通过计算各节点虚拟节点分布的相对标准差来衡量
   *
   * @return 负载不平衡指数（0表示完全平衡，越大越不平衡）
   */
  public double calculateBalanceMetric() {
    try {
      readLock.lock();

      if (nodes.size() <= 1) {
        return 0.0; // 单节点或空环视为平衡
      }

      // 统计每个实际节点的虚拟节点数量
      Map<T, Integer> nodeCounts = new ConcurrentHashMap<>();
      for (T node : nodes) {
        nodeCounts.put(node, 0);
      }

      // 计数
      for (T node : hashRing.values()) {
        nodeCounts.put(node, nodeCounts.getOrDefault(node, 0) + 1);
      }

      // 计算均值
      double mean = nodeCounts.values().stream().mapToInt(Integer::intValue).average().orElse(0);

      // 计算方差
      double variance = nodeCounts.values().stream().mapToDouble(count -> Math.pow(count - mean, 2))
          .average().orElse(0);

      // 计算标准差
      double stdDev = Math.sqrt(variance);

      // 计算相对标准差（变异系数）
      return (mean > 0) ? (stdDev / mean) : 0;
    } finally {
      readLock.unlock();
    }
  }

  /**
   * 判断哈希环是否平衡
   *
   * @param threshold 平衡阈值，相对标准差小于此值视为平衡
   * @return 如果平衡则返回true，否则返回false
   */
  public boolean isBalanced(double threshold) {
    if (threshold < 0) {
      throw new IllegalArgumentException("Threshold must be non-negative");
    }
    return calculateBalanceMetric() < threshold;
  }

  /**
   * 判断哈希环是否平衡，使用默认阈值0.1
   *
   * @return 如果平衡则返回true，否则返回false
   */
  public boolean isBalanced() {
    return isBalanced(0.1);
  }

  /**
   * 哈希函数接口
   */
  public interface HashFunction {

    long hash(String key);
  }

  /**
   * CRC32哈希函数实现，参照Nginx的实现，性能更优
   */
  public static class Crc32Hash implements HashFunction {

    private final ThreadLocal<CRC32> crc32 = ThreadLocal.withInitial(CRC32::new);

    @Override
    public long hash(String key) {
      CRC32 crc = crc32.get();
      crc.reset();
      crc.update(key.getBytes(StandardCharsets.UTF_8));
      return crc.getValue();
    }
  }

  /**
   * MD5哈希函数实现
   */
  public static class MD5Hash implements HashFunction {

    private final ThreadLocal<MessageDigest> md5 = ThreadLocal.withInitial(() -> {
      try {
        return MessageDigest.getInstance("MD5");
      } catch (NoSuchAlgorithmException e) {
        throw new RuntimeException("MD5 not supported", e);
      }
    });

    @Override
    public long hash(String key) {
      MessageDigest digest = md5.get();
      digest.reset();
      digest.update(key.getBytes(StandardCharsets.UTF_8));
      byte[] bytes = digest.digest();

      return ((long) (bytes[3] & 0xFF) << 24) | ((long) (bytes[2] & 0xFF) << 16) | (
          (long) (bytes[1] & 0xFF) << 8) | ((long) (bytes[0] & 0xFF));
    }
  }

  /**
   * Ketama哈希算法实现，兼容Memcached的一致性哈希
   */
  public static class KetamaHash implements HashFunction {

    private final ThreadLocal<MessageDigest> md5 = ThreadLocal.withInitial(() -> {
      try {
        return MessageDigest.getInstance("MD5");
      } catch (NoSuchAlgorithmException e) {
        throw new RuntimeException("MD5 not supported", e);
      }
    });

    @Override
    public long hash(String key) {
      MessageDigest digest = md5.get();
      digest.reset();
      digest.update(key.getBytes(StandardCharsets.UTF_8));
      byte[] bytes = digest.digest();

      // Ketama风格的哈希值提取，使用每4字节为一组生成哈希值
      return ((long) (bytes[3] & 0xFF) << 24) | ((long) (bytes[2] & 0xFF) << 16) | (
          (long) (bytes[1] & 0xFF) << 8) | ((long) (bytes[0] & 0xFF)) & 0xFFFFFFFFL;
    }
  }
}