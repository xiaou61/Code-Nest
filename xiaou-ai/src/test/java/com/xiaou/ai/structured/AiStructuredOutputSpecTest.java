package com.xiaou.ai.structured;

import cn.hutool.json.JSONUtil;
import com.xiaou.ai.prompt.AiPromptCatalog;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiStructuredOutputSpecTest {

    @Test
    void shouldRegisterUniqueStructuredOutputSpecIds() {
        List<AiStructuredOutputSpec> specs = AiStructuredOutputCatalog.all();
        Set<String> ids = specs.stream()
                .map(AiStructuredOutputSpec::specId)
                .collect(Collectors.toSet());

        assertEquals(specs.size(), ids.size(), "结构化输出契约不允许重复，避免 Prompt 与 schema 对不上");
    }

    @Test
    void shouldCoverAllPromptSpecs() {
        Set<String> promptIds = AiPromptCatalog.all().stream()
                .map(spec -> spec.promptId())
                .collect(Collectors.toSet());
        Set<String> schemaPromptIds = AiStructuredOutputCatalog.all().stream()
                .map(AiStructuredOutputSpec::specId)
                .collect(Collectors.toSet());

        assertEquals(promptIds, schemaPromptIds, "每个 PromptSpec 都应有对应的结构化输出契约");
    }

    @Test
    void shouldValidateAllStructuredOutputFixtures() {
        for (AiStructuredOutputSpec spec : AiStructuredOutputCatalog.all()) {
            String json = AiStructuredOutputFixtures.json(spec);
            assertNotNull(json, () -> "缺少结构化输出样例: " + spec.specId());

            AiStructuredOutputValidator.ValidationResult result = switch (spec.rootType()) {
                case OBJECT -> spec.validateObject(JSONUtil.parseObj(json));
                case ARRAY -> spec.validateArray(JSONUtil.parseArray(json));
            };
            assertTrue(result.valid(), () -> "结构化输出样例不满足契约: " + spec.specId() + ", reason=" + result.reason());
        }
    }

    @Test
    void shouldRejectWrongRootTypeValidation() {
        AiStructuredOutputValidator.ValidationResult objectAsArray = AiStructuredOutputCatalog.all().stream()
                .filter(spec -> spec.rootType() == AiStructuredOutputSpec.RootType.OBJECT)
                .findFirst()
                .orElseThrow()
                .validateArray(JSONUtil.parseArray("[]"));

        assertFalse(objectAsArray.valid());
        assertEquals("root_type_mismatch", objectAsArray.reason());
    }

    @Test
    void shouldGenerateJsonSchemaForAllStructuredOutputSpecs() {
        for (AiStructuredOutputSpec spec : AiStructuredOutputCatalog.all()) {
            var schema = spec.jsonSchema();
            assertEquals("https://json-schema.org/draft/2020-12/schema", schema.getStr("$schema"));
            assertEquals(spec.schemaId(), schema.getStr("$id"));
            assertEquals(spec.promptSpec().key(), schema.getStr("title"));
            assertEquals(spec.promptSpec().key(), schema.getStr("x-prompt-key"));
            assertEquals(spec.promptSpec().version(), schema.getStr("x-prompt-version"));
            assertEquals(spec.rootType() == AiStructuredOutputSpec.RootType.OBJECT ? "object" : "array", schema.getStr("type"));
        }
    }
}
