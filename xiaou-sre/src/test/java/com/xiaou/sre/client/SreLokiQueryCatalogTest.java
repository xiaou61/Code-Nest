package com.xiaou.sre.client;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SreLokiQueryCatalogTest {

    @Test
    void knownAlertUsesFixedLogQlAndUnknownFallsBackToDefault() {
        SreLokiQueryCatalog catalog = new SreLokiQueryCatalog();

        SreLokiQueryCatalog.QuerySpec known = catalog.find("CodeNestTargetDown");
        SreLokiQueryCatalog.QuerySpec fallback = catalog.find("user-controlled-alert");

        assertThat(known.sourceRef()).isEqualTo("application_errors");
        assertThat(known.logQl()).contains("job=\"code-nest\"");
        assertThat(fallback).isEqualTo(catalog.find(null));
        assertThat(catalog.isAllowed(known)).isTrue();
        assertThat(catalog.isAllowed(new SreLokiQueryCatalog.QuerySpec(
                "user", "{job=\"user\"}"))).isFalse();
    }
}
