package com.xiaou.system.agent;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 智能体工具目录服务，用于运行时自描述已注册的后端工具。
 *
 * @author xiaou
 */
@Component
@RequiredArgsConstructor
public class AgentToolCatalogService {

    private final ObjectProvider<AgentToolRegistry> toolRegistryProvider;

    public List<AgentToolDefinition> definitions() {
        AgentToolRegistry registry = toolRegistryProvider.getIfAvailable();
        return registry == null ? List.of() : registry.definitions();
    }
}
