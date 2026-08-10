package com.xiaou.system.agent.task;

import com.xiaou.system.agent.AgentToolCall;
import com.xiaou.system.domain.SysAgentTaskStep;

/**
 * Persisted running step and its server-validated call.
 */
public record AgentTaskStepExecution(SysAgentTaskStep step, AgentToolCall call) {
}
