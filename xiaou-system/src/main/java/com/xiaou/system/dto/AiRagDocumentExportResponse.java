package com.xiaou.system.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * RAG 文档导出响应。
 *
 * @author xiaou
 */
@Data
public class AiRagDocumentExportResponse {

    /**
     * 服务地址。
     */
    private String endpoint;

    /**
     * 场景筛选。
     */
    private String scene;

    /**
     * 当前导出文档数。
     */
    private Integer totalCount;

    /**
     * 调用耗时。
     */
    private Long latencyMs;

    /**
     * 导出的文档列表。
     */
    private List<AiRagDocumentExportItemResponse> documents = new ArrayList<>();
}
