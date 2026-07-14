package com.xiaou.system.mapper;

import com.xiaou.system.domain.SysAgentAudit;
import com.xiaou.system.dto.AgentAuditQueryRequest;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 智能体审计 Mapper
 *
 * @author xiaou
 */
@Mapper
public interface SysAgentAuditMapper {

    int insert(SysAgentAudit agentAudit);

    int updateByAuditId(SysAgentAudit agentAudit);

    SysAgentAudit selectByAuditId(String auditId);

    List<SysAgentAudit> selectList(AgentAuditQueryRequest query);
}
