-- RCA evaluation queue governance: immutable membership, leases, deadlines and build provenance.
-- Apply after sre_rca_evaluation.sql and sre_rca_evaluation_suite.sql while the evaluation worker is disabled.

ALTER TABLE `sre_rca_evaluation_run`
    MODIFY COLUMN `status` VARCHAR(16) NOT NULL COMMENT 'QUEUED/RUNNING/SUCCEEDED/DEGRADED/FAILED',
    MODIFY COLUMN `started_at` DATETIME NULL COMMENT '首次领取执行时间',
    ADD COLUMN `active_admin_id` BIGINT NULL COMMENT '活动运行管理员ID，终态清空' AFTER `requested_by`,
    ADD COLUMN `attempts` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '原子领取次数' AFTER `failure_code`,
    ADD COLUMN `next_attempt_at` DATETIME NULL COMMENT '下次可领取时间' AFTER `attempts`,
    ADD COLUMN `claimed_at` DATETIME NULL COMMENT '最近领取时间' AFTER `next_attempt_at`,
    ADD COLUMN `heartbeat_at` DATETIME NULL COMMENT '最近用例级心跳时间' AFTER `claimed_at`,
    ADD COLUMN `deadline_at` DATETIME NULL COMMENT '运行绝对截止时间' AFTER `heartbeat_at`,
    ADD COLUMN `max_duration_seconds` INT UNSIGNED NOT NULL DEFAULT 1800 COMMENT '冻结的最长执行秒数' AFTER `deadline_at`,
    ADD COLUMN `source_revision` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'unknown' COMMENT '冻结的源码修订' AFTER `max_duration_seconds`,
    ADD COLUMN `build_id` VARCHAR(128) NOT NULL DEFAULT 'legacy' COMMENT '冻结的构建ID' AFTER `source_revision`,
    ADD COLUMN `build_version` VARCHAR(64) NOT NULL DEFAULT 'unknown' COMMENT '冻结的构建版本' AFTER `build_id`,
    ADD UNIQUE KEY `uk_sre_rca_eval_run_active_admin` (`active_admin_id`),
    ADD KEY `idx_sre_rca_eval_run_claim` (`status`, `next_attempt_at`, `id`),
    ADD KEY `idx_sre_rca_eval_run_lease` (`status`, `heartbeat_at`),
    ADD KEY `idx_sre_rca_eval_run_deadline` (`status`, `deadline_at`);

-- A pre-migration synchronous RUNNING row cannot carry a trustworthy lease or build identity.
UPDATE `sre_rca_evaluation_run`
SET `status` = 'FAILED',
    `failure_code` = 'EVALUATION_MIGRATION_INTERRUPTED',
    `gate_status` = CASE
        WHEN `suite_version_id` IS NULL THEN 'NOT_APPLICABLE'
        ELSE 'FAILED'
    END,
    `completed_at` = COALESCE(`completed_at`, CURRENT_TIMESTAMP),
    `active_admin_id` = NULL,
    `update_time` = CURRENT_TIMESTAMP
WHERE `status` = 'RUNNING';

CREATE TABLE IF NOT EXISTS `sre_rca_evaluation_run_case` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '运行成员ID',
  `evaluation_run_id` BIGINT NOT NULL COMMENT '评测运行ID',
  `case_id` BIGINT NOT NULL COMMENT '不可变评测用例ID',
  `case_ordinal` INT UNSIGNED NOT NULL COMMENT '运行内规范顺序，从1开始',
  `case_content_sha256` CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '入队时用例内容指纹',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '冻结时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sre_rca_eval_run_case` (`evaluation_run_id`, `case_id`),
  UNIQUE KEY `uk_sre_rca_eval_run_ordinal` (`evaluation_run_id`, `case_ordinal`),
  KEY `idx_sre_rca_eval_run_case_lookup` (`case_id`, `evaluation_run_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SRE RCA评测运行不可变成员';
