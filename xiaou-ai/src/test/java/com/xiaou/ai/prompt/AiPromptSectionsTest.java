package com.xiaou.ai.prompt;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiPromptSectionsTest {

    @Test
    void shouldWrapRagContextWithExplicitDelimiter() {
        String ragSection = AiPromptSections.ragSection("缓存一致性要关注延迟双删与消息补偿");

        assertTrue(ragSection.contains("<knowledge_context>"));
        assertTrue(ragSection.contains("</knowledge_context>"));
        assertTrue(ragSection.contains("优先采用 score 更高"));
        assertTrue(ragSection.contains("缓存一致性要关注延迟双删与消息补偿"));
    }

    @Test
    void shouldReturnBlankWhenRagContextMissing() {
        assertEquals("", AiPromptSections.ragSection("   "));
    }
}
