package com.xiaou.ai.rag;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/**
 * LlamaIndex 文档导出响应。
 *
 * @author xiaou
 */
@Data
@Accessors(chain = true)
public class LlamaIndexDocumentExportResponse {

    /**
     * 当前返回的文档总数。
     */
    private Integer totalCount;

    /**
     * 文档列表。
     */
    private List<LlamaIndexKnowledgeDocument> documents = new ArrayList<>();
}
