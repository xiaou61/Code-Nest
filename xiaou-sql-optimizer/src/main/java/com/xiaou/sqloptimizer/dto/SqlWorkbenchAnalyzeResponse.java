package com.xiaou.sqloptimizer.dto;

import com.xiaou.ai.dto.sql.SqlAnalyzeResult;
import com.xiaou.ai.dto.sql.SqlRewriteResult;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * SQL优化工作台分析响应
 *
 * @author xiaou
 */
@Data
@Accessors(chain = true)
public class SqlWorkbenchAnalyzeResponse {

    /**
     * 记录ID
     */
    private Long recordId;

    /**
     * 工作流版本
     */
    private String workflowVersion;

    /**
     * 是否降级结果
     */
    private Boolean fallback;

    /**
     * 分析结果
     */
    private SqlAnalyzeResult analysis;

    /**
     * 重写建议
     */
    private SqlRewriteResult rewrite;
}
