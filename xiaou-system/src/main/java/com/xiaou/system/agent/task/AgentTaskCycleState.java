package com.xiaou.system.agent.task;

import org.bsc.langgraph4j.state.AgentState;

import java.util.Map;

/**
 * In-memory state for one graph cycle; durable state remains in MySQL.
 */
public class AgentTaskCycleState extends AgentState {

    public static final String TASK_ID = "taskId";
    public static final String LEASE_OWNER = "leaseOwner";
    public static final String CONTEXT = "context";
    public static final String DECISION = "decision";
    public static final String POLICY_DECISION = "policyDecision";
    public static final String OUTCOME = "outcome";
    public static final String ROUTE = "route";

    public AgentTaskCycleState(Map<String, Object> data) {
        super(data);
    }

    public String taskId() {
        return stringValue(TASK_ID);
    }

    public String leaseOwner() {
        return stringValue(LEASE_OWNER);
    }

    public AgentTaskPlanDecision decision() {
        Object value = data().get(DECISION);
        return value instanceof AgentTaskPlanDecision decision ? decision : null;
    }

    public AgentTaskCycleContext context() {
        Object value = data().get(CONTEXT);
        return value instanceof AgentTaskCycleContext context ? context : null;
    }

    public AgentTaskPlanDecision policyDecision() {
        Object value = data().get(POLICY_DECISION);
        return value instanceof AgentTaskPlanDecision decision ? decision : null;
    }

    public AgentTaskCycleOutcome outcome() {
        Object value = data().get(OUTCOME);
        return value instanceof AgentTaskCycleOutcome outcome ? outcome : null;
    }

    private String stringValue(String key) {
        Object value = data().get(key);
        return value == null ? "" : value.toString();
    }
}
