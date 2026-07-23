package com.xiaou.sre.client;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SrePrometheusQueryCatalogTest {

    private final SrePrometheusQueryCatalog catalog = new SrePrometheusQueryCatalog();

    @Test
    void unknownAlertUsesFixedDefaultQuery() {
        SrePrometheusQueryCatalog.QuerySpec spec = catalog.find("unknown\" or __name__=~\".*");

        assertThat(spec.sourceRef()).isEqualTo("target_up");
        assertThat(spec.promQl()).isEqualTo("up{job=\"code-nest\"}");
        assertThat(spec.promQl()).doesNotContain("unknown");
    }

    @Test
    void knownAlertMapsToAuditedQuery() {
        SrePrometheusQueryCatalog.QuerySpec spec = catalog.find("CodeNestHighHttpErrorRatio");

        assertThat(spec.sourceRef()).isEqualTo("http_5xx_rate");
        assertThat(spec.promQl()).contains("status=~\"5..\"");
        assertThat(catalog.isAllowed(spec)).isTrue();
        assertThat(catalog.isAllowed(new SrePrometheusQueryCatalog.QuerySpec(
                "arbitrary", "up{job=\"user-input\"}"))).isFalse();
    }
}
