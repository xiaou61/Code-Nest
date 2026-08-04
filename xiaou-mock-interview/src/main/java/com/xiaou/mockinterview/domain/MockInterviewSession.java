package com.xiaou.mockinterview.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 模拟面试会话实体
 *
 * @author xiaou
 */
@Data
@Accessors(chain = true)
public class MockInterviewSession {

    /**
     * 会话ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 面试方向（java/frontend/python等）
     */
    private String direction;

    /**
     * 难度级别：1-初级 2-中级 3-高级
     */
    private Integer level;

    /**
     * 面试类型：1-技术 2-综合 3-专项
     */
    private Integer interviewType;

    /**
     * 专项面试关注点，仅保留用户确认后的短技术主题。
     */
    private String specializedTopic;

    /**
     * AI风格：1-温和 2-标准 3-压力
     */
    private Integer style;

    /**
     * 题目数量
     */
    private Integer questionCount;

    /**
     * 出题模式：1-本地题库 2-AI出题
     */
    private Integer questionMode;

    /**
     * 预计时长（分钟）
     */
    private Integer durationMinutes;

    /**
     * 状态：0-进行中 1-已完成 2-已中断
     */
    private Integer status;

    /**
     * 总分（满分100）
     */
    private Integer totalScore;

    /**
     * 知识得分
     */
    private Integer knowledgeScore;

    /**
     * 深度得分
     */
    private Integer depthScore;

    /**
     * 表达得分
     */
    private Integer expressionScore;

    /**
     * 应变得分
     */
    private Integer adaptabilityScore;

    /**
     * AI总体评价
     */
    private String aiSummary;

    /**
     * AI改进建议
     */
    private String aiSuggestion;

    /**
     * 当前题目序号
     */
    private Integer currentQuestionOrder;

    /**
     * 开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime endTime;

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

    // =================== 查询时使用的非数据库字段 ===================

    /**
     * 方向名称（查询时使用）
     */
    private String directionName;

    /**
     * 难度名称（查询时使用）
     */
    private String levelName;

    /**
     * 风格名称（查询时使用）
     */
    private String styleName;

    /**
     * 已回答题目数（查询时使用）
     */
    private Integer answeredCount;
}
