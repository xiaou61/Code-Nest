package com.xiaou.ai.eval;

/**
 * AI 回归评测场景执行器。
 *
 * @author xiaou
 */
public interface AiEvalScenario {

    /**
     * 场景键。
     */
    String scenarioKey();

    /**
     * 执行指定评测用例。
     */
    Object execute(AiEvalCase testCase);
}
