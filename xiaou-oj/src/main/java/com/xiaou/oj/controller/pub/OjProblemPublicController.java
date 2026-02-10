package com.xiaou.oj.controller.pub;

import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.core.domain.Result;
import com.xiaou.oj.domain.OjProblem;
import com.xiaou.oj.domain.OjProblemTag;
import com.xiaou.oj.domain.OjSolution;
import com.xiaou.oj.dto.ProblemQueryRequest;
import com.xiaou.oj.mapper.OjProblemTagMapper;
import com.xiaou.oj.service.OjProblemService;
import com.xiaou.oj.service.OjSolutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * OJ题目公开接口 (用户端)
 *
 * @author xiaou
 */
@Tag(name = "OJ题目", description = "OJ题目查询功能")
@RestController
@RequestMapping("/oj")
@RequiredArgsConstructor
public class OjProblemPublicController {

    private final OjProblemService problemService;
    private final OjProblemTagMapper tagMapper;
    private final OjSolutionService solutionService;

    @Operation(summary = "分页查询公开题目")
    @PostMapping("/problems/list")
    public Result<PageResult<OjProblem>> getProblems(@RequestBody ProblemQueryRequest request) {
        request.setStatus(1); // 只查询公开题目
        return Result.success(problemService.getProblems(request));
    }

    @Operation(summary = "获取题目详情")
    @GetMapping("/problems/{id}")
    public Result<OjProblem> getProblem(@PathVariable Long id) {
        OjProblem problem = problemService.getProblemById(id);
        if (problem != null && problem.getStatus() != null && problem.getStatus() == 0) {
            return Result.error("题目不存在或未公开");
        }
        return Result.success(problem);
    }

    @Operation(summary = "获取所有标签")
    @GetMapping("/tags")
    public Result<List<OjProblemTag>> getTags() {
        return Result.success(tagMapper.selectAll());
    }

    @Operation(summary = "获取题目的标准答案")
    @GetMapping("/problems/{id}/solutions")
    public Result<List<OjSolution>> getSolutions(@PathVariable Long id) {
        return Result.success(solutionService.getSolutionsByProblemId(id));
    }
}
