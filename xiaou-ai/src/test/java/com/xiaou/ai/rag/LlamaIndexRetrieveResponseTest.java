package com.xiaou.ai.rag;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LlamaIndexRetrieveResponseTest {

    @Test
    void shouldBuildStructuredKnowledgeContextSnippet() {
        LlamaIndexRetrieveResponse response = new LlamaIndexRetrieveResponse()
                .setNodes(List.of(new LlamaIndexRetrieveResponse.Node()
                        .setId("doc-cache-1")
                        .setScore(0.93D)
                        .setBestMatchField("text")
                        .setMatchedTerms(List.of("缓存一致性", "延迟双删"))
                        .setMetadata(Map.of("source", "handbook", "scene", "mock_interview"))
                        .setText("缓存一致性可以从旁路缓存、延迟双删和消息补偿三个方向展开。")));

        String snippet = response.toContextSnippet();

        assertTrue(snippet.contains("<knowledge_item>"));
        assertTrue(snippet.contains("id=doc-cache-1"));
        assertTrue(snippet.contains("score=0.93"));
        assertTrue(snippet.contains("best_match_field=text"));
        assertTrue(snippet.contains("matched_terms=缓存一致性, 延迟双删"));
        assertTrue(snippet.contains("metadata="));
        assertTrue(snippet.contains("content:"));
        assertTrue(snippet.contains("</knowledge_item>"));
    }
}
