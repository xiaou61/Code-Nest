package com.xiaou.system.agent.task;

/**
 * Produces one bounded next-step decision for a durable administrator task.
 */
public interface AgentTaskPlanner {

    AgentTaskPlanDecision plan(AgentTaskPlanningContext context);
}
