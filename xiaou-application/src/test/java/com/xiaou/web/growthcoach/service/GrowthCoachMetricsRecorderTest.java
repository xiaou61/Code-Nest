package com.xiaou.web.growthcoach.service;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GrowthCoachMetricsRecorderTest {

    @Test
    void recordsActionAndConflictMetricsWithoutUserTags() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        GrowthCoachMetricsRecorder recorder = new GrowthCoachMetricsRecorder(registry);

        recorder.recordAction("growth.plan.adjust", "preview", "preview_created", 1_000_000L);
        recorder.recordConflict("growth.plan.adjust", "plan_version_or_task_snapshot");

        assertThat(registry.find("xiaou.growth_coach.action.runs")
                .tags("action_id", "growth.plan.adjust", "stage", "preview", "outcome", "preview_created")
                .counter().count()).isEqualTo(1D);
        assertThat(registry.find("xiaou.growth_coach.action.duration")
                .tags("action_id", "growth.plan.adjust", "stage", "preview", "outcome", "preview_created")
                .timer().count()).isEqualTo(1L);
        assertThat(registry.find("xiaou.growth_coach.action.conflicts")
                .tags("action_id", "growth.plan.adjust", "reason", "plan_version_or_task_snapshot")
                .counter().count()).isEqualTo(1D);
        assertThat(registry.getMeters().stream()
                .flatMap(meter -> meter.getId().getTags().stream())
                .noneMatch(tag -> "user_id".equals(tag.getKey()))).isTrue();
    }
}
