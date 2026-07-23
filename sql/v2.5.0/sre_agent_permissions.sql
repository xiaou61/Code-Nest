-- P3 SRE 只读 RCA Agent 权限。

INSERT IGNORE INTO `sys_permission`
(`parent_id`, `permission_name`, `permission_code`, `permission_type`, `path`, `component`, `icon`, `sort_order`, `status`, `description`, `create_by`)
VALUES
(0, '智能体调查 SRE 事故', 'agent:sre:incident:read', 2, NULL, NULL, NULL, 2415, 0,
 '允许管理员智能体读取受限证据并生成只读 RCA 报告', NULL);

INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`, `create_by`)
SELECT role_table.`id`, permission_table.`id`, NULL
FROM `sys_role` role_table
JOIN `sys_permission` permission_table
  ON permission_table.`permission_code` = 'agent:sre:incident:read'
WHERE role_table.`role_code` = 'SUPER_ADMIN';
