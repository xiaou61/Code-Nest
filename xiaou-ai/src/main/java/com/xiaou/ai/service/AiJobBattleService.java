package com.xiaou.ai.service;

import com.xiaou.ai.dto.jobbattle.JobBattleInterviewReviewResult;
import com.xiaou.ai.dto.jobbattle.JobBattleJdParseResult;
import com.xiaou.ai.dto.jobbattle.JobBattlePlanResult;
import com.xiaou.ai.dto.jobbattle.JobBattleResumeMatchResult;
import com.xiaou.ai.dto.jobbattle.JobBattleTargetAnalysisResult;

/**
 * 求职作战台AI服务接口
 *
 * @author xiaou
 */
public interface AiJobBattleService {

    /**
     * 解析岗位JD
     *
     * @param jdText      JD原文
     * @param targetRole  目标岗位（可选）
     * @param targetLevel 目标级别（可选）
     * @param city        目标城市（可选）
     * @return JD解析结果
     */
    JobBattleJdParseResult parseJd(String jdText, String targetRole, String targetLevel, String city);

    /**
     * 评估简历与JD的匹配度
     *
     * @param parsedJdJson       结构化JD JSON
     * @param resumeText         简历内容
     * @param projectHighlights  项目亮点（可选）
     * @param targetCompanyType  公司类型（可选）
     * @return 匹配评估结果
     */
    JobBattleResumeMatchResult matchResume(String parsedJdJson, String resumeText,
                                           String projectHighlights, String targetCompanyType);

    /**
     * 生成补短板行动计划
     *
     * @param gapsJson               能力差距列表JSON
     * @param targetDays             计划天数
     * @param weeklyHours            每周可投入时长
     * @param preferredLearningMode  学习偏好（可选）
     * @param nextInterviewDate      下一场面试日期（可选）
     * @return 行动计划结果
     */
    JobBattlePlanResult generatePlan(String gapsJson, Integer targetDays, Integer weeklyHours,
                                     String preferredLearningMode, String nextInterviewDate);

    /**
     * 生成面试复盘总结
     *
     * @param interviewNotes   面试记录
     * @param qaTranscriptJson 问答记录（可选）
     * @param interviewResult  面试结果（pass/reject/pending）
     * @param targetRole       目标岗位
     * @param nextInterviewDate 下一场面试日期（可选）
     * @return 复盘结果
     */
    JobBattleInterviewReviewResult reviewInterview(String interviewNotes, String qaTranscriptJson,
                                                   String interviewResult, String targetRole,
                                                   String nextInterviewDate);

    /**
     * 单岗位综合分析（用于批量匹配引擎内部串联 JD 解析与简历匹配）
     *
     * @param jdText             JD 原文
     * @param targetRole         目标岗位
     * @param targetLevel        目标级别
     * @param city               城市
     * @param resumeText         简历内容
     * @param projectHighlights  项目亮点
     * @param targetCompanyType  公司类型
     * @return 单岗位综合分析结果
     */
    JobBattleTargetAnalysisResult analyzeTarget(String jdText, String targetRole, String targetLevel, String city,
                                                String resumeText, String projectHighlights, String targetCompanyType);
}

