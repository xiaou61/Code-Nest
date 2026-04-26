package com.xiaou.ai.rag;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/**
 * LlamaIndex 文档列表响应。
 *
 * @author xiaou
 */
@Data
@Accessors(chain = true)
public class LlamaIndexDocumentListResponse {

    /**
     * 当前返回的文档总数。
     */
    private Integer totalCount;

    /**
     * 文档列表。
     */
    private List<LlamaIndexDocumentListItem> documents = new ArrayList<>();
}
