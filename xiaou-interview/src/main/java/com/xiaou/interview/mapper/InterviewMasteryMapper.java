package com.xiaou.interview.mapper;

import com.xiaou.interview.domain.InterviewMasteryRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 面试题掌握度记录Mapper
 *
 * @author xiaou
 */
@Mapper
public interface InterviewMasteryMapper {

    /**
     * 插入掌握度记录
     */
    int insert(InterviewMasteryRecord record);

    /**
     * 更新掌握度记录
     */
    int update(InterviewMasteryRecord record);

    /**
     * 根据用户ID和题目ID查询
     */
    InterviewMasteryRecord selectByUserAndQuestion(@Param("userId") Long userId, @Param("questionId") Long questionId);

    /**
     * 批量查询用户对多个题目的掌握度
     */
    List<InterviewMasteryRecord> selectByUserAndQuestions(@Param("userId") Long userId, @Param("questionIds") List<Long> questionIds);

    /**
     * 按更新时间增量读取掌握度记录，供 Growth Coach 证据投影使用。
     */
    List<InterviewMasteryRecord> selectChangedForEvidence(@Param("userId") Long userId,
                                                           @Param("afterUpdateTime") LocalDateTime afterUpdateTime,
                                                           @Param("afterSourceId") Long afterSourceId,
                                                           @Param("limit") int limit);

    /**
     * 查询近期变更的用户，供证据补偿任务发现待投影数据。
     */
    List<Long> selectUserIdsChangedForEvidence(@Param("updatedAfter") LocalDateTime updatedAfter,
                                                @Param("limit") int limit);

    /**
     * 查询用户待复习题目（逾期）
     */
    List<InterviewMasteryRecord> selectOverdueReview(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    /**
     * 查询用户今日待复习题目
     */
    List<InterviewMasteryRecord> selectTodayReview(@Param("userId") Long userId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 查询用户本周待复习题目
     */
    List<InterviewMasteryRecord> selectWeekReview(@Param("userId") Long userId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 统计逾期复习数量
     */
    int countOverdueReview(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    /**
     * 统计今日待复习数量
     */
    int countTodayReview(@Param("userId") Long userId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 统计本周待复习数量
     */
    int countWeekReview(@Param("userId") Long userId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 统计用户已学习总题数
     */
    int countTotalLearned(@Param("userId") Long userId);

    /**
     * 按掌握度等级统计
     */
    int countByMasteryLevel(@Param("userId") Long userId, @Param("masteryLevel") Integer masteryLevel);

    /**
     * 插入历史记录
     */
    int insertHistory(@Param("userId") Long userId, @Param("questionId") Long questionId, 
                      @Param("masteryLevel") Integer masteryLevel, @Param("isReview") Integer isReview);
}
