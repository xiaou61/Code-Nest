package com.xiaou.mockinterview.mapper;

import com.xiaou.mockinterview.domain.CareerLoopAction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 求职闭环动作Mapper
 *
 * @author xiaou
 */
@Mapper
public interface CareerLoopActionMapper {

    int insert(CareerLoopAction action);

    List<CareerLoopAction> selectBySessionId(@Param("sessionId") Long sessionId);

    int markDoneById(@Param("id") Long id, @Param("sessionId") Long sessionId);

    int deleteTodoBySessionId(@Param("sessionId") Long sessionId);
}

