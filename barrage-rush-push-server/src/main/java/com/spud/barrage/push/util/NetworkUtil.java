package com.spud.barrage.push.util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import lombok.extern.slf4j.Slf4j;

/**
 * 网络工具类
 * 提供网络相关的工具方法
 *
 * @author Spud
 * @date 2025/3/30
 */
@Slf4j
public class NetworkUtil {

  private NetworkUtil() {
    // 防止实例化
  }

  /**
   * 获取本地主机名
   *
   * @return 本地主机名
   */
  public static String getLocalHostname() {
    try {
      return InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      log.warn("无法获取本地主机名: {}", e.getMessage());
      return null;
    }
  }

  /**
   * 获取本地IP地址
   *
   * @return 本地IP地址
   */
  public static String getLocalIp() {
    try {
      InetAddress localHost = InetAddress.getLocalHost();
      if (!localHost.isLoopbackAddress()) {
        return localHost.getHostAddress();
      }

      // 尝试获取非回环地址
      Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
      while (networkInterfaces.hasMoreElements()) {
        NetworkInterface networkInterface = networkInterfaces.nextElement();
        if (networkInterface.isLoopback() || !networkInterface.isUp()) {
          continue;
        }

        Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
        while (addresses.hasMoreElements()) {
          InetAddress address = addresses.nextElement();
          if (!address.isLoopbackAddress() && address.isSiteLocalAddress()) {
            return address.getHostAddress();
          }
        }
      }

      // 如果找不到合适的地址，返回回环地址
      return "127.0.0.1";
    } catch (UnknownHostException | SocketException e) {
      log.warn("无法获取本地IP地址: {}", e.getMessage());
      return "127.0.0.1";
    }
  }
}