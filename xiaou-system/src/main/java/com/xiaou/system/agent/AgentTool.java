package com.xiaou.system.agent;

import java.util.Optional;

/**
 * 管理员智能体工具适配器。
 *
 * @author xiaou
 */
public interface AgentTool {

    AgentToolDefinition definition();

    /**
     * 可选的离线确定性解析提示。正常运行时由 LLM 根据 definition/schema 规划，
     * 只有模型不可用或规划无结果时才使用该兜底能力。
     *
     * @param message 用户自然语言请求
     * @return 确定性工具调用，无法解析时返回空
     */
    default Optional<AgentToolCall> resolve(String message) {
        return Optional.empty();
    }

    AgentToolPreview preview(AgentToolCall call, AgentExecutionContext context);

    AgentToolResult execute(AgentToolCall call, AgentExecutionContext context);
}
