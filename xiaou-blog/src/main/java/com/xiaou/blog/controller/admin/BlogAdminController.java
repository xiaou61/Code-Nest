package com.xiaou.blog.controller.admin;

import com.xiaou.blog.dto.*;
import com.xiaou.blog.service.BlogArticleService;
import com.xiaou.blog.service.BlogCategoryService;
import com.xiaou.blog.service.BlogConfigService;
import com.xiaou.blog.service.BlogTagService;
import com.xiaou.common.annotation.Log;
import com.xiaou.common.annotation.RequireAdmin;
import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.core.domain.Result;
import com.xiaou.common.exception.BusinessException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 博客管理端控制器
 * 
 * @author xiaou
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/admin/blog")
@RequiredArgsConstructor
public class BlogAdminController {
    
    private final BlogConfigService blogConfigService;
    private final BlogArticleService blogArticleService;
    private final BlogCategoryService blogCategoryService;
    private final BlogTagService blogTagService;
    
    // ========== 博客管理 ==========
    
    /**
     * 获取博客统计数据
     */
    @RequireAdmin
    @GetMapping("/statistics")
    public Result<BlogStatisticsResponse> getStatistics() {
        BlogStatisticsResponse response = blogConfigService.getStatistics();
        return Result.success(response);
    }
    
    // ========== 文章管理 ==========
    
    /**
     * 管理端获取文章列表
     */
    @RequireAdmin
    @Log(module = "博客管理", type = Log.OperationType.SELECT, description = "查询文章列表")
    @PostMapping("/article/list")
    public Result<PageResult<ArticleSimpleResponse>> getArticleList(@Valid @RequestBody AdminArticleListRequest request) {
        PageResult<ArticleSimpleResponse> result = blogArticleService.getAdminArticleList(request);
        return Result.success(result);
    }
    
    /**
     * 置顶文章
     */
    @RequireAdmin
    @Log(module = "博客管理", type = Log.OperationType.UPDATE, description = "置顶文章")
    @PostMapping("/article/top")
    public Result<Void> topArticle(
            @RequestParam @Positive(message = "文章ID必须为正数") Long id,
            @RequestParam(required = false) @Min(value = 1, message = "置顶天数必须大于等于1") Integer duration) {
        blogArticleService.topArticle(id, duration);
        return Result.success();
    }
    
    /**
     * 取消置顶
     */
    @RequireAdmin
    @Log(module = "博客管理", type = Log.OperationType.UPDATE, description = "取消置顶")
    @PostMapping("/article/cancel-top")
    public Result<Void> cancelTop(@RequestParam @Positive(message = "文章ID必须为正数") Long id) {
        blogArticleService.cancelTop(id);
        return Result.success();
    }
    
    /**
     * 更新文章状态
     */
    @RequireAdmin
    @Log(module = "博客管理", type = Log.OperationType.UPDATE, description = "更新文章状态")
    @PostMapping("/article/update-status")
    public Result<Void> updateArticleStatus(
            @RequestParam @Positive(message = "文章ID必须为正数") Long id,
            @RequestParam @Min(value = 0, message = "文章状态不合法") @Max(value = 3, message = "文章状态不合法") Integer status) {
        blogArticleService.updateStatus(id, status);
        return Result.success();
    }
    
    /**
     * 删除文章
     */
    @RequireAdmin
    @Log(module = "博客管理", type = Log.OperationType.DELETE, description = "删除文章")
    @DeleteMapping("/article/{id}")
    public Result<Void> deleteArticle(@Positive(message = "文章ID必须为正数") @PathVariable Long id) {
        blogArticleService.deleteArticleByAdmin(id);
        return Result.success();
    }
    
    // ========== 分类管理 ==========
    
    /**
     * 获取分类列表
     */
    @RequireAdmin
    @GetMapping("/category/list")
    public Result<PageResult<?>> getCategoryList(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于等于1") Integer pageNum,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "每页数量必须大于等于1") Integer pageSize) {
        PageResult<?> result = blogCategoryService.getCategoryList(pageNum, pageSize);
        return Result.success(result);
    }
    
    /**
     * 创建分类
     */
    @RequireAdmin
    @Log(module = "博客管理", type = Log.OperationType.INSERT, description = "创建分类")
    @PostMapping("/category/create")
    public Result<Void> createCategory(@Valid @RequestBody CategoryCreateRequest request) {
        blogCategoryService.createCategory(request);
        return Result.success();
    }
    
    /**
     * 更新分类
     */
    @RequireAdmin
    @Log(module = "博客管理", type = Log.OperationType.UPDATE, description = "更新分类")
    @PostMapping("/category/update")
    public Result<Void> updateCategory(
            @RequestParam @Positive(message = "分类ID必须为正数") Long id,
            @Valid @RequestBody CategoryUpdateRequest request) {
        if (request.getId() != null && !id.equals(request.getId())) {
            throw new BusinessException("请求中的分类ID不一致");
        }
        blogCategoryService.updateCategory(id, request);
        return Result.success();
    }
    
    /**
     * 删除分类
     */
    @RequireAdmin
    @Log(module = "博客管理", type = Log.OperationType.DELETE, description = "删除分类")
    @DeleteMapping("/category/{id}")
    public Result<Void> deleteCategory(@Positive(message = "分类ID必须为正数") @PathVariable Long id) {
        blogCategoryService.deleteCategory(id);
        return Result.success();
    }
    
    // ========== 标签管理 ==========
    
    /**
     * 获取标签列表
     */
    @RequireAdmin
    @GetMapping("/tag/list")
    public Result<PageResult<?>> getTagList(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于等于1") Integer pageNum,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "每页数量必须大于等于1") Integer pageSize) {
        PageResult<?> result = blogTagService.getTagList(pageNum, pageSize);
        return Result.success(result);
    }
    
    /**
     * 合并标签
     */
    @RequireAdmin
    @Log(module = "博客管理", type = Log.OperationType.UPDATE, description = "合并标签")
    @PostMapping("/tag/merge")
    public Result<Void> mergeTags(
            @RequestParam @Positive(message = "源标签ID必须为正数") Long sourceTagId,
            @RequestParam @Positive(message = "目标标签ID必须为正数") Long targetTagId) {
        blogTagService.mergeTags(sourceTagId, targetTagId);
        return Result.success();
    }
    
    /**
     * 删除标签
     */
    @RequireAdmin
    @Log(module = "博客管理", type = Log.OperationType.DELETE, description = "删除标签")
    @DeleteMapping("/tag/{id}")
    public Result<Void> deleteTag(@Positive(message = "标签ID必须为正数") @PathVariable Long id) {
        blogTagService.deleteTag(id);
        return Result.success();
    }
}

