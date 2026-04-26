package com.xiaou.ai.service.impl;

import com.xiaou.ai.dto.jobbattle.JobBattleInterviewReviewResult;
import com.xiaou.ai.dto.jobbattle.JobBattleJdParseResult;
import com.xiaou.ai.dto.jobbattle.JobBattlePlanResult;
import com.xiaou.ai.dto.jobbattle.JobBattleResumeMatchResult;
import com.xiaou.ai.dto.jobbattle.JobBattleTargetAnalysisResult;
import com.xiaou.ai.graph.jobbattle.JobBattleGraphRunner;
import com.xiaou.ai.service.AiJobBattleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 求职作战台 AI 服务实现。
 *
 * @author xiaou
 */
@Service
@RequiredArgsConstructor
public class AiJobBattleServiceImpl implements AiJobBattleService {

    private final JobBattleGraphRunner jobBattleGraphRunner;

    @Override
    public JobBattleJdParseResult parseJd(String jdText, String targetRole, String targetLevel, String city) {
        return jobBattleGraphRunner.runParseJd(jdText, targetRole, targetLevel, city);
    }

    @Override
    public JobBattleResumeMatchResult matchResume(String parsedJdJson, String resumeText,
                                                  String projectHighlights, String targetCompanyType) {
        return jobBattleGraphRunner.runMatchResume(parsedJdJson, resumeText, projectHighlights, targetCompanyType);
    }

    @Override
    public JobBattlePlanResult generatePlan(String gapsJson, Integer targetDays, Integer weeklyHours,
                                            String preferredLearningMode, String nextInterviewDate) {
        return jobBattleGraphRunner.runGeneratePlan(
                gapsJson,
                targetDays,
                weeklyHours,
                preferredLearningMode,
                nextInterviewDate
        );
    }

    @Override
    public JobBattleInterviewReviewResult reviewInterview(String interviewNotes, String qaTranscriptJson,
                                                          String interviewResult, String targetRole,
                                                          String nextInterviewDate) {
        return jobBattleGraphRunner.runReviewInterview(
                interviewNotes,
                qaTranscriptJson,
                interviewResult,
                targetRole,
                nextInterviewDate
        );
    }

    @Override
    public JobBattleTargetAnalysisResult analyzeTarget(String jdText, String targetRole, String targetLevel, String city,
                                                       String resumeText, String projectHighlights, String targetCompanyType) {
        return jobBattleGraphRunner.runAnalyzeTarget(
                jdText,
                targetRole,
                targetLevel,
                city,
                resumeText,
                projectHighlights,
                targetCompanyType
        );
    }
}
