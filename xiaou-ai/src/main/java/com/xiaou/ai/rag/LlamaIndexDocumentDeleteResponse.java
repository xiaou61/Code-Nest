package com.xiaou.ai.rag;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * LlamaIndex 文档删除响应。
 *
 * @author xiaou
 */
@Data
@Accessors(chain = true)
public class LlamaIndexDocumentDeleteResponse {

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
}
