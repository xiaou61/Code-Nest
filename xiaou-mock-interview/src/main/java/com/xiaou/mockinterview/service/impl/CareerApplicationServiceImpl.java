package com.xiaou.mockinterview.service.impl;

import com.xiaou.common.exception.BusinessException;
import com.xiaou.mockinterview.domain.CareerApplicationRecord;
import com.xiaou.mockinterview.domain.CareerLoopSession;
import com.xiaou.mockinterview.domain.MockInterviewSession;
import com.xiaou.mockinterview.dto.request.CareerApplicationUpsertRequest;
import com.xiaou.mockinterview.dto.request.CareerLoopStartRequest;
import com.xiaou.mockinterview.dto.response.CareerApplicationResponse;
import com.xiaou.mockinterview.dto.response.CareerApplicationSummaryResponse;
import com.xiaou.mockinterview.enums.CareerApplicationStatusEnum;
import com.xiaou.mockinterview.mapper.CareerApplicationRecordMapper;
import com.xiaou.mockinterview.mapper.CareerLoopSessionMapper;
import com.xiaou.mockinterview.mapper.JobBattleMatchRecordMapper;
import com.xiaou.mockinterview.mapper.JobBattlePlanRecordMapper;
import com.xiaou.mockinterview.mapper.MockInterviewSessionMapper;
import com.xiaou.mockinterview.service.CareerApplicationService;
import com.xiaou.mockinterview.service.CareerLoopService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 仅管理当前用户主动维护的投递记录，不代表平台验证的投递结果。
 */
@Service
@RequiredArgsConstructor
public class CareerApplicationServiceImpl implements CareerApplicationService {

    private final CareerApplicationRecordMapper applicationRecordMapper;
    private final CareerLoopSessionMapper sessionMapper;
    private final CareerLoopService careerLoopService;
    private final JobBattleMatchRecordMapper matchRecordMapper;
    private final JobBattlePlanRecordMapper planRecordMapper;
    private final MockInterviewSessionMapper mockInterviewSessionMapper;

