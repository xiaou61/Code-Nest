package com.xiaou.oj.controller.admin;

import com.xiaou.common.annotation.Log;
import com.xiaou.common.annotation.RequireAdmin;
import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.core.domain.Result;
import com.xiaou.common.satoken.StpAdminUtil;
import com.xiaou.oj.domain.OjContest;
import com.xiaou.oj.dto.contest.ContestQueryRequest;
import com.xiaou.oj.dto.contest.ContestSaveRequest;
import com.xiaou.oj.service.OjContestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * OJ 赛事管理接口（管理端）
 *
 * @author xiaou
 */
@Tag(name = "OJ赛事管理", description = "赛事创建、编排、状态管理")
@RestController
@RequestMapping("/admin/oj/contests")
@RequiredArgsConstructor
public class OjContestController {

    private final OjContestService contestService;

    @Operation(summary = "分页查询赛事")
    @PostMapping("/list")
    @RequireAdmin
    public Result<PageResult<OjContest>> list(@RequestBody ContestQueryRequest request) {
        return Result.success(contestService.getContests(request));
    }

    @Operation(summary = "赛事详情")
    @GetMapping("/{id}")
    @RequireAdmin
    public Result<OjContest> detail(@PathVariable Long id) {
        return Result.success(contestService.getContestById(id));
    }

    @Operation(summary = "创建赛事")
    @PostMapping
    @RequireAdmin
    @Log(module = "OJ赛事", type = Log.OperationType.INSERT, description = "创建赛事")
    public Result<Long> create(@Valid @RequestBody ContestSaveRequest request) {
        Long adminId = StpAdminUtil.getLoginIdAsLong();
        return Result.success(contestService.createContest(adminId, request));
    }

    @Operation(summary = "更新赛事")
    @PutMapping("/{id}")
    @RequireAdmin
    @Log(module = "OJ赛事", type = Log.OperationType.UPDATE, description = "更新赛事")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ContestSaveRequest request) {
        contestService.updateContest(id, request);
        return Result.success();
    }

    @Operation(summary = "删除赛事")
    @DeleteMapping("/{id}")
    @RequireAdmin
    @Log(module = "OJ赛事", type = Log.OperationType.DELETE, description = "删除赛事")
    public Result<Void> delete(@PathVariable Long id) {
        contestService.deleteContest(id);
        return Result.success();
    }

    @Operation(summary = "更新赛事状态")
    @PostMapping("/{id}/status")
    @RequireAdmin
    @Log(module = "OJ赛事", type = Log.OperationType.UPDATE, description = "更新赛事状态")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody ContestStatusRequest request) {
        contestService.updateContestStatus(id, request.getStatus());
        return Result.success();
    }

    @Data
    public static class ContestStatusRequest {
        private Integer status;
    }
}
