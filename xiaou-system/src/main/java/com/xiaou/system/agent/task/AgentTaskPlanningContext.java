package com.xiaou.system.agent.task;

import com.xiaou.system.domain.SysAgentTaskStep;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Persisted task history supplied to one planner cycle.
 */
public record AgentTaskPlanningContext(
        String goal,
        List<SysAgentTaskStep> completedSteps,
        int remainingSteps,
        Map<String, Object> workflowContext
) {
    public AgentTaskPlanningContext(String goal, List<SysAgentTaskStep> completedSteps, int remainingSteps) {
        this(goal, completedSteps, remainingSteps, Map.of());
    }

    public AgentTaskPlanningContext {
        goal = goal == null ? "" : goal.trim();
        completedSteps = completedSteps == null ? List.of() : List.copyOf(completedSteps);
        remainingSteps = Math.max(0, remainingSteps);
        workflowContext = workflowContext == null
                ? Map.of()
                : java.util.Collections.unmodifiableMap(new LinkedHashMap<>(workflowContext));
    }
}
