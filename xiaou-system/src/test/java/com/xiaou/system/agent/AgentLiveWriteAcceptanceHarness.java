package com.xiaou.system.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysql.cj.jdbc.MysqlDataSource;
import com.xiaou.ai.client.AiModelFactory;
import com.xiaou.ai.metrics.AiMetricsRecorder;
import com.xiaou.ai.metrics.AiRuntimeMetricsCollector;
import com.xiaou.ai.support.AiExecutionSupport;
import com.xiaou.common.config.AiProperties;
import com.xiaou.system.dto.AgentAuditResponse;
import com.xiaou.system.dto.AgentChatRequest;
import com.xiaou.system.dto.AgentChatResponse;
import com.xiaou.system.service.SysAgentAuditService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.LocalCacheScope;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

final class AgentLiveWriteAcceptanceHarness {

    private static final String ENABLE_ENV = "AGENT_LIVE_WRITE_TEST";
    private static final String BASE_URL_ENV = "XIAOU_AI_BASE_URL";
    private static final String API_KEY_ENV = "XIAOU_AI_API_KEY";
    private static final String MODEL_ENV = "XIAOU_AI_CHAT_MODEL";
    private static final String MYSQL_URL_ENV = "AGENT_TEST_MYSQL_URL";
    private static final String MYSQL_USERNAME_ENV = "AGENT_TEST_MYSQL_USERNAME";
    private static final String MYSQL_PASSWORD_ENV = "AGENT_TEST_MYSQL_PASSWORD";
    private static final String DEFAULT_MODEL = "gpt-5.5";
    private static final String AUDIT_MAPPER = "mapper/SysAgentAuditMapper.xml";

