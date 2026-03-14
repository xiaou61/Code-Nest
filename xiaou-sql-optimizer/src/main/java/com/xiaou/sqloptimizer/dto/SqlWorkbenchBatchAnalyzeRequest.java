package com.xiaou.sqloptimizer.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * SQL工作台批量分析请求
 *
 * @author xiaou
 */
@Data
public class SqlWorkbenchBatchAnalyzeRequest {

    /**
     * 批量分析项
     */
    @Valid
    @NotEmpty(message = "批量分析项不能为空")
    @Size(max = 20, message = "批量分析最多支持20条")
    private List<SqlAnalyzeRequest> items;
}
