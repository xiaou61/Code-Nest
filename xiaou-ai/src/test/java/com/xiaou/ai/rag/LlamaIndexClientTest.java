package com.xiaou.ai.rag;

import com.xiaou.common.config.AiProperties;
import com.xiaou.common.exception.ai.AiRetrievalException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class LlamaIndexClientTest {

    @Test
    void shouldRetrieveNodesFromGateway() {
        AiProperties properties = buildAiProperties();
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        LlamaIndexClient client = new LlamaIndexClient(properties, builder);

        server.expect(requestTo("http://localhost:18080/api/v1/retrieve"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andRespond(withSuccess("""
                        {
                          "query": "filesort optimize",
                          "nodes": [
                            {
                              "id": "doc-1",
                              "score": 0.91,
                              "text": "Use composite index to avoid filesort",
                              "metadata": {
                                "source": "mysql-manual"
                              },
                              "matchedTerms": ["filesort", "index"],
                              "scoreBreakdown": {
                                "textTermScore": 2.73,
                                "termCoverage": 1.2
                              },
                              "bestMatchField": "text"
                            }
                          ],
                          "fallback": false
                        }
                        """, MediaType.APPLICATION_JSON));

        LlamaIndexRetrieveResponse response = client.retrieve(new LlamaIndexRetrieveRequest()
                .setQuery("filesort optimize")
                .setScene("sql_optimize")
                .setTopK(3));

        assertEquals("filesort optimize", response.getQuery());
        assertEquals(1, response.getNodes().size());
        assertEquals("doc-1", response.getNodes().get(0).getId());
        assertEquals("text", response.getNodes().get(0).getBestMatchField());
        assertTrue(response.getNodes().get(0).getMatchedTerms().contains("filesort"));
        assertTrue(response.getNodes().get(0).getScoreBreakdown().containsKey("textTermScore"));
        server.verify();
    }

    @Test
    void shouldReadGatewayHealth() {
        AiProperties properties = buildAiProperties();
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        LlamaIndexClient client = new LlamaIndexClient(properties, builder);

        server.expect(requestTo("http://localhost:18080/health"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andRespond(withSuccess("""
                        {
                          "status": "ok",
                          "authEnabled": true,
                          "documentCount": 4,
                          "sceneCount": 3,
                          "dataFile": "data/knowledge-base.json"
                        }
                        """, MediaType.APPLICATION_JSON));

        LlamaIndexHealthResponse response = client.health();

        assertEquals("ok", response.getStatus());
        assertTrue(response.isAuthEnabled());
        assertEquals(4, response.getDocumentCount());
        server.verify();
    }

    @Test
    void shouldListDocumentsFromGateway() {
        AiProperties properties = buildAiProperties();
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        LlamaIndexClient client = new LlamaIndexClient(properties, builder);

        server.expect(requestTo("http://localhost:18080/api/v1/admin/documents?scene=sql_optimize&limit=20"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andRespond(withSuccess("""
                        {
                          "totalCount": 1,
                          "documents": [
                            {
                              "id": "doc-1",
                              "scene": "sql_optimize",
                              "textPreview": "Using filesort 往往意味着排序未命中索引",
                              "metadata": {
                                "source": "mysql-manual"
                              }
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        LlamaIndexDocumentListResponse response = client.listDocuments("sql_optimize", 20);

        assertEquals(1, response.getTotalCount());
        assertEquals("doc-1", response.getDocuments().get(0).getId());
        server.verify();
    }

    @Test
    void shouldImportDocumentsToGateway() {
        AiProperties properties = buildAiProperties();
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        LlamaIndexClient client = new LlamaIndexClient(properties, builder);

        server.expect(requestTo("http://localhost:18080/api/v1/admin/documents/import"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andRespond(withSuccess("""
                        {
                          "importedCount": 2,
                          "totalCount": 10
                        }
                        """, MediaType.APPLICATION_JSON));

        LlamaIndexDocumentImportResponse response = client.importDocuments(new LlamaIndexDocumentImportRequest()
                .setReplace(false)
                .setDocuments(java.util.List.of(
                        new LlamaIndexKnowledgeDocument().setId("doc-1").setText("A"),
                        new LlamaIndexKnowledgeDocument().setId("doc-2").setText("B")
                )));

        assertEquals(2, response.getImportedCount());
        assertEquals(10, response.getTotalCount());
        server.verify();
    }

    @Test
    void shouldThrowAiRetrievalExceptionWhenGatewayFailed() {
        AiProperties properties = buildAiProperties();
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        LlamaIndexClient client = new LlamaIndexClient(properties, builder);

        server.expect(requestTo("http://localhost:18080/api/v1/retrieve"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR));

        assertThrows(AiRetrievalException.class, () -> client.retrieve(new LlamaIndexRetrieveRequest()
                .setQuery("filesort optimize")
                .setScene("sql_optimize")));
        server.verify();
    }

    private AiProperties buildAiProperties() {
        AiProperties properties = new AiProperties();
        properties.getRag().setEnabled(true);
        properties.getRag().setEndpoint("http://localhost:18080");
        properties.getRag().setApiKey("test-key");
        properties.getRag().setDefaultTopK(5);
        return properties;
    }
}
