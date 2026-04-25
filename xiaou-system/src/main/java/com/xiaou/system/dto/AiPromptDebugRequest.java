package com.xiaou.system.dto;

import lombok.Data;

/**
 * Prompt 在线调试请求。
 *
 * @author xiaou
 */
@Data
public class AiPromptDebugRequest {

    /**
     * Prompt 唯一标识，格式为 key:version。
     */
    private String promptId;

    /**
     * 变量 JSON 对象字符串。
     */
    private String variablesJson;

    /**
     * 是否真实执行模型调用。
     */
    private Boolean execute;
}
