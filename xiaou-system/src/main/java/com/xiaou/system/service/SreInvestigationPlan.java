package com.xiaou.system.service;

import java.util.List;

/**
 * 经过后端白名单收敛后的有界调查计划。
 *
 * @author xiaou
 */
public record SreInvestigationPlan(
        String decision,
        String reason,
        List<String> toolKeys,
        String generationMode,
        String invocationOutcome
) {
    public SreInvestigationPlan {
        toolKeys = toolKeys == null ? List.of() : List.copyOf(toolKeys);
    }
}
