package com.xiaou.ai.prompt.interview;

import com.xiaou.ai.prompt.AiRagQuerySpec;

/**
 * 模拟面试 RAG 检索查询模板。
 *
 * @author xiaou
 */
public final class InterviewRagQuerySpecs {

    public static final AiRagQuerySpec GENERATE_QUESTIONS = AiRagQuerySpec.of(
            "mock_interview.retrieve.generate_questions",
            "v1",
            """
                    模拟面试题生成参考：
                    面试方向={{direction}}
                    难度={{level}}
                    数量={{count}}
                    请召回该方向常见考点、经典题型和高频知识点。
                    """
    );

    public static final AiRagQuerySpec GENERATE_SUMMARY = AiRagQuerySpec.of(
            "mock_interview.retrieve.generate_summary",
            "v1",
            """
                    模拟面试复盘参考：
                    面试方向={{direction}}
                    难度={{level}}
                    题目总数={{questionCount}}
                    已回答={{answeredCount}}
                    已跳过={{skippedCount}}
                    总分={{totalScore}}
                    请召回该方向常见能力模型、复盘建议和提升路线。
                    """
    );

    public static final AiRagQuerySpec GENERATE_FOLLOW_UP = AiRagQuerySpec.of(
            "mock_interview.retrieve.generate_follow_up",
            "v1",
            """
                    模拟面试追问参考：
                    面试方向={{direction}}
                    难度={{level}}
                    风格={{style}}
                    题目={{question}}
                    回答={{answer}}
                    当前追问次数={{followUpCount}}
                    请召回该问题涉及的核心知识点和常见追问方向。
                    """
    );

    public static final AiRagQuerySpec EVALUATE_ANSWER = AiRagQuerySpec.of(
            "mock_interview.retrieve.evaluate_answer",
            "v1",
            """
                    模拟面试评价参考：
                    面试方向={{direction}}
                    难度={{level}}
                    风格={{style}}
                    题目={{question}}
                    回答={{answer}}
                    当前追问次数={{followUpCount}}
                    请召回评分要点、核心知识点和常见误区。
                    """
    );

    private InterviewRagQuerySpecs() {
    }
}
