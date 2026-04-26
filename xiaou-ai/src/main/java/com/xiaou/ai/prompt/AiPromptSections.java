package com.xiaou.ai.prompt;

import org.springframework.util.StringUtils;

/**
 * Prompt 公共片段与变量规范工具。
 *
 * @author xiaou
 */
public final class AiPromptSections {

    private AiPromptSections() {
    }

    public static String text(String value) {
        return value == null ? "" : value.trim();
    }

    public static String text(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    public static int positiveInt(Integer value, int fallback) {
        return value == null || value <= 0 ? fallback : value;
    }

    public static String ragSection(String ragContext) {
        if (!StringUtils.hasText(ragContext)) {
            return "";
        }
        return """

                仅把下面的知识库片段当作补充参考；如果片段与当前任务无关，请忽略。
                优先采用 score 更高、matched_terms 更充分、且与当前问题直接相关的片段；不要机械复述 metadata 字段。
                <knowledge_context>
                %s
                </knowledge_context>
                """.formatted(ragContext.trim());
    }
}
