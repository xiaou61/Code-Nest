package com.xiaou.oj.service.impl;

import com.github.pagehelper.PageHelper;
import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.exception.BusinessException;
import com.xiaou.oj.domain.OjContest;
import com.xiaou.oj.domain.OjContestParticipant;
import com.xiaou.oj.dto.contest.ContestQueryRequest;
import com.xiaou.oj.dto.contest.ContestSaveRequest;
import com.xiaou.oj.mapper.OjContestMapper;
import com.xiaou.oj.mapper.OjContestParticipantMapper;
import com.xiaou.oj.mapper.OjContestProblemMapper;
import com.xiaou.oj.service.OjContestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * OJ 赛事服务实现
 *
 * @author xiaou
 */
@Service
@RequiredArgsConstructor
public class OjContestServiceImpl implements OjContestService {

    private final OjContestMapper contestMapper;
    private final OjContestProblemMapper contestProblemMapper;
    private final OjContestParticipantMapper contestParticipantMapper;

    @Override
    @Transactional
    public Long createContest(Long adminId, ContestSaveRequest request) {
        validateContestRequest(request);

        OjContest contest = new OjContest()
                .setTitle(request.getTitle())
                .setDescription(request.getDescription())
                .setContestType(request.getContestType())
                .setStatus(request.getStatus() == null ? 0 : request.getStatus())
                .setStartTime(request.getStartTime())
                .setEndTime(request.getEndTime())
                .setCreatedBy(adminId);
        contestMapper.insert(contest);
        replaceContestProblems(contest.getId(), request.getProblemIds());
        return contest.getId();
    }

    @Override
    @Transactional
    public void updateContest(Long contestId, ContestSaveRequest request) {
        validateContestRequest(request);
        OjContest existing = contestMapper.selectById(contestId);
        if (existing == null) {
            throw new BusinessException("赛事不存在");
        }

        OjContest contest = new OjContest()
                .setId(contestId)
                .setTitle(request.getTitle())
                .setDescription(request.getDescription())
                .setContestType(request.getContestType())
                .setStatus(request.getStatus() == null ? existing.getStatus() : request.getStatus())
                .setStartTime(request.getStartTime())
                .setEndTime(request.getEndTime());
        contestMapper.updateById(contest);
        replaceContestProblems(contestId, request.getProblemIds());
    }

    @Override
    @Transactional
    public void deleteContest(Long contestId) {
        contestProblemMapper.deleteByContestId(contestId);
        contestParticipantMapper.deleteByContestId(contestId);
        contestMapper.deleteById(contestId);
    }

    @Override
    public OjContest getContestById(Long contestId) {
        OjContest contest = contestMapper.selectById(contestId);
        if (contest == null) {
            throw new BusinessException("赛事不存在");
        }
        contest.setProblemIds(contestProblemMapper.selectProblemIdsByContestId(contestId));
        return contest;
    }

    @Override
    public PageResult<OjContest> getContests(ContestQueryRequest request) {
        PageHelper.startPage(request.getPageNum(), request.getPageSize());
        List<OjContest> records = contestMapper.selectPage(request);
        long total = contestMapper.countPage(request);
        return PageResult.of(request.getPageNum(), request.getPageSize(), total, records);
    }

    @Override
    public PageResult<OjContest> getPublicContests(ContestQueryRequest request) {
        PageHelper.startPage(request.getPageNum(), request.getPageSize());
        List<OjContest> records = contestMapper.selectPublicPage(request);
        long total = contestMapper.countPublicPage(request);
        return PageResult.of(request.getPageNum(), request.getPageSize(), total, records);
    }

    @Override
    @Transactional
    public void updateContestStatus(Long contestId, Integer status) {
        if (status == null) {
            throw new BusinessException("状态不能为空");
        }
        OjContest existing = contestMapper.selectById(contestId);
        if (existing == null) {
            throw new BusinessException("赛事不存在");
        }
        contestMapper.updateStatus(contestId, status);
    }

    @Override
    @Transactional
    public void joinContest(Long contestId, Long userId) {
        OjContest contest = contestMapper.selectById(contestId);
        if (contest == null) {
            throw new BusinessException("赛事不存在");
        }
        if (contest.getStatus() != null && contest.getStatus() == 0) {
            throw new BusinessException("赛事未发布");
        }
        if (contest.getEndTime() != null && LocalDateTime.now().isAfter(contest.getEndTime())) {
            throw new BusinessException("赛事已结束");
        }
        contestParticipantMapper.insert(new OjContestParticipant()
                .setContestId(contestId)
                .setUserId(userId));
    }

    private void validateContestRequest(ContestSaveRequest request) {
        if (request == null) {
            throw new BusinessException("请求参数不能为空");
        }
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new BusinessException("赛事标题不能为空");
        }
        if (request.getContestType() == null || request.getContestType().isBlank()) {
            throw new BusinessException("赛事类型不能为空");
        }
        validateContestWindow(request.getStartTime(), request.getEndTime());
        if (request.getProblemIds() == null || request.getProblemIds().isEmpty()) {
            throw new BusinessException("赛事至少需要1道题目");
        }
    }

    private void validateContestWindow(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            throw new BusinessException("开始时间和结束时间不能为空");
        }
        if (!endTime.isAfter(startTime)) {
            throw new BusinessException("结束时间必须晚于开始时间");
        }
    }

    private void replaceContestProblems(Long contestId, List<Long> problemIds) {
        List<Long> uniqueProblemIds = new java.util.ArrayList<>(new LinkedHashSet<>(problemIds));
        contestProblemMapper.deleteByContestId(contestId);
        if (!uniqueProblemIds.isEmpty()) {
            contestProblemMapper.insertBatch(contestId, uniqueProblemIds);
        }
    }
}
