package com.xiaou.oj.service.impl;

import com.github.pagehelper.PageHelper;
import com.xiaou.common.core.domain.PageResult;
import com.xiaou.oj.domain.OjProblem;
import com.xiaou.oj.domain.OjSubmission;
import com.xiaou.oj.dto.OjStatisticsVO;
import com.xiaou.oj.dto.SubmissionQueryRequest;
import com.xiaou.oj.dto.SubmitCodeRequest;
import com.xiaou.oj.enums.JudgeLanguage;
import com.xiaou.oj.enums.SubmissionStatus;
import com.xiaou.oj.judge.JudgeService;
import com.xiaou.oj.mapper.OjProblemMapper;
import com.xiaou.oj.mapper.OjSubmissionMapper;
import com.xiaou.oj.service.OjSubmissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
    private final JudgeService judgeService;

    @Override
    public Long submitCode(Long userId, SubmitCodeRequest request) {
        // 校验语言
        JudgeLanguage.of(request.getLanguage());

        // 校验题目
        OjProblem problem = problemMapper.selectById(request.getProblemId());
        if (problem == null) {
            throw new RuntimeException("题目不存在");
        }

        // 创建提交记录
        OjSubmission submission = new OjSubmission()
                .setProblemId(request.getProblemId())
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
        return submissionMapper.selectById(id);
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
}
