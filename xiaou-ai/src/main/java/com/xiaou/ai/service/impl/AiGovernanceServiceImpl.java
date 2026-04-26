package com.xiaou.ai.service.impl;

import com.xiaou.ai.dto.governance.AiGovernanceOverviewResponse;
import com.xiaou.ai.metrics.AiRuntimeMetricsCollector;
import com.xiaou.ai.metrics.AiRuntimeMetricsSnapshot;
import com.xiaou.ai.metrics.AiRuntimeOverviewMetrics;
import com.xiaou.ai.metrics.AiRuntimeSceneMetrics;
import com.xiaou.ai.prompt.AiPromptCatalog;
import com.xiaou.ai.prompt.AiPromptSpec;
import com.xiaou.ai.prompt.AiRagQueryCatalog;
import com.xiaou.ai.rag.AiRagRetrievalCatalog;
import com.xiaou.ai.service.AiGovernanceService;
import com.xiaou.ai.structured.AiStructuredOutputCatalog;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AI治理服务实现
 *
 * @author xiaou
 */
@Service
public class AiGovernanceServiceImpl implements AiGovernanceService {

    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final Set<String> STRUCTURED_COVERED_PROMPT_IDS = AiStructuredOutputCatalog.all().stream()
            .map(item -> item.promptSpec().promptId())
            .collect(Collectors.toUnmodifiableSet());

    private static final Set<String> RAG_QUERY_IDS = AiRagQueryCatalog.all().stream()
            .map(item -> item.queryId())
            .collect(Collectors.toUnmodifiableSet());

    private static final Set<String> RAG_RETRIEVAL_IDS = AiRagRetrievalCatalog.all().stream()
            .map(item -> item.profileId())
            .collect(Collectors.toUnmodifiableSet());

    private final AiRuntimeMetricsCollector runtimeMetricsCollector;

    public AiGovernanceServiceImpl(AiRuntimeMetricsCollector runtimeMetricsCollector) {
        this.runtimeMetricsCollector = runtimeMetricsCollector;
    }

    @Override
    public AiGovernanceOverviewResponse getOverview() {
        AiRuntimeMetricsSnapshot runtimeSnapshot = runtimeMetricsCollector.snapshot();
        Set<String> observedPromptKeys = runtimeSnapshot.getSceneStats().stream()
                .map(AiRuntimeSceneMetrics::getPromptKey)
                .collect(Collectors.toUnmodifiableSet());

        List<AiGovernanceOverviewResponse.WorkflowItem> workflows = AiPromptCatalog.all().stream()
                .map(item -> buildWorkflowItem(item, observedPromptKeys))
                .toList();

        int total = workflows.size();
        int configured = (int) workflows.stream().filter(AiGovernanceOverviewResponse.WorkflowItem::getConfigured).count();
        int fallbackCovered = (int) workflows.stream().filter(AiGovernanceOverviewResponse.WorkflowItem::getFallbackCovered).count();
        int schemaCovered = (int) workflows.stream().filter(AiGovernanceOverviewResponse.WorkflowItem::getSchemaCovered).count();
        int ragCovered = (int) workflows.stream().filter(AiGovernanceOverviewResponse.WorkflowItem::getRagCovered).count();
        int runtimeObserved = (int) workflows.stream().filter(AiGovernanceOverviewResponse.WorkflowItem::getRuntimeObserved).count();
        double coverageRate = total == 0 ? 0D : Math.round(fallbackCovered * 1000D / total) / 10D;
        AiGovernanceOverviewResponse.RuntimeInsight runtimeInsight = buildRuntimeInsight(runtimeSnapshot, runtimeObserved);
        List<AiGovernanceOverviewResponse.CoverageMatrixItem> coverageMatrix = buildCoverageMatrix(
                total,
                configured,
                schemaCovered,
                ragCovered,
                runtimeObserved
        );
        List<AiGovernanceOverviewResponse.RiskItem> riskItems = buildRiskItems(workflows, runtimeSnapshot);
        int qualityScore = buildQualityScore(total, configured, schemaCovered, ragCovered, runtimeObserved, runtimeInsight, riskItems);
        String qualityGrade = resolveQualityGrade(qualityScore);
        boolean hasHighRisk = riskItems.stream().anyMatch(item -> "HIGH".equals(item.getSeverity()));
        boolean healthy = configured == total && qualityScore >= 80 && !hasHighRisk;

        return new AiGovernanceOverviewResponse()
                .setTotalWorkflows(total)
                .setConfiguredWorkflows(configured)
                .setFallbackCoveredWorkflows(fallbackCovered)
                .setFallbackCoverageRate(coverageRate)
                .setHealthy(healthy)
                .setHealthLevel(healthy ? "STABLE" : "WATCH")
                .setQualityScore(qualityScore)
                .setQualityGrade(qualityGrade)
                .setSummary(buildSummary(total, configured, coverageRate, qualityScore, runtimeInsight))
                .setGeneratedAt(LocalDateTime.now())
                .setWorkflows(workflows)
                .setQualityRules(buildQualityRules())
                .setImprovementSuggestions(buildImprovementSuggestions(workflows, riskItems))
                .setRuntimeInsight(runtimeInsight)
                .setCoverageMatrix(coverageMatrix)
                .setRiskItems(riskItems);
    }

