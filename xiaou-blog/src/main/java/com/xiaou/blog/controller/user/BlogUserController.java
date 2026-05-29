package com.xiaou.blog.controller.user;

import com.xiaou.blog.dto.*;
import com.xiaou.blog.service.BlogArticleService;
import com.xiaou.blog.service.BlogCategoryService;
import com.xiaou.blog.service.BlogConfigService;
import com.xiaou.blog.service.BlogTagService;
import com.xiaou.common.annotation.Log;
import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.core.domain.Result;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 博客用户端控制器
 * 
 * @author xiaou
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/user/blog")
@RequiredArgsConstructor
public class BlogUserController {
    
    private final BlogConfigService blogConfigService;
    private final BlogArticleService blogArticleService;
    private final BlogCategoryService blogCategoryService;
    private final BlogTagService blogTagService;
    
    // ========== 博客配置相关 ==========
    
    /**
     * 开通博客（扣除50积分）
     */
    @Log(module = "博客", type = Log.OperationType.INSERT, description = "开通博客")
    @PostMapping("/open")
    public Result<BlogOpenResponse> openBlog() {
        BlogOpenResponse response = blogConfigService.openBlog();
        return Result.success(response);
    }
    
    /**
     * 检查博客开通状态
     */
    @GetMapping("/check-status")
    public Result<BlogCheckStatusResponse> checkBlogStatus() {
        BlogCheckStatusResponse response = blogConfigService.checkBlogStatus();
        return Result.success(response);
    }
    
    /**
     * 获取博客配置
     */
    @GetMapping("/config/{userId}")
    public Result<BlogConfigResponse> getBlogConfig(@Positive(message = "用户ID必须为正数") @PathVariable Long userId) {
        BlogConfigResponse response = blogConfigService.getBlogConfigByUserId(userId);
        return Result.success(response);
    }
    
    /**
     * 更新博客配置
     */
    @Log(module = "博客", type = Log.OperationType.UPDATE, description = "更新博客配置")
    @PostMapping("/config/update")
    public Result<Void> updateBlogConfig(@Valid @RequestBody BlogConfigUpdateRequest request) {
        blogConfigService.updateBlogConfig(request);
        return Result.success();
    }
    
    // ========== 文章管理相关 ==========
    
    /**
     * 创建文章（草稿）
     */
    @Log(module = "博客", type = Log.OperationType.INSERT, description = "创建文章草稿")
    @PostMapping("/article/create")
    public Result<Long> createArticle(@Valid @RequestBody ArticlePublishRequest request) {
        Long articleId = blogArticleService.createArticle(request);
        return Result.success(articleId);
    }
    
    /**
     * 发布文章（扣除20积分）
     */
    @Log(module = "博客", type = Log.OperationType.INSERT, description = "发布文章")
    @PostMapping("/article/publish")
    public Result<ArticlePublishResponse> publishArticle(@Valid @RequestBody ArticlePublishRequest request) {
        ArticlePublishResponse response = blogArticleService.publishArticle(request);
        return Result.success(response);
    }
    
    /**
     * 更新文章
     */
    @Log(module = "博客", type = Log.OperationType.UPDATE, description = "更新文章")
    @PostMapping("/article/update/{id}")
    public Result<Void> updateArticle(
            @Positive(message = "文章ID必须为正数") @PathVariable Long id,
            @Valid @RequestBody ArticlePublishRequest request) {
        blogArticleService.updateArticle(id, request);
        return Result.success();
    }
    
    /**
     * 删除文章
     */
    @Log(module = "博客", type = Log.OperationType.DELETE, description = "删除文章")
    @DeleteMapping("/article/{id}")
    public Result<Void> deleteArticle(@Positive(message = "文章ID必须为正数") @PathVariable Long id) {
        blogArticleService.deleteArticle(id);
        return Result.success();
    }
    
    /**
     * 获取文章详情
     */
    @GetMapping("/article/{id}")
    public Result<ArticleDetailResponse> getArticleDetail(@Positive(message = "文章ID必须为正数") @PathVariable Long id) {
        ArticleDetailResponse response = blogArticleService.getArticleDetail(id);
        return Result.success(response);
    }
    
    /**
     * 获取用户的文章列表
     */
    @PostMapping("/article/list")
    public Result<PageResult<ArticleSimpleResponse>> getUserArticleList(@Valid @RequestBody ArticleListRequest request) {
        PageResult<ArticleSimpleResponse> result = blogArticleService.getUserArticleList(request);
        return Result.success(result);
    }
    
    /**
     * 获取我的文章列表
     */
    @PostMapping("/article/my-list")
    public Result<PageResult<ArticleSimpleResponse>> getMyArticleList(@Valid @RequestBody ArticleListRequest request) {
        PageResult<ArticleSimpleResponse> result = blogArticleService.getMyArticleList(request);
        return Result.success(result);
    }
    
    /**
     * 获取我的草稿列表
     */
    @PostMapping("/article/draft-list")
    public Result<PageResult<ArticleSimpleResponse>> getMyDraftList(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于等于1") Integer pageNum,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页数量必须大于等于1") Integer pageSize) {
        PageResult<ArticleSimpleResponse> result = blogArticleService.getMyDraftList(pageNum, pageSize);
        return Result.success(result);
    }
    
    /**
     * 按分类获取文章
     */
    @PostMapping("/article/by-category")
    public Result<PageResult<ArticleSimpleResponse>> getArticlesByCategory(
            @RequestParam @Positive(message = "分类ID必须为正数") Long categoryId,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于等于1") Integer pageNum,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页数量必须大于等于1") Integer pageSize) {
        ArticleListRequest request = new ArticleListRequest();
        request.setCategoryId(categoryId);
        request.setPageNum(pageNum);
        request.setPageSize(pageSize);
        PageResult<ArticleSimpleResponse> result = blogArticleService.getUserArticleList(request);
        return Result.success(result);
    }
    
    // ========== 分类标签相关 ==========
    
    /**
     * 获取所有分类
     */
    @GetMapping("/categories")
    public Result<?> getAllCategories() {
        return Result.success(blogCategoryService.getAllCategories());
    }
    
    /**
     * 获取所有标签
     */
    @GetMapping("/tags")
    public Result<?> getAllTags() {
        return Result.success(blogTagService.getAllTags());
    }
    
    /**
     * 获取热门标签
     */
    @GetMapping("/tags/hot")
    public Result<?> getHotTags(@RequestParam(defaultValue = "10") @Min(value = 1, message = "数量必须大于等于1") Integer limit) {
        return Result.success(blogTagService.getHotTags(limit));
    }
}

