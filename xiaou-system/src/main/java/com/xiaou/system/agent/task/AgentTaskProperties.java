package com.xiaou.system.agent.task;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Durable administrator-agent task runtime limits.
 *
 * @author xiaou
 */
@Data
@Component
@ConfigurationProperties(prefix = "xiaou.admin-agent.task")
public class AgentTaskProperties {

    private boolean enabled;
    private int batchSize = 2;
    private int maxSteps = 5;
    private long leaseSeconds = 300L;
    private long fixedDelayMs = 2_000L;
    private long initialDelayMs = 10_000L;
    private int maxResultJsonChars = 50_000;
    private int maxSummaryChars = 1_000;
    private int maxWorkflowContextJsonChars = 4_000;
    private int maxWorkflowContextKeys = 20;
    private int maxWorkflowContextDepth = 4;
    private int maxWorkflowContextCollectionItems = 20;
    private int maxWorkflowContextStringChars = 1_000;

    public int normalizedBatchSize() {
        return Math.max(1, Math.min(batchSize, 20));
    }

    public int normalizedMaxSteps() {
        return Math.max(1, Math.min(maxSteps, 10));
    }

    public long normalizedLeaseSeconds() {
        return Math.max(90L, Math.min(leaseSeconds, 3_600L));
    }

    public long normalizedFixedDelayMs() {
        return Math.max(250L, Math.min(fixedDelayMs, 60_000L));
    }

    public long normalizedInitialDelayMs() {
        return Math.max(0L, Math.min(initialDelayMs, 300_000L));
    }

    public int normalizedMaxResultJsonChars() {
        return Math.max(1_000, Math.min(maxResultJsonChars, 1_000_000));
    }

    public int normalizedMaxSummaryChars() {
        return Math.max(100, Math.min(maxSummaryChars, 4_000));
    }

    public int normalizedMaxWorkflowContextJsonChars() {
        return Math.max(512, Math.min(maxWorkflowContextJsonChars, 20_000));
    }

    public int normalizedMaxWorkflowContextKeys() {
        return Math.max(1, Math.min(maxWorkflowContextKeys, 50));
    }

    public int normalizedMaxWorkflowContextDepth() {
        return Math.max(1, Math.min(maxWorkflowContextDepth, 8));
    }

    public int normalizedMaxWorkflowContextCollectionItems() {
        return Math.max(1, Math.min(maxWorkflowContextCollectionItems, 100));
    }

    public int normalizedMaxWorkflowContextStringChars() {
        return Math.max(64, Math.min(maxWorkflowContextStringChars, 4_000));
    }
}
