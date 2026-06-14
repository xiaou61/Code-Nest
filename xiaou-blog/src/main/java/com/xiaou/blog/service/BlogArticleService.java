package com.xiaou.blog.service;

import com.xiaou.blog.domain.BlogArticle;
import com.xiaou.blog.dto.*;
import com.xiaou.common.core.domain.PageResult;

import java.util.List;

/**
 * 博客文章Service接口
 * 
 * @author xiaou
 */
public interface BlogArticleService {
    
    /**
     * 创建文章（草稿）
     */
    Long createArticle(ArticlePublishRequest request);
    
    /**
     * 发布文章（扣除20积分）
     */
    ArticlePublishResponse publishArticle(ArticlePublishRequest request);
    
    /**
     * 更新文章
     */
    void updateArticle(Long id, ArticlePublishRequest request);
    
    /**
     * 删除文章
     */
    void deleteArticle(Long id);

    /**
     * 管理端删除文章
     */
    void deleteArticleByAdmin(Long id);
    
    /**
     * 获取文章详情
     */
    ArticleDetailResponse getArticleDetail(Long id);
    
    /**
     * 获取用户的文章列表（分页）
     */
    PageResult<ArticleSimpleResponse> getUserArticleList(ArticleListRequest request);
    
    /**
     * 获取我的文章列表
     */
    PageResult<ArticleSimpleResponse> getMyArticleList(ArticleListRequest request);
    
    /**
     * 获取我的草稿列表
     */
    PageResult<ArticleSimpleResponse> getMyDraftList(Integer pageNum, Integer pageSize);
    
    /**
     * 管理端获取文章列表
     */
    PageResult<ArticleSimpleResponse> getAdminArticleList(AdminArticleListRequest request);
    
    /**
     * 置顶文章
     */
    void topArticle(Long id, Integer duration);
    
    /**
     * 取消置顶
     */
    void cancelTop(Long id);
    
    /**
     * 更新文章状态
     */
    void updateStatus(Long id, Integer status);
    
    /**
     * 按分类获取文章
     */
    PageResult<ArticleSimpleResponse> getArticlesByCategory(Long categoryId, Integer pageNum, Integer pageSize);
    
    /**
     * 获取相关文章
     */
    List<ArticleSimpleResponse> getRelatedArticles(Long articleId, Integer limit);
}

