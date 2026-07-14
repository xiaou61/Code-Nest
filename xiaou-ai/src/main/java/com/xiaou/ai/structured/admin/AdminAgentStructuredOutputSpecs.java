package com.xiaou.ai.structured.admin;

import com.xiaou.ai.prompt.admin.AdminAgentPromptSpecs;
import com.xiaou.ai.structured.AiStructuredOutputSpec;

/**
 * 管理员智能体结构化输出契约。
 *
 * @author xiaou
 */
public final class AdminAgentStructuredOutputSpecs {

    public static final AiStructuredOutputSpec PLAN = AiStructuredOutputSpec.object(
            AdminAgentPromptSpecs.PLAN,
            validator -> validator
                    .requireString("toolName")
                    .requireObject("input")
                    .requireNumberRange("confidence", 0D, 1D)
                    .requireStringArray("missingFields")
    );

    private AdminAgentStructuredOutputSpecs() {
    }
}
