package com.xiaou.learningasset.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;
import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.exception.BusinessException;
import com.xiaou.common.utils.PageHelper;
import com.xiaou.learningasset.domain.LearningAssetCandidate;
import com.xiaou.learningasset.domain.LearningAssetRecord;
import com.xiaou.learningasset.dto.request.LearningAssetCandidateUpdateRequest;
import com.xiaou.learningasset.dto.request.LearningAssetConfirmRequest;
import com.xiaou.learningasset.dto.request.LearningAssetConvertRequest;
import com.xiaou.learningasset.dto.request.LearningAssetRecordQueryRequest;
import com.xiaou.learningasset.dto.response.LearningAssetCandidateResponse;
import com.xiaou.learningasset.dto.response.LearningAssetRecordDetailResponse;
import com.xiaou.learningasset.dto.response.LearningAssetRecordSummaryResponse;
import com.xiaou.learningasset.dto.source.LearningAssetSourceSnapshot;
import com.xiaou.learningasset.dto.transform.TransformCandidateDraft;
import com.xiaou.learningasset.dto.transform.TransformResult;
import com.xiaou.learningasset.enums.LearningAssetCandidateStatus;
import com.xiaou.learningasset.enums.LearningAssetRecordStatus;
import com.xiaou.learningasset.enums.TargetAssetType;
import com.xiaou.learningasset.enums.TransformMode;
import com.xiaou.learningasset.mapper.LearningAssetCandidateMapper;
import com.xiaou.learningasset.mapper.LearningAssetPublishLogMapper;
import com.xiaou.learningasset.mapper.LearningAssetRecordMapper;
import com.xiaou.learningasset.service.LearningAssetService;
import com.xiaou.learningasset.service.LearningAssetSourceService;
import com.xiaou.learningasset.service.LearningAssetTransformEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 学习资产服务实现
 */
@Service
@RequiredArgsConstructor
public class LearningAssetServiceImpl implements LearningAssetService {

    private final LearningAssetRecordMapper recordMapper;
    private final LearningAssetCandidateMapper candidateMapper;
    private final LearningAssetPublishLogMapper publishLogMapper;
    private final LearningAssetSourceService sourceService;
    private final LearningAssetTransformEngine transformEngine;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LearningAssetRecordDetailResponse convert(Long userId, LearningAssetConvertRequest request) {
        LearningAssetSourceSnapshot snapshot = sourceService.loadSnapshot(userId, request);
        TransformMode mode = parseTransformMode(request.getTransformMode());
        List<TargetAssetType> targetTypes = parseTargetTypes(request.getTargetTypes());
        TransformResult transformResult = transformEngine.transform(snapshot, mode, targetTypes);

        LocalDateTime now = LocalDateTime.now();
        LearningAssetRecord record = new LearningAssetRecord()
                .setUserId(userId)
                .setSourceType(snapshot.getSourceType())
                .setSourceId(snapshot.getSourceId())
                .setSourceTitle(snapshot.getTitle())
                .setSourceSnapshot(JSONUtil.toJsonStr(snapshot))
                .setTransformMode(mode.name())
                .setTargetTypes(String.join(",", request.getTargetTypes()))
                .setStatus(transformResult.getCandidates().isEmpty()
                        ? LearningAssetRecordStatus.FAILED.name()
                        : LearningAssetRecordStatus.PENDING_CONFIRM.name())
                .setSourceHash(DigestUtil.sha256Hex(JSONUtil.toJsonStr(snapshot)))
                .setSummaryText(transformResult.getSummary())
                .setFailReason(transformResult.getCandidates().isEmpty() ? "未生成任何候选学习资产" : null)
                .setTotalCandidates(transformResult.getCandidates().size())
                .setPublishedCandidates(0)
                .setCreateTime(now)
                .setUpdateTime(now);
        recordMapper.insert(record);

        int sortOrder = 1;
        for (TransformCandidateDraft draft : transformResult.getCandidates()) {
            candidateMapper.insert(new LearningAssetCandidate()
                    .setRecordId(record.getId())
                    .setUserId(userId)
                    .setAssetType(draft.getAssetType().getCode())
                    .setTitle(draft.getTitle())
                    .setContentJson(draft.getContentJson())
                    .setTags(draft.getTags())
                    .setDifficulty(null)
                    .setConfidenceScore(draft.getConfidenceScore())
                    .setDedupeKey(DigestUtil.md5Hex(draft.getAssetType().getCode() + ":" + draft.getTitle()))
                    .setTargetModule(draft.getTargetModule())
                    .setStatus(LearningAssetCandidateStatus.DRAFT.name())
                    .setSortOrder(sortOrder++)
                    .setCreateTime(now)
                    .setUpdateTime(now));
        }

        return getRecordDetail(userId, record.getId());
    }

