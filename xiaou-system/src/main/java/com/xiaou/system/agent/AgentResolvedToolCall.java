package com.xiaou.system.agent;

/**
 * 已解析出的工具和调用参数。
 *
 * @author xiaou
 */
public record AgentResolvedToolCall(AgentTool tool, AgentToolCall call) {
}
