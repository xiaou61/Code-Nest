package com.xiaou.sqloptimizer.dto;

import com.xiaou.ai.dto.sql.SqlAnalyzeResult;
import com.xiaou.ai.dto.sql.SqlCompareResult;
import com.xiaou.ai.dto.sql.SqlRewriteResult;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 工作台记录持久化载荷
 *
 * @author xiaou
 */
@Data
@Accessors(chain = true)
public class SqlWorkbenchRecordPayload {

    /**
     * 工作流版本
     */
    private String workflowVersion;

    /**
     * 是否包含降级结果
     */
    private Boolean fallback;

    /**
     * 分析结果
     */
    private SqlAnalyzeResult analysis;

    /**
     * 重写结果
     */
    private SqlRewriteResult rewrite;

    /**
     * 对比结果
     */
    private SqlCompareResult compare;
}
