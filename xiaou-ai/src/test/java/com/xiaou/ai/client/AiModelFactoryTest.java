package com.xiaou.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.xiaou.common.config.AiProperties;
import com.xiaou.common.exception.ai.AiConfigurationException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
            properties.getModel().setReasoningEffort("max");
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
            assertEquals("max", globalRequest.path("reasoning_effort").asText());
            assertTrue(globalRequest.path("messages").isArray());

            JsonNode promptRequest = new ObjectMapper().readTree(requestBodies.get(1));
            assertEquals(123, promptRequest.path("max_completion_tokens").asInt());
            assertEquals("max", promptRequest.path("reasoning_effort").asText());
            assertEquals("bounded-user", promptRequest.path("messages").get(1).path("content").asText());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldRejectUnsupportedReasoningEffort() {
        AiProperties properties = new AiProperties();
        properties.setBaseUrl("http://localhost:1/v1");
        properties.setApiKey("test-key");
        properties.getModel().setChat("test-model");
        properties.getModel().setReasoningEffort("fastest");

        AiConfigurationException error = assertThrows(
                AiConfigurationException.class,
                () -> new AiModelFactory(properties).getChatModel()
        );

        assertTrue(error.getMessage().contains("reasoning effort"));
    }

    @Test
    void shouldRouteOpenAiRequestsThroughConfiguredProxy() throws Exception {
        List<String> requestUris = new ArrayList<>();
        HttpServer proxy = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        proxy.createContext("/", exchange -> {
            requestUris.add(exchange.getRequestURI().toString());
            byte[] response = "{\"id\":\"test\",\"object\":\"chat.completion\",\"model\":\"test-model\",\"choices\":[{\"index\":0,\"message\":{\"role\":\"assistant\",\"content\":\"PROXY_OK\"},\"finish_reason\":\"stop\"}]}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (exchange) {
                exchange.getResponseBody().write(response);
            }
        });
        proxy.start();

        try {
            AiProperties properties = new AiProperties();
            properties.setBaseUrl("http://unreachable.invalid/v1");
            properties.setApiKey("test-key");
            properties.setProxyUrl("http://localhost:" + proxy.getAddress().getPort());
            properties.getModel().setChat("test-model");
            properties.getTimeout().setConnectMs(1000);
            properties.getTimeout().setReadMs(5000);
            properties.getRetry().setMaxAttempts(0);

            AiChatResult result = new AiModelFactory(properties).chat("system", "user");

            assertEquals("PROXY_OK", result.getContent());
            assertEquals(1, requestUris.size());
            assertEquals("http://unreachable.invalid/v1/chat/completions", requestUris.get(0));
        } finally {
            proxy.stop(0);
        }
    }

    @Test
    void shouldRejectUnsupportedProxyUrl() {
        AiProperties properties = new AiProperties();
        properties.setBaseUrl("http://localhost:1/v1");
        properties.setApiKey("test-key");
        properties.setProxyUrl("socks5://localhost:1080");
        properties.getModel().setChat("test-model");

        AiConfigurationException error = assertThrows(
                AiConfigurationException.class,
                () -> new AiModelFactory(properties).getChatModel()
        );

        assertTrue(error.getMessage().contains("proxy URL"));
    }
}
