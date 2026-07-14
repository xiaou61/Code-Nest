package com.xiaou.system.agent;

import java.util.Optional;

/**
 * 管理员智能体计划解析器，只负责生成受限工具调用候选。
 *
 * @author xiaou
 */
public interface AgentPlanResolver {

    Optional<AgentResolvedToolCall> resolve(String message);

    default AgentPlanResolution resolvePlan(AgentExecutionContext context) {
        return resolvePlan(context == null ? "" : context.message());
    }

    default AgentPlanResolution resolvePlan(String message) {
        return resolve(message)
                .map(AgentPlanResolution::resolved)
                .orElseGet(AgentPlanResolution::empty);
    }
}
