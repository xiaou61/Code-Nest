package com.xiaou.ai.rag;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * LlamaIndex 文档导入响应。
 *
 * @author xiaou
 */
@Data
@Accessors(chain = true)
public class LlamaIndexDocumentImportResponse {

    /**
     * 本次导入数量。
     */
    private Integer importedCount;

    /**
     * 导入后总文档数。
     */
    private Integer totalCount;
}
