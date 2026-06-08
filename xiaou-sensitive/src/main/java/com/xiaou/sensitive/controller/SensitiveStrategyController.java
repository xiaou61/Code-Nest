package com.xiaou.sensitive.controller;

import com.xiaou.common.annotation.RequireAdmin;
import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.core.domain.Result;
import com.xiaou.common.core.domain.ResultCode;
import com.xiaou.sensitive.domain.SensitiveStrategy;
import com.xiaou.sensitive.dto.SensitiveStrategyQuery;
import com.xiaou.sensitive.service.SensitiveStrategyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 敏感词策略管理控制器
 *
 * @author xiaou
 */
@RestController
@RequestMapping("/sensitive/strategy")
@RequiredArgsConstructor
public class SensitiveStrategyController {

    private final SensitiveStrategyService strategyService;

    /**
     * 分页查询策略列表
     */
    @PostMapping("/list")
    @RequireAdmin
    public Result<PageResult<SensitiveStrategy>> list(@RequestBody SensitiveStrategyQuery query) {
        PageResult<SensitiveStrategy> result = strategyService.listStrategy(query);
        return Result.success(result);
    }

    /**
     * 根据ID查询策略详情
     */
    @GetMapping("/{id}")
    @RequireAdmin
    public Result<SensitiveStrategy> getById(@PathVariable Integer id) {
        SensitiveStrategy strategy = strategyService.getStrategyById(id);
        if (strategy == null) {
            return Result.error(ResultCode.DATA_NOT_EXIST.getCode(), "策略不存在");
        }
        return Result.success(strategy);
    }

    /**
     * 根据模块和等级获取策略
     */
    @GetMapping("/get")
    public Result<SensitiveStrategy> getStrategy(@RequestParam String module, @RequestParam Integer level) {
        SensitiveStrategy strategy = strategyService.getStrategy(module, level);
        return Result.success(strategy);
    }

    /**
     * 更新策略
     */
    @PostMapping("/update")
    @RequireAdmin
    public Result<Void> update(@RequestBody SensitiveStrategy strategy) {
        if (strategy.getId() == null) {
            return Result.error(ResultCode.PARAM_VALIDATE_ERROR.getCode(), "ID不能为空");
        }
        boolean success = strategyService.updateStrategy(strategy);
        return success ? Result.success() : Result.error(ResultCode.DATA_NOT_EXIST.getCode(), "策略不存在");
    }

    /**
     * 重置策略为默认值
     */
    @PostMapping("/reset/{id}")
    @RequireAdmin
    public Result<Void> reset(@PathVariable Integer id) {
        boolean success = strategyService.resetStrategy(id);
        return success ? Result.success() : Result.error(ResultCode.DATA_NOT_EXIST.getCode(), "策略不存在");
    }

    /**
     * 刷新策略缓存
     */
    @PostMapping("/refresh")
    @RequireAdmin
    public Result<Void> refreshCache() {
        strategyService.refreshCache();
        return Result.success();
    }
}
