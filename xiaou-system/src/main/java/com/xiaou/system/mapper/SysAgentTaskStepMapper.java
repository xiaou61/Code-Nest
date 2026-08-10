package com.xiaou.system.mapper;

import com.xiaou.system.domain.SysAgentTaskStep;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Durable administrator-agent task-step mapper.
 */
@Mapper
public interface SysAgentTaskStepMapper {

    int insert(SysAgentTaskStep step);

    SysAgentTaskStep selectByTaskAndOrder(@Param("taskId") String taskId,
                                          @Param("stepOrder") int stepOrder);

    SysAgentTaskStep selectLatest(String taskId);

    SysAgentTaskStep selectByAuditId(String auditId);

    List<SysAgentTaskStep> selectByTaskId(String taskId);

    int transition(SysAgentTaskStep step);

    int cancelUnstarted(String taskId);

    int existsFingerprint(@Param("taskId") String taskId,
                          @Param("fingerprint") String fingerprint);
}
