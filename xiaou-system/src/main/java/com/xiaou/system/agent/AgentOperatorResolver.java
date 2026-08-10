package com.xiaou.system.agent;

import com.xiaou.system.domain.SysAdmin;
import com.xiaou.system.service.SysAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Resolves the current administrator identity and authorization snapshot.
 *
 * @author xiaou
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentOperatorResolver {

    private final SysAdminService adminService;

    public AgentOperator resolve(Long adminId) {
        return new AgentOperator(
                adminId,
                resolveName(adminId),
                "",
                resolveRoles(adminId),
                resolvePermissions(adminId)
        );
    }

    private String resolveName(Long adminId) {
        try {
            SysAdmin admin = adminService.getById(adminId);
            if (admin != null && admin.getUsername() != null) {
                return admin.getUsername();
            }
        } catch (Exception e) {
            log.warn("获取智能体操作人失败，adminId={}", adminId);
        }
        return String.valueOf(adminId);
    }

    private List<String> resolveRoles(Long adminId) {
        try {
            List<String> roles = adminService.getAdminRoles(adminId);
            return roles == null ? List.of() : roles;
        } catch (Exception e) {
            log.warn("获取智能体操作人角色失败，adminId={}", adminId);
            return List.of();
        }
    }

    private List<String> resolvePermissions(Long adminId) {
        try {
            List<String> permissions = adminService.getAdminPermissions(adminId);
            return permissions == null ? List.of() : permissions;
        } catch (Exception e) {
            log.warn("获取智能体操作人权限失败，adminId={}", adminId);
            return List.of();
        }
    }
}
