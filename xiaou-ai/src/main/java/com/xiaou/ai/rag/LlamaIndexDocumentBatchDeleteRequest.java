package com.xiaou.ai.rag;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/**
 * LlamaIndex 批量删除文档请求。
 *
 * @author xiaou
 */
@Data
@Accessors(chain = true)
public class LlamaIndexDocumentBatchDeleteRequest {

    /**
     * 待删除的文档 ID 列表。
     */
    private List<String> documentIds = new ArrayList<>();
}
