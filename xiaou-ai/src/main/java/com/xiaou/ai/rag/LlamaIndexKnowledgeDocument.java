package com.xiaou.ai.rag;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * LlamaIndex 知识文档。
 *
 * @author xiaou
 */
@Data
@Accessors(chain = true)
public class LlamaIndexKnowledgeDocument {

    /**
     * 文档 ID。
     */
    private String id;

    /**
     * 文档文本。
     */
    private String text;

    /**
     * 场景名称。
     */
    private String scene;

    /**
     * 元数据。
     */
    private Map<String, Object> metadata = new LinkedHashMap<>();
}
