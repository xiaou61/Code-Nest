package com.xiaou.system.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * SRE 调查计划冻结输入。
 *
 * @author xiaou
 */
public record SreInvestigationPlanningInput(
        String contextJson,
        Set<String> availableToolKeys,
        List<String> fallbackToolKeys
) {
    public SreInvestigationPlanningInput {
        availableToolKeys = availableToolKeys == null
                ? Set.of()
                : java.util.Collections.unmodifiableSet(new LinkedHashSet<>(availableToolKeys));
        fallbackToolKeys = fallbackToolKeys == null ? List.of() : List.copyOf(fallbackToolKeys);
    }
}
