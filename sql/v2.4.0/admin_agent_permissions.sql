-- 管理员端超级智能体内置工具权限种子。
-- 说明：权限由后端 AgentToolDefinition.requiredPermissions 声明，前端不维护动作目录。

INSERT IGNORE INTO `sys_permission`
(`parent_id`, `permission_name`, `permission_code`, `permission_type`, `path`, `component`, `icon`, `sort_order`, `status`, `description`, `create_by`)
VALUES
(0, '智能体查询工具目录', 'agent:runtime:tool-catalog:read', 2, NULL, NULL, NULL, 2400, 0, '允许管理员智能体查询后端工具目录', NULL),
(0, '智能体查询运行时状态', 'agent:runtime:status:read', 2, NULL, NULL, NULL, 2406, 0, '允许管理员智能体查询后端运行时状态', NULL),
(0, '智能体运行时上线自检', 'agent:runtime:readiness:read', 2, NULL, NULL, NULL, 2413, 0, '允许管理员智能体执行后端运行时 readiness 上线自检', NULL),
(0, '智能体查询当前会话上下文', 'agent:runtime:session:read', 2, NULL, NULL, NULL, 2407, 0, '允许管理员智能体查询当前会话上下文摘要', NULL),
(0, '智能体查询工具调用指标', 'agent:runtime:metrics:read', 2, NULL, NULL, NULL, 2408, 0, '允许管理员智能体查询后端工具调用指标', NULL),
(0, '智能体查询当前操作者', 'agent:runtime:operator:read', 2, NULL, NULL, NULL, 2409, 0, '允许管理员智能体查询当前请求操作者上下文', NULL),
(0, '智能体查询规划器诊断', 'agent:runtime:planner:read', 2, NULL, NULL, NULL, 2410, 0, '允许管理员智能体查询 planner 契约、命中规则和规划预演结果', NULL),
(0, '智能体检查工具访问', 'agent:runtime:tool-access:read', 2, NULL, NULL, NULL, 2411, 0, '允许管理员智能体检查当前操作者对工具的访问条件', NULL),
(0, '智能体策略预检', 'agent:runtime:policy:read', 2, NULL, NULL, NULL, 2412, 0, '允许管理员智能体对工具调用执行只读策略预检', NULL),
(0, '智能体查询审计记录', 'agent:runtime:audit:read', 2, NULL, NULL, NULL, 2401, 0, '允许管理员智能体查询自身审计记录', NULL),
(0, '智能体查询操作日志', 'agent:system:operation-log:read', 2, NULL, NULL, NULL, 2402, 0, '允许管理员智能体查询系统操作日志', NULL),
(0, '智能体清理操作日志', 'agent:system:operation-log:clean', 2, NULL, NULL, NULL, 2403, 0, '允许管理员智能体清理系统操作日志', NULL),
(0, '智能体查询聊天禁言', 'agent:chat:user-ban:read', 2, NULL, NULL, NULL, 2404, 0, '允许管理员智能体查询聊天用户禁言状态', NULL),
(0, '智能体解除聊天禁言', 'agent:chat:user-ban:write', 2, NULL, NULL, NULL, 2405, 0, '允许管理员智能体解除聊天用户禁言', NULL),
(0, '智能体查询抽奖监控', 'agent:points:lottery:monitor:read', 2, NULL, NULL, NULL, 2414, 0, '允许管理员智能体查询抽奖实时监控', NULL),
(0, '智能体调查 SRE 事故', 'agent:sre:incident:read', 2, NULL, NULL, NULL, 2415, 0, '允许管理员智能体读取受限证据并生成只读 RCA 报告', NULL);

INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`, `create_by`)
SELECT role_table.`id`, permission_table.`id`, NULL
FROM `sys_role` role_table
JOIN `sys_permission` permission_table
  ON permission_table.`permission_code` IN (
    'agent:runtime:tool-catalog:read',
    'agent:runtime:status:read',
    'agent:runtime:readiness:read',
    'agent:runtime:session:read',
    'agent:runtime:metrics:read',
    'agent:runtime:operator:read',
    'agent:runtime:planner:read',
    'agent:runtime:tool-access:read',
    'agent:runtime:policy:read',
    'agent:runtime:audit:read',
    'agent:system:operation-log:read',
    'agent:system:operation-log:clean',
    'agent:chat:user-ban:read',
    'agent:chat:user-ban:write',
    'agent:points:lottery:monitor:read',
    'agent:sre:incident:read'
  )
WHERE role_table.`role_code` = 'SUPER_ADMIN';
