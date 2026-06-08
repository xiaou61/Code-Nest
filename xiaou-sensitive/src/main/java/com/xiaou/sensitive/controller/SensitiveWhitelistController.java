package com.xiaou.sensitive.controller;

import com.xiaou.common.annotation.RequireAdmin;
import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.core.domain.Result;
import com.xiaou.common.core.domain.ResultCode;
import com.xiaou.sensitive.domain.SensitiveWhitelist;
import com.xiaou.sensitive.dto.SensitiveWhitelistQuery;
import com.xiaou.sensitive.service.SensitiveWhitelistService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 敏感词白名单管理控制器
 *
 * @author xiaou
 */
@RestController
@RequestMapping("/sensitive/whitelist")
@RequiredArgsConstructor
public class SensitiveWhitelistController {

    private final SensitiveWhitelistService whitelistService;

    /**
     * 分页查询白名单列表
     */
    @PostMapping("/list")
    @RequireAdmin
    public Result<PageResult<SensitiveWhitelist>> list(@RequestBody SensitiveWhitelistQuery query) {
        PageResult<SensitiveWhitelist> result = whitelistService.listWhitelist(query);
        return Result.success(result);
    }

    /**
     * 根据ID查询白名单详情
     */
    @GetMapping("/{id}")
    @RequireAdmin
    public Result<SensitiveWhitelist> getById(@PathVariable Long id) {
        SensitiveWhitelist whitelist = whitelistService.getWhitelistById(id);
        if (whitelist == null) {
            return Result.error(ResultCode.DATA_NOT_EXIST.getCode(), "白名单不存在");
        }
        return Result.success(whitelist);
    }

    /**
     * 新增白名单
     */
    @PostMapping("/add")
    @RequireAdmin
    public Result<Void> add(@RequestBody SensitiveWhitelist whitelist) {
        boolean success = whitelistService.addWhitelist(whitelist);
        return success ? Result.success() : Result.error(ResultCode.BUSINESS_ERROR.getCode(), "新增白名单失败");
    }

    /**
     * 更新白名单
     */
    @PostMapping("/update")
    @RequireAdmin
    public Result<Void> update(@RequestBody SensitiveWhitelist whitelist) {
        if (whitelist.getId() == null) {
            return Result.error(ResultCode.PARAM_VALIDATE_ERROR.getCode(), "ID不能为空");
        }
        boolean success = whitelistService.updateWhitelist(whitelist);
        return success ? Result.success() : Result.error(ResultCode.DATA_NOT_EXIST.getCode(), "白名单不存在");
    }

    /**
     * 删除白名单
     */
    @PostMapping("/delete/{id}")
    @RequireAdmin
    public Result<Void> delete(@PathVariable Long id) {
        boolean success = whitelistService.deleteWhitelist(id);
        return success ? Result.success() : Result.error(ResultCode.DATA_NOT_EXIST.getCode(), "白名单不存在");
    }

    /**
     * 批量删除白名单
     */
    @PostMapping("/delete/batch")
    @RequireAdmin
    public Result<Void> deleteBatch(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.error(ResultCode.PARAM_VALIDATE_ERROR.getCode(), "ID列表不能为空");
        }
        boolean success = whitelistService.deleteWhitelistBatch(ids);
        return success ? Result.success() : Result.error(ResultCode.DATA_NOT_EXIST.getCode(), "白名单不存在");
    }

    /**
     * 批量导入白名单
     */
    @PostMapping("/import")
    @RequireAdmin
    public Result<SensitiveWhitelistService.ImportResult> importWhitelist(@RequestBody ImportRequest request) {
        if (request.getWords() == null || request.getWords().isEmpty()) {
            return Result.error(ResultCode.PARAM_VALIDATE_ERROR.getCode(), "导入词汇列表不能为空");
        }
        
        SensitiveWhitelistService.ImportResult result = whitelistService.importWhitelist(
                request.getWords(), 
                request.getCreatorId()
        );
        return Result.success(result);
    }

    /**
     * 刷新白名单缓存
     */
    @PostMapping("/refresh")
    @RequireAdmin
    public Result<Void> refreshCache() {
        whitelistService.refreshCache();
        return Result.success();
    }

    /**
     * 导入请求对象
     */
    public static class ImportRequest {
        private List<String> words;
        private Long creatorId;

        public List<String> getWords() {
            return words;
        }

        public void setWords(List<String> words) {
            this.words = words;
        }

        public Long getCreatorId() {
            return creatorId;
        }

        public void setCreatorId(Long creatorId) {
            this.creatorId = creatorId;
        }
    }
}
