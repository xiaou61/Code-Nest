package com.xiaou.ai.client;

import dev.langchain4j.model.output.TokenUsage;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 统一 AI 对话结果。
 *
 * @author xiaou
 */
@Data
@Accessors(chain = true)
public class AiChatResult {

    /**
     * 模型返回文本。
     */
    private String content;

    /**
     * 实际执行模型名。
     */
    private String modelName;

    /**
     * Token 使用情况。
     */
    private TokenUsage tokenUsage;
}
