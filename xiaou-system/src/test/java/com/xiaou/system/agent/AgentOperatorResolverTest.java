package com.xiaou.system.agent;

import com.xiaou.system.domain.SysAdmin;
import com.xiaou.system.service.SysAdminService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentOperatorResolverTest {

    @Test
    void shouldResolveCurrentRolesAndPermissionsOnEveryCall() {
        SysAdminService adminService = mock(SysAdminService.class);
        SysAdmin admin = new SysAdmin();
        admin.setId(7L);
        admin.setUsername("admin");
        when(adminService.getById(7L)).thenReturn(admin);
        when(adminService.getAdminRoles(7L)).thenReturn(List.of("ADMIN"));
        when(adminService.getAdminPermissions(7L))
                .thenReturn(List.of("agent:system:read"))
                .thenReturn(List.of("agent:system:read", "agent:system:write"));

        AgentOperatorResolver resolver = new AgentOperatorResolver(adminService);

        assertEquals(List.of("agent:system:read"), resolver.resolve(7L).permissions());
        assertEquals(List.of("agent:system:read", "agent:system:write"), resolver.resolve(7L).permissions());
    }

    @Test
    void shouldUseSafeFallbacksWhenAdminLookupFails() {
        SysAdminService adminService = mock(SysAdminService.class);
        when(adminService.getById(9L)).thenThrow(new IllegalStateException("unavailable"));
        when(adminService.getAdminRoles(9L)).thenThrow(new IllegalStateException("unavailable"));
        when(adminService.getAdminPermissions(9L)).thenThrow(new IllegalStateException("unavailable"));

        AgentOperator operator = new AgentOperatorResolver(adminService).resolve(9L);

        assertEquals("9", operator.name());
        assertEquals(List.of(), operator.roles());
        assertEquals(List.of(), operator.permissions());
    }
}
