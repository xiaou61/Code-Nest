package com.xiaou.blog.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.xiaou.blog.domain.BlogConfig;
import com.xiaou.blog.dto.BlogCheckStatusResponse;
import com.xiaou.blog.dto.BlogConfigResponse;
import com.xiaou.blog.dto.BlogConfigUpdateRequest;
import com.xiaou.blog.dto.BlogOpenResponse;
import com.xiaou.blog.dto.BlogStatisticsResponse;
import com.xiaou.blog.mapper.BlogArticleMapper;
import com.xiaou.blog.mapper.BlogConfigMapper;
import com.xiaou.blog.service.BlogConfigService;
import com.xiaou.common.exception.BusinessException;
import com.xiaou.common.satoken.StpUserUtil;
import com.xiaou.common.utils.JsonUtils;
import com.xiaou.points.mapper.UserPointsBalanceMapper;
import com.xiaou.points.mapper.UserPointsDetailMapper;
import com.xiaou.points.domain.UserPointsDetail;
import com.xiaou.points.enums.PointsType;
import com.xiaou.user.api.UserInfoApiService;
import com.xiaou.user.api.dto.SimpleUserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * 博客配置Service实现类
 * 
 * @author xiaou
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BlogConfigServiceImpl implements BlogConfigService {
    
    private final BlogConfigMapper blogConfigMapper;
    private final BlogArticleMapper blogArticleMapper;
    private final UserPointsBalanceMapper pointsBalanceMapper;
    private final UserPointsDetailMapper pointsDetailMapper;
    private final UserInfoApiService userInfoApiService;
    
    private static final int OPEN_BLOG_POINTS = 50;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BlogOpenResponse openBlog() {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        
        // 检查是否已开通
        BlogConfig existConfig = blogConfigMapper.selectByUserId(userId);
        if (existConfig != null) {
            throw new BusinessException("您已开通博客，无需重复开通");
        }
        
        // 检查积分是否充足
        var balance = pointsBalanceMapper.selectByUserId(userId);
        int currentPoints = balance != null ? balance.getTotalPoints() : 0;
        if (balance == null || balance.getTotalPoints() < OPEN_BLOG_POINTS) {
            throw new BusinessException("积分不足，当前积分：" + currentPoints + "，开通博客需要" + OPEN_BLOG_POINTS + "积分");
        }
        
        // 扣除积分
        int result = pointsBalanceMapper.subtractPoints(userId, OPEN_BLOG_POINTS);
        if (result <= 0) {
            throw new BusinessException("积分扣除失败");
        }
        
        // 获取扣除后的余额
        balance = pointsBalanceMapper.selectByUserId(userId);
        
        // 记录积分明细
        UserPointsDetail detail = new UserPointsDetail();
        detail.setUserId(userId);
        detail.setPointsChange(-OPEN_BLOG_POINTS);
        detail.setPointsType(PointsType.ADMIN_GRANT.getCode()); // 使用后台发放类型
        detail.setDescription("开通博客");
        detail.setBalanceAfter(balance.getTotalPoints());
        detail.setCreateTime(java.time.LocalDateTime.now());
        pointsDetailMapper.insert(detail);
        
        // 获取用户信息
        SimpleUserInfo user = userInfoApiService.getSimpleUserInfo(userId);
        
        // 创建博客配置
        BlogConfig config = new BlogConfig();
        config.setUserId(userId);
        config.setBlogName(user.getNickname() + "的博客");
        config.setBlogDescription("这是一个技术博客");
        config.setBlogAvatar(user.getAvatar());
        config.setIsPublic(1);
        config.setPointsCost(OPEN_BLOG_POINTS);
        config.setStatus(1);
        
        blogConfigMapper.insert(config);
        
        // 构建响应
        BlogOpenResponse response = new BlogOpenResponse();
        response.setBlogId(config.getId());
        response.setUserId(userId);
        response.setPointsRemaining(currentPoints - OPEN_BLOG_POINTS);
        response.setCreateTime(new Date());
        
        log.info("用户{}开通博客成功，扣除{}积分", userId, OPEN_BLOG_POINTS);
        
        return response;
    }
    
    @Override
    public BlogCheckStatusResponse checkBlogStatus() {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        
        BlogConfig config = blogConfigMapper.selectByUserId(userId);
        
        BlogCheckStatusResponse response = new BlogCheckStatusResponse();
        
        if (config != null) {
            // 已开通
            response.setIsOpened(true);
            response.setBlogId(config.getId());
            response.setCreateTime(config.getCreateTime());
        } else {
            // 未开通
            var balance = pointsBalanceMapper.selectByUserId(userId);
            Integer currentPoints = balance != null ? balance.getTotalPoints() : 0;
            response.setIsOpened(false);
            response.setCurrentPoints(currentPoints);
            response.setRequiredPoints(OPEN_BLOG_POINTS);
            response.setCanOpen(currentPoints >= OPEN_BLOG_POINTS);
        }
        
        return response;
    }
    
    @Override
    public BlogConfigResponse getBlogConfigByUserId(Long userId) {
        BlogConfig config = blogConfigMapper.selectByUserId(userId);
        if (config == null) {
            throw new BusinessException("该用户未开通博客");
        }
        
        BlogConfigResponse response = BeanUtil.copyProperties(config, BlogConfigResponse.class);
        
        // 解析JSON字段
        if (config.getPersonalTags() != null) {
            response.setPersonalTags(JSONUtil.toList(config.getPersonalTags(), String.class));
        }
        if (config.getSocialLinks() != null) {
            response.setSocialLinks(JsonUtils.parseStringMap(config.getSocialLinks()));
        }
        
        return response;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBlogConfig(BlogConfigUpdateRequest request) {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        
        BlogConfig config = blogConfigMapper.selectByUserId(userId);
        if (config == null) {
            throw new BusinessException("请先开通博客");
        }
        
        // 更新配置
        BlogConfig updateConfig = new BlogConfig();
        updateConfig.setId(config.getId());
        
        if (request.getBlogName() != null) {
            updateConfig.setBlogName(request.getBlogName());
        }
        if (request.getBlogDescription() != null) {
            updateConfig.setBlogDescription(request.getBlogDescription());
        }
        if (request.getBlogAvatar() != null) {
            updateConfig.setBlogAvatar(request.getBlogAvatar());
        }
        if (request.getBlogCover() != null) {
            updateConfig.setBlogCover(request.getBlogCover());
        }
        if (request.getBlogNotice() != null) {
            updateConfig.setBlogNotice(request.getBlogNotice());
        }
        if (request.getPersonalTags() != null) {
            updateConfig.setPersonalTags(JSONUtil.toJsonStr(request.getPersonalTags()));
        }
        if (request.getSocialLinks() != null) {
            updateConfig.setSocialLinks(JSONUtil.toJsonStr(request.getSocialLinks()));
        }
        if (request.getIsPublic() != null) {
            updateConfig.setIsPublic(request.getIsPublic());
        }
        
        blogConfigMapper.updateById(updateConfig);
        
        log.info("用户{}更新博客配置成功", userId);
    }
    
    @Override
    public BlogConfig getBlogByUserId(Long userId) {
        return blogConfigMapper.selectByUserId(userId);
    }
    
    @Override
    public void updateTotalArticles(Long userId, Integer increment) {
        blogConfigMapper.updateTotalArticles(userId, increment);
    }
    
    @Override
    public BlogStatisticsResponse getStatistics() {
        BlogStatisticsResponse response = new BlogStatisticsResponse();
        
        response.setTotalBlogs(blogConfigMapper.selectTotalCount());
        response.setTotalArticles(blogArticleMapper.selectTotalCount());
        response.setActiveBloggers(blogConfigMapper.selectActiveBloggersCount());
        
        return response;
    }
}

