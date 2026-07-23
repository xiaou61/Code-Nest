package com.xiaou.sre.client;

import com.sun.net.httpserver.HttpServer;
import com.xiaou.sre.config.SreLokiProperties;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SreLokiClientTest {

    @Test
    void queryParsesAndTruncatesLokiStreams() throws Exception {
        SreLokiProperties properties = enabledProperties();
        properties.setMaxResults(1);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/loki/api/v1/query_range", exchange -> {
            assertThat(exchange.getRequestMethod()).isEqualTo("GET");
            String rawQuery = exchange.getRequestURI().getRawQuery();
            assertThat(rawQuery).contains("query=").contains("start=").contains("end=");
            String encodedQuery = rawQuery.substring(rawQuery.indexOf("query=") + "query=".length());
            int separator = encodedQuery.indexOf('&');
            if (separator >= 0) {
                encodedQuery = encodedQuery.substring(0, separator);
            }
            assertThat(URLDecoder.decode(encodedQuery, StandardCharsets.UTF_8))
                    .isEqualTo("{job=\"code-nest\"} |~ \"(?i)(error|exception|timeout)\"");
            byte[] response = """
                    {
                      "status": "success",
                      "data": {
                        "resultType": "streams",
                        "result": [
                          {"stream": {"job": "code-nest"}, "values": [["1710000000000000000", "first error Authorization: Bearer supersecret token=abc password=secret-value"]]},
                          {"stream": {"job": "code-nest"}, "values": [["1710000000000000001", "second error"]]}
                        ]
                      }
                    }
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (exchange) {
                exchange.getResponseBody().write(response);
            }
        });
        server.start();
        properties.setEndpoint("http://127.0.0.1:" + server.getAddress().getPort());

        try {
            SreLokiQueryCatalog catalog = new SreLokiQueryCatalog();
            SreLokiClient client = new SreLokiClient(properties, RestClient.builder(), catalog);
            Instant end = Instant.parse("2026-07-20T10:00:00Z");
            SreLokiQueryResult result = client.query(catalog.find("CodeNestTargetDown"),
                    end.minusSeconds(300), end);

            assertThat(result.getResultType()).isEqualTo("streams");
            assertThat(result.getResults()).hasSize(1);
            assertThat(result.isTruncated()).isTrue();
            assertThat(result.getResults().get(0).get("values").toString()).contains("first error", "[REDACTED]");
            assertThat(result.getResults().get(0).get("values").toString())
                    .doesNotContain("supersecret", "abc", "secret-value");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void invalidEndpointAndWindowAreRejectedBeforeNetworkCall() {
        SreLokiProperties properties = enabledProperties();
        properties.setEndpoint("file:///var/log");
        SreLokiQueryCatalog catalog = new SreLokiQueryCatalog();
        SreLokiClient client = new SreLokiClient(properties, RestClient.builder(), catalog);
        Instant end = Instant.now();

        assertThatThrownBy(() -> client.query(catalog.find("CodeNestTargetDown"),
                end.minusSeconds(60), end))
                .isInstanceOf(SreLokiException.class)
                .hasMessageContaining("HTTP 地址");

        properties.setEndpoint("http://127.0.0.1:3100");
        assertThatThrownBy(() -> client.query(catalog.find("CodeNestTargetDown"),
                end.minusSeconds(3_700), end))
                .isInstanceOf(SreLokiException.class)
                .hasMessageContaining("时间窗口");
    }

    private SreLokiProperties enabledProperties() {
        SreLokiProperties properties = new SreLokiProperties();
        properties.setEnabled(true);
        properties.setMaxRangeMinutes(60);
        return properties;
    }
}
