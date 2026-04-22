package com.xiaou.system.service.impl.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaou.common.config.AiProperties;
import com.xiaou.system.dto.AiRegressionRunResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AiRegressionRunStateRepositoryTest {

    private final AiRegressionRunStateRepository repository = new AiRegressionRunStateRepository(
            new ObjectMapper(),
            new AiProperties(),
            new StaticListableBeanFactory().getBeanProvider(StringRedisTemplate.class)
    );

    @Test
    void shouldKeepLatestRunAtTheFrontOfHistory() {
        AiRegressionRunResponse first = new AiRegressionRunResponse();
        first.setCaseId("community-summary-success");
        first.setExecutedAt(1713740800000L);

        AiRegressionRunResponse second = new AiRegressionRunResponse();
        second.setCaseId("sql-analyze-success");
        second.setExecutedAt(1713827200000L);

        repository.saveLatest(first);
        repository.saveLatest(second);

        List<AiRegressionRunResponse> history = repository.loadHistory(10);

        assertEquals(2, history.size());
        assertEquals("sql-analyze-success", history.get(0).getCaseId());
        assertEquals("community-summary-success", history.get(1).getCaseId());
    }

    @Test
    void shouldApplyHistoryLimitWhenReading() {
        AiRegressionRunResponse first = new AiRegressionRunResponse();
        first.setCaseId("community-summary-success");
        first.setExecutedAt(1713740800000L);

        AiRegressionRunResponse second = new AiRegressionRunResponse();
        second.setCaseId("sql-analyze-success");
        second.setExecutedAt(1713827200000L);

        repository.saveLatest(first);
        repository.saveLatest(second);

        List<AiRegressionRunResponse> history = repository.loadHistory(1);

        assertEquals(1, history.size());
        assertEquals("sql-analyze-success", history.get(0).getCaseId());
    }
}
