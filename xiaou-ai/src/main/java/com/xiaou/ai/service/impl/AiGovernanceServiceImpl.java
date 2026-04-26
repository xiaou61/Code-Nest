package com.xiaou.ai.service.impl;

import cn.hutool.core.util.StrUtil;
import com.xiaou.ai.dto.governance.AiGovernanceOverviewResponse;
import com.xiaou.ai.service.AiGovernanceService;
import com.xiaou.common.enums.CozeWorkflowEnum;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * AI治理服务实现
 *
 * @author xiaou
 */
@Service
public class AiGovernanceServiceImpl implements AiGovernanceService {

    private static final Set<CozeWorkflowEnum> FALLBACK_COVERED_WORKFLOWS = Set.of(
            CozeWorkflowEnum.COMMUNITY_POST_SUMMARY,
            CozeWorkflowEnum.MOCK_INTERVIEW_EVALUATE,
            CozeWorkflowEnum.MOCK_INTERVIEW_SUMMARY,
            CozeWorkflowEnum.MOCK_INTERVIEW_GENERATE_QUESTIONS,
            CozeWorkflowEnum.JOB_BATTLE_JD_PARSE,
            CozeWorkflowEnum.JOB_BATTLE_RESUME_MATCH,
            CozeWorkflowEnum.JOB_BATTLE_PLAN_GENERATE,
            CozeWorkflowEnum.JOB_BATTLE_INTERVIEW_REVIEW,
            CozeWorkflowEnum.SQL_OPTIMIZE_ANALYZE,
            CozeWorkflowEnum.SQL_OPTIMIZE_ANALYZE_V2,
            CozeWorkflowEnum.SQL_OPTIMIZE_REWRITE_V2,
            CozeWorkflowEnum.SQL_OPTIMIZE_COMPARE_V2
    );

    @Override
    public AiGovernanceOverviewResponse getOverview() {
        List<AiGovernanceOverviewResponse.WorkflowItem> workflows = Arrays.stream(CozeWorkflowEnum.values())
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

    private AiGovernanceOverviewResponse.WorkflowItem buildWorkflowItem(CozeWorkflowEnum workflow) {
        boolean configured = StrUtil.isNotBlank(workflow.getWorkflowId());
        boolean fallbackCovered = FALLBACK_COVERED_WORKFLOWS.contains(workflow);

        return new AiGovernanceOverviewResponse.WorkflowItem()
                .setCode(workflow.name())
                .setWorkflowId(maskWorkflowId(workflow.getWorkflowId()))
                .setWorkflowName(workflow.getWorkflowName())
                .setDescription(workflow.getDescription())
                .setDomain(resolveDomain(workflow))
                .setConfigured(configured)
                .setFallbackCovered(fallbackCovered)
                .setRiskLevel(resolveRiskLevel(configured, fallbackCovered));
    }

    private String resolveDomain(CozeWorkflowEnum workflow) {
        String code = workflow.name();
        if (code.startsWith("COMMUNITY")) {
            return "社区内容";
        }
        if (code.startsWith("MOCK_INTERVIEW")) {
            return "模拟面试";
        }
        if (code.startsWith("JOB_BATTLE")) {
            return "求职作战台";
        }
        if (code.startsWith("SQL_OPTIMIZE")) {
            return "慢SQL优化";
        }
        return "通用测试";
    }

    private String resolveRiskLevel(boolean configured, boolean fallbackCovered) {
        if (!configured) {
            return "HIGH";
        }
        return fallbackCovered ? "LOW" : "MEDIUM";
    }

    private String maskWorkflowId(String workflowId) {
        if (StrUtil.isBlank(workflowId) || workflowId.length() <= 8) {
            return workflowId;
        }
        return workflowId.substring(0, 4) + "****" + workflowId.substring(workflowId.length() - 4);
    }

    private String buildSummary(int total, int configured, double coverageRate) {
        return String.format("当前登记 %d 个AI工作流，已配置 %d 个，兜底覆盖率 %.1f%%。", total, configured, coverageRate);
    }

    private List<String> buildQualityRules() {
        return List.of(
                "工作流调用失败时必须返回可用的本地兜底结果，避免阻断核心业务流程。",
                "结构化解析失败时必须保留风险提示，前端可明确识别 fallback 状态。",
                "新增 Coze 工作流必须先登记到 CozeWorkflowEnum，再补充治理目录与兜底策略。",
                "AI 输出仅作为辅助建议，关键业务状态推进仍由服务端规则校验。"
        );
    }

    private List<String> buildImprovementSuggestions(List<AiGovernanceOverviewResponse.WorkflowItem> workflows) {
        boolean hasMediumRisk = workflows.stream().anyMatch(item -> "MEDIUM".equals(item.getRiskLevel()));
        boolean hasHighRisk = workflows.stream().anyMatch(item -> "HIGH".equals(item.getRiskLevel()));
        if (hasHighRisk) {
            return List.of("存在未配置工作流，请优先补齐工作流ID或下线对应入口。");
        }
        if (hasMediumRisk) {
            return List.of("存在未纳入兜底清单的工作流，建议补充本地降级结果或明确测试用途。");
        }
        return List.of("当前AI治理状态稳定，可继续补充调用成功率、平均耗时和fallback次数等运行指标。");
    }
}
