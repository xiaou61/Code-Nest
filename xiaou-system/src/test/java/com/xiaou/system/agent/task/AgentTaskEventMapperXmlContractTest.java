package com.xiaou.system.agent.task;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentTaskEventMapperXmlContractTest {

    @Test
    void shouldReadOnlyOwnedEventsAfterStableCursor() throws Exception {
        Configuration configuration = new Configuration();
        try (InputStream input = Resources.getResourceAsStream("mapper/SysAgentTaskEventMapper.xml")) {
            new XMLMapperBuilder(input, configuration, "mapper/SysAgentTaskEventMapper.xml",
                    configuration.getSqlFragments()).parse();
        }

        BoundSql sql = configuration.getMappedStatement(
                "com.xiaou.system.mapper.SysAgentTaskEventMapper.selectOwnedAfter")
                .getBoundSql(Map.of("taskId", "task-1", "operatorId", 7L,
                        "afterCursor", 10L, "limit", 51));
        String normalized = sql.getSql().replaceAll("\\s+", " ").trim().toUpperCase(Locale.ROOT);

        assertTrue(normalized.contains("INNER JOIN SYS_AGENT_TASK TASK ON TASK.TASK_ID = EVENT.TASK_ID"), normalized);
        assertTrue(normalized.contains("TASK.OPERATOR_ID = ?"), normalized);
        assertTrue(normalized.contains("EVENT.ID > COALESCE(?, 0)"), normalized);
        assertTrue(normalized.contains("ORDER BY EVENT.ID ASC LIMIT ?"), normalized);
    }
}
