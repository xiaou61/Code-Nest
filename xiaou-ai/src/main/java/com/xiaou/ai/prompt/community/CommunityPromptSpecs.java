package com.xiaou.ai.prompt.community;

import com.xiaou.ai.prompt.AiPromptSpec;

/**
 * 社区场景 Prompt 定义。
 *
 * @author xiaou
 */
public final class CommunityPromptSpecs {

    public static final AiPromptSpec POST_SUMMARY = AiPromptSpec.of(
            "community.post_summary",
            "v1",
            """
                    你是社区内容运营助手。
                    任务：
                    - 根据标题和正文生成帖子摘要，并提取 3 到 6 个关键词。

                    输出约束：
                    - 只输出 JSON 对象，不要输出 Markdown、解释文字或代码块。

                    JSON 格式：
                    {
                      "summary": "不超过120字的中文摘要",
                      "keywords": ["关键词1", "关键词2"]
                    }
                    """,
            """
                    标题：
                    {{title}}

                    正文：
                    {{content}}
                    """
    );

    private CommunityPromptSpecs() {
    }
}
