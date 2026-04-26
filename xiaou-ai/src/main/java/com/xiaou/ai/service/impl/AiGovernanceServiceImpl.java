package com.xiaou.ai.service.impl;

import com.xiaou.ai.dto.governance.AiGovernanceOverviewResponse;
import com.xiaou.ai.prompt.AiPromptCatalog;
import com.xiaou.ai.prompt.AiPromptSpec;
import com.xiaou.ai.prompt.AiRagQueryCatalog;
import com.xiaou.ai.rag.AiRagRetrievalCatalog;
import com.xiaou.ai.service.AiGovernanceService;
import com.xiaou.ai.structured.AiStructuredOutputCatalog;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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

    private static final Set<String> STRUCTURED_COVERED_PROMPT_IDS = AiStructuredOutputCatalog.all().stream()
            .map(item -> item.promptSpec().promptId())
            .collect(Collectors.toUnmodifiableSet());

    private static final Set<String> RAG_QUERY_IDS = AiRagQueryCatalog.all().stream()
            .map(item -> item.queryId())
            .collect(Collectors.toUnmodifiableSet());

    private static final Set<String> RAG_RETRIEVAL_IDS = AiRagRetrievalCatalog.all().stream()
            .map(item -> item.profileId())
            .collect(Collectors.toUnmodifiableSet());

    @Override
    public AiGovernanceOverviewResponse getOverview() {
        List<AiGovernanceOverviewResponse.WorkflowItem> workflows = AiPromptCatalog.all().stream()
                .map(this::buildWorkflowItem)
                .toList();

        int total = workflows.size();
        int configured = (int) workflows.stream().filter(AiGovernanceOverviewResponse.WorkflowItem::getConfigured).count();
        int fallbackCovered = (int) workflows.stream().filter(AiGovernanceOverviewResponse.WorkflowItem::getFallbackCovered).count();
        double coverageRate = total == 0 ? 0D : Math.round(fallbackCovered * 1000D / total) / 10D;
        boolean healthy = configured == total && coverageRate >= 90D;

        return new AiGovernanceOverviewResponse()
                .setTotalWorkflows(total)
                .setConfiguredWorkflows(configured)
                .setFallbackCoveredWorkflows(fallbackCovered)
                .setFallbackCoverageRate(coverageRate)
                .setHealthy(healthy)
                .setHealthLevel(healthy ? "STABLE" : "WATCH")
                .setSummary(buildSummary(total, configured, coverageRate))
                .setGeneratedAt(LocalDateTime.now())
                .setWorkflows(workflows)
                .setQualityRules(buildQualityRules())
                .setImprovementSuggestions(buildImprovementSuggestions(workflows));
    }

    private AiGovernanceOverviewResponse.WorkflowItem buildWorkflowItem(AiPromptSpec promptSpec) {
        boolean configured = !promptSpec.systemPrompt().isBlank() && !promptSpec.userTemplate().isBlank();
        boolean fallbackCovered = STRUCTURED_COVERED_PROMPT_IDS.contains(promptSpec.promptId());

        return new AiGovernanceOverviewResponse.WorkflowItem()
                .setCode(promptSpec.key())
                .setWorkflowId(promptSpec.promptId())
                .setWorkflowName(buildWorkflowName(promptSpec))
                .setDescription(buildDescription(promptSpec, fallbackCovered))
                .setDomain(resolveDomain(promptSpec.key()))
                .setConfigured(configured)
                .setFallbackCovered(fallbackCovered)
                .setRiskLevel(resolveRiskLevel(configured, fallbackCovered));
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

    private String buildDescription(AiPromptSpec promptSpec, boolean fallbackCovered) {
        String variables = promptSpec.templateVariables().isEmpty()
                ? "无变量"
                : String.join("、", promptSpec.templateVariables());
        String qualityGuard = fallbackCovered ? "已绑定结构化输出契约" : "待补充结构化输出契约";
        String ragStatus = RAG_QUERY_IDS.contains(promptSpec.promptId()) || RAG_RETRIEVAL_IDS.contains(promptSpec.promptId())
                ? "已配置 RAG 检索画像"
                : "未绑定 RAG 检索画像";
        return String.format("Prompt %s，变量：%s；%s；%s。", promptSpec.version(), variables, qualityGuard, ragStatus);
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

    private String buildSummary(int total, int configured, double coverageRate) {
        return String.format("当前登记 %d 个AI Runtime场景，已配置 %d 个，结构化兜底覆盖率 %.1f%%。", total, configured, coverageRate);
    }

    private List<String> buildQualityRules() {
        return List.of(
                "新增 AI 场景必须先登记 Prompt Catalog，并明确场景域、版本号和输入变量。",
                "结构化输出必须绑定 Schema 契约，解析失败时保留风险提示和本地兜底结果。",
                "需要知识库增强的场景必须登记 RAG Query 与 Retrieval Profile，便于观测召回质量。",
                "AI 输出仅作为辅助建议，关键业务状态推进仍由服务端规则校验。"
        );
    }

    private List<String> buildImprovementSuggestions(List<AiGovernanceOverviewResponse.WorkflowItem> workflows) {
        boolean hasMediumRisk = workflows.stream().anyMatch(item -> "MEDIUM".equals(item.getRiskLevel()));
        boolean hasHighRisk = workflows.stream().anyMatch(item -> "HIGH".equals(item.getRiskLevel()));
        if (hasHighRisk) {
            return List.of("存在未完整配置的 AI 场景，请优先补齐 Prompt 模板或下线对应入口。");
        }
        if (hasMediumRisk) {
            return List.of("存在未绑定结构化输出契约的 AI 场景，建议补充 Schema 校验或明确测试用途。");
        }
        return List.of("当前AI治理状态稳定，可继续补充调用成功率、平均耗时和fallback次数等运行指标。");
    }
}
