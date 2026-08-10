package com.xiaou.system.agent.task;

import java.io.Serializable;

/**
 * Serializable graph context. The snapshot is a one-invocation copy only; all mutations
 * still use MySQL compare-and-set transitions as the source of truth.
 */
public final class AgentTaskCycleContext implements Serializable {

    private final String taskId;
    private final String leaseOwner;
    private final boolean active;
    private final AgentTaskSnapshot snapshot;

    public AgentTaskCycleContext(AgentTaskSnapshot snapshot, String leaseOwner) {
        this.taskId = snapshot == null || snapshot.task() == null ? "" : snapshot.task().getTaskId();
        this.leaseOwner = leaseOwner == null ? "" : leaseOwner;
        this.active = snapshot != null
                && snapshot.task() != null
                && AgentTaskStatus.RUNNING.name().equals(snapshot.task().getStatus())
                && leaseOwner != null
                && leaseOwner.equals(snapshot.task().getLeaseOwner());
        this.snapshot = snapshot;
    }

    public AgentTaskCycleContext(String taskId, String leaseOwner, boolean active) {
        this.taskId = taskId == null ? "" : taskId;
        this.leaseOwner = leaseOwner == null ? "" : leaseOwner;
        this.active = active;
        this.snapshot = null;
    }

    public String taskId() {
        return taskId;
    }

    public String leaseOwner() {
        return leaseOwner;
    }

    public boolean active() {
        return active;
    }

    public AgentTaskSnapshot snapshot() {
        return snapshot;
    }
}
