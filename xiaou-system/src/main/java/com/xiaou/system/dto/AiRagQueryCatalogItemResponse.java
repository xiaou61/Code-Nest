package com.xiaou.system.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * RAG 查询模板清单项。
 *
 * @author xiaou
 */
@Data
public class AiRagQueryCatalogItemResponse {

    /**
     * 业务域。
     */
    private String domain;

    /**
     * 查询模板 Key。
     */
    private String key;

    /**
     * 查询模板版本。
     */
    private String version;

    /**
     * 查询模板唯一标识。
     */
    private String queryId;

    /**
     * 查询模板内容。
     */
    private String template;

    /**
     * 模板变量列表。
     */
    private List<String> templateVariables = new ArrayList<>();
}
