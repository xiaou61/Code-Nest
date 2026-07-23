package com.xiaou.sre.client;

import com.xiaou.sre.config.SrePrometheusProperties;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SrePrometheusClientTest {

    @Test
    void queryParsesAndTruncatesPrometheusVectorResults() throws Exception {
        SrePrometheusProperties properties = enabledProperties();
        properties.setMaxResults(1);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/query", exchange -> {
            assertThat(exchange.getRequestMethod()).isEqualTo("GET");
            assertThat(exchange.getRequestURI().getRawQuery()).contains("query=");
            String query = URLDecoder.decode(exchange.getRequestURI().getRawQuery().substring("query=".length()),
                    StandardCharsets.UTF_8);
            assertThat(query).isEqualTo("up{job=\"code-nest\"}");
            byte[] response = """
                    {
                      "status": "success",
                      "data": {
                        "resultType": "vector",
                        "result": [
                          {"metric": {"__name__": "up", "instance": "app:9999"}, "value": [1710000000, "1"]},
                          {"metric": {"__name__": "up", "instance": "node:9100"}, "value": [1710000000, "1"]}
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
        RestClient.Builder builder = RestClient.builder();
        SrePrometheusQueryCatalog catalog = new SrePrometheusQueryCatalog();
        SrePrometheusClient client = new SrePrometheusClient(properties, builder, catalog);

        SrePrometheusQueryResult result = client.query(catalog.find("CodeNestTargetDown"));

        assertThat(result.getResultType()).isEqualTo("vector");
        assertThat(result.getResults()).hasSize(1);
        assertThat(result.isTruncated()).isTrue();
        assertThat(result.getResults().get(0).get("metric").toString()).contains("app:9999");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void invalidEndpointIsRejectedBeforeNetworkCall() {
        SrePrometheusProperties properties = enabledProperties();
        properties.setEndpoint("file:///etc/prometheus");
        RestClient.Builder builder = RestClient.builder();
        SrePrometheusClient client = new SrePrometheusClient(
                properties, builder, new SrePrometheusQueryCatalog());

        assertThatThrownBy(() -> client.query(new SrePrometheusQueryCatalog().find("CodeNestTargetDown")))
                .isInstanceOf(SrePrometheusException.class)
                .hasMessageContaining("HTTP 地址");
    }

    @Test
    void arbitraryQuerySpecIsRejected() {
        SrePrometheusProperties properties = enabledProperties();
        SrePrometheusClient client = new SrePrometheusClient(
                properties, RestClient.builder(), new SrePrometheusQueryCatalog());

        assertThatThrownBy(() -> client.query(new SrePrometheusQueryCatalog.QuerySpec(
                "user", "up{job=\"user-input\"}")))
                .isInstanceOf(SrePrometheusException.class)
                .hasMessageContaining("白名单");
    }

    private SrePrometheusProperties enabledProperties() {
        SrePrometheusProperties properties = new SrePrometheusProperties();
        properties.setEnabled(true);
        properties.setEndpoint("http://127.0.0.1:9090");
        return properties;
    }
}
