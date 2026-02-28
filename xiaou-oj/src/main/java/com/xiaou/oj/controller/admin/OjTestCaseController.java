package com.xiaou.oj.controller.admin;

import com.xiaou.common.annotation.Log;
import com.xiaou.common.annotation.RequireAdmin;
import com.xiaou.common.core.domain.Result;
import com.xiaou.oj.domain.OjTestCase;
import com.xiaou.oj.service.OjTestCaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * OJ测试用例管理接口 (管理端)
 *
 * @author xiaou
 */
@Tag(name = "OJ测试用例管理", description = "OJ测试用例增删改查功能")
@RestController
@RequestMapping("/admin/oj/test-cases")
@RequiredArgsConstructor
public class OjTestCaseController {

    private final OjTestCaseService testCaseService;

    @Operation(summary = "添加测试用例")
    @PostMapping
    @RequireAdmin
    @Log(module = "OJ测试用例", type = Log.OperationType.INSERT, description = "添加测试用例")
    public Result<Long> addTestCase(@RequestBody OjTestCase testCase) {
        Long id = testCaseService.addTestCase(testCase);
        return Result.success(id);
    }

    @Operation(summary = "更新测试用例")
    @PutMapping("/{id}")
    @RequireAdmin
    @Log(module = "OJ测试用例", type = Log.OperationType.UPDATE, description = "更新测试用例")
    public Result<Void> updateTestCase(@PathVariable Long id, @RequestBody OjTestCase testCase) {
        testCaseService.updateTestCase(id, testCase);
        return Result.success();
    }

    @Operation(summary = "删除测试用例")
    @DeleteMapping("/{id}")
    @RequireAdmin
    @Log(module = "OJ测试用例", type = Log.OperationType.DELETE, description = "删除测试用例")
    public Result<Void> deleteTestCase(@PathVariable Long id) {
        testCaseService.deleteTestCase(id);
        return Result.success();
    }

    @Operation(summary = "获取题目的测试用例")
    @GetMapping("/problem/{problemId}")
    @RequireAdmin
    public Result<List<OjTestCase>> getTestCases(@PathVariable Long problemId) {
        return Result.success(testCaseService.getTestCasesByProblemId(problemId));
    }
}
