-- Code-Nest SRE P2: Alertmanager event, incident aggregate and transactional outbox.
-- Apply after the base schema. The tables intentionally do not add cross-domain foreign keys;
-- existing Code-Nest modules use independent incremental tables and the SRE event source is external.

CREATE TABLE IF NOT EXISTS `sre_alert_event` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '告警事件ID',
    `source` VARCHAR(32) NOT NULL COMMENT '事件来源，如 alertmanager',
    `fingerprint` VARCHAR(128) NOT NULL COMMENT 'Alertmanager fingerprint',
    `alert_name` VARCHAR(200) NOT NULL COMMENT '告警名称',
    `status` VARCHAR(16) NOT NULL COMMENT 'FIRING 或 RESOLVED',
    `severity` VARCHAR(32) NOT NULL DEFAULT 'warning' COMMENT '告警等级',
    `service` VARCHAR(100) NOT NULL DEFAULT 'unknown' COMMENT '责任服务',
    `labels_json` MEDIUMTEXT NULL COMMENT '告警标签 JSON',
    `annotations_json` MEDIUMTEXT NULL COMMENT '告警注释 JSON',
    `starts_at` VARCHAR(40) NOT NULL COMMENT '告警开始时间（规范化 ISO-8601）',
    `ends_at` VARCHAR(40) NULL COMMENT '告警结束时间（规范化 ISO-8601）',
    `generator_url` VARCHAR(1000) NULL COMMENT 'Prometheus 生成器链接',
    `raw_payload` MEDIUMTEXT NULL COMMENT '单条告警 JSON 快照',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sre_alert_source_fingerprint_start` (`source`, `fingerprint`, `starts_at`),
    KEY `idx_sre_alert_status_service` (`status`, `service`),
    KEY `idx_sre_alert_update_time` (`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SRE 原始告警事件';

CREATE TABLE IF NOT EXISTS `sre_incident` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '事故ID',
    `incident_no` VARCHAR(64) NOT NULL COMMENT '事故编号',
    `incident_key` VARCHAR(320) NOT NULL COMMENT '服务与告警名称聚合键',
    `service` VARCHAR(100) NOT NULL DEFAULT 'unknown' COMMENT '责任服务',
    `alert_name` VARCHAR(200) NOT NULL COMMENT '主告警名称',
    `severity` VARCHAR(32) NOT NULL DEFAULT 'warning' COMMENT '事故等级',
    `state` VARCHAR(32) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/ACKNOWLEDGED/INVESTIGATING/MITIGATED/RESOLVED/CLOSED',
    `summary` VARCHAR(500) NULL COMMENT '事故摘要',
    `first_seen` DATETIME NOT NULL COMMENT '首次发现时间',
    `last_seen` DATETIME NOT NULL COMMENT '最近发现时间',
    `acknowledged_by` BIGINT NULL COMMENT '确认管理员ID',
    `acknowledged_at` DATETIME NULL COMMENT '确认时间',
    `resolved_at` DATETIME NULL COMMENT '恢复时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sre_incident_no` (`incident_no`),
    KEY `idx_sre_incident_key_state` (`incident_key`, `state`),
    KEY `idx_sre_incident_state_severity` (`state`, `severity`),
    KEY `idx_sre_incident_last_seen` (`last_seen`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SRE 事故';

CREATE TABLE IF NOT EXISTS `sre_incident_alert_relation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '关联ID',
    `incident_id` BIGINT NOT NULL COMMENT '事故ID',
    `alert_event_id` BIGINT NOT NULL COMMENT '告警事件ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sre_incident_alert` (`incident_id`, `alert_event_id`),
    UNIQUE KEY `uk_sre_alert_event_relation` (`alert_event_id`),
    KEY `idx_sre_relation_incident` (`incident_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SRE 事故与告警关联';

CREATE TABLE IF NOT EXISTS `sre_outbox_event` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'outbox事件ID',
    `aggregate_type` VARCHAR(32) NOT NULL COMMENT '聚合类型',
    `aggregate_id` BIGINT NOT NULL COMMENT '聚合ID',
    `event_type` VARCHAR(64) NOT NULL COMMENT '事件类型',
    `payload_json` MEDIUMTEXT NOT NULL COMMENT '事件载荷',
    `state` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PROCESSING/SUCCEEDED/FAILED',
    `attempts` INT NOT NULL DEFAULT 0 COMMENT '尝试次数',
    `next_attempt_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下次处理时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_sre_outbox_state_next_attempt` (`state`, `next_attempt_at`),
    KEY `idx_sre_outbox_aggregate` (`aggregate_type`, `aggregate_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SRE 事务性outbox';

CREATE TABLE IF NOT EXISTS `sre_incident_evidence` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '证据ID',
    `incident_id` BIGINT NOT NULL COMMENT '事故ID',
    `outbox_event_id` BIGINT NOT NULL COMMENT '生成该证据的Outbox事件ID',
    `source_type` VARCHAR(32) NOT NULL COMMENT '证据来源类型',
    `source_ref` VARCHAR(128) NULL COMMENT '来源记录引用',
    `query_text` VARCHAR(1000) NULL COMMENT '查询或定位描述',
    `snapshot_json` MEDIUMTEXT NOT NULL COMMENT '证据快照 JSON',
    `captured_at` DATETIME NOT NULL COMMENT '证据采集时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sre_evidence_outbox_source` (`outbox_event_id`, `source_type`),
    KEY `idx_sre_evidence_incident_captured` (`incident_id`, `captured_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SRE 事故证据快照';
