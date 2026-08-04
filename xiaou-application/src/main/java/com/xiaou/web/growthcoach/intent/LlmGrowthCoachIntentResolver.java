package com.xiaou.web.growthcoach.intent;

import cn.hutool.json.JSONObject;
import com.xiaou.ai.prompt.growthcoach.GrowthCoachPromptSpecs;
import com.xiaou.ai.structured.AiStructuredOutputValidator;
import com.xiaou.ai.structured.growthcoach.GrowthCoachStructuredOutputSpecs;
import com.xiaou.ai.support.AiExecutionSupport;
import com.xiaou.ai.util.AiJsonResponseParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * 使用统一 AI 运行时解析意图，失败时回落到确定性解析。
 */
@Service
@RequiredArgsConstructor
public class LlmGrowthCoachIntentResolver implements GrowthCoachIntentResolver {

    private static final String SCENE_NAME = "growth_coach.plan_adjustment_intent";

    private final AiExecutionSupport aiExecutionSupport;
    private final DeterministicGrowthCoachIntentParser fallbackParser;

    @Override
    public GrowthCoachIntent resolve(String message) {
        GrowthCoachIntent fallback = fallbackParser.parse(message);
        return aiExecutionSupport.chatWithFallback(
                SCENE_NAME,
                GrowthCoachPromptSpecs.PLAN_ADJUSTMENT_INTENT,
                Map.of("message", promptData(message)),
                response -> parse(response, fallback),
                () -> fallback
        );
    }

    private GrowthCoachIntent parse(String response, GrowthCoachIntent fallback) {
        JSONObject json = AiJsonResponseParser.parse(response);
        AiStructuredOutputValidator.ValidationResult validation =
                GrowthCoachStructuredOutputSpecs.PLAN_ADJUSTMENT_INTENT.validateObject(json);
        if (!validation.valid()) {
            throw new IllegalArgumentException("growth coach intent output invalid: " + validation.reason());
        }

        GrowthCoachIntent result = new GrowthCoachIntent();
        Integer minutes = json.getInt("availableMinutes", 0);
        result.setAvailableMinutes(minutes != null && minutes > 0 ? minutes : fallback.getAvailableMinutes());
        String role = json.getStr("targetRole", "").trim();
        result.setTargetRole(StringUtils.hasText(role) ? role : fallback.getTargetRole());
        result.setPrioritizeInterview(json.getInt("prioritizeInterview", 0) == 1 || fallback.isPrioritizeInterview());
        result.setSummary(json.getStr("summary", fallback.getSummary()));
        result.setResolutionMode("LLM_STRUCTURED");
        return result;
    }

    private String promptData(String message) {
        if (message == null) {
            return "";
        }
        return message.trim()
                .replace("<user_message>", "&lt;user_message&gt;")
                .replace("</user_message>", "&lt;/user_message&gt;");
    }
}
