package com.xiaou.oj.controller.admin;

import com.xiaou.common.annotation.Log;
import com.xiaou.common.annotation.RequireAdmin;
import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.core.domain.Result;
import com.xiaou.oj.domain.OjProblem;
import com.xiaou.oj.domain.OjProblemTag;
import com.xiaou.oj.dto.ProblemQueryRequest;
import com.xiaou.oj.mapper.OjProblemTagMapper;
import com.xiaou.oj.service.OjProblemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * OJ题目管理接口 (管理端)
 *
 * @author xiaou
 */
@Tag(name = "OJ题目管理", description = "OJ题目增删改查功能")
@RestController
@RequestMapping("/admin/oj/problems")
@RequiredArgsConstructor
public class OjProblemController {

    private final OjProblemService problemService;
    private final OjProblemTagMapper tagMapper;

    @Operation(summary = "创建题目")
    @PostMapping
    @RequireAdmin
    @Log(module = "OJ题目", type = Log.OperationType.INSERT, description = "创建题目")
    public Result<Long> createProblem(@RequestBody OjProblem problem,
                                     @RequestParam(required = false) List<Long> tagIds) {
        Long id = problemService.createProblem(problem, tagIds);
        return Result.success(id);
    }

    @Operation(summary = "更新题目")
    @PutMapping("/{id}")
    @RequireAdmin
    @Log(module = "OJ题目", type = Log.OperationType.UPDATE, description = "更新题目")
    public Result<Void> updateProblem(@PathVariable Long id,
                                     @RequestBody OjProblem problem,
                                     @RequestParam(required = false) List<Long> tagIds) {
        problemService.updateProblem(id, problem, tagIds);
        return Result.success();
    }

    @Operation(summary = "删除题目")
    @DeleteMapping("/{id}")
    @RequireAdmin
    @Log(module = "OJ题目", type = Log.OperationType.DELETE, description = "删除题目")
    public Result<Void> deleteProblem(@PathVariable Long id) {
        problemService.deleteProblem(id);
        return Result.success();
    }

    @Operation(summary = "获取题目详情")
    @GetMapping("/{id}")
    @RequireAdmin
    public Result<OjProblem> getProblem(@PathVariable Long id) {
        return Result.success(problemService.getProblemById(id));
    }

    @Operation(summary = "分页查询题目")
    @PostMapping("/list")
    @RequireAdmin
    public Result<PageResult<OjProblem>> getProblems(@RequestBody ProblemQueryRequest request) {
        return Result.success(problemService.getProblems(request));
    }

    @Operation(summary = "获取所有标签")
    @GetMapping("/tags")
    @RequireAdmin
    public Result<List<OjProblemTag>> getTags() {
        return Result.success(tagMapper.selectAll());
    }

    @Operation(summary = "创建标签")
    @PostMapping("/tags")
    @RequireAdmin
    @Log(module = "OJ标签", type = Log.OperationType.INSERT, description = "创建标签")
    public Result<Long> createTag(@RequestBody OjProblemTag tag) {
        tagMapper.insert(tag);
        return Result.success(tag.getId());
    }
}
