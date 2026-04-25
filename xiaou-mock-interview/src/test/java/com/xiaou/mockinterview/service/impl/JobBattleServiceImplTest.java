package com.xiaou.mockinterview.service.impl;

import com.xiaou.ai.dto.jobbattle.JobBattleJdParseResult;
import com.xiaou.ai.dto.jobbattle.JobBattlePlanResult;
import com.xiaou.ai.dto.jobbattle.JobBattleResumeMatchResult;
import com.xiaou.ai.dto.jobbattle.JobBattleTargetAnalysisResult;
import com.xiaou.ai.service.AiJobBattleService;
import com.xiaou.mockinterview.domain.JobBattleMatchRecord;
import com.xiaou.mockinterview.domain.JobBattlePlanRecord;
import com.xiaou.mockinterview.dto.request.JobBattleGeneratePlanRequest;
import com.xiaou.mockinterview.dto.request.JobBattleMatchEngineRunRequest;
import com.xiaou.mockinterview.dto.response.JobBattleMatchEngineResult;
import com.xiaou.mockinterview.mapper.JobBattleMatchRecordMapper;
import com.xiaou.mockinterview.mapper.JobBattlePlanRecordMapper;
import com.xiaou.mockinterview.service.CareerLoopService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobBattleServiceImplTest {

    @Mock
    private AiJobBattleService aiJobBattleService;

    @Mock
    private JobBattlePlanRecordMapper planRecordMapper;

    @Mock
    private JobBattleMatchRecordMapper matchRecordMapper;

    @Mock
    private CareerLoopService careerLoopService;

    @InjectMocks
    private JobBattleServiceImpl jobBattleService;

    @Test
    void shouldRunMatchEngineUsingAnalyzeTargetAndRankTargets() {
        when(aiJobBattleService.analyzeTarget(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(buildTargetAnalysis("Java后端开发", 86, 78, false, "高并发"))
                .thenReturn(buildTargetAnalysis("Go后端开发", 72, 60, true, "云原生"));
        doAnswer(invocation -> {
            JobBattleMatchRecord record = invocation.getArgument(0);
            record.setId(66L);
            return 1;
        }).when(matchRecordMapper).insert(any(JobBattleMatchRecord.class));

        JobBattleMatchEngineResult result = jobBattleService.runMatchEngine(1001L, buildMatchEngineRequest());

        assertEquals(66L, result.getAnalysisId());
        assertEquals("Java后端", result.getBestTargetRole());
        assertEquals(2, result.getRanking().size());
        assertEquals("Java后端", result.getRanking().get(0).getTargetRole());
        assertEquals(Integer.valueOf(1), result.getRanking().get(0).getRank());
        assertEquals("Go后端", result.getRanking().get(1).getTargetRole());
        assertEquals(1, result.getFallbackCount());
        verify(aiJobBattleService).analyzeTarget(eq("JD-1"), eq("Java后端"), eq("高级"), eq("上海"), eq("我的简历文本"), eq("做过订单系统重构"), eq("大厂"));
        verify(aiJobBattleService).analyzeTarget(eq("JD-2"), eq("Go后端"), eq("高级"), eq("杭州"), eq("我的简历文本"), eq("做过订单系统重构"), eq("大厂"));
        verify(careerLoopService).onEvent(eq(1001L), any());
    }

    @Test
    void shouldGeneratePlanAndPersistRecord() {
        JobBattlePlanResult aiResult = new JobBattlePlanResult()
                .setPlanName("四周求职计划")
                .setTotalDays(28)
                .setWeeklyGoals(List.of("完成P0技能补齐"))
                .setDailyTasks(List.of())
                .setMilestones(List.of())
                .setRiskAndFallback(List.of("时间不足时优先练面试"))
                .setFallback(false);
        when(aiJobBattleService.generatePlan(eq("[{\"skill\":\"分布式\"}]"), eq(28), eq(8), eq("实战"), eq("2026-05-15")))
                .thenReturn(aiResult);
        doAnswer(invocation -> {
            JobBattlePlanRecord record = invocation.getArgument(0);
            record.setId(88L);
            return 1;
        }).when(planRecordMapper).insert(any(JobBattlePlanRecord.class));

        JobBattlePlanResult result = jobBattleService.generatePlan(2002L, buildGeneratePlanRequest());

        assertEquals("四周求职计划", result.getPlanName());
        ArgumentCaptor<JobBattlePlanRecord> captor = ArgumentCaptor.forClass(JobBattlePlanRecord.class);
        verify(planRecordMapper).insert(captor.capture());
        assertEquals("四周求职计划", captor.getValue().getPlanName());
        assertFalse(Boolean.TRUE.equals(captor.getValue().getFallback()));
        assertTrue(captor.getValue().getPlanResultJson().contains("四周求职计划"));
        verify(careerLoopService).onEvent(eq(2002L), any());
    }

    private JobBattleTargetAnalysisResult buildTargetAnalysis(String jobTitle, int overallScore, int passRate,
                                                              boolean fallback, String missingKeyword) {
        Map<String, Integer> dimensionScores = new LinkedHashMap<>();
        dimensionScores.put("skillMatch", overallScore);
        dimensionScores.put("projectDepth", Math.max(40, overallScore - 5));
        dimensionScores.put("architectureAbility", Math.max(35, overallScore - 8));
        dimensionScores.put("communicationClarity", Math.max(45, overallScore + 3));

        JobBattleResumeMatchResult resumeMatch = new JobBattleResumeMatchResult()
                .setOverallScore(overallScore)
                .setDimensionScores(dimensionScores)
                .setStrengths(List.of("项目经历匹配"))
                .setMissingKeywords(List.of(missingKeyword))
                .setEstimatedPassRate(passRate)
                .setGaps(List.of(new JobBattleResumeMatchResult.Gap()
                        .setSkill("系统设计")
                        .setPriority("P0")
                        .setWhy("缺少大规模场景")
                        .setSuggestedAction("补充一次高并发项目复盘")))
                .setResumeRewriteSuggestions(List.of())
                .setFallback(fallback);

        JobBattleJdParseResult jdParse = new JobBattleJdParseResult()
                .setJobTitle(jobTitle)
                .setLevel("高级")
                .setMustSkills(List.of("Java", "系统设计"))
                .setNiceSkills(List.of())
                .setResponsibilities(List.of())
                .setSeniorityYears("5年以上")
                .setKeywords(List.of("高并发"))
                .setRiskPoints(List.of())
                .setSummary("强调复杂系统经验")
                .setFallback(fallback);

        return new JobBattleTargetAnalysisResult()
                .setJdParse(jdParse)
                .setResumeMatch(resumeMatch)
                .setFallback(fallback);
    }

    private JobBattleMatchEngineRunRequest buildMatchEngineRequest() {
        JobBattleMatchEngineRunRequest.TargetJob target1 = new JobBattleMatchEngineRunRequest.TargetJob();
        target1.setJdText("JD-1");
        target1.setTargetRole("Java后端");
        target1.setTargetLevel("高级");
        target1.setCity("上海");

        JobBattleMatchEngineRunRequest.TargetJob target2 = new JobBattleMatchEngineRunRequest.TargetJob();
        target2.setJdText("JD-2");
        target2.setTargetRole("Go后端");
        target2.setTargetLevel("高级");
        target2.setCity("杭州");

        JobBattleMatchEngineRunRequest request = new JobBattleMatchEngineRunRequest();
        request.setResumeText("我的简历文本");
        request.setProjectHighlights("做过订单系统重构");
        request.setTargetCompanyType("大厂");
        request.setTargets(List.of(target1, target2));
        return request;
    }

    private JobBattleGeneratePlanRequest buildGeneratePlanRequest() {
        JobBattleGeneratePlanRequest request = new JobBattleGeneratePlanRequest();
        request.setGapsJson("[{\"skill\":\"分布式\"}]");
        request.setTargetDays(28);
        request.setWeeklyHours(8);
        request.setPreferredLearningMode("实战");
        request.setNextInterviewDate("2026-05-15");
        return request;
    }
}
