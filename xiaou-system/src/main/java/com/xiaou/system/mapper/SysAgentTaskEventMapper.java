package com.xiaou.system.mapper;

import com.xiaou.system.domain.SysAgentTaskEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Append-only mapper for durable administrator-agent task events.
 */
@Mapper
public interface SysAgentTaskEventMapper {

    int insert(SysAgentTaskEvent event);

    List<SysAgentTaskEvent> selectOwnedAfter(
            @Param("taskId") String taskId,
            @Param("operatorId") Long operatorId,
            @Param("afterCursor") Long afterCursor,
            @Param("limit") int limit
    );
}
