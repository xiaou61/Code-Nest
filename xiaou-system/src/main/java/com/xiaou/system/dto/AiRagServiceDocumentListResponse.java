package com.xiaou.system.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * RAG 服务文档列表响应。
 *
 * @author xiaou
 */
@Data
public class AiRagServiceDocumentListResponse {

    /**
     * 服务地址。
     */
    private String endpoint;

    /**
     * 场景筛选。
     */
    private String scene;

    /**
     * 查询上限。
     */
    private Integer limit;

    /**
     * 当前返回文档数。
     */
    private Integer totalCount;

    /**
     * 调用耗时。
     */
    private Long latencyMs;

    /**
     * 文档列表。
     */
    private List<AiRagServiceDocumentItemResponse> documents = new ArrayList<>();
}
