package com.xiaou.system.agent;

import lombok.Data;

/**
 * 管理员智能体单轮会话摘要。
 *
 * @author xiaou
 */
@Data
public class AgentSessionTurn {

    private String message;

    private String status;

    private String answer;

    private String toolName;

    private String auditId;

    private String traceId;
}
