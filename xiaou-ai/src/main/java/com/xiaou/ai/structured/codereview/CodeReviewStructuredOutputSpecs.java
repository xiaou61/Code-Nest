package com.xiaou.ai.structured.codereview;

import com.xiaou.ai.prompt.codereview.CodeReviewPromptSpecs;
import com.xiaou.ai.structured.AiStructuredOutputSpec;

/**
 * CodePen 审查结果的结构化输出契约。
 */
public final class CodeReviewStructuredOutputSpecs {

    public static final AiStructuredOutputSpec CODEPEN_REVIEW = AiStructuredOutputSpec.object(
            CodeReviewPromptSpecs.CODEPEN_REVIEW,
            validator -> validator
                    .requireIntRange("score", 0, 100)
                    .requireString("summary")
                    .requireObjectArray("findings", item -> item
                            .requireString("severity")
                            .requireString("area")
                            .requireString("title")
                            .requireString("description")
                            .requireString("recommendedAction"))
                    .requireObjectArray("actionItems", item -> item
                            .requireString("title")
                            .requireString("description")
                            .requireString("verification"))
    );

    private CodeReviewStructuredOutputSpecs() {
    }
}
