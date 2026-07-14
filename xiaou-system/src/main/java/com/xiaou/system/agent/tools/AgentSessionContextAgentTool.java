package com.xiaou.system.agent.tools;

import com.xiaou.system.agent.AgentExecutionContext;
import com.xiaou.system.agent.AgentSessionProperties;
import com.xiaou.system.agent.AgentSessionSnapshot;
import com.xiaou.system.agent.AgentSessionTurn;
import com.xiaou.system.agent.AgentTool;
import com.xiaou.system.agent.AgentToolCall;
import com.xiaou.system.agent.AgentToolDefinition;
import com.xiaou.system.agent.AgentToolPreview;
import com.xiaou.system.agent.AgentToolResult;
import com.xiaou.system.dto.AgentChatArtifact;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 查询当前管理员智能体会话上下文的通用只读工具。
 *
 * @author xiaou
 */
@Component
@RequiredArgsConstructor
public class AgentSessionContextAgentTool implements AgentTool {

    private static final int TEXT_LIMIT = 500;

    private final AgentSessionProperties sessionProperties;

    @Override
    public AgentToolDefinition definition() {
        AgentToolDefinition definition = new AgentToolDefinition();
        definition.setName("system.agent.session.context");
        definition.setTitle("查询当前会话上下文");
        definition.setDescription("查询当前管理员智能体会话已加载的最近对话摘要，用于解释 planner 上下文来源。");
        definition.setIntent("system.agent.session.context");
        definition.setRoute("/admin/agent/chat");
        definition.setRiskLevel("readonly");
        definition.setRiskCategory("READONLY");
        definition.setInputSchema(Map.of());
        definition.setRequiredInputKeys(List.of());
        definition.setRequiredPermissions(List.of("agent:runtime:session:read"));
        return definition;
    }

    @Override
    public Optional<AgentToolCall> resolve(String message) {
        String normalized = normalize(message);
        if (!containsAny(normalized, List.of("你记住了什么", "记住了什么", "会话上下文", "当前上下文", "上下文是什么"))) {
            return Optional.empty();
        }

        AgentToolCall call = new AgentToolCall();
        call.setToolName(definition().getName());
        call.setSummary("查询当前管理员智能体会话上下文");
        call.setInput(new LinkedHashMap<>());
        return Optional.of(call);
    }

    @Override
    public AgentToolPreview preview(AgentToolCall call, AgentExecutionContext context) {
        AgentToolPreview preview = new AgentToolPreview();
        preview.setSummary("查询当前会话上下文是只读动作，不需要写入预览。");
        return preview;
    }

    @Override
    public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
        AgentSessionSnapshot snapshot = snapshot(context);
        List<AgentSessionTurn> turns = snapshot.getRecentTurns() == null ? List.of() : snapshot.getRecentTurns();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sessionId", normalize(snapshot.getSessionId()));
        data.put("recentTurnCount", turns.size());
        data.put("maxRecentTurns", Math.max(sessionProperties.getMaxRecentTurns(), 0));
        data.put("recentTurns", turns.stream().map(this::turnDescriptor).toList());

        AgentToolResult result = new AgentToolResult();
        result.setSuccess(true);
        result.setSummary("当前会话已保留 " + turns.size() + " 轮智能体上下文。");
        result.getArtifacts().add(new AgentChatArtifact(
                "agentSessionContext",
                "当前会话上下文",
                data
        ));
        result.getNextActions().add("如果上下文不符合预期，可以开启新的聊天会话重新描述任务。");
        return result;
    }

    private AgentSessionSnapshot snapshot(AgentExecutionContext context) {
        if (context == null || context.sessionSnapshot() == null) {
            return new AgentSessionSnapshot();
        }
        return context.sessionSnapshot();
    }

    private Map<String, Object> turnDescriptor(AgentSessionTurn turn) {
        Map<String, Object> item = new LinkedHashMap<>();
        if (turn == null) {
            return item;
        }
        item.put("message", limit(turn.getMessage()));
        item.put("status", normalize(turn.getStatus()));
        item.put("answer", limit(turn.getAnswer()));
        item.put("toolName", normalize(turn.getToolName()));
        item.put("auditId", normalize(turn.getAuditId()));
        item.put("traceId", normalize(turn.getTraceId()));
        return item;
    }

    private boolean containsAny(String value, List<String> keywords) {
        return keywords.stream().anyMatch(value::contains);
    }

    private String limit(String value) {
        String text = normalize(value);
        if (text.length() <= TEXT_LIMIT) {
            return text;
        }
        return text.substring(0, TEXT_LIMIT);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
