package com.xiaou.oj.service.impl;

import com.github.pagehelper.PageHelper;
import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.exception.BusinessException;
import com.xiaou.oj.contest.ContestRuleValidator;
import com.xiaou.oj.domain.OjContest;
import com.xiaou.oj.domain.OjProblem;
import com.xiaou.oj.domain.OjSubmission;
import com.xiaou.oj.dto.OjStatisticsVO;
import com.xiaou.oj.dto.SubmissionQueryRequest;
import com.xiaou.oj.dto.SubmitCodeRequest;
import com.xiaou.oj.enums.JudgeLanguage;
import com.xiaou.oj.enums.SubmissionStatus;
import com.xiaou.oj.judge.JudgeService;
import com.xiaou.oj.mapper.OjContestMapper;
import com.xiaou.oj.mapper.OjContestParticipantMapper;
import com.xiaou.oj.mapper.OjContestProblemMapper;
import com.xiaou.oj.mapper.OjProblemMapper;
import com.xiaou.oj.mapper.OjSubmissionMapper;
import com.xiaou.oj.service.OjSubmissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * OJ提交记录服务实现
 *
 * @author xiaou
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OjSubmissionServiceImpl implements OjSubmissionService {

    private final OjSubmissionMapper submissionMapper;
    private final OjProblemMapper problemMapper;
    private final OjContestMapper contestMapper;
    private final OjContestProblemMapper contestProblemMapper;
    private final OjContestParticipantMapper contestParticipantMapper;
    private final JudgeService judgeService;

    @Override
    public Long submitCode(Long userId, SubmitCodeRequest request) {
        // 校验语言
        JudgeLanguage.of(request.getLanguage());

        // 校验题目
        OjProblem problem = problemMapper.selectById(request.getProblemId());
        if (problem == null) {
            throw new BusinessException("题目不存在");
        }

        if (request.getContestId() != null) {
            validateContestSubmit(request.getContestId(), userId, request.getProblemId());
        }

        // 创建提交记录
        OjSubmission submission = new OjSubmission()
                .setProblemId(request.getProblemId())
                .setContestId(request.getContestId())
                .setUserId(userId)
                .setLanguage(request.getLanguage())
                .setCode(request.getCode())
                .setStatus(SubmissionStatus.PENDING.getValue());
        submissionMapper.insert(submission);

        // 增加提交数
        problemMapper.increaseSubmitCount(request.getProblemId());

        // 异步判题
        judgeService.judge(submission.getId());

        return submission.getId();
    }

    @Override
    public OjSubmission getSubmissionById(Long id) {
        OjSubmission submission = submissionMapper.selectById(id);
        if (submission != null && "accepted".equals(submission.getStatus())) {
            // 检查是否为首次AC（当前提交是该题最早的accepted）
            boolean isFirstAc = submissionMapper.isFirstAccepted(submission.getUserId(), submission.getProblemId(), id);
            if (isFirstAc) {
                OjProblem problem = problemMapper.selectById(submission.getProblemId());
                if (problem != null) {
                    submission.setPointsEarned(getAcPoints(problem.getDifficulty()));
                }
            }
        }
        return submission;
    }

    private int getAcPoints(String difficulty) {
        if ("hard".equals(difficulty)) return 500;
        if ("medium".equals(difficulty)) return 200;
        return 100;
    }

    @Override
    public PageResult<OjSubmission> getSubmissions(SubmissionQueryRequest request) {
        PageHelper.startPage(request.getPageNum(), request.getPageSize());
        List<OjSubmission> list = submissionMapper.selectPage(request);
        long total = submissionMapper.countPage(request);
        return PageResult.of(request.getPageNum(), request.getPageSize(), total, list);
    }

    @Override
    public OjStatisticsVO getStatistics(Long userId) {
        OjStatisticsVO vo = new OjStatisticsVO();
        vo.setTotalSubmissions(submissionMapper.countByUserId(userId));
        vo.setAcceptedProblems(submissionMapper.countAcceptedProblemsByUserId(userId));
        vo.setAttemptedProblems(submissionMapper.countAttemptedProblemsByUserId(userId));
        vo.setEasyAccepted(submissionMapper.countAcceptedByDifficulty(userId, "easy"));
        vo.setMediumAccepted(submissionMapper.countAcceptedByDifficulty(userId, "medium"));
        vo.setHardAccepted(submissionMapper.countAcceptedByDifficulty(userId, "hard"));
        return vo;
    }

    private void validateContestSubmit(Long contestId, Long userId, Long problemId) {
        OjContest contest = contestMapper.selectById(contestId);
        if (contest == null) {
            throw new BusinessException("赛事不存在");
        }
        ContestRuleValidator.checkCanSubmit(contest, LocalDateTime.now());

        boolean joined = contestParticipantMapper.existsByContestIdAndUserId(contestId, userId);
        if (!joined) {
            throw new BusinessException("请先报名赛事");
        }

        boolean existsProblem = contestProblemMapper.existsProblemInContest(contestId, problemId);
        if (!existsProblem) {
            throw new BusinessException("题目不属于该赛事");
        }
    }
}
