package com.xiaou.ai.eval.scenario;

import com.xiaou.ai.eval.AiEvalCase;
import com.xiaou.ai.service.impl.AiCommunityServiceImpl;

/**
 * 社区摘要评测场景。
 *
 * @author xiaou
 */
public class CommunitySummaryEvalScenario extends AbstractAiEvalScenario {

    @Override
    public String scenarioKey() {
        return "community_summary";
    }

    @Override
    public Object execute(AiEvalCase testCase) {
        AiCommunityServiceImpl service = new AiCommunityServiceImpl(
                buildExecutionSupport(testCase),
                buildMetricsRecorder()
        );
        return service.generatePostSummary(
                stringInput(testCase, "title"),
                stringInput(testCase, "content")
        );
    }
}
