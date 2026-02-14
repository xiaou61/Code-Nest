package com.xiaou.points.service;

import com.xiaou.common.core.domain.PageResult;
import com.xiaou.points.dto.*;

/**
 * 积分服务接口
 * 
 * @author xiaou
 */
public interface PointsService {
    
    /**
     * 用户打卡
     */
    CheckinResponse checkin(Long userId);
    
    /**
     * 获取用户积分余额信息
     */
    PointsBalanceResponse getPointsBalance(Long userId);
    
    /**
     * 获取用户积分明细列表
     */
    PageResult<PointsDetailResponse> getPointsDetailList(PointsDetailQueryRequest request);
    
    /**
     * 获取打卡日历
     */
    CheckinCalendarResponse getCheckinCalendar(Long userId, String yearMonth);
    
    /**
     * 获取打卡统计数据
     */
    CheckinStatisticsResponse getCheckinStatistics(Long userId, Integer months);
    
    /**
     * 管理员发放积分
     */
    AdminGrantPointsResponse grantPoints(AdminGrantPointsRequest request, Long adminId);
    
    /**
     * 获取所有用户积分明细列表（管理端）
     */
    PageResult<PointsDetailResponse> getAllPointsDetailList(PointsDetailQueryRequest request);
    
    /**
     * 获取积分统计数据（管理端）
     */
    AdminPointsStatisticsResponse getAdminStatistics();
    
    /**
     * 为新用户创建积分账户
     */
    void createPointsAccountForNewUser(Long userId);
    
    /**
     * 获取用户积分列表（管理端）
     */
    PageResult<UserPointsRankingResponse> getUserPointsList(UserPointsListRequest request);
    
    /**
     * 批量发放积分
     */
    BatchGrantPointsResponse batchGrantPoints(BatchGrantPointsRequest request, Long adminId);
    
    /**
     * 根据用户ID获取用户积分信息
     */
    UserPointsInfoResponse getUserPointsInfo(Long userId);

    /**
     * 系统发放积分（供OJ等模块调用）
     */
    void grantSystemPoints(Long userId, int points, int type, String description);
}
