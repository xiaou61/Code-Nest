package com.xiaou.system.agent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaou.chat.domain.ChatUserBan;
import com.xiaou.chat.mapper.ChatRoomMapper;
import com.xiaou.chat.mapper.ChatUserBanMapper;
import com.xiaou.chat.service.ChatRoomService;
import com.xiaou.chat.service.ChatUserBanService;
import com.xiaou.chat.service.impl.ChatRoomServiceImpl;
import com.xiaou.chat.service.impl.ChatUserBanServiceImpl;
import com.xiaou.points.domain.LotteryPrizeConfig;
import com.xiaou.points.domain.LotteryStatisticsDaily;
import com.xiaou.points.mapper.LotteryAdjustHistoryMapper;
import com.xiaou.points.mapper.LotteryDrawRecordMapper;
import com.xiaou.points.mapper.LotteryPrizeConfigMapper;
import com.xiaou.points.mapper.LotteryStatisticsDailyMapper;
import com.xiaou.points.mapper.UserLotteryLimitMapper;
import com.xiaou.points.service.LotteryAdminService;
import com.xiaou.points.service.LotteryNormalizeService;
import com.xiaou.points.service.LotteryStockService;
import com.xiaou.points.service.impl.LotteryAdminServiceImpl;
import com.xiaou.points.service.impl.LotteryNormalizeServiceImpl;
import com.xiaou.points.service.impl.LotteryStockServiceImpl;
import com.xiaou.system.agent.AgentLiveWriteAcceptanceHarness.DatabaseContext;
import com.xiaou.system.domain.SysAgentAudit;
import com.xiaou.system.domain.SysOperationLog;
import com.xiaou.system.dto.AgentAuditResponse;
import com.xiaou.system.dto.AgentChatRequest;
import com.xiaou.system.dto.AgentChatResponse;
import com.xiaou.system.mapper.SysAgentAuditMapper;
import com.xiaou.system.mapper.SysOperationLogMapper;
import com.xiaou.system.service.SysAgentAuditService;
import com.xiaou.system.service.SysOperationLogService;
import com.xiaou.system.service.impl.SysAgentAuditServiceImpl;
import com.xiaou.system.service.impl.SysOperationLogServiceImpl;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentRuntimeLiveWriteAcceptanceTest {

    private static final int OPERATION_LOG_RETENTION_DAYS = 30;
    private static final long LIVE_USER_ID = 88L;
    private static final String FAILED_AUDIT_ID = "agent-audit-1";
    private static final String FIXTURE_PATH = "/agent/admin-agent-planner-regression-cases.json";
    private static final TypeReference<List<PlannerRegressionCase>> CASE_LIST_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldRouteAndExecuteEveryRegisteredToolWithLiveAiAndRealBackendServices() throws Exception {
        AgentLiveWriteAcceptanceHarness harness = AgentLiveWriteAcceptanceHarness.requireEnabled();
        harness.withIsolatedDatabase(schemaStatements(), mapperResources(), context -> {
            RealServices services = realServices(context);
            SeededFixtures fixtures = seedFixtures(context, services);

            try (LiveRuntime runtime = liveRuntime(harness, services)) {
                List<PlannerRegressionCase> scenarios = resolvedScenarios();
                Set<String> registeredTools = runtime.registry().definitions().stream()
                        .map(AgentToolDefinition::getName)
                        .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
                Set<String> coveredTools = scenarios.stream()
                        .map(testCase -> testCase.expectedToolName)
                        .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

                assertEquals(26, registeredTools.size(), "Live acceptance must use the complete production tool registry");
                assertEquals(registeredTools, coveredTools, "Every registered production tool needs one live scenario");

                for (PlannerRegressionCase scenario : scenarios) {
                    AgentChatResponse response = runtime.orchestrator().chat(
                            request("live-all-" + scenario.id, scenario.message),
                            runtime.operator()
                    );
                    assertEquals(scenario.expectedToolName, response.getToolName(), diagnostics(scenario, response));

                    AgentToolDefinition definition = runtime.registry().find(scenario.expectedToolName)
                            .orElseThrow()
                            .definition();
                    if (requiresConfirmation(definition)) {
                        confirmAndAssertWrite(runtime, services, fixtures, scenario, response);
                    } else {
                        assertEquals("answered", response.getStatus(), diagnostics(scenario, response));
                        assertTrue(hasTrace(response, "tool.executed", "done"), diagnostics(scenario, response));
                        assertFalse(response.getArtifacts().isEmpty(), diagnostics(scenario, response));
                    }
                }

                AgentChatResponse clarification = runtime.orchestrator().chat(
                        request("live-missing-required-input", "请解除当前用户的禁言，但我没有提供用户编号。"),
                        runtime.operator()
                );
                assertEquals("rejected", clarification.getStatus(), clarification.getAnswer());
                assertEquals(AgentChatErrorCode.PLAN_CLARIFICATION_REQUIRED.code(), clarification.getErrorCode());

                assertNull(services.chatUserBanService().getActiveBan(LIVE_USER_ID));
                assertNull(services.operationLogMapper().selectById(fixtures.expiredOperationLog().getId()));
                assertNotNull(services.operationLogMapper().selectById(fixtures.recentOperationLog().getId()));
            }
        });
    }

    private LiveRuntime liveRuntime(AgentLiveWriteAcceptanceHarness harness, RealServices services) {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        AnnotationConfigApplicationContext spring = new AnnotationConfigApplicationContext();
        spring.registerBean(ObjectMapper.class, () -> objectMapper);
        spring.registerBean(MeterRegistry.class, () -> meterRegistry);
        spring.registerBean(SysAgentAuditService.class, services::auditService);
        spring.registerBean(SysOperationLogService.class, services::operationLogService);
        spring.registerBean(ChatUserBanService.class, services::chatUserBanService);
        spring.registerBean(LotteryAdminService.class, services::lotteryAdminService);
        spring.registerBean(AgentSessionProperties.class, AgentSessionProperties::new);
        spring.registerBean(AgentPolicyEngine.class, AgentPolicyEngine::new);
        spring.registerBean(AgentToolCatalogService.class);
        spring.registerBean(AgentToolRegistry.class);
        spring.scan("com.xiaou.system.agent.tools");
        spring.refresh();

        AgentToolRegistry registry = spring.getBean(AgentToolRegistry.class);
        LlmAgentPlanResolver strictResolver = harness.strictPlanResolver(registry, meterRegistry);
        spring.getBeanFactory().registerSingleton("liveAgentPlanResolver", strictResolver);
        assertSame(strictResolver, spring.getBeanProvider(AgentPlanResolver.class).getIfAvailable());

        AgentChatOrchestrator orchestrator = harness.orchestrator(
                registry,
                strictResolver,
                services.auditService(),
                meterRegistry
        );
        return new LiveRuntime(orchestrator, registry, operator(registry), spring);
    }

    private RealServices realServices(DatabaseContext context) {
        ChatRoomMapper roomMapper = context.mapper(ChatRoomMapper.class);
        ChatUserBanMapper banMapper = context.mapper(ChatUserBanMapper.class);
        SysOperationLogMapper operationLogMapper = context.mapper(SysOperationLogMapper.class);
        SysAgentAuditMapper auditMapper = context.mapper(SysAgentAuditMapper.class);

        ChatRoomService roomService = new ChatRoomServiceImpl(roomMapper);
        ChatUserBanService chatUserBanService = new ChatUserBanServiceImpl(banMapper, roomService);
        SysOperationLogService operationLogService = new SysOperationLogServiceImpl(operationLogMapper);
        SysAgentAuditService auditService = new SysAgentAuditServiceImpl(auditMapper);

        LotteryPrizeConfigMapper prizeConfigMapper = context.mapper(LotteryPrizeConfigMapper.class);
        LotteryDrawRecordMapper drawRecordMapper = context.mapper(LotteryDrawRecordMapper.class);
        LotteryStatisticsDailyMapper statisticsMapper = context.mapper(LotteryStatisticsDailyMapper.class);
        LotteryAdjustHistoryMapper adjustHistoryMapper = context.mapper(LotteryAdjustHistoryMapper.class);
        UserLotteryLimitMapper userLimitMapper = context.mapper(UserLotteryLimitMapper.class);
        LotteryNormalizeService normalizeService = new LotteryNormalizeServiceImpl(prizeConfigMapper);
        LotteryStockService stockService = new LotteryStockServiceImpl(prizeConfigMapper, null);
        LotteryAdminService lotteryAdminService = new LotteryAdminServiceImpl(
                prizeConfigMapper,
                drawRecordMapper,
                statisticsMapper,
                adjustHistoryMapper,
                userLimitMapper,
                normalizeService,
                stockService
        );

        return new RealServices(
                auditService,
                operationLogService,
                chatUserBanService,
                lotteryAdminService,
                auditMapper,
                operationLogMapper,
                banMapper,
                prizeConfigMapper,
                statisticsMapper
        );
    }

    private SeededFixtures seedFixtures(DatabaseContext context, RealServices services) throws Exception {
        ChatUserBan seededBan = seedActiveBan(services.chatUserBanMapper(), LIVE_USER_ID);
        SysOperationLog expiredLog = seedOperationLog(
                services.operationLogMapper(),
                "expired",
                LocalDateTime.now().minusDays(OPERATION_LOG_RETENTION_DAYS + 15L)
        );
        SysOperationLog recentLog = seedOperationLog(
                services.operationLogMapper(),
                "recent",
                LocalDateTime.now().minusDays(2)
        );
        seedFailedAudit(services.auditMapper());
        seedLottery(services.prizeConfigMapper(), services.statisticsMapper());

        assertEquals(1, context.queryForInt("SELECT COUNT(*) FROM chat_user_bans WHERE id = ?", seededBan.getId()));
        return new SeededFixtures(seededBan, expiredLog, recentLog);
    }

    private void confirmAndAssertWrite(LiveRuntime runtime,
                                       RealServices services,
                                       SeededFixtures fixtures,
                                       PlannerRegressionCase scenario,
                                       AgentChatResponse preview) {
        assertEquals("confirm_required", preview.getStatus(), diagnostics(scenario, preview));
        assertNotNull(preview.getAuditId());
        assertNotNull(preview.getConfirmation());
        assertNotNull(preview.getConfirmation().getRequiredText());

        if ("chat.userBan.unban".equals(scenario.expectedToolName)) {
            assertNotNull(services.chatUserBanService().getActiveBan(LIVE_USER_ID));
        } else if ("system.operationLog.cleanExpired".equals(scenario.expectedToolName)) {
            assertNotNull(services.operationLogMapper().selectById(fixtures.expiredOperationLog().getId()));
            assertNotNull(services.operationLogMapper().selectById(fixtures.recentOperationLog().getId()));
        }

        AgentChatRequest confirmation = new AgentChatRequest();
        confirmation.setSessionId("live-confirm-" + scenario.id);
        confirmation.setAuditId(preview.getAuditId());
        confirmation.setConfirmationText(preview.getConfirmation().getRequiredText());
        AgentChatResponse executed = runtime.orchestrator().chat(confirmation, runtime.operator());

        assertEquals("executed", executed.getStatus(), diagnostics(scenario, executed));
        assertEquals(scenario.expectedToolName, executed.getToolName());
        assertTrue(hasTrace(executed, "audit.confirmed", "done"));
        assertTrue(hasTrace(executed, "tool.executed", "done"));
        assertTrue(hasTrace(executed, "audit.result_recorded", "done"));
        AgentAuditResponse audit = services.auditService().getByAuditId(preview.getAuditId());
        assertNotNull(audit);
        assertEquals("EXECUTED", audit.getStatus());
    }

    private AgentOperator operator(AgentToolRegistry registry) {
        Set<String> permissions = new LinkedHashSet<>();
        Set<String> roles = new LinkedHashSet<>(List.of("ADMIN", "SUPER_ADMIN"));
        for (AgentToolDefinition definition : registry.definitions()) {
            if (definition.getRequiredPermissions() != null) {
                permissions.addAll(definition.getRequiredPermissions());
            }
            if (definition.getRequiredRoles() != null) {
                roles.addAll(definition.getRequiredRoles());
            }
        }
        return new AgentOperator(
                100L,
                "live-all-tools-admin",
                "live-test-tenant",
                new ArrayList<>(roles),
                new ArrayList<>(permissions)
        );
    }

    private List<PlannerRegressionCase> resolvedScenarios() throws IOException {
        Map<String, PlannerRegressionCase> firstByTool = new LinkedHashMap<>();
        for (PlannerRegressionCase testCase : loadCases()) {
            if ("RESOLVED".equals(testCase.expectedStatus)) {
                firstByTool.putIfAbsent(testCase.expectedToolName, testCase);
            }
        }
        return new ArrayList<>(firstByTool.values());
    }

    private List<PlannerRegressionCase> loadCases() throws IOException {
        InputStream inputStream = getClass().getResourceAsStream(FIXTURE_PATH);
        assertNotNull(inputStream, "Missing fixture: " + FIXTURE_PATH);
        try (inputStream) {
            return objectMapper.readValue(inputStream, CASE_LIST_TYPE);
        }
    }

    private AgentChatRequest request(String sessionId, String message) {
        AgentChatRequest request = new AgentChatRequest();
        request.setSessionId(sessionId);
        request.setMessage(message);
        return request;
    }

    private boolean requiresConfirmation(AgentToolDefinition definition) {
        return definition.isConfirmationRequired()
                || definition.isDestructive()
                || !"readonly".equalsIgnoreCase(definition.getRiskLevel())
                || !"READONLY".equalsIgnoreCase(definition.getRiskCategory());
    }

    private boolean hasTrace(AgentChatResponse response, String stage, String status) {
        return response.getTrace().stream().anyMatch(step ->
                stage.equals(step.getStage()) && status.equals(step.getStatus()));
    }

    private String diagnostics(PlannerRegressionCase scenario, AgentChatResponse response) {
        return scenario.id + ": status=" + response.getStatus()
                + ", tool=" + response.getToolName()
                + ", error=" + response.getErrorCode()
                + ", answer=" + response.getAnswer();
    }

    private ChatUserBan seedActiveBan(ChatUserBanMapper mapper, long userId) {
        ChatUserBan ban = new ChatUserBan();
        ban.setUserId(userId);
        ban.setRoomId(1L);
        ban.setBanReason("agent live all-tools acceptance fixture");
        ban.setBanStartTime(new java.util.Date());
        ban.setBanEndTime(null);
        ban.setOperatorId(100L);
        ban.setStatus(1);
        assertEquals(1, mapper.insert(ban));
        assertNotNull(ban.getId());
        return ban;
    }

    private SysOperationLog seedOperationLog(SysOperationLogMapper mapper,
                                             String fixtureType,
                                             LocalDateTime operationTime) {
        SysOperationLog operationLog = new SysOperationLog();
        operationLog.setOperationId("agent-live-" + fixtureType + "-" + UUID.randomUUID());
        operationLog.setModule("agent-live-all-tools");
        operationLog.setOperationType("DELETE");
        operationLog.setDescription("isolated " + fixtureType + " operation log fixture");
        operationLog.setMethod("AgentRuntimeLiveWriteAcceptanceTest");
        operationLog.setRequestUri("/test/agent/live-all-tools");
        operationLog.setRequestMethod("POST");
        operationLog.setOperatorId(100L);
        operationLog.setOperatorName("live-all-tools-admin");
        operationLog.setStatus(0);
        operationLog.setOperationTime(operationTime);
        operationLog.setCostTime(1L);
        assertEquals(1, mapper.insert(operationLog));
        assertNotNull(operationLog.getId());
        return operationLog;
    }

    private void seedFailedAudit(SysAgentAuditMapper mapper) {
        LocalDateTime now = LocalDateTime.now();
        SysAgentAudit audit = new SysAgentAudit();
        audit.setAuditId(FAILED_AUDIT_ID);
        audit.setConfirmationId("failed-confirmation-1");
        audit.setIdempotencyKey("failed-idempotency-1");
        audit.setUserMessage("历史失败写入请求");
        audit.setIntent("chat.userBan.unban");
        audit.setActionId("chat.userBan.unban");
        audit.setRoute("/admin/agent/chat");
        audit.setRiskLevel("medium");
        audit.setRiskCategory("WRITE");
        audit.setStatus("FAILED");
        audit.setSummary("历史写入执行失败");
        audit.setPayloadJson("{\"userId\":999}");
        audit.setDiffJson("[]");
        audit.setPlanJson("[]");
        audit.setResultJson("{\"summary\":\"历史写入执行失败\",\"artifacts\":[],\"nextActions\":[]}");
        audit.setErrorMessage("isolated backend failure");
        audit.setOperatorId(100L);
        audit.setOperatorName("live-all-tools-admin");
        audit.setConfirmedTime(now.minusMinutes(1));
        audit.setExecutedTime(now);
        audit.setCreatedTime(now.minusMinutes(2));
        audit.setUpdatedTime(now);
        assertEquals(1, mapper.insert(audit));
    }

    private void seedLottery(LotteryPrizeConfigMapper prizeMapper,
                             LotteryStatisticsDailyMapper statisticsMapper) {
        LotteryStatisticsDaily statistics = new LotteryStatisticsDaily();
        statistics.setStatDate(LocalDate.now());
        statistics.setTotalDrawCount(9);
        statistics.setTotalCostPoints(90L);
        statistics.setTotalRewardPoints(30L);
        statistics.setPlatformProfitPoints(60L);
        statistics.setActualReturnRate(new BigDecimal("0.3333"));
        statistics.setUniqueUserCount(3);
        statistics.setSpecialPrizeCount(0);
        statistics.setFirstPrizeCount(0);
        statistics.setSecondPrizeCount(0);
        statistics.setThirdPrizeCount(0);
        statistics.setFourthPrizeCount(0);
        statistics.setFifthPrizeCount(0);
        statistics.setSixthPrizeCount(0);
        statistics.setNoPrizeCount(9);
        assertEquals(1, statisticsMapper.insert(statistics));

        LotteryPrizeConfig prize = new LotteryPrizeConfig();
        prize.setPrizeName("Live acceptance prize");
        prize.setPrizeLevel(8);
        prize.setPrizePoints(0);
        prize.setBaseProbability(BigDecimal.ONE);
        prize.setCurrentProbability(BigDecimal.ONE);
        prize.setTargetReturnRate(BigDecimal.ZERO);
        prize.setMaxReturnRate(BigDecimal.ONE);
        prize.setMinReturnRate(BigDecimal.ZERO);
        prize.setDailyStock(-1);
        prize.setTotalStock(-1);
        prize.setCurrentStock(-1);
        prize.setDisplayOrder(1);
        prize.setPrizeIcon("live");
        prize.setPrizeDesc("isolated live acceptance fixture");
        prize.setAdjustStrategy("FIXED");
        prize.setIsActive(1);
        assertEquals(1, prizeMapper.insert(prize));
    }

    private List<String> mapperResources() {
        return List.of(
                "mapper/ChatRoomMapper.xml",
                "mapper/ChatUserBanMapper.xml",
                "mapper/SysOperationLogMapper.xml",
                "mapper/LotteryPrizeConfigMapper.xml",
                "mapper/LotteryDrawRecordMapper.xml",
                "mapper/LotteryStatisticsDailyMapper.xml",
                "mapper/LotteryAdjustHistoryMapper.xml",
                "mapper/UserLotteryLimitMapper.xml"
        );
    }

    private List<String> schemaStatements() {
        return List.of(
                """
                        CREATE TABLE chat_rooms (
                          id BIGINT NOT NULL AUTO_INCREMENT,
                          room_name VARCHAR(100) NOT NULL,
                          room_type TINYINT DEFAULT 1,
                          description VARCHAR(500),
                          max_users INT DEFAULT 0,
                          status TINYINT DEFAULT 1,
                          create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                          update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                          PRIMARY KEY (id)
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                        """,
                """
                        CREATE TABLE chat_user_bans (
                          id BIGINT NOT NULL AUTO_INCREMENT,
                          user_id BIGINT NOT NULL,
                          room_id BIGINT NOT NULL,
                          ban_reason VARCHAR(500),
                          ban_start_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                          ban_end_time DATETIME NULL,
                          operator_id BIGINT,
                          status TINYINT DEFAULT 1,
                          create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                          update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                          PRIMARY KEY (id),
                          INDEX idx_user_room_status (user_id, room_id, status)
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                        """,
                """
                        CREATE TABLE sys_operation_log (
                          id BIGINT NOT NULL AUTO_INCREMENT,
                          operation_id VARCHAR(64) NOT NULL,
                          module VARCHAR(50),
                          operation_type VARCHAR(20),
                          description VARCHAR(500),
                          method VARCHAR(200),
                          request_uri VARCHAR(500),
                          request_method VARCHAR(10),
                          request_params TEXT,
                          response_data TEXT,
                          operator_id BIGINT,
                          operator_name VARCHAR(50),
                          operator_ip VARCHAR(128),
                          operation_location VARCHAR(255),
                          browser VARCHAR(50),
                          os VARCHAR(50),
                          status TINYINT DEFAULT 0,
                          error_msg VARCHAR(2000),
                          operation_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                          cost_time BIGINT DEFAULT 0,
                          PRIMARY KEY (id),
                          INDEX idx_operation_time (operation_time)
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                        """,
                """
                        CREATE TABLE lottery_prize_config (
                          id BIGINT NOT NULL AUTO_INCREMENT,
                          prize_name VARCHAR(100) NOT NULL,
                          prize_level INT NOT NULL,
                          prize_points INT NOT NULL DEFAULT 0,
                          base_probability DECIMAL(12,8) NOT NULL DEFAULT 0,
                          current_probability DECIMAL(12,8) NOT NULL DEFAULT 0,
                          target_return_rate DECIMAL(12,8) DEFAULT 0,
                          max_return_rate DECIMAL(12,8) DEFAULT 1,
                          min_return_rate DECIMAL(12,8) DEFAULT 0,
                          actual_return_rate DECIMAL(12,8) DEFAULT 0,
                          total_draw_count INT DEFAULT 0,
                          total_win_count INT DEFAULT 0,
                          today_draw_count INT DEFAULT 0,
                          today_win_count INT DEFAULT 0,
                          daily_stock INT DEFAULT -1,
                          total_stock INT DEFAULT -1,
                          current_stock INT DEFAULT -1,
                          display_order INT DEFAULT 0,
                          prize_icon VARCHAR(255),
                          prize_desc VARCHAR(500),
                          is_active TINYINT DEFAULT 1,
                          is_suspended TINYINT DEFAULT 0,
                          suspend_reason VARCHAR(500),
                          suspend_until DATETIME NULL,
                          adjust_strategy VARCHAR(30),
                          last_adjust_time DATETIME NULL,
                          create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                          update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                          PRIMARY KEY (id)
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                        """,
                """
                        CREATE TABLE lottery_statistics_daily (
                          id BIGINT NOT NULL AUTO_INCREMENT,
                          stat_date DATE NOT NULL,
                          total_draw_count INT DEFAULT 0,
                          total_cost_points BIGINT DEFAULT 0,
                          total_reward_points BIGINT DEFAULT 0,
                          profit_points BIGINT DEFAULT 0,
                          actual_return_rate DECIMAL(12,8) DEFAULT 0,
                          unique_user_count INT DEFAULT 0,
                          special_prize_count INT DEFAULT 0,
                          first_prize_count INT DEFAULT 0,
                          second_prize_count INT DEFAULT 0,
                          third_prize_count INT DEFAULT 0,
                          fourth_prize_count INT DEFAULT 0,
                          fifth_prize_count INT DEFAULT 0,
                          sixth_prize_count INT DEFAULT 0,
                          no_prize_count INT DEFAULT 0,
                          create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                          update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                          PRIMARY KEY (id),
                          UNIQUE KEY uk_stat_date (stat_date)
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                        """,
                """
                        INSERT INTO chat_rooms (id, room_name, room_type, description, max_users, status)
                        VALUES (1, 'Agent acceptance room', 1, 'isolated live all-tools test', 0, 1)
                        """
        );
    }

    private record LiveRuntime(AgentChatOrchestrator orchestrator,
                               AgentToolRegistry registry,
                               AgentOperator operator,
                               AnnotationConfigApplicationContext spring) implements AutoCloseable {

        @Override
        public void close() {
            spring.close();
        }
    }

    private record RealServices(SysAgentAuditService auditService,
                                SysOperationLogService operationLogService,
                                ChatUserBanService chatUserBanService,
                                LotteryAdminService lotteryAdminService,
                                SysAgentAuditMapper auditMapper,
                                SysOperationLogMapper operationLogMapper,
                                ChatUserBanMapper chatUserBanMapper,
                                LotteryPrizeConfigMapper prizeConfigMapper,
                                LotteryStatisticsDailyMapper statisticsMapper) {
    }

    private record SeededFixtures(ChatUserBan seededBan,
                                  SysOperationLog expiredOperationLog,
                                  SysOperationLog recentOperationLog) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class PlannerRegressionCase {
        public String id;
        public String message;
        public String expectedStatus;
        public String expectedToolName;
    }
}