    private static final String AUDIT_SCHEMA = """
            CREATE TABLE sys_agent_audit (
              id BIGINT NOT NULL AUTO_INCREMENT,
              audit_id VARCHAR(80) NOT NULL,
              confirmation_id VARCHAR(120) NOT NULL,
              idempotency_key VARCHAR(160),
              user_message VARCHAR(1000),
              intent VARCHAR(120) NOT NULL,
              action_id VARCHAR(160) NOT NULL,
              route VARCHAR(200),
              risk_level VARCHAR(40),
              risk_category VARCHAR(80),
              status VARCHAR(20) NOT NULL DEFAULT 'PREVIEW',
              summary VARCHAR(500),
              payload_json TEXT,
              diff_json TEXT,
              plan_json TEXT,
              result_json TEXT,
              error_message VARCHAR(1000),
              operator_id BIGINT,
              operator_name VARCHAR(50),
              confirmed_time DATETIME,
              executed_time DATETIME,
              created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
              updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              PRIMARY KEY (id),
              UNIQUE KEY uk_agent_audit_id (audit_id),
              UNIQUE KEY uk_agent_idempotency_key (idempotency_key)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;

    private final AcceptanceConfig config;

    private AgentLiveWriteAcceptanceHarness(AcceptanceConfig config) {
        this.config = config;
    }

    static AgentLiveWriteAcceptanceHarness requireEnabled() {
        assumeTrue("true".equalsIgnoreCase(env(ENABLE_ENV)),
                "Set AGENT_LIVE_WRITE_TEST=true to run the live write acceptance test");
        AcceptanceConfig config = loadConfig();
        assumeTrue(config.isComplete(),
                "Live write acceptance requires AI and isolated MySQL environment variables");
        return new AgentLiveWriteAcceptanceHarness(config);
    }

    void withIsolatedDatabase(List<String> schemaStatements,
                              List<String> mapperResources,
                              DatabaseScenario scenario) throws Exception {
        String databaseName = "code_nest_agent_it_"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String serverUrl = normalizeServerUrl(config.mysqlUrl());
        createDatabase(serverUrl, databaseName);
        try {
            MysqlDataSource dataSource = dataSource(databaseUrl(serverUrl, databaseName));
            initializeSchema(dataSource, schemaStatements);
            SqlSessionFactory sessionFactory = buildSessionFactory(dataSource, mapperResources);
            try (SqlSession session = sessionFactory.openSession(true)) {
                scenario.run(new DatabaseContext(session, dataSource));
            }
        } finally {
            dropDatabase(serverUrl, databaseName);
        }
    }

    AgentChatOrchestrator orchestrator(List<AgentTool> tools, SysAgentAuditService auditService) {
        AgentToolRegistry registry = new AgentToolRegistry(tools);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        return orchestrator(registry, strictPlanResolver(registry, meterRegistry), auditService, meterRegistry);
    }

    LlmAgentPlanResolver strictPlanResolver(AgentToolRegistry registry, MeterRegistry meterRegistry) {
        AiProperties properties = new AiProperties();
        properties.setEnabled(true);
        properties.setProvider("openai-compatible");
        properties.setBaseUrl(config.aiBaseUrl());
        properties.setApiKey(config.aiApiKey());
        properties.getModel().setChat(config.aiModel());
        properties.getTimeout().setReadMs(60000);
        properties.getRetry().setMaxAttempts(2);

        AiMetricsRecorder metricsRecorder = new AiMetricsRecorder(
                meterRegistry,
                new AiRuntimeMetricsCollector(),
                properties
        );
        AiExecutionSupport executionSupport = new AiExecutionSupport(
                new AiModelFactory(properties),
                metricsRecorder,
                properties
        );
        return new LlmAgentPlanResolver(
                new DeterministicAgentPlanResolver(registry),
                registry,
                executionSupport,
                new ObjectMapper(),
                false
        );
    }

    AgentChatOrchestrator orchestrator(AgentToolRegistry registry,
                                       AgentPlanResolver planResolver,
                                       SysAgentAuditService auditService,
                                       MeterRegistry meterRegistry) {
        return new AgentChatOrchestrator(
                registry,
                planResolver,
                new AgentPolicyEngine(),
                auditService,
                new ObjectMapper(),
                new AgentSessionContextStore(),
                new AgentToolMetricsRecorder(meterRegistry)
        );
    }

    ConfirmedWriteResult executeConfirmedWrite(AgentChatOrchestrator orchestrator,
                                               SysAgentAuditService auditService,
                                               AgentOperator operator,
                                               String sessionId,
                                               String message,
                                               AgentTool expectedTool,
                                               Runnable previewStateAssertion) {
        AgentChatRequest previewRequest = new AgentChatRequest();
        previewRequest.setSessionId(sessionId);
        previewRequest.setMessage(message);
        AgentChatResponse preview = orchestrator.chat(previewRequest, operator);

        AgentToolDefinition definition = expectedTool.definition();
        assertNotNull(preview);
        assertEquals("confirm_required", preview.getStatus(), preview.getErrorMessage());
        assertEquals(definition.getName(), preview.getToolName());
        assertNotNull(preview.getAuditId());
        assertNotNull(preview.getConfirmation());
        assertNotNull(preview.getConfirmation().getRequiredText());
        assertFalse(preview.getConfirmation().getRequiredText().isBlank());
        assertEquals(definition.getConfirmationText(), preview.getConfirmation().getRequiredText());
        AgentAuditResponse previewAudit = auditService.getByAuditId(preview.getAuditId());
        assertNotNull(previewAudit);
        assertEquals("PREVIEW", previewAudit.getStatus());
        assertTrace(preview, "audit.preview_created", "done");

        previewStateAssertion.run();

        AgentChatRequest confirmRequest = new AgentChatRequest();
        confirmRequest.setSessionId(sessionId);
        confirmRequest.setAuditId(preview.getAuditId());
        confirmRequest.setConfirmationText(preview.getConfirmation().getRequiredText());
        AgentChatResponse executed = orchestrator.chat(confirmRequest, operator);

        assertNotNull(executed);
        assertEquals("executed", executed.getStatus(), executed.getErrorMessage());
        assertEquals(definition.getName(), executed.getToolName());
        assertEquals(preview.getAuditId(), executed.getAuditId());
        AgentAuditResponse audit = auditService.getByAuditId(preview.getAuditId());
        assertNotNull(audit);
        assertEquals("EXECUTED", audit.getStatus());
        assertNotNull(audit.getConfirmedTime());
        assertNotNull(audit.getExecutedTime());
        assertTrace(executed, "audit.confirmed", "done");
        assertTrace(executed, "tool.executed", "done");
        assertTrace(executed, "audit.result_recorded", "done");
        return new ConfirmedWriteResult(preview, executed, audit);
    }

    private void initializeSchema(MysqlDataSource dataSource, List<String> schemaStatements) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(AUDIT_SCHEMA);
            for (String schemaStatement : schemaStatements) {
                if (StringUtils.hasText(schemaStatement)) {
                    statement.execute(schemaStatement);
                }
            }
        }
    }

    private SqlSessionFactory buildSessionFactory(MysqlDataSource dataSource,
                                                  List<String> mapperResources) throws Exception {
        Environment environment = new Environment(
                "agent-live-write",
                new JdbcTransactionFactory(),
                dataSource
        );
        Configuration configuration = new Configuration(environment);
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setLocalCacheScope(LocalCacheScope.STATEMENT);

        Set<String> resources = new LinkedHashSet<>();
        resources.add(AUDIT_MAPPER);
        resources.addAll(mapperResources);
        for (String resource : resources) {
            parseMapper(configuration, resource);
        }
        return new SqlSessionFactoryBuilder().build(configuration);
    }

    private void parseMapper(Configuration configuration, String resource) throws Exception {
        try (InputStream input = Resources.getResourceAsStream(resource)) {
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
    }

    private MysqlDataSource dataSource(String url) {
        MysqlDataSource dataSource = new MysqlDataSource();
        dataSource.setURL(url);
        dataSource.setUser(config.mysqlUsername());
        dataSource.setPassword(config.mysqlPassword());
        return dataSource;
    }

    private void createDatabase(String serverUrl, String databaseName) throws Exception {
        try (Connection connection = connection(serverUrl);
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE `" + databaseName
                    + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        }
    }

    private void dropDatabase(String serverUrl, String databaseName) throws Exception {
        try (Connection connection = connection(serverUrl);
             Statement statement = connection.createStatement()) {
            statement.execute("DROP DATABASE IF EXISTS `" + databaseName + "`");
        }
    }

    private Connection connection(String url) throws Exception {
        return java.sql.DriverManager.getConnection(url, config.mysqlUsername(), config.mysqlPassword());
    }

    private void assertTrace(AgentChatResponse response, String stage, String status) {
        assertTrue(response.getTrace().stream().anyMatch(step ->
                        stage.equals(step.getStage()) && status.equals(step.getStatus())),
                () -> "Missing trace " + stage + "=" + status + ": " + response.getTrace());
    }

    private static AcceptanceConfig loadConfig() {
        String model = env(MODEL_ENV);
        return new AcceptanceConfig(
                env(BASE_URL_ENV),
                env(API_KEY_ENV),
                StringUtils.hasText(model) ? model : DEFAULT_MODEL,
                env(MYSQL_URL_ENV),
                env(MYSQL_USERNAME_ENV),
                env(MYSQL_PASSWORD_ENV)
        );
    }

    private static String env(String name) {
        String value = System.getenv(name);
        return value == null ? "" : value.trim();
    }

    private String normalizeServerUrl(String rawUrl) {
        String value = rawUrl.trim().replace("jdbc:p6spy:mysql:", "jdbc:mysql:");
        int queryIndex = value.indexOf('?');
        String query = queryIndex >= 0 ? value.substring(queryIndex) : "";
        String base = queryIndex >= 0 ? value.substring(0, queryIndex) : value;
        int pathIndex = base.indexOf('/', "jdbc:mysql://".length());
        if (pathIndex < 0) {
            return base + "/" + query;
        }
        return base.substring(0, pathIndex + 1) + query;
    }

    private String databaseUrl(String serverUrl, String databaseName) {
        int queryIndex = serverUrl.indexOf('?');
        if (queryIndex < 0) {
            return serverUrl + databaseName;
        }
        return serverUrl.substring(0, queryIndex) + databaseName + serverUrl.substring(queryIndex);
    }

    @FunctionalInterface
    interface DatabaseScenario {
        void run(DatabaseContext context) throws Exception;
    }

    record DatabaseContext(SqlSession session, MysqlDataSource dataSource) {

        <T> T mapper(Class<T> mapperType) {
            return session.getMapper(mapperType);
        }

        int queryForInt(String sql, Object... parameters) throws Exception {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                for (int i = 0; i < parameters.length; i++) {
                    statement.setObject(i + 1, parameters[i]);
                }
                try (ResultSet resultSet = statement.executeQuery()) {
                    assertTrue(resultSet.next(), "Expected scalar query to return one row");
                    return resultSet.getInt(1);
                }
            }
        }
    }

    record ConfirmedWriteResult(AgentChatResponse preview,
                                AgentChatResponse executed,
                                AgentAuditResponse audit) {
    }

    private record AcceptanceConfig(String aiBaseUrl,
                                    String aiApiKey,
                                    String aiModel,
                                    String mysqlUrl,
                                    String mysqlUsername,
                                    String mysqlPassword) {

        private boolean isComplete() {
            return StringUtils.hasText(aiBaseUrl)
                    && StringUtils.hasText(aiApiKey)
                    && StringUtils.hasText(aiModel)
                    && StringUtils.hasText(mysqlUrl)
                    && StringUtils.hasText(mysqlUsername)
                    && StringUtils.hasText(mysqlPassword);
        }
    }
}
