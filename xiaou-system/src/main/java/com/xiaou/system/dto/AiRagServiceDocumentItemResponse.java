package com.xiaou.system.dto;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * RAG 服务文档列表项。
 *
 * @author xiaou
 */
@Data
public class AiRagServiceDocumentItemResponse {

    /**
     * 文档 ID。
     */
    private String id;

    /**
     * 场景。
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
