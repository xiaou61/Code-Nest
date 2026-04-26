package com.xiaou.ai.util;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 通用 AI JSON 响应解析工具。
 *
 * @author xiaou
 */
@Slf4j
public class AiJsonResponseParser {

    private AiJsonResponseParser() {
    }

    public static JSONObject parse(String response) {
        if (StrUtil.isBlank(response)) {
            log.warn("AI 响应为空");
            return null;
        }

        try {
            JSONObject json = parseObject(response);
            if (json == null) {
                log.warn("AI 响应不是有效 JSON 对象: {}", response);
                return null;
            }

            if (json.isEmpty() && response.contains("{")) {
                JSONObject merged = parseBestObjectFromConcatenated(response);
                if (merged != null) {
                    json = merged;
                }
            }

            if (json.containsKey("output")) {
                Object output = json.get("output");
                if (output instanceof String outputStr && StrUtil.isNotBlank(outputStr)) {
                    JSONObject outputJson = parseObject(outputStr);
                    if (outputJson != null) {
                        return outputJson;
                    }
                }
            }

            return json;
        } catch (Exception e) {
            log.error("解析 AI 响应失败: {}", response, e);
            return null;
        }
    }

    private static JSONObject parseObject(String text) {
        if (StrUtil.isBlank(text)) {
            return null;
        }

        String normalized = text.trim()
                .replace("```json", "")
                .replace("```", "")
                .trim();

        try {
            return JSONUtil.parseObj(normalized);
        } catch (Exception ignored) {
            // ignore
        }

        int start = normalized.indexOf('{');
        int end = normalized.lastIndexOf('}');
        if (start >= 0 && end > start) {
            try {
                return JSONUtil.parseObj(normalized.substring(start, end + 1));
            } catch (Exception ignored) {
                // ignore
            }
        }

        return null;
    }

    private static JSONObject parseBestObjectFromConcatenated(String text) {
        List<String> candidates = splitTopLevelJsonObjects(text);
        JSONObject best = null;
        int bestSize = -1;

        for (String candidate : candidates) {
            JSONObject object = parseObject(candidate);
            if (object == null) {
                continue;
            }
            if (object.containsKey("output")) {
                return object;
            }
            if (object.size() > bestSize) {
                best = object;
                bestSize = object.size();
            }
        }

        return best;
    }

    private static List<String> splitTopLevelJsonObjects(String text) {
        List<String> result = new ArrayList<>();
        if (StrUtil.isBlank(text)) {
            return result;
        }

        String source = text.trim();
        int depth = 0;
        int start = -1;
        boolean inString = false;
        boolean escaped = false;

        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);

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
                    result.add(source.substring(start, i + 1));
                    start = -1;
                }
            }
        }

        return result;
    }

    public static String getString(JSONObject json, String key, String fallback) {
        if (json == null) {
            return fallback;
        }
        String value = json.getStr(key);
        return StrUtil.isNotBlank(value) ? value : fallback;
    }

    public static Integer getInt(JSONObject json, String key, Integer fallback) {
        if (json == null) {
            return fallback;
        }
        return json.getInt(key, fallback);
    }

    public static boolean isErrorResponse(String response) {
        if (StrUtil.isBlank(response)) {
            return true;
        }
        return response.contains("[ERROR]")
                || response.contains("Workflow not found")
                || response.contains("invalid_request")
                || response.contains("\"error\"");
    }
}
