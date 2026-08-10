package com.xiaou.system.agent.task;

import com.xiaou.system.domain.SysAgentTask;
import com.xiaou.system.domain.SysAgentTaskStep;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentTaskMapperXmlContractTest {

    private static final String TASK_MAPPER = "mapper/SysAgentTaskMapper.xml";
    private static final String STEP_MAPPER = "mapper/SysAgentTaskStepMapper.xml";

    @Test
    void shouldGuardTaskClaimAndTransitionWithPersistedState() throws Exception {
        Configuration configuration = configuration(TASK_MAPPER);

        BoundSql claim = boundSql(configuration,
                "com.xiaou.system.mapper.SysAgentTaskMapper.claim",
                Map.of(
                        "taskId", "agent-task-1",
                        "leaseOwner", "worker-1",
                        "claimedAt", LocalDateTime.now()
                ));
        assertTrue(sql(claim).contains("WHERE TASK_ID = ? AND STATUS = 'QUEUED'"), sql(claim));

        SysAgentTask transition = new SysAgentTask();
        transition.setTaskId("agent-task-1");
        transition.setStatus(AgentTaskStatus.COMPLETED.name());
        transition.setExpectedStatus(AgentTaskStatus.RUNNING.name());
        transition.setExpectedLeaseOwner("worker-1");
        transition.setUpdatedTime(LocalDateTime.now());
        BoundSql guardedTransition = boundSql(configuration,
                "com.xiaou.system.mapper.SysAgentTaskMapper.transition", transition);

        assertTrue(sql(guardedTransition).contains("WHERE TASK_ID = ? AND STATUS = ? AND LEASE_OWNER = ?"),
                sql(guardedTransition));
        assertEquals(List.of("taskId", "expectedStatus", "expectedLeaseOwner"),
                tailProperties(guardedTransition, 3));

        transition.setExpectedLeaseOwner(null);
        BoundSql statusOnlyTransition = boundSql(configuration,
                "com.xiaou.system.mapper.SysAgentTaskMapper.transition", transition);
        assertFalse(sql(statusOnlyTransition).contains("AND LEASE_OWNER = ?"), sql(statusOnlyTransition));
        assertEquals(List.of("taskId", "expectedStatus"), tailProperties(statusOnlyTransition, 2));
    }

    @Test
    void shouldFenceStaleRecoveryWithTheObservedHeartbeatCutoff() throws Exception {
        Configuration configuration = configuration(TASK_MAPPER);
        SysAgentTask transition = new SysAgentTask();
        transition.setTaskId("agent-task-1");
        transition.setStatus(AgentTaskStatus.QUEUED.name());
        transition.setExpectedStatus(AgentTaskStatus.RUNNING.name());
        transition.setExpectedLeaseOwner("lease-1");
        transition.setUpdatedTime(LocalDateTime.now());
        LocalDateTime staleBefore = LocalDateTime.now().minusMinutes(5);

        BoundSql fenced = boundSql(configuration,
                "com.xiaou.system.mapper.SysAgentTaskMapper.transitionStale",
                Map.of("task", transition, "staleBefore", staleBefore));

        String sql = sql(fenced);
        assertTrue(sql.contains("WHERE TASK_ID = ? AND STATUS = ? AND LEASE_OWNER = ?"), sql);
        assertTrue(sql.contains("COALESCE(HEARTBEAT_AT, CLAIMED_AT, UPDATED_TIME) <= ?"), sql);
        assertEquals(List.of(
                        "task.taskId",
                        "task.expectedStatus",
                        "task.expectedLeaseOwner",
                        "staleBefore"
                ), tailProperties(fenced, 4));
    }

    @Test
    void shouldGuardStepTransitionByTaskOrderAndExpectedStatus() throws Exception {
        Configuration configuration = configuration(STEP_MAPPER);
        SysAgentTaskStep transition = new SysAgentTaskStep();
        transition.setTaskId("agent-task-1");
        transition.setStepOrder(2);
        transition.setStatus(AgentTaskStepStatus.COMPLETED.name());
        transition.setExpectedStatus(AgentTaskStepStatus.RUNNING.name());
        transition.setUpdatedTime(LocalDateTime.now());

        BoundSql boundSql = boundSql(configuration,
                "com.xiaou.system.mapper.SysAgentTaskStepMapper.transition", transition);

        assertTrue(sql(boundSql).contains("WHERE TASK_ID = ? AND STEP_ORDER = ? AND STATUS = ?"), sql(boundSql));
        assertEquals(List.of("taskId", "stepOrder", "expectedStatus"), tailProperties(boundSql, 3));
    }

    @Test
    void shouldRestrictCancellationToOwnerAndCancellableStates() throws Exception {
        Configuration configuration = configuration(TASK_MAPPER);
        BoundSql boundSql = boundSql(configuration,
                "com.xiaou.system.mapper.SysAgentTaskMapper.cancelOwned",
                Map.of(
                        "taskId", "agent-task-1",
                        "operatorId", 7L,
                        "reason", "stop",
                        "cancelledAt", LocalDateTime.now()
                ));

        String sql = sql(boundSql);
        assertTrue(sql.contains("WHERE TASK_ID = ? AND OPERATOR_ID = ?"), sql);
        assertTrue(sql.contains("STATUS IN ('QUEUED', 'RUNNING', 'WAITING_CONFIRMATION', 'WAITING_INPUT', 'PAUSED')"), sql);
    }

    @Test
    void shouldOnlyExposeQueuedTasksForWorkerClaims() throws Exception {
        Configuration configuration = configuration(TASK_MAPPER);
        BoundSql boundSql = boundSql(configuration,
                "com.xiaou.system.mapper.SysAgentTaskMapper.selectClaimableIds",
                Map.of("limit", 10));

        String sql = sql(boundSql);
        assertTrue(sql.contains("WHERE STATUS = 'QUEUED'"), sql);
        assertFalse(sql.contains("PAUSED"), sql);
        assertFalse(sql.contains("WAITING_INPUT"), sql);
    }

    private Configuration configuration(String resource) throws Exception {
        Configuration configuration = new Configuration();
        try (InputStream input = Resources.getResourceAsStream(resource)) {
            XMLMapperBuilder builder = new XMLMapperBuilder(
                    input,
                    configuration,
                    resource,
                    configuration.getSqlFragments()
            );
            builder.parse();
        }
        return configuration;
    }

    private BoundSql boundSql(Configuration configuration, String statementId, Object parameter) {
        MappedStatement statement = configuration.getMappedStatement(statementId);
        return statement.getBoundSql(parameter);
    }

    private List<String> tailProperties(BoundSql boundSql, int count) {
        List<String> properties = boundSql.getParameterMappings().stream()
                .map(ParameterMapping::getProperty)
                .toList();
        return properties.subList(properties.size() - count, properties.size());
    }

    private String sql(BoundSql boundSql) {
        return boundSql.getSql().replaceAll("\\s+", " ").trim().toUpperCase(Locale.ROOT);
    }
}