    private AiGovernanceOverviewResponse.WorkflowItem buildWorkflowItem(AiPromptSpec promptSpec, Set<String> observedPromptKeys) {
        boolean configured = !promptSpec.systemPrompt().isBlank() && !promptSpec.userTemplate().isBlank();
        boolean schemaCovered = STRUCTURED_COVERED_PROMPT_IDS.contains(promptSpec.promptId());
        boolean ragCovered = hasRagProfile(promptSpec);
        boolean runtimeObserved = observedPromptKeys.contains(promptSpec.key());

        return new AiGovernanceOverviewResponse.WorkflowItem()
                .setCode(promptSpec.key())
                .setWorkflowId(promptSpec.promptId())
                .setWorkflowName(buildWorkflowName(promptSpec))
                .setDescription(buildDescription(promptSpec, schemaCovered, ragCovered, runtimeObserved))
                .setDomain(resolveDomain(promptSpec.key()))
                .setConfigured(configured)
                .setFallbackCovered(schemaCovered)
                .setSchemaCovered(schemaCovered)
                .setRagCovered(ragCovered)
                .setRuntimeObserved(runtimeObserved)
                .setRiskLevel(resolveRiskLevel(configured, schemaCovered));
    }

    private String buildWorkflowName(AiPromptSpec promptSpec) {
        return switch (resolveDomain(promptSpec.key())) {
            case "社区内容" -> "社区摘要";
            case "模拟面试" -> buildShortName(promptSpec.key(), "mock_interview.");
            case "求职作战台" -> buildShortName(promptSpec.key(), "job_battle.");
            case "慢SQL优化" -> buildShortName(promptSpec.key(), "sql_optimize.");
            default -> promptSpec.key();
        };
    }

    private String buildShortName(String key, String prefix) {
        return key.replace(prefix, "").replace('_', ' ');
    }

    private String buildDescription(AiPromptSpec promptSpec, boolean schemaCovered, boolean ragCovered, boolean runtimeObserved) {
        String variables = promptSpec.templateVariables().isEmpty()
                ? "无变量"
                : String.join("、", promptSpec.templateVariables());
        String qualityGuard = schemaCovered ? "已绑定结构化输出契约" : "待补充结构化输出契约";
        String ragStatus = ragCovered ? "已配置 RAG 检索画像" : "未绑定 RAG 检索画像";
        String runtimeStatus = runtimeObserved ? "近期已有运行观测" : "近期暂无运行样本";
        return String.format("Prompt %s，变量：%s；%s；%s；%s。", promptSpec.version(), variables, qualityGuard, ragStatus, runtimeStatus);
    }

    private String resolveDomain(String code) {
        if (code.startsWith("community.")) {
            return "社区内容";
        }
        if (code.startsWith("mock_interview.")) {
            return "模拟面试";
        }
        if (code.startsWith("job_battle.")) {
            return "求职作战台";
        }
        if (code.startsWith("sql_optimize.")) {
            return "慢SQL优化";
        }
        return "通用场景";
    }

