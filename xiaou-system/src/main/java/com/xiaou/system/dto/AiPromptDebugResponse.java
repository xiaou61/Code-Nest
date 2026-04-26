package com.xiaou.system.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Prompt 在线调试响应。
 *
 * @author xiaou
 */
@Data
public class AiPromptDebugResponse {

    /**
     * 业务域。
     */
    private String domain;

    /**
     * Prompt 唯一标识。
     */
    private String promptId;

    /**
     * Prompt Key。
     */
    private String promptKey;

    /**
     * Prompt 版本。
     */
    private String promptVersion;

    /**
     * 模板变量列表。
     */
    private List<String> templateVariables = new ArrayList<>();

    /**
     * 规范化后的变量 JSON。
     */
    private String variablesJson;

    /**
     * System Prompt。
     */
    private String systemPrompt;

    /**
     * 渲染后的 User Prompt。
     */
    private String renderedUserPrompt;

    /**
     * 是否已执行模型调用。
     */
    private boolean executed;

    /**
     * 是否绑定结构化输出契约。
     */
    private boolean structuredOutputBound;

    /**
     * 结构化输出根类型。
     */
    private String schemaRootType;

    /**
     * 结构化输出 Schema 标识。
     */
    private String schemaId;

    /**
     * 格式化后的 JSON Schema。
     */
    private String schemaJson;

    /**
     * 实际执行模型名。
     */
    private String modelName;

    /**
     * 模型原始返回。
     */
    private String rawResponse;

    /**
     * 格式化后的解析 JSON。
     */
    private String parsedResponseJson;

    /**
     * 结构化校验是否通过。
     */
    private Boolean structuredValid;

    /**
     * 结构化校验失败原因。
     */
    private String structuredValidationReason;

    /**
     * 输入 Token 数。
     */
    private Integer inputTokens;

    /**
     * 输出 Token 数。
     */
    private Integer outputTokens;

    /**
     * 总 Token 数。
     */
    private Integer totalTokens;
}
