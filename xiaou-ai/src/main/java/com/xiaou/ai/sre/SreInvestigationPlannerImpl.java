package com.xiaou.ai.sre;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.xiaou.ai.prompt.sre.SreInvestigationPromptSpecs;
import com.xiaou.ai.structured.sre.SreInvestigationStructuredOutputSpecs;
import com.xiaou.ai.support.AiExecutionResult;
import com.xiaou.ai.support.AiExecutionSupport;
import com.xiaou.ai.util.AiJsonResponseParser;
import com.xiaou.sre.service.rca.SreInvestigationPlan;
import com.xiaou.sre.service.rca.SreInvestigationPlanner;
import com.xiaou.sre.service.rca.SreInvestigationPlanningInput;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 通过统一 AI 运行时生成计划，并由后端再次收敛到固定只读工具白名单。
 *
 * @author xiaou
 */
@Service
@RequiredArgsConstructor
public class SreInvestigationPlannerImpl implements SreInvestigationPlanner {

    private static final String SCENE = "sre.incident.investigation.plan";
    private static final int MAX_CONTEXT_LENGTH = 60_000;
    private static final int MAX_ROUNDS = 5;
    private static final int MAX_REASON_LENGTH = 500;

    private final AiExecutionSupport aiExecutionSupport;

    @Override
    public SreInvestigationPlan plan(SreInvestigationPlanningInput input) {
        validateInput(input);
        Set<String> allowed = normalizedAllowed(input.availableToolKeys());
        if (allowed.isEmpty()) {
            return new SreInvestigationPlan(
                    "STOP", "没有已启用的固定只读调查工具。", List.of(), "FALLBACK", "NO_AVAILABLE_TOOLS");
        }

        AiExecutionResult<SreInvestigationPlan> execution = aiExecutionSupport.chatWithFallbackResult(
                SCENE,
                SreInvestigationPromptSpecs.PLAN,
                Map.of(
                        "incidentContextJson", input.contextJson(),
                        "availableToolKeys", JSONUtil.toJsonStr(allowed)
                ),
                response -> parse(response, allowed),
                () -> fallback(input.fallbackToolKeys(), allowed)
        );
        SreInvestigationPlan value = execution.value() == null
                ? fallback(input.fallbackToolKeys(), allowed)
                : execution.value();
        String outcome = safeCode(execution.outcome(), "UNEXPECTED_FAILURE");
        String mode = "SUCCESS".equals(outcome) ? "AI" : "FALLBACK";
        return new SreInvestigationPlan(
                value.decision(), value.reason(), value.toolKeys(), mode, outcome);
    }

    private SreInvestigationPlan parse(String response, Set<String> allowed) {
        JSONObject json = AiJsonResponseParser.parse(response);
        var validation = SreInvestigationStructuredOutputSpecs.PLAN.validateObject(json);
        if (!validation.valid()) {
            throw new IllegalArgumentException("SRE 调查计划不符合结构化契约: " + validation.reason());
        }
        String decision = json.getStr("decision").trim().toUpperCase(Locale.ROOT);
        List<String> toolKeys = "STOP".equals(decision)
                ? List.of()
                : boundedAllowedTools(json.getJSONArray("toolKeys"), allowed);
        if ("INVESTIGATE".equals(decision) && toolKeys.isEmpty()) {
            return new SreInvestigationPlan(
                    "STOP", "计划未包含可执行的固定只读工具。", List.of(), null, null);
        }
        return new SreInvestigationPlan(
                decision,
                boundedReason(json.getStr("reason"), "未提供调查理由。"),
                toolKeys,
                null,
                null
        );
    }

    private SreInvestigationPlan fallback(List<String> requested, Set<String> allowed) {
        LinkedHashSet<String> tools = new LinkedHashSet<>();
        if (requested != null) {
            requested.stream()
                    .filter(StringUtils::hasText)
                    .map(value -> value.trim().toLowerCase(Locale.ROOT))
                    .filter(allowed::contains)
                    .limit(MAX_ROUNDS)
                    .forEach(tools::add);
        }
        if (tools.isEmpty()) {
            allowed.stream().limit(Math.min(3, MAX_ROUNDS)).forEach(tools::add);
        }
        return new SreInvestigationPlan(
                tools.isEmpty() ? "STOP" : "INVESTIGATE",
                tools.isEmpty() ? "没有可执行的固定只读工具。" : "使用确定性只读调查计划。",
                List.copyOf(tools),
                null,
                null
        );
    }

    private List<String> boundedAllowedTools(JSONArray array, Set<String> allowed) {
        if (array == null) {
            return List.of();
        }
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (int index = 0; index < array.size() && result.size() < MAX_ROUNDS; index++) {
            String toolKey = String.valueOf(array.get(index)).trim().toLowerCase(Locale.ROOT);
            if (allowed.contains(toolKey)) {
                result.add(toolKey);
            }
        }
        return List.copyOf(result);
    }

    private Set<String> normalizedAllowed(Set<String> values) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        values.stream()
                .filter(StringUtils::hasText)
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .filter(value -> value.matches("(?:prom|loki)_[a-z0-9_]{1,64}"))
                .forEach(result::add);
        return java.util.Collections.unmodifiableSet(result);
    }

    private void validateInput(SreInvestigationPlanningInput input) {
        if (input == null || !StringUtils.hasText(input.contextJson())
                || input.contextJson().length() > MAX_CONTEXT_LENGTH) {
            throw new IllegalArgumentException("SRE 调查计划上下文不合法");
        }
    }

    private String boundedReason(String value, String fallback) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        String normalized = value.replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", "").trim();
        return normalized.length() <= MAX_REASON_LENGTH
                ? normalized
                : normalized.substring(0, MAX_REASON_LENGTH);
    }

    private String safeCode(String value, String fallback) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return normalized.matches("[A-Z][A-Z0-9_]{0,63}") ? normalized : fallback;
    }
}
