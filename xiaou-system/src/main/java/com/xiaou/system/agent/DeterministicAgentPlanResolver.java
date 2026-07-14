package com.xiaou.system.agent;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 模型不可用或模型没有给出可执行计划时的离线确定性兜底解析器。
 *
 * @author xiaou
 */
@Component
@RequiredArgsConstructor
public class DeterministicAgentPlanResolver implements AgentPlanResolver {

    private final AgentToolRegistry toolRegistry;

    @Override
    public Optional<AgentResolvedToolCall> resolve(String message) {
        return toolRegistry.resolve(message);
    }
}
