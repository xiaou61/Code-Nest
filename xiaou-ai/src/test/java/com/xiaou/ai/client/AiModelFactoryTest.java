package com.xiaou.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.xiaou.common.config.AiProperties;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiModelFactoryTest {

    @Test
    void shouldSendGlobalAndPromptCompletionLimitsToOpenAiCompatibleEndpoint() throws Exception {
        List<String> requestBodies = new ArrayList<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            requestBodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = "{\"id\":\"test\",\"object\":\"chat.completion\",\"model\":\"test-model\",\"choices\":[{\"index\":0,\"message\":{\"role\":\"assistant\",\"content\":\"LIVE_CONTRACT_OK\"},\"finish_reason\":\"stop\"}]}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (exchange) {
                exchange.getResponseBody().write(response);
            }
        });
        server.start();

        try {
            AiProperties properties = new AiProperties();
            properties.setBaseUrl("http://localhost:" + server.getAddress().getPort() + "/v1");
            properties.setApiKey("test-key");
            properties.getModel().setChat("test-model");
            properties.getModel().setMaxCompletionTokens(321);
            properties.getTimeout().setReadMs(5000);
            properties.getRetry().setMaxAttempts(0);

            AiModelFactory factory = new AiModelFactory(properties);
            AiChatResult result = factory.chat("system", "user");
            AiChatResult boundedResult = factory.chat("system", "bounded-user", 123);

            assertEquals("LIVE_CONTRACT_OK", result.getContent());
            assertEquals("LIVE_CONTRACT_OK", boundedResult.getContent());
            assertEquals(2, requestBodies.size());

            JsonNode globalRequest = new ObjectMapper().readTree(requestBodies.get(0));
            assertEquals(321, globalRequest.path("max_completion_tokens").asInt());
            assertEquals("test-model", globalRequest.path("model").asText());
            assertTrue(globalRequest.path("messages").isArray());

            JsonNode promptRequest = new ObjectMapper().readTree(requestBodies.get(1));
            assertEquals(123, promptRequest.path("max_completion_tokens").asInt());
            assertEquals("bounded-user", promptRequest.path("messages").get(1).path("content").asText());
        } finally {
            server.stop(0);
        }
    }
}
