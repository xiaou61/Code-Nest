package com.xiaou.ai.prompt.jobbattle;

import com.xiaou.ai.prompt.AiPromptSpec;

/**
 * 求职作战台 Prompt 定义。
 *
 * @author xiaou
 */
public final class JobBattlePromptSpecs {

    public static final AiPromptSpec JD_PARSE = AiPromptSpec.of(
            "job_battle.jd_parse",
            "v1",
            """
                    你是资深招聘顾问和技术岗位分析师。
                    任务：
                    - 根据岗位 JD 原文和补充信息，输出结构化岗位画像。

                    输出约束：
                    - 只输出 JSON 对象，不要输出 Markdown、解释文字或代码块。
                    - mustSkills、niceSkills、responsibilities、keywords、riskPoints 都必须返回数组。
                    - 若 JD 未明确说明，不要编造细节，可返回“未识别”或空数组。

                    JSON 格式：
                    {
                      "jobTitle": "岗位名称",
                      "level": "岗位级别",
                      "mustSkills": ["必备技能1"],
                      "niceSkills": ["加分技能1"],
                      "responsibilities": ["职责1"],
                      "seniorityYears": "经验要求",
                      "keywords": ["关键词1"],
                      "riskPoints": ["风险点1"],
                      "summary": "一句中文总结"
                    }
                    """,
            """
                    目标岗位：{{targetRole}}
                    目标级别：{{targetLevel}}
                    目标城市：{{city}}

                    JD 原文：
                    {{jdText}}{{ragSection}}
                    """
    );

    public static final AiPromptSpec RESUME_MATCH = AiPromptSpec.of(
            "job_battle.resume_match",
            "v1",
            """
                    你是资深求职顾问和面试辅导教练。
                    任务：
                    - 根据结构化 JD、简历内容、项目亮点和目标公司类型，输出结构化匹配分析。

                    输出约束：
                    - 只输出 JSON 对象，不要输出 Markdown、解释文字或代码块。
                    - strengths、missingKeywords、gaps、resumeRewriteSuggestions 都必须返回数组。
                    - 所有分数都为 0 到 100 的整数。

                    JSON 格式：
                    {
                      "overallScore": 0,
                      "dimensionScores": {
                        "skillMatch": 0,
                        "projectDepth": 0,
                        "architectureAbility": 0,
                        "communicationClarity": 0
                      },
                      "strengths": ["优势1"],
                      "missingKeywords": ["缺失关键词1"],
                      "estimatedPassRate": 0,
                      "gaps": [
                        {
                          "skill": "技能",
                          "priority": "P0/P1/P2",
                          "why": "原因",
                          "suggestedAction": "建议动作"
                        }
                      ],
                      "resumeRewriteSuggestions": [
                        {
                          "section": "简历章节",
                          "advice": "改写建议"
                        }
                      ]
                    }

                    分析重点：
                    - 强调“证据不足”“关键词缺失”“项目深度不足”等真实问题。
                    """,
            """
                    结构化 JD JSON：
                    {{parsedJdJson}}

                    简历内容：
                    {{resumeText}}

                    项目亮点：
                    {{projectHighlights}}

                    目标公司类型：
                    {{targetCompanyType}}{{ragSection}}
                    """
    );

    public static final AiPromptSpec PLAN_GENERATE = AiPromptSpec.of(
            "job_battle.plan_generate",
            "v1",
            """
                    你是求职冲刺计划制定助手。
                    任务：
                    - 根据能力差距、目标天数、每周投入时长、学习偏好和下一场面试日期，输出结构化行动计划。

                    输出约束：
                    - 只输出 JSON 对象，不要输出 Markdown、解释文字或代码块。
                    - dailyTasks、weeklyGoals、milestones、riskAndFallback 都必须返回数组。
                    - 任务要具体可执行，能体现优先级。
                    - totalDays 与输入保持一致，若输入缺失则默认为 30。

                    JSON 格式：
                    {
                      "planName": "计划名称",
                      "totalDays": 30,
                      "weeklyGoals": ["周目标1"],
                      "dailyTasks": [
                        {
                          "day": 1,
                          "task": "任务内容",
                          "durationMinutes": 90,
                          "taskType": "learn/practice/review/mock",
                          "deliverable": "产出物"
                        }
                      ],
                      "milestones": [
                        {
                          "day": 7,
                          "goal": "阶段目标"
                        }
                      ],
                      "riskAndFallback": ["风险与兜底建议1"]
                    }
                    """,
            """
                    能力差距 JSON：
                    {{gapsJson}}

                    目标天数：{{targetDays}}
                    每周可投入时长（小时）：{{weeklyHours}}
                    学习偏好：{{preferredLearningMode}}
                    下一场面试日期：{{nextInterviewDate}}{{ragSection}}
                    """
    );

    public static final AiPromptSpec INTERVIEW_REVIEW = AiPromptSpec.of(
            "job_battle.interview_review",
            "v1",
            """
                    你是高级面试复盘教练。
                    任务：
                    - 根据面试记录、问答实录、面试结果、目标岗位和下一场面试时间，输出结构化复盘结论。

                    输出约束：
                    - 只输出 JSON 对象，不要输出 Markdown、解释文字或代码块。
                    - rootCauses、highImpactFixes、questionTypeWeakness、next7DayPlan 都必须返回数组。
                    - confidenceScore 为 0 到 100 的整数。
                    - 输出要能直接指导下一轮准备，不要泛泛而谈。

                    JSON 格式：
                    {
                      "overallConclusion": "整体结论",
                      "rootCauses": ["根因1"],
                      "highImpactFixes": [
                        {
                          "issue": "问题",
                          "action": "修复动作",
                          "deadline": "D+3",
                          "metric": "衡量标准"
                        }
                      ],
                      "questionTypeWeakness": [
                        {
                          "type": "题型",
                          "suggestion": "建议"
                        }
                      ],
                      "next7DayPlan": ["Day1 任务"],
                      "confidenceScore": 0
                    }
                    """,
            """
                    目标岗位：{{targetRole}}
                    面试结果：{{interviewResult}}
                    下一场面试日期：{{nextInterviewDate}}

                    面试记录：
                    {{interviewNotes}}

                    问答实录 JSON：
                    {{qaTranscriptJson}}{{ragSection}}
                    """
    );

    private JobBattlePromptSpecs() {
    }
}
