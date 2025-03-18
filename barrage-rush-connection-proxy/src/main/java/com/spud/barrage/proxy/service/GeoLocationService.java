package com.spud.barrage.proxy.service;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.record.City;
import com.maxmind.geoip2.record.Country;
import com.maxmind.geoip2.record.Subdivision;
import com.spud.barrage.proxy.config.ProxyProperties;
import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author Spud
 * @date 2025/3/11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeoLocationService {

  private final ProxyProperties properties;
  // 中国主要城市与区域的映射
  private final Map<String, String> chinaRegionMap = new HashMap<>();
  // 中国主要城市与省份的映射（用于修正城市名称）
  private final Map<String, String> chinaCityProvinceMap = new HashMap<>();
  // 中国主要城市的中文名称映射
  private final Map<String, String> chinaCityChineseNameMap = new HashMap<>();
  private DatabaseReader geoIpReader;

  @PostConstruct
  public void init() {
    if (properties.getGeoLocation().isEnabled()) {
      try {
        File database = new File(properties.getGeoLocation().getDatabasePath());
        geoIpReader = new DatabaseReader.Builder(database).build();

        // 初始化中国区域映射
        initChinaRegionMap();

        // 初始化中国城市映射
        initChinaCityMaps();
      } catch (IOException e) {
        log.error("Failed to initialize GeoIP database: {}", e.getMessage());
      }
    }
  }

  /**
   * 初始化中国区域映射
   */
  private void initChinaRegionMap() {
    // 华北地区
    chinaRegionMap.put("北京", "NORTH");
    chinaRegionMap.put("天津", "NORTH");
    chinaRegionMap.put("河北", "NORTH");
    chinaRegionMap.put("山西", "NORTH");
    chinaRegionMap.put("内蒙古", "NORTH");

    // 华东地区
    chinaRegionMap.put("上海", "EAST");
    chinaRegionMap.put("江苏", "EAST");
    chinaRegionMap.put("浙江", "EAST");
    chinaRegionMap.put("安徽", "EAST");
    chinaRegionMap.put("福建", "EAST");
    chinaRegionMap.put("江西", "EAST");
    chinaRegionMap.put("山东", "EAST");

    // 华南地区
    chinaRegionMap.put("广东", "SOUTH");
    chinaRegionMap.put("广西", "SOUTH");
    chinaRegionMap.put("海南", "SOUTH");

    // 华中地区
    chinaRegionMap.put("河南", "CENTRAL");
    chinaRegionMap.put("湖北", "CENTRAL");
    chinaRegionMap.put("湖南", "CENTRAL");

    // 西南地区
    chinaRegionMap.put("重庆", "SOUTHWEST");
    chinaRegionMap.put("四川", "SOUTHWEST");
    chinaRegionMap.put("贵州", "SOUTHWEST");
    chinaRegionMap.put("云南", "SOUTHWEST");
    chinaRegionMap.put("西藏", "SOUTHWEST");

    // 西北地区
    chinaRegionMap.put("陕西", "NORTHWEST");
    chinaRegionMap.put("甘肃", "NORTHWEST");
    chinaRegionMap.put("青海", "NORTHWEST");
    chinaRegionMap.put("宁夏", "NORTHWEST");
    chinaRegionMap.put("新疆", "NORTHWEST");

    // 东北地区
    chinaRegionMap.put("辽宁", "NORTHEAST");
    chinaRegionMap.put("吉林", "NORTHEAST");
    chinaRegionMap.put("黑龙江", "NORTHEAST");

    // 特别行政区
    chinaRegionMap.put("香港", "SOUTH");
    chinaRegionMap.put("澳门", "SOUTH");
    chinaRegionMap.put("台湾", "EAST");
  }

  /**
   * 初始化中国城市映射
   */
  private void initChinaCityMaps() {
    // 直辖市
    addCityMapping("Beijing", "北京", "北京");
    addCityMapping("Shanghai", "上海", "上海");
    addCityMapping("Tianjin", "天津", "天津");
    addCityMapping("Chongqing", "重庆", "重庆");

    // 华北地区
    addCityMapping("Shijiazhuang", "石家庄", "河北");
    addCityMapping("Tangshan", "唐山", "河北");
    addCityMapping("Baoding", "保定", "河北");
    addCityMapping("Taiyuan", "太原", "山西");
    addCityMapping("Datong", "大同", "山西");
    addCityMapping("Hohhot", "呼和浩特", "内蒙古");
    addCityMapping("Baotou", "包头", "内蒙古");

    // 华东地区
    addCityMapping("Nanjing", "南京", "江苏");
    addCityMapping("Suzhou", "苏州", "江苏");
    addCityMapping("Wuxi", "无锡", "江苏");
    addCityMapping("Changzhou", "常州", "江苏");
    addCityMapping("Hangzhou", "杭州", "浙江");
    addCityMapping("Ningbo", "宁波", "浙江");
    addCityMapping("Wenzhou", "温州", "浙江");
    addCityMapping("Hefei", "合肥", "安徽");
    addCityMapping("Fuzhou", "福州", "福建");
    addCityMapping("Xiamen", "厦门", "福建");
    addCityMapping("Quanzhou", "泉州", "福建");
    addCityMapping("Nanchang", "南昌", "江西");
    addCityMapping("Jinan", "济南", "山东");
    addCityMapping("Qingdao", "青岛", "山东");
    addCityMapping("Yantai", "烟台", "山东");

    // 华南地区
    addCityMapping("Guangzhou", "广州", "广东");
    addCityMapping("Shenzhen", "深圳", "广东");
    addCityMapping("Dongguan", "东莞", "广东");
    addCityMapping("Foshan", "佛山", "广东");
    addCityMapping("Zhuhai", "珠海", "广东");
    addCityMapping("Nanning", "南宁", "广西");
    addCityMapping("Guilin", "桂林", "广西");
    addCityMapping("Haikou", "海口", "海南");
    addCityMapping("Sanya", "三亚", "海南");

    // 华中地区
    addCityMapping("Zhengzhou", "郑州", "河南");
    addCityMapping("Luoyang", "洛阳", "河南");
    addCityMapping("Wuhan", "武汉", "湖北");
    addCityMapping("Yichang", "宜昌", "湖北");
    addCityMapping("Changsha", "长沙", "湖南");
    addCityMapping("Zhuzhou", "株洲", "湖南");
    addCityMapping("Xiangtan", "湘潭", "湖南");

    // 西南地区
    addCityMapping("Chengdu", "成都", "四川");
    addCityMapping("Mianyang", "绵阳", "四川");
    addCityMapping("Guiyang", "贵阳", "贵州");
    addCityMapping("Zunyi", "遵义", "贵州");
    addCityMapping("Kunming", "昆明", "云南");
    addCityMapping("Dali", "大理", "云南");
    addCityMapping("Lijiang", "丽江", "云南");
    addCityMapping("Lhasa", "拉萨", "西藏");

    // 西北地区
    addCityMapping("Xi'an", "西安", "陕西");
    addCityMapping("Xianyang", "咸阳", "陕西");
    addCityMapping("Lanzhou", "兰州", "甘肃");
    addCityMapping("Xining", "西宁", "青海");
    addCityMapping("Yinchuan", "银川", "宁夏");
    addCityMapping("Urumqi", "乌鲁木齐", "新疆");
    addCityMapping("Kashgar", "喀什", "新疆");

    // 东北地区
    addCityMapping("Shenyang", "沈阳", "辽宁");
    addCityMapping("Dalian", "大连", "辽宁");
    addCityMapping("Changchun", "长春", "吉林");
    addCityMapping("Jilin", "吉林", "吉林");
    addCityMapping("Harbin", "哈尔滨", "黑龙江");
    addCityMapping("Qiqihar", "齐齐哈尔", "黑龙江");

    // 特别行政区
    addCityMapping("Hong Kong", "香港", "香港");
    addCityMapping("Macau", "澳门", "澳门");
    addCityMapping("Taipei", "台北", "台湾");
    addCityMapping("Kaohsiung", "高雄", "台湾");
    addCityMapping("Taichung", "台中", "台湾");
  }

  /**
   * 添加城市映射
   */
  private void addCityMapping(String englishName, String chineseName, String province) {
    chinaCityChineseNameMap.put(englishName, chineseName);
    chinaCityProvinceMap.put(englishName, province);
    chinaCityProvinceMap.put(chineseName, province);
  }

  /**
   * 获取IP地址对应的区域
   */
  public String getRegion(String ipAddress) {
    if (!properties.getGeoLocation().isEnabled() || geoIpReader == null) {
      return properties.getGeoLocation().getDefaultRegion();
    }

    try {
      InetAddress ip = InetAddress.getByName(ipAddress);
      CityResponse response = geoIpReader.city(ip);
      Country country = response.getCountry();

      // 如果是中国IP，返回具体区域
      if ("CN".equals(country.getIsoCode())) {
        return getChinaRegion(response);
      }

      // 非中国IP按照大洲分类
      return switch (country.getIsoCode()) {
        case "JP", "KR", "SG", "TH", "VN", "MY", "ID", "PH" -> "ASIA";
        case "US", "CA", "MX" -> "NA";
        case "GB", "DE", "FR", "IT", "ES", "NL", "SE", "CH" -> "EU";
        case "AU", "NZ" -> "OCEANIA";
        case "BR", "AR", "CL", "CO", "PE" -> "SA";
        default -> properties.getGeoLocation().getDefaultRegion();
      };
    } catch (Exception e) {
      log.error("Failed to get region for IP {}: {}", ipAddress, e.getMessage());
      return properties.getGeoLocation().getDefaultRegion();
    }
  }

  /**
   * 获取中国IP的具体区域
   */
  private String getChinaRegion(CityResponse response) {
    try {
      // 获取省份信息
      Subdivision subdivision = response.getMostSpecificSubdivision();
      String province = subdivision.getName();

      // 获取城市信息
      City city = response.getCity();
      String cityName = city.getName();

      // 尝试获取中文省份名称
      String chineseProvince = subdivision.getNames().get("zh-CN");
      if (chineseProvince != null) {
        province = chineseProvince;
      }

      // 尝试获取中文城市名称
      String chineseCity = city.getNames().get("zh-CN");
      if (chineseCity != null) {
        cityName = chineseCity;
      } else {
        // 如果没有中文名称，尝试从映射中获取
        String mappedChineseCity = chinaCityChineseNameMap.get(cityName);
        if (mappedChineseCity != null) {
          cityName = mappedChineseCity;
        }
      }

      // 记录日志
      log.info("中国IP定位: 省份={}, 城市={}", province, cityName);

      // 根据省份查找区域
      String region = chinaRegionMap.get(province);
      if (region != null) {
        return region;
      }

      // 如果找不到省份映射，尝试通过城市查找省份
      String cityProvince = chinaCityProvinceMap.get(cityName);
      if (cityProvince != null) {
        region = chinaRegionMap.get(cityProvince);
        if (region != null) {
          return region;
        }
      }

      // 如果找不到映射，根据经纬度判断
      double latitude = response.getLocation().getLatitude();
      if (latitude > 35) {
        return "NORTH"; // 北方
      } else {
        return "SOUTH"; // 南方
      }
    } catch (Exception e) {
      log.error("Failed to get China region: {}", e.getMessage());
      return "CENTRAL"; // 默认中部
    }
  }

  /**
   * 获取IP地址的详细位置信息
   */
  public Map<String, String> getLocationDetail(String ipAddress) {
    Map<String, String> result = new HashMap<>();
    result.put("country", "未知");
    result.put("province", "未知");
    result.put("city", "未知");

    if (!properties.getGeoLocation().isEnabled() || geoIpReader == null) {
      return result;
    }

    try {
      InetAddress ip = InetAddress.getByName(ipAddress);
      CityResponse response = geoIpReader.city(ip);

      // 国家
      Country country = response.getCountry();
      result.put("country", country.getNames().get("zh-CN") != null ?
          country.getNames().get("zh-CN") : country.getName());

      // 省份
      Subdivision subdivision = response.getMostSpecificSubdivision();
      String province = subdivision.getName();
      String chineseProvince = subdivision.getNames().get("zh-CN");
      if (chineseProvince != null) {
        province = chineseProvince;
      }
      result.put("province", province);

      // 城市
      City city = response.getCity();
      String cityName = city.getName();
      String chineseCity = city.getNames().get("zh-CN");

      if (chineseCity != null) {
        cityName = chineseCity;
      } else if ("CN".equals(country.getIsoCode())) {
        // 如果是中国城市但没有中文名称，尝试从映射中获取
        String mappedChineseCity = chinaCityChineseNameMap.get(cityName);
        if (mappedChineseCity != null) {
          cityName = mappedChineseCity;
        }

        // 如果没有找到城市，但有省份，尝试使用省会城市
        if (cityName == null || cityName.isEmpty()) {
          for (Map.Entry<String, String> entry : chinaCityProvinceMap.entrySet()) {
            if (entry.getValue().equals(province)) {
              cityName = entry.getKey();
              break;
            }
          }
        }
      }

      result.put("city", cityName != null ? cityName : "未知");

      // 经纬度
      result.put("latitude", String.valueOf(response.getLocation().getLatitude()));
      result.put("longitude", String.valueOf(response.getLocation().getLongitude()));

      // 如果是中国IP，添加区域信息
      if ("CN".equals(country.getIsoCode())) {
        String region = getChinaRegion(response);
        result.put("region", region);
      }

      return result;
    } catch (Exception e) {
      log.error("Failed to get location detail for IP {}: {}", ipAddress, e.getMessage());
      return result;
    }
  }

  /**
   * 获取中国城市名称（中文）
   */
  public String getChineseCityName(String englishName) {
    if (englishName == null) {
      return "未知";
    }

    String chineseName = chinaCityChineseNameMap.get(englishName);
    return chineseName != null ? chineseName : englishName;
  }

  /**
   * 获取城市所在省份
   */
  public String getCityProvince(String cityName) {
    if (cityName == null) {
      return "未知";
    }

    String province = chinaCityProvinceMap.get(cityName);
    return province != null ? province : "未知";
  }
} 