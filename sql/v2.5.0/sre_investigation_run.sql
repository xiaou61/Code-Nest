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
