-- Code-Nest SRE v2.5.0: versioned RCA evaluation suites and aggregate quality gates.
-- Apply after sre_rca_evaluation.sql. This file is a one-time incremental migration.

CREATE TABLE IF NOT EXISTS `sre_rca_evaluation_suite` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '稳定评测套件ID',
    `suite_key` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '稳定且唯一的套件Key',
    `name` VARCHAR(128) NOT NULL COMMENT '套件名称',
    `description` VARCHAR(500) NULL COMMENT '套件用途说明',
    `created_by` BIGINT NOT NULL COMMENT '创建管理员ID',
    `created_at` DATETIME NOT NULL COMMENT '创建时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sre_rca_eval_suite_key` (`suite_key`),
    KEY `idx_sre_rca_eval_suite_created` (`created_at`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SRE RCA稳定评测套件';

CREATE TABLE IF NOT EXISTS `sre_rca_evaluation_suite_version` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '不可变套件版本ID',
    `suite_id` BIGINT NOT NULL COMMENT '稳定套件ID',
    `version_no` INT UNSIGNED NOT NULL COMMENT '套件内递增版本号',
    `case_count` INT UNSIGNED NOT NULL COMMENT '冻结用例数量，最大100',
    `manifest_sha256` CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '成员和策略清单SHA-256',
    `manifest_schema_id` VARCHAR(128) NOT NULL COMMENT 'manifest规范ID',
    `scoring_policy_id` VARCHAR(128) NOT NULL COMMENT '透明评分策略ID',
    `gate_evaluator_id` VARCHAR(128) NOT NULL COMMENT '聚合门禁评估器ID',
    `minimum_pass_rate` DECIMAL(5,2) NOT NULL COMMENT '最低通过率，0到100',
    `minimum_average_score` DECIMAL(6,2) NOT NULL COMMENT '最低平均分，0到100',
    `require_all_safety` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否要求全部结果满足只读安全',
    `require_no_degraded` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否禁止降级或失败结果',
    `published_by` BIGINT NOT NULL COMMENT '发布管理员ID',
    `published_at` DATETIME NOT NULL COMMENT '发布时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sre_rca_eval_suite_version` (`suite_id`, `version_no`),
    UNIQUE KEY `uk_sre_rca_eval_suite_manifest` (`suite_id`, `manifest_sha256`),
    KEY `idx_sre_rca_eval_suite_version_published` (`suite_id`, `published_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SRE RCA不可变评测套件版本';

CREATE TABLE IF NOT EXISTS `sre_rca_evaluation_suite_case` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '套件版本成员ID',
    `suite_version_id` BIGINT NOT NULL COMMENT '不可变套件版本ID',
    `case_id` BIGINT NOT NULL COMMENT '不可变评测用例ID',
    `case_ordinal` INT UNSIGNED NOT NULL COMMENT '规范成员顺序，从1开始',
    `case_content_sha256` CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '上下文、基准报告与期望结论指纹',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sre_rca_eval_suite_case` (`suite_version_id`, `case_id`),
    UNIQUE KEY `uk_sre_rca_eval_suite_ordinal` (`suite_version_id`, `case_ordinal`),
    KEY `idx_sre_rca_eval_suite_case_lookup` (`case_id`, `suite_version_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SRE RCA评测套件不可变成员';

ALTER TABLE `sre_rca_evaluation_run`
    ADD COLUMN `suite_version_id` BIGINT NULL COMMENT '不可变套件版本ID，临时回放为空' AFTER `requested_case_id`,
    ADD COLUMN `suite_key` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL COMMENT '运行时冻结的套件Key' AFTER `suite_version_id`,
    ADD COLUMN `suite_version` INT UNSIGNED NULL COMMENT '运行时冻结的套件版本号' AFTER `suite_key`,
    ADD COLUMN `suite_manifest_sha256` CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL COMMENT '运行时冻结的套件manifest' AFTER `suite_version`,
    ADD COLUMN `trigger_source` VARCHAR(16) NOT NULL DEFAULT 'MANUAL' COMMENT 'MANUAL/CI' AFTER `suite_manifest_sha256`,
    ADD COLUMN `pass_rate` DECIMAL(5,2) NULL COMMENT '本次通过率，0到100' AFTER `average_score`,
    ADD COLUMN `unsafe_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '未通过只读安全的结果数' AFTER `pass_rate`,
    ADD COLUMN `degraded_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '降级或失败结果数' AFTER `unsafe_count`,
    ADD COLUMN `scoring_policy_id` VARCHAR(128) NULL COMMENT '套件冻结的评分策略ID' AFTER `schema_id`,
    ADD COLUMN `gate_evaluator_id` VARCHAR(128) NULL COMMENT '套件冻结的门禁评估器ID' AFTER `scoring_policy_id`,
    ADD COLUMN `gate_status` VARCHAR(16) NOT NULL DEFAULT 'NOT_APPLICABLE' COMMENT 'NOT_APPLICABLE/PENDING/PASSED/FAILED' AFTER `gate_evaluator_id`,
    ADD COLUMN `gate_minimum_pass_rate` DECIMAL(5,2) NULL COMMENT '冻结的最低通过率' AFTER `gate_status`,
    ADD COLUMN `gate_minimum_average_score` DECIMAL(6,2) NULL COMMENT '冻结的最低平均分' AFTER `gate_minimum_pass_rate`,
    ADD COLUMN `gate_require_all_safety` TINYINT(1) NULL COMMENT '冻结的全部安全要求' AFTER `gate_minimum_average_score`,
    ADD COLUMN `gate_require_no_degraded` TINYINT(1) NULL COMMENT '冻结的无降级要求' AFTER `gate_require_all_safety`,
    ADD COLUMN `gate_detail_json` TEXT NULL COMMENT '固定门禁失败码JSON，不含报告正文' AFTER `gate_require_no_degraded`,
    ADD KEY `idx_sre_rca_eval_run_suite_started` (`suite_version_id`, `started_at`),
    ADD KEY `idx_sre_rca_eval_run_gate_started` (`gate_status`, `started_at`);
