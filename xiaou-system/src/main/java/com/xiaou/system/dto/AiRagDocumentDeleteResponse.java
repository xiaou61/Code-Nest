package com.xiaou.system.dto;

import lombok.Data;

/**
 * RAG 文档删除响应。
 *
 * @author xiaou
 */
@Data
public class AiRagDocumentDeleteResponse {

    /**
     * 服务地址。
     */
    private String endpoint;

    /**
     * 被删除的文档 ID。
     */
    private String documentId;

    /**
     * 本次删除数量。
     */
    private Integer deletedCount;

    /**
     * 删除后剩余文档总数。
     */
    private Integer totalCount;

    /**
     * 调用耗时。
     */
    private Long latencyMs;
}
