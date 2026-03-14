package com.xiaou.sqloptimizer.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.xiaou.ai.dto.sql.SqlAnalyzeResult;
import com.xiaou.ai.dto.sql.SqlCompareResult;
import com.xiaou.ai.dto.sql.SqlRewriteResult;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工作台案例详情响应
 *
 * @author xiaou
 */
@Data
@Accessors(chain = true)
public class SqlWorkbenchCaseDetailResponse {

    /**
     * 记录ID
     */
    private Long id;

    /**
     * 工作流版本
     */
    private String workflowVersion;

    /**
     * 是否降级
     */
    private Boolean fallback;

    /**
     * 原始SQL
     */
    private String originalSql;

    /**
     * EXPLAIN结果
     */
    private String explainResult;

    /**
     * EXPLAIN格式
     */
    private String explainFormat;

    /**
     * MySQL版本
     */
    private String mysqlVersion;

    /**
     * 表结构
     */
    private List<SqlAnalyzeRequest.TableStructure> tableStructures;

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

    /**
     * 评分
     */
    private Integer score;

    /**
     * 是否收藏
     */
    private Boolean favorite;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
}
