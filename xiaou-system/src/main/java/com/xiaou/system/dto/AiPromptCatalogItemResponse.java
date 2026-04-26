package com.xiaou.system.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Prompt 清单项。
 *
 * @author xiaou
 */
@Data
public class AiPromptCatalogItemResponse {

    /**
     * 业务域。
     */
    private String domain;

    /**
     * Prompt Key。
     */
    private String key;

    /**
     * Prompt 版本。
     */
    private String version;

    /**
     * Prompt 唯一标识。
     */
    private String promptId;

    /**
     * 系统 Prompt。
     */
    private String systemPrompt;

    /**
     * 用户模板。
     */
    private String userTemplate;

    /**
     * 模板变量列表。
     */
    private List<String> templateVariables = new ArrayList<>();
}