    private String resolveRiskLevel(boolean configured, boolean fallbackCovered) {
        if (!configured) {
            return "HIGH";
        }
        return fallbackCovered ? "LOW" : "MEDIUM";
    }

    private boolean hasRagProfile(AiPromptSpec promptSpec) {
        return RAG_QUERY_IDS.contains(promptSpec.promptId()) || RAG_RETRIEVAL_IDS.contains(promptSpec.promptId());
    }

    private AiGovernanceOverviewResponse.RuntimeInsight buildRuntimeInsight(
            AiRuntimeMetricsSnapshot snapshot,
            int runtimeObserved
    ) {
        AiRuntimeOverviewMetrics overview = snapshot.getOverview();
        long total = overview.getTotalInvocations();
        double successRate = calcRate(overview.getSuccessCount(), total);
        double errorRate = calcRate(overview.getErrorCount(), total);
        double fallbackRate = calcRate(overview.getFallbackCount(), total);
        double parseFailureRate = calcRate(overview.getStructuredParseFailureCount(), total);
        String statusText;
        if (total == 0) {
            statusText = "暂无运行样本";
        } else if (successRate >= 95D && errorRate <= 2D) {
            statusText = "运行稳定";
        } else if (successRate >= 85D) {
            statusText = "需要观察";
        } else {
            statusText = "需要治理";
        }

        return new AiGovernanceOverviewResponse.RuntimeInsight()
                .setTotalInvocations(total)
                .setSuccessRate(successRate)
                .setErrorRate(errorRate)
                .setFallbackRate(fallbackRate)
                .setStructuredParseFailureRate(parseFailureRate)
                .setAverageLatencyMs(overview.getAverageLatencyMs())
                .setObservedScenes(runtimeObserved)
                .setRecentCallCount(snapshot.getRecentCalls() == null ? 0 : snapshot.getRecentCalls().size())
                .setLastInvocationAt(formatTimestamp(overview.getLastInvocationAt()))
                .setStatusText(statusText);
    }

    private List<AiGovernanceOverviewResponse.CoverageMatrixItem> buildCoverageMatrix(
            int total,
            int configured,
            int schemaCovered,
            int ragCovered,
            int runtimeObserved
    ) {
        return List.of(
                buildCoverageItem("prompt", "Prompt 目录", configured, total, "Prompt Catalog 已登记且模板完整的场景比例"),
                buildCoverageItem("schema", "Schema 契约", schemaCovered, total, "结构化输出契约与解析兜底覆盖比例"),
                buildCoverageItem("rag", "RAG 画像", ragCovered, total, "Query 与 Retrieval Profile 覆盖比例"),
                buildCoverageItem("regression", "回归准备度", schemaCovered, total, "以结构化契约作为黄金样例回归准备度基线"),
                buildCoverageItem("runtime", "运行观测", runtimeObserved, total, "最近运行指标中可观测到的场景比例")
        );
    }

    private AiGovernanceOverviewResponse.CoverageMatrixItem buildCoverageItem(
            String key,
            String name,
            int covered,
            int total,
            String description
    ) {
        double rate = calcRate(covered, total);
        String status;
        if (rate >= 90D) {
            status = "STABLE";
        } else if (rate >= 60D) {
            status = "WATCH";
        } else {
            status = "RISK";
        }
        return new AiGovernanceOverviewResponse.CoverageMatrixItem()
                .setKey(key)
                .setName(name)
                .setCovered(covered)
                .setTotal(total)
                .setRate(rate)
                .setStatus(status)
                .setDescription(description);
    }

