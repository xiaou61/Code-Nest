package com.xiaou.ai.sre;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.xiaou.ai.prompt.sre.SreRcaPromptSpecs;
import com.xiaou.ai.structured.AiStructuredOutputValidator;
import com.xiaou.ai.structured.sre.SreRcaStructuredOutputSpecs;
import com.xiaou.ai.support.AiExecutionResult;
import com.xiaou.ai.support.AiExecutionSupport;
import com.xiaou.ai.util.AiJsonResponseParser;
import com.xiaou.sre.dto.rca.SreRcaReport;
import com.xiaou.sre.service.rca.SreModelExecution;
import com.xiaou.sre.service.rca.SreRcaAnalysisInput;
import com.xiaou.sre.service.rca.SreRcaAnalyzer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * RCA 结构化输出解析、证据归属校验与确定性降级实现。
 *
 * @author xiaou
 */
@Service
@RequiredArgsConstructor
public class SreRcaAnalyzerImpl implements SreRcaAnalyzer {

    private static final String SCENE = "sre.incident.rca";
    private static final int MAX_CONTEXT_LENGTH = 60_000;
    private static final int MAX_EVIDENCE = 12;
    private static final int MAX_OBSERVATIONS = 8;
    private static final int MAX_HYPOTHESES = 5;
    private static final int MAX_RECOMMENDATIONS = 5;
    private static final int MAX_LIMITATIONS = 10;

    private static final Pattern BEARER_PATTERN = Pattern.compile(
            "(?i)\\bBearer\\s+[A-Za-z0-9._~+/=-]{6,}");
    private static final Pattern INLINE_SECRET_PATTERN = Pattern.compile(
            "(?i)\\b(authorization|password|passwd|token|secret|api[-_ ]?key|cookie|credential)"
                    + "\\b\\s*[:=]\\s*([^\\s,;]+)");
    private static final Pattern STANDALONE_CREDENTIAL_PATTERN = Pattern.compile(
            "(?i)\\b(?:sk-[A-Za-z0-9_-]{16,}|gh[pousr]_[A-Za-z0-9]{20,}|AKIA[0-9A-Z]{16})\\b");
    private static final Pattern CONTROL_PATTERN = Pattern.compile("[\\p{Cntrl}&&[^\\r\\n\\t]]");

    private final AiExecutionSupport aiExecutionSupport;

    @Override
    public SreModelExecution<SreRcaReport> analyze(SreRcaAnalysisInput input) {
        validateInput(input);
        Set<Long> allowedEvidenceIds = input.evidenceReferences().stream()
                .map(SreRcaReport.EvidenceReference::id)
                .filter(id -> id != null && id > 0)
                .limit(MAX_EVIDENCE)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        AiExecutionResult<SreRcaReport> execution = aiExecutionSupport.chatWithFallbackResult(
                SCENE,
                SreRcaPromptSpecs.INVESTIGATE,
                Map.of("incidentContextJson", input.contextJson()),
                response -> parseReport(response, input, allowedEvidenceIds),
                () -> fallback(input)
        );
        return new SreModelExecution<>(
                execution.value(),
                execution.outcome(),
                execution.provider(),
                execution.configuredModel(),
                execution.actualModel()
        );
    }

    @Override
    public SreRcaReport fallback(SreRcaAnalysisInput input) {
        if (input == null || input.incidentId() == null || input.incidentId() <= 0) {
            throw new IllegalArgumentException("SRE RCA 冻结输入不合法");
        }
        return fallbackReport(input);
    }

    @Override
    public String promptId() {
        return SreRcaPromptSpecs.INVESTIGATE.promptId();
    }

    @Override
    public String schemaId() {
        return SreRcaStructuredOutputSpecs.REPORT.schemaId();
    }

    private void validateInput(SreRcaAnalysisInput input) {
        if (input == null
                || input.incidentId() == null
                || input.incidentId() <= 0
                || !StringUtils.hasText(input.contextJson())
                || input.contextJson().length() > MAX_CONTEXT_LENGTH) {
            throw new IllegalArgumentException("SRE RCA 冻结输入不合法");
        }
    }

