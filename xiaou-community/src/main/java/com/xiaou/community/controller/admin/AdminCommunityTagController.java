package com.xiaou.community.controller.admin;

import com.xiaou.common.annotation.Log;
import com.xiaou.common.annotation.RequireAdmin;
import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.core.domain.Result;
import com.xiaou.community.domain.CommunityTag;
import com.xiaou.community.dto.CommunityTagCreateRequest;
import com.xiaou.community.dto.CommunityTagQueryRequest;
import com.xiaou.community.dto.CommunityTagUpdateRequest;
import com.xiaou.community.service.CommunityTagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 后台标签管理控制器
 * 
 * @author xiaou
 */
@Slf4j
@RestController
@RequestMapping("/admin/community/tags")
@RequiredArgsConstructor
public class AdminCommunityTagController {
    
    private final CommunityTagService communityTagService;
    
    /**
     * 创建标签
     */
    @RequireAdmin
    @Log(module = "社区管理", type = Log.OperationType.INSERT, description = "创建标签")
    @PostMapping
    public Result<Void> createTag(@Validated @RequestBody CommunityTagCreateRequest request) {
        communityTagService.createTag(request);
        return Result.success();
    }
    
    /**
     * 更新标签
     */
    @RequireAdmin
    @Log(module = "社区管理", type = Log.OperationType.UPDATE, description = "更新标签")
    @PutMapping("/{id}")
    public Result<Void> updateTag(@PathVariable Long id, @Validated @RequestBody CommunityTagUpdateRequest request) {
        request.setId(id);
        communityTagService.updateTag(request);
        return Result.success();
    }
    
    /**
     * 删除标签
     */
    @RequireAdmin
    @Log(module = "社区管理", type = Log.OperationType.DELETE, description = "删除标签")
    @DeleteMapping("/{id}")
    public Result<Void> deleteTag(@PathVariable Long id) {
        communityTagService.deleteTag(id);
        return Result.success();
    }

    /**
     * 启用/禁用标签
     */
    @RequireAdmin
    @Log(module = "社区管理", type = Log.OperationType.UPDATE, description = "切换标签状态")
    @PatchMapping("/{id}/status")
    public Result<Void> toggleTagStatus(@PathVariable Long id) {
        communityTagService.toggleTagStatus(id);
        return Result.success();
    }
    
    /**
     * 获取标签详情
     */
    @RequireAdmin
    @GetMapping("/{id}")
    public Result<CommunityTag> getTag(@PathVariable Long id) {
        CommunityTag tag = communityTagService.getById(id);
        return Result.success(tag);
    }
    
    /**
     * 分页查询标签列表
     */
    @RequireAdmin
    @Log(module = "社区管理", type = Log.OperationType.SELECT, description = "查询标签列表")
    @PostMapping("/list")
    public Result<PageResult<CommunityTag>> getTagList(@RequestBody CommunityTagQueryRequest request) {
        PageResult<CommunityTag> result = communityTagService.getAdminList(request);
        return Result.success(result);
    }
}