    @Override
    public PageResult<LearningAssetRecordSummaryResponse> getRecordList(Long userId, LearningAssetRecordQueryRequest request) {
        request.setUserId(userId);
        return PageHelper.doPage(request.getPageNum(), request.getPageSize(), () ->
                recordMapper.selectByUserId(request).stream().map(this::toSummaryResponse).toList());
    }

    @Override
    public LearningAssetRecordDetailResponse getRecordDetail(Long userId, Long recordId) {
        LearningAssetRecord record = recordMapper.selectByUserIdAndId(userId, recordId);
        if (record == null) {
            throw new BusinessException("转化记录不存在");
        }
        LearningAssetRecordDetailResponse response = new LearningAssetRecordDetailResponse();
        response.setRecordId(record.getId());
        response.setSourceType(record.getSourceType());
        response.setSourceId(record.getSourceId());
        response.setSourceTitle(record.getSourceTitle());
        response.setSourceSnapshot(record.getSourceSnapshot());
        response.setTransformMode(record.getTransformMode());
        response.setStatus(record.getStatus());
        response.setStatusText(recordStatusText(record.getStatus()));
        response.setSummaryText(record.getSummaryText());
        response.setTotalCandidates(record.getTotalCandidates());
        response.setPublishedCandidates(record.getPublishedCandidates());
        response.setFailReason(record.getFailReason());
        response.setCandidates(candidateMapper.selectByRecordId(recordId).stream().map(this::toCandidateResponse).toList());
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCandidate(Long userId, Long candidateId, LearningAssetCandidateUpdateRequest request) {
        LearningAssetCandidate candidate = candidateMapper.selectById(candidateId);
        if (candidate == null || !candidate.getUserId().equals(userId)) {
            throw new BusinessException("候选项不存在");
        }
        if (!LearningAssetCandidateStatus.DRAFT.name().equals(candidate.getStatus())
                && !LearningAssetCandidateStatus.SELECTED.name().equals(candidate.getStatus())) {
            throw new BusinessException("当前状态不允许编辑");
        }
        candidate.setTitle(request.getTitle())
                .setContentJson(request.getContentJson())
                .setTags(request.getTags())
                .setDifficulty(request.getDifficulty())
                .setUpdateTime(LocalDateTime.now());
        candidateMapper.updateEditable(candidate);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LearningAssetRecordDetailResponse confirmCandidates(Long userId, Long recordId, LearningAssetConfirmRequest request) {
        LearningAssetRecord record = requireOwnedRecord(userId, recordId);
        List<LearningAssetCandidate> candidates = candidateMapper.selectByRecordId(recordId);
        if (candidates.isEmpty()) {
            throw new BusinessException("当前记录没有可确认候选项");
        }
        for (LearningAssetCandidate candidate : candidates) {
            if (!candidate.getUserId().equals(userId)) {
                throw new BusinessException("存在无权操作的候选项");
            }
            String nextStatus = request.getCandidateIds().contains(candidate.getId())
                    ? LearningAssetCandidateStatus.SELECTED.name()
                    : LearningAssetCandidateStatus.DISCARDED.name();
            candidateMapper.updateStatus(candidate.getId(), nextStatus, candidate.getTargetId(), candidate.getReviewNote());
        }
        record.setStatus(LearningAssetRecordStatus.PENDING_CONFIRM.name())
                .setFailReason(null)
                .setUpdateTime(LocalDateTime.now());
        recordMapper.update(record);
        return getRecordDetail(userId, recordId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LearningAssetRecordDetailResponse discardCandidate(Long userId, Long candidateId) {
        LearningAssetCandidate candidate = candidateMapper.selectById(candidateId);
        if (candidate == null || !candidate.getUserId().equals(userId)) {
            throw new BusinessException("候选项不存在");
        }
        if (LearningAssetCandidateStatus.DISCARDED.name().equals(candidate.getStatus())) {
            return getRecordDetail(userId, candidate.getRecordId());
        }
        if (!LearningAssetCandidateStatus.DRAFT.name().equals(candidate.getStatus())
                && !LearningAssetCandidateStatus.SELECTED.name().equals(candidate.getStatus())) {
            throw new BusinessException("当前状态不允许丢弃");
        }

        candidateMapper.updateStatus(candidateId, LearningAssetCandidateStatus.DISCARDED.name(), candidate.getTargetId(), null);

        LearningAssetRecord record = requireOwnedRecord(userId, candidate.getRecordId());
        record.setStatus(LearningAssetRecordStatus.PENDING_CONFIRM.name())
                .setFailReason(null)
                .setUpdateTime(LocalDateTime.now());
        recordMapper.update(record);
        return getRecordDetail(userId, record.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LearningAssetRecordDetailResponse retry(Long userId, Long recordId) {
        LearningAssetRecord record = requireOwnedRecord(userId, recordId);
        LearningAssetConvertRequest request = new LearningAssetConvertRequest();
        request.setSourceType(record.getSourceType());
        request.setSourceId(record.getSourceId());
        request.setTransformMode(record.getTransformMode());
        request.setTargetTypes(StrUtil.split(record.getTargetTypes(), ','));
        request.setUseExistingSummary(Boolean.TRUE);
        return convert(userId, request);
    }

    private TransformMode parseTransformMode(String mode) {
        if (StrUtil.isBlank(mode)) {
            return TransformMode.STUDY;
        }
        return TransformMode.valueOf(mode.trim().toUpperCase());
    }

    private List<TargetAssetType> parseTargetTypes(List<String> targetTypes) {
        List<TargetAssetType> results = new ArrayList<>();
        for (String item : targetTypes) {
            for (TargetAssetType type : TargetAssetType.values()) {
                if (type.getCode().equalsIgnoreCase(item)) {
                    results.add(type);
                    break;
                }
            }
        }
        if (results.isEmpty()) {
            throw new BusinessException("未解析到有效资产类型");
        }
        return results;
    }

    private LearningAssetRecordSummaryResponse toSummaryResponse(LearningAssetRecord record) {
        LearningAssetRecordSummaryResponse response = new LearningAssetRecordSummaryResponse();
        response.setRecordId(record.getId());
        response.setSourceType(record.getSourceType());
        response.setSourceId(record.getSourceId());
        response.setSourceTitle(record.getSourceTitle());
        response.setStatus(record.getStatus());
        response.setStatusText(recordStatusText(record.getStatus()));
        response.setTotalCandidates(record.getTotalCandidates());
        response.setPublishedCandidates(record.getPublishedCandidates());
        response.setCreateTime(record.getCreateTime());
        return response;
    }

    private LearningAssetCandidateResponse toCandidateResponse(LearningAssetCandidate candidate) {
        LearningAssetCandidateResponse response = new LearningAssetCandidateResponse();
        response.setId(candidate.getId());
        response.setAssetType(candidate.getAssetType());
        response.setAssetTypeText(candidate.getAssetType());
        response.setTitle(candidate.getTitle());
        response.setContentJson(candidate.getContentJson());
        response.setTags(candidate.getTags());
        response.setDifficulty(candidate.getDifficulty());
        response.setConfidenceScore(candidate.getConfidenceScore());
        response.setStatus(candidate.getStatus());
        response.setStatusText(candidateStatusText(candidate.getStatus()));
        response.setTargetModule(candidate.getTargetModule());
        response.setTargetId(candidate.getTargetId());
        response.setReviewNote(candidate.getReviewNote());
        return response;
    }

    private String recordStatusText(String status) {
        return LearningAssetRecordStatus.valueOf(status).getText();
    }

    private String candidateStatusText(String status) {
        return LearningAssetCandidateStatus.valueOf(status).getText();
    }

    private LearningAssetRecord requireOwnedRecord(Long userId, Long recordId) {
        LearningAssetRecord record = recordMapper.selectByUserIdAndId(userId, recordId);
        if (record == null) {
            throw new BusinessException("转化记录不存在");
        }
        return record;
    }
}
