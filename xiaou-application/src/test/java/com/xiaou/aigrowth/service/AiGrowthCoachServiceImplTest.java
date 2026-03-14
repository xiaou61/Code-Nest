package com.xiaou.aigrowth.service;

import com.xiaou.ai.dto.growthcoach.GrowthCoachChatResult;
import com.xiaou.ai.dto.growthcoach.GrowthCoachSnapshotAiResult;
import com.xiaou.ai.service.AiGrowthCoachWorkflowService;
import com.xiaou.aigrowth.domain.AiGrowthCoachAction;
import com.xiaou.aigrowth.domain.AiGrowthCoachChatMessage;
import com.xiaou.aigrowth.domain.AiGrowthCoachChatSession;
import com.xiaou.aigrowth.domain.AiGrowthCoachConfig;
import com.xiaou.aigrowth.domain.AiGrowthCoachSnapshot;
import com.xiaou.aigrowth.dto.request.AiGrowthCoachChatRequest;
import com.xiaou.aigrowth.dto.request.AiGrowthCoachRefreshRequest;
import com.xiaou.aigrowth.dto.response.AiGrowthCoachChatResponse;
import com.xiaou.aigrowth.dto.response.AiGrowthCoachSnapshotDetailResponse;
import com.xiaou.aigrowth.mapper.AiGrowthCoachActionMapper;
import com.xiaou.aigrowth.mapper.AiGrowthCoachChatMessageMapper;
import com.xiaou.aigrowth.mapper.AiGrowthCoachChatSessionMapper;
import com.xiaou.aigrowth.mapper.AiGrowthCoachConfigMapper;
import com.xiaou.aigrowth.mapper.AiGrowthCoachReplanLogMapper;
import com.xiaou.aigrowth.mapper.AiGrowthCoachSnapshotMapper;
import com.xiaou.aigrowth.service.impl.AiGrowthCoachServiceImpl;
import com.xiaou.ai.dto.jobbattle.JobBattleResumeMatchResult;
import com.xiaou.blog.dto.ArticleListRequest;
import com.xiaou.blog.dto.ArticleSimpleResponse;
import com.xiaou.blog.service.BlogArticleService;
import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.exception.BusinessException;
import com.xiaou.community.dto.CommunityPostResponse;
import com.xiaou.community.service.CommunityHotPostService;
import com.xiaou.codepen.dto.CodePenDetailResponse;
import com.xiaou.codepen.service.CodePenService;
import com.xiaou.flashcard.dto.response.FlashcardStudyStatsVO;
import com.xiaou.flashcard.service.FlashcardStudyService;
import com.xiaou.interview.dto.ReviewStatsResponse;
import com.xiaou.interview.service.InterviewLearnRecordService;
import com.xiaou.interview.service.InterviewMasteryService;
import com.xiaou.learningasset.dto.request.LearningAssetRecordQueryRequest;
import com.xiaou.learningasset.dto.response.LearningAssetRecordSummaryResponse;
import com.xiaou.learningasset.service.LearningAssetService;
import com.xiaou.mockinterview.domain.CareerLoopSession;
import com.xiaou.mockinterview.domain.CareerLoopSnapshot;
import com.xiaou.mockinterview.dto.response.CareerLoopCurrentResponse;
import com.xiaou.mockinterview.dto.response.JobBattleMatchEngineResult;
import com.xiaou.mockinterview.service.CareerLoopService;
import com.xiaou.mockinterview.service.JobBattleService;
import com.xiaou.oj.dto.OjStatisticsVO;
import com.xiaou.oj.service.OjSubmissionService;
import com.xiaou.plan.dto.PlanStatsResponse;
import com.xiaou.plan.service.PlanService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiGrowthCoachServiceImplTest {

    @Mock
    private AiGrowthCoachSnapshotMapper snapshotMapper;

    @Mock
    private AiGrowthCoachActionMapper actionMapper;

    @Mock
    private AiGrowthCoachChatSessionMapper chatSessionMapper;

    @Mock
    private AiGrowthCoachChatMessageMapper chatMessageMapper;

    @Mock
    private AiGrowthCoachConfigMapper configMapper;

    @Mock
    private AiGrowthCoachReplanLogMapper replanLogMapper;

    @Mock
    private AiGrowthCoachWorkflowService workflowService;

    @Mock
    private PlanService planService;

    @Mock
    private FlashcardStudyService flashcardStudyService;

    @Mock
    private InterviewMasteryService interviewMasteryService;

    @Mock
    private InterviewLearnRecordService interviewLearnRecordService;

    @Mock
    private OjSubmissionService ojSubmissionService;

    @Mock
    private CareerLoopService careerLoopService;

    @Mock
    private JobBattleService jobBattleService;

    @Mock
    private LearningAssetService learningAssetService;

    @Mock
    private BlogArticleService blogArticleService;

    @Mock
    private CommunityHotPostService communityHotPostService;

    @Mock
    private CodePenService codePenService;

    @InjectMocks
    private AiGrowthCoachServiceImpl service;

    @Test
    void refreshShouldRejectDisabledScene() {
        Long userId = 1001L;
        AiGrowthCoachRefreshRequest request = new AiGrowthCoachRefreshRequest();
        request.setScene("hybrid");
        request.setForce(true);
        when(configMapper.selectAll()).thenReturn(List.of(
                new AiGrowthCoachConfig().setConfigKey("scene_enabled_hybrid").setConfigValue("false").setStatus("ENABLED")
        ));

        BusinessException exception = assertThrows(BusinessException.class, () -> service.refresh(userId, request));

        assertTrue(exception.getMessage().contains("场景"));
        verify(snapshotMapper, never()).insert(any(AiGrowthCoachSnapshot.class));
        verify(workflowService, never()).generateSnapshot(any());
    }

    @Test
    void refreshShouldPersistFallbackSnapshotActionsAndResourcesWhenWorkflowUnavailable() throws Exception {
        Long userId = 1001L;
        AiGrowthCoachRefreshRequest request = new AiGrowthCoachRefreshRequest();
        request.setScene("hybrid");
        request.setForce(true);

        when(snapshotMapper.selectLatestReady(eq(userId), eq("HYBRID"))).thenReturn(null);
        when(planService.getStatsOverview(userId)).thenReturn(PlanStatsResponse.builder()
                .weekCheckinCount(2)
                .todayCompletedCount(1)
                .todayPendingCount(3)
                .build());

        FlashcardStudyStatsVO flashcardStats = new FlashcardStudyStatsVO();
        flashcardStats.setTodayLearnedCount(12);
        flashcardStats.setTodayDueCount(8);
        flashcardStats.setStreakDays(5);
        when(flashcardStudyService.getStudyStats(userId, null)).thenReturn(flashcardStats);

        ReviewStatsResponse reviewStats = new ReviewStatsResponse()
                .setOverdueCount(6)
                .setTodayCount(4)
                .setLevel1Count(8)
                .setLevel2Count(5);
        when(interviewMasteryService.getReviewStats(userId)).thenReturn(reviewStats);
        when(interviewLearnRecordService.getTotalLearnedCount(userId)).thenReturn(48);

        OjStatisticsVO ojStatistics = new OjStatisticsVO();
        ojStatistics.setAcceptedProblems(26);
        ojStatistics.setMediumAccepted(11);
        when(ojSubmissionService.getStatistics(userId)).thenReturn(ojStatistics);

        CareerLoopCurrentResponse current = new CareerLoopCurrentResponse()
                .setSession(new CareerLoopSession()
                        .setId(201L)
                        .setUserId(userId)
                        .setTargetRole("Java后端")
                        .setWeeklyHours(8)
                        .setCurrentStage("PLAN_EXECUTING")
                        .setHealthScore(68))
                .setSnapshot(new CareerLoopSnapshot()
                        .setPlanProgress(40)
                        .setMockCount(1)
                        .setLatestMockScore(73))
                .setRiskFlags(List.of("项目表达偏弱", "系统设计稳定性不足"))
                .setNextSuggestions(List.of("补 Redis", "补系统设计"));
        when(careerLoopService.getCurrent(userId)).thenReturn(current);

        JobBattleMatchEngineResult matchResult = new JobBattleMatchEngineResult()
                .setAnalysisId(301L)
                .setBestScore(74)
                .setAverageScore(68)
                .setBestTargetRole("Java后端");
        JobBattleResumeMatchResult.Gap gap = new JobBattleResumeMatchResult.Gap()
                .setSkill("Redis")
                .setPriority("P0")
                .setWhy("命中多个岗位高频要求")
                .setSuggestedAction("补缓存一致性和击穿治理");
        matchResult.setRanking(List.of(new JobBattleMatchEngineResult.TargetScore()
                .setRank(1)
                .setTargetRole("Java后端")
                .setEngineScore(74)
                .setP0GapCount(1)
                .setTopGaps(List.of(gap))));
        when(jobBattleService.getLatestMatchEngineResult(userId)).thenReturn(matchResult);

        LearningAssetRecordSummaryResponse assetRecord = new LearningAssetRecordSummaryResponse();
        assetRecord.setRecordId(401L);
        assetRecord.setSourceType("blog");
        assetRecord.setSourceId(9001L);
        assetRecord.setSourceTitle("Redis 一致性复盘");
        assetRecord.setStatus("PUBLISHED");
        PageResult<LearningAssetRecordSummaryResponse> assetPage = PageResult.of(1, 10, 1L, List.of(assetRecord));
        when(learningAssetService.getRecordList(eq(userId), any(LearningAssetRecordQueryRequest.class))).thenReturn(assetPage);

        ArticleSimpleResponse article = new ArticleSimpleResponse();
        article.setId(501L);
        article.setUserId(3001L);
        article.setTitle("Java后端冲刺路线");
        article.setSummary("围绕 Redis、MySQL 和系统设计建立面试补短板清单。");
        when(blogArticleService.getUserArticleList(any(ArticleListRequest.class)))
                .thenReturn(PageResult.of(1, 3, 1L, List.of(article)));

        CommunityPostResponse communityPost = new CommunityPostResponse();
        communityPost.setId(601L);
        communityPost.setTitle("最近一次系统设计复盘");
        communityPost.setAiSummary("聚焦缓存穿透、击穿和一致性。");
        when(communityHotPostService.getHotPosts(3)).thenReturn(List.of(communityPost));

        CodePenDetailResponse codePen = new CodePenDetailResponse();
        codePen.setId(701L);
        codePen.setTitle("Redis 可视化演示");
        codePen.setDescription("用交互 Demo 理解缓存淘汰策略和命中率。");
        when(codePenService.getRecommendList(3)).thenReturn(List.of(codePen));

        when(workflowService.generateSnapshot(any())).thenReturn(null);

        doAnswer(invocation -> {
            AiGrowthCoachSnapshot snapshot = invocation.getArgument(0);
            snapshot.setId(501L);
            snapshot.setGeneratedAt(LocalDateTime.now());
            return 1;
        }).when(snapshotMapper).insert(any(AiGrowthCoachSnapshot.class));
        doAnswer(invocation -> {
            AiGrowthCoachAction action = invocation.getArgument(0);
            if (action.getId() == null) {
                action.setId((long) (900 + action.getSortOrder()));
            }
            return 1;
        }).when(actionMapper).insert(any(AiGrowthCoachAction.class));
        when(actionMapper.selectBySnapshotId(501L)).thenReturn(List.of(
                new AiGrowthCoachAction().setId(901L).setSnapshotId(501L).setUserId(userId)
                        .setTitle("补强 Redis 专项").setPriority("P0").setActionType("skill_gap")
                        .setTargetRoute("/interview").setReason("Redis 是当前影响岗位匹配的关键短板")
                        .setExpectedGain("提升岗位匹配稳定性").setEstimatedMinutes(60).setStatus("TODO").setSortOrder(1),
                new AiGrowthCoachAction().setId(902L).setSnapshotId(501L).setUserId(userId)
                        .setTitle("清理逾期复习题").setPriority("P0").setActionType("review")
                        .setTargetRoute("/interview/review?type=overdue").setReason("当前有 6 道逾期题需要回收")
                        .setExpectedGain("恢复知识点稳定度").setEstimatedMinutes(35).setStatus("TODO").setSortOrder(2),
                new AiGrowthCoachAction().setId(903L).setSnapshotId(501L).setUserId(userId)
                        .setTitle("补齐本周计划节奏").setPriority("P1").setActionType("plan")
                        .setTargetRoute("/plan").setReason("当前计划进度偏慢")
                        .setExpectedGain("提高执行稳定性").setEstimatedMinutes(30).setStatus("TODO").setSortOrder(3)
        ));

        AiGrowthCoachSnapshotDetailResponse response = service.refresh(userId, request);

        assertNotNull(response);
        assertEquals(501L, response.getSnapshotId());
        assertEquals("READY", response.getStatus());
        assertTrue(response.getFallbackOnly());
        assertTrue(response.getOverallScore() > 0);
        assertTrue(response.getHeadline().contains("本周"));
        assertTrue(response.getActions().size() >= 3);
        assertTrue(response.getRiskFlags().stream().anyMatch(item -> item.contains("Redis") || item.contains("系统设计")));
        List<?> resources = invokeListGetter(response, "getResources");
        assertNotNull(resources);
        assertTrue(resources.size() >= 4);
        assertTrue(resources.stream().map(item -> readProperty(item, "resourceType")).anyMatch("ASSET"::equals));
        assertTrue(resources.stream().map(item -> readProperty(item, "resourceType")).anyMatch("BLOG"::equals));
        assertTrue(resources.stream().map(item -> readProperty(item, "resourceType")).anyMatch("COMMUNITY"::equals));
        assertTrue(resources.stream().map(item -> readProperty(item, "resourceType")).anyMatch("CODEPEN"::equals));
        verify(snapshotMapper).insert(any(AiGrowthCoachSnapshot.class));
        verify(actionMapper).deleteBySnapshotId(501L);
        verify(actionMapper, atLeast(1)).insert(any(AiGrowthCoachAction.class));
    }

    @Test
    void getFailuresShouldReturnFailedAndFallbackSnapshots() throws Exception {
        AiGrowthCoachSnapshot failed = new AiGrowthCoachSnapshot()
                .setId(801L)
                .setUserId(1001L)
                .setSceneScope("CAREER")
                .setStatus("FAILED")
                .setFailReason("工作流超时")
                .setGeneratedAt(LocalDateTime.now().minusHours(2));
        AiGrowthCoachSnapshot fallback = new AiGrowthCoachSnapshot()
                .setId(802L)
                .setUserId(1002L)
                .setSceneScope("HYBRID")
                .setStatus("READY")
                .setFallbackOnly(true)
                .setHeadline("当前为规则兜底结果")
                .setGeneratedAt(LocalDateTime.now().minusHours(1));
        when(snapshotMapper.selectFailures(50)).thenReturn(List.of(failed, fallback));

        Method method = service.getClass().getMethod("getFailures");
        List<?> failures = (List<?>) method.invoke(service);

        assertEquals(2, failures.size());
        assertEquals(801L, readProperty(failures.get(0), "snapshotId"));
        assertEquals("FAILED", readProperty(failures.get(0), "status"));
        assertEquals(Boolean.TRUE, readProperty(failures.get(1), "fallbackOnly"));
    }

    @Test
    void replanShouldReturnCompressedActionsWhenWorkflowUnavailable() throws Exception {
        Long userId = 1001L;
        AiGrowthCoachSnapshot snapshot = new AiGrowthCoachSnapshot()
                .setId(501L)
                .setUserId(userId)
                .setSceneScope("HYBRID")
                .setHeadline("本周建议优先补 Redis 与系统设计")
                .setSummaryJson("{\"summary\":\"建议先做高收益动作\",\"suggestedQuestions\":[\"如果我只剩 70 分钟怎么办？\"]}")
                .setSourceDigestJson("{\"targetRole\":\"Java后端\"}")
                .setStatus("READY");
        when(snapshotMapper.selectById(501L)).thenReturn(snapshot);
        when(actionMapper.selectBySnapshotId(501L)).thenReturn(List.of(
                new AiGrowthCoachAction().setId(901L).setSnapshotId(501L).setUserId(userId)
                        .setTitle("补强 Redis 专项").setDescription("优先补齐岗位短板").setPriority("P0")
                        .setActionType("skill_gap").setTargetRoute("/interview")
                        .setReason("Redis 是当前最关键的短板").setExpectedGain("提升面试命中率")
                        .setEstimatedMinutes(60).setStatus("TODO").setSortOrder(1),
                new AiGrowthCoachAction().setId(902L).setSnapshotId(501L).setUserId(userId)
                        .setTitle("清理逾期复习题").setDescription("回收知识点").setPriority("P1")
                        .setActionType("review").setTargetRoute("/interview/review?type=overdue")
                        .setReason("当前有逾期题").setExpectedGain("恢复知识稳定度")
                        .setEstimatedMinutes(35).setStatus("TODO").setSortOrder(2),
                new AiGrowthCoachAction().setId(903L).setSnapshotId(501L).setUserId(userId)
                        .setTitle("补齐本周计划节奏").setDescription("恢复执行节奏").setPriority("P2")
                        .setActionType("plan").setTargetRoute("/plan")
                        .setReason("计划进度偏慢").setExpectedGain("恢复执行曲线")
                        .setEstimatedMinutes(20).setStatus("TODO").setSortOrder(3)
        ));

        Object response = invokeReplan(userId, 501L, "hybrid", 70);

        assertNotNull(response);
        assertEquals(501L, readProperty(response, "snapshotId"));
        assertEquals(70, readProperty(response, "availableMinutes"));
        assertEquals(115, readProperty(response, "originalTotalMinutes"));
        assertEquals(Boolean.TRUE, readProperty(response, "fallbackOnly"));
        assertTrue(String.valueOf(readProperty(response, "summary")).contains("70"));
        assertEquals(1, invokeListGetter(response, "getActions").size());
        assertEquals(2, invokeListGetter(response, "getDeferredActions").size());
        assertEquals("补强 Redis 专项", readProperty(invokeListGetter(response, "getActions").get(0), "title"));
    }

    @Test
    void replanShouldPersistPreviewLogWhenReturningResult() throws Exception {
        Long userId = 1001L;
        AiGrowthCoachSnapshot snapshot = new AiGrowthCoachSnapshot()
                .setId(601L)
                .setUserId(userId)
                .setSceneScope("HYBRID")
                .setHeadline("本周建议先保留高收益动作")
                .setSummaryJson("{\"summary\":\"优先做高收益动作\"}")
                .setSourceDigestJson("{\"targetRole\":\"Java后端\"}")
                .setStatus("READY");
        when(snapshotMapper.selectById(601L)).thenReturn(snapshot);
        when(actionMapper.selectBySnapshotId(601L)).thenReturn(List.of(
                new AiGrowthCoachAction().setId(1001L).setSnapshotId(601L).setUserId(userId)
                        .setTitle("补强 Redis 专项").setPriority("P0").setEstimatedMinutes(60).setStatus("TODO").setSortOrder(1),
                new AiGrowthCoachAction().setId(1002L).setSnapshotId(601L).setUserId(userId)
                        .setTitle("清理逾期复习题").setPriority("P1").setEstimatedMinutes(35).setStatus("TODO").setSortOrder(2)
        ));
        DynamicMapperTracker tracker = attachDynamicMapper(service, "replanLogMapper", Map.of(
                "insert", 1,
                "countAll", 0L,
                "countToday", 0L,
                "countFallback", 0L,
                "avgCompressionRate", 0D
        ));

        Object response = invokeReplan(userId, 601L, "hybrid", 70);

        assertNotNull(response);
        assertEquals(1, tracker.invocationCount("insert"));
        Object log = tracker.firstArgument("insert");
        assertEquals(userId, readProperty(log, "userId"));
        assertEquals(601L, readProperty(log, "snapshotId"));
        assertEquals("HYBRID", readProperty(log, "sceneScope"));
        assertEquals(70, readProperty(log, "availableMinutes"));
        assertEquals(95, readProperty(log, "originalTotalMinutes"));
        assertEquals(1, readProperty(log, "selectedCount"));
        assertEquals(1, readProperty(log, "deferredCount"));
        assertEquals(Boolean.TRUE, readProperty(log, "fallbackOnly"));
    }

    @Test
    void replanShouldRejectForeignSnapshot() throws Exception {
        Long userId = 1001L;
        when(snapshotMapper.selectById(501L)).thenReturn(new AiGrowthCoachSnapshot()
                .setId(501L)
                .setUserId(2002L)
                .setSceneScope("CAREER")
                .setStatus("READY"));

        InvocationTargetException exception = assertThrows(InvocationTargetException.class,
                () -> invokeReplan(userId, 501L, "career", 90));

        assertTrue(exception.getCause() instanceof BusinessException);
        assertTrue(exception.getCause().getMessage().contains("无权"));
        verify(actionMapper, never()).selectBySnapshotId(501L);
    }

    @Test
    void getStatisticsShouldIncludeReplanMetrics() throws Exception {
        DynamicMapperTracker tracker = attachDynamicMapper(service, "replanLogMapper", Map.of(
                "countAll", 18L,
                "countToday", 5L,
                "countFallback", 6L,
                "avgCompressionRate", 32.5D,
                "insert", 1
        ));
        when(snapshotMapper.countAll()).thenReturn(20L);
        when(snapshotMapper.countToday()).thenReturn(4L);
        when(snapshotMapper.countFailed()).thenReturn(2L);
        when(snapshotMapper.countFallback()).thenReturn(3L);
        when(actionMapper.countAll()).thenReturn(10L);
        when(actionMapper.countDone()).thenReturn(4L);
        when(chatSessionMapper.countAll()).thenReturn(8L);
        when(chatMessageMapper.countAll()).thenReturn(24L);
        when(snapshotMapper.countBySceneScope("LEARNING")).thenReturn(6L);
        when(snapshotMapper.countBySceneScope("CAREER")).thenReturn(5L);
        when(snapshotMapper.countBySceneScope("HYBRID")).thenReturn(9L);

        Object response = service.getStatistics();

        assertNotNull(response);
        assertEquals(18L, readProperty(response, "totalReplans"));
        assertEquals(5L, readProperty(response, "todayReplans"));
        assertEquals(33.33D, readProperty(response, "replanFallbackRate"));
        assertEquals(32.5D, readProperty(response, "avgCompressionRate"));
        assertEquals(0, tracker.invocationCount("insert"));
    }

    @Test
    void chatShouldCreateSessionAndFallbackAssistantMessageWhenWorkflowReplyMissing() {
        Long userId = 1001L;
        AiGrowthCoachChatRequest request = new AiGrowthCoachChatRequest();
        request.setSnapshotId(501L);
        request.setScene("career");
        request.setMessage("如果我这周只有5小时，应该怎么压缩安排？");

        AiGrowthCoachSnapshot snapshot = new AiGrowthCoachSnapshot()
                .setId(501L)
                .setUserId(userId)
                .setSceneScope("CAREER")
                .setHeadline("本周建议优先补 Redis 与系统设计")
                .setSummaryJson("{\"focus\":\"Redis 与系统设计\"}")
                .setSourceDigestJson("{\"targetRole\":\"Java后端\"}")
                .setStatus("READY");
        when(snapshotMapper.selectById(501L)).thenReturn(snapshot);
        when(chatSessionMapper.selectLatestBySnapshotId(userId, 501L)).thenReturn(null);

        doAnswer(invocation -> {
            AiGrowthCoachChatSession session = invocation.getArgument(0);
            session.setId(601L);
            return 1;
        }).when(chatSessionMapper).insert(any(AiGrowthCoachChatSession.class));
        doAnswer(invocation -> {
            AiGrowthCoachChatMessage message = invocation.getArgument(0);
            if (message.getId() == null) {
                message.setId(System.nanoTime());
            }
            return 1;
        }).when(chatMessageMapper).insert(any(AiGrowthCoachChatMessage.class));

        when(workflowService.chat(any())).thenReturn(new GrowthCoachChatResult());
        when(chatMessageMapper.selectBySessionId(601L)).thenReturn(List.of(
                new AiGrowthCoachChatMessage().setId(701L).setSessionId(601L).setUserId(userId).setRole("user").setContent(request.getMessage()).setStatus("SUCCESS"),
                new AiGrowthCoachChatMessage().setId(702L).setSessionId(601L).setUserId(userId).setRole("assistant").setContent("建议保留 Redis 和系统设计两个高收益动作。").setStatus("SUCCESS")
        ));

        AiGrowthCoachChatResponse response = service.chat(userId, request);

        assertNotNull(response);
        assertEquals(601L, response.getSessionId());
        assertTrue(response.getReply().contains("Redis"));
        assertEquals(2, response.getMessages().size());
        verify(chatSessionMapper).insert(any(AiGrowthCoachChatSession.class));
        verify(chatMessageMapper, atLeast(1)).insert(any(AiGrowthCoachChatMessage.class));
        verify(chatSessionMapper, never()).updateTitle(anyLong(), eq(""));
    }

    private List<?> invokeListGetter(Object target, String getterName) throws Exception {
        Method method = target.getClass().getMethod(getterName);
        return (List<?>) method.invoke(target);
    }

    private Object readProperty(Object target, String propertyName) {
        try {
            String getterName = "get" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
            Method method = target.getClass().getMethod(getterName);
            return method.invoke(target);
        } catch (Exception ex) {
            throw new AssertionError("读取属性失败: " + propertyName, ex);
        }
    }

    private Object invokeReplan(Long userId, Long snapshotId, String scene, int availableMinutes) throws Exception {
        Class<?> requestClass = Class.forName("com.xiaou.aigrowth.dto.request.AiGrowthCoachReplanRequest");
        Object request = requestClass.getDeclaredConstructor().newInstance();
        requestClass.getMethod("setSnapshotId", Long.class).invoke(request, snapshotId);
        requestClass.getMethod("setScene", String.class).invoke(request, scene);
        requestClass.getMethod("setAvailableMinutes", Integer.class).invoke(request, availableMinutes);
        Method method = service.getClass().getMethod("replan", Long.class, requestClass);
        return method.invoke(service, userId, request);
    }

    private DynamicMapperTracker attachDynamicMapper(Object target, String fieldName, Map<String, Object> returnValues) throws Exception {
        Field field = findField(target.getClass(), fieldName);
        assertNotNull(field, "缺少字段: " + fieldName);
        field.setAccessible(true);
        DynamicMapperTracker tracker = new DynamicMapperTracker(returnValues);
        Object proxy = Proxy.newProxyInstance(field.getType().getClassLoader(), new Class[]{field.getType()}, tracker);
        field.set(target, proxy);
        return tracker;
    }

    private Field findField(Class<?> type, String fieldName) {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static final class DynamicMapperTracker implements InvocationHandler {

        private final Map<String, Object> returnValues;
        private final Map<String, List<Object[]>> invocations = new HashMap<>();

        private DynamicMapperTracker(Map<String, Object> returnValues) {
            this.returnValues = returnValues == null ? Map.of() : returnValues;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String methodName = method.getName();
            if ("toString".equals(methodName)) {
                return "DynamicMapperTrackerProxy";
            }
            if ("hashCode".equals(methodName)) {
                return System.identityHashCode(proxy);
            }
            if ("equals".equals(methodName)) {
                return proxy == (args == null || args.length == 0 ? null : args[0]);
            }
            invocations.computeIfAbsent(methodName, key -> new ArrayList<>())
                    .add(args == null ? new Object[0] : args.clone());
            if (returnValues.containsKey(methodName)) {
                return returnValues.get(methodName);
            }
            return defaultValue(method.getReturnType());
        }

        private int invocationCount(String methodName) {
            return invocations.getOrDefault(methodName, List.of()).size();
        }

        private Object firstArgument(String methodName) {
            List<Object[]> arguments = invocations.getOrDefault(methodName, List.of());
            if (arguments.isEmpty() || arguments.get(0).length == 0) {
                return null;
            }
            return arguments.get(0)[0];
        }

        private Object defaultValue(Class<?> type) {
            if (!type.isPrimitive()) {
                return null;
            }
            if (type == boolean.class) {
                return false;
            }
            if (type == byte.class) {
                return (byte) 0;
            }
            if (type == short.class) {
                return (short) 0;
            }
            if (type == int.class) {
                return 0;
            }
            if (type == long.class) {
                return 0L;
            }
            if (type == float.class) {
                return 0F;
            }
            if (type == double.class) {
                return 0D;
            }
            if (type == char.class) {
                return '\0';
            }
            return null;
        }
    }
}
