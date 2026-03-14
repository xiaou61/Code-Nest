package com.xiaou.aigrowth.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.xiaou.ai.dto.growthcoach.GrowthCoachChatResult;
import com.xiaou.ai.dto.growthcoach.GrowthCoachActionReplanRequest;
import com.xiaou.ai.dto.growthcoach.GrowthCoachActionReplanResult;
import com.xiaou.ai.dto.growthcoach.GrowthCoachSnapshotAiRequest;
import com.xiaou.ai.dto.growthcoach.GrowthCoachSnapshotAiResult;
import com.xiaou.ai.dto.jobbattle.JobBattleResumeMatchResult;
import com.xiaou.ai.service.AiGrowthCoachWorkflowService;
import com.xiaou.aigrowth.domain.AiGrowthCoachAction;
import com.xiaou.aigrowth.domain.AiGrowthCoachChatMessage;
import com.xiaou.aigrowth.domain.AiGrowthCoachChatSession;
import com.xiaou.aigrowth.domain.AiGrowthCoachConfig;
import com.xiaou.aigrowth.domain.AiGrowthCoachReplanLog;
import com.xiaou.aigrowth.domain.AiGrowthCoachSnapshot;
import com.xiaou.aigrowth.dto.request.AiGrowthCoachChatRequest;
import com.xiaou.aigrowth.dto.request.AiGrowthCoachConfigUpdateRequest;
import com.xiaou.aigrowth.dto.request.AiGrowthCoachReplanRequest;
import com.xiaou.aigrowth.dto.request.AiGrowthCoachRefreshRequest;
import com.xiaou.aigrowth.dto.response.AiGrowthCoachActionResponse;
import com.xiaou.aigrowth.dto.response.AiGrowthCoachChatMessageResponse;
import com.xiaou.aigrowth.dto.response.AiGrowthCoachChatResponse;
import com.xiaou.aigrowth.dto.response.AiGrowthCoachConfigResponse;
import com.xiaou.aigrowth.dto.response.AiGrowthCoachFailureResponse;
import com.xiaou.aigrowth.dto.response.AiGrowthCoachReplanResponse;
import com.xiaou.aigrowth.dto.response.AiGrowthCoachResourceResponse;
import com.xiaou.aigrowth.dto.response.AiGrowthCoachSnapshotDetailResponse;
import com.xiaou.aigrowth.dto.response.AiGrowthCoachStatisticsResponse;
import com.xiaou.aigrowth.mapper.AiGrowthCoachActionMapper;
import com.xiaou.aigrowth.mapper.AiGrowthCoachChatMessageMapper;
import com.xiaou.aigrowth.mapper.AiGrowthCoachChatSessionMapper;
import com.xiaou.aigrowth.mapper.AiGrowthCoachConfigMapper;
import com.xiaou.aigrowth.mapper.AiGrowthCoachReplanLogMapper;
import com.xiaou.aigrowth.mapper.AiGrowthCoachSnapshotMapper;
import com.xiaou.aigrowth.service.AiGrowthCoachService;
import com.xiaou.blog.dto.ArticleListRequest;
import com.xiaou.blog.dto.ArticleSimpleResponse;
import com.xiaou.blog.service.BlogArticleService;
import com.xiaou.codepen.dto.CodePenDetailResponse;
import com.xiaou.codepen.service.CodePenService;
import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.exception.BusinessException;
import com.xiaou.community.dto.CommunityPostResponse;
import com.xiaou.community.service.CommunityHotPostService;
import com.xiaou.flashcard.dto.response.FlashcardStudyStatsVO;
import com.xiaou.flashcard.service.FlashcardStudyService;
import com.xiaou.interview.dto.ReviewStatsResponse;
import com.xiaou.interview.service.InterviewLearnRecordService;
import com.xiaou.interview.service.InterviewMasteryService;
import com.xiaou.learningasset.dto.request.LearningAssetRecordQueryRequest;
import com.xiaou.learningasset.dto.response.LearningAssetRecordSummaryResponse;
import com.xiaou.learningasset.service.LearningAssetService;
import com.xiaou.mockinterview.domain.CareerLoopSnapshot;
import com.xiaou.mockinterview.dto.response.CareerLoopCurrentResponse;
import com.xiaou.mockinterview.dto.response.JobBattleMatchEngineResult;
import com.xiaou.mockinterview.service.CareerLoopService;
import com.xiaou.mockinterview.service.JobBattleService;
import com.xiaou.oj.dto.OjStatisticsVO;
import com.xiaou.oj.service.OjSubmissionService;
import com.xiaou.plan.dto.PlanStatsResponse;
import com.xiaou.plan.service.PlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/**
 * AI成长教练服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiGrowthCoachServiceImpl implements AiGrowthCoachService {

    private static final String SCENE_HYBRID = "HYBRID";
    private static final String STATUS_READY = "READY";
    private static final String ACTION_TODO = "TODO";
    private static final int RESOURCE_LIMIT = 3;
    private static final int FAILURE_LIMIT = 50;

    private final AiGrowthCoachSnapshotMapper snapshotMapper;
    private final AiGrowthCoachActionMapper actionMapper;
    private final AiGrowthCoachChatSessionMapper chatSessionMapper;
    private final AiGrowthCoachChatMessageMapper chatMessageMapper;
    private final AiGrowthCoachConfigMapper configMapper;
    private final AiGrowthCoachReplanLogMapper replanLogMapper;
    private final AiGrowthCoachWorkflowService workflowService;
    private final PlanService planService;
    private final FlashcardStudyService flashcardStudyService;
    private final InterviewMasteryService interviewMasteryService;
    private final InterviewLearnRecordService interviewLearnRecordService;
    private final OjSubmissionService ojSubmissionService;
    private final CareerLoopService careerLoopService;
    private final JobBattleService jobBattleService;
    private final LearningAssetService learningAssetService;
    private final BlogArticleService blogArticleService;
    private final CommunityHotPostService communityHotPostService;
    private final CodePenService codePenService;

    private final AiGrowthCoachRuleEngine ruleEngine = new AiGrowthCoachRuleEngine();

    @Override
    public AiGrowthCoachSnapshotDetailResponse getSummary(Long userId, String scene) {
        String sceneScope = normalizeScene(scene);
        assertSceneEnabled(sceneScope, loadConfigMap());
        AiGrowthCoachSnapshot latest = snapshotMapper.selectLatestReady(userId, sceneScope);
        if (latest == null || isExpired(latest)) {
            AiGrowthCoachRefreshRequest request = new AiGrowthCoachRefreshRequest();
            request.setScene(sceneScope);
            request.setForce(true);
            return refresh(userId, request);
        }
        return toDetailResponse(latest);
    }

    @Override
    public AiGrowthCoachSnapshotDetailResponse getSnapshotDetail(Long userId, Long snapshotId) {
        AiGrowthCoachSnapshot snapshot = requireSnapshot(snapshotId);
        if (!Objects.equals(snapshot.getUserId(), userId)) {
            throw new BusinessException("无权查看该教练快照");
        }
        return toDetailResponse(snapshot);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiGrowthCoachSnapshotDetailResponse refresh(Long userId, AiGrowthCoachRefreshRequest request) {
        String sceneScope = normalizeScene(request == null ? null : request.getScene());
        Map<String, String> configMap = loadConfigMap();
        assertSceneEnabled(sceneScope, configMap);
        boolean force = request != null && Boolean.TRUE.equals(request.getForce());
        AiGrowthCoachSnapshot latest = snapshotMapper.selectLatestReady(userId, sceneScope);
        if (!force && latest != null && !isExpired(latest)) {
            return toDetailResponse(latest);
        }
        List<String> suggestedQuestions = parseSuggestedQuestions(configMap.get("chat_suggested_questions"));
        AiGrowthCoachRuleEngine.SourceBundle sourceBundle = loadSourceBundle(userId);
        AiGrowthCoachRuleEngine.RuleSnapshotResult ruleResult = ruleEngine.build(
                sceneScope,
                sourceBundle,
                getConfigInt(configMap, "max_actions", 4),
                suggestedQuestions
        );

        GrowthCoachSnapshotAiResult aiResult = workflowService.generateSnapshot(new GrowthCoachSnapshotAiRequest()
                .setScene(sceneScope)
                .setHeadline(ruleResult.headline())
                .setSummaryJson(ruleResult.summaryJson())
                .setSourceDigestJson(ruleResult.sourceDigestJson())
                .setActionJson(JSONUtil.toJsonStr(ruleResult.actions())));

        SnapshotPayload payload = mergePayload(ruleResult, aiResult, suggestedQuestions);
        AiGrowthCoachSnapshot snapshot = new AiGrowthCoachSnapshot()
                .setUserId(userId)
                .setSceneScope(sceneScope)
                .setSnapshotDate(LocalDate.now())
                .setLearningScore(ruleResult.learningScore())
                .setCareerScore(ruleResult.careerScore())
                .setExecutionScore(ruleResult.executionScore())
                .setOverallScore(ruleResult.overallScore())
                .setRiskLevel(payload.riskLevel())
                .setHeadline(payload.headline())
                .setSummaryJson(payload.summaryJson())
                .setSourceDigestJson(ruleResult.sourceDigestJson())
                .setFallbackOnly(payload.fallbackOnly())
                .setStatus(STATUS_READY)
                .setGeneratedAt(LocalDateTime.now())
                .setExpireAt(LocalDateTime.now().plusHours(getConfigInt(configMap, "snapshot_expire_hours", 12)));
        snapshotMapper.insert(snapshot);
        actionMapper.deleteBySnapshotId(snapshot.getId());
        int sort = 1;
        for (AiGrowthCoachRuleEngine.ActionDraft draft : ruleResult.actions()) {
            actionMapper.insert(new AiGrowthCoachAction()
                    .setSnapshotId(snapshot.getId())
                    .setUserId(userId)
                    .setTitle(draft.title())
                    .setDescription(draft.description())
                    .setPriority(draft.priority())
                    .setActionType(draft.actionType())
                    .setTargetRoute(draft.targetRoute())
                    .setReason(draft.reason())
                    .setExpectedGain(draft.expectedGain())
                    .setEstimatedMinutes(draft.estimatedMinutes())
                    .setStatus(ACTION_TODO)
                    .setSortOrder(sort++));
        }
        return toDetailResponse(snapshot);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiGrowthCoachChatResponse chat(Long userId, AiGrowthCoachChatRequest request) {
        AiGrowthCoachSnapshot snapshot = requireSnapshot(request.getSnapshotId());
        if (!Objects.equals(snapshot.getUserId(), userId)) {
            throw new BusinessException("无权查看该教练快照");
        }
        AiGrowthCoachChatSession session = resolveSession(userId, snapshot, request);
        chatMessageMapper.insert(new AiGrowthCoachChatMessage()
                .setSessionId(session.getId())
                .setUserId(userId)
                .setRole("user")
                .setContent(request.getMessage())
                .setStatus("SUCCESS"));

        GrowthCoachChatResult aiResult = workflowService.chat(new com.xiaou.ai.dto.growthcoach.GrowthCoachChatRequest()
                .setScene(normalizeScene(request.getScene() == null ? snapshot.getSceneScope() : request.getScene()))
                .setMessage(request.getMessage())
                .setHeadline(snapshot.getHeadline())
                .setSummaryJson(snapshot.getSummaryJson())
                .setSourceDigestJson(snapshot.getSourceDigestJson()));
        String reply = aiResult == null || StrUtil.isBlank(aiResult.getReply())
                ? buildFallbackChatReply(snapshot, request.getMessage())
                : aiResult.getReply();
        List<String> suggestedQuestions = aiResult == null || aiResult.getSuggestedQuestions() == null || aiResult.getSuggestedQuestions().isEmpty()
                ? parseSuggestedQuestions(loadConfigMap().get("chat_suggested_questions"))
                : aiResult.getSuggestedQuestions();

        chatMessageMapper.insert(new AiGrowthCoachChatMessage()
                .setSessionId(session.getId())
                .setUserId(userId)
                .setRole("assistant")
                .setContent(reply)
                .setReferenceJson(snapshot.getSourceDigestJson())
                .setStatus("SUCCESS"));
        chatSessionMapper.updateLastMessageAt(session.getId(), LocalDateTime.now());
        return buildChatResponse(session, snapshot, reply, suggestedQuestions);
    }

    @Override
    public AiGrowthCoachReplanResponse replan(Long userId, AiGrowthCoachReplanRequest request) {
        if (request == null || request.getSnapshotId() == null) {
            throw new BusinessException("快照不存在");
        }
        if (request.getAvailableMinutes() == null || request.getAvailableMinutes() <= 0) {
            throw new BusinessException("可用时长必须大于 0");
        }
        AiGrowthCoachSnapshot snapshot = requireSnapshot(request.getSnapshotId());
        if (!Objects.equals(snapshot.getUserId(), userId)) {
            throw new BusinessException("无权查看该教练快照");
        }
        List<AiGrowthCoachAction> actions = actionMapper.selectBySnapshotId(snapshot.getId());
        AiGrowthCoachRuleEngine.ActionReplanResult ruleResult = ruleEngine.replan(actions, request.getAvailableMinutes());
        GrowthCoachActionReplanResult aiResult = workflowService.replan(new GrowthCoachActionReplanRequest()
                .setScene(normalizeScene(StrUtil.blankToDefault(request.getScene(), snapshot.getSceneScope())))
                .setAvailableMinutes(request.getAvailableMinutes())
                .setHeadline(snapshot.getHeadline())
                .setSummaryJson(snapshot.getSummaryJson())
                .setSourceDigestJson(snapshot.getSourceDigestJson())
                .setSelectedActionJson(JSONUtil.toJsonStr(toActionResponses(ruleResult.selectedActions())))
                .setDeferredActionJson(JSONUtil.toJsonStr(toActionResponses(ruleResult.deferredActions()))));
        AiGrowthCoachReplanResponse response = new AiGrowthCoachReplanResponse();
        response.setSnapshotId(snapshot.getId());
        response.setScene(snapshot.getSceneScope());
        response.setAvailableMinutes(request.getAvailableMinutes());
        response.setOriginalTotalMinutes(ruleResult.originalTotalMinutes());
        response.setFallbackOnly(aiResult == null || StrUtil.isBlank(aiResult.getSummary()));
        response.setSummary(StrUtil.blankToDefault(aiResult == null ? null : aiResult.getSummary(), ruleResult.summary()));
        response.setSuggestedQuestions(aiResult == null || aiResult.getSuggestedQuestions() == null || aiResult.getSuggestedQuestions().isEmpty()
                ? buildFallbackReplanQuestions(request.getAvailableMinutes())
                : aiResult.getSuggestedQuestions());
        response.setActions(toActionResponses(ruleResult.selectedActions()));
        response.setDeferredActions(toActionResponses(ruleResult.deferredActions()));
        replanLogMapper.insert(new AiGrowthCoachReplanLog()
                .setUserId(userId)
                .setSnapshotId(snapshot.getId())
                .setSceneScope(snapshot.getSceneScope())
                .setAvailableMinutes(request.getAvailableMinutes())
                .setOriginalTotalMinutes(ruleResult.originalTotalMinutes())
                .setSelectedCount(response.getActions() == null ? 0 : response.getActions().size())
                .setDeferredCount(response.getDeferredActions() == null ? 0 : response.getDeferredActions().size())
                .setFallbackOnly(response.getFallbackOnly()));
        return response;
    }

    @Override
    public AiGrowthCoachChatResponse getChatSession(Long userId, Long sessionId) {
        AiGrowthCoachChatSession session = chatSessionMapper.selectByIdAndUserId(sessionId, userId);
        if (session == null) {
            throw new BusinessException("对话会话不存在");
        }
        return buildChatResponse(session, requireSnapshot(session.getSnapshotId()), null,
                parseSuggestedQuestions(loadConfigMap().get("chat_suggested_questions")));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateActionStatus(Long userId, Long actionId, String status) {
        AiGrowthCoachAction action = actionMapper.selectById(actionId);
        if (action == null || !Objects.equals(action.getUserId(), userId)) {
            throw new BusinessException("动作不存在");
        }
        if (actionMapper.updateStatus(actionId, userId, normalizeActionStatus(status)) <= 0) {
            throw new BusinessException("动作更新失败");
        }
    }

    @Override
    public AiGrowthCoachStatisticsResponse getStatistics() {
        AiGrowthCoachStatisticsResponse response = new AiGrowthCoachStatisticsResponse();
        long totalSnapshots = snapshotMapper.countAll();
        long totalSessions = chatSessionMapper.countAll();
        long totalMessages = chatMessageMapper.countAll();
        long actionTotal = actionMapper.countAll();
        response.setTotalSnapshots(totalSnapshots);
        response.setTodaySnapshots(snapshotMapper.countToday());
        response.setFailedSnapshots(snapshotMapper.countFailed());
        response.setFallbackSnapshots(snapshotMapper.countFallback());
        response.setTotalChatSessions(totalSessions);
        response.setTotalMessages(totalMessages);
        response.setAvgMessagesPerSession(totalSessions <= 0 ? 0D : round(totalMessages * 1.0 / totalSessions));
        response.setActionDoneRate(actionTotal <= 0 ? 0D : round(actionMapper.countDone() * 100.0 / actionTotal));
        long totalReplans = replanLogMapper.countAll();
        response.setTotalReplans(totalReplans);
        response.setTodayReplans(replanLogMapper.countToday());
        response.setReplanFallbackRate(totalReplans <= 0 ? 0D : round(replanLogMapper.countFallback() * 100.0 / totalReplans));
        response.setAvgCompressionRate(round(nvl(replanLogMapper.avgCompressionRate())));
        Map<String, Long> sceneDistribution = new LinkedHashMap<>();
        sceneDistribution.put("LEARNING", snapshotMapper.countBySceneScope("LEARNING"));
        sceneDistribution.put("CAREER", snapshotMapper.countBySceneScope("CAREER"));
        sceneDistribution.put(SCENE_HYBRID, snapshotMapper.countBySceneScope(SCENE_HYBRID));
        response.setSceneDistribution(sceneDistribution);
        return response;
    }

    @Override
    public List<AiGrowthCoachFailureResponse> getFailures() {
        List<AiGrowthCoachFailureResponse> response = new ArrayList<>();
        for (AiGrowthCoachSnapshot snapshot : snapshotMapper.selectFailures(FAILURE_LIMIT)) {
            AiGrowthCoachFailureResponse item = new AiGrowthCoachFailureResponse();
            item.setSnapshotId(snapshot.getId());
            item.setUserId(snapshot.getUserId());
            item.setScene(snapshot.getSceneScope());
            item.setStatus(snapshot.getStatus());
            item.setFallbackOnly(Boolean.TRUE.equals(snapshot.getFallbackOnly()));
            item.setHeadline(snapshot.getHeadline());
            item.setFailReason(snapshot.getFailReason());
            item.setGeneratedAt(snapshot.getGeneratedAt());
            response.add(item);
        }
        return response;
    }

    @Override
    public List<AiGrowthCoachConfigResponse> getConfigs() {
        List<AiGrowthCoachConfigResponse> response = new ArrayList<>();
        for (AiGrowthCoachConfig item : configMapper.selectAll()) {
            AiGrowthCoachConfigResponse dto = new AiGrowthCoachConfigResponse();
            dto.setId(item.getId());
            dto.setConfigKey(item.getConfigKey());
            dto.setConfigValue(item.getConfigValue());
            dto.setRemark(item.getRemark());
            dto.setStatus(item.getStatus());
            dto.setUpdateTime(item.getUpdateTime());
            response.add(dto);
        }
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateConfigs(List<AiGrowthCoachConfigUpdateRequest> requests) {
        if (requests == null) {
            return;
        }
        for (AiGrowthCoachConfigUpdateRequest request : requests) {
            if (request == null || StrUtil.isBlank(request.getConfigKey())) {
                continue;
            }
            AiGrowthCoachConfig config = new AiGrowthCoachConfig()
                    .setConfigKey(request.getConfigKey())
                    .setConfigValue(request.getConfigValue())
                    .setRemark(request.getRemark())
                    .setStatus(StrUtil.blankToDefault(request.getStatus(), "ENABLED"));
            if (configMapper.selectByKey(request.getConfigKey()) == null) {
                configMapper.insert(config);
            } else {
                configMapper.updateByKey(config);
            }
        }
    }

    private AiGrowthCoachRuleEngine.SourceBundle loadSourceBundle(Long userId) {
        PlanStatsResponse planStats = safeCall(() -> planService.getStatsOverview(userId), PlanStatsResponse.builder().build());
        FlashcardStudyStatsVO flashcardStats = safeCall(() -> flashcardStudyService.getStudyStats(userId, null), new FlashcardStudyStatsVO());
        ReviewStatsResponse reviewStats = safeCall(() -> interviewMasteryService.getReviewStats(userId), new ReviewStatsResponse());
        Integer totalLearned = safeCall(() -> interviewLearnRecordService.getTotalLearnedCount(userId), 0);
        OjStatisticsVO ojStats = safeCall(() -> ojSubmissionService.getStatistics(userId), new OjStatisticsVO());
        CareerLoopCurrentResponse current = safeCall(() -> careerLoopService.getCurrent(userId), null);
        JobBattleMatchEngineResult matchResult = safeCall(() -> jobBattleService.getLatestMatchEngineResult(userId), null);
        PageResult<LearningAssetRecordSummaryResponse> assetPage = safeCall(() -> {
            LearningAssetRecordQueryRequest query = new LearningAssetRecordQueryRequest();
            query.setPageNum(1);
            query.setPageSize(5);
            return learningAssetService.getRecordList(userId, query);
        }, PageResult.of(1, 5, 0L, Collections.emptyList()));

        String bestGapSkill = null;
        if (matchResult != null && matchResult.getRanking() != null && !matchResult.getRanking().isEmpty()) {
            JobBattleMatchEngineResult.TargetScore top = matchResult.getRanking().get(0);
            if (top.getTopGaps() != null && !top.getTopGaps().isEmpty()) {
                JobBattleResumeMatchResult.Gap gap = top.getTopGaps().get(0);
                bestGapSkill = gap == null ? null : gap.getSkill();
            }
        }
        int publishedAssetCount = 0;
        if (assetPage.getRecords() != null) {
            for (LearningAssetRecordSummaryResponse item : assetPage.getRecords()) {
                if (item != null && "PUBLISHED".equalsIgnoreCase(item.getStatus())) {
                    publishedAssetCount++;
                }
            }
        }
        CareerLoopSnapshot careerSnapshot = current == null ? null : current.getSnapshot();
        return new AiGrowthCoachRuleEngine.SourceBundle(
                current != null && current.getSession() != null ? current.getSession().getTargetRole() : "通用方向",
                current != null && current.getSession() != null ? nvl(current.getSession().getHealthScore()) : 60,
                careerSnapshot == null ? 0 : nvl(careerSnapshot.getPlanProgress()),
                careerSnapshot == null ? 0 : nvl(careerSnapshot.getLatestMockScore()),
                planStats == null ? 0 : nvl(planStats.getWeekCheckinCount()),
                flashcardStats == null ? 0 : nvl(flashcardStats.getTodayLearnedCount()),
                flashcardStats == null ? 0 : nvl(flashcardStats.getTodayDueCount()),
                flashcardStats == null ? 0 : nvl(flashcardStats.getStreakDays()),
                reviewStats == null ? 0 : nvl(reviewStats.getOverdueCount()),
                totalLearned == null ? 0 : totalLearned,
                ojStats == null ? 0 : nvl(ojStats.getAcceptedProblems()),
                matchResult == null ? 0 : nvl(matchResult.getBestScore()),
                bestGapSkill,
                current == null || current.getRiskFlags() == null ? Collections.emptyList() : current.getRiskFlags(),
                publishedAssetCount
        );
    }

    private SnapshotPayload mergePayload(AiGrowthCoachRuleEngine.RuleSnapshotResult ruleResult,
                                         GrowthCoachSnapshotAiResult aiResult,
                                         List<String> suggestedQuestions) {
        JSONObject summary = parseObject(ruleResult.summaryJson());
        Set<String> riskFlags = new LinkedHashSet<>(parseArray(summary, "riskFlags"));
        if (aiResult != null && aiResult.getRiskFlags() != null) {
            riskFlags.addAll(aiResult.getRiskFlags());
        }
        JSONObject payloadSummary = new JSONObject();
        payloadSummary.set("summary", aiResult != null && StrUtil.isNotBlank(aiResult.getSummary()) ? aiResult.getSummary() : summary.getStr("summary"));
        payloadSummary.set("focusAreas", aiResult != null && aiResult.getFocusAreas() != null && !aiResult.getFocusAreas().isEmpty()
                ? aiResult.getFocusAreas() : parseArray(summary, "focusAreas"));
        payloadSummary.set("riskFlags", new ArrayList<>(riskFlags));
        payloadSummary.set("suggestedQuestions", aiResult != null && aiResult.getRecommendedQuestions() != null && !aiResult.getRecommendedQuestions().isEmpty()
                ? aiResult.getRecommendedQuestions() : suggestedQuestions);
        return new SnapshotPayload(
                StrUtil.blankToDefault(aiResult == null ? null : aiResult.getHeadline(), ruleResult.headline()),
                StrUtil.blankToDefault(aiResult == null ? null : aiResult.getRiskLevel(), ruleResult.riskLevel()),
                JSONUtil.toJsonStr(payloadSummary),
                aiResult == null
        );
    }

    private AiGrowthCoachSnapshotDetailResponse toDetailResponse(AiGrowthCoachSnapshot snapshot) {
        JSONObject summary = parseObject(snapshot.getSummaryJson());
        AiGrowthCoachSnapshotDetailResponse response = new AiGrowthCoachSnapshotDetailResponse();
        response.setSnapshotId(snapshot.getId());
        response.setScene(snapshot.getSceneScope());
        response.setStatus(snapshot.getStatus());
        response.setLearningScore(nvl(snapshot.getLearningScore()));
        response.setCareerScore(nvl(snapshot.getCareerScore()));
        response.setExecutionScore(nvl(snapshot.getExecutionScore()));
        response.setOverallScore(nvl(snapshot.getOverallScore()));
        response.setRiskLevel(StrUtil.blankToDefault(snapshot.getRiskLevel(), "MEDIUM"));
        response.setHeadline(snapshot.getHeadline());
        response.setSummary(summary.getStr("summary"));
        response.setFocusAreas(parseArray(summary, "focusAreas"));
        response.setRiskFlags(parseArray(summary, "riskFlags"));
        response.setSuggestedQuestions(parseArray(summary, "suggestedQuestions"));
        response.setSourceDigest(parseMap(snapshot.getSourceDigestJson()));
        response.setFallbackOnly(Boolean.TRUE.equals(snapshot.getFallbackOnly()));
        response.setResources(buildRecommendedResources(snapshot));
        response.setGeneratedAt(snapshot.getGeneratedAt());
        response.setExpireAt(snapshot.getExpireAt());
        List<AiGrowthCoachActionResponse> actions = new ArrayList<>();
        for (AiGrowthCoachAction item : actionMapper.selectBySnapshotId(snapshot.getId())) {
            actions.add(toActionResponse(item));
        }
        response.setActions(actions);
        return response;
    }

    private AiGrowthCoachChatResponse buildChatResponse(AiGrowthCoachChatSession session, AiGrowthCoachSnapshot snapshot,
                                                        String reply, List<String> suggestedQuestions) {
        List<AiGrowthCoachChatMessageResponse> messages = new ArrayList<>();
        for (AiGrowthCoachChatMessage item : chatMessageMapper.selectBySessionId(session.getId())) {
            AiGrowthCoachChatMessageResponse message = new AiGrowthCoachChatMessageResponse();
            message.setMessageId(item.getId());
            message.setRole(item.getRole());
            message.setContent(item.getContent());
            message.setStatus(item.getStatus());
            message.setCreateTime(item.getCreateTime());
            messages.add(message);
        }
        AiGrowthCoachChatResponse response = new AiGrowthCoachChatResponse();
        response.setSessionId(session.getId());
        response.setSnapshotId(snapshot.getId());
        response.setScene(snapshot.getSceneScope());
        response.setTitle(session.getTitle());
        response.setReply(reply == null && !messages.isEmpty() ? messages.get(messages.size() - 1).getContent() : reply);
        response.setSuggestedQuestions(suggestedQuestions);
        response.setMessages(messages);
        return response;
    }

    private List<AiGrowthCoachActionResponse> toActionResponses(List<AiGrowthCoachAction> actions) {
        List<AiGrowthCoachActionResponse> responses = new ArrayList<>();
        if (actions == null) {
            return responses;
        }
        for (AiGrowthCoachAction item : actions) {
            responses.add(toActionResponse(item));
        }
        return responses;
    }

    private AiGrowthCoachActionResponse toActionResponse(AiGrowthCoachAction item) {
        AiGrowthCoachActionResponse action = new AiGrowthCoachActionResponse();
        action.setActionId(item.getId());
        action.setTitle(item.getTitle());
        action.setDescription(item.getDescription());
        action.setPriority(item.getPriority());
        action.setActionType(item.getActionType());
        action.setTargetRoute(item.getTargetRoute());
        action.setReason(item.getReason());
        action.setExpectedGain(item.getExpectedGain());
        action.setEstimatedMinutes(item.getEstimatedMinutes());
        action.setStatus(item.getStatus());
        action.setSortOrder(item.getSortOrder());
        return action;
    }

    private AiGrowthCoachChatSession resolveSession(Long userId, AiGrowthCoachSnapshot snapshot, AiGrowthCoachChatRequest request) {
        AiGrowthCoachChatSession session = request.getSessionId() == null
                ? chatSessionMapper.selectLatestBySnapshotId(userId, snapshot.getId())
                : chatSessionMapper.selectByIdAndUserId(request.getSessionId(), userId);
        if (session != null) {
            return session;
        }
        AiGrowthCoachChatSession created = new AiGrowthCoachChatSession()
                .setUserId(userId)
                .setSnapshotId(snapshot.getId())
                .setSceneScope(normalizeScene(request.getScene() == null ? snapshot.getSceneScope() : request.getScene()))
                .setTitle(buildSessionTitle(request.getMessage()))
                .setStatus("ACTIVE")
                .setLastMessageAt(LocalDateTime.now());
        chatSessionMapper.insert(created);
        return created;
    }

    private String buildFallbackChatReply(AiGrowthCoachSnapshot snapshot, String message) {
        List<String> focusAreas = parseArray(parseObject(snapshot.getSummaryJson()), "focusAreas");
        String focus = focusAreas.isEmpty()
                ? StrUtil.blankToDefault(snapshot.getHeadline(), "当前高优先动作")
                : String.join("、", focusAreas.subList(0, Math.min(2, focusAreas.size())));
        if (message != null && message.contains("5小时")) {
            return "如果这周只有 5 小时，建议只保留 " + focus + " 两类动作，优先做 1 个专项补强 + 1 次复习清理，不必把所有建议一次做满。";
        }
        return "这条建议主要基于你最近的快照数据，当前最值得优先推进的是 " + focus + "。先把高收益动作做完，再回来刷新会更划算。";
    }

    private List<String> buildFallbackReplanQuestions(Integer availableMinutes) {
        int nextBudget = availableMinutes == null ? 30 : availableMinutes + 30;
        return List.of(
                "如果我再多 " + nextBudget + " 分钟，应该加哪一项？",
                "为什么优先保留这些动作？",
                "哪些动作可以明天再做？"
        );
    }

    private List<AiGrowthCoachResourceResponse> buildRecommendedResources(AiGrowthCoachSnapshot snapshot) {
        List<AiGrowthCoachResourceResponse> resources = new ArrayList<>();
        String focus = resolvePrimaryFocus(snapshot);

        LearningAssetRecordQueryRequest assetQuery = new LearningAssetRecordQueryRequest();
        assetQuery.setPageNum(1);
        assetQuery.setPageSize(RESOURCE_LIMIT);
        PageResult<LearningAssetRecordSummaryResponse> assetPage = safeCall(
                () -> learningAssetService.getRecordList(snapshot.getUserId(), assetQuery),
                PageResult.of(1, RESOURCE_LIMIT, 0L, Collections.emptyList())
        );
        if (assetPage.getRecords() != null) {
            for (LearningAssetRecordSummaryResponse item : assetPage.getRecords()) {
                if (item != null && "PUBLISHED".equalsIgnoreCase(item.getStatus())) {
                    resources.add(buildResource("ASSET", "学习资产", item.getSourceTitle(),
                            "最近沉淀的学习资产，可直接回看并承接训练结果。",
                            "/learning-assets?recordId=" + item.getRecordId(), focus));
                }
            }
        }

        ArticleListRequest articleRequest = new ArticleListRequest();
        articleRequest.setUserId(snapshot.getUserId());
        articleRequest.setStatus(1);
        articleRequest.setPageNum(1);
        articleRequest.setPageSize(RESOURCE_LIMIT);
        PageResult<ArticleSimpleResponse> articlePage = safeCall(
                () -> blogArticleService.getUserArticleList(articleRequest),
                PageResult.of(1, RESOURCE_LIMIT, 0L, Collections.emptyList())
        );
        if (articlePage.getRecords() != null) {
            for (ArticleSimpleResponse item : articlePage.getRecords()) {
                if (item != null && item.getId() != null && item.getUserId() != null) {
                    resources.add(buildResource("BLOG", "博客文章", item.getTitle(),
                            StrUtil.blankToDefault(item.getSummary(), "查看对应专题总结与复盘。"),
                            "/blog/" + item.getUserId() + "/article/" + item.getId(), focus));
                }
            }
        }

        for (CommunityPostResponse item : safeCall(() -> communityHotPostService.getHotPosts(RESOURCE_LIMIT), List.<CommunityPostResponse>of())) {
            if (item != null && item.getId() != null) {
                resources.add(buildResource("COMMUNITY", "社区帖子", item.getTitle(),
                        StrUtil.blankToDefault(item.getAiSummary(), abbreviate(item.getContent(), 42)),
                        "/community/posts/" + item.getId(), focus));
            }
        }

        for (CodePenDetailResponse item : safeCall(() -> codePenService.getRecommendList(RESOURCE_LIMIT), List.<CodePenDetailResponse>of())) {
            if (item != null && item.getId() != null) {
                resources.add(buildResource("CODEPEN", "CodePen 作品", item.getTitle(),
                        StrUtil.blankToDefault(item.getDescription(), "通过交互作品强化知识理解和表达。"),
                        "/codepen/" + item.getId(), focus));
            }
        }

        return resources;
    }

    private AiGrowthCoachResourceResponse buildResource(String type, String label, String title,
                                                        String summary, String route, String focus) {
        AiGrowthCoachResourceResponse response = new AiGrowthCoachResourceResponse();
        response.setResourceType(type);
        response.setSourceLabel(label);
        response.setTitle(StrUtil.blankToDefault(title, label));
        response.setSummary(StrUtil.blankToDefault(summary, "推荐回看该资源。"));
        response.setRoute(route);
        response.setReason(StrUtil.isBlank(focus) ? "适合作为当前阶段的补强材料" : "围绕当前优先关注的「" + focus + "」补充理解");
        return response;
    }

    private String resolvePrimaryFocus(AiGrowthCoachSnapshot snapshot) {
        List<String> focusAreas = parseArray(parseObject(snapshot.getSummaryJson()), "focusAreas");
        return focusAreas.isEmpty() ? null : focusAreas.get(0);
    }

    private String abbreviate(String text, int maxLength) {
        String safe = StrUtil.blankToDefault(text, "").trim();
        if (safe.length() <= maxLength) {
            return safe;
        }
        return safe.substring(0, maxLength) + "...";
    }

    private Map<String, String> loadConfigMap() {
        Map<String, String> map = new LinkedHashMap<>();
        for (AiGrowthCoachConfig config : configMapper.selectAll()) {
            if (config != null && StrUtil.isNotBlank(config.getConfigKey())) {
                map.put(config.getConfigKey(), config.getConfigValue());
            }
        }
        return map;
    }

    private List<String> parseSuggestedQuestions(String raw) {
        if (StrUtil.isBlank(raw)) {
            return List.of("为什么先做这个？", "如果我只有 5 小时怎么办？", "这个建议基于哪些数据？");
        }
        String[] parts = raw.split("\\|");
        List<String> result = new ArrayList<>();
        for (String item : parts) {
            if (StrUtil.isNotBlank(item)) {
                result.add(item.trim());
            }
        }
        return result;
    }

    private JSONObject parseObject(String jsonText) {
        if (StrUtil.isBlank(jsonText)) {
            return new JSONObject();
        }
        try {
            return JSONUtil.parseObj(jsonText);
        } catch (Exception ex) {
            return new JSONObject();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseMap(String jsonText) {
        if (StrUtil.isBlank(jsonText)) {
            return new LinkedHashMap<>();
        }
        try {
            return JSONUtil.parseObj(jsonText).toBean(LinkedHashMap.class);
        } catch (Exception ex) {
            return new LinkedHashMap<>();
        }
    }

    private List<String> parseArray(JSONObject json, String key) {
        JSONArray array = json == null ? null : json.getJSONArray(key);
        if (array == null) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (Object item : array) {
            if (item != null && StrUtil.isNotBlank(item.toString())) {
                result.add(item.toString());
            }
        }
        return result;
    }

    private String normalizeScene(String scene) {
        if (StrUtil.isBlank(scene)) {
            return SCENE_HYBRID;
        }
        String normalized = scene.trim().toUpperCase(Locale.ROOT);
        if (List.of("LEARNING", "CAREER", SCENE_HYBRID).contains(normalized)) {
            return normalized;
        }
        if ("STUDY".equals(normalized)) {
            return "LEARNING";
        }
        if ("JOB".equals(normalized) || "JOB_BATTLE".equals(normalized)) {
            return "CAREER";
        }
        return SCENE_HYBRID;
    }

    private String normalizeActionStatus(String status) {
        String normalized = StrUtil.blankToDefault(status, ACTION_TODO).trim().toUpperCase(Locale.ROOT);
        if (!List.of("TODO", "IN_PROGRESS", "DONE", "SKIPPED").contains(normalized)) {
            throw new BusinessException("不支持的动作状态");
        }
        return normalized;
    }

    private void assertSceneEnabled(String sceneScope, Map<String, String> configMap) {
        String key = switch (sceneScope) {
            case "LEARNING" -> "scene_enabled_learning";
            case "CAREER" -> "scene_enabled_career";
            default -> "scene_enabled_hybrid";
        };
        String value = configMap == null ? null : configMap.get(key);
        if (value != null && !"true".equalsIgnoreCase(value.trim())) {
            throw new BusinessException("当前教练场景未开启: " + sceneScope);
        }
    }

    private String buildSessionTitle(String message) {
        String safe = StrUtil.blankToDefault(message, "成长教练对话").trim();
        return safe.length() > 16 ? safe.substring(0, 16) : safe;
    }

    private AiGrowthCoachSnapshot requireSnapshot(Long snapshotId) {
        AiGrowthCoachSnapshot snapshot = snapshotMapper.selectById(snapshotId);
        if (snapshot == null) {
            throw new BusinessException("教练快照不存在");
        }
        return snapshot;
    }

    private boolean isExpired(AiGrowthCoachSnapshot snapshot) {
        return snapshot != null && snapshot.getExpireAt() != null && snapshot.getExpireAt().isBefore(LocalDateTime.now());
    }

    private int getConfigInt(Map<String, String> configMap, String key, int defaultValue) {
        try {
            return Integer.parseInt(StrUtil.blankToDefault(configMap.get(key), String.valueOf(defaultValue)));
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private int nvl(Integer value) {
        return value == null ? 0 : value;
    }

    private double nvl(Double value) {
        return value == null ? 0D : value;
    }

    private <T> T safeCall(Supplier<T> supplier, T fallback) {
        try {
            T value = supplier.get();
            return value == null ? fallback : value;
        } catch (Exception ex) {
            log.warn("AI成长教练聚合数据获取失败", ex);
            return fallback;
        }
    }

    private record SnapshotPayload(String headline, String riskLevel, String summaryJson, boolean fallbackOnly) {
    }
}
