package com.xiaou.system.agent.task;

/**
 * Stable, bounded event names exposed by the durable task timeline.
 */
public enum AgentTaskEventType {
    TASK_CREATED,
    TASK_CLAIMED,
    TASK_QUEUED,
    TASK_STATE_CHANGED,
    TASK_PAUSED,
    TASK_RESUMED,
    TASK_WAITING_INPUT,
    TASK_WAITING_CONFIRMATION,
    TASK_COMPLETED,
    TASK_FAILED,
    TASK_CANCELLED,
    TASK_REQUIRES_REVIEW,
    TASK_RECOVERED,
    STEP_STARTED,
    STEP_WAITING_CONFIRMATION,
    STEP_COMPLETED,
    STEP_FAILED,
    STEP_CANCELLED,
    STEP_REQUIRES_REVIEW,
    STEP_STATE_CHANGED,
    INPUT_SUBMITTED
}
