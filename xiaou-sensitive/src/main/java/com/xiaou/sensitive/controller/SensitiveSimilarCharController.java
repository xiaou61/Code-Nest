package com.xiaou.sensitive.controller;

import com.xiaou.common.annotation.RequireAdmin;
import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.core.domain.Result;
import com.xiaou.common.core.domain.ResultCode;
import com.xiaou.sensitive.domain.SensitiveSimilarChar;
import com.xiaou.sensitive.dto.SensitiveSimilarCharQuery;
import com.xiaou.sensitive.service.SensitiveSimilarCharService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 形似字映射管理控制器
 *
 * @author xiaou
 */
@RestController
@RequestMapping("/sensitive/similar-char")
@RequiredArgsConstructor
public class SensitiveSimilarCharController {

    private final SensitiveSimilarCharService similarCharService;

    /**
     * 查询形似字列表
     */
    @PostMapping("/list")
    @RequireAdmin
    public Result<PageResult<SensitiveSimilarChar>> list(@RequestBody SensitiveSimilarCharQuery query) {
        PageResult<SensitiveSimilarChar> result = similarCharService.listSimilarChars(query);
        return Result.success(result);
    }

    /**
     * 获取形似字详情
     */
    @GetMapping("/{id}")
    @RequireAdmin
    public Result<SensitiveSimilarChar> getById(@PathVariable Integer id) {
        SensitiveSimilarChar similarChar = similarCharService.getSimilarCharById(id);
        if (similarChar == null) {
            return Result.error(ResultCode.DATA_NOT_EXIST.getCode(), "形似字不存在");
        }
        return Result.success(similarChar);
    }

    /**
     * 新增形似字
     */
    @PostMapping("/add")
    @RequireAdmin
    public Result<Void> add(@RequestBody SensitiveSimilarChar similarChar) {
        boolean success = similarCharService.addSimilarChar(similarChar);
        return success ? Result.success() : Result.error(ResultCode.BUSINESS_ERROR.getCode(), "新增形似字失败");
    }

    /**
     * 批量新增形似字
     */
    @PostMapping("/batch-add")
    @RequireAdmin
    public Result<Void> batchAdd(@RequestBody List<SensitiveSimilarChar> similarChars) {
        if (similarChars == null || similarChars.isEmpty()) {
            return Result.error(ResultCode.PARAM_VALIDATE_ERROR.getCode(), "形似字列表不能为空");
        }
        boolean success = similarCharService.batchAddSimilarChars(similarChars);
        return success ? Result.success() : Result.error(ResultCode.BUSINESS_ERROR.getCode(), "批量新增形似字失败");
    }

    /**
     * 更新形似字
     */
    @PostMapping("/update")
    @RequireAdmin
    public Result<Void> update(@RequestBody SensitiveSimilarChar similarChar) {
        if (similarChar.getId() == null) {
            return Result.error(ResultCode.PARAM_VALIDATE_ERROR.getCode(), "ID不能为空");
        }
        boolean success = similarCharService.updateSimilarChar(similarChar);
        return success ? Result.success() : Result.error(ResultCode.DATA_NOT_EXIST.getCode(), "形似字不存在");
    }

    /**
     * 删除形似字
     */
    @PostMapping("/delete/{id}")
    @RequireAdmin
    public Result<Void> delete(@PathVariable Integer id) {
        boolean success = similarCharService.deleteSimilarChar(id);
        return success ? Result.success() : Result.error(ResultCode.DATA_NOT_EXIST.getCode(), "形似字不存在");
    }

    /**
     * 刷新形似字缓存
     */
    @PostMapping("/refresh")
    @RequireAdmin
    public Result<Void> refreshCache() {
        similarCharService.refreshCache();
        return Result.success();
    }
}
