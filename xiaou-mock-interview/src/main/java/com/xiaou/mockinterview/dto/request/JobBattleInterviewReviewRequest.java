package com.xiaou.mockinterview.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 求职作战台-面试复盘请求
 *
 * @author xiaou
 */
@Data
public class JobBattleInterviewReviewRequest {

    /**
     * 面试记录
     */
    @NotBlank(message = "面试记录不能为空")
    private String interviewNotes;

    /**
     * 问答记录（可选）
     */
    private String qaTranscriptJson;

    /**
     * 面试结果（pass/reject/pending）
     */
    @NotBlank(message = "面试结果不能为空")
    private String interviewResult;

    /**
     * 目标岗位
     */
    @NotBlank(message = "目标岗位不能为空")
    private String targetRole;

    /**
     * 下一场面试日期（可选，yyyy-MM-dd）
     */
    private String nextInterviewDate;
}

