package com.xiaou.system.agent.task;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AgentTaskPropertiesTest {

    @Test
    void shouldDefaultToDisabledAndBoundRuntimeLimits() {
        AgentTaskProperties properties = new AgentTaskProperties();

        assertFalse(properties.isEnabled());
        assertEquals(5, properties.normalizedMaxSteps());

        properties.setBatchSize(0);
        properties.setMaxSteps(100);
        properties.setLeaseSeconds(10);
        properties.setFixedDelayMs(10);
        properties.setMaxResultJsonChars(100);

        assertEquals(1, properties.normalizedBatchSize());
        assertEquals(10, properties.normalizedMaxSteps());
        assertEquals(90L, properties.normalizedLeaseSeconds());
        assertEquals(250L, properties.normalizedFixedDelayMs());
        assertEquals(1_000, properties.normalizedMaxResultJsonChars());
    }
}
