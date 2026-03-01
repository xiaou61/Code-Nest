package com.xiaou.mockinterview.mapper;

import com.xiaou.mockinterview.domain.CareerLoopSnapshot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 求职闭环快照Mapper
 *
 * @author xiaou
 */
@Mapper
public interface CareerLoopSnapshotMapper {

    CareerLoopSnapshot selectBySessionId(@Param("sessionId") Long sessionId);

    int insert(CareerLoopSnapshot snapshot);

    int updateBySessionId(CareerLoopSnapshot snapshot);
}

