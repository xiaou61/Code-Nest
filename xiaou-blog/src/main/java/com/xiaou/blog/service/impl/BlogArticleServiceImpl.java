package com.xiaou.blog.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.xiaou.common.utils.PageHelper;
import com.xiaou.blog.domain.BlogArticle;
import com.xiaou.blog.domain.BlogCategory;
import com.xiaou.blog.domain.BlogConfig;
import com.xiaou.blog.dto.*;
import com.xiaou.blog.mapper.BlogArticleMapper;
import com.xiaou.blog.mapper.BlogCategoryMapper;
import com.xiaou.blog.mapper.BlogTagMapper;
import com.xiaou.blog.service.BlogArticleService;
import com.xiaou.blog.service.BlogConfigService;
import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.exception.BusinessException;
import com.xiaou.common.satoken.StpUserUtil;
import com.xiaou.points.mapper.UserPointsBalanceMapper;
import com.xiaou.points.mapper.UserPointsDetailMapper;
import com.xiaou.points.domain.UserPointsDetail;
import com.xiaou.points.enums.PointsType;
import com.xiaou.sensitive.api.SensitiveCheckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 博客文章Service实现类
 * 
 * @author xiaou
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BlogArticleServiceImpl implements BlogArticleService {
    
    private final BlogArticleMapper blogArticleMapper;
    private final BlogCategoryMapper blogCategoryMapper;
    private final BlogTagMapper blogTagMapper;
    private final BlogConfigService blogConfigService;
    private final UserPointsBalanceMapper pointsBalanceMapper;
    private final UserPointsDetailMapper pointsDetailMapper;
    private final SensitiveCheckService sensitiveCheckService;
    
    private static final int PUBLISH_ARTICLE_POINTS = 20;
    private static final int MIN_CONTENT_LENGTH = 50;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createArticle(ArticlePublishRequest request) {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        
        // 检查是否已开通博客
        BlogConfig blogConfig = blogConfigService.getBlogByUserId(userId);
        if (blogConfig == null) {
            throw new BusinessException("请先开通博客");
        }
        
        // 创建文章（草稿状态）
        BlogArticle article = buildArticle(request, userId);
        article.setStatus(0); // 草稿
        
        blogArticleMapper.insert(article);
        
        log.info("用户{}创建文章草稿：{}", userId, article.getId());
        
        return article.getId();
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArticlePublishResponse publishArticle(ArticlePublishRequest request) {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        
        // 检查是否已开通博客
        BlogConfig blogConfig = blogConfigService.getBlogByUserId(userId);
        if (blogConfig == null) {
            throw new BusinessException("请先开通博客");
        }
        
        // 验证文章内容
        validateArticle(request);
        
        // 敏感词检测
        if (sensitiveCheckService.containsSensitiveWords(request.getTitle(), "blog")) {
            throw new BusinessException("文章标题包含敏感词");
        }
        if (sensitiveCheckService.containsSensitiveWords(request.getContent(), "blog")) {
            throw new BusinessException("文章内容包含敏感词");
        }
        
        BlogArticle article;
        boolean isNewArticle = false;
        
        if (request.getId() != null) {
            // 编辑已有文章
            article = blogArticleMapper.selectById(request.getId());
            if (article == null) {
                throw new BusinessException("文章不存在");
            }
            if (!article.getUserId().equals(userId)) {
                throw new BusinessException("无权操作");
            }
            
            // 如果是草稿首次发布，需要扣除积分
            if (article.getStatus() == 0) {
                isNewArticle = true;
            }
            
            updateArticleFields(article, request);
        } else {
            // 新建文章
            article = buildArticle(request, userId);
            isNewArticle = true;
        }
        
        // 如果是新发布的文章，扣除积分
        Integer remainingPoints = null;
        if (isNewArticle) {
            var balance = pointsBalanceMapper.selectByUserId(userId);
            if (balance == null || balance.getTotalPoints() < PUBLISH_ARTICLE_POINTS) {
                int currentPoints = balance != null ? balance.getTotalPoints() : 0;
                throw new BusinessException("积分不足，当前积分：" + currentPoints + "，发布文章需要" + PUBLISH_ARTICLE_POINTS + "积分");
            }
            
            // 扣除积分
            int result = pointsBalanceMapper.subtractPoints(userId, PUBLISH_ARTICLE_POINTS);
            if (result <= 0) {
                throw new BusinessException("积分扣除失败");
            }
            
            // 获取扣除后的余额
            balance = pointsBalanceMapper.selectByUserId(userId);
            remainingPoints = balance.getTotalPoints();
            
            // 记录积分明细
            UserPointsDetail detail = new UserPointsDetail();
            detail.setUserId(userId);
            detail.setPointsChange(-PUBLISH_ARTICLE_POINTS);
            detail.setPointsType(PointsType.ADMIN_GRANT.getCode());
            detail.setDescription("发布博客文章");
            detail.setBalanceAfter(balance.getTotalPoints());
            detail.setCreateTime(java.time.LocalDateTime.now());
            pointsDetailMapper.insert(detail);
        }
        
        article.setStatus(1); // 已发布
        article.setPublishTime(new Date());
        
        if (request.getId() != null) {
            blogArticleMapper.updateById(article);
        } else {
            blogArticleMapper.insert(article);
        }
        
        // 更新博客文章总数
        if (isNewArticle) {
            blogConfigService.updateTotalArticles(userId, 1);
        }
        
        // 更新分类文章数量
        if (request.getCategoryId() != null) {
            blogCategoryMapper.incrementArticleCount(request.getCategoryId());
        }
        
        // 更新标签使用次数
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            for (String tagName : request.getTags()) {
                blogTagMapper.incrementUseCount(tagName);
            }
        }
        
        log.info("用户{}发布文章成功，文章ID：{}，是否扣除积分：{}", userId, article.getId(), isNewArticle);
        
        ArticlePublishResponse response = new ArticlePublishResponse();
        response.setArticleId(article.getId());
        response.setPointsRemaining(remainingPoints);
        response.setPublishTime(article.getPublishTime());
        
        return response;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateArticle(Long id, ArticlePublishRequest request) {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        
        BlogArticle article = blogArticleMapper.selectById(id);
        if (article == null) {
            throw new BusinessException("文章不存在");
        }
        if (!article.getUserId().equals(userId)) {
            throw new BusinessException("无权操作");
        }
        
        // 验证文章内容
        validateArticle(request);
        
        // 敏感词检测
        if (sensitiveCheckService.containsSensitiveWords(request.getTitle(), "blog")) {
            throw new BusinessException("文章标题包含敏感词");
        }
        if (sensitiveCheckService.containsSensitiveWords(request.getContent(), "blog")) {
            throw new BusinessException("文章内容包含敏感词");
        }
        
        updateArticleFields(article, request);
        blogArticleMapper.updateById(article);
        
        log.info("用户{}更新文章：{}", userId, id);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteArticle(Long id) {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        
        BlogArticle article = blogArticleMapper.selectById(id);
        if (article == null) {
            throw new BusinessException("文章不存在");
        }
        if (!article.getUserId().equals(userId)) {
            throw new BusinessException("无权操作");
        }
        
        // 软删除
        BlogArticle updateArticle = new BlogArticle();
        updateArticle.setId(id);
        updateArticle.setStatus(3); // 已删除
        blogArticleMapper.updateById(updateArticle);
        
        // 如果文章是已发布状态，更新博客文章总数
        if (article.getStatus() == 1) {
            blogConfigService.updateTotalArticles(userId, -1);
        }
        
        log.info("用户{}删除文章：{}", userId, id);
    }
    
    @Override
    public ArticleDetailResponse getArticleDetail(Long id) {
        BlogArticle article = blogArticleMapper.selectById(id);
        if (article == null) {
            throw new BusinessException("文章不存在");
        }
        
        // 检查权限：只有作者可以查看草稿和已删除的文章
        Long currentUserId = StpUserUtil.isLogin() ? StpUserUtil.getLoginIdAsLong() : null;
        if ((article.getStatus() == 0 || article.getStatus() == 3) 
            && (currentUserId == null || !article.getUserId().equals(currentUserId))) {
            throw new BusinessException("文章不存在或已下架");
        }
        
        ArticleDetailResponse response = BeanUtil.copyProperties(article, ArticleDetailResponse.class);
        
        // 解析标签
        if (article.getTags() != null) {
            response.setTags(JSONUtil.toList(article.getTags(), String.class));
        }
        
        // 获取分类名称
        if (article.getCategoryId() != null) {
            BlogCategory category = blogCategoryMapper.selectById(article.getCategoryId());
            if (category != null) {
                response.setCategoryName(category.getCategoryName());
            }
        }
        
        // 判断是否可编辑和删除
        response.setCanEdit(currentUserId != null && article.getUserId().equals(currentUserId));
        response.setCanDelete(currentUserId != null && article.getUserId().equals(currentUserId));
        
        // 获取相关文章
        List<ArticleSimpleResponse> relatedArticles = getRelatedArticles(id, 5);
        response.setRelatedArticles(relatedArticles);
        
        return response;
    }
    
    @Override
    public PageResult<ArticleSimpleResponse> getUserArticleList(ArticleListRequest request) {
        // 先获取分页的原始文章数据
        PageResult<BlogArticle> pageResult = PageHelper.doPage(request.getPageNum(), request.getPageSize(), () -> {
            return blogArticleMapper.selectUserArticleList(
                request.getUserId(),
                request.getCategoryId(),
                request.getTags() != null ? JSONUtil.toJsonStr(request.getTags()) : null,
                1 // 只查询已发布的文章
            );
        });
        
        // 在分页外进行DTO转换，避免额外查询干扰分页计数
        List<ArticleSimpleResponse> responses = pageResult.getRecords().stream()
            .map(this::convertToSimpleResponse)
            .collect(Collectors.toList());
        
        // 构造返回结果，保持分页信息
        return PageResult.of(
            pageResult.getPageNum(),
            pageResult.getPageSize(),
            pageResult.getTotal(),
            responses
        );
    }
    
    @Override
    public PageResult<ArticleSimpleResponse> getMyArticleList(ArticleListRequest request) {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        request.setUserId(userId);
        request.setStatus(1); // 已发布
        
        // 先获取分页的原始文章数据
        PageResult<BlogArticle> pageResult = PageHelper.doPage(request.getPageNum(), request.getPageSize(), () -> {
            return blogArticleMapper.selectUserArticleList(
                userId,
                request.getCategoryId(),
                request.getTags() != null ? JSONUtil.toJsonStr(request.getTags()) : null,
                1
            );
        });
        
        // 在分页外进行DTO转换，避免额外查询干扰分页计数
        List<ArticleSimpleResponse> responses = pageResult.getRecords().stream()
            .map(this::convertToSimpleResponse)
            .collect(Collectors.toList());
        
        // 构造返回结果，保持分页信息
        return PageResult.of(
            pageResult.getPageNum(),
            pageResult.getPageSize(),
            pageResult.getTotal(),
            responses
        );
    }
    
    @Override
    public PageResult<ArticleSimpleResponse> getMyDraftList(Integer pageNum, Integer pageSize) {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        
        // 先获取分页的原始草稿数据
        PageResult<BlogArticle> pageResult = PageHelper.doPage(pageNum, pageSize, () -> {
            return blogArticleMapper.selectByUserIdAndStatus(userId, 0);
        });
        
        // 在分页外进行DTO转换，避免额外查询干扰分页计数
        List<ArticleSimpleResponse> responses = pageResult.getRecords().stream()
            .map(this::convertToSimpleResponse)
            .collect(Collectors.toList());
        
        // 构造返回结果，保持分页信息
        return PageResult.of(
            pageResult.getPageNum(),
            pageResult.getPageSize(),
            pageResult.getTotal(),
            responses
        );
    }
    
    @Override
    public PageResult<ArticleSimpleResponse> getAdminArticleList(AdminArticleListRequest request) {
        // 先获取分页的原始文章数据
        PageResult<BlogArticle> pageResult = PageHelper.doPage(request.getPageNum(), request.getPageSize(), () -> {
            return blogArticleMapper.selectAdminArticleList(
                request.getUserId(),
                request.getCategoryId(),
                request.getStatus(),
                request.getKeyword(),
                request.getStartTime(),
                request.getEndTime()
            );
        });
        
        // 在分页外进行DTO转换，避免额外查询干扰分页计数
        List<ArticleSimpleResponse> responses = pageResult.getRecords().stream()
            .map(this::convertToSimpleResponse)
            .collect(Collectors.toList());
        
        // 构造返回结果，保持分页信息
        return PageResult.of(
            pageResult.getPageNum(),
            pageResult.getPageSize(),
            pageResult.getTotal(),
            responses
        );
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void topArticle(Long id, Integer duration) {
        BlogArticle article = blogArticleMapper.selectById(id);
        if (article == null) {
            throw new BusinessException("文章不存在");
        }
        
        BlogArticle updateArticle = new BlogArticle();
        updateArticle.setId(id);
        updateArticle.setIsTop(1);
        
        // 计算置顶过期时间
        if (duration != null && duration > 0) {
            Date expireTime = new Date(System.currentTimeMillis() + duration * 24 * 60 * 60 * 1000L);
            updateArticle.setTopExpireTime(expireTime);
        }
        
        blogArticleMapper.updateById(updateArticle);
        
        log.info("文章{}设置置顶，时长：{}天", id, duration);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelTop(Long id) {
        BlogArticle updateArticle = new BlogArticle();
        updateArticle.setId(id);
        updateArticle.setIsTop(0);
        updateArticle.setTopExpireTime(null);
        
        blogArticleMapper.updateById(updateArticle);
        
        log.info("取消文章{}置顶", id);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        BlogArticle article = blogArticleMapper.selectById(id);
        if (article == null) {
            throw new BusinessException("文章不存在");
        }
        
        BlogArticle updateArticle = new BlogArticle();
        updateArticle.setId(id);
        updateArticle.setStatus(status);
        
        blogArticleMapper.updateById(updateArticle);
        
        log.info("更新文章{}状态为：{}", id, status);
    }
    
    @Override
    public List<ArticleSimpleResponse> getArticlesByCategory(Long categoryId, Integer limit) {
        List<BlogArticle> articles = blogArticleMapper.selectByCategoryId(categoryId, limit);
        
        return articles.stream()
            .map(this::convertToSimpleResponse)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<ArticleSimpleResponse> getRelatedArticles(Long articleId, Integer limit) {
        BlogArticle article = blogArticleMapper.selectById(articleId);
        if (article == null) {
            return new ArrayList<>();
        }
        
        List<BlogArticle> relatedArticles = blogArticleMapper.selectRelatedByTags(
            article.getTags(),
            articleId,
            limit
        );
        
        return relatedArticles.stream()
            .map(this::convertToSimpleResponse)
            .collect(Collectors.toList());
    }
    
    // ========== 私有方法 ==========
    
    /**
     * 验证文章内容
     */
    private void validateArticle(ArticlePublishRequest request) {
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new BusinessException("文章标题不能为空");
        }
        if (request.getTitle().length() > 200) {
            throw new BusinessException("文章标题不能超过200个字符");
        }
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new BusinessException("文章内容不能为空");
        }
        if (request.getContent().length() < MIN_CONTENT_LENGTH) {
            throw new BusinessException("文章内容不能少于" + MIN_CONTENT_LENGTH + "个字符");
        }
        if (request.getCategoryId() == null) {
            throw new BusinessException("请选择文章分类");
        }
        if (request.getTags() != null && request.getTags().size() > 5) {
            throw new BusinessException("文章标签最多5个");
        }
    }
    
    /**
     * 构建文章对象
     */
    private BlogArticle buildArticle(ArticlePublishRequest request, Long userId) {
        BlogArticle article = new BlogArticle();
        article.setUserId(userId);
        article.setTitle(request.getTitle());
        article.setCoverImage(request.getCoverImage());
        article.setContent(request.getContent());
        article.setCategoryId(request.getCategoryId());
        article.setIsOriginal(request.getIsOriginal() != null ? request.getIsOriginal() : 1);
        article.setPointsCost(PUBLISH_ARTICLE_POINTS);
        
        // 处理摘要
        if (request.getSummary() != null && !request.getSummary().trim().isEmpty()) {
            article.setSummary(request.getSummary());
        } else {
            // 自动提取前200字符
            String summary = request.getContent().substring(0, Math.min(200, request.getContent().length()));
            article.setSummary(summary);
        }
        
        // 处理标签
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            article.setTags(JSONUtil.toJsonStr(request.getTags()));
        }
        
        return article;
    }
    
    /**
     * 更新文章字段
     */
    private void updateArticleFields(BlogArticle article, ArticlePublishRequest request) {
        article.setTitle(request.getTitle());
        article.setCoverImage(request.getCoverImage());
        article.setContent(request.getContent());
        article.setCategoryId(request.getCategoryId());
        article.setIsOriginal(request.getIsOriginal() != null ? request.getIsOriginal() : 1);
        
        if (request.getSummary() != null && !request.getSummary().trim().isEmpty()) {
            article.setSummary(request.getSummary());
        } else {
            String summary = request.getContent().substring(0, Math.min(200, request.getContent().length()));
            article.setSummary(summary);
        }
        
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            article.setTags(JSONUtil.toJsonStr(request.getTags()));
        }
    }
    
    /**
     * 转换为简单响应对象
     */
    private ArticleSimpleResponse convertToSimpleResponse(BlogArticle article) {
        ArticleSimpleResponse response = BeanUtil.copyProperties(article, ArticleSimpleResponse.class);
        
        // 解析标签
        if (article.getTags() != null) {
            response.setTags(JSONUtil.toList(article.getTags(), String.class));
        }
        
        if (article.getCategoryName() != null) {
            response.setCategoryName(article.getCategoryName());
        } else if (article.getCategoryId() != null) {
            BlogCategory category = blogCategoryMapper.selectById(article.getCategoryId());
            if (category != null) {
                response.setCategoryName(category.getCategoryName());
            }
        }
        
        return response;
    }
}

