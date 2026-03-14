package com.xiaou.sqloptimizer.dto;

import com.xiaou.ai.dto.sql.SqlCompareResult;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * SQL工作台收益对比响应
 *
 * @author xiaou
 */
@Data
@Accessors(chain = true)
public class SqlWorkbenchCompareResponse {

    /**
     * 工作流版本
     */
    private String workflowVersion;

    /**
     * 是否降级结果
     */
    private Boolean fallback;

    /**
     * 对比结果
     */
    private SqlCompareResult compare;
}
