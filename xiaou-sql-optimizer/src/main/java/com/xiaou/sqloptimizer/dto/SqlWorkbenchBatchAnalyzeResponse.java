package com.xiaou.sqloptimizer.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * SQL工作台批量分析响应
 *
 * @author xiaou
 */
@Data
@Accessors(chain = true)
public class SqlWorkbenchBatchAnalyzeResponse {

    /**
     * 总数
     */
    private Integer total;

    /**
     * 成功数
     */
    private Integer successCount;

    /**
     * 降级数
     */
    private Integer fallbackCount;

    /**
     * 失败数
     */
    private Integer failedCount;

    /**
     * 明细
     */
    private List<Item> items;

    @Data
    @Accessors(chain = true)
    public static class Item {
        private Integer index;
        private Boolean success;
        private String message;
        private SqlWorkbenchAnalyzeResponse result;
    }
}
