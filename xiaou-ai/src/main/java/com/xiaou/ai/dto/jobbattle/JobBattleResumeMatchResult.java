package com.xiaou.ai.dto.jobbattle;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 求职作战台-简历匹配结果
 *
 * @author xiaou
 */
@Data
@Accessors(chain = true)
public class JobBattleResumeMatchResult {

    /**
     * 总体匹配分（0-100）
     */
    private Integer overallScore;

    /**
     * 分维度评分
     */
    private Map<String, Integer> dimensionScores;

    /**
     * 优势项
     */
    private List<String> strengths;

    /**
     * 差距项
     */
    private List<Gap> gaps;

    /**
     * 简历改写建议
     */
    private List<ResumeRewriteSuggestion> resumeRewriteSuggestions;

    /**
     * 缺失关键词
     */
    private List<String> missingKeywords;

    /**
     * 预估通过率（0-100）
     */
    private Integer estimatedPassRate;

    /**
     * 是否为降级结果
     */
    private boolean fallback;

    @Data
    @Accessors(chain = true)
    public static class Gap {
        private String skill;
        private String priority;
        private String why;
        private String suggestedAction;
    }

    @Data
    @Accessors(chain = true)
    public static class ResumeRewriteSuggestion {
        private String section;
        private String advice;
    }

    /**
     * 创建降级结果
     */
    public static JobBattleResumeMatchResult fallbackResult(String parsedJdJson, String resumeText) {
        int overallScore = 60;
        List<String> missing = new ArrayList<>();
        List<Gap> gaps = new ArrayList<>();

        try {
            JSONObject jdJson = JSONUtil.parseObj(parsedJdJson);
            if (jdJson.containsKey("output")) {
                jdJson = JSONUtil.parseObj(jdJson.getStr("output"));
            }

            List<String> mustSkills = jdJson.getBeanList("mustSkills", String.class);
            if (mustSkills != null && !mustSkills.isEmpty()) {
                String resume = resumeText == null ? "" : resumeText.toLowerCase(Locale.ROOT);
                int matched = 0;
                for (String skill : mustSkills) {
                    if (skill == null || skill.trim().isEmpty()) {
                        continue;
                    }
                    if (resume.contains(skill.toLowerCase(Locale.ROOT))) {
                        matched++;
                    } else {
                        missing.add(skill);
                        gaps.add(new Gap()
                                .setSkill(skill)
                                .setPriority("P0")
                                .setWhy("JD要求但简历中未明显体现")
                                .setSuggestedAction("补充与该技能相关的项目证据和量化结果"));
                    }
                }
                overallScore = 40 + (matched * 60 / Math.max(1, mustSkills.size()));
            }
        } catch (Exception ignored) {
            // 降级流程中忽略解析异常
        }

        Map<String, Integer> dimensionScores = new HashMap<>();
        dimensionScores.put("skillMatch", overallScore);
        dimensionScores.put("projectDepth", Math.max(40, overallScore - 8));
        dimensionScores.put("architectureAbility", Math.max(35, overallScore - 12));
        dimensionScores.put("communicationClarity", Math.max(45, overallScore + 5));

        List<String> strengths = new ArrayList<>();
        strengths.add("已生成基础匹配评估结果");
        strengths.add("建议补充项目中的技术细节和业务指标");

        List<ResumeRewriteSuggestion> rewriteSuggestions = new ArrayList<>();
        rewriteSuggestions.add(new ResumeRewriteSuggestion()
                .setSection("项目经历")
                .setAdvice("按“背景-挑战-方案-结果”补充结构化叙述"));
        rewriteSuggestions.add(new ResumeRewriteSuggestion()
                .setSection("技术栈")
                .setAdvice("优先把JD必备技能放在前排并与项目经历对应"));

        return new JobBattleResumeMatchResult()
                .setOverallScore(Math.min(100, Math.max(0, overallScore)))
                .setDimensionScores(dimensionScores)
                .setStrengths(strengths)
                .setGaps(gaps)
                .setResumeRewriteSuggestions(rewriteSuggestions)
                .setMissingKeywords(missing)
                .setEstimatedPassRate(Math.max(20, overallScore - 10))
                .setFallback(true);
    }
}

