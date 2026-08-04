package com.xiaou.web.growthcoach.intent;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 模型不可用时只解析明确表达的时间、岗位和面试优先级。
 */
@Component
public class DeterministicGrowthCoachIntentParser {

    private static final Pattern DURATION_PATTERN = Pattern.compile(
            "(?<!\\d)(\\d+(?:\\.\\d+)?)\\s*(小时|h|H|分钟|分)(?![\\p{L}])"
    );

    public GrowthCoachIntent parse(String message) {
        String normalized = message == null ? "" : message.trim();
        GrowthCoachIntent intent = new GrowthCoachIntent();
        intent.setAvailableMinutes(parseMinutes(normalized));
        intent.setTargetRole(parseRole(normalized));
        intent.setPrioritizeInterview(containsInterviewSignal(normalized));
        intent.setSummary(intent.getAvailableMinutes() == null
                ? "未识别明确的可投入时间"
                : "已按明确表达的约束解析");
        intent.setResolutionMode("DETERMINISTIC_FALLBACK");
        return intent;
    }

    private Integer parseMinutes(String message) {
        Matcher matcher = DURATION_PATTERN.matcher(message);
        if (!matcher.find()) {
            return null;
        }
        try {
            double value = Double.parseDouble(matcher.group(1));
            String unit = matcher.group(2).toLowerCase(Locale.ROOT);
            int minutes = (int) Math.round(unit.equals("小时") || unit.equals("h") ? value * 60 : value);
            return minutes > 0 ? minutes : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String parseRole(String message) {
        if (!StringUtils.hasText(message)) {
            return "";
        }
        String normalized = message.replaceAll("\\s+", " ");
        if (containsAny(normalized, "Java 后端", "Java后端")) {
            return "Java 后端";
        }
        if (containsAny(normalized, "后端开发", "后端岗位", "后端")) {
            return "后端开发";
        }
        if (containsAny(normalized, "前端开发", "前端岗位", "前端")) {
            return "前端开发";
        }
        if (containsAny(normalized, "全栈开发", "全栈")) {
            return "全栈开发";
        }
        if (containsAny(normalized, "算法工程师", "算法岗")) {
            return "算法工程师";
        }
        if (containsAny(normalized, "测试开发", "测试岗", "SDET")) {
            return "测试开发";
        }
        return "";
    }

    private boolean containsInterviewSignal(String message) {
        return containsAny(message, "面试", "笔试", "复试");
    }

    private boolean containsAny(String source, String... values) {
        if (!StringUtils.hasText(source)) {
            return false;
        }
        String normalized = source.toLowerCase(Locale.ROOT);
        for (String value : values) {
            if (normalized.contains(value.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
