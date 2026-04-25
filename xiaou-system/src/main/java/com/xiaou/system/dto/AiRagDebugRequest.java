package com.xiaou.system.dto;

import lombok.Data;

/**
 * LlamaIndex 检索调试请求。
 *
 * @author xiaou
 */
@Data
public class AiRagDebugRequest {

    /**
     * 检索画像标识，可选。
     */
    private String profileId;

    /**
     * 查询文本。
     */
    private String query;

    /**
     * 检索场景。
     */
    private String scene;

    /**
     * 检索返回条数。
     */
    private Integer topK;

    /**
     * 元数据过滤 JSON 对象字符串。
     */
    private String metadataFiltersJson;
}
