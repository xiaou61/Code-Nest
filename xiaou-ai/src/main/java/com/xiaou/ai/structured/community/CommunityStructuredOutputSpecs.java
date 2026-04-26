package com.xiaou.ai.structured.community;

import com.xiaou.ai.prompt.community.CommunityPromptSpecs;
import com.xiaou.ai.structured.AiStructuredOutputSpec;

/**
 * 社区结构化输出契约。
 *
 * @author xiaou
 */
public final class CommunityStructuredOutputSpecs {

    public static final AiStructuredOutputSpec POST_SUMMARY = AiStructuredOutputSpec.object(
            CommunityPromptSpecs.POST_SUMMARY,
            validator -> validator
                    .requireString("summary")
                    .requireStringOrStringArray("keywords")
    );

    private CommunityStructuredOutputSpecs() {
    }
}
