package com.xiaou.system.dto;

import lombok.Data;

/**
 * 自定义 RAG 文档导入响应。
 *
 * @author xiaou
 */
@Data
public class AiRagDocumentImportResponse {

    /**
     * 服务地址。
     */
    private String endpoint;

    /**
     * 是否替换导入。
     */
    private boolean replace;

    /**
     * 默认场景。
     */
    private String defaultScene;

    /**
     * 请求解析出的文档数。
     */
    private Integer requestedDocumentCount;

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
