-- ===================================
-- AI Growth Coach P0
-- 版本: v2.5.0
-- 说明: 请按版本顺序执行一次；新库同时需同步全量建库脚本。
-- ===================================

ALTER TABLE `growth_autopilot_goal`
    ADD COLUMN `weekly_minutes` INT NOT NULL DEFAULT 480 COMMENT '每周精确预算（分钟）' AFTER `weekly_hours`,
    ADD COLUMN `plan_version` INT NOT NULL DEFAULT 1 COMMENT '计划乐观锁版本' AFTER `weekly_minutes`,
    ADD COLUMN `last_action_run_id` VARCHAR(80) DEFAULT NULL COMMENT '最近计划调整运行ID' AFTER `plan_version`;

UPDATE `growth_autopilot_goal`
SET `weekly_minutes` = GREATEST(0, COALESCE(`weekly_hours`, 8) * 60)
WHERE `weekly_minutes` = 480;

ALTER TABLE `growth_autopilot_task`
    ADD COLUMN `task_key` VARCHAR(80) DEFAULT NULL COMMENT '跨版本关联任务键' AFTER `route_path`,
    ADD COLUMN `plan_version` INT NOT NULL DEFAULT 1 COMMENT '所属计划版本' AFTER `task_key`,
    ADD COLUMN `resource_type` VARCHAR(40) DEFAULT NULL COMMENT '可执行资源类型' AFTER `plan_version`,
    ADD COLUMN `resource_id` VARCHAR(120) DEFAULT NULL COMMENT '可执行资源ID' AFTER `resource_type`,
    ADD COLUMN `resource_version` VARCHAR(80) DEFAULT NULL COMMENT '资源版本' AFTER `resource_id`,
    ADD COLUMN `selection_reason` VARCHAR(500) DEFAULT NULL COMMENT '任务选择原因' AFTER `resource_version`,
    ADD COLUMN `completion_rule_json` LONGTEXT DEFAULT NULL COMMENT '完成校验规则JSON' AFTER `selection_reason`,
    ADD COLUMN `superseded_by_task_id` BIGINT DEFAULT NULL COMMENT '替代此任务的新任务ID' AFTER `completion_rule_json`;

UPDATE `growth_autopilot_task`
SET `resource_type` = 'route',
    `resource_id` = `route_path`,
    `resource_version` = 'legacy',
    `completion_rule_json` = '{"type":"route"}'
WHERE `route_path` IS NOT NULL
  AND `route_path` <> ''
  AND `resource_id` IS NULL;

CREATE INDEX `idx_growth_task_plan_version`
    ON `growth_autopilot_task` (`goal_id`, `plan_version`);

CREATE INDEX `idx_growth_task_evidence_cursor`
    ON `growth_autopilot_task` (`user_id`, `update_time`, `id`);

CREATE INDEX `idx_mock_interview_evidence_cursor`
    ON `mock_interview_session` (`user_id`, `update_time`, `id`);

CREATE INDEX `idx_growth_task_evidence_change`
    ON `growth_autopilot_task` (`update_time`, `user_id`);

CREATE INDEX `idx_mock_interview_evidence_change`
    ON `mock_interview_session` (`update_time`, `user_id`);

ALTER TABLE `oj_submission`
    ADD COLUMN `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
        COMMENT '判题结果更新时间' AFTER `create_time`;

CREATE INDEX `idx_oj_submission_evidence_cursor`
    ON `oj_submission` (`user_id`, `update_time`, `id`);

CREATE INDEX `idx_oj_submission_evidence_change`
    ON `oj_submission` (`update_time`, `user_id`);

CREATE TABLE IF NOT EXISTS `growth_autopilot_revision` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `goal_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `version` INT NOT NULL,
    `base_version` INT NOT NULL,
    `source` VARCHAR(32) NOT NULL,
    `action_run_id` VARCHAR(80) DEFAULT NULL,
    `constraint_json` LONGTEXT DEFAULT NULL,
    `diff_json` LONGTEXT DEFAULT NULL,
    `snapshot_hash` CHAR(64) DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_growth_revision_goal_version` (`goal_id`, `version`),
    UNIQUE KEY `uk_growth_revision_action_run` (`action_run_id`),
    KEY `idx_growth_revision_user_time` (`user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成长计划版本快照';

