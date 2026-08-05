package com.xiaou.system.agent.tools;

import com.xiaou.system.agent.AbstractReadonlyAgentTool;
import com.xiaou.system.agent.AgentExecutionContext;
import com.xiaou.system.agent.AgentToolCall;
import com.xiaou.system.agent.AgentToolDefinitionBuilder;
import com.xiaou.system.agent.AgentToolResult;
import com.xiaou.sre.dto.rca.SreRcaReport;
import com.xiaou.sre.service.rca.SreIncidentRcaService;
import com.xiaou.sre.service.rca.SreRcaTriggerSource;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 通过管理员智能体生成只读 SRE 根因分析报告。
 *
 * @author xiaou
 */
@Component
public class SreIncidentRcaAgentTool extends AbstractReadonlyAgentTool {

    private static final Pattern INCIDENT_ID_PATTERN = Pattern.compile(
            "(?i)(?:事故|incident)\\s*(?:编号|id|#|:|：)?\\s*(\\d+)");
    private static final Pattern RCA_ID_PATTERN = Pattern.compile("(?i)\\brca\\s*(?:#|:)?\\s*(\\d+)");

    private final SreIncidentRcaService rcaService;

    public SreIncidentRcaAgentTool(SreIncidentRcaService rcaService) {
        super(AgentToolDefinitionBuilder.readonly("sre.incident.rca", "调查 SRE 事故根因")
                .description("读取受限事故上下文并生成带证据引用的只读 RCA 报告；不会执行任何修复动作。")
                .route("/admin/sre/incidents/{incidentId}/rca")
                .input("incidentId", Map.of(
                        "type", "integer",
                        "minimum", 1,
                        "description", "SRE 事故 ID"
                ), true)
                .permission("agent:sre:incident:read")
                .build());
        this.rcaService = rcaService;
    }

    @Override
    public Optional<AgentToolCall> resolve(String message) {
        String normalized = normalize(message).toLowerCase(Locale.ROOT);
        if (!containsAny(normalized, List.of("分析", "调查", "诊断", "根因", "rca", "为什么"))) {
            return Optional.empty();
        }
        Long incidentId = extractId(normalized);
        if (incidentId == null) {
            return Optional.empty();
        }
        return Optional.of(call("调查事故 " + incidentId + " 的根因", Map.of("incidentId", incidentId)));
    }

    @Override
    public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
        Long incidentId = positiveLong(call == null || call.getInput() == null
                ? null
                : call.getInput().get("incidentId"));
        if (incidentId == null) {
            return failure("事故 ID 不合法", "请提供大于 0 的 SRE 事故 ID 后重试。");
        }

        Long requestedBy = context == null || context.operator() == null ? null : context.operator().id();
        Optional<SreRcaReport> reportOptional = rcaService.investigate(
                incidentId, SreRcaTriggerSource.AGENT_TOOL, requestedBy);
        if (reportOptional.isEmpty()) {
            return failure("事故不存在", "请先确认事故 ID，可通过事故列表查询后重试。");
        }

        SreRcaReport report = reportOptional.get();
        Map<String, Object> data = reportData(report);
        String summary = "FALLBACK".equals(report.generationMode())
                ? "事故 " + report.incidentNo() + " 的证据已整理，但当前证据不足以生成可靠自动根因；未执行任何动作。"
                : "已完成事故 " + report.incidentNo() + " 的只读根因分析，共引用 "
                + report.evidenceReferences().size() + " 条证据；未执行任何动作。";
        List<String> nextActions = "INSUFFICIENT_EVIDENCE".equals(report.conclusionStatus())
                ? List.of("请人工复核报告中的证据限制，并补充缺失的指标、日志或发布记录。")
                : List.of("请人工复核报告中的假设和建议；所有处置动作仍需要独立审批。");
        return success(summary, artifact("sreIncidentRca", "SRE 事故根因分析", data), nextActions);
    }

    private Map<String, Object> reportData(SreRcaReport report) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("incidentId", report.incidentId());
        data.put("incidentNo", report.incidentNo());
        data.put("generationMode", report.generationMode());
        data.put("conclusionStatus", report.conclusionStatus());
        data.put("severityAssessment", report.severityAssessment());
        data.put("executiveSummary", report.executiveSummary());
        data.put("observations", report.observations());
        data.put("hypotheses", report.hypotheses());
        data.put("recommendedNextSteps", report.recommendedNextSteps());
        data.put("evidenceReferences", report.evidenceReferences());
        data.put("limitations", report.limitations());
        data.put("contextTruncated", report.contextTruncated());
        data.put("executionAllowed", false);
        data.put("generatedAt", report.generatedAt());
        return data;
    }

    private AgentToolResult failure(String message, String nextAction) {
        AgentToolResult result = new AgentToolResult();
        result.setSuccess(false);
        result.setSummary(message);
        result.setErrorMessage(message);
        result.getNextActions().add(nextAction);
        return result;
    }

    private Long extractId(String message) {
        Matcher incidentMatcher = INCIDENT_ID_PATTERN.matcher(message);
        if (incidentMatcher.find()) {
            return positiveLong(incidentMatcher.group(1));
        }
        Matcher rcaMatcher = RCA_ID_PATTERN.matcher(message);
        return rcaMatcher.find() ? positiveLong(rcaMatcher.group(1)) : null;
    }

    private Long positiveLong(Object value) {
        if (value == null) {
            return null;
        }
        try {
            long parsed = value instanceof Number number
                    ? number.longValue()
                    : Long.parseLong(String.valueOf(value).trim());
            return parsed > 0 ? parsed : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
