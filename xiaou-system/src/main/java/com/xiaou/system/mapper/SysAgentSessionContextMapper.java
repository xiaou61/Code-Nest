package com.xiaou.system.mapper;

import com.xiaou.system.domain.SysAgentSessionContext;
import org.apache.ibatis.annotations.Mapper;

/**
 * 管理员智能体会话上下文 Mapper。
 *
 * @author xiaou
 */
@Mapper
public interface SysAgentSessionContextMapper {

    SysAgentSessionContext selectBySessionId(String sessionId);

    int upsert(SysAgentSessionContext sessionContext);
}