    private SreRcaReport parseReport(String response,
                                     SreRcaAnalysisInput input,
                                     Set<Long> allowedEvidenceIds) {
        JSONObject json = AiJsonResponseParser.parse(response);
        AiStructuredOutputValidator.ValidationResult validation =
                SreRcaStructuredOutputSpecs.REPORT.validateObject(json);
        if (!validation.valid()) {
            throw new IllegalArgumentException("SRE RCA 输出不符合结构化契约: " + validation.reason());
        }

        ParseState state = new ParseState();
        List<SreRcaReport.Observation> observations = parseObjects(
                json.getJSONArray("observations"),
                MAX_OBSERVATIONS,
                item -> observation(item, allowedEvidenceIds, state)
        ).stream().filter(java.util.Objects::nonNull).toList();
        List<SreRcaReport.Hypothesis> hypotheses = parseObjects(
                json.getJSONArray("hypotheses"),
                MAX_HYPOTHESES,
                item -> hypothesis(item, allowedEvidenceIds, state)
        );
        List<SreRcaReport.RecommendedNextStep> recommendations = parseObjects(
                json.getJSONArray("recommendedNextSteps"),
                MAX_RECOMMENDATIONS,
                item -> recommendation(item, allowedEvidenceIds, state)
        );
        List<String> limitations = stringList(json.getJSONArray("limitations"), MAX_LIMITATIONS, 500);
        if (state.invalidReference) {
            limitations = appendDistinct(limitations, "模型输出包含无有效证据引用，已由后端移除或降级。",
                    MAX_LIMITATIONS);
        }
        if (input.contextTruncated()) {
            limitations = appendDistinct(limitations, "输入模型的事故上下文已按安全上限裁剪。",
                    MAX_LIMITATIONS);
        }

        return new SreRcaReport(
                input.incidentId(),
                safeText(input.incidentNo(), 64, "UNKNOWN"),
                "AI",
                normalizeConclusion(json.getStr("conclusionStatus"), observations, hypotheses),
                json.getStr("severityAssessment"),
                safeText(json.getStr("executiveSummary"), 2_000, "未生成有效摘要。"),
                observations,
                hypotheses,
                recommendations,
                boundedReferences(input.evidenceReferences()),
                limitations,
                input.contextTruncated(),
                false,
                LocalDateTime.now()
        );
    }

    private SreRcaReport.Observation observation(JSONObject item,
                                                 Set<Long> allowedEvidenceIds,
                                                 ParseState state) {
        List<Long> evidenceIds = evidenceIds(item.getJSONArray("evidenceIds"), allowedEvidenceIds, state);
        if (evidenceIds.isEmpty()) {
            return null;
        }
        return new SreRcaReport.Observation(
                safeText(item.getStr("statement"), 1_000, "未提供观察描述。"),
                evidenceIds
        );
    }

    private SreRcaReport.Hypothesis hypothesis(JSONObject item,
                                               Set<Long> allowedEvidenceIds,
                                               ParseState state) {
        List<Long> evidenceIds = evidenceIds(item.getJSONArray("evidenceIds"), allowedEvidenceIds, state);
        List<Long> counterEvidenceIds = evidenceIds(
                item.getJSONArray("counterEvidenceIds"), allowedEvidenceIds, state);
        boolean supported = !evidenceIds.isEmpty();
        Double rawConfidence = item.getDouble("confidence", 0D);
        double confidence = rawConfidence == null ? 0D : Math.max(0D, Math.min(1D, rawConfidence));
        if (!supported) {
            confidence = Math.min(confidence, 0.2D);
        }
        return new SreRcaReport.Hypothesis(
                safeText(item.getStr("title"), 200, "未命名假设"),
                safeText(item.getStr("reasoning"), 2_000, "证据不足。"),
                confidence,
                supported ? "SUPPORTED" : "INSUFFICIENT",
                evidenceIds,
                counterEvidenceIds,
                stringList(item.getJSONArray("nextChecks"), 5, 500)
        );
    }

    private SreRcaReport.RecommendedNextStep recommendation(JSONObject item,
                                                            Set<Long> allowedEvidenceIds,
                                                            ParseState state) {
        return new SreRcaReport.RecommendedNextStep(
                safeText(item.getStr("description"), 1_000, "人工复核事故证据。"),
                item.getStr("risk"),
                evidenceIds(item.getJSONArray("evidenceIds"), allowedEvidenceIds, state)
        );
    }

    private String normalizeConclusion(String requested,
                                       List<SreRcaReport.Observation> observations,
                                       List<SreRcaReport.Hypothesis> hypotheses) {
        long supportedHypotheses = hypotheses.stream()
                .filter(item -> "SUPPORTED".equals(item.evidenceStatus()))
                .count();
        if (observations.isEmpty() && supportedHypotheses == 0) {
            return "INSUFFICIENT_EVIDENCE";
        }
        if (hypotheses.isEmpty()) {
            return "PARTIAL";
        }
        if (supportedHypotheses < hypotheses.size() && "SUPPORTED".equals(requested)) {
            return "PARTIAL";
        }
        return requested;
    }

