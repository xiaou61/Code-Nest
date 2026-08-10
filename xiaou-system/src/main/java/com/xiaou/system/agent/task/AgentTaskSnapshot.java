package com.xiaou.system.agent.task;

import com.xiaou.system.domain.SysAgentTask;
import com.xiaou.system.domain.SysAgentTaskStep;

import java.util.List;

/**
 * Authoritative persisted task state loaded for one execution cycle.
 */
public record AgentTaskSnapshot(SysAgentTask task, List<SysAgentTaskStep> steps)
        implements java.io.Serializable {
    public AgentTaskSnapshot {
        steps = steps == null ? List.of() : List.copyOf(steps);
    }

    public SysAgentTaskStep latestStep() {
        return steps.isEmpty() ? null : steps.get(steps.size() - 1);
    }
}
