package com.xiaou.ai.util;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Coze响应解析工具
 * 统一处理Coze工作流返回的数据格式
 *
 * @author xiaou
 */
@Slf4j
public class CozeResponseParser {

    /**
     * 解析Coze工作流响应
     * Coze返回格式: {"output": "{...实际JSON...}"}
     * 需要先取出output字段，再二次解析
     *
     * @param response Coze原始响应
     * @return 解析后的JSONObject，解析失败返回null
     */
    public static JSONObject parse(String response) {
        if (StrUtil.isBlank(response)) {
            log.warn("Coze响应为空");
            return null;
        }

        try {
            JSONObject json = parseObject(response);
            if (json == null) {
                log.warn("Coze响应不是有效JSON对象: {}", response);
                return null;
            }

            // 兼容流式消息拼接场景：{}{"output":"..."}，优先拿包含output的对象
            if (json.isEmpty() && response.contains("\"output\"")) {
                JSONObject merged = parseBestObjectFromConcatenated(response);
                if (merged != null) {
                    json = merged;
                }
            }

            // 检查是否有output包装
            if (json.containsKey("output")) {
                String outputStr = json.getStr("output");
                if (StrUtil.isNotBlank(outputStr)) {
                    // 二次解析output内容（兼容代码块/杂质文本）
                    JSONObject outputJson = parseObject(outputStr);
                    if (outputJson != null) {
                        return outputJson;
                    }
                }
            }

            // 如果没有output包装，直接返回
            return json;

        } catch (Exception e) {
            log.error("解析Coze响应失败: {}", response, e);
            return null;
        }
    }

    /**
     * 尝试将任意文本解析为JSON对象
     * 兼容以下格式：
     * 1. 纯JSON对象
     * 2. ```json ... ``` 代码块
     * 3. 带前后说明文本的JSON片段
     */
    private static JSONObject parseObject(String text) {
        if (StrUtil.isBlank(text)) {
            return null;
        }

        String normalized = text.trim();
        normalized = normalized.replace("```json", "").replace("```", "").trim();

        // 先按完整内容解析
        try {
            return JSONUtil.parseObj(normalized);
        } catch (Exception ignored) {
            // ignore
        }

        // 再尝试抽取第一个JSON对象片段
        int start = normalized.indexOf('{');
        int end = normalized.lastIndexOf('}');
        if (start >= 0 && end > start) {
            String candidate = normalized.substring(start, end + 1);
            try {
                return JSONUtil.parseObj(candidate);
            } catch (Exception ignored) {
                // ignore
            }
        }

        return null;
    }

    /**
     * 从拼接的多个JSON对象中提取最佳对象：
     * 1) 优先包含output字段
     * 2) 其次返回字段更多的对象
     */
    private static JSONObject parseBestObjectFromConcatenated(String text) {
        List<String> candidates = splitTopLevelJsonObjects(text);
        if (candidates.isEmpty()) {
            return null;
        }

        JSONObject best = null;
        int bestSize = -1;
        for (String candidate : candidates) {
            JSONObject obj = parseObject(candidate);
            if (obj == null) {
                continue;
            }
            if (obj.containsKey("output")) {
                return obj;
            }
            if (obj.size() > bestSize) {
                best = obj;
                bestSize = obj.size();
            }
        }
        return best;
    }

    /**
     * 将字符串中顶层JSON对象切分出来，兼容{}{}拼接格式
     */
    private static List<String> splitTopLevelJsonObjects(String text) {
        List<String> result = new ArrayList<>();
        if (StrUtil.isBlank(text)) {
            return result;
        }

        String s = text.trim();
        int depth = 0;
        int start = -1;
        boolean inString = false;
        boolean escaped = false;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (inString) {
                if (escaped) {
                    escaped = false;
                    continue;
                }
                if (c == '\\') {
                    escaped = true;
                    continue;
                }
                if (c == '"') {
                    inString = false;
                }
                continue;
            }

            if (c == '"') {
                inString = true;
                continue;
            }

            if (c == '{') {
                if (depth == 0) {
                    start = i;
                }
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && start >= 0) {
                    result.add(s.substring(start, i + 1));
                    start = -1;
                }
            }
        }

        return result;
    }

    /**
     * 安全获取字符串字段
     *
     * @param json     JSON对象
     * @param key      字段名
     * @param fallback 默认值
     * @return 字段值或默认值
     */
    public static String getString(JSONObject json, String key, String fallback) {
        if (json == null) {
            return fallback;
        }
        String value = json.getStr(key);
        return StrUtil.isNotBlank(value) ? value : fallback;
    }

    /**
     * 安全获取整数字段
     *
     * @param json     JSON对象
     * @param key      字段名
     * @param fallback 默认值
     * @return 字段值或默认值
     */
    public static Integer getInt(JSONObject json, String key, Integer fallback) {
        if (json == null) {
            return fallback;
        }
        return json.getInt(key, fallback);
    }

    /**
     * 检查响应是否包含错误
     *
     * @param response Coze原始响应
     * @return 是否为错误响应
     */
    public static boolean isErrorResponse(String response) {
        if (StrUtil.isBlank(response)) {
            return true;
        }
        return response.contains("[ERROR]") || response.contains("Workflow not found");
    }
}
