package com.xiaou.codepen.controller.admin;

import com.xiaou.codepen.domain.CodePen;
import com.xiaou.codepen.domain.CodePenComment;
import com.xiaou.codepen.domain.CodePenTag;
import com.xiaou.codepen.dto.*;
import com.xiaou.codepen.mapper.CodePenMapper;
import com.xiaou.codepen.service.CodePenCommentService;
import com.xiaou.codepen.service.CodePenService;
import com.xiaou.codepen.service.CodePenTagService;
import com.xiaou.common.annotation.Log;
import com.xiaou.common.annotation.RequireAdmin;
import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.core.domain.Result;
import com.xiaou.common.exception.BusinessException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 代码共享器管理端控制器
 *
 * @author xiaou
 */
@Validated
@RestController
@RequestMapping("/admin/code-pen")
@RequiredArgsConstructor
public class CodePenAdminController {

    private static final long SYSTEM_USER_ID = 1L;

    private final CodePenService codePenService;
    private final CodePenMapper codePenMapper;
    private final CodePenTagService tagService;
    private final CodePenCommentService commentService;

    // ========== 作品管理相关 ==========

    /**
     * 获取作品列表（管理端）
     */
    @RequireAdmin
    @PostMapping("/list")
    public Result<PageResult<CodePenDetailResponse>> getList(@Valid @RequestBody CodePenListRequest request) {
        return Result.success(codePenService.getList(request, null));
    }

    /**
     * 获取作品详情
     */
    @RequireAdmin
    @GetMapping("/{id}")
    public Result<CodePenDetailResponse> getDetail(@Positive(message = "作品ID必须为正数") @PathVariable Long id) {
        CodePenDetailResponse response = codePenService.getDetail(id, null);
        response.setCanViewCode(true);
        return Result.success(response);
    }

    /**
     * 更新作品状态
     */
    @RequireAdmin
    @Log(module = "代码共享器", type = Log.OperationType.UPDATE, description = "更新作品状态")
    @PostMapping("/update-status")
    public Result<Boolean> updateStatus(@Valid @RequestBody UpdateStatusRequest request) {
        int result = codePenMapper.updateStatus(request.getId(), request.getStatus());
        return Result.success(result > 0);
    }

    /**
     * 设置推荐
     */
    @RequireAdmin
    @Log(module = "代码共享器", type = Log.OperationType.UPDATE, description = "设置推荐")
    @PostMapping("/recommend")
    public Result<Boolean> setRecommend(@Valid @RequestBody RecommendRequest request) {
        int result = codePenMapper.setRecommend(request.getId(), request.getExpireTime());
        return Result.success(result > 0);
    }

    /**
     * 取消推荐
     */
    @RequireAdmin
    @Log(module = "代码共享器", type = Log.OperationType.UPDATE, description = "取消推荐")
    @PostMapping("/cancel-recommend")
    public Result<Boolean> cancelRecommend(@Valid @RequestBody CheckForkPriceRequest request) {
        int result = codePenMapper.cancelRecommend(request.getPenId());
        return Result.success(result > 0);
    }

