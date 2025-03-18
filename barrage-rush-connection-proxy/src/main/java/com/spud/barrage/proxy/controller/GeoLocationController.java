package com.spud.barrage.proxy.controller;

import com.spud.barrage.proxy.service.GeoLocationService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/geo")
@RequiredArgsConstructor
public class GeoLocationController {

  private final GeoLocationService geoLocationService;

  @GetMapping("/lookup")
  public Map<String, Object> lookupIp(@RequestParam(required = false) String ip,
      HttpServletRequest request) {
    // 如果没有提供IP，使用请求的IP
    String ipAddress = ip;
    if (ipAddress == null || ipAddress.isEmpty()) {
      ipAddress = getClientIp(request);
    }

    Map<String, Object> result = new HashMap<>();
    result.put("ip", ipAddress);
    result.put("region", geoLocationService.getRegion(ipAddress));
    result.put("detail", geoLocationService.getLocationDetail(ipAddress));

    return result;
  }

  private String getClientIp(HttpServletRequest request) {
    String forwardedFor = request.getHeader("X-Forwarded-For");
    if (forwardedFor != null && !forwardedFor.isEmpty()) {
      return forwardedFor.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }
} 