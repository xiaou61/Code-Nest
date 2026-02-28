package com.xiaou.oj.service;

import com.xiaou.oj.domain.OjSolution;

import java.util.List;

/**
 * OJ标准答案服务接口
 *
 * @author xiaou
 */
public interface OjSolutionService {

    Long addSolution(OjSolution solution);

    void updateSolution(Long id, OjSolution solution);

    void deleteSolution(Long id);

    List<OjSolution> getSolutionsByProblemId(Long problemId);
}