    /**
     * 删除作品（管理端）
     */
    @RequireAdmin
    @Log(module = "代码共享器", type = Log.OperationType.DELETE, description = "删除作品")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@Positive(message = "作品ID必须为正数") @PathVariable Long id) {
        int result = codePenMapper.deleteById(id);
        return Result.success(result > 0);
    }

    // ========== 模板管理相关 ==========

    /**
     * 创建系统模板
     */
    @RequireAdmin
    @Log(module = "代码共享器", type = Log.OperationType.INSERT, description = "创建系统模板")
    @PostMapping("/template/create")
    public Result<CodePenCreateResponse> createTemplate(@Valid @RequestBody CodePenCreateRequest request) {
        CodePen template = new CodePen();
        template.setUserId(SYSTEM_USER_ID);
        template.setTitle(request.getTitle());
        template.setDescription(request.getDescription());
        template.setHtmlCode(request.getHtmlCode());
        template.setCssCode(request.getCssCode());
        template.setJsCode(request.getJsCode());
        template.setIsTemplate(1);
        template.setStatus(1);
        template.setIsPublic(1);
        template.setIsFree(1);
        template.setForkPrice(0);

        codePenMapper.insert(template);

        CodePenCreateResponse response = CodePenCreateResponse.builder()
                .penId(template.getId())
                .createTime(template.getCreateTime())
                .build();

        return Result.success(response);
    }

    /**
     * 更新模板
     */
    @RequireAdmin
    @Log(module = "代码共享器", type = Log.OperationType.UPDATE, description = "更新模板")
    @PostMapping("/template/update")
    public Result<Boolean> updateTemplate(@Valid @RequestBody CodePenCreateRequest request) {
        if (request.getId() == null) {
            throw new BusinessException("模板ID不能为空");
        }

        CodePen template = codePenMapper.selectById(request.getId());
        if (template == null || template.getIsTemplate() == null || template.getIsTemplate() != 1) {
            throw new BusinessException("模板不存在");
        }

        CodePen updateTemplate = new CodePen();
        updateTemplate.setId(request.getId());
        updateTemplate.setTitle(request.getTitle());
        updateTemplate.setDescription(request.getDescription());
        updateTemplate.setHtmlCode(request.getHtmlCode());
        updateTemplate.setCssCode(request.getCssCode());
        updateTemplate.setJsCode(request.getJsCode());

        int result = codePenMapper.updateById(updateTemplate);
        return Result.success(result > 0);
    }

    /**
     * 模板列表
     */
    @RequireAdmin
    @PostMapping("/template/list")
    public Result<List<CodePen>> getTemplateList() {
        return Result.success(codePenService.getTemplateList());
    }

    /**
     * 删除模板
     */
    @RequireAdmin
    @Log(module = "代码共享器", type = Log.OperationType.DELETE, description = "删除模板")
    @DeleteMapping("/template/{id}")
    public Result<Boolean> deleteTemplate(@Positive(message = "模板ID必须为正数") @PathVariable Long id) {
        int result = codePenMapper.deleteById(id);
        return Result.success(result > 0);
    }

    // ========== 标签管理相关 ==========

    /**
     * 创建标签
     */
    @RequireAdmin
    @Log(module = "代码共享器", type = Log.OperationType.INSERT, description = "创建标签")
    @PostMapping("/tag/create")
    public Result<Long> createTag(@RequestBody CodePenTag tag) {
        Long tagId = tagService.createTag(tag.getTagName(), tag.getTagDescription());
        return Result.success(tagId);
    }

    /**
     * 更新标签
     */
    @RequireAdmin
    @Log(module = "代码共享器", type = Log.OperationType.UPDATE, description = "更新标签")
    @PostMapping("/tag/update")
    public Result<Boolean> updateTag(@RequestBody CodePenTag tag) {
        boolean result = tagService.updateTag(tag.getId(), tag.getTagName(), tag.getTagDescription());
        return Result.success(result);
    }

    /**
     * 删除标签
     */
    @RequireAdmin
    @Log(module = "代码共享器", type = Log.OperationType.DELETE, description = "删除标签")
    @DeleteMapping("/tag/{id}")
    public Result<Boolean> deleteTag(@Positive(message = "标签ID必须为正数") @PathVariable Long id) {
        boolean result = tagService.deleteTag(id);
        return Result.success(result);
    }

    /**
     * 合并标签
     */
    @RequireAdmin
    @Log(module = "代码共享器", type = Log.OperationType.UPDATE, description = "合并标签")
    @PostMapping("/tag/merge")
    public Result<Boolean> mergeTags(@Valid @RequestBody MergeTagRequest request) {
        boolean result = tagService.mergeTags(request.getSourceId(), request.getTargetId());
        return Result.success(result);
    }

    /**
     * 标签列表
     */
    @RequireAdmin
    @PostMapping("/tag/list")
    public Result<List<CodePenTag>> getTagList() {
        return Result.success(tagService.getAllTags());
    }

    // ========== 评论管理相关 ==========

    /**
     * 评论列表
     */
    @RequireAdmin
    @PostMapping("/comment/list")
    public Result<List<CodePenComment>> getCommentList(@Valid @RequestBody CheckForkPriceRequest request) {
        return Result.success(commentService.getCommentList(request.getPenId()));
    }

    /**
     * 隐藏评论
     */
    @RequireAdmin
    @Log(module = "代码共享器", type = Log.OperationType.UPDATE, description = "隐藏评论")
    @PostMapping("/comment/hide")
    public Result<Boolean> hideComment(@Valid @RequestBody IdRequest request) {
        boolean result = commentService.hideComment(request.getId());
        return Result.success(result);
    }

    /**
     * 删除评论
     */
    @RequireAdmin
    @Log(module = "代码共享器", type = Log.OperationType.DELETE, description = "删除评论")
    @DeleteMapping("/comment/{id}")
    public Result<Boolean> deleteComment(@Positive(message = "评论ID必须为正数") @PathVariable Long id) {
        boolean result = commentService.adminDeleteComment(id);
        return Result.success(result);
    }

    // ========== 基础数据相关 ==========

    /**
     * 获取统计数据
     */
    @RequireAdmin
    @GetMapping("/statistics")
    public Result<CodePenStatisticsResponse> getStatistics() {
        return Result.success(codePenService.getStatistics());
    }
}
