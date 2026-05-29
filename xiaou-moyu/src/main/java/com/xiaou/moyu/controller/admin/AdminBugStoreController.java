package com.xiaou.moyu.controller.admin;

import com.xiaou.common.annotation.Log;
import com.xiaou.common.annotation.RequireAdmin;
import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.core.domain.Result;
import com.xiaou.common.exception.BusinessException;
import com.xiaou.common.satoken.StpAdminUtil;
import com.xiaou.moyu.domain.BugItem;
import com.xiaou.moyu.dto.AdminBugItemRequest;
import com.xiaou.moyu.dto.BugItemQueryRequest;
import com.xiaou.moyu.service.BugStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import java.util.List;

/**
 * 管理端Bug商店控制器
 *
 * @author xiaou
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/admin/moyu/bug-store")
@RequiredArgsConstructor
public class AdminBugStoreController {
    
    private final BugStoreService bugStoreService;
    
    /**
     * 分页查询Bug列表
     */
    @Log(module = "摸鱼工具", type = Log.OperationType.SELECT, description = "获取Bug列表")
    @PostMapping("/list")
    @RequireAdmin
    public Result<PageResult<BugItem>> getBugList(@Valid @RequestBody BugItemQueryRequest query) {
        PageResult<BugItem> result = bugStoreService.getBugList(query);
        return Result.success(result);
    }
    
    /**
     * 根据ID获取Bug详情
     */
    @Log(module = "摸鱼工具", type = Log.OperationType.SELECT, description = "获取Bug详情")
    @GetMapping("/{id}")
    @RequireAdmin
    public Result<BugItem> getBugById(@PathVariable @Positive(message = "Bug ID必须大于0") Long id) {
        BugItem bugItem = bugStoreService.getBugById(id);
        if (bugItem == null) {
            throw new BusinessException("Bug不存在");
        }
        return Result.success(bugItem);
    }
    
    /**
     * 添加Bug
     */
    @Log(module = "摸鱼工具", type = Log.OperationType.INSERT, description = "添加Bug")
    @PostMapping
    @RequireAdmin
    public Result<Long> addBug(@Valid @RequestBody AdminBugItemRequest request) {
        Long adminId = StpAdminUtil.getLoginIdAsLong();
        Long bugId = bugStoreService.addBug(request, adminId);
        if (bugId == null) {
            throw new BusinessException("添加Bug失败");
        }
        return Result.success(bugId);
    }
    
    /**
     * 更新Bug
     */
    @Log(module = "摸鱼工具", type = Log.OperationType.UPDATE, description = "更新Bug")
    @PutMapping("/{id}")
    @RequireAdmin
    public Result<Void> updateBug(@PathVariable @Positive(message = "Bug ID必须大于0") Long id,
                                  @Valid @RequestBody AdminBugItemRequest request) {
        request.setId(id);

        boolean success = bugStoreService.updateBug(request);
        if (!success) {
            throw new BusinessException("更新Bug失败");
        }
        return Result.success();
    }
    
    /**
     * 删除Bug
     */
    @Log(module = "摸鱼工具", type = Log.OperationType.DELETE, description = "删除Bug")
    @DeleteMapping("/{id}")
    @RequireAdmin
    public Result<Void> deleteBug(@PathVariable @Positive(message = "Bug ID必须大于0") Long id) {
        boolean success = bugStoreService.deleteBug(id);
        if (!success) {
            throw new BusinessException("删除Bug失败");
        }
        return Result.success();
    }
    
    /**
     * 批量导入Bug
     */
    @Log(module = "摸鱼工具", type = Log.OperationType.INSERT, description = "批量导入Bug")
    @PostMapping("/batch-import")
    @RequireAdmin
    public Result<Void> batchImportBugs(@RequestBody @NotEmpty(message = "导入数据不能为空")
                                        List<@Valid AdminBugItemRequest> requests) {
        Long adminId = StpAdminUtil.getLoginIdAsLong();
        boolean success = bugStoreService.batchImportBugs(requests, adminId);
        if (!success) {
            throw new BusinessException("批量导入Bug失败");
        }
        return Result.success();
    }
}
