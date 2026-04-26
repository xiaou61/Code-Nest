package com.xiaou.ai.rag;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * LlamaIndex 文档列表项。
 *
 * @author xiaou
 */
@Data
@Accessors(chain = true)
public class LlamaIndexDocumentListItem {

    /**
     * 文档 ID。
     */
    private String id;

    /**
     * 场景名称。
     */
    private String scene;

    /**
     * 文本预览。
     */
    private String textPreview;

    /**
     * 元数据。
     */
    private Map<String, Object> metadata = new LinkedHashMap<>();
}
