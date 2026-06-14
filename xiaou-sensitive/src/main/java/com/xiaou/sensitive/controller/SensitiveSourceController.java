package com.xiaou.sensitive.controller;

import com.xiaou.common.annotation.RequireAdmin;
import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.core.domain.Result;
import com.xiaou.common.core.domain.ResultCode;
import com.xiaou.sensitive.domain.SensitiveSource;
import com.xiaou.sensitive.dto.SensitiveSourceQuery;
import com.xiaou.sensitive.service.SensitiveSourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 敏感词来源管理控制器
 *
 * @author xiaou
 */
@RestController
@RequestMapping("/sensitive/source")
@RequiredArgsConstructor
public class SensitiveSourceController {

    private final SensitiveSourceService sourceService;

    /**
     * 查询词库来源列表
     */
    @PostMapping("/list")
    @RequireAdmin
    public Result<PageResult<SensitiveSource>> list(@RequestBody SensitiveSourceQuery query) {
        PageResult<SensitiveSource> result = sourceService.listSources(query);
        return Result.success(result);
    }

    /**
     * 获取词库来源详情
     */
    @GetMapping("/{id}")
    @RequireAdmin
    public Result<SensitiveSource> getById(@PathVariable Integer id) {
        SensitiveSource source = sourceService.getSourceById(id);
        if (source == null) {
            return Result.error(ResultCode.DATA_NOT_EXIST.getCode(), "词库来源不存在");
        }
        return Result.success(source);
    }

    /**
     * 新增词库来源
     */
    @PostMapping("/add")
    @RequireAdmin
    public Result<Void> add(@RequestBody SensitiveSource source) {
        boolean success = sourceService.addSource(source);
        return success ? Result.success() : Result.error(ResultCode.BUSINESS_ERROR.getCode(), "新增词库来源失败");
    }

    /**
     * 更新词库来源
     */
    @PostMapping("/update")
    @RequireAdmin
    public Result<Void> update(@RequestBody SensitiveSource source) {
        if (source.getId() == null) {
            return Result.error(ResultCode.PARAM_VALIDATE_ERROR.getCode(), "ID不能为空");
        }
        boolean success = sourceService.updateSource(source);
        return success ? Result.success() : Result.error(ResultCode.DATA_NOT_EXIST.getCode(), "词库来源不存在");
    }

    /**
     * 删除词库来源
     */
    @PostMapping("/delete/{id}")
    @RequireAdmin
    public Result<Void> delete(@PathVariable Integer id) {
        boolean success = sourceService.deleteSource(id);
        return success ? Result.success() : Result.error(ResultCode.DATA_NOT_EXIST.getCode(), "词库来源不存在");
    }

    /**
     * 测试连接
     */
    @PostMapping("/test-connection/{id}")
    @RequireAdmin
    public Result<String> testConnection(@PathVariable Integer id) {
        String message = sourceService.testConnection(id);
        return Result.success(message);
    }

    /**
     * 手动同步词库
     */
    @PostMapping("/sync/{id}")
    @RequireAdmin
    public Result<SensitiveSourceService.SyncResult> sync(@PathVariable Integer id) {
        SensitiveSourceService.SyncResult result = sourceService.syncSource(id);
        return Result.success(result);
    }
}
