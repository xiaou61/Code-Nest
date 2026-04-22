package com.xiaou.system.dto;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * RAG 检索画像清单项。
 *
 * @author xiaou
 */
@Data
public class AiRetrievalProfileResponse {

    /**
     * 业务域。
     */
    private String domain;

    /**
     * 检索画像唯一标识。
     */
    private String profileId;

    /**
     * 绑定的查询模板 Key。
     */
    private String queryKey;

    /**
     * 绑定的查询模板版本。
     */
    private String queryVersion;

    /**
     * 绑定的查询模板唯一标识。
     */
    private String queryId;

    /**
     * LlamaIndex 检索场景。
     */
    private String scene;

    /**
     * 检索返回条数。
     */
    private int topK;

    /**
     * 元数据过滤条件。
     */
    private Map<String, Object> metadataFilters = new LinkedHashMap<>();
}
