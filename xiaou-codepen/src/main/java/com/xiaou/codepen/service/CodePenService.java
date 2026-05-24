package com.xiaou.codepen.service;

import com.xiaou.codepen.domain.CodePen;
import com.xiaou.codepen.dto.*;

import java.util.List;

/**
 * 代码作品Service
 * 
 * @author xiaou
 */
public interface CodePenService {
    
    /**
     * 创建/保存作品（草稿或发布）
     * 首次发布时奖励10积分
     */
    CodePenCreateResponse createOrSave(CodePenCreateRequest request, Long userId);
    
    /**
     * 更新作品
     */
    boolean update(CodePenCreateRequest request, Long userId);
    
    /**
     * 删除作品（软删除）
     */
    boolean delete(Long id, Long userId);
    
    /**
     * 获取作品详情
     * 付费作品未购买时隐藏源码
     */
    CodePenDetailResponse getDetail(Long id, Long currentUserId);

    /**
     * 批量获取作品详情
     */
    List<CodePenDetailResponse> getDetailsByIds(List<Long> ids, Long currentUserId);
    
    /**
     * 分页查询作品列表
     */
    com.xiaou.common.core.domain.PageResult<CodePenDetailResponse> getList(CodePenListRequest request, Long currentUserId);
    
    /**
     * 查询我的作品列表
     */
    List<CodePenDetailResponse> getMyList(Long userId, Integer status);
    
    /**
     * 检查Fork价格和用户积分
     */
    CheckForkPriceResponse checkForkPrice(Long penId, Long userId);
    
    /**
     * Fork作品（免费或付费）
     * 付费时积分转给原作者
     */
    ForkResponse forkPen(ForkRequest request, Long userId);
    
    /**
     * 点赞作品
     */
    boolean like(Long penId, Long userId);
    
    /**
     * 取消点赞
     */
    boolean unlike(Long penId, Long userId);
    
    /**
     * 收藏作品
     */
    boolean collect(Long penId, Long userId, Long folderId);
    
    /**
     * 取消收藏
     */
    boolean uncollect(Long penId, Long userId);
    
    /**
     * 增加浏览数
     */
    boolean incrementView(Long penId);
    
    /**
     * 查询推荐作品
     */
    List<CodePenDetailResponse> getRecommendList(Integer limit);
    
    /**
     * 查询热门作品
     */
    List<CodePenDetailResponse> getHotList(Integer limit);
    
    /**
     * 查询系统模板列表
     */
    List<CodePen> getTemplateList();
    
    /**
     * 获取统计数据
     */
    CodePenStatisticsResponse getStatistics();
    
    /**
     * 获取用户收益统计
     */
    IncomeStatsResponse getIncomeStats(Long userId);
}

