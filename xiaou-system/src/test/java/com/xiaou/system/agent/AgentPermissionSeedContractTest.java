package com.xiaou.system.agent;

import com.xiaou.chat.service.ChatUserBanService;
import com.xiaou.points.service.LotteryAdminService;
import com.xiaou.system.service.SysAgentAuditService;
import com.xiaou.system.service.SysOperationLogService;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class AgentPermissionSeedContractTest {

    private static final List<String> PERMISSION_SEED_FILES = List.of(
            "sql/MySql/code_nest_data.sql",
            "sql/v2.4.0/admin_agent_permissions.sql"
    );
    private static final Pattern AGENT_PERMISSION_PATTERN = Pattern.compile("'(agent:[^']+)'");
    private static final String ROLE_GRANT_MARKER = "ON permission_table.`permission_code` IN (";

    private final SysOperationLogService operationLogService = mock(SysOperationLogService.class);
    private final ChatUserBanService chatUserBanService = mock(ChatUserBanService.class);
    private final LotteryAdminService lotteryAdminService = mock(LotteryAdminService.class);
    private final SysAgentAuditService auditService = mock(SysAgentAuditService.class);
    private final AgentToolCatalogService catalogService = mock(AgentToolCatalogService.class);

    @Test
    void builtInAgentToolPermissionsShouldBeSeededAndGrantedToSuperAdmin() throws IOException {
        Set<String> requiredPermissions = requiredPermissions();
        Path root = repositoryRoot();

        for (String seedFile : PERMISSION_SEED_FILES) {
            String sql = Files.readString(root.resolve(seedFile), StandardCharsets.UTF_8);
            Set<String> seededPermissions = extractAgentPermissions(sql);
            Set<String> grantedPermissions = extractSuperAdminGrantPermissions(sql);

            assertTrue(seededPermissions.containsAll(requiredPermissions),
                    seedFile + " missing sys_permission seed(s): " + missing(requiredPermissions, seededPermissions));
            assertTrue(grantedPermissions.containsAll(requiredPermissions),
                    seedFile + " missing SUPER_ADMIN grant seed(s): " + missing(requiredPermissions, grantedPermissions));
        }
    }

    private Set<String> requiredPermissions() {
        Set<String> permissions = new LinkedHashSet<>();
        for (AgentTool tool : AgentToolTestCatalog.builtInTools(
                operationLogService,
                chatUserBanService,
                lotteryAdminService,
                auditService,
                catalogService
        )) {
            AgentToolDefinition definition = tool.definition();
            if (definition.getRequiredPermissions() != null) {
                permissions.addAll(definition.getRequiredPermissions());
            }
        }
        return permissions;
    }

    private Set<String> extractAgentPermissions(String sql) {
        Matcher matcher = AGENT_PERMISSION_PATTERN.matcher(sql);
        Set<String> permissions = new LinkedHashSet<>();
        while (matcher.find()) {
            permissions.add(matcher.group(1));
        }
        return permissions;
    }

    private Set<String> extractSuperAdminGrantPermissions(String sql) {
        int markerIndex = sql.indexOf(ROLE_GRANT_MARKER);
        if (markerIndex < 0) {
            return Set.of();
        }
        int whereIndex = sql.indexOf("WHERE role_table.`role_code` = 'SUPER_ADMIN'", markerIndex);
        String grantBlock = whereIndex < 0 ? sql.substring(markerIndex) : sql.substring(markerIndex, whereIndex);
        return extractAgentPermissions(grantBlock);
    }

    private Set<String> missing(Set<String> required, Set<String> actual) {
        Set<String> missing = new LinkedHashSet<>(required);
        missing.removeAll(actual);
        return missing;
    }

    private Path repositoryRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null && !Files.exists(current.resolve("sql/MySql/code_nest_data.sql"))) {
            current = current.getParent();
        }
        if (current == null) {
            throw new IllegalStateException("Cannot locate repository root from " + Path.of("").toAbsolutePath());
        }
        return current;
    }
}
