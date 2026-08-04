package com.xiaou.mockinterview.mapper;

import com.xiaou.mockinterview.domain.CareerLoopStageLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 求职闭环阶段日志Mapper
 *
 * @author xiaou
 */
@Mapper
public interface CareerLoopStageLogMapper {

    int insert(CareerLoopStageLog stageLog);

    List<CareerLoopStageLog> selectBySessionId(@Param("sessionId") Long sessionId);

    /**
     * 按创建时间增量读取用户求职阶段日志，供成长证据投影使用。
     */
    List<CareerLoopStageLog> selectChangedForEvidence(@Param("userId") Long userId,
                                                       @Param("afterCreateTime") LocalDateTime afterCreateTime,
                                                       @Param("afterSourceId") Long afterSourceId,
                                                       @Param("limit") int limit);

    /**
     * 查询近期产生阶段日志的用户，用于投影补偿扫描。
     */
    List<Long> selectUserIdsChangedForEvidence(@Param("createdAfter") LocalDateTime createdAfter,
                                                @Param("limit") int limit);
}

