package com.xiaou.mockinterview.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.exception.BusinessException;
import com.xiaou.common.utils.PageHelper;
import com.xiaou.interview.domain.InterviewQuestion;
import com.xiaou.interview.domain.InterviewQuestionSet;
import com.xiaou.interview.mapper.InterviewQuestionSetMapper;
import com.xiaou.mockinterview.domain.*;
import com.xiaou.mockinterview.dto.AIEvaluationResult;
import com.xiaou.mockinterview.dto.request.CareerLoopEventRequest;
import com.xiaou.mockinterview.dto.request.CreateInterviewRequest;
import com.xiaou.mockinterview.dto.request.InterviewHistoryRequest;
import com.xiaou.mockinterview.dto.request.SubmitAnswerRequest;
import com.xiaou.mockinterview.dto.response.*;
import com.xiaou.mockinterview.enums.*;
import com.xiaou.mockinterview.service.QuestionSelectorService.GeneratedQuestion;
import com.xiaou.mockinterview.mapper.*;
import com.xiaou.mockinterview.service.AIInterviewerService;
import com.xiaou.mockinterview.service.CareerLoopService;
import com.xiaou.mockinterview.service.MockInterviewService;
import com.xiaou.mockinterview.service.QuestionSelectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 模拟面试核心服务实现
 *
 * @author xiaou
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MockInterviewServiceImpl implements MockInterviewService {

    private final MockInterviewSessionMapper sessionMapper;
    private final MockInterviewQAMapper qaMapper;
    private final MockInterviewDirectionMapper directionMapper;
    private final MockInterviewUserStatsMapper userStatsMapper;
    private final QuestionSelectorService questionSelectorService;
    private final AIInterviewerService aiInterviewerService;
    private final CareerLoopService careerLoopService;
    private final InterviewQuestionSetMapper questionSetMapper;

    @Override
    public List<MockInterviewDirection> getDirections() {
        return directionMapper.selectAllEnabled();
    }

    @Override
    public InterviewConfigResponse getConfig() {
        InterviewConfigResponse config = new InterviewConfigResponse();

        // 难度级别
        List<InterviewConfigResponse.LevelOption> levels = Arrays.stream(InterviewLevelEnum.values())
                .map(level -> {
                    InterviewConfigResponse.LevelOption option = new InterviewConfigResponse.LevelOption();
                    option.setCode(level.getCode());
                    option.setName(level.getName());
                    option.setDescription(level.getDescription());
                    return option;
                }).collect(Collectors.toList());
        config.setLevels(levels);

        // 面试类型
        List<InterviewConfigResponse.TypeOption> types = Arrays.stream(InterviewTypeEnum.values())
                .map(type -> {
                    InterviewConfigResponse.TypeOption option = new InterviewConfigResponse.TypeOption();
                    option.setCode(type.getCode());
                    option.setName(type.getName());
                    option.setDescription(type.getDescription());
                    return option;
                }).collect(Collectors.toList());
        config.setTypes(types);

        // AI风格
        List<InterviewConfigResponse.StyleOption> styles = Arrays.stream(InterviewStyleEnum.values())
                .map(style -> {
                    InterviewConfigResponse.StyleOption option = new InterviewConfigResponse.StyleOption();
                    option.setCode(style.getCode());
                    option.setName(style.getName());
                    option.setDescription(style.getDescription());
                    return option;
                }).collect(Collectors.toList());
        config.setStyles(styles);

        // 题目数量
        config.setQuestionCounts(Arrays.asList(5, 8, 10));

        // 出题模式
        List<InterviewConfigResponse.ModeOption> modes = Arrays.stream(QuestionModeEnum.values())
                .map(mode -> {
                    InterviewConfigResponse.ModeOption option = new InterviewConfigResponse.ModeOption();
                    option.setCode(mode.getCode());
                    option.setName(mode.getName());
                    option.setDescription(mode.getDescription());
                    return option;
                }).collect(Collectors.toList());
        config.setQuestionModes(modes);

        return config;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InterviewSessionResponse createInterview(Long userId, CreateInterviewRequest request) {
        // 验证参数
        validateCreateRequest(request);

        // 检查是否有进行中的面试
        MockInterviewSession ongoing = sessionMapper.selectOngoingByUserId(userId);
        if (ongoing != null) {
            throw new BusinessException("您有正在进行的面试，请先完成或结束当前面试");
        }

        // 确定出题模式（1-本地题库 2-AI出题，默认AI出题）
        int questionMode = request.getQuestionMode() != null ? request.getQuestionMode() : QuestionModeEnum.AI.getCode();

        // 创建会话
        MockInterviewSession session = new MockInterviewSession()
                .setUserId(userId)
                .setDirection(request.getDirection())
                .setLevel(request.getLevel())
                .setInterviewType(request.getInterviewType() != null ? request.getInterviewType() : 1)
                .setStyle(request.getStyle() != null ? request.getStyle() : 2)
                .setQuestionCount(request.getQuestionCount())
                .setQuestionMode(questionMode)
                .setDurationMinutes(calculateEstimatedDuration(request.getQuestionCount()))
                .setStatus(SessionStatusEnum.ONGOING.getCode())
                .setCurrentQuestionOrder(0)
                .setCreateTime(LocalDateTime.now())
                .setUpdateTime(LocalDateTime.now());

        sessionMapper.insert(session);

        // 根据出题模式选择题目
        LocalDateTime now = LocalDateTime.now();
        List<MockInterviewQA> qaList = new ArrayList<>();

        if (questionMode == QuestionModeEnum.LOCAL.getCode()) {
            // 本地题库模式：从题库中抽取
            List<InterviewQuestion> questions;
            if (request.getQuestionSetIds() != null && !request.getQuestionSetIds().isEmpty()) {
                // 用户指定了题库，从指定题库中选题
                questions = questionSelectorService.selectQuestionsFromSets(
                        request.getQuestionSetIds(), request.getQuestionCount(), userId);
            } else {
                // 未指定题库，根据方向自动选题
                questions = questionSelectorService.selectQuestions(
                        request.getDirection(), request.getLevel(), request.getQuestionCount(), userId);
            }

            if (questions.isEmpty()) {
                throw new BusinessException("没有找到符合条件的题目，请尝试AI出题模式");
            }

            for (int i = 0; i < questions.size(); i++) {
                InterviewQuestion q = questions.get(i);
                MockInterviewQA qa = new MockInterviewQA()
                        .setSessionId(session.getId())
                        .setUserId(userId)
                        .setQuestionId(q.getId())
                        .setQuestionOrder(i + 1)
                        .setQuestionContent(q.getTitle())
                        .setQuestionType(QuestionTypeEnum.MAIN.getCode())
                        .setReferenceAnswer(q.getAnswer())
                        .setStatus(QAStatusEnum.PENDING.getCode())
                        .setCreateTime(now)
                        .setUpdateTime(now);
                qaList.add(qa);
            }
        } else {
            // AI出题模式：AI生成题目
            List<GeneratedQuestion> generatedQuestions = questionSelectorService.generateQuestionsByAI(
                    request.getDirection(), request.getLevel(), request.getQuestionCount());

            if (generatedQuestions.isEmpty()) {
                throw new BusinessException("AI出题失败，请稍后重试或选择本地题库模式");
            }

            for (int i = 0; i < generatedQuestions.size(); i++) {
                GeneratedQuestion q = generatedQuestions.get(i);
                MockInterviewQA qa = new MockInterviewQA()
                        .setSessionId(session.getId())
                        .setUserId(userId)
                        .setQuestionOrder(i + 1)
                        .setQuestionContent(q.getQuestionContent())
                        .setQuestionType(QuestionTypeEnum.MAIN.getCode())
                        .setReferenceAnswer(q.getReferenceAnswer())
                        .setKnowledgePoints(q.getKnowledgePoints())
                        .setStatus(QAStatusEnum.PENDING.getCode())
                        .setCreateTime(now)
                        .setUpdateTime(now);
                qaList.add(qa);
            }
        }

        qaMapper.batchInsert(qaList);

        // 更新用户统计
        updateUserStatsOnCreate(userId);

        // 构建响应
        return buildSessionResponse(session);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InterviewQuestionResponse startInterview(Long userId, Long sessionId) {
        MockInterviewSession session = getValidSession(userId, sessionId);

        // 更新开始时间
        session.setStartTime(LocalDateTime.now());
        session.setCurrentQuestionOrder(1);
        sessionMapper.updateById(session);

        // 获取第一题
        return getNextQuestion(userId, sessionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AnswerFeedbackResponse submitAnswer(Long userId, SubmitAnswerRequest request) {
        MockInterviewSession session = getValidSession(userId, request.getSessionId());
        MockInterviewQA qa = qaMapper.selectById(request.getQaId());

        if (qa == null || !qa.getSessionId().equals(request.getSessionId())) {
            throw new BusinessException("问答记录不存在");
        }

        if (qa.getStatus() != QAStatusEnum.PENDING.getCode()) {
            throw new BusinessException("该问题已回答或已跳过");
        }

        // 统计追问次数
        int followUpCount = 0;
        if (qa.getParentQaId() != null) {
            List<MockInterviewQA> followUps = qaMapper.selectFollowUpsByParentId(qa.getParentQaId());
            followUpCount = followUps.size();
        }

        // 调用AI评价
        AIEvaluationResult evaluation = aiInterviewerService.evaluateAnswer(
                session, qa.getQuestionContent(), request.getAnswer(), followUpCount);

        // 更新问答记录
        qa.setUserAnswer(request.getAnswer())
                .setAnswerDurationSeconds(request.getDurationSeconds())
                .setScore(evaluation.getScore())
                .setAiFeedback(JSONUtil.toJsonStr(evaluation.getFeedback()))
                .setStatus(QAStatusEnum.ANSWERED.getCode())
                .setUpdateTime(LocalDateTime.now());
        qaMapper.updateById(qa);

        // 构建响应
        AnswerFeedbackResponse response = new AnswerFeedbackResponse();
        response.setQaId(qa.getId());
        response.setScore(evaluation.getScore());

        AnswerFeedbackResponse.Feedback feedback = new AnswerFeedbackResponse.Feedback();
        feedback.setStrengths(evaluation.getFeedback().getStrengths());
        feedback.setImprovements(evaluation.getFeedback().getImprovements());
        response.setFeedback(feedback);

        // 判断是否需要追问
        if ("followUp".equals(evaluation.getNextAction()) && StrUtil.isNotBlank(evaluation.getFollowUpQuestion())) {
            // 创建追问记录
            MockInterviewQA followUp = new MockInterviewQA()
                    .setSessionId(session.getId())
                    .setUserId(userId)
                    .setQuestionOrder(qa.getQuestionType() == QuestionTypeEnum.MAIN.getCode() ?
                            qa.getQuestionOrder() : qaMapper.selectById(qa.getParentQaId()).getQuestionOrder())
                    .setQuestionContent(evaluation.getFollowUpQuestion())
                    .setQuestionType(QuestionTypeEnum.FOLLOW_UP.getCode())
                    .setParentQaId(qa.getQuestionType() == QuestionTypeEnum.MAIN.getCode() ? qa.getId() : qa.getParentQaId())
                    .setStatus(QAStatusEnum.PENDING.getCode())
                    .setCreateTime(LocalDateTime.now())
                    .setUpdateTime(LocalDateTime.now());
            qaMapper.insert(followUp);

            response.setNextAction("followUp");
            AnswerFeedbackResponse.FollowUpQuestion followUpQuestion = new AnswerFeedbackResponse.FollowUpQuestion();
            followUpQuestion.setQaId(followUp.getId());
            followUpQuestion.setQuestionContent(evaluation.getFollowUpQuestion());
            followUpQuestion.setQuestionType(QuestionTypeEnum.FOLLOW_UP.getCode());
            response.setFollowUpQuestion(followUpQuestion);
        } else {
            response.setNextAction("nextQuestion");
        }

        // 检查是否还有下一题
        int answeredCount = qaMapper.countAnsweredBySessionId(session.getId());
        response.setHasNext(answeredCount < session.getQuestionCount());

        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InterviewQuestionResponse skipQuestion(Long userId, Long sessionId, Long qaId) {
        getValidSession(userId, sessionId);
        MockInterviewQA qa = qaMapper.selectById(qaId);

        if (qa == null || !qa.getSessionId().equals(sessionId)) {
            throw new BusinessException("问答记录不存在");
        }

        // 更新状态为已跳过
        qa.setStatus(QAStatusEnum.SKIPPED.getCode())
                .setScore(0)
                .setUpdateTime(LocalDateTime.now());
        qaMapper.updateById(qa);

        // 返回下一题
        return getNextQuestion(userId, sessionId);
    }

    @Override
    public InterviewQuestionResponse getNextQuestion(Long userId, Long sessionId) {
        MockInterviewSession session = getValidSession(userId, sessionId);

        // 获取下一个待回答的问题
        MockInterviewQA nextQa = qaMapper.selectCurrentPendingBySessionId(sessionId);

        InterviewQuestionResponse response = new InterviewQuestionResponse();

        if (nextQa == null) {
            // 没有更多问题了
            response.setFinished(true);
            return response;
        }

        response.setQaId(nextQa.getId());
        response.setQuestionOrder(nextQa.getQuestionOrder());
        response.setTotalQuestions(session.getQuestionCount());
        response.setQuestionContent(nextQa.getQuestionContent());
        response.setQuestionType(nextQa.getQuestionType());
        response.setFinished(false);

        // 解析知识点
        if (StrUtil.isNotBlank(nextQa.getKnowledgePoints())) {
            response.setKnowledgePoints(Arrays.asList(nextQa.getKnowledgePoints().split(",")));
        }

        // 预计回答时间（180秒）
        response.setEstimatedTime(180);

        // 更新当前题目序号
        session.setCurrentQuestionOrder(nextQa.getQuestionOrder());
        sessionMapper.updateById(session);

        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InterviewReportResponse endInterview(Long userId, Long sessionId) {
        // 不使用 getValidSession，因为它会检查状态必须是进行中
        MockInterviewSession session = sessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new BusinessException("面试会话不存在");
        }
        
        // 如果已经完成，直接返回报告
        if (session.getStatus() == SessionStatusEnum.COMPLETED.getCode()) {
            return getReport(userId, sessionId);
        }

        // 计算得分
        calculateAndUpdateScores(session);

        // 更新会话状态（不再自动生成总结，用户手动触发）
        session.setStatus(SessionStatusEnum.COMPLETED.getCode())
                .setEndTime(LocalDateTime.now())
                .setUpdateTime(LocalDateTime.now());
        sessionMapper.updateById(session);

        // 更新用户统计
        updateUserStatsOnComplete(userId, session);
        pushLoopInterviewDone(userId, sessionId, session.getTotalScore());

        return getReport(userId, sessionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InterviewReportResponse generateSummary(Long userId, Long sessionId) {
        MockInterviewSession session = sessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new BusinessException("面试记录不存在");
        }

        // 如果分数为空，先计算分数
        if (session.getTotalScore() == null) {
            calculateAndUpdateScores(session);
        }
        
        // 如果状态还是进行中，更新为已完成
        boolean needUpdateStats = false;
        if (session.getStatus() == SessionStatusEnum.ONGOING.getCode()) {
            session.setStatus(SessionStatusEnum.COMPLETED.getCode());
            session.setEndTime(LocalDateTime.now());
            needUpdateStats = true;
        }

        // 获取问答记录
        List<MockInterviewQA> qaList = qaMapper.selectBySessionId(sessionId);

        // 生成AI总结和建议
        AIInterviewerService.SummaryResult summaryResult = aiInterviewerService.generateSummaryAndSuggestions(session, qaList);

        // 更新会话
        session.setAiSummary(summaryResult.getSummary())
                .setAiSuggestion(JSONUtil.toJsonStr(summaryResult.getSuggestions()))
                .setUpdateTime(LocalDateTime.now());
        sessionMapper.updateById(session);
        
        // 更新用户统计
        if (needUpdateStats) {
            updateUserStatsOnComplete(userId, session);
        }
        pushLoopInterviewDone(userId, sessionId, session.getTotalScore());

        return getReport(userId, sessionId);
    }

    @Override
    public MockInterviewSession getSessionStatus(Long userId, Long sessionId) {
        return getValidSession(userId, sessionId);
    }

    @Override
    public PageResult<MockInterviewSession> getHistory(Long userId, InterviewHistoryRequest request) {
        return PageHelper.doPage(request.getPageNum(), request.getPageSize(),
                () -> sessionMapper.selectPageByUserId(userId, request));
    }

    @Override
    public InterviewReportResponse getReport(Long userId, Long sessionId) {
        MockInterviewSession session = sessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new BusinessException("面试记录不存在");
        }

        InterviewReportResponse report = new InterviewReportResponse();
        report.setSessionId(session.getId());
        report.setDirection(session.getDirection());
        report.setLevel(session.getLevel());
        report.setTotalScore(session.getTotalScore());

        // 设置方向和级别名称
        MockInterviewDirection direction = directionMapper.selectByCode(session.getDirection());
        if (direction != null) {
            report.setDirectionName(direction.getDirectionName());
        }
        InterviewLevelEnum levelEnum = InterviewLevelEnum.getByCode(session.getLevel());
        if (levelEnum != null) {
            report.setLevelName(levelEnum.getDisplayText());
        }

        // 各维度得分
        InterviewReportResponse.Dimensions dimensions = new InterviewReportResponse.Dimensions();
        dimensions.setKnowledge(session.getKnowledgeScore());
        dimensions.setDepth(session.getDepthScore());
        dimensions.setExpression(session.getExpressionScore());
        dimensions.setAdaptability(session.getAdaptabilityScore());
        report.setDimensions(dimensions);

        report.setAiSummary(session.getAiSummary());

        // 解析改进建议
        if (StrUtil.isNotBlank(session.getAiSuggestion())) {
            try {
                report.setAiSuggestion(JSONUtil.toList(session.getAiSuggestion(), String.class));
            } catch (Exception e) {
                report.setAiSuggestion(Collections.singletonList(session.getAiSuggestion()));
            }
        }

        // 构建问答详情
        List<MockInterviewQA> mainQuestions = qaMapper.selectMainQuestionsBySessionId(sessionId);
        List<InterviewReportResponse.QADetail> qaDetails = new ArrayList<>();

        for (MockInterviewQA mainQa : mainQuestions) {
            InterviewReportResponse.QADetail detail = buildQADetail(mainQa);

            // 获取追问
            List<MockInterviewQA> followUps = qaMapper.selectFollowUpsByParentId(mainQa.getId());
            if (!followUps.isEmpty()) {
                List<InterviewReportResponse.QADetail> followUpDetails = followUps.stream()
                        .map(this::buildQADetail)
                        .collect(Collectors.toList());
                detail.setFollowUps(followUpDetails);
            }

            qaDetails.add(detail);
        }
        report.setQaList(qaDetails);

        // 计算面试时长
        if (session.getStartTime() != null && session.getEndTime() != null) {
            report.setDuration((int) ChronoUnit.SECONDS.between(session.getStartTime(), session.getEndTime()));
        }
        report.setStartTime(session.getStartTime());
        report.setEndTime(session.getEndTime());

        return report;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteInterview(Long userId, Long sessionId) {
        MockInterviewSession session = sessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new BusinessException("面试记录不存在");
        }

        // 删除问答记录
        qaMapper.deleteBySessionId(sessionId);
        // 删除会话
        sessionMapper.deleteById(sessionId);

        log.info("用户{}删除了面试记录{}", userId, sessionId);
    }

    @Override
    public InterviewStatsResponse getStats(Long userId) {
        MockInterviewUserStats stats = userStatsMapper.selectByUserId(userId);

        InterviewStatsResponse response = new InterviewStatsResponse();

        if (stats == null) {
            response.setTotalInterviews(0);
            response.setCompletedInterviews(0);
            response.setAvgScore(BigDecimal.ZERO);
            response.setHighestScore(0);
            response.setTotalQuestions(0);
            response.setCorrectQuestions(0);
            response.setCorrectRate(BigDecimal.ZERO);
            response.setInterviewStreak(0);
            response.setMaxStreak(0);
            response.setCompletionRate(BigDecimal.ZERO);
            return response;
        }

        response.setTotalInterviews(stats.getTotalInterviews());
        response.setCompletedInterviews(stats.getCompletedInterviews());
        response.setAvgScore(stats.getAvgScore());
        response.setHighestScore(stats.getHighestScore());
        response.setTotalQuestions(stats.getTotalQuestions());
        response.setCorrectQuestions(stats.getCorrectQuestions());
        response.setInterviewStreak(stats.getInterviewStreak());
        response.setMaxStreak(stats.getMaxStreak());

        // 计算正确率
        if (stats.getTotalQuestions() > 0) {
            BigDecimal correctRate = BigDecimal.valueOf(stats.getCorrectQuestions())
                    .divide(BigDecimal.valueOf(stats.getTotalQuestions()), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            response.setCorrectRate(correctRate);
        } else {
            response.setCorrectRate(BigDecimal.ZERO);
        }

        // 计算完成率
        if (stats.getTotalInterviews() > 0) {
            BigDecimal completionRate = BigDecimal.valueOf(stats.getCompletedInterviews())
                    .divide(BigDecimal.valueOf(stats.getTotalInterviews()), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            response.setCompletionRate(completionRate);
        } else {
            response.setCompletionRate(BigDecimal.ZERO);
        }

        return response;
    }

    // =================== 私有方法 ===================

    private void validateCreateRequest(CreateInterviewRequest request) {
        if (StrUtil.isBlank(request.getDirection())) {
            throw new BusinessException("请选择面试方向");
        }
        if (request.getLevel() == null) {
            throw new BusinessException("请选择难度级别");
        }
        if (request.getQuestionCount() == null || request.getQuestionCount() < 1) {
            throw new BusinessException("请选择题目数量");
        }

        // 验证方向是否存在
        MockInterviewDirection direction = directionMapper.selectByCode(request.getDirection());
        if (direction == null || direction.getStatus() != 1) {
            throw new BusinessException("面试方向不存在或已禁用");
        }
    }

    private int calculateEstimatedDuration(int questionCount) {
        // 每题约3-4分钟
        return questionCount * 4;
    }

    private MockInterviewSession getValidSession(Long userId, Long sessionId) {
        MockInterviewSession session = sessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new BusinessException("面试会话不存在");
        }
        if (session.getStatus() != SessionStatusEnum.ONGOING.getCode()) {
            throw new BusinessException("面试已结束");
        }
        return session;
    }

    private InterviewSessionResponse buildSessionResponse(MockInterviewSession session) {
        InterviewSessionResponse response = new InterviewSessionResponse();
        response.setSessionId(session.getId());
        response.setDirection(session.getDirection());
        response.setLevel(session.getLevel());
        response.setInterviewType(session.getInterviewType());
        response.setStyle(session.getStyle());
        response.setQuestionCount(session.getQuestionCount());
        response.setEstimatedMinutes(session.getDurationMinutes());
        response.setStatus(session.getStatus());
        response.setCreateTime(session.getCreateTime());

        // 设置名称
        MockInterviewDirection direction = directionMapper.selectByCode(session.getDirection());
        if (direction != null) {
            response.setDirectionName(direction.getDirectionName());
        }
        InterviewLevelEnum levelEnum = InterviewLevelEnum.getByCode(session.getLevel());
        if (levelEnum != null) {
            response.setLevelName(levelEnum.getDisplayText());
        }
        InterviewStyleEnum styleEnum = InterviewStyleEnum.getByCode(session.getStyle());
        if (styleEnum != null) {
            response.setStyleName(styleEnum.getName());
        }

        return response;
    }

    private InterviewReportResponse.QADetail buildQADetail(MockInterviewQA qa) {
        InterviewReportResponse.QADetail detail = new InterviewReportResponse.QADetail();
        detail.setQaId(qa.getId());
        detail.setQuestionOrder(qa.getQuestionOrder());
        detail.setQuestionContent(qa.getQuestionContent());
        detail.setUserAnswer(qa.getUserAnswer());
        detail.setScore(qa.getScore());
        detail.setReferenceAnswer(qa.getReferenceAnswer());

        // 解析反馈
        if (StrUtil.isNotBlank(qa.getAiFeedback())) {
            try {
                AnswerFeedbackResponse.Feedback feedback = JSONUtil.toBean(qa.getAiFeedback(),
                        AnswerFeedbackResponse.Feedback.class);
                detail.setFeedback(feedback);
            } catch (Exception e) {
                log.warn("解析AI反馈失败: {}", qa.getAiFeedback());
            }
        }

        return detail;
    }

    private void calculateAndUpdateScores(MockInterviewSession session) {
        List<MockInterviewQA> qaList = qaMapper.selectBySessionId(session.getId());

        if (qaList.isEmpty()) {
            session.setTotalScore(0);
            return;
        }

        // 计算各维度得分
        int totalScore = 0;
        int count = 0;

        for (MockInterviewQA qa : qaList) {
            if (qa.getScore() != null) {
                totalScore += qa.getScore();
                count++;
            }
        }

        // 计算总分（0-100）
        int avgScore = count > 0 ? (totalScore * 10 / count) : 0;
        session.setTotalScore(Math.min(avgScore, 100));

        // 简单设置各维度得分（实际可以通过AI更精细地评估）
        session.setKnowledgeScore(Math.min(avgScore + 5, 100));
        session.setDepthScore(Math.max(avgScore - 5, 0));
        session.setExpressionScore(avgScore);
        session.setAdaptabilityScore(avgScore);
    }

    private void updateUserStatsOnCreate(Long userId) {
        MockInterviewUserStats stats = userStatsMapper.selectByUserId(userId);
        if (stats == null) {
            stats = new MockInterviewUserStats()
                    .setUserId(userId)
                    .setTotalInterviews(1)
                    .setCompletedInterviews(0)
                    .setAvgScore(BigDecimal.ZERO)
                    .setHighestScore(0)
                    .setTotalQuestions(0)
                    .setCorrectQuestions(0)
                    .setInterviewStreak(0)
                    .setMaxStreak(0)
                    .setCreateTime(LocalDateTime.now())
                    .setUpdateTime(LocalDateTime.now());
            userStatsMapper.insert(stats);
        } else {
            userStatsMapper.incrementInterviewCount(userId);
        }
    }

    private void updateUserStatsOnComplete(Long userId, MockInterviewSession session) {
        MockInterviewUserStats stats = userStatsMapper.selectByUserId(userId);
        if (stats == null) {
            return;
        }

        // 更新完成次数
        stats.setCompletedInterviews(stats.getCompletedInterviews() + 1);

        // 更新平均分
        Double avgScore = qaMapper.calculateAvgScoreBySessionId(session.getId());
        if (avgScore != null) {
            BigDecimal newAvg = BigDecimal.valueOf(avgScore).setScale(2, RoundingMode.HALF_UP);
            // 重新计算总体平均分
            BigDecimal totalAvg = stats.getAvgScore()
                    .multiply(BigDecimal.valueOf(stats.getCompletedInterviews() - 1))
                    .add(newAvg)
                    .divide(BigDecimal.valueOf(stats.getCompletedInterviews()), 2, RoundingMode.HALF_UP);
            stats.setAvgScore(totalAvg);
        }

        // 更新最高分
        if (session.getTotalScore() != null && session.getTotalScore() > stats.getHighestScore()) {
            stats.setHighestScore(session.getTotalScore());
        }

        // 更新题目统计
        int answered = qaMapper.countAnsweredBySessionId(session.getId());
        int highScore = qaMapper.countHighScoreBySessionId(session.getId());
        stats.setTotalQuestions(stats.getTotalQuestions() + answered);
        stats.setCorrectQuestions(stats.getCorrectQuestions() + highScore);

        // 更新连续天数
        LocalDate today = LocalDate.now();
        if (stats.getLastInterviewDate() == null) {
            stats.setInterviewStreak(1);
        } else if (stats.getLastInterviewDate().equals(today.minusDays(1))) {
            stats.setInterviewStreak(stats.getInterviewStreak() + 1);
        } else if (!stats.getLastInterviewDate().equals(today)) {
            stats.setInterviewStreak(1);
        }

        if (stats.getInterviewStreak() > stats.getMaxStreak()) {
            stats.setMaxStreak(stats.getInterviewStreak());
        }

        stats.setLastInterviewDate(today);
        stats.setUpdateTime(LocalDateTime.now());

        userStatsMapper.updateByUserId(stats);
    }

    @Override
    public List<InterviewQuestionSet> getQuestionSets(String direction) {
        // 获取所有公开的题库
        List<InterviewQuestionSet> allSets = questionSetMapper.selectPublicQuestionSets(null);
        
        // 如果指定了方向，可以根据方向关联的分类ID过滤
        if (StrUtil.isNotBlank(direction)) {
            List<Long> categoryIds = questionSelectorService.getCategoryIds(direction);
            if (!categoryIds.isEmpty()) {
                // 过滤出属于该方向的题库
                allSets = allSets.stream()
                        .filter(set -> set.getCategoryId() == null || categoryIds.contains(set.getCategoryId()))
                        .collect(Collectors.toList());
            }
        }
        
        return allSets;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InterviewQuestionResponse requestFollowUp(Long userId, Long sessionId, Long qaId) {
        MockInterviewSession session = getValidSession(userId, sessionId);
        MockInterviewQA parentQa = qaMapper.selectById(qaId);

        if (parentQa == null || !parentQa.getSessionId().equals(sessionId)) {
            throw new BusinessException("问答记录不存在");
        }

        // 检查该题是否已回答
        if (parentQa.getStatus() != QAStatusEnum.ANSWERED.getCode()) {
            throw new BusinessException("请先回答当前问题");
        }

        // 检查追问次数（每题最多追问2次）
        Long actualParentId = parentQa.getQuestionType() == QuestionTypeEnum.MAIN.getCode() 
                ? parentQa.getId() : parentQa.getParentQaId();
        List<MockInterviewQA> existingFollowUps = qaMapper.selectFollowUpsByParentId(actualParentId);
        if (existingFollowUps.size() >= 2) {
            throw new BusinessException("每道题最多可以追问2次");
        }

        // 生成追问问题
        String followUpQuestion = aiInterviewerService.generateFollowUpQuestion(
                session, parentQa.getQuestionContent(), parentQa.getUserAnswer(), existingFollowUps.size());

        // 创建追问记录
        MockInterviewQA followUp = new MockInterviewQA()
                .setSessionId(session.getId())
                .setUserId(userId)
                .setQuestionOrder(parentQa.getQuestionOrder())
                .setQuestionContent(followUpQuestion)
                .setQuestionType(QuestionTypeEnum.FOLLOW_UP.getCode())
                .setParentQaId(actualParentId)
                .setStatus(QAStatusEnum.PENDING.getCode())
                .setCreateTime(LocalDateTime.now())
                .setUpdateTime(LocalDateTime.now());
        qaMapper.insert(followUp);

        // 返回追问题目
        InterviewQuestionResponse response = new InterviewQuestionResponse();
        response.setQaId(followUp.getId());
        response.setQuestionOrder(followUp.getQuestionOrder());
        response.setTotalQuestions(session.getQuestionCount());
        response.setQuestionContent(followUp.getQuestionContent());
        response.setQuestionType(QuestionTypeEnum.FOLLOW_UP.getCode());
        response.setFinished(false);

        return response;
    }

    private void pushLoopInterviewDone(Long userId, Long sessionId, Integer score) {
        try {
            careerLoopService.onEvent(userId, new CareerLoopEventRequest()
                    .setSource("mock_interview")
                    .setTargetStage(CareerLoopStageEnum.INTERVIEW_DONE.name())
                    .setRefId(sessionId == null ? null : String.valueOf(sessionId))
                    .setLatestMockScore(score)
                    .setMockCount(1)
                    .setNote("完成模拟面试"));
        } catch (Exception e) {
            log.warn("推送求职闭环面试完成事件失败，userId={}, sessionId={}", userId, sessionId, e);
        }
    }
}
