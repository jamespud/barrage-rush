package com.spud.barrage.proxy.service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 区域服务
 * 负责基于IP地址确定客户端所在区域
 *
 * @author Spud
 * @date 2025/3/30
 */
@Slf4j
@Service
public class RegionService {

  @Value("${barrage.region.default:cn-east-1}")
  private String defaultRegion;

  @Value("${barrage.region.available:cn-east-1,cn-north-1,cn-south-1}")
  private String availableRegions;

  // IP段到区域的映射缓存
  private final Map<String, String> ipRangeCache = new ConcurrentHashMap<>();

  /**
   * 获取默认区域
   */
  public String getDefaultRegion() {
    return defaultRegion;
  }

  /**
   * 检查区域是否有效
   */
  public boolean isValidRegion(String region) {
    List<String> regions = getAvailableRegions();
    return regions.contains(region);
  }

  /**
   * 获取可用区域列表
   */
  public List<String> getAvailableRegions() {
    return Arrays.asList(availableRegions.split(","));
  }

  /**
   * 根据IP地址获取区域
   * 这里实现一个简单的IP地址到区域的映射
   * 实际应用中可能需要引入第三方IP地理位置库
   */
  public String getRegionByIp(String ip) {
    if (ip == null || ip.isEmpty()) {
      return defaultRegion;
    }

    // 获取IP地址的前缀（简化的网段匹配）
    String ipPrefix = getIpPrefix(ip);

    // 检查缓存
    if (ipRangeCache.containsKey(ipPrefix)) {
      return ipRangeCache.get(ipPrefix);
    }

    // 否则根据IP确定区域
    // 这是一个简化的实现，实际应用中应该使用专业的IP地理位置库
    String region = determineRegionByIp(ip);

    // 缓存结果
    ipRangeCache.put(ipPrefix, region);

    return region;
  }

  /**
   * 获取IP地址的前缀（简化的网段匹配）
   */
  private String getIpPrefix(String ip) {
    int lastDotIndex = ip.lastIndexOf('.');
    return lastDotIndex > 0 ? ip.substring(0, lastDotIndex) : ip;
  }

  /**
   * 根据IP确定区域的简单实现
   * 实际应用中应该使用专业的IP地理位置库
   */
  private String determineRegionByIp(String ip) {
    // 简单的IP区域映射
    // 这里只是示例，实际应用中应该使用专业的IP地理位置库

    // 判断IP范围
    if (ip.startsWith("10.") || ip.startsWith("192.168.")) {
      // 内部网络
      return defaultRegion;
    }

    // 根据IP的第一个字节做简单区分
    // 非常简化的示例，实际使用时需要使用专业的地理位置服务
    try {
      String[] octets = ip.split("\\.");
      int firstOctet = Integer.parseInt(octets[0]);

      // 简单示例：基于IP段做区域划分
      if (firstOctet < 100) {
        return "cn-east-1"; // 华东
      } else if (firstOctet < 150) {
        return "cn-north-1"; // 华北
      } else if (firstOctet < 200) {
        return "cn-south-1"; // 华南
      } else {
        return "global"; // 国际
      }
    } catch (Exception e) {
      log.warn("解析IP地址错误: {}", ip, e);
      return defaultRegion;
    }
  }
}