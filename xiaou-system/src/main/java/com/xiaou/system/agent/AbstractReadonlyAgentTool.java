package com.xiaou.system.agent;

import com.xiaou.system.dto.AgentChatArtifact;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 只读 AgentTool 通用基类，统一 definition、preview 和结果包装。
 *
 * @author xiaou
 */
public abstract class AbstractReadonlyAgentTool implements AgentTool {

    private final AgentToolDefinition definition;

    protected AbstractReadonlyAgentTool(AgentToolDefinition definition) {
        if (!isReadonly(definition)) {
            throw new IllegalArgumentException("AbstractReadonlyAgentTool only accepts readonly definitions");
        }
        this.definition = definition;
    }

    @Override
    public final AgentToolDefinition definition() {
        return definition;
    }

    @Override
    public AgentToolPreview preview(AgentToolCall call, AgentExecutionContext context) {
        AgentToolPreview preview = new AgentToolPreview();
        preview.setSummary(readonlyPreviewSummary());
        return preview;
    }

    protected AgentToolCall call(String summary, Map<String, Object> input) {
        AgentToolCall call = new AgentToolCall();
        call.setToolName(definition.getName());
        call.setSummary(summary);
        call.setInput(input == null ? new LinkedHashMap<>() : new LinkedHashMap<>(input));
        return call;
    }

    protected AgentChatArtifact artifact(String type, String title, Map<String, Object> data) {
        return new AgentChatArtifact(type, title, data == null ? Map.of() : data);
    }

    protected AgentToolResult success(String summary, AgentChatArtifact artifact, List<String> nextActions) {
        return success(summary, artifact == null ? List.of() : List.of(artifact), nextActions);
    }

    protected AgentToolResult success(String summary, List<AgentChatArtifact> artifacts, List<String> nextActions) {
        AgentToolResult result = new AgentToolResult();
        result.setSuccess(true);
        result.setSummary(summary);
        if (artifacts != null) {
            result.getArtifacts().addAll(artifacts);
        }
        if (nextActions != null) {
            result.getNextActions().addAll(nextActions);
        }
        return result;
    }

    protected boolean containsAny(String value, List<String> keywords) {
        String normalized = normalize(value);
        return keywords != null && keywords.stream().anyMatch(normalized::contains);
    }

    protected String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    protected String readonlyPreviewSummary() {
        return definition.getTitle() + "是只读动作，不需要写入预览。";
    }

    private boolean isReadonly(AgentToolDefinition definition) {
        return definition != null
                && "readonly".equalsIgnoreCase(normalize(definition.getRiskLevel()))
                && "READONLY".equalsIgnoreCase(normalize(definition.getRiskCategory()))
                && !definition.isDestructive()
                && !definition.isConfirmationRequired();
    }
}
