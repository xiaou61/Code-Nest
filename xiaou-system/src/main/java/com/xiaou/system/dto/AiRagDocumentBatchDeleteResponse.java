package com.xiaou.system.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * RAG 文档批量删除响应。
 *
 * @author xiaou
 */
@Data
public class AiRagDocumentBatchDeleteResponse {

    /**
     * 服务地址。
     */
    private String endpoint;

    /**
     * 本次请求收到的文档 ID 数量。
     */
    private Integer requestedCount;

    /**
     * 本次成功删除的文档数量。
     */
    private Integer deletedCount;

    /**
     * 成功删除的文档 ID 列表。
     */
    private List<String> deletedDocumentIds = new ArrayList<>();

    /**
     * 未命中的文档数量。
     */
    private Integer missingCount;

    /**
     * 未命中的文档 ID 列表。
     */
    private List<String> missingDocumentIds = new ArrayList<>();

    /**
     * 删除后剩余文档总数。
     */
    private Integer totalCount;

    /**
     * 调用耗时。
     */
    private Long latencyMs;
}
