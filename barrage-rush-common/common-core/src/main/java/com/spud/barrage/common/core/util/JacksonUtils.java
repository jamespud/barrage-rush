package com.spud.barrage.common.core.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Jackson 对象转换工具类
 * 特性：
 * 1. 线程安全的单例 ObjectMapper
 * 2. 支持泛型对象的转换
 * 3. 自动处理 Java 8 时间类型
 * 4. 可配置的异常处理策略
 *
 * @author Spud
 * @date 2025/4/2
 */
public class JacksonUtils {

  // 单例 ObjectMapper（线程安全）
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  static {
    // 基础配置
    OBJECT_MAPPER
        // 反序列化时忽略未知字段（避免解析失败）
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        // 序列化时日期格式化为时间戳（可自定义）
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        // 注册 Java 8 时间类型支持模块
        .registerModule(new JavaTimeModule());
  }

  /**
   * 对象转 JSON 字符串
   * @param object 待转换对象
   * @return JSON 字符串，失败返回 null
   */
  public static String toJson(Object object) throws JsonProcessingException {
    return OBJECT_MAPPER.writeValueAsString(object);
  }

  /**
   * JSON 字符串转对象（普通类型）
   * @param json  JSON 字符串
   * @param clazz 目标类型 Class
   * @return 目标对象，失败返回 null
   */
  public static <T> T fromJson(String json, Class<T> clazz) throws JsonProcessingException {
    return OBJECT_MAPPER.readValue(json, clazz);
  }

  /**
   * JSON 字符串转对象（复杂泛型类型）
   * 示例：List<User> users = fromJson(json, new TypeReference<List<User>>(){});
   * @param json JSON 字符串
   * @param type 类型引用（TypeReference）
   * @return 目标对象，失败返回 null
   */
  public static <T> T fromJson(String json, TypeReference<T> type) throws JsonProcessingException {
    return OBJECT_MAPPER.readValue(json, type);
  }

  /**
   * 对象转对象（支持基础类型、POJO、集合的字段映射）
   * @param source 源对象
   * @param targetType 目标类型 Class
   * @return 转换后的目标对象，失败返回 null
   */
  public static <T> T convert(Object source, Class<T> targetType) {
    if (source == null) {
      return null;
    }
    return OBJECT_MAPPER.convertValue(source, targetType);
  }

  /**
   * 对象转对象（支持泛型类型）
   * 示例：List<DTO> list = convert(entityList, new TypeReference<List<DTO>>() {});
   * @param source 源对象
   * @param type 目标类型引用（TypeReference）
   * @return 转换后的目标对象，失败返回 null
   */
  public static <T> T convert(Object source, TypeReference<T> type) {
    if (source == null) {
      return null;
    }
    return OBJECT_MAPPER.convertValue(source, type);
  }

  /**
   * 深拷贝对象（通过序列化/反序列化实现）
   * @param object 原始对象
   * @param clazz  目标类型 Class
   * @return 拷贝后的新对象，失败返回 null
   */
  public static <T> T deepCopy(T object, Class<T> clazz) throws JsonProcessingException {
    return fromJson(toJson(object), clazz);
  }
}
