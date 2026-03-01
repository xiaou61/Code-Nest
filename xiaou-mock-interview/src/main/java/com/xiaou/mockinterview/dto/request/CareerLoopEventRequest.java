package com.xiaou.mockinterview.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 求职闭环事件请求
 *
 * @author xiaou
 */
@Data
@Accessors(chain = true)
public class CareerLoopEventRequest {

    /**
     * 事件来源：job_battle/mock_interview/plan/manual
     */
    private String source;

    /**
     * 目标阶段
     */
    private String targetStage;

    /**
     * 关联记录ID
     */
    private String refId;

    /**
     * 备注
     */
    private String note;

    /**
     * 执行计划进度（0-100）
     */
    private Integer planProgress;

    /**
     * 模拟面试次数
     */
    private Integer mockCount;

    /**
     * 最近模拟面试分数
     */
    private Integer latestMockScore;

    /**
     * 复盘次数
     */
    private Integer reviewCount;

    /**
     * 简历最近更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime resumeUpdatedAt;

    /**
     * 风险项
     */
    private List<String> riskFlags;

    /**
     * 下一步建议
     */
    private List<String> nextSuggestions;
}

