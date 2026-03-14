package com.xiaou.sqloptimizer.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * SQL工作台收益对比请求
 *
 * @author xiaou
 */
@Data
public class SqlWorkbenchCompareRequest {

    /**
     * 可选：关联的工作台记录ID，用于将对比结果回写到历史案例
     */
    private Long recordId;

    /**
     * 优化前SQL
     */
    @NotBlank(message = "优化前SQL不能为空")
    private String beforeSql;

    /**
     * 优化后SQL
     */
    @NotBlank(message = "优化后SQL不能为空")
    private String afterSql;

    /**
     * 优化前EXPLAIN
     */
    @NotBlank(message = "优化前EXPLAIN不能为空")
    private String beforeExplain;

    /**
     * 优化后EXPLAIN
     */
    @NotBlank(message = "优化后EXPLAIN不能为空")
    private String afterExplain;

    /**
     * EXPLAIN格式
     */
    private String explainFormat = "TABLE";
}
