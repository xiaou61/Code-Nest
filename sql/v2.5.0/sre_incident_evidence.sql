-- P2.1 增量迁移：已有 SRE 告警/事故表的环境可单独执行此文件。
-- Outbox Worker 默认关闭；确认迁移完成后再设置 XIAOU_SRE_OUTBOX_ENABLED=true。

CREATE TABLE IF NOT EXISTS `sre_incident_evidence` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '证据ID',
    `incident_id` BIGINT NOT NULL COMMENT '事故ID',
    `outbox_event_id` BIGINT NULL COMMENT '生成该证据的Outbox事件ID，调查轮次证据为空',
    `investigation_run_id` BIGINT NULL COMMENT '生成该证据的调查运行ID，Outbox证据为空',
    `query_fingerprint` CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL COMMENT '固定只读查询SHA-256',
    `source_type` VARCHAR(32) NOT NULL COMMENT '证据来源类型',
    `source_ref` VARCHAR(128) NULL COMMENT '来源记录引用',
    `query_text` VARCHAR(1000) NULL COMMENT '查询或定位描述',
    `snapshot_json` MEDIUMTEXT NOT NULL COMMENT '证据快照 JSON',
    `captured_at` DATETIME NOT NULL COMMENT '证据采集时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sre_evidence_outbox_source` (`outbox_event_id`, `source_type`),
    UNIQUE KEY `uk_sre_evidence_run_query` (`investigation_run_id`, `query_fingerprint`),
    KEY `idx_sre_evidence_run_captured` (`investigation_run_id`, `captured_at`),
    KEY `idx_sre_evidence_incident_captured` (`incident_id`, `captured_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SRE 事故证据快照';