    private List<AiGovernanceOverviewResponse.RiskItem> buildRiskItems(
            List<AiGovernanceOverviewResponse.WorkflowItem> workflows,
            AiRuntimeMetricsSnapshot snapshot
    ) {
        List<AiGovernanceOverviewResponse.RiskItem> result = new ArrayList<>();
        for (AiGovernanceOverviewResponse.WorkflowItem workflow : workflows) {
            if (!Boolean.TRUE.equals(workflow.getConfigured())) {
                result.add(buildRisk("HIGH", workflow.getWorkflowName(), "Prompt", "Prompt 模板未完整配置。", "补齐系统提示词和用户模板，或临时下线入口。", 96));
            }
            if (!Boolean.TRUE.equals(workflow.getSchemaCovered())) {
                result.add(buildRisk("MEDIUM", workflow.getWorkflowName(), "Schema", "缺少结构化输出契约。", "补充 Schema 校验、解析失败提示和本地兜底结果。", 72));
            }
            if (!Boolean.TRUE.equals(workflow.getRagCovered())) {
                result.add(buildRisk("LOW", workflow.getWorkflowName(), "RAG", "暂未绑定 RAG 查询或检索画像。", "确认该场景是否需要知识库增强，需要则登记 Query 与 Retrieval Profile。", 38));
            }
            if (!Boolean.TRUE.equals(workflow.getRuntimeObserved())) {
                result.add(buildRisk("LOW", workflow.getWorkflowName(), "Runtime", "近期暂无运行观测样本。", "上线后通过 Prompt 试跑或真实业务调用补齐可观测样本。", 30));
            }
        }

        if (snapshot.getSceneStats() != null) {
            for (AiRuntimeSceneMetrics scene : snapshot.getSceneStats()) {
                long invocations = scene.getInvocations();
                if (invocations <= 0) {
                    continue;
                }
                double errorRate = calcRate(scene.getErrorCount(), invocations);
                double fallbackRate = calcRate(scene.getFallbackCount(), invocations);
                double parseFailureRate = calcRate(scene.getStructuredParseFailureCount(), invocations);
                if (errorRate >= 20D) {
                    result.add(buildRisk("HIGH", scene.getScene(), "Error Rate", String.format("运行失败率 %.1f%%。", errorRate), "优先查看最近调用错误和模型配置。", 92));
                } else if (errorRate >= 8D) {
                    result.add(buildRisk("MEDIUM", scene.getScene(), "Error Rate", String.format("运行失败率 %.1f%%。", errorRate), "补充失败样例并执行回归。", 68));
                }
                if (fallbackRate >= 20D) {
                    result.add(buildRisk("MEDIUM", scene.getScene(), "Fallback", String.format("兜底触发率 %.1f%%。", fallbackRate), "检查结构化解析、Prompt 输出约束和模型稳定性。", 64));
                }
                if (parseFailureRate > 0D) {
                    result.add(buildRisk("MEDIUM", scene.getScene(), "Schema Parse", String.format("结构化解析失败率 %.1f%%。", parseFailureRate), "收敛输出格式并增加解析失败样例。", 62));
                }
                if (scene.getAverageLatencyMs() >= 8000L) {
                    result.add(buildRisk("MEDIUM", scene.getScene(), "Latency", "平均耗时 " + scene.getAverageLatencyMs() + " ms。", "评估模型、上下文长度和 RAG 召回数量。", 58));
                }
            }
        }

        if (result.isEmpty()) {
            result.add(buildRisk("LOW", "AI Runtime", "Governance", "当前暂无明显质量风险。", "保持 Prompt、Schema、RAG 与回归样例同步更新。", 10));
        }

        return result.stream()
                .sorted(Comparator.comparingInt(AiGovernanceOverviewResponse.RiskItem::getScore).reversed())
                .limit(10)
                .toList();
    }

    private AiGovernanceOverviewResponse.RiskItem buildRisk(
            String severity,
            String scene,
            String metric,
            String reason,
            String suggestion,
            int score
    ) {
        return new AiGovernanceOverviewResponse.RiskItem()
                .setSeverity(severity)
                .setScene(scene)
                .setMetric(metric)
                .setReason(reason)
                .setSuggestion(suggestion)
                .setScore(score);
    }

