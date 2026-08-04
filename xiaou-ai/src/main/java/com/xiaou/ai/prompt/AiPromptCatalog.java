package com.xiaou.ai.prompt;

import com.xiaou.ai.prompt.admin.AdminAgentPromptSpecs;
import com.xiaou.ai.prompt.community.CommunityPromptSpecs;
import com.xiaou.ai.prompt.codereview.CodeReviewPromptSpecs;
import com.xiaou.ai.prompt.growthcoach.GrowthCoachPromptSpecs;
import com.xiaou.ai.prompt.interview.InterviewPromptSpecs;
import com.xiaou.ai.prompt.jobbattle.JobBattlePromptSpecs;
import com.xiaou.ai.prompt.sql.SqlOptimizePromptSpecs;
import com.xiaou.ai.prompt.sre.SreInvestigationPromptSpecs;
import com.xiaou.ai.prompt.sre.SreRcaPromptSpecs;

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
            AdminAgentPromptSpecs.class,
            CommunityPromptSpecs.class,
            CodeReviewPromptSpecs.class,
            GrowthCoachPromptSpecs.class,
            InterviewPromptSpecs.class,
            JobBattlePromptSpecs.class,
            SreInvestigationPromptSpecs.class,
            SreRcaPromptSpecs.class,
            SqlOptimizePromptSpecs.class
    );

    private static final List<AiPromptSpec> SPECS = AiCatalogSupport.collectStaticSpecs(HOLDERS, AiPromptSpec.class);

    private AiPromptCatalog() {
    }

    public static List<AiPromptSpec> all() {
        return SPECS;
    }
}
