package com.xiaou.ai.prompt.jobbattle;

import com.xiaou.ai.prompt.AiRagQuerySpec;

/**
 * 求职作战台 RAG 检索查询模板。
 *
 * @author xiaou
 */
public final class JobBattleRagQuerySpecs {

    public static final AiRagQuerySpec PARSE_JD = AiRagQuerySpec.of(
            "job_battle.retrieve.parse_jd",
            "v1",
            """
                    求职作战台 JD 解析参考：
                    目标岗位={{targetRole}}
                    目标级别={{targetLevel}}
                    城市={{city}}
                    JD={{jdText}}
                    请召回该岗位常见要求、技能关键词和岗位画像模板。
                    """
    );

    public static final AiRagQuerySpec MATCH_RESUME = AiRagQuerySpec.of(
            "job_battle.retrieve.match_resume",
            "v1",
            """
                    求职作战台简历匹配参考：
                    结构化JD={{parsedJdJson}}
                    简历={{resumeText}}
                    项目亮点={{projectHighlights}}
                    目标公司类型={{targetCompanyType}}
                    请召回该类岗位常见简历筛选标准、能力模型和关键词。
                    """
    );

    public static final AiRagQuerySpec GENERATE_PLAN = AiRagQuerySpec.of(
            "job_battle.retrieve.generate_plan",
            "v1",
            """
                    求职作战台计划生成参考：
                    能力差距={{gapsJson}}
                    目标天数={{targetDays}}
                    每周投入={{weeklyHours}}
                    学习偏好={{preferredLearningMode}}
                    下一场面试={{nextInterviewDate}}
                    请召回常见冲刺计划模板、优先级排序和学习节奏建议。
                    """
    );

    public static final AiRagQuerySpec REVIEW_INTERVIEW = AiRagQuerySpec.of(
            "job_battle.retrieve.review_interview",
            "v1",
            """
                    求职作战台复盘参考：
                    目标岗位={{targetRole}}
                    面试结果={{interviewResult}}
                    面试记录={{interviewNotes}}
                    问答实录={{qaTranscriptJson}}
                    下一场面试={{nextInterviewDate}}
                    请召回答题复盘框架、常见失分点和修复建议模板。
                    """
    );

    public static final AiRagQuerySpec ANALYZE_TARGET = AiRagQuerySpec.of(
            "job_battle.retrieve.analyze_target",
            "v1",
            """
                    求职作战台单岗位综合分析参考：
                    目标岗位={{targetRole}}
                    目标级别={{targetLevel}}
                    城市={{city}}
                    JD={{jdText}}
                    简历={{resumeText}}
                    项目亮点={{projectHighlights}}
                    目标公司类型={{targetCompanyType}}
                    请召回岗位画像、筛选标准和常见差距项。
                    """
    );

    private JobBattleRagQuerySpecs() {
    }
}
