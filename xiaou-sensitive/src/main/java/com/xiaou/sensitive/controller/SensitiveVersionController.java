package com.xiaou.sensitive.controller;

import com.xiaou.common.annotation.RequireAdmin;
import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.core.domain.Result;
import com.xiaou.common.core.domain.ResultCode;
import com.xiaou.sensitive.domain.SensitiveVersion;
import com.xiaou.sensitive.service.SensitiveVersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 敏感词版本管理控制器
 *
 * @author xiaou
 */
@RestController
@RequestMapping("/sensitive/version")
@RequiredArgsConstructor
public class SensitiveVersionController {

    private final SensitiveVersionService versionService;

    /**
     * 查询版本历史列表
     */
    @PostMapping("/list")
    @RequireAdmin
    public Result<PageResult<SensitiveVersion>> list(@RequestBody PageQuery query) {
        PageResult<SensitiveVersion> result = versionService.listVersions(
                query.getPageNum(), 
                query.getPageSize()
        );
        return Result.success(result);
    }

    /**
     * 获取版本详情
     */
    @GetMapping("/{id}")
    @RequireAdmin
    public Result<SensitiveVersion> getById(@PathVariable Long id) {
        SensitiveVersion version = versionService.getVersionById(id);
        if (version == null) {
            return Result.error(ResultCode.DATA_NOT_EXIST.getCode(), "版本不存在");
        }
        return Result.success(version);
    }

    /**
     * 回滚到指定版本
     */
    @PostMapping("/rollback/{id}")
    @RequireAdmin
    public Result<Void> rollback(@PathVariable Long id, @RequestBody RollbackRequest request) {
        boolean success = versionService.rollbackToVersion(id, request.getOperatorId());
        return success ? Result.success() : Result.error(ResultCode.DATA_NOT_EXIST.getCode(), "版本不存在或无法回滚");
    }

    /**
     * 获取最新版本号
     */
    @GetMapping("/latest")
    public Result<String> getLatest() {
        String versionNo = versionService.getLatestVersionNo();
        return Result.success(versionNo);
    }

    /**
     * 分页查询对象
     */
    public static class PageQuery {
        private Integer pageNum = 1;
        private Integer pageSize = 10;

        public Integer getPageNum() { return pageNum; }
        public void setPageNum(Integer pageNum) { this.pageNum = pageNum; }
        public Integer getPageSize() { return pageSize; }
        public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
    }

    /**
     * 回滚请求对象
     */
    public static class RollbackRequest {
        private Long operatorId;

        public Long getOperatorId() { return operatorId; }
        public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }
    }
}
