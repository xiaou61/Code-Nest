package com.xiaou.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Coze工作流枚举
 * 统一管理所有Coze工作流的配置信息
 *
 * @author xiaou
 */
@Getter
@AllArgsConstructor
public enum CozeWorkflowEnum {
    
    /**
     * 示例工作流 - 文本生成
     */
    TEST("7553122464865255463", "测试工作流", "test"),
    
    /**
     * 社区帖子AI摘要生成
     */
    COMMUNITY_POST_SUMMARY("7556892077167673394", "社区帖子AI摘要", "生成帖子摘要和关键词"),

    /**
     * 模拟面试 - AI评价答案
     */
    MOCK_INTERVIEW_EVALUATE("7586301520637100074", "模拟面试答案评价", "AI评价候选人的答案并给出反馈"),

    /**
     * 模拟面试 - AI生成总结
     */
    MOCK_INTERVIEW_SUMMARY("7586302912424607787", "模拟面试总结", "AI生成面试总结报告"),

    /**
     * 模拟面试 - AI出题
     */
    MOCK_INTERVIEW_GENERATE_QUESTIONS("7586303842754347035", "模拟面试AI出题", "AI根据方向和难度生成面试题目"),

    /**
     * 求职作战台 - JD解析
     */
    JOB_BATTLE_JD_PARSE("7611786534887194639", "求职作战台JD解析", "解析岗位JD并提取结构化要求"),

    /**
     * 求职作战台 - 简历匹配评估
     */
    JOB_BATTLE_RESUME_MATCH("7611787299885432838", "求职作战台简历匹配评估", "评估简历与岗位JD的匹配度并识别能力差距"),

    /**
     * 求职作战台 - 补短板行动计划
     */
    JOB_BATTLE_PLAN_GENERATE("7611787804339388468", "求职作战台行动计划生成", "生成求职冲刺提升计划"),

    /**
     * 求职作战台 - 面试复盘总结
     */
    JOB_BATTLE_INTERVIEW_REVIEW("7611788282342473738", "求职作战台面试复盘", "基于面试记录生成复盘结论与改进建议"),

    /**
     * 慢SQL优化分析
     */
    SQL_OPTIMIZE_ANALYZE("7613589259505352738", "慢SQL优化分析", "分析SQL执行计划并给出优化建议"),

    /**
     * 慢SQL优化分析 2.0
     */
    SQL_OPTIMIZE_ANALYZE_V2("7613589861928435755", "慢SQL优化分析2.0", "分析SQL执行计划并输出结构化诊断结果"),

    /**
     * 慢SQL重写建议 2.0
     */
    SQL_OPTIMIZE_REWRITE_V2("7613590170381697058", "慢SQL重写建议2.0", "生成优化SQL和索引建议"),

    /**
     * 慢SQL收益对比 2.0
     */
    SQL_OPTIMIZE_COMPARE_V2("7613590435751329801", "慢SQL收益对比2.0", "对比优化前后收益并输出评估"),

    /**
     * AI成长教练 - 快照生成
     */
    GROWTH_COACH_SNAPSHOT("7614880000000001001", "AI成长教练快照生成", "根据学习与求职聚合数据输出结构化诊断快照"),

    /**
     * AI成长教练 - 行动重排
     */
    GROWTH_COACH_ACTION_REPLAN("7614880000000001002", "AI成长教练行动重排", "基于当前时间预算重排行动建议"),

    /**
     * AI成长教练 - 追问对话
     */
    GROWTH_COACH_CHAT("7614880000000001003", "AI成长教练追问对话", "围绕当前诊断快照输出可解释追问回答"),

    /**
     * 科技热点速览 - 翻译
     */
    TECH_BRIEFING_TRANSLATE("7614880000000002001", "科技热点翻译", "将海外科技新闻翻译为中文标题、摘要和正文"),

    /**
     * 科技热点速览 - AI摘要
     */
    TECH_BRIEFING_SUMMARY("7614880000000002002", "科技热点摘要", "提炼科技新闻的中文速览、影响与关键词");

    /**
     * 工作流ID
     */
    private final String workflowId;
    
    /**
     * 工作流名称
     */
    private final String workflowName;
    
    /**
     * 工作流描述
     */
    private final String description;
    
    /**
     * 根据工作流ID获取枚举
     *
     * @param workflowId 工作流ID
     * @return 工作流枚举，未找到时返回null
     */
    public static CozeWorkflowEnum getByWorkflowId(String workflowId) {
        for (CozeWorkflowEnum workflow : values()) {
            if (workflow.getWorkflowId().equals(workflowId)) {
                return workflow;
            }
        }
        return null;
    }
    
    /**
     * 检查工作流ID是否存在
     *
     * @param workflowId 工作流ID
     * @return 是否存在
     */
    public static boolean exists(String workflowId) {
        return getByWorkflowId(workflowId) != null;
    }
}
