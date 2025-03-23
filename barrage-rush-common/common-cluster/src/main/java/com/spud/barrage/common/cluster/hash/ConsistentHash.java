package com.spud.barrage.common.cluster.hash;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;

/**
 * 一致性哈希算法的通用实现
 * 支持虚拟节点、节点权重和自定义哈希函数
 * 
 * @author Spud
 * @date 2025/3/23
 */
@Slf4j
public class ConsistentHash<T> {

    /**
     * 哈希函数接口
     */
    public interface HashFunction {
        long hash(String key);
    }

    /**
     * 默认虚拟节点数量
     */
    public static final int DEFAULT_VIRTUAL_NODE_COUNT = 160;

    /**
     * MD5哈希函数实现
     */
    public static class MD5Hash implements HashFunction {
        @Override
        public long hash(String key) {
            MessageDigest md5;
            try {
                md5 = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("MD5 not supported", e);
            }

            md5.reset();
            md5.update(key.getBytes(StandardCharsets.UTF_8));
            byte[] digest = md5.digest();

            return ((long) (digest[3] & 0xFF) << 24)
                    | ((long) (digest[2] & 0xFF) << 16)
                    | ((long) (digest[1] & 0xFF) << 8)
                    | ((long) (digest[0] & 0xFF));
        }
    }

    /**
     * 哈希环 - 有序映射结构
     */
    private final TreeMap<Long, T> hashRing = new TreeMap<>();

    /**
     * 节点集合 - 用于快速查找节点是否存在
     */
    private final Set<T> nodes = ConcurrentHashMap.newKeySet();

    /**
     * 节点权重映射
     */
    private final Map<T, Integer> nodeWeights = new ConcurrentHashMap<>();

    /**
     * 虚拟节点数量
     */
    private final int virtualNodeCount;

    /**
     * 哈希函数
     */
    private final HashFunction hashFunction;

    /**
     * 使用默认配置创建一致性哈希实例
     */
    public ConsistentHash() {
        this(DEFAULT_VIRTUAL_NODE_COUNT, new MD5Hash());
    }

    /**
     * 使用指定的虚拟节点数量创建一致性哈希实例
     * 
     * @param virtualNodeCount 虚拟节点数量
     */
    public ConsistentHash(int virtualNodeCount) {
        this(virtualNodeCount, new MD5Hash());
    }

    /**
     * 使用指定的虚拟节点数量和哈希函数创建一致性哈希实例
     * 
     * @param virtualNodeCount 虚拟节点数量
     * @param hashFunction     哈希函数
     */
    public ConsistentHash(int virtualNodeCount, HashFunction hashFunction) {
        this.virtualNodeCount = virtualNodeCount;
        this.hashFunction = hashFunction;
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
        if (node == null) {
            throw new IllegalArgumentException("Node cannot be null");
        }

        if (weight <= 0) {
            throw new IllegalArgumentException("Weight must be positive");
        }

        if (nodes.contains(node)) {
            log.debug("Node {} already exists, updating weight from {} to {}",
                    node, nodeWeights.getOrDefault(node, 1), weight);
            // 如果节点已存在，先移除再添加以更新权重
            removeNode(node);
        }

        nodes.add(node);
        nodeWeights.put(node, weight);

        // 添加虚拟节点
        int actualVirtualNodes = virtualNodeCount * weight;
        for (int i = 0; i < actualVirtualNodes; i++) {
            String virtualNodeName = getVirtualNodeName(node, i);
            long hash = hashFunction.hash(virtualNodeName);
            hashRing.put(hash, node);
        }

        log.debug("Added node {} with weight {}, total virtual nodes: {}",
                node, weight, actualVirtualNodes);
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

        nodes.remove(node);
        int weight = nodeWeights.remove(node);
        int actualVirtualNodes = virtualNodeCount * weight;

        // 移除虚拟节点
        for (int i = 0; i < actualVirtualNodes; i++) {
            String virtualNodeName = getVirtualNodeName(node, i);
            long hash = hashFunction.hash(virtualNodeName);
            hashRing.remove(hash);
        }

        log.debug("Removed node {} with weight {}, total virtual nodes removed: {}",
                node, weight, actualVirtualNodes);
    }

    /**
     * 获取负责指定键的节点
     * 
     * @param key 键
     * @return 负责该键的节点，如果环为空则返回null
     */
    public T getNode(String key) {
        if (hashRing.isEmpty()) {
            return null;
        }

        long hash = hashFunction.hash(key);

        // 找到大于等于当前哈希值的第一个节点
        Map.Entry<Long, T> entry = hashRing.ceilingEntry(hash);

        // 如果没有更大的，则取第一个节点（环形结构）
        if (entry == null) {
            entry = hashRing.firstEntry();
        }

        return entry.getValue();
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
     * 获取虚拟节点的名称
     * 
     * @param node  实际节点
     * @param index 虚拟节点索引
     * @return 虚拟节点名称
     */
    protected String getVirtualNodeName(T node, int index) {
        return node.toString() + "#" + index;
    }

    /**
     * 清空哈希环
     */
    public void clear() {
        hashRing.clear();
        nodes.clear();
        nodeWeights.clear();
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

        for (T node : nodes) {
            addNode(node);
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

        for (Map.Entry<T, Integer> entry : nodeWeights.entrySet()) {
            addNode(entry.getKey(), entry.getValue());
        }
    }

    /**
     * 计算负载均衡度量
     * 通过计算各节点虚拟节点分布的相对标准差来衡量
     * 
     * @return 负载不平衡指数（0表示完全平衡，越大越不平衡）
     */
    public double calculateBalanceMetric() {
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
        double mean = nodeCounts.values().stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0);

        // 计算方差
        double variance = nodeCounts.values().stream()
                .mapToDouble(count -> Math.pow(count - mean, 2))
                .average()
                .orElse(0);

        // 计算标准差
        double stdDev = Math.sqrt(variance);

        // 计算相对标准差（变异系数）
        return (mean > 0) ? (stdDev / mean) : 0;
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
}