    @Override
    public List<CareerApplicationResponse> listForUser(Long userId, String status) {
        if (userId == null || userId <= 0) {
            return List.of();
        }
        String normalizedStatus = normalizeFilterStatus(status);
        return applicationRecordMapper.selectByUser(userId, normalizedStatus).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public CareerApplicationSummaryResponse getSummary(Long userId) {
        List<CareerApplicationRecord> records = userId == null || userId <= 0
                ? List.of()
                : applicationRecordMapper.selectByUser(userId, null);
        CareerApplicationSummaryResponse summary = new CareerApplicationSummaryResponse();
        summary.setTotalCount(records.size());
        summary.setPreparingCount(count(records, CareerApplicationStatusEnum.PREPARING));
        summary.setAppliedCount(count(records, CareerApplicationStatusEnum.APPLIED));
        summary.setInterviewingCount(count(records, CareerApplicationStatusEnum.INTERVIEWING));
        summary.setOfferCount(count(records, CareerApplicationStatusEnum.OFFER));
        summary.setRejectedCount(count(records, CareerApplicationStatusEnum.REJECTED));
        summary.setWithdrawnCount(count(records, CareerApplicationStatusEnum.WITHDRAWN));
        summary.setActiveCount((int) records.stream()
                .filter(record -> !statusOf(record).isTerminal())
                .count());
        summary.setDueFollowUpCount((int) records.stream()
                .filter(record -> !statusOf(record).isTerminal())
                .map(CareerApplicationRecord::getNextFollowUpDate)
                .filter(Objects::nonNull)
                .filter(date -> !date.isAfter(LocalDate.now()))
                .count());
        summary.setNextFollowUpDate(records.stream()
                .filter(record -> !statusOf(record).isTerminal())
                .map(CareerApplicationRecord::getNextFollowUpDate)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(null));
        summary.setLatestUpdatedAt(records.stream()
                .map(CareerApplicationRecord::getUpdateTime)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null));
        return summary;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CareerApplicationResponse create(Long userId, CareerApplicationUpsertRequest request) {
        CareerLoopSession session = ensureActiveSession(userId);
        CareerApplicationRecord record = new CareerApplicationRecord()
                .setUserId(userId)
                .setSessionId(session.getId());
        applyRequest(record, request, userId);
        applicationRecordMapper.insert(record);
        return toResponse(requireOwned(record.getId(), userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CareerApplicationResponse update(Long userId, Long recordId, CareerApplicationUpsertRequest request) {
        CareerApplicationRecord record = requireOwned(recordId, userId);
        applyRequest(record, request, userId);
        if (applicationRecordMapper.updateOwned(record) != 1) {
            throw new BusinessException("投递记录已变化，请刷新后重试");
        }
        return toResponse(requireOwned(recordId, userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long recordId) {
        if (recordId == null || recordId <= 0 || applicationRecordMapper.logicalDeleteOwned(recordId, userId) != 1) {
            throw new BusinessException("投递记录不存在");
        }
    }

    private CareerLoopSession ensureActiveSession(Long userId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException("用户信息无效");
        }
        CareerLoopSession session = sessionMapper.selectActiveByUserId(userId);
        if (session != null) {
            return session;
        }
        return careerLoopService.start(userId, new CareerLoopStartRequest());
    }

    private CareerApplicationRecord requireOwned(Long recordId, Long userId) {
        if (recordId == null || recordId <= 0 || userId == null || userId <= 0) {
            throw new BusinessException("投递记录不存在");
        }
        CareerApplicationRecord record = applicationRecordMapper.selectByIdAndUser(recordId, userId);
        if (record == null) {
            throw new BusinessException("投递记录不存在");
        }
        return record;
    }

    private void applyRequest(CareerApplicationRecord record, CareerApplicationUpsertRequest request, Long userId) {
        if (request == null) {
            throw new BusinessException("投递记录不能为空");
        }
        CareerApplicationStatusEnum status = CareerApplicationStatusEnum.require(request.getStatus());
        if (status != CareerApplicationStatusEnum.PREPARING && request.getAppliedDate() == null) {
            throw new BusinessException("已投递后的状态需要填写投递日期");
        }
        if (status.isTerminal() && request.getNextFollowUpDate() != null) {
            throw new BusinessException("终态投递记录不能设置下一次跟进日期");
        }
        validateSourceOwnership(userId, request);
        record.setMatchRecordId(request.getMatchRecordId());
        record.setPlanRecordId(request.getPlanRecordId());
        record.setMockInterviewSessionId(request.getMockInterviewSessionId());
        record.setCompanyName(normalizeRequired(request.getCompanyName(), "公司名称不能为空"));
        record.setPositionName(normalizeRequired(request.getPositionName(), "岗位名称不能为空"));
        record.setStatus(status.name());
        record.setAppliedDate(request.getAppliedDate());
        record.setNextFollowUpDate(request.getNextFollowUpDate());
        record.setNote(normalizeOptional(request.getNote()));
    }

    private CareerApplicationResponse toResponse(CareerApplicationRecord record) {
        CareerApplicationStatusEnum status = statusOf(record);
        CareerApplicationResponse response = new CareerApplicationResponse();
        response.setId(record.getId());
        response.setMatchRecordId(record.getMatchRecordId());
        response.setPlanRecordId(record.getPlanRecordId());
        response.setMockInterviewSessionId(record.getMockInterviewSessionId());
        response.setCompanyName(record.getCompanyName());
        response.setPositionName(record.getPositionName());
        response.setStatus(status.name());
        response.setStatusLabel(status.getLabel());
        response.setAppliedDate(record.getAppliedDate());
        response.setNextFollowUpDate(record.getNextFollowUpDate());
        response.setNote(record.getNote());
        response.setCreateTime(record.getCreateTime());
        response.setUpdateTime(record.getUpdateTime());
        return response;
    }

    private void validateSourceOwnership(Long userId, CareerApplicationUpsertRequest request) {
        Long matchRecordId = request.getMatchRecordId();
        if (matchRecordId != null
                && (matchRecordId <= 0 || matchRecordMapper.selectByIdAndUserId(matchRecordId, userId) == null)) {
            throw new BusinessException("岗位匹配记录不存在或无权访问");
        }

        Long planRecordId = request.getPlanRecordId();
        if (planRecordId != null
                && (planRecordId <= 0 || planRecordMapper.selectByIdAndUserId(planRecordId, userId) == null)) {
            throw new BusinessException("补短板计划记录不存在或无权访问");
        }

        Long interviewSessionId = request.getMockInterviewSessionId();
        if (interviewSessionId != null) {
            MockInterviewSession session = interviewSessionId <= 0
                    ? null
                    : mockInterviewSessionMapper.selectById(interviewSessionId);
            if (session == null || !Objects.equals(userId, session.getUserId())) {
                throw new BusinessException("模拟面试记录不存在或无权访问");
            }
        }
    }

    private int count(List<CareerApplicationRecord> records, CareerApplicationStatusEnum expected) {
        return (int) records.stream().filter(record -> statusOf(record) == expected).count();
    }

    private CareerApplicationStatusEnum statusOf(CareerApplicationRecord record) {
        return CareerApplicationStatusEnum.require(record == null ? null : record.getStatus());
    }

    private String normalizeFilterStatus(String status) {
        return StringUtils.hasText(status) ? CareerApplicationStatusEnum.require(status).name() : null;
    }

    private String normalizeRequired(String value, String message) {
        String normalized = normalizeOptional(value);
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException(message);
        }
        return normalized;
    }

    private String normalizeOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
