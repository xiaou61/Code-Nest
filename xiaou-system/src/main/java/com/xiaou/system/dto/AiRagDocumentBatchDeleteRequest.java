package com.xiaou.system.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * RAG 文档批量删除请求。
 *
 * @author xiaou
 */
@Data
public class AiRagDocumentBatchDeleteRequest {

    /**
     * 待删除的文档 ID 列表。
     */
    private List<String> documentIds = new ArrayList<>();
}
