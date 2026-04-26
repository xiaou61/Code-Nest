package com.xiaou.ai.rag;

import com.xiaou.ai.prompt.AiPromptFixtures;
import com.xiaou.ai.prompt.AiRagQueryCatalog;
import com.xiaou.ai.prompt.AiRagQuerySpec;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiRagRetrievalProfileTest {

    @Test
    void shouldRegisterUniqueRetrievalProfileIds() {
        List<AiRagRetrievalProfile> profiles = AiRagRetrievalCatalog.all();
        Set<String> ids = profiles.stream()
                .map(AiRagRetrievalProfile::profileId)
                .collect(Collectors.toSet());

        assertEquals(profiles.size(), ids.size(), "RAG 检索画像不允许重复，避免 query 与 filter 绑定混乱");
    }

    @Test
    void shouldCoverAllRagQuerySpecs() {
        Set<String> queryIds = AiRagQueryCatalog.all().stream()
                .map(AiRagQuerySpec::queryId)
                .collect(Collectors.toSet());
        Set<String> profileIds = AiRagRetrievalCatalog.all().stream()
                .map(AiRagRetrievalProfile::profileId)
                .collect(Collectors.toSet());

        assertEquals(queryIds, profileIds, "每个 RAG query 都应有对应的检索画像");
    }

    @Test
    void shouldBuildRetrieveRequestsFromProfiles() {
        for (AiRagRetrievalProfile profile : AiRagRetrievalCatalog.all()) {
            String query = profile.querySpec().render(AiPromptFixtures.ragQueryVariables(profile.querySpec()));
            LlamaIndexRetrieveRequest request = profile.buildRequest(query);

            assertEquals(profile.scene(), request.getScene());
            assertEquals(profile.topK(), request.getTopK());
            assertEquals(profile.metadataFilters(), request.getMetadataFilters());
            assertTrue(request.getMetadataFilters().containsKey("domain"));
            assertTrue(request.getMetadataFilters().containsKey("task"));
            assertFalse(request.getQuery().contains("{{"));
        }
    }

    @Test
    void shouldKeepRetrievalProfilesBoundToQuerySpecs() {
        for (AiRagRetrievalProfile profile : AiRagRetrievalCatalog.all()) {
            assertNotNull(profile.querySpec());
            assertTrue(profile.topK() > 0);
            assertFalse(profile.scene().isBlank());
        }
    }
}
