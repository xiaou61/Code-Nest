package com.xiaou.ai.rag;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * LlamaIndex 检索请求。
 *
 * @author xiaou
 */
@Data
@Accessors(chain = true)
public class LlamaIndexRetrieveRequest {

    /**
     * 检索查询文本
     */
    private String query;

    /**
     * 场景名称
     */
    private String scene;

    /**
     * 检索数量
     */
    private Integer topK;

    /**
     * 元数据过滤条件
     */
    private Map<String, Object> metadataFilters = new LinkedHashMap<>();
}
