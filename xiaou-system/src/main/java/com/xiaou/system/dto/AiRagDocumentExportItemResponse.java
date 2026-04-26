package com.xiaou.system.dto;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * RAG 文档导出项。
 *
 * @author xiaou
 */
@Data
public class AiRagDocumentExportItemResponse {

    /**
     * 文档 ID。
     */
    private String id;

    /**
     * 场景。
     */
    private String scene;

    /**
     * 文档正文。
     */
    private String text;

    /**
     * 元数据。
     */
    private Map<String, Object> metadata = new LinkedHashMap<>();
}
