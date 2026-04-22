package com.xiaou.ai.rag;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/**
 * LlamaIndex 批量删除文档响应。
 *
 * @author xiaou
 */
@Data
@Accessors(chain = true)
public class LlamaIndexDocumentBatchDeleteResponse {

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
}
