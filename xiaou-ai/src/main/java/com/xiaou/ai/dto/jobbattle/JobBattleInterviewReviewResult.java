package com.xiaou.ai.dto.jobbattle;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 求职作战台-面试复盘结果
 *
 * @author xiaou
 */
@Data
@Accessors(chain = true)
public class JobBattleInterviewReviewResult {

    /**
     * 整体结论
     */
    private String overallConclusion;

    /**
     * 根因分析
     */
    private List<String> rootCauses;

    /**
     * 高影响修复动作
     */
    private List<HighImpactFix> highImpactFixes;

    /**
     * 题型短板分析
     */
    private List<QuestionTypeWeakness> questionTypeWeakness;

    /**
     * 7天行动计划
     */
    private List<String> next7DayPlan;

    /**
     * 信心分（0-100）
     */
    private Integer confidenceScore;

    /**
     * 是否为降级结果
     */
    private boolean fallback;

    @Data
    @Accessors(chain = true)
    public static class HighImpactFix {
        private String issue;
        private String action;
        private String deadline;
        private String metric;
    }

    @Data
    @Accessors(chain = true)
    public static class QuestionTypeWeakness {
        private String type;
        private String suggestion;
    }

    /**
     * 创建降级结果
     */
    public static JobBattleInterviewReviewResult fallbackResult(String interviewResult) {
        boolean pass = "pass".equalsIgnoreCase(interviewResult);
        String conclusion = pass
                ? "本次面试整体表现较好，建议继续巩固优势并补齐表达细节。"
                : "本次面试仍有改进空间，建议围绕高频题型进行针对性强化。";

        return new JobBattleInterviewReviewResult()
                .setOverallConclusion(conclusion)
                .setRootCauses(List.of(
                        "回答结构化程度不足",
                        "项目证据与指标表达不充分"
                ))
                .setHighImpactFixes(List.of(
                        new HighImpactFix()
                                .setIssue("系统设计题表述不成体系")
                                .setAction("使用固定答题模板进行10题演练")
                                .setDeadline("D+5")
                                .setMetric("每题5分钟内清晰表达方案与权衡")
                ))
                .setQuestionTypeWeakness(List.of(
                        new QuestionTypeWeakness()
                                .setType("系统设计")
                                .setSuggestion("按“容量估算-架构方案-瓶颈-优化”顺序回答")
                ))
                .setNext7DayPlan(List.of(
                        "Day1-2：补齐2个项目的性能优化案例",
                        "Day3-4：系统设计题专项训练",
                        "Day5-6：高频追问演练并录音复盘",
                        "Day7：一次完整模拟面试+总结"
                ))
                .setConfidenceScore(pass ? 78 : 62)
                .setFallback(true);
    }
}

