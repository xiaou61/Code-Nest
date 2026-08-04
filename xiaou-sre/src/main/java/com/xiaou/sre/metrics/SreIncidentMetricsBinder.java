package com.xiaou.sre.metrics;

import com.xiaou.sre.mapper.SreIncidentMapper;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Exposes the current open incident count without caching a stale value. */
@Component
@RequiredArgsConstructor
public class SreIncidentMetricsBinder implements MeterBinder {

    private final SreIncidentMapper incidentMapper;

    @Override
    public void bindTo(MeterRegistry registry) {
        Gauge.builder("xiaou.sre.incidents.open", incidentMapper, this::countOpen)
                .description("Current incidents that still require attention")
                .register(registry);
    }

    private double countOpen(SreIncidentMapper mapper) {
        try {
            return Math.max(0L, mapper.countOpen());
        } catch (RuntimeException ignored) {
            // A metrics scrape must not take down the application when MySQL is unavailable.
            return 0D;
        }
    }
}
