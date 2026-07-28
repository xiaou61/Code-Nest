package com.xiaou.ai.prompt.sre;

import cn.hutool.json.JSONUtil;
import com.xiaou.ai.structured.sre.SreInvestigationStructuredOutputSpecs;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SreInvestigationPromptContractTest {

    @Test
    void planPromptOnlyAllowsBoundedReadOnlyToolSelection() {
        assertThat(SreInvestigationPromptSpecs.PLAN.maxCompletionTokens()).isEqualTo(500);
        assertThat(SreInvestigationPromptSpecs.PLAN.templateVariables())
                .isEqualTo(Set.of("incidentContextJson", "availableToolKeys"));
        assertThat(SreInvestigationPromptSpecs.PLAN.systemPrompt())
                .contains("固定只读工具", "最多选择 5 个", "不能生成 PromQL", "不能执行任何写操作");

        String rendered = SreInvestigationPromptSpecs.PLAN.renderUser(Map.of(
                "incidentContextJson", "{\"incident\":{\"id\":11}}",
                "availableToolKeys", "[\"prom_target_up\"]"
        ));

        assertThat(rendered).contains(
                "<untrusted_incident_context_json>",
                "<allowed_read_only_tool_keys>",
                "prom_target_up"
        );
    }

    @Test
    void planSchemaRejectsUnknownDecision() {
        var valid = SreInvestigationStructuredOutputSpecs.PLAN.validateObject(JSONUtil.parseObj("""
                {
                  "decision": "INVESTIGATE",
                  "reason": "需要补充目标健康证据",
                  "toolKeys": ["prom_target_up"]
                }
                """));
        var invalid = SreInvestigationStructuredOutputSpecs.PLAN.validateObject(JSONUtil.parseObj("""
                {
                  "decision": "EXECUTE",
                  "reason": "尝试修复",
                  "toolKeys": ["shell_restart"]
                }
                """));

        assertThat(valid.valid()).isTrue();
        assertThat(invalid.valid()).isFalse();
    }
}
