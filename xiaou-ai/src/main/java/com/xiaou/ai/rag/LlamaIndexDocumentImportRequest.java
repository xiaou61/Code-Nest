package com.xiaou.ai.rag;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/**
 * LlamaIndex 文档导入请求。
 *
 * @author xiaou
 */
@Data
@Accessors(chain = true)
public class LlamaIndexDocumentImportRequest {

    /**
     * 待导入文档。
     */
    private List<LlamaIndexKnowledgeDocument> documents = new ArrayList<>();

    /**
     * 是否替换现有文档。
     */
    private boolean replace;
}
