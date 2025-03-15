package com.spud.barrage.common.mq.util;

public class ShardingUtil {
    public static int getShardingIndex(String userId, int shardingCount) {
        return Math.abs(userId.hashCode() % shardingCount);
    }
} 