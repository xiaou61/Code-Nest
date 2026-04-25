package com.xiaou.system.dto;

import lombok.Data;

/**
 * RAG 样例知识导入响应。
 *
 * @author xiaou
 */
@Data
public class AiRagSampleImportResponse {

    /**
     * 服务地址。
     */
    private String endpoint;

    /**
     * 是否替换导入。
     */
    private boolean replace;

    /**
     * 样例文档数量。
     */
    private Integer sampleDocumentCount;

    /**
     * 本次导入数量。
     */
    private Integer importedCount;

    /**
     * 导入后总文档数。
     */
    private Integer totalCount;

    /**
     * 调用耗时。
     */
    private Long latencyMs;
}
