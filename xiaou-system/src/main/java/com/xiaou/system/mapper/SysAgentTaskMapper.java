package com.xiaou.system.mapper;

import com.xiaou.system.domain.SysAgentTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Durable administrator-agent task mapper.
 */
@Mapper
public interface SysAgentTaskMapper {

    int insert(SysAgentTask task);

    SysAgentTask selectByTaskId(String taskId);

    SysAgentTask selectOwned(@Param("taskId") String taskId, @Param("operatorId") Long operatorId);

    List<SysAgentTask> selectOwnedList(@Param("operatorId") Long operatorId,
                                       @Param("status") String status,
                                       @Param("limit") int limit);

    List<String> selectClaimableIds(@Param("limit") int limit);

    List<String> selectStaleRunningIds(@Param("staleBefore") LocalDateTime staleBefore,
                                       @Param("limit") int limit);

    int claim(@Param("taskId") String taskId,
              @Param("leaseOwner") String leaseOwner,
              @Param("claimedAt") LocalDateTime claimedAt);

    int heartbeat(@Param("taskId") String taskId,
                  @Param("leaseOwner") String leaseOwner,
                  @Param("heartbeatAt") LocalDateTime heartbeatAt);

    int transition(SysAgentTask task);

    int transitionStale(@Param("task") SysAgentTask task,
                        @Param("staleBefore") LocalDateTime staleBefore);

    int cancelOwned(@Param("taskId") String taskId,
                    @Param("operatorId") Long operatorId,
                    @Param("reason") String reason,
                    @Param("cancelledAt") LocalDateTime cancelledAt);

    long countByStatus(String status);
}
