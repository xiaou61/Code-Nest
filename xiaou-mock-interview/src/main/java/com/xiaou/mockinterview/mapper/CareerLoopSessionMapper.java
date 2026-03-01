package com.xiaou.mockinterview.mapper;

import com.xiaou.mockinterview.domain.CareerLoopSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 求职闭环会话Mapper
 *
 * @author xiaou
 */
@Mapper
public interface CareerLoopSessionMapper {

    CareerLoopSession selectActiveByUserId(@Param("userId") Long userId);

    int insert(CareerLoopSession session);

    int updateById(CareerLoopSession session);
}

