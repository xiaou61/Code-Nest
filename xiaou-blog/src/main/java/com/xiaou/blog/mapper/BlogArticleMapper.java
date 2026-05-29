package com.xiaou.blog.mapper;

import com.xiaou.blog.domain.BlogArticle;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 博客文章Mapper
 * 
 * @author xiaou
 */
@Mapper
public interface BlogArticleMapper {
    
    /**
     * 插入文章
     */
    int insert(BlogArticle article);
    
    /**
     * 根据ID查询文章
     */
    BlogArticle selectById(Long id);
    
    /**
     * 更新文章
     */
    int updateById(BlogArticle article);
    
    /**
     * 根据用户ID和状态查询文章列表
     */
    List<BlogArticle> selectByUserIdAndStatus(@Param("userId") Long userId, @Param("status") Integer status);
    
    /**
     * 查询用户的文章列表（分页）
     */
    List<BlogArticle> selectUserArticleList(@Param("userId") Long userId, 
                                            @Param("categoryId") Long categoryId,
                                            @Param("tags") String tags,
                                            @Param("status") Integer status);
    
    /**
     * 查询文章总数
     */
    Long selectTotalCount();
    
    /**
     * 查询文章总数（按条件）
     */
    Long selectCountByCondition(@Param("userId") Long userId,
                                 @Param("categoryId") Long categoryId,
                                 @Param("status") Integer status,
                                 @Param("keyword") String keyword,
                                 @Param("startTime") String startTime,
                                 @Param("endTime") String endTime);
    
    /**
     * 管理端查询文章列表
     */
    List<BlogArticle> selectAdminArticleList(@Param("userId") Long userId,
                                             @Param("categoryId") Long categoryId,
                                             @Param("status") Integer status,
                                             @Param("keyword") String keyword,
                                             @Param("startTime") String startTime,
                                             @Param("endTime") String endTime);
    
    /**
     * 置顶文章
     */
    int topArticle(@Param("id") Long id, @Param("topExpireTime") String topExpireTime);
    
    /**
     * 取消置顶
     */
    int cancelTop(Long id);
    
    /**
     * 更新文章状态
     */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
    
    /**
     * 根据分类ID查询文章
     */
    List<BlogArticle> selectByCategoryId(@Param("categoryId") Long categoryId, @Param("limit") Integer limit);
    
    /**
     * 根据标签查询相关文章
     */
    List<BlogArticle> selectRelatedByTags(@Param("tags") String tags, @Param("excludeId") Long excludeId, @Param("limit") Integer limit);

    /**
     * 根据标签名查询文章
     */
    List<BlogArticle> selectByTagName(@Param("tagName") String tagName);
}

