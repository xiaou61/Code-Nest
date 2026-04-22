package com.xiaou.system.dto;

import lombok.Data;

/**
 * 自定义 RAG 文档导入请求。
 *
 * @author xiaou
 */
@Data
public class AiRagDocumentImportRequest {

    /**
     * 是否替换现有文档。
     */
    private Boolean replace;

    /**
     * 默认场景。
     */
    private String defaultScene;

    /**
     * 文档 JSON。
     */
    private String documentsJson;
}
