package com.xiaou.mockinterview.mapper;

import com.xiaou.mockinterview.domain.MockInterviewSession;
import com.xiaou.mockinterview.dto.request.InterviewHistoryRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 模拟面试会话Mapper
 *
 * @author xiaou
 */
@Mapper
public interface MockInterviewSessionMapper {

    /**
     * 插入会话
     */
    int insert(MockInterviewSession session);

    /**
     * 根据ID更新会话
     */
    int updateById(MockInterviewSession session);

    /**
     * 根据ID查询会话
     */
    MockInterviewSession selectById(Long id);

    /**
     * 根据用户ID查询进行中的会话
     */
    MockInterviewSession selectOngoingByUserId(Long userId);

    /**
     * 分页查询用户的面试历史
     */
    List<MockInterviewSession> selectPageByUserId(@Param("userId") Long userId,
                                                   @Param("request") InterviewHistoryRequest request);

    /**
     * 统计用户的面试历史数量
     */
    long countByUserId(@Param("userId") Long userId,
                       @Param("request") InterviewHistoryRequest request);

    /**
     * 根据ID删除会话
     */
    int deleteById(Long id);

    /**
     * 统计用户某方向的面试次数
     */
    int countByUserIdAndDirection(@Param("userId") Long userId, @Param("direction") String direction);

    /**
     * 查询用户最近完成的面试
     */
    List<MockInterviewSession> selectRecentCompleted(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * 管理端分页查询所有面试记录
     */
    List<MockInterviewSession> selectPageAll(@Param("request") InterviewHistoryRequest request);

    /**
     * 管理端统计所有面试记录数量
     */
    long countAll(@Param("request") InterviewHistoryRequest request);

    /**
     * 管理端统计总览
     */
    Map<String, Object> selectAdminOverviewStats(@Param("startTime") String startTime,
                                                 @Param("endTime") String endTime);

    /**
     * 管理端方向分布统计
     */
    List<Map<String, Object>> selectDirectionDistributionStats(@Param("startTime") String startTime,
                                                               @Param("endTime") String endTime);
}
