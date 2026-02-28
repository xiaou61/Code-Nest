package com.xiaou.oj.controller.admin;

import com.xiaou.common.annotation.Log;
import com.xiaou.common.annotation.RequireAdmin;
import com.xiaou.common.core.domain.Result;
import com.xiaou.oj.domain.OjSolution;
import com.xiaou.oj.service.OjSolutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * OJ标准答案管理接口 (管理端)
 *
 * @author xiaou
 */
@Tag(name = "OJ标准答案管理", description = "OJ标准答案增删改查功能")
@RestController
@RequestMapping("/admin/oj/solutions")
@RequiredArgsConstructor
public class OjSolutionController {

    private final OjSolutionService solutionService;

    @Operation(summary = "添加标准答案")
    @PostMapping
    @RequireAdmin
    @Log(module = "OJ标准答案", type = Log.OperationType.INSERT, description = "添加标准答案")
    public Result<Long> addSolution(@RequestBody OjSolution solution) {
        Long id = solutionService.addSolution(solution);
        return Result.success(id);
    }

    @Operation(summary = "更新标准答案")
    @PutMapping("/{id}")
    @RequireAdmin
    @Log(module = "OJ标准答案", type = Log.OperationType.UPDATE, description = "更新标准答案")
    public Result<Void> updateSolution(@PathVariable Long id, @RequestBody OjSolution solution) {
        solutionService.updateSolution(id, solution);
        return Result.success();
    }

    @Operation(summary = "删除标准答案")
    @DeleteMapping("/{id}")
    @RequireAdmin
    @Log(module = "OJ标准答案", type = Log.OperationType.DELETE, description = "删除标准答案")
    public Result<Void> deleteSolution(@PathVariable Long id) {
        solutionService.deleteSolution(id);
        return Result.success();
    }

    @Operation(summary = "获取题目的标准答案")
    @GetMapping("/problem/{problemId}")
    @RequireAdmin
    public Result<List<OjSolution>> getSolutions(@PathVariable Long problemId) {
        return Result.success(solutionService.getSolutionsByProblemId(problemId));
    }
}
