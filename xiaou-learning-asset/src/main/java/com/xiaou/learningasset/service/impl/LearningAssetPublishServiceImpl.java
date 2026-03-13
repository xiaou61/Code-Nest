package com.xiaou.learningasset.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.xiaou.common.domain.Notification;
import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.enums.NotificationPriorityEnum;
import com.xiaou.common.enums.NotificationSourceEnum;
import com.xiaou.common.enums.NotificationStatusEnum;
import com.xiaou.common.enums.NotificationTypeEnum;
import com.xiaou.common.exception.BusinessException;
import com.xiaou.common.utils.PageHelper;
import com.xiaou.common.service.NotificationService;
import com.xiaou.flashcard.dto.request.FlashcardBatchCreateRequest;
import com.xiaou.flashcard.dto.request.FlashcardDeckCreateRequest;
import com.xiaou.flashcard.service.FlashcardDeckService;
import com.xiaou.flashcard.service.FlashcardService;
import com.xiaou.interview.domain.InterviewQuestion;
import com.xiaou.interview.domain.InterviewQuestionSet;
import com.xiaou.interview.service.InterviewQuestionService;
import com.xiaou.interview.service.InterviewQuestionSetService;
import com.xiaou.knowledge.domain.KnowledgeMap;
import com.xiaou.knowledge.domain.KnowledgeNode;
import com.xiaou.knowledge.dto.request.CreateKnowledgeNodeRequest;
import com.xiaou.knowledge.service.KnowledgeMapService;
import com.xiaou.knowledge.service.KnowledgeNodeService;
import com.xiaou.learningasset.domain.LearningAssetCandidate;
import com.xiaou.learningasset.domain.LearningAssetPublishLog;
import com.xiaou.learningasset.domain.LearningAssetRecord;
import com.xiaou.learningasset.dto.request.LearningAssetAdminCandidateUpdateRequest;
import com.xiaou.learningasset.dto.request.LearningAssetApproveRequest;
import com.xiaou.learningasset.dto.request.LearningAssetMergeRequest;
import com.xiaou.learningasset.dto.request.LearningAssetReviewQueryRequest;
import com.xiaou.learningasset.dto.response.LearningAssetPublishResponse;
import com.xiaou.learningasset.dto.response.LearningAssetReviewCandidateResponse;
import com.xiaou.learningasset.dto.response.LearningAssetStatisticsResponse;
import com.xiaou.learningasset.enums.LearningAssetCandidateStatus;
import com.xiaou.learningasset.enums.LearningAssetRecordStatus;
import com.xiaou.learningasset.enums.TargetAssetType;
import com.xiaou.learningasset.mapper.LearningAssetCandidateMapper;
import com.xiaou.learningasset.mapper.LearningAssetPublishLogMapper;
import com.xiaou.learningasset.mapper.LearningAssetRecordMapper;
import com.xiaou.learningasset.service.LearningAssetPublishService;
import com.xiaou.plan.dto.PlanCreateRequest;
import com.xiaou.plan.dto.PlanResponse;
import com.xiaou.plan.service.PlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 学习资产发布与审核服务实现
 */
@Service
@RequiredArgsConstructor
public class LearningAssetPublishServiceImpl implements LearningAssetPublishService {

    private final LearningAssetRecordMapper recordMapper;
    private final LearningAssetCandidateMapper candidateMapper;
    private final LearningAssetPublishLogMapper publishLogMapper;
    private final FlashcardDeckService flashcardDeckService;
    private final FlashcardService flashcardService;
    private final PlanService planService;
    private final KnowledgeMapService knowledgeMapService;
    private final KnowledgeNodeService knowledgeNodeService;
    private final InterviewQuestionService interviewQuestionService;
    private final InterviewQuestionSetService interviewQuestionSetService;
    private final NotificationService notificationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LearningAssetPublishResponse publish(Long userId, Long recordId, List<Long> candidateIds) {
        LearningAssetRecord record = requireOwnedRecord(userId, recordId);
        List<LearningAssetCandidate> allCandidates = new ArrayList<>(candidateMapper.selectByRecordId(recordId));
        List<LearningAssetCandidate> selectedCandidates = filterPublishableCandidates(allCandidates, candidateIds);
        if (selectedCandidates.isEmpty()) {
            throw new BusinessException("当前没有可发布的候选项");
        }

        LearningAssetPublishResponse response = new LearningAssetPublishResponse();
        response.setRecordId(recordId);

        publishFlashcards(userId, record, selectedCandidates, response);
        publishPlans(userId, selectedCandidates, response);
        submitReviewCandidates(userId, selectedCandidates, response);
        updateRecordStatus(record, allCandidates);
        sendPublishSummaryNotification(userId, record, response);
        return response;
    }