    private int buildQualityScore(
            int total,
            int configured,
            int schemaCovered,
            int ragCovered,
            int runtimeObserved,
            AiGovernanceOverviewResponse.RuntimeInsight runtimeInsight,
            List<AiGovernanceOverviewResponse.RiskItem> riskItems
    ) {
        double promptScore = calcRate(configured, total) * 0.25D;
        double schemaScore = calcRate(schemaCovered, total) * 0.25D;
        double ragScore = calcRate(ragCovered, total) * 0.15D;
        double runtimeCoverageScore = calcRate(runtimeObserved, total) * 0.10D;
        double runtimeQualityScore = runtimeInsight.getTotalInvocations() == null || runtimeInsight.getTotalInvocations() == 0
                ? 82D
                : Math.max(0D, runtimeInsight.getSuccessRate() - runtimeInsight.getFallbackRate() * 0.35D - runtimeInsight.getStructuredParseFailureRate() * 0.45D);
        double runtimeScore = runtimeQualityScore * 0.25D;
        int riskPenalty = riskItems == null ? 0 : riskItems.stream()
                .mapToInt(item -> switch (item.getSeverity()) {
                    case "HIGH" -> 10;
                    case "MEDIUM" -> 5;
                    default -> 1;
                })
                .sum();
        return clamp((int) Math.round(promptScore + schemaScore + ragScore + runtimeCoverageScore + runtimeScore - Math.min(riskPenalty, 28)), 0, 100);
    }

    private String resolveQualityGrade(int qualityScore) {
        if (qualityScore >= 90) {
            return "A";
        }
        if (qualityScore >= 75) {
            return "B";
        }
        if (qualityScore >= 60) {
            return "C";
        }
        return "D";
    }

    private String buildSummary(
            int total,
            int configured,
            double coverageRate,
            int qualityScore,
            AiGovernanceOverviewResponse.RuntimeInsight runtimeInsight
    ) {
        return String.format(
                "当前登记 %d 个AI Runtime场景，已配置 %d 个，结构化兜底覆盖率 %.1f%%，质量分 %d，运行状态：%s。",
                total,
                configured,
                coverageRate,
                qualityScore,
                runtimeInsight.getStatusText()
        );
    }

    private List<String> buildQualityRules() {
        return List.of(
                "新增 AI 场景必须先登记 Prompt Catalog，并明确场景域、版本号和输入变量。",
                "结构化输出必须绑定 Schema 契约，解析失败时保留风险提示和本地兜底结果。",
                "需要知识库增强的场景必须登记 RAG Query 与 Retrieval Profile，便于观测召回质量。",
                "关键 AI 场景必须沉淀黄金样例，回归结果与线上运行指标需要在同一治理页面查看。",
                "AI 输出仅作为辅助建议，关键业务状态推进仍由服务端规则校验。"
        );
    }

    private List<String> buildImprovementSuggestions(
            List<AiGovernanceOverviewResponse.WorkflowItem> workflows,
            List<AiGovernanceOverviewResponse.RiskItem> riskItems
    ) {
        boolean hasMediumRisk = workflows.stream().anyMatch(item -> "MEDIUM".equals(item.getRiskLevel()));
        boolean hasHighRisk = workflows.stream().anyMatch(item -> "HIGH".equals(item.getRiskLevel()));
        boolean hasRuntimeRisk = riskItems.stream().anyMatch(item -> !"LOW".equals(item.getSeverity()) && !"Prompt".equals(item.getMetric()) && !"Schema".equals(item.getMetric()));
        if (hasHighRisk) {
            return List.of("存在未完整配置的 AI 场景，请优先补齐 Prompt 模板或下线对应入口。");
        }
        if (hasRuntimeRisk) {
            return List.of("运行指标出现异常，请优先查看风险队列中的失败率、兜底率或耗时指标。");
        }
        if (hasMediumRisk) {
            return List.of("存在未绑定结构化输出契约的 AI 场景，建议补充 Schema 校验或明确测试用途。");
        }
        return List.of("当前AI治理状态稳定，可继续补充黄金样例回归和线上运行样本。");
    }

    private double calcRate(long value, long total) {
        if (total <= 0) {
            return 0D;
        }
        return Math.round(value * 1000D / total) / 10D;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private String formatTimestamp(Long timestamp) {
        if (timestamp == null || timestamp <= 0) {
            return "-";
        }
        return DATETIME_FORMAT.format(Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()));
    }
}
