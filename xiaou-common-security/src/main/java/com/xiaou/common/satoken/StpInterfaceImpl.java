package com.xiaou.common.satoken;

import cn.dev33.satoken.stp.StpInterface;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Sa-Token 权限验证接口实现
 * 用于查询用户的权限和角色
 * 
 * @author xiaou
 */
@Slf4j
@Component
public class StpInterfaceImpl implements StpInterface {
    
    /**
     * 返回指定账号的权限列表
     * 
     * @param loginId 账号ID
     * @param loginType 账号类型（admin/user）
     * @return 权限列表
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        List<String> permissions = new ArrayList<>();
        
        if ("admin".equals(loginType)) {
            // 管理员权限（可根据实际业务从数据库查询）
            // 示例：permissions = sysAdminService.getPermissionsByUserId(Long.parseLong(loginId.toString()));
            permissions.add("admin");  // 基础管理员权限
            log.debug("获取管理员权限列表，loginId: {}, permissions: {}", loginId, permissions);
        } else if ("user".equals(loginType)) {
            // 普通用户权限
            permissions.add("user");  // 基础用户权限
            log.debug("获取用户权限列表，loginId: {}, permissions: {}", loginId, permissions);
        }
        
        return permissions;
    }
    
    /**
     * 返回指定账号的角色列表
     * 
     * @param loginId 账号ID
     * @param loginType 账号类型（admin/user）
     * @return 角色列表
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        List<String> roles = new ArrayList<>();
        
        if ("admin".equals(loginType)) {
            // 管理员角色
            roles.add("admin");
            log.debug("获取管理员角色列表，loginId: {}, roles: {}", loginId, roles);
        } else if ("user".equals(loginType)) {
            // 普通用户角色
            roles.add("user");
            log.debug("获取用户角色列表，loginId: {}, roles: {}", loginId, roles);
        }
        
        return roles;
    }
}
