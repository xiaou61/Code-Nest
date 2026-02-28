package com.xiaou.oj.service.impl;

import com.xiaou.oj.domain.OjSolution;
import com.xiaou.oj.mapper.OjSolutionMapper;
import com.xiaou.oj.service.OjSolutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * OJ标准答案服务实现
 *
 * @author xiaou
 */
@Service
@RequiredArgsConstructor
public class OjSolutionServiceImpl implements OjSolutionService {

    private final OjSolutionMapper solutionMapper;

    @Override
    public Long addSolution(OjSolution solution) {
        if (solution.getSortOrder() == null) {
            int count = solutionMapper.countByProblemId(solution.getProblemId());
            solution.setSortOrder(count + 1);
        }
        solutionMapper.insert(solution);
        return solution.getId();
    }

    @Override
    public void updateSolution(Long id, OjSolution solution) {
        solution.setId(id);
        solutionMapper.updateById(solution);
    }

    @Override
    public void deleteSolution(Long id) {
        solutionMapper.deleteById(id);
    }

    @Override
    public List<OjSolution> getSolutionsByProblemId(Long problemId) {
        return solutionMapper.selectByProblemId(problemId);
    }
}
