package com.xiaou.oj.service.impl;

import com.github.pagehelper.PageHelper;
import com.xiaou.common.core.domain.PageResult;
import com.xiaou.oj.domain.OjProblem;
import com.xiaou.oj.domain.OjProblemTag;
import com.xiaou.oj.dto.ProblemQueryRequest;
import com.xiaou.oj.mapper.OjProblemMapper;
import com.xiaou.oj.mapper.OjProblemTagMapper;
import com.xiaou.oj.mapper.OjTestCaseMapper;
import com.xiaou.oj.service.OjProblemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * OJ题目服务实现
 *
 * @author xiaou
 */
@Service
@RequiredArgsConstructor
public class OjProblemServiceImpl implements OjProblemService {

    private final OjProblemMapper problemMapper;
    private final OjProblemTagMapper tagMapper;
    private final OjTestCaseMapper testCaseMapper;

    @Override
    @Transactional
    public Long createProblem(OjProblem problem, List<Long> tagIds) {
        problem.setAcceptedCount(0);
        problem.setSubmitCount(0);
        if (problem.getStatus() == null) {
            problem.setStatus(0);
        }
        problemMapper.insert(problem);

        // 关联标签
        if (tagIds != null && !tagIds.isEmpty()) {
            for (Long tagId : tagIds) {
                problemMapper.insertProblemTag(problem.getId(), tagId);
            }
        }
        return problem.getId();
    }

    @Override
    @Transactional
    public void updateProblem(Long id, OjProblem problem, List<Long> tagIds) {
        problem.setId(id);
        problemMapper.updateById(problem);

        // 更新标签关联
        if (tagIds != null) {
            problemMapper.deleteProblemTags(id);
            for (Long tagId : tagIds) {
                problemMapper.insertProblemTag(id, tagId);
            }
        }
    }

    @Override
    @Transactional
    public void deleteProblem(Long id) {
        problemMapper.deleteById(id);
        problemMapper.deleteProblemTags(id);
        testCaseMapper.deleteByProblemId(id);
    }

    @Override
    public OjProblem getProblemById(Long id) {
        OjProblem problem = problemMapper.selectById(id);
        if (problem != null) {
            // 加载标签
            List<Long> tagIds = problemMapper.selectTagIdsByProblemId(id);
            if (!tagIds.isEmpty()) {
                List<OjProblemTag> tags = tagMapper.selectByIds(tagIds);
                problem.setTags(tags);
            }
        }
        return problem;
    }

    @Override
    public PageResult<OjProblem> getProblems(ProblemQueryRequest request) {
        PageHelper.startPage(request.getPageNum(), request.getPageSize());
        List<OjProblem> list = problemMapper.selectPage(request);
        long total = problemMapper.countPage(request);
        return PageResult.of(request.getPageNum(), request.getPageSize(), total, list);
    }

    @Override
    public OjProblem getDailyProblem() {
        long count = problemMapper.countPublic();
        if (count == 0) return null;
        int offset = (int) (Math.abs((long) LocalDate.now().hashCode()) % count);
        OjProblem problem = problemMapper.selectPublicByOffset(offset);
        if (problem != null) {
            List<Long> tagIds = problemMapper.selectTagIdsByProblemId(problem.getId());
            if (!tagIds.isEmpty()) {
                problem.setTags(tagMapper.selectByIds(tagIds));
            }
        }
        return problem;
    }
}
