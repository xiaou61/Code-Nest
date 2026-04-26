package com.xiaou.ai.prompt;

import com.xiaou.ai.prompt.interview.InterviewRagQuerySpecs;
import com.xiaou.ai.prompt.jobbattle.JobBattleRagQuerySpecs;
import com.xiaou.ai.prompt.sql.SqlOptimizeRagQuerySpecs;

import java.util.List;

/**
 * RAG 检索查询清单。
 *
 * @author xiaou
 */
public final class AiRagQueryCatalog {

    private static final List<Class<?>> HOLDERS = List.of(
            InterviewRagQuerySpecs.class,
            JobBattleRagQuerySpecs.class,
            SqlOptimizeRagQuerySpecs.class
    );

    private static final List<AiRagQuerySpec> SPECS = AiCatalogSupport.collectStaticSpecs(HOLDERS, AiRagQuerySpec.class);

    private AiRagQueryCatalog() {
    }

    public static List<AiRagQuerySpec> all() {
        return SPECS;
    }
}
