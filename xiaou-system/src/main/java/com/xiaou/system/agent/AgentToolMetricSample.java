package com.xiaou.system.agent;

import lombok.Data;

/**
 * 单次工具调用指标样本。trace/session 只用于排障上下文，不进入指标标签。
 *
 * @author xiaou
 */
@Data
public class AgentToolMetricSample {

    private String toolName;

    private String phase;

    private String outcome;

    private String riskLevel;

    private String riskCategory;

    private long durationNanos;

    private String traceId;

    private String sessionId;
}
