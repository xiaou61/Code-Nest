package com.xiaou.system.agent.task;

import com.xiaou.system.domain.SysAgentTask;
import com.xiaou.system.domain.SysAgentTaskStep;

/**
 * Atomic claim of one waiting confirmation.
 */
public record AgentTaskConfirmationClaim(
        SysAgentTask task,
        SysAgentTaskStep step,
        String leaseOwner
) {
}
