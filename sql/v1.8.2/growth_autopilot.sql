-- ===================================
-- 学习成长自动驾驶（Growth Autopilot）表结构
-- 版本: v1.8.2
-- 创建时间: 2026-03-01
-- ===================================

CREATE TABLE IF NOT EXISTS `growth_autopilot_goal` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `week_start` DATE NOT NULL COMMENT '周起始日期（周一）',
    `week_end` DATE NOT NULL COMMENT '周结束日期（周日）',
    `target_role` VARCHAR(128) DEFAULT NULL COMMENT '目标岗位',
    `weekly_hours` INT DEFAULT 8 COMMENT '每周投入时长（小时）',
    `total_score_target` INT DEFAULT 0 COMMENT '目标总分',
    `total_score_completed` INT DEFAULT 0 COMMENT '已完成总分',
    `total_tasks` INT DEFAULT 0 COMMENT '任务总数',
    `completed_tasks` INT DEFAULT 0 COMMENT '已完成任务数',
    `completion_rate` INT DEFAULT 0 COMMENT '完成率（0-100）',
    `risk_level` VARCHAR(16) DEFAULT 'low' COMMENT '风险等级：low/medium/high',
    `status` VARCHAR(16) DEFAULT 'active' COMMENT '状态：active/archived',
    `generated_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '计划生成时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `uk_user_week` (`user_id`, `week_start`),
    KEY `idx_user_time` (`user_id`, `create_time` DESC),
    KEY `idx_week` (`week_start`, `week_end`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成长自动驾驶周目标';

CREATE TABLE IF NOT EXISTS `growth_autopilot_task` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `goal_id` BIGINT NOT NULL COMMENT '周目标ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `module_key` VARCHAR(32) NOT NULL COMMENT '模块标识',
    `module_name` VARCHAR(64) NOT NULL COMMENT '模块名称',
    `task_date` DATE NOT NULL COMMENT '任务日期',
    `title` VARCHAR(255) NOT NULL COMMENT '任务标题',
    `description` VARCHAR(512) DEFAULT NULL COMMENT '任务描述',
    `planned_minutes` INT DEFAULT 30 COMMENT '计划投入分钟数',
    `task_score` INT DEFAULT 1 COMMENT '任务分值',
    `priority` VARCHAR(8) DEFAULT 'P2' COMMENT '优先级：P1/P2/P3',
    `status` VARCHAR(16) DEFAULT 'todo' COMMENT '状态：todo/done/missed',
    `source` VARCHAR(16) DEFAULT 'auto' COMMENT '来源：auto/replan',
    `route_path` VARCHAR(255) DEFAULT NULL COMMENT '前端跳转路径',
    `complete_time` DATETIME DEFAULT NULL COMMENT '完成时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY `idx_goal_date` (`goal_id`, `task_date`),
    KEY `idx_goal_status` (`goal_id`, `status`),
    KEY `idx_user_date` (`user_id`, `task_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成长自动驾驶任务';

CREATE TABLE IF NOT EXISTS `growth_autopilot_event` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `goal_id` BIGINT NOT NULL COMMENT '周目标ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `event_type` VARCHAR(32) NOT NULL COMMENT '事件类型：generate/replan/complete',
    `event_detail` VARCHAR(512) DEFAULT NULL COMMENT '事件描述',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY `idx_goal_time` (`goal_id`, `create_time` DESC),
    KEY `idx_user_time` (`user_id`, `create_time` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成长自动驾驶事件日志';