CREATE TABLE IF NOT EXISTS `growth_coach_action_run` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `run_id` VARCHAR(80) NOT NULL,
    `user_id` BIGINT NOT NULL,
    `client_request_id` VARCHAR(80) NOT NULL,
    `action_id` VARCHAR(120) NOT NULL,
    `status` VARCHAR(32) NOT NULL,
    `base_plan_version` INT DEFAULT NULL,
    `target_plan_version` INT DEFAULT NULL,
    `request_hash` CHAR(64) NOT NULL,
    `message_redacted` VARCHAR(1000) DEFAULT NULL,
    `intent_json` LONGTEXT DEFAULT NULL,
    `preview_json` LONGTEXT DEFAULT NULL,
    `preview_hash` CHAR(64) DEFAULT NULL,
    `result_json` LONGTEXT DEFAULT NULL,
    `error_code` VARCHAR(64) DEFAULT NULL,
    `error_message` VARCHAR(1000) DEFAULT NULL,
    `prompt_key` VARCHAR(120) DEFAULT NULL,
    `prompt_version` VARCHAR(32) DEFAULT NULL,
    `model_name` VARCHAR(120) DEFAULT NULL,
    `expires_at` DATETIME DEFAULT NULL,
    `executed_at` DATETIME DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_growth_coach_run_id` (`run_id`),
    UNIQUE KEY `uk_growth_coach_user_request` (`user_id`, `client_request_id`),
    KEY `idx_growth_coach_user_time` (`user_id`, `created_at`),
    KEY `idx_growth_coach_status_expiry` (`status`, `expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户成长教练动作运行记录';

CREATE TABLE IF NOT EXISTS `growth_coach_action_event` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `run_id` VARCHAR(80) NOT NULL,
    `sequence_no` INT NOT NULL,
    `from_status` VARCHAR(32) DEFAULT NULL,
    `to_status` VARCHAR(32) NOT NULL,
    `event_type` VARCHAR(64) NOT NULL,
    `detail_json` LONGTEXT DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_growth_coach_event_sequence` (`run_id`, `sequence_no`),
    KEY `idx_growth_coach_event_time` (`run_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户成长教练动作事件';

CREATE TABLE IF NOT EXISTS `growth_evidence` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `evidence_id` VARCHAR(96) NOT NULL,
    `user_id` BIGINT NOT NULL,
    `evidence_type` VARCHAR(40) NOT NULL,
    `source_module` VARCHAR(40) NOT NULL,
    `source_type` VARCHAR(40) NOT NULL,
    `source_id` VARCHAR(120) NOT NULL,
    `skill_key` VARCHAR(120) DEFAULT NULL,
    `summary_json` LONGTEXT DEFAULT NULL,
    `quality_level` VARCHAR(32) NOT NULL,
    `observed_at` DATETIME NOT NULL,
    `valid_from` DATETIME NOT NULL,
    `valid_to` DATETIME DEFAULT NULL,
    `content_hash` CHAR(64) NOT NULL,
    `projector_version` VARCHAR(32) NOT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_growth_evidence_id` (`evidence_id`),
    UNIQUE KEY `uk_growth_evidence_source` (
        `user_id`, `source_module`, `source_type`, `source_id`, `evidence_type`, `projector_version`
    ),
    KEY `idx_growth_evidence_user_observed` (`user_id`, `valid_to`, `observed_at`),
    KEY `idx_growth_evidence_skill_time` (`user_id`, `skill_key`, `observed_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户成长证据结构化索引';

CREATE TABLE IF NOT EXISTS `growth_evidence_projection_cursor` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `source_key` VARCHAR(64) NOT NULL,
    `cursor_time` DATETIME NOT NULL,
    `cursor_source_id` BIGINT NOT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_growth_evidence_cursor` (`user_id`, `source_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成长证据来源增量投影游标';
