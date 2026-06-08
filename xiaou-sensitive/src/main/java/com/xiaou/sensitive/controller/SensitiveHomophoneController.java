package com.xiaou.sensitive.controller;

import com.xiaou.common.annotation.RequireAdmin;
import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.core.domain.Result;
import com.xiaou.common.core.domain.ResultCode;
import com.xiaou.sensitive.domain.SensitiveHomophone;
import com.xiaou.sensitive.dto.SensitiveHomophoneQuery;
import com.xiaou.sensitive.service.SensitiveHomophoneService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 同音字映射管理控制器
 *
 * @author xiaou
 */
@RestController
@RequestMapping("/sensitive/homophone")
@RequiredArgsConstructor
public class SensitiveHomophoneController {

    private final SensitiveHomophoneService homophoneService;

    /**
     * 查询同音字列表
     */
    @PostMapping("/list")
    @RequireAdmin
    public Result<PageResult<SensitiveHomophone>> list(@RequestBody SensitiveHomophoneQuery query) {
        PageResult<SensitiveHomophone> result = homophoneService.listHomophones(query);
        return Result.success(result);
    }

    /**
     * 获取同音字详情
     */
    @GetMapping("/{id}")
    @RequireAdmin
    public Result<SensitiveHomophone> getById(@PathVariable Integer id) {
        SensitiveHomophone homophone = homophoneService.getHomophoneById(id);
        if (homophone == null) {
            return Result.error(ResultCode.DATA_NOT_EXIST.getCode(), "同音字不存在");
        }
        return Result.success(homophone);
    }

    /**
     * 新增同音字
     */
    @PostMapping("/add")
    @RequireAdmin
    public Result<Void> add(@RequestBody SensitiveHomophone homophone) {
        boolean success = homophoneService.addHomophone(homophone);
        return success ? Result.success() : Result.error(ResultCode.BUSINESS_ERROR.getCode(), "新增同音字失败");
    }

    /**
     * 批量新增同音字
     */
    @PostMapping("/batch-add")
    @RequireAdmin
    public Result<Void> batchAdd(@RequestBody List<SensitiveHomophone> homophones) {
        if (homophones == null || homophones.isEmpty()) {
            return Result.error(ResultCode.PARAM_VALIDATE_ERROR.getCode(), "同音字列表不能为空");
        }
        boolean success = homophoneService.batchAddHomophones(homophones);
        return success ? Result.success() : Result.error(ResultCode.BUSINESS_ERROR.getCode(), "批量新增同音字失败");
    }

    /**
     * 更新同音字
     */
    @PostMapping("/update")
    @RequireAdmin
    public Result<Void> update(@RequestBody SensitiveHomophone homophone) {
        if (homophone.getId() == null) {
            return Result.error(ResultCode.PARAM_VALIDATE_ERROR.getCode(), "ID不能为空");
        }
        boolean success = homophoneService.updateHomophone(homophone);
        return success ? Result.success() : Result.error(ResultCode.DATA_NOT_EXIST.getCode(), "同音字不存在");
    }

    /**
     * 删除同音字
     */
    @PostMapping("/delete/{id}")
    @RequireAdmin
    public Result<Void> delete(@PathVariable Integer id) {
        boolean success = homophoneService.deleteHomophone(id);
        return success ? Result.success() : Result.error(ResultCode.DATA_NOT_EXIST.getCode(), "同音字不存在");
    }

    /**
     * 刷新同音字缓存
     */
    @PostMapping("/refresh")
    @RequireAdmin
    public Result<Void> refreshCache() {
        homophoneService.refreshCache();
        return Result.success();
    }
}
