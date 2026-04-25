package com.xiaou.ai.structured;

import cn.hutool.json.JSONUtil;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiStructuredOutputValidatorTest {

    @Test
    void shouldValidateNestedObjectAndArraySchema() {
        var json = JSONUtil.parseObj("""
                {
                  "score": 8,
                  "feedback": {
                    "strengths": ["回答有条理"],
                    "improvements": ["缺少边界条件"]
                  },
                  "nextAction": "followUp",
                  "followUpQuestion": "",
                  "referencePoints": ["索引失效"]
                }
                """);

        var result = AiStructuredOutputValidator.object(json)
                .requireIntRange("score", 0, 10)
                .requireObject("feedback", nested -> nested
                        .requireStringArray("strengths")
                        .requireStringArray("improvements"))
                .requireStringInSet("nextAction", Set.of("followUp", "nextQuestion"))
                .requireString("followUpQuestion")
                .requireStringArray("referencePoints")
                .validate();

        assertTrue(result.valid());
    }

    @Test
    void shouldReportPreciseFailureReasonWhenArrayItemSchemaIsInvalid() {
        var array = JSONUtil.parseArray("""
                [
                  {"question": "Q1", "answer": "A1", "knowledgePoints": "K1"},
                  {"question": "Q2", "answer": 123, "knowledgePoints": "K2"}
                ]
                """);

        var result = AiStructuredOutputValidator.array(array)
                .requireObjectItems(item -> item
                        .requireString("question")
                        .requireString("answer")
                        .requireString("knowledgePoints"))
                .validate();

        assertFalse(result.valid());
        assertEquals("item_1.answer_missing_or_not_string", result.reason());
    }

    @Test
    void shouldAcceptStringOrStringArrayForBackwardCompatibleFields() {
        var json = JSONUtil.parseObj("""
                {
                  "summary": "摘要",
                  "keywords": "SQL优化,索引"
                }
                """);

        var result = AiStructuredOutputValidator.object(json)
                .requireString("summary")
                .requireStringOrStringArray("keywords")
                .validate();

        assertTrue(result.valid());
    }
}
