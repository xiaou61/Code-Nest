package com.xiaou.oj.service;

import com.xiaou.common.core.domain.PageResult;
import com.xiaou.oj.domain.OjProblem;
import com.xiaou.oj.dto.ProblemQueryRequest;

import java.util.List;

/**
 * OJ题目服务接口
 *
 * @author xiaou
 */
public interface OjProblemService {

    Long createProblem(OjProblem problem, List<Long> tagIds);

    void updateProblem(Long id, OjProblem problem, List<Long> tagIds);

    void deleteProblem(Long id);

    OjProblem getProblemById(Long id);

    PageResult<OjProblem> getProblems(ProblemQueryRequest request);
}
