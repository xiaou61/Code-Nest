package com.xiaou.mockinterview.mapper;

import com.xiaou.mockinterview.domain.CareerLoopStageLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
}

