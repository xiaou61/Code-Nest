package com.xiaou.ai.rag;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * LlamaIndex 服务健康检查响应。
 *
 * @author xiaou
 */
@Data
@Accessors(chain = true)
public class LlamaIndexHealthResponse {

    /**
     * 服务状态。
     */
    private String status;

    /**
     * 是否启用鉴权。
     */
    private boolean authEnabled;

    /**
     * 当前文档数量。
     */
    private Integer documentCount;

    /**
     * 场景数量。
     */
    private Integer sceneCount;

    /**
     * 数据文件路径。
     */
    private String dataFile;
}
