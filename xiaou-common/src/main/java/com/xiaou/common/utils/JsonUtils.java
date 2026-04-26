package com.xiaou.common.utils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON工具类
 * 用于处理实体类中的JSON字段转换
 *
 * @author xiaou
 */
@Slf4j
public class JsonUtils {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 将字符串列表转换为JSON字符串
     *
     * @param list 字符串列表
     * @return JSON字符串
     */
    public static String listToJson(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            log.error("转换List到JSON失败", e);
            return null;
        }
    }
    
    /**
     * 将整数列表转换为JSON字符串
     *
     * @param list 整数列表
     * @return JSON字符串
     */
    public static String integerListToJson(List<Integer> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            log.error("转换Integer List到JSON失败", e);
            return null;
        }
    }
    
    /**
     * 将JSON字符串转换为字符串列表
     *
     * @param json JSON字符串
     * @return 字符串列表
     */
    public static List<String> jsonToStringList(String json) {
        if (!StringUtils.hasText(json)) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            log.error("转换JSON到String List失败: {}", json, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 将JSON字符串转换为整数列表
     *
     * @param json JSON字符串
     * @return 整数列表
     */
    public static List<Integer> jsonToIntegerList(String json) {
        if (!StringUtils.hasText(json)) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Integer>>() {});
        } catch (JsonProcessingException e) {
            log.error("转换JSON到Integer List失败: {}", json, e);
            return new ArrayList<>();
        }
    }
    
    // ==================== FastJson2扩展方法 ====================
    
    /**
     * 对象转JSON字符串（使用FastJson2）
     *
     * @param obj 对象
     * @return JSON字符串
     */
    public static String toJsonString(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return JSON.toJSONString(obj);
        } catch (Exception e) {
            log.error("对象转JSON失败", e);
            return null;
        }
    }
    
    /**
     * JSON字符串转对象（使用FastJson2）
     *
     * @param json JSON字符串
     * @param clazz 目标类型
     * @param <T> 泛型
     * @return 对象实例
     */
    public static <T> T parseObject(String json, Class<T> clazz) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return JSON.parseObject(json, clazz);
        } catch (Exception e) {
            log.error("JSON转对象失败: {}", json, e);
            return null;
        }
    }
    
    /**
     * JSON字符串转列表（使用FastJson2）
     *
     * @param json JSON字符串
     * @param clazz 元素类型
     * @param <T> 泛型
     * @return 列表
     */
    public static <T> List<T> parseArray(String json, Class<T> clazz) {
        if (!StringUtils.hasText(json)) {
            return new ArrayList<>();
        }
        try {
            return JSON.parseArray(json, clazz);
        } catch (Exception e) {
            log.error("JSON转列表失败: {}", json, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * JSON字符串转Map（使用FastJson2）
     *
     * @param json JSON字符串
     * @return Map对象
     */
    public static Map<String, Object> parseMap(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            JSONObject jsonObject = JSON.parseObject(json);
            return jsonObject == null ? null : new LinkedHashMap<>(jsonObject);
        } catch (Exception e) {
            log.error("JSON转Map失败: {}", json, e);
            return null;
        }
    }

    /**
     * JSON字符串转字符串Map（使用FastJson2）
     *
     * @param json JSON字符串
     * @return 字符串Map对象
     */
    public static Map<String, String> parseStringMap(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            JSONObject jsonObject = JSON.parseObject(json);
            if (jsonObject == null) {
                return null;
            }
            Map<String, String> result = new LinkedHashMap<>();
            jsonObject.forEach((key, value) -> result.put(key, value == null ? null : value.toString()));
            return result;
        } catch (Exception e) {
            log.error("JSON转String Map失败: {}", json, e);
            return null;
        }
    }
    
    /**
     * 对象转JSONObject（使用FastJson2）
     *
     * @param obj 对象
     * @return JSONObject
     */
    public static JSONObject toJSONObject(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            if (obj instanceof String) {
                return JSON.parseObject((String) obj);
            }
            return (JSONObject) JSON.toJSON(obj);
        } catch (Exception e) {
            log.error("对象转JSONObject失败", e);
            return null;
        }
    }
    
    /**
     * 判断字符串是否为有效的JSON
     *
     * @param json 字符串
     * @return 是否为有效JSON
     */
    public static boolean isValidJson(String json) {
        if (!StringUtils.hasText(json)) {
            return false;
        }
        try {
            JSON.parseObject(json);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
