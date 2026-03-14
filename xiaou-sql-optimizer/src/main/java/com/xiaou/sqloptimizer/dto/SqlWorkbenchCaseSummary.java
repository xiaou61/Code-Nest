package com.xiaou.sqloptimizer.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 工作台案例摘要
 *
 * @author xiaou
 */
@Data
@Accessors(chain = true)
public class SqlWorkbenchCaseSummary {

    /**
     * 记录ID
     */
    private Long id;

    /**
     * 评分
     */
    private Integer score;

    /**
     * 工作流版本
     */
    private String workflowVersion;

    /**
     * 是否降级
     */
    private Boolean fallback;

    /**
     * 是否包含重写结果
     */
    private Boolean hasRewrite;

    /**
     * 是否包含收益对比结果
     */
    private Boolean hasCompare;

    /**
     * 是否收藏
     */
    private Boolean favorite;

    /**
     * 问题总数
     */
    private Integer problemCount;

    /**
     * 最高严重级别（HIGH/MEDIUM/LOW/NONE）
     */
    private String highestSeverity;

    /**
     * 原始SQL预览
     */
    private String originalSqlPreview;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
