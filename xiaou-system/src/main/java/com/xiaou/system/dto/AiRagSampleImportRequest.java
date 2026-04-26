package com.xiaou.system.dto;

import lombok.Data;

/**
 * RAG 样例知识导入请求。
 *
 * @author xiaou
 */
@Data
public class AiRagSampleImportRequest {

    /**
     * 是否替换现有文档。
     */
    private Boolean replace;
}