    private SreRcaReport fallbackReport(SreRcaAnalysisInput input) {
        List<SreRcaReport.EvidenceReference> references = boundedReferences(input.evidenceReferences());
        List<SreRcaReport.Observation> observations = references.stream()
                .limit(MAX_OBSERVATIONS)
                .map(item -> new SreRcaReport.Observation(
                        "已采集 " + safeText(item.sourceType(), 64, "UNKNOWN")
                                + " 证据，状态为 " + safeText(item.status(), 32, "UNKNOWN") + "。",
                        List.of(item.id())
                ))
                .toList();
        List<Long> referenceIds = references.stream()
                .map(SreRcaReport.EvidenceReference::id)
                .filter(java.util.Objects::nonNull)
                .limit(5)
                .toList();
        List<String> limitations = new ArrayList<>(List.of(
                "AI 运行时不可用、超时或输出不符合结构化契约。",
                "当前仅返回已采集事实，未形成自动根因结论。"
        ));
        if (input.contextTruncated()) {
            limitations.add("事故上下文已按安全上限裁剪。");
        }
        String incidentNo = safeText(input.incidentNo(), 64, "UNKNOWN");
        return new SreRcaReport(
                input.incidentId(),
                incidentNo,
                "FALLBACK",
                "INSUFFICIENT_EVIDENCE",
                severity(input.severity()),
                "事故 " + incidentNo + " 已收集 " + references.size()
                        + " 条可引用证据；当前证据不足以生成可靠自动根因结论。",
                observations,
                List.of(),
                List.of(new SreRcaReport.RecommendedNextStep(
                        "由管理员人工复核现有事故证据和监控时间窗口。",
                        "READ_ONLY",
                        referenceIds
                )),
                references,
                limitations,
                input.contextTruncated(),
                false,
                LocalDateTime.now()
        );
    }

    private List<SreRcaReport.EvidenceReference> boundedReferences(
            List<SreRcaReport.EvidenceReference> references) {
        if (references == null) {
            return List.of();
        }
        return references.stream()
                .filter(java.util.Objects::nonNull)
                .filter(item -> item.id() != null && item.id() > 0)
                .limit(MAX_EVIDENCE)
                .toList();
    }

    private List<Long> evidenceIds(JSONArray array, Set<Long> allowed, ParseState state) {
        LinkedHashSet<Long> result = new LinkedHashSet<>();
        if (array == null) {
            state.invalidReference = true;
            return List.of();
        }
        for (int i = 0; i < array.size() && result.size() < MAX_EVIDENCE; i++) {
            try {
                Long id = Long.valueOf(String.valueOf(array.get(i)).trim());
                if (allowed.contains(id)) {
                    result.add(id);
                } else {
                    state.invalidReference = true;
                }
            } catch (RuntimeException ignored) {
                state.invalidReference = true;
            }
        }
        return List.copyOf(result);
    }

    private List<String> stringList(JSONArray array, int maxItems, int maxLength) {
        if (array == null) {
            return List.of();
        }
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (int i = 0; i < array.size() && result.size() < maxItems; i++) {
            String value = safeText(String.valueOf(array.get(i)), maxLength, "");
            if (StringUtils.hasText(value)) {
                result.add(value);
            }
        }
        return List.copyOf(result);
    }

    private <T> List<T> parseObjects(JSONArray array, int maxItems, Function<JSONObject, T> mapper) {
        List<T> result = new ArrayList<>();
        for (int i = 0; array != null && i < array.size() && result.size() < maxItems; i++) {
            JSONObject item = array.getJSONObject(i);
            if (item != null) {
                result.add(mapper.apply(item));
            }
        }
        return result;
    }

    private List<String> appendDistinct(List<String> values, String value, int maxItems) {
        LinkedHashSet<String> result = new LinkedHashSet<>(values);
        result.add(value);
        return result.stream().limit(maxItems).toList();
    }

    private String safeText(String value, int maxLength, String fallback) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        String sanitized = CONTROL_PATTERN.matcher(value).replaceAll("").trim();
        sanitized = BEARER_PATTERN.matcher(sanitized).replaceAll("Bearer [REDACTED]");
        sanitized = INLINE_SECRET_PATTERN.matcher(sanitized).replaceAll("$1=[REDACTED]");
        sanitized = STANDALONE_CREDENTIAL_PATTERN.matcher(sanitized).replaceAll("[REDACTED]");
        return sanitized.length() <= maxLength ? sanitized : sanitized.substring(0, maxLength);
    }

    private String severity(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "critical" -> "CRITICAL";
            case "high" -> "HIGH";
            case "warning", "medium" -> "MEDIUM";
            case "low", "info" -> "LOW";
            default -> "UNKNOWN";
        };
    }

    private static final class ParseState {
        private boolean invalidReference;
    }
}