    @Override
    public PageResult<LearningAssetReviewCandidateResponse> getReviewList(LearningAssetReviewQueryRequest request) {
        return PageHelper.doPage(request.getPageNum(), request.getPageSize(), () ->
                candidateMapper.selectReviewingCandidates(request.getAssetType()).stream()
                        .map(this::toReviewResponse)
                        .toList());
    }

    @Override
    public LearningAssetReviewCandidateResponse getReviewDetail(Long candidateId) {
        return toReviewResponse(requireCandidate(candidateId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateReviewCandidate(Long adminId, Long candidateId, LearningAssetAdminCandidateUpdateRequest request) {
        LearningAssetCandidate candidate = requireReviewingCandidate(candidateId);
        JSONUtil.parse(request.getContentJson());
        candidate.setTitle(request.getTitle())
                .setContentJson(request.getContentJson())
                .setTags(request.getTags())
                .setDifficulty(request.getDifficulty())
                .setReviewNote(StrUtil.blankToDefault(candidate.getReviewNote(), null));
        candidateMapper.updateEditable(candidate);
        logPublish(candidateId, "admin_edit", candidate.getTargetModule(), candidate.getTargetId(), adminId, "success", "管理员更新审核候选内容");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long approve(Long adminId, Long candidateId, LearningAssetApproveRequest request) {
        LearningAssetCandidate candidate = requireReviewingCandidate(candidateId);
        LearningAssetRecord record = requireRecord(candidate.getRecordId());
        List<LearningAssetCandidate> allCandidates = new ArrayList<>(candidateMapper.selectByRecordId(record.getId()));

        Long targetId = switch (resolveTargetType(candidate.getAssetType())) {
            case KNOWLEDGE_NODE -> approveKnowledgeNode(candidate, request);
            case INTERVIEW_QUESTION -> approveInterviewQuestion(candidate, request);
            default -> throw new BusinessException("当前资产类型无需管理员审批");
        };

        candidate.setStatus(LearningAssetCandidateStatus.PUBLISHED.name())
                .setTargetId(targetId)
                .setReviewNote(request.getNote());
        candidateMapper.updateStatus(candidateId, LearningAssetCandidateStatus.PUBLISHED.name(), targetId, request.getNote());
        logPublish(candidateId, "admin_publish", candidate.getTargetModule(), targetId, adminId, "success", "审核通过并发布");
        replaceCandidate(allCandidates, candidate);
        updateRecordStatus(record, allCandidates);
        sendAuditResultNotification(candidate,
                "学习资产审核通过",
                buildAuditMessage(candidate, "已通过审核并完成发布", request.getNote()));
        return targetId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long merge(Long adminId, Long candidateId, LearningAssetMergeRequest request) {
        LearningAssetCandidate candidate = requireReviewingCandidate(candidateId);
        LearningAssetRecord record = requireRecord(candidate.getRecordId());
        List<LearningAssetCandidate> allCandidates = new ArrayList<>(candidateMapper.selectByRecordId(record.getId()));
        Long targetId = switch (resolveTargetType(candidate.getAssetType())) {
            case KNOWLEDGE_NODE -> mergeKnowledgeNode(request.getExistingTargetId());
            case INTERVIEW_QUESTION -> mergeInterviewQuestion(request.getExistingTargetId());
            default -> throw new BusinessException("当前资产类型不支持合并到已有内容");
        };
        String note = StrUtil.blankToDefault(request.getNote(), "已合并到既有内容");
        candidate.setStatus(LearningAssetCandidateStatus.PUBLISHED.name())
                .setTargetId(targetId)
                .setReviewNote(note);
        candidateMapper.updateStatus(candidateId, LearningAssetCandidateStatus.PUBLISHED.name(), targetId, note);
        logPublish(candidateId, "admin_merge", candidate.getTargetModule(), targetId, adminId, "success", note);
        replaceCandidate(allCandidates, candidate);
        updateRecordStatus(record, allCandidates);
        sendAuditResultNotification(candidate,
                "学习资产已合并入库",
                buildAuditMessage(candidate, "已合并到既有内容并沉淀成功", note));
        return targetId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long adminId, Long candidateId, String note) {
        LearningAssetCandidate candidate = requireReviewingCandidate(candidateId);
        LearningAssetRecord record = requireRecord(candidate.getRecordId());
        List<LearningAssetCandidate> allCandidates = new ArrayList<>(candidateMapper.selectByRecordId(record.getId()));

        candidate.setStatus(LearningAssetCandidateStatus.REJECTED.name())
                .setReviewNote(note);
        candidateMapper.updateStatus(candidateId, LearningAssetCandidateStatus.REJECTED.name(), candidate.getTargetId(), note);
        logPublish(candidateId, "admin_reject", candidate.getTargetModule(), candidate.getTargetId(), adminId, "failed", note);
        replaceCandidate(allCandidates, candidate);
        updateRecordStatus(record, allCandidates);
        sendAuditResultNotification(candidate,
                "学习资产审核未通过",
                buildAuditMessage(candidate, "未通过审核，请根据备注调整后重新提交", note));
    }

    @Override
    public LearningAssetStatisticsResponse getStatistics() {
        LearningAssetStatisticsResponse response = new LearningAssetStatisticsResponse();

        Long totalTransforms = safeLong(recordMapper.countAll());
        Long successTransforms = safeLong(recordMapper.countSuccess());
        Long reviewingTransforms = safeLong(recordMapper.countStatus(LearningAssetRecordStatus.REVIEWING.name()));
        Long publishedTransforms = safeLong(recordMapper.countStatus(LearningAssetRecordStatus.PUBLISHED.name()))
                + safeLong(recordMapper.countStatus(LearningAssetRecordStatus.PARTIAL_PUBLISHED.name()));
        Long totalCandidates = safeLong(candidateMapper.countAll());
        Long editedCandidates = safeLong(candidateMapper.countEdited());
        Long submittedCandidates = safeLong(candidateMapper.countByStatuses(List.of(
                LearningAssetCandidateStatus.REVIEWING.name(),
                LearningAssetCandidateStatus.PUBLISHED.name()
        )));
        Long rejectedCandidates = safeLong(candidateMapper.countByStatuses(List.of(
                LearningAssetCandidateStatus.REJECTED.name()
        )));

        response.getOverview()
                .setTotalTransforms(totalTransforms)
                .setSuccessTransforms(successTransforms)
                .setReviewingTransforms(reviewingTransforms)
                .setPublishedTransforms(publishedTransforms)
                .setTotalCandidates(totalCandidates)
                .setEditedCandidates(editedCandidates)
                .setSubmittedCandidates(submittedCandidates)
                .setRejectedCandidates(rejectedCandidates)
                .setTransformSuccessRate(percent(successTransforms, totalTransforms))
                .setEditRate(percent(editedCandidates, totalCandidates))
                .setRejectRate(percent(rejectedCandidates, submittedCandidates));

        response.setSourceStats(enrichSourceStats(recordMapper.selectSourceStatistics()));
        response.setAssetStats(enrichAssetStats(candidateMapper.selectAssetStatistics()));
        response.setFailReasonStats(recordMapper.selectFailReasonStatistics());
        response.setTopSourceStats(enrichTopSources(recordMapper.selectTopSourceStatistics()));
        return response;
    }

    private void publishFlashcards(Long userId,
                                   LearningAssetRecord record,
                                   List<LearningAssetCandidate> selectedCandidates,
                                   LearningAssetPublishResponse response) {
        List<LearningAssetCandidate> flashcards = selectedCandidates.stream()
                .filter(candidate -> resolveTargetType(candidate.getAssetType()) == TargetAssetType.FLASHCARD)
                .toList();
        if (flashcards.isEmpty()) {
            return;
        }

        FlashcardDeckCreateRequest deckRequest = new FlashcardDeckCreateRequest();
        deckRequest.setName(StrUtil.maxLength(record.getSourceTitle() + " · 学习闪卡", 100));
        deckRequest.setDescription("由学习资产转化引擎生成");
        deckRequest.setTags(collectTags(flashcards));
        deckRequest.setIsPublic(Boolean.FALSE);

        Long deckId = flashcardDeckService.createDeck(deckRequest, userId);
        FlashcardBatchCreateRequest batchRequest = new FlashcardBatchCreateRequest();
        batchRequest.setDeckId(deckId);
        List<FlashcardBatchCreateRequest.FlashcardItem> items = new ArrayList<>();
        for (LearningAssetCandidate candidate : flashcards) {
            JSONObject json = JSONUtil.parseObj(candidate.getContentJson());
            FlashcardBatchCreateRequest.FlashcardItem item = new FlashcardBatchCreateRequest.FlashcardItem();
            item.setFrontContent(json.getStr("frontContent"));
            item.setBackContent(json.getStr("backContent"));
            item.setContentType(json.getInt("contentType", 1));
            item.setTags(candidate.getTags());
            items.add(item);

            candidate.setStatus(LearningAssetCandidateStatus.PUBLISHED.name())
                    .setTargetId(deckId);
            candidateMapper.updateStatus(candidate.getId(), LearningAssetCandidateStatus.PUBLISHED.name(), deckId, "已发布到闪卡卡组");
            logPublish(candidate.getId(), "direct", candidate.getTargetModule(), deckId, userId, "success", "发布到闪卡卡组成功");
            response.getPublishedCandidateIds().add(candidate.getId());
        }
        batchRequest.setCards(items);
        flashcardService.batchCreateCards(batchRequest, userId);
        response.setFlashcardDeckId(deckId);
        response.setPublishedCount(response.getPublishedCount() + flashcards.size());
    }

    private void publishPlans(Long userId,
                              List<LearningAssetCandidate> selectedCandidates,
                              LearningAssetPublishResponse response) {
        List<LearningAssetCandidate> planCandidates = selectedCandidates.stream()
                .filter(candidate -> resolveTargetType(candidate.getAssetType()) == TargetAssetType.PRACTICE_PLAN)
                .toList();
        for (LearningAssetCandidate candidate : planCandidates) {
            JSONObject json = JSONUtil.parseObj(candidate.getContentJson());
            PlanCreateRequest request = new PlanCreateRequest();
            request.setPlanName(StrUtil.blankToDefault(json.getStr("planName"), candidate.getTitle()));
            request.setPlanDesc(StrUtil.blankToDefault(json.getStr("planDesc"), "由学习资产转化生成"));
            request.setPlanType(2);
            request.setTargetType(3);
            request.setTargetValue(json.getInt("targetValue", 1));
            request.setTargetUnit(StrUtil.blankToDefault(json.getStr("targetUnit"), "次"));
            request.setStartDate(LocalDate.now());
            request.setRepeatType(1);
            request.setRemindEnabled(0);

            PlanResponse plan = planService.createPlan(userId, request);
            candidate.setStatus(LearningAssetCandidateStatus.PUBLISHED.name())
                    .setTargetId(plan.getId());
            candidateMapper.updateStatus(candidate.getId(), LearningAssetCandidateStatus.PUBLISHED.name(), plan.getId(), "已发布到计划中心");
            logPublish(candidate.getId(), "direct", candidate.getTargetModule(), plan.getId(), userId, "success", "发布到计划中心成功");
            response.getPlanIds().add(plan.getId());
            response.getPublishedCandidateIds().add(candidate.getId());
            response.setPublishedCount(response.getPublishedCount() + 1);
        }
    }

    private void submitReviewCandidates(Long userId,
                                        List<LearningAssetCandidate> selectedCandidates,
                                        LearningAssetPublishResponse response) {
        List<LearningAssetCandidate> reviewCandidates = selectedCandidates.stream()
                .filter(candidate -> {
                    TargetAssetType targetType = resolveTargetType(candidate.getAssetType());
                    return targetType == TargetAssetType.KNOWLEDGE_NODE
                            || targetType == TargetAssetType.INTERVIEW_QUESTION;
                })
                .toList();

        for (LearningAssetCandidate candidate : reviewCandidates) {
            candidate.setStatus(LearningAssetCandidateStatus.REVIEWING.name());
            candidateMapper.updateStatus(candidate.getId(), LearningAssetCandidateStatus.REVIEWING.name(), null, "已提交审核");
            logPublish(candidate.getId(), "review_submit", candidate.getTargetModule(), null, userId, "success", "已提交管理员审核");
            response.getReviewingCandidateIds().add(candidate.getId());
            response.setReviewingCount(response.getReviewingCount() + 1);
        }
    }

    private Long approveKnowledgeNode(LearningAssetCandidate candidate, LearningAssetApproveRequest request) {
        if (request.getMapId() == null) {
            throw new BusinessException("知识节点发布必须指定图谱");
        }
        KnowledgeMap knowledgeMap = knowledgeMapService.getById(request.getMapId());
        if (knowledgeMap == null) {
            throw new BusinessException("目标知识图谱不存在");
        }
        JSONObject json = JSONUtil.parseObj(candidate.getContentJson());
        CreateKnowledgeNodeRequest createRequest = new CreateKnowledgeNodeRequest();
        createRequest.setParentId(request.getParentId() == null ? 0L : request.getParentId());
        createRequest.setTitle(StrUtil.blankToDefault(json.getStr("title"), candidate.getTitle()));
        createRequest.setNodeType(1);
        createRequest.setSortOrder(0);
        createRequest.setIsExpanded(Boolean.TRUE);
        return knowledgeNodeService.createNode(request.getMapId(), createRequest);
    }

    private Long mergeKnowledgeNode(Long existingTargetId) {
        if (existingTargetId == null) {
            throw new BusinessException("合并知识节点必须指定目标节点");
        }
        KnowledgeNode knowledgeNode = knowledgeNodeService.getById(existingTargetId);
        if (knowledgeNode == null) {
            throw new BusinessException("目标知识节点不存在");
        }
        return existingTargetId;
    }

    private Long approveInterviewQuestion(LearningAssetCandidate candidate, LearningAssetApproveRequest request) {
        if (request.getQuestionSetId() == null) {
            throw new BusinessException("面试题发布必须指定题单");
        }
        InterviewQuestionSet questionSet = interviewQuestionSetService.getQuestionSetById(request.getQuestionSetId());
        if (questionSet == null) {
            throw new BusinessException("目标题单不存在");
        }
        JSONObject json = JSONUtil.parseObj(candidate.getContentJson());
        InterviewQuestion question = new InterviewQuestion()
                .setQuestionSetId(request.getQuestionSetId())
                .setTitle(StrUtil.blankToDefault(json.getStr("title"), candidate.getTitle()))
                .setAnswer(StrUtil.blankToDefault(json.getStr("answer"), candidate.getContentJson()))
                .setSortOrder(0);
        return interviewQuestionService.createQuestion(question);
    }

    private Long mergeInterviewQuestion(Long existingTargetId) {
        if (existingTargetId == null) {
            throw new BusinessException("合并面试题必须指定目标题目");
        }
        if (interviewQuestionService.getQuestionById(existingTargetId) == null) {
            throw new BusinessException("目标面试题不存在");
        }
        return existingTargetId;
    }

    private List<LearningAssetCandidate> filterPublishableCandidates(List<LearningAssetCandidate> allCandidates, List<Long> candidateIds) {
        Set<Long> expectedIds = CollUtil.isEmpty(candidateIds)
                ? allCandidates.stream()
                .filter(candidate -> LearningAssetCandidateStatus.SELECTED.name().equals(candidate.getStatus()))
                .map(LearningAssetCandidate::getId)
                .collect(Collectors.toSet())
                : new HashSet<>(candidateIds);

        return allCandidates.stream()
                .filter(candidate -> expectedIds.contains(candidate.getId()))
                .peek(candidate -> {
                    if (!LearningAssetCandidateStatus.SELECTED.name().equals(candidate.getStatus())) {
                        throw new BusinessException("仅支持发布已确认候选项");
                    }
                })
                .toList();
    }

    private void updateRecordStatus(LearningAssetRecord record, List<LearningAssetCandidate> allCandidates) {
        int publishedCount = (int) allCandidates.stream()
                .filter(candidate -> LearningAssetCandidateStatus.PUBLISHED.name().equals(candidate.getStatus()))
                .count();
        boolean hasReviewing = allCandidates.stream()
                .anyMatch(candidate -> LearningAssetCandidateStatus.REVIEWING.name().equals(candidate.getStatus()));
        boolean hasPending = allCandidates.stream()
                .anyMatch(candidate -> LearningAssetCandidateStatus.DRAFT.name().equals(candidate.getStatus())
                        || LearningAssetCandidateStatus.SELECTED.name().equals(candidate.getStatus()));
        boolean hasRejected = allCandidates.stream()
                .anyMatch(candidate -> LearningAssetCandidateStatus.REJECTED.name().equals(candidate.getStatus()));

        String status;
        String failReason = null;
        if (publishedCount == allCandidates.size() && publishedCount > 0) {
            status = LearningAssetRecordStatus.PUBLISHED.name();
        } else if (hasReviewing && publishedCount > 0) {
            status = LearningAssetRecordStatus.PARTIAL_PUBLISHED.name();
        } else if (hasReviewing) {
            status = LearningAssetRecordStatus.REVIEWING.name();
        } else if (publishedCount > 0) {
            status = LearningAssetRecordStatus.PARTIAL_PUBLISHED.name();
        } else if (hasRejected && !hasPending) {
            status = LearningAssetRecordStatus.FAILED.name();
            failReason = "候选资产未通过审核";
        } else {
            status = LearningAssetRecordStatus.PENDING_CONFIRM.name();
        }

        record.setPublishedCandidates(publishedCount)
                .setStatus(status)
                .setFailReason(failReason)
                .setUpdateTime(LocalDateTime.now());
        recordMapper.update(record);
    }

    private LearningAssetReviewCandidateResponse toReviewResponse(LearningAssetCandidate candidate) {
        LearningAssetRecord record = requireRecord(candidate.getRecordId());
        LearningAssetReviewCandidateResponse response = new LearningAssetReviewCandidateResponse();
        response.setCandidateId(candidate.getId());
        response.setRecordId(candidate.getRecordId());
        response.setUserId(candidate.getUserId());
        response.setAssetType(candidate.getAssetType());
        response.setAssetTypeText(candidate.getAssetType());
        response.setTitle(candidate.getTitle());
        response.setContentJson(candidate.getContentJson());
        response.setTags(candidate.getTags());
        response.setDifficulty(candidate.getDifficulty());
        response.setConfidenceScore(candidate.getConfidenceScore());
        response.setStatus(candidate.getStatus());
        response.setStatusText(LearningAssetCandidateStatus.valueOf(candidate.getStatus()).getText());
        response.setReviewNote(candidate.getReviewNote());
        response.setTargetModule(candidate.getTargetModule());
        response.setTargetId(candidate.getTargetId());
        response.setSourceType(record.getSourceType());
        response.setSourceId(record.getSourceId());
        response.setSourceTitle(record.getSourceTitle());
        response.setSourceSnapshot(record.getSourceSnapshot());
        response.setCreateTime(candidate.getCreateTime());
        return response;
    }

    private void replaceCandidate(List<LearningAssetCandidate> allCandidates, LearningAssetCandidate updatedCandidate) {
        for (int i = 0; i < allCandidates.size(); i++) {
            if (allCandidates.get(i).getId().equals(updatedCandidate.getId())) {
                allCandidates.set(i, updatedCandidate);
                return;
            }
        }
        allCandidates.add(updatedCandidate);
    }

    private void logPublish(Long candidateId,
                            String publishType,
                            String targetModule,
                            Long targetId,
                            Long operatorId,
                            String publishResult,
                            String message) {
        publishLogMapper.insert(new LearningAssetPublishLog()
                .setCandidateId(candidateId)
                .setPublishType(publishType)
                .setTargetModule(targetModule)
                .setTargetId(targetId)
                .setOperatorId(operatorId)
                .setPublishResult(publishResult)
                .setMessage(message)
                .setCreateTime(LocalDateTime.now()));
    }

    private void sendPublishSummaryNotification(Long userId,
                                                LearningAssetRecord record,
                                                LearningAssetPublishResponse response) {
        if (userId == null) {
            return;
        }

        String title;
        if (response.getPublishedCount() > 0 && response.getReviewingCount() > 0) {
            title = "学习资产已发布，部分内容进入审核";
        } else if (response.getPublishedCount() > 0) {
            title = "学习资产发布成功";
        } else {
            title = "学习资产已提交审核";
        }

        String content = String.format("《%s》本次已发布 %d 项学习资产，%d 项进入审核，前往“我的学习资产”可继续学习或跟踪进度。",
                StrUtil.blankToDefault(record.getSourceTitle(), "当前内容"),
                response.getPublishedCount(),
                response.getReviewingCount());
        sendNotification(userId, title, content, record.getId());
    }

    private void sendAuditResultNotification(LearningAssetCandidate candidate, String title, String content) {
        sendNotification(candidate.getUserId(), title, content, candidate.getRecordId());
    }

    private String buildAuditMessage(LearningAssetCandidate candidate, String actionText, String note) {
        StringBuilder builder = new StringBuilder();
        builder.append("《")
                .append(StrUtil.blankToDefault(candidate.getTitle(), "未命名候选资产"))
                .append("》对应的")
                .append(assetTypeText(candidate.getAssetType()))
                .append(actionText)
                .append("。");
        if (StrUtil.isNotBlank(note)) {
            builder.append(" 审核备注：").append(note);
        }
        return builder.toString();
    }

    private void sendNotification(Long receiverId, String title, String content, Long sourceId) {
        if (receiverId == null) {
            return;
        }
        Notification notification = new Notification();
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(NotificationTypeEnum.SYSTEM.getCode());
        notification.setPriority(NotificationPriorityEnum.MEDIUM.getCode());
        notification.setSenderId(0L);
        notification.setReceiverId(receiverId);
        notification.setSourceModule(NotificationSourceEnum.SYSTEM.getCode());
        notification.setSourceId(sourceId == null ? null : String.valueOf(sourceId));
        notification.setStatus(NotificationStatusEnum.UNREAD.getCode());
        notification.setCreatedTime(LocalDateTime.now());
        notification.setUpdatedTime(LocalDateTime.now());
        notificationService.sendNotification(notification);
    }

    private List<LearningAssetStatisticsResponse.SourceStat> enrichSourceStats(List<LearningAssetStatisticsResponse.SourceStat> stats) {
        List<LearningAssetStatisticsResponse.SourceStat> results = stats == null ? new ArrayList<>() : stats;
        for (LearningAssetStatisticsResponse.SourceStat stat : results) {
            stat.setSourceTypeText(sourceTypeText(stat.getSourceType()))
                    .setSuccessRate(percent(stat.getSuccessCount(), stat.getTotalCount()));
        }
        return results;
    }

    private List<LearningAssetStatisticsResponse.AssetStat> enrichAssetStats(List<LearningAssetStatisticsResponse.AssetStat> stats) {
        List<LearningAssetStatisticsResponse.AssetStat> results = stats == null ? new ArrayList<>() : stats;
        for (LearningAssetStatisticsResponse.AssetStat stat : results) {
            long publishBase = safeLong(stat.getPublishedCount()) + safeLong(stat.getReviewingCount());
            stat.setAssetTypeText(assetTypeText(stat.getAssetType()))
                    .setPublishRate(percent(publishBase, stat.getTotalCount()));
        }
        return results;
    }

    private List<LearningAssetStatisticsResponse.TopSourceStat> enrichTopSources(List<LearningAssetStatisticsResponse.TopSourceStat> stats) {
        List<LearningAssetStatisticsResponse.TopSourceStat> results = stats == null ? new ArrayList<>() : stats;
        for (LearningAssetStatisticsResponse.TopSourceStat stat : results) {
            stat.setSourceTypeText(sourceTypeText(stat.getSourceType()));
        }
        return results;
    }

    private String collectTags(List<LearningAssetCandidate> candidates) {
        Set<String> tags = new LinkedHashSet<>();
        for (LearningAssetCandidate candidate : candidates) {
            if (StrUtil.isBlank(candidate.getTags())) {
                continue;
            }
            tags.addAll(StrUtil.split(candidate.getTags(), ','));
        }
        return tags.isEmpty() ? null : String.join(",", tags);
    }

    private TargetAssetType resolveTargetType(String assetType) {
        for (TargetAssetType type : TargetAssetType.values()) {
            if (type.getCode().equalsIgnoreCase(assetType)) {
                return type;
            }
        }
        throw new BusinessException("未知资产类型：" + assetType);
    }

    private String sourceTypeText(String sourceType) {
        return switch (StrUtil.blankToDefault(sourceType, "")) {
            case "blog" -> "博客文章";
            case "community" -> "社区帖子";
            case "codepen" -> "CodePen 作品";
            case "mock_interview" -> "模拟面试报告";
            default -> sourceType;
        };
    }

    private String assetTypeText(String assetType) {
        return switch (StrUtil.blankToDefault(assetType, "")) {
            case "flashcard" -> "闪卡卡组";
            case "knowledge_node" -> "知识节点候选";
            case "practice_plan" -> "练习清单";
            case "interview_question" -> "面试题草稿";
            default -> assetType;
        };
    }

    private Long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private Double percent(Long numerator, Long denominator) {
        long denominatorValue = safeLong(denominator);
        if (denominatorValue <= 0) {
            return 0D;
        }
        long numeratorValue = safeLong(numerator);
        return Math.round((numeratorValue * 10000D / denominatorValue)) / 100D;
    }

    private LearningAssetRecord requireOwnedRecord(Long userId, Long recordId) {
        LearningAssetRecord record = recordMapper.selectByUserIdAndId(userId, recordId);
        if (record == null) {
            throw new BusinessException("转化记录不存在");
        }
        return record;
    }

    private LearningAssetRecord requireRecord(Long recordId) {
        LearningAssetRecord record = recordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException("转化记录不存在");
        }
        return record;
    }

    private LearningAssetCandidate requireCandidate(Long candidateId) {
        LearningAssetCandidate candidate = candidateMapper.selectById(candidateId);
        if (candidate == null) {
            throw new BusinessException("候选项不存在");
        }
        return candidate;
    }

    private LearningAssetCandidate requireReviewingCandidate(Long candidateId) {
        LearningAssetCandidate candidate = requireCandidate(candidateId);
        if (!LearningAssetCandidateStatus.REVIEWING.name().equals(candidate.getStatus())) {
            throw new BusinessException("当前候选项不在审核中");
        }
        return candidate;
    }
}
