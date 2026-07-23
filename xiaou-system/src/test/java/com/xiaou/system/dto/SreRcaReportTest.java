package com.xiaou.system.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SreRcaReportTest {

    @Test
    void executionIsAlwaysDisabledByTheBackendContract() {
        SreRcaReport report = new SreRcaReport(
                11L,
                "SRE-001",
                "AI",
                "SUPPORTED",
                "HIGH",
                "只读分析结果",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                false,
                true,
                LocalDateTime.of(2026, 7, 23, 1, 10)
        );

        assertThat(report.executionAllowed()).isFalse();
    }
}
