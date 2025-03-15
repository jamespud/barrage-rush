package com.spud.barrage.proxy.config;

/**
 * WebSocket服务器连接常量
 * 
 * @author Spud
 * @date 2025/3/13
 */
public class WSServerConnection {
    
    /**
     * WebSocket服务器连接数Redis键
     */
    public static final String WS_CONNECTION = "ws:server:%d:connections";
    
    /**
     * WebSocket服务器延迟Redis键
     */
    public static final String WS_LATENCY = "ws:server:%d:latency";
    
    /**
     * WebSocket服务器区域Redis键
     */
    public static final String WS_REGION = "ws:region:%s:servers";
    
    /**
     * WebSocket服务器健康状态Redis键
     */
    public static final String WS_HEALTH = "ws:server:%d:health";
    
    /**
     * WebSocket服务器信息Redis键
     */
    public static final String WS_INFO = "ws:server:%d:info";
    
    /**
     * WebSocket服务器URL模板
     */
    public static final String WS_URL_TEMPLATE = "ws://%s:%d/ws";
    
    /**
     * WebSocket服务器健康检查URL模板
     */
    public static final String WS_HEALTH_URL_TEMPLATE = "http://%s:%d/health";
    
    /**
     * 健康状态：正常
     */
    public static final String HEALTH_UP = "UP";
    
    /**
     * 健康状态：异常
     */
    public static final String HEALTH_DOWN = "DOWN";
    
    /**
     * 健康状态：未知
     */
    public static final String HEALTH_UNKNOWN = "UNKNOWN";
    
    /**
     * 服务器注册过期时间（秒）
     */
    public static final int SERVER_EXPIRE_SECONDS = 30;
}
