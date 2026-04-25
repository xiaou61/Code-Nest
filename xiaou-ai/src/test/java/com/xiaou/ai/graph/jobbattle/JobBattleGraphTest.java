package com.xiaou.ai.graph.jobbattle;

import com.xiaou.ai.dto.jobbattle.JobBattleInterviewReviewResult;
import com.xiaou.ai.dto.jobbattle.JobBattleJdParseResult;
import com.xiaou.ai.dto.jobbattle.JobBattlePlanResult;
import com.xiaou.ai.dto.jobbattle.JobBattleResumeMatchResult;
import com.xiaou.ai.dto.jobbattle.JobBattleTargetAnalysisResult;
import com.xiaou.ai.rag.LlamaIndexClient;
import com.xiaou.ai.rag.LlamaIndexRetrieveResponse;
import com.xiaou.ai.scene.jobbattle.JobBattleSceneSupport;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobBattleGraphTest {

    @Mock
    private JobBattleSceneSupport sceneSupport;

    @Mock
    private LlamaIndexClient llamaIndexClient;

    @InjectMocks
    private JobBattleGraphRunner graphRunner;

    @Test
    void shouldRunParseJdGraphWithRagContext() {
        when(llamaIndexClient.isAvailable()).thenReturn(true);
        when(llamaIndexClient.retrieve(any())).thenReturn(new LlamaIndexRetrieveResponse()
                .setQuery("java")
                .setNodes(List.of(new LlamaIndexRetrieveResponse.Node()
                        .setId("doc-1")
                        .setScore(0.93)
                        .setText("Java 后端 JD 常见要求包括 Spring Boot、MySQL、分布式治理"))));

        when(sceneSupport.parseJd(eq("需要 Java 和 Spring Boot 经验"), eq("Java后端"), eq("中级"), eq("上海"), anyString()))
                .thenReturn(new JobBattleJdParseResult()
                        .setJobTitle("Java后端开发")
                        .setLevel("中级")
                        .setMustSkills(List.of("Java", "Spring Boot"))
                        .setNiceSkills(List.of("Redis"))
                        .setResponsibilities(List.of("负责核心服务开发"))
                        .setSeniorityYears("3-5年")
                        .setKeywords(List.of("分布式"))
                        .setRiskPoints(List.of())
                        .setSummary("聚焦后端基础与系统设计")
                        .setFallback(false));

        JobBattleJdParseResult result = graphRunner.runParseJd("需要 Java 和 Spring Boot 经验", "Java后端", "中级", "上海");

        assertEquals("Java后端开发", result.getJobTitle());
        ArgumentCaptor<String> ragContextCaptor = ArgumentCaptor.forClass(String.class);
        verify(sceneSupport).parseJd(eq("需要 Java 和 Spring Boot 经验"), eq("Java后端"), eq("中级"), eq("上海"), ragContextCaptor.capture());
        assertTrue(ragContextCaptor.getValue().contains("Spring Boot"));
    }

    @Test
    void shouldRunMatchResumeGraphWithRagContext() {
        when(llamaIndexClient.isAvailable()).thenReturn(true);
        when(llamaIndexClient.retrieve(any())).thenReturn(new LlamaIndexRetrieveResponse()
                .setQuery("resume")
                .setNodes(List.of(new LlamaIndexRetrieveResponse.Node()
                        .setId("doc-2")
                        .setScore(0.88)
                        .setText("简历匹配时优先关注关键词命中、项目指标和技术深度"))));

        Map<String, Integer> dimensionScores = new LinkedHashMap<>();
        dimensionScores.put("skillMatch", 82);
        dimensionScores.put("projectDepth", 78);
        dimensionScores.put("architectureAbility", 75);
        dimensionScores.put("communicationClarity", 80);

        when(sceneSupport.matchResume(anyString(), eq("我做过微服务和缓存优化"), eq("负责订单链路重构"), eq("大厂"), anyString()))
                .thenReturn(new JobBattleResumeMatchResult()
                        .setOverallScore(81)
                        .setDimensionScores(dimensionScores)
                        .setStrengths(List.of("项目经验贴近岗位"))
                        .setMissingKeywords(List.of("K8s"))
                        .setEstimatedPassRate(73)
                        .setGaps(List.of())
                        .setResumeRewriteSuggestions(List.of())
                        .setFallback(false));

        JobBattleResumeMatchResult result = graphRunner.runMatchResume(
                "{\"jobTitle\":\"Java后端开发\"}",
                "我做过微服务和缓存优化",
                "负责订单链路重构",
                "大厂"
        );

        assertEquals(81, result.getOverallScore());
        ArgumentCaptor<String> ragContextCaptor = ArgumentCaptor.forClass(String.class);
        verify(sceneSupport).matchResume(anyString(), eq("我做过微服务和缓存优化"), eq("负责订单链路重构"), eq("大厂"), ragContextCaptor.capture());
        assertTrue(ragContextCaptor.getValue().contains("关键词命中"));
    }

    @Test
    void shouldRunAnalyzeTargetGraphAndReuseParsedJdForMatchEngine() {
        when(llamaIndexClient.isAvailable()).thenReturn(false);
        when(sceneSupport.parseJd(eq("JD文本"), eq("Java后端"), eq("高级"), eq("杭州"), anyString()))
                .thenReturn(new JobBattleJdParseResult()
                        .setJobTitle("Java后端开发")
                        .setLevel("高级")
                        .setMustSkills(List.of("Java", "架构设计"))
                        .setNiceSkills(List.of())
                        .setResponsibilities(List.of())
                        .setSeniorityYears("5年以上")
                        .setKeywords(List.of("高并发"))
                        .setRiskPoints(List.of())
                        .setSummary("强调架构能力")
                        .setFallback(false));
        when(sceneSupport.matchResume(anyString(), eq("简历文本"), eq("项目亮点"), eq("独角兽"), anyString()))
                .thenReturn(new JobBattleResumeMatchResult()
                        .setOverallScore(79)
                        .setDimensionScores(Map.of("skillMatch", 79))
                        .setStrengths(List.of("项目与 JD 匹配"))
                        .setMissingKeywords(List.of("容器化"))
                        .setEstimatedPassRate(70)
                        .setGaps(List.of())
                        .setResumeRewriteSuggestions(List.of())
                        .setFallback(false));

        JobBattleTargetAnalysisResult result = graphRunner.runAnalyzeTarget(
                "JD文本",
                "Java后端",
                "高级",
                "杭州",
                "简历文本",
                "项目亮点",
                "独角兽"
        );

        assertFalse(result.isFallback());
        assertEquals("Java后端开发", result.getJdParse().getJobTitle());
        assertEquals(79, result.getResumeMatch().getOverallScore());

        ArgumentCaptor<String> parsedJdCaptor = ArgumentCaptor.forClass(String.class);
        verify(sceneSupport).matchResume(parsedJdCaptor.capture(), eq("简历文本"), eq("项目亮点"), eq("独角兽"), anyString());
        assertTrue(parsedJdCaptor.getValue().contains("Java后端开发"));
    }

    @Test
    void shouldRunGeneratePlanGraphWithoutRagRetrieval() {
        when(llamaIndexClient.isAvailable()).thenReturn(false);
        when(sceneSupport.generatePlan(eq("[{\"skill\":\"系统设计\"}]"), eq(21), eq(8), eq("视频+实战"), eq("2026-05-10"), anyString()))
                .thenReturn(new JobBattlePlanResult()
                        .setPlanName("三周冲刺计划")
                        .setTotalDays(21)
                        .setWeeklyGoals(List.of("完成系统设计专题"))
                        .setDailyTasks(List.of())
                        .setMilestones(List.of())
                        .setRiskAndFallback(List.of("时间不足优先做P0"))
                        .setFallback(false));

        JobBattlePlanResult result = graphRunner.runGeneratePlan(
                "[{\"skill\":\"系统设计\"}]",
                21,
                8,
                "视频+实战",
                "2026-05-10"
        );

        assertEquals("三周冲刺计划", result.getPlanName());
        verify(llamaIndexClient, never()).retrieve(any());
    }

    @Test
    void shouldReturnFallbackReviewResultFromGraph() {
        when(llamaIndexClient.isAvailable()).thenReturn(false);
        when(sceneSupport.reviewInterview(anyString(), anyString(), eq("reject"), eq("Java后端"), eq("2026-05-12"), anyString()))
                .thenReturn(JobBattleInterviewReviewResult.fallbackResult("reject"));

        JobBattleInterviewReviewResult result = graphRunner.runReviewInterview(
                "面试官追问系统设计较多",
                "[{\"q\":\"限流\",\"a\":\"...\"}]",
                "reject",
                "Java后端",
                "2026-05-12"
        );

        assertTrue(result.isFallback());
        assertTrue(result.getOverallConclusion().contains("改进空间") || result.getOverallConclusion().contains("建议"));
    }

    @Test
    void shouldMarkAnalyzeTargetFallbackWhenResumeMatchFallsBack() {
        when(llamaIndexClient.isAvailable()).thenReturn(false);
        when(sceneSupport.parseJd(eq("JD文本"), eq("Java后端"), eq("高级"), eq("杭州"), anyString()))
                .thenReturn(new JobBattleJdParseResult()
                        .setJobTitle("Java后端开发")
                        .setLevel("高级")
                        .setMustSkills(List.of("Java", "Spring Boot"))
                        .setNiceSkills(List.of())
                        .setResponsibilities(List.of())
                        .setSeniorityYears("5年以上")
                        .setKeywords(List.of("高并发"))
                        .setRiskPoints(List.of())
                        .setSummary("强调核心链路稳定性")
                        .setFallback(false));
        when(sceneSupport.matchResume(anyString(), eq("简历文本"), eq("项目亮点"), eq("独角兽"), anyString()))
                .thenReturn(JobBattleResumeMatchResult.fallbackResult(
                        "{\"mustSkills\":[\"Java\",\"Redis\"]}",
                        "简历文本"
                ));

        JobBattleTargetAnalysisResult result = graphRunner.runAnalyzeTarget(
                "JD文本",
                "Java后端",
                "高级",
                "杭州",
                "简历文本",
                "项目亮点",
                "独角兽"
        );

        assertTrue(result.isFallback());
        assertFalse(result.getJdParse().isFallback());
        assertTrue(result.getResumeMatch().isFallback());
    }
}
