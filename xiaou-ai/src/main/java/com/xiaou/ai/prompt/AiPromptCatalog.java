package com.xiaou.ai.prompt;

import com.xiaou.ai.prompt.community.CommunityPromptSpecs;
import com.xiaou.ai.prompt.interview.InterviewPromptSpecs;
import com.xiaou.ai.prompt.jobbattle.JobBattlePromptSpecs;
import com.xiaou.ai.prompt.sql.SqlOptimizePromptSpecs;

import java.util.List;

/**
 * AI Prompt 清单。
 *
 * <p>集中维护所有 PromptSpec，便于统一做规范校验、后续观测和回归样例治理。</p>
 *
 * @author xiaou
 */
public final class AiPromptCatalog {

    private static final List<Class<?>> HOLDERS = List.of(
            CommunityPromptSpecs.class,
            InterviewPromptSpecs.class,
            JobBattlePromptSpecs.class,
            SqlOptimizePromptSpecs.class
    );

    private static final List<AiPromptSpec> SPECS = AiCatalogSupport.collectStaticSpecs(HOLDERS, AiPromptSpec.class);

    private AiPromptCatalog() {
    }

    public static List<AiPromptSpec> all() {
        return SPECS;
    }
}
