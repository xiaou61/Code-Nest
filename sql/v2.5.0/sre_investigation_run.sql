-- Code-Nest SRE v2.5.0: persistent read-only RCA runs and auditable stage trace.

CREATE TABLE IF NOT EXISTS `sre_investigation_run` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '调查运行ID',
    `incident_id` BIGINT NOT NULL COMMENT '事故ID',
    `status` VARCHAR(16) NOT NULL COMMENT 'RUNNING/SUCCEEDED/DEGRADED/FAILED',
    `trigger_source` VARCHAR(32) NOT NULL COMMENT 'ADMIN_API/AGENT_TOOL/SYSTEM',
    `requested_by` BIGINT NULL COMMENT '发起管理员ID',
    `generation_mode` VARCHAR(16) NULL COMMENT 'AI/FALLBACK',
    `conclusion_status` VARCHAR(32) NULL COMMENT '结构化结论状态',
    `alert_count` INT NOT NULL DEFAULT 0 COMMENT '输入告警数量',
    `evidence_count` INT NOT NULL DEFAULT 0 COMMENT '输入证据数量',
    `context_truncated` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '上下文是否被裁剪',
    `report_json` MEDIUMTEXT NULL COMMENT '已脱敏结构化RCA报告',
    `failure_code` VARCHAR(128) NULL COMMENT '受控失败码，不保存异常详情',
    `started_at` DATETIME NOT NULL COMMENT '开始时间',
    `completed_at` DATETIME NULL COMMENT '结束时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_sre_run_incident_started` (`incident_id`, `started_at`),
    KEY `idx_sre_run_status_started` (`status`, `started_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SRE只读调查运行';

CREATE TABLE IF NOT EXISTS `sre_investigation_artifact` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '调查回放产物ID',
    `incident_id` BIGINT NOT NULL COMMENT '事故ID，用于归属校验',
    `run_id` BIGINT NOT NULL COMMENT '调查运行ID，一次运行只保存一份',
    `context_json` MEDIUMTEXT NOT NULL COMMENT '模型实际接收的已脱敏受限上下文',
    `context_sha256` CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '上下文SHA-256',
    `context_length` INT UNSIGNED NOT NULL COMMENT '上下文字符数，最大60000',
    `context_truncated` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '模型上下文是否被裁剪',
    `prompt_id` VARCHAR(128) NOT NULL COMMENT 'Prompt ID及版本',
    `schema_id` VARCHAR(255) NOT NULL COMMENT '结构化输出Schema ID',
    `provider` VARCHAR(64) NOT NULL COMMENT '请求配置的AI提供商',
    `configured_model` VARCHAR(128) NULL COMMENT '请求构建时配置模型',
    `actual_model` VARCHAR(128) NULL COMMENT '上游响应声明的实际模型',
    `invocation_outcome` VARCHAR(32) NOT NULL COMMENT '模型调用或降级结果',
    `created_at` DATETIME NOT NULL COMMENT '产物固化时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sre_artifact_run` (`run_id`),
    KEY `idx_sre_artifact_incident_created` (`incident_id`, `created_at`),
    KEY `idx_sre_artifact_context_hash` (`context_sha256`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SRE调查脱敏回放输入与来源';

CREATE TABLE IF NOT EXISTS `sre_investigation_step` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '调查步骤ID',
    `run_id` BIGINT NOT NULL COMMENT '调查运行ID',
    `step_order` INT NOT NULL COMMENT '步骤顺序',
    `step_code` VARCHAR(64) NOT NULL COMMENT '步骤编码',
    `status` VARCHAR(16) NOT NULL COMMENT 'SUCCEEDED/DEGRADED/FAILED/SKIPPED',
    `detail` VARCHAR(500) NULL COMMENT '受控步骤摘要',
    `recorded_at` DATETIME NOT NULL COMMENT '记录时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sre_step_run_order` (`run_id`, `step_order`),
    KEY `idx_sre_step_run_recorded` (`run_id`, `recorded_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SRE调查步骤轨迹';

CREATE TABLE IF NOT EXISTS `sre_investigation_feedback` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '反馈修订ID',
    `run_id` BIGINT NOT NULL COMMENT '调查运行ID',
    `accuracy` VARCHAR(16) NOT NULL COMMENT 'ACCURATE/PARTIAL/INACCURATE',
    `gap_type` VARCHAR(32) NULL COMMENT '检索/推理/工具/路由等主要缺口',
    `note` VARCHAR(1000) NULL COMMENT '已清理的管理员备注',
    `expected_conclusion` VARCHAR(2000) NULL COMMENT '已清理的期望结论',
    `reviewed_by` BIGINT NOT NULL COMMENT '评价管理员ID',
    `reviewed_at` DATETIME NOT NULL COMMENT '评价时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_sre_feedback_run_revision` (`run_id`, `id`),
    KEY `idx_sre_feedback_reviewed_at` (`reviewed_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SRE调查人工反馈修订';
