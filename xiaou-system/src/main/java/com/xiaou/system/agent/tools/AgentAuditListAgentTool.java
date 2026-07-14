package com.xiaou.system.agent.tools;

import com.xiaou.common.core.domain.PageResult;
import com.xiaou.system.agent.AgentExecutionContext;
import com.xiaou.system.agent.AgentTool;
import com.xiaou.system.agent.AgentToolCall;
import com.xiaou.system.agent.AgentToolDefinition;
import com.xiaou.system.agent.AgentToolPreview;
import com.xiaou.system.agent.AgentToolResult;
import com.xiaou.system.dto.AgentAuditQueryRequest;
import com.xiaou.system.dto.AgentAuditResponse;
import com.xiaou.system.dto.AgentChatArtifact;
import com.xiaou.system.service.SysAgentAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 查询管理员智能体审计记录的通用只读工具。
 *
 * @author xiaou
 */
@Component
@RequiredArgsConstructor
public class AgentAuditListAgentTool implements AgentTool {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d+)");
    private static final Map<String, String> STATUS_KEYWORDS = Map.of(
            "待确认", "PREVIEW",
            "已确认", "CONFIRMED",
            "已取消", "CANCELLED",
            "已执行", "EXECUTED",
            "成功", "EXECUTED",
            "失败", "FAILED"
    );

    private final SysAgentAuditService auditService;

    @Override
    public AgentToolDefinition definition() {
        AgentToolDefinition definition = new AgentToolDefinition();
        definition.setName("system.agent.audit.list");
        definition.setTitle("查询智能体审计记录");
        definition.setDescription("分页查询管理员智能体最近审计记录，支持按状态和工具名过滤。");
        definition.setIntent("system.agent.audit.list");
        definition.setRoute("/admin/agent/chat");
        definition.setRiskLevel("readonly");
        definition.setRiskCategory("READONLY");
        definition.setInputSchema(Map.of(
                "pageNum", Map.of("type", "integer", "minimum", 1, "default", 1),
                "pageSize", Map.of("type", "integer", "minimum", 1, "maximum", 20, "default", 10),
                "status", Map.of(
                        "type", "string",
                        "enum", List.of("PREVIEW", "CONFIRMED", "CANCELLED", "EXECUTED", "FAILED")
                ),
                "actionId", Map.of("type", "string", "maxLength", 160)
        ));
        definition.setRequiredInputKeys(List.of());
        definition.setRequiredPermissions(List.of("agent:runtime:audit:read"));
        return definition;
    }

    @Override
    public Optional<AgentToolCall> resolve(String message) {
        String normalized = normalize(message);
        if (containsAny(normalized, List.of("审计详情", "记录详情", "执行详情", "确认详情"))) {
            return Optional.empty();
        }
        if (!containsAny(normalized, List.of("智能体审计", "智能体记录", "agent审计", "agent记录", "执行记录", "待确认记录", "失败记录"))) {
            return Optional.empty();
        }

        Map<String, Object> input = new LinkedHashMap<>();
        input.put("pageNum", 1);
        input.put("pageSize", clamp(firstNumber(normalized, 10), 1, 20));
        String status = resolveStatus(normalized);
        if (StringUtils.hasText(status)) {
            input.put("status", status);
        }

        AgentToolCall call = new AgentToolCall();
        call.setToolName(definition().getName());
        call.setSummary("查询管理员智能体审计记录");
        call.setInput(input);
        return Optional.of(call);
    }

    @Override
    public AgentToolPreview preview(AgentToolCall call, AgentExecutionContext context) {
        AgentToolPreview preview = new AgentToolPreview();
        preview.setSummary("查询智能体审计记录是只读动作，不需要写入预览。");
        return preview;
    }

    @Override
    public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
        AgentAuditQueryRequest query = new AgentAuditQueryRequest();
        query.setPageNum(clamp(intValue(call.getInput().get("pageNum"), 1), 1, Integer.MAX_VALUE));
        query.setPageSize(clamp(intValue(call.getInput().get("pageSize"), 10), 1, 20));
        query.setStatus(stringValue(call.getInput().get("status")));
        query.setActionId(stringValue(call.getInput().get("actionId")));

        PageResult<AgentAuditResponse> page = auditService.getAuditPage(query);
        List<Map<String, Object>> records = page.getRecords() == null ? List.of() : page.getRecords().stream()
                .map(this::toSummary)
                .toList();

        AgentToolResult result = new AgentToolResult();
        result.setSuccess(true);
        result.setSummary(records.isEmpty()
                ? "没有查询到匹配的智能体审计记录。"
                : "已查询到最近 " + records.size() + " 条智能体审计记录。");
        result.getArtifacts().add(new AgentChatArtifact(
                "agentAuditList",
                "智能体审计记录",
                Map.of(
                        "pageNum", page.getPageNum() == null ? query.getPageNum() : page.getPageNum(),
                        "pageSize", page.getPageSize() == null ? query.getPageSize() : page.getPageSize(),
                        "total", page.getTotal() == null ? records.size() : page.getTotal(),
                        "records", records
                )
        ));
        result.getNextActions().add("可以继续按状态过滤，例如：查询失败的智能体审计记录。");
        return result;
    }

    private Map<String, Object> toSummary(AgentAuditResponse audit) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("auditId", audit.getAuditId());
        item.put("actionId", audit.getActionId());
        item.put("intent", audit.getIntent());
        item.put("status", audit.getStatus());
        item.put("statusText", audit.getStatusText());
        item.put("riskCategory", audit.getRiskCategory());
        item.put("summary", audit.getSummary());
        item.put("operatorName", audit.getOperatorName());
        item.put("createdTime", audit.getCreatedTime());
        item.put("executedTime", audit.getExecutedTime());
        item.put("errorMessage", audit.getErrorMessage());
        return item;
    }

    private String resolveStatus(String message) {
        return STATUS_KEYWORDS.entrySet().stream()
                .filter(entry -> message.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse("");
    }

    private int firstNumber(String message, int fallback) {
        Matcher matcher = NUMBER_PATTERN.matcher(message);
        if (!matcher.find()) {
            return fallback;
        }
        return Integer.parseInt(matcher.group(1));
    }

    private int intValue(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return fallback;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private boolean containsAny(String value, List<String> keywords) {
        return keywords.stream().anyMatch(value::contains);
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
