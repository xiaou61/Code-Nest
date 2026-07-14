package com.xiaou.system.agent.tools;

import com.xiaou.common.core.domain.PageResult;
import com.xiaou.system.agent.AbstractReadonlyAgentTool;
import com.xiaou.system.agent.AgentExecutionContext;
import com.xiaou.system.agent.AgentToolCall;
import com.xiaou.system.agent.AgentToolDefinitionBuilder;
import com.xiaou.system.agent.AgentToolResult;
import com.xiaou.system.dto.OperationLogQueryRequest;
import com.xiaou.system.dto.OperationLogResponse;
import com.xiaou.system.service.SysOperationLogService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 查询操作日志工具。
 *
 * @author xiaou
 */
@Component
public class OperationLogListAgentTool extends AbstractReadonlyAgentTool {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d+)");

    private final SysOperationLogService operationLogService;

    public OperationLogListAgentTool(SysOperationLogService operationLogService) {
        super(AgentToolDefinitionBuilder.readonly("system.operationLog.list", "查询操作日志")
                .description("分页查询最近操作日志，返回结构化摘要。")
                .route("/logs/operation")
                .input("pageNum", Map.of("type", "integer", "default", 1), true)
                .input("pageSize", Map.of("type", "integer", "minimum", 1, "maximum", 20), true)
                .permission("agent:system:operation-log:read")
                .build());
        this.operationLogService = operationLogService;
    }

    @Override
    public Optional<AgentToolCall> resolve(String message) {
        String normalized = normalize(message);
        if (!normalized.contains("操作日志")) {
            return Optional.empty();
        }
        if (!(normalized.contains("查") || normalized.contains("看") || normalized.contains("最近") || normalized.contains("列表"))) {
            return Optional.empty();
        }

        int limit = clamp(firstNumber(normalized, 5), 1, 20);
        return Optional.of(call("查询最近 " + limit + " 条操作日志", new LinkedHashMap<>(Map.of(
                "pageNum", 1,
                "pageSize", limit
        ))));
    }

    @Override
    public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
        int pageSize = intValue(call.getInput().get("pageSize"), 5);
        OperationLogQueryRequest query = new OperationLogQueryRequest();
        query.setPageNum(1);
        query.setPageSize(clamp(pageSize, 1, 20));
        PageResult<OperationLogResponse> page = operationLogService.getOperationLogPage(query);
        List<Map<String, Object>> records = page.getRecords() == null ? List.of() : page.getRecords().stream()
                .map(this::toSummary)
                .toList();

        return success(records.isEmpty()
                ? "没有查询到操作日志。"
                : "已查询到最近 " + records.size() + " 条操作日志。",
                artifact("operationLogList", "最近操作日志", Map.of(
                        "total", page.getTotal() == null ? records.size() : page.getTotal(),
                        "records", records
                )),
                List.of("如果需要清理旧日志，请先说明保留天数，例如：清理30天前操作日志。"));
    }

    private Map<String, Object> toSummary(OperationLogResponse log) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", log.getId());
        item.put("module", log.getModule());
        item.put("operationType", log.getOperationType());
        item.put("description", log.getDescription());
        item.put("operatorName", log.getOperatorName());
        item.put("statusText", log.getStatusText());
        item.put("operationTime", log.getOperationTime());
        return item;
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

}
