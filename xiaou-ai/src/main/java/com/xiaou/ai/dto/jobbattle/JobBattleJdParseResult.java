package com.xiaou.ai.dto.jobbattle;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 求职作战台-JD解析结果
 *
 * @author xiaou
 */
@Data
@Accessors(chain = true)
public class JobBattleJdParseResult {

    /**
     * 岗位名称
     */
    private String jobTitle;

    /**
     * 岗位级别
     */
    private String level;

    /**
     * 必备技能
     */
    private List<String> mustSkills;

    /**
     * 加分技能
     */
    private List<String> niceSkills;

    /**
     * 岗位职责
     */
    private List<String> responsibilities;

    /**
     * 年限要求
     */
    private String seniorityYears;

    /**
     * 关键词
     */
    private List<String> keywords;

    /**
     * 风险点
     */
    private List<String> riskPoints;

    /**
     * 总结
     */
    private String summary;

    /**
     * 是否为降级结果
     */
    private boolean fallback;

    /**
     * 创建降级结果
     */
    public static JobBattleJdParseResult fallbackResult(String jdText) {
        return new JobBattleJdParseResult()
                .setJobTitle("未识别岗位")
                .setLevel("未知")
                .setMustSkills(List.of())
                .setNiceSkills(List.of())
                .setResponsibilities(List.of())
                .setSeniorityYears("未识别")
                .setKeywords(List.of())
                .setRiskPoints(List.of("当前为降级模式，建议稍后重试AI解析"))
                .setSummary("已返回基础解析结果，请稍后重试以获取完整岗位画像")
                .setFallback(true);
    }
}

