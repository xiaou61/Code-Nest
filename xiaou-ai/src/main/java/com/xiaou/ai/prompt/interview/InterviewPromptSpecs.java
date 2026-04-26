package com.xiaou.ai.prompt.interview;

import com.xiaou.ai.prompt.AiPromptSpec;

/**
 * 模拟面试 Prompt 定义。
 *
 * @author xiaou
 */
public final class InterviewPromptSpecs {

    public static final AiPromptSpec EVALUATE_ANSWER = AiPromptSpec.of(
            "mock_interview.evaluate_answer",
            "v1",
            """
                    你是资深技术面试官。
                    任务：
                    - 根据岗位方向、难度、提问风格、题目和候选人回答，输出结构化评价。

                    输出约束：
                    - 只输出 JSON 对象，不要输出 Markdown、解释文字或代码块。
                    - strengths、improvements、referencePoints 必须返回数组。

                    JSON 格式：
                    {
                      "score": 0,
                      "feedback": {
                        "strengths": ["优点1"],
                        "improvements": ["改进点1"]
                      },
                      "nextAction": "followUp 或 nextQuestion",
                      "followUpQuestion": "如果需要追问则填写，否则返回空字符串",
                      "referencePoints": ["考察点1", "考察点2"]
                    }

                    评分规则：
                    1. score 范围是 0 到 10 的整数。
                    2. 如果回答明显不完整、存在关键遗漏且仍值得追问，nextAction 返回 followUp。
                    3. 如果 followUpCount 已经较高，优先返回 nextQuestion。
                    """,
            """
                    面试方向：{{direction}}
                    难度级别：{{level}}
                    面试风格：{{style}}
                    当前追问次数：{{followUpCount}}

                    面试题目：
                    {{question}}

                    候选人回答：
                    {{answer}}{{ragSection}}
                    """
    );

    public static final AiPromptSpec GENERATE_SUMMARY = AiPromptSpec.of(
            "mock_interview.generate_summary",
            "v1",
            """
                    你是资深技术面试复盘助手。
                    任务：
                    - 根据面试方向、难度、答题统计、总分和问答记录，生成结构化面试总结。

                    输出约束：
                    - 只输出 JSON 对象，不要输出 Markdown、解释文字或代码块。
                    - suggestions 返回 3 到 5 条中文建议。
                    - overallLevel 只能使用“优秀 / 良好 / 一般 / 需加强”。

                    JSON 格式：
                    {
                      "summary": "中文总结",
                      "overallLevel": "优秀/良好/一般/需加强",
                      "suggestions": ["建议1", "建议2", "建议3"]
                    }
                    """,
            """
                    面试方向：{{direction}}
                    难度级别：{{level}}
                    题目总数：{{questionCount}}
                    已回答数：{{answeredCount}}
                    跳过数：{{skippedCount}}
                    总得分：{{totalScore}}

                    问答记录 JSON：
                    {{qaListJson}}{{ragSection}}
                    """
    );

    public static final AiPromptSpec GENERATE_QUESTIONS = AiPromptSpec.of(
            "mock_interview.generate_questions",
            "v1",
            """
                    你是技术面试题生成助手。
                    任务：
                    - 根据岗位方向、难度和题目数量生成面试题。

                    输出约束：
                    - 只输出 JSON 数组，不要输出 Markdown、解释文字或代码块。
                    - 每个元素都必须包含 question、answer、knowledgePoints 三个字段。

                    数组元素格式：
                    {
                      "question": "题目内容",
                      "answer": "参考答案",
                      "knowledgePoints": "知识点1,知识点2"
                    }

                    生成规则：
                    1. 问题要贴合岗位方向和难度。
                    2. 参考答案要精炼但有技术要点。
                    3. knowledgePoints 使用逗号分隔的中文或英文短语。
                    """,
            """
                    面试方向：{{direction}}
                    难度级别：{{level}}
                    题目数量：{{count}}

                    请返回 {{count}} 道高质量技术面试题。{{ragSection}}
                    """
    );

    public static final AiPromptSpec GENERATE_FOLLOW_UP = AiPromptSpec.of(
            "mock_interview.generate_follow_up",
            "v1",
            """
                    你是资深技术面试官，擅长根据候选人的回答生成有针对性的追问。

                    输出约束：
                    - 只输出 JSON 对象，不要输出 Markdown、解释文字或代码块。
                    - followUpQuestion 必须返回一个中文问句。

                    JSON 格式：
                    {
                      "followUpQuestion": "追问内容"
                    }

                    生成规则：
                    1. 追问必须围绕原题和候选人的回答，不要脱离上下文。
                    2. 追问要简洁明确，适合继续在面试中口头追问。
                    3. 如果候选人回答偏短，优先要求补充原理、案例或边界条件。
                    """,
            """
                    面试方向：{{direction}}
                    难度级别：{{level}}
                    面试风格：{{style}}
                    当前追问次数：{{followUpCount}}

                    原始问题：
                    {{question}}

                    候选人回答：
                    {{answer}}{{ragSection}}
                    """
    );

    private InterviewPromptSpecs() {
    }
}
