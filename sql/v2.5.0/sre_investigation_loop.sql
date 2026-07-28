-- Code-Nest SRE v2.5.0: bounded read-only investigation evidence.
-- Apply once after sre_incident_evidence.sql and sre_investigation_run.sql while RCA traffic is stopped.

ALTER TABLE `sre_incident_evidence`
    MODIFY COLUMN `outbox_event_id` BIGINT NULL COMMENT '生成该证据的Outbox事件ID，调查轮次证据为空',
    ADD COLUMN `investigation_run_id` BIGINT NULL COMMENT '生成该证据的调查运行ID，Outbox证据为空' AFTER `outbox_event_id`,
    ADD COLUMN `query_fingerprint` CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL COMMENT '固定只读查询SHA-256' AFTER `investigation_run_id`,
    ADD UNIQUE KEY `uk_sre_evidence_run_query` (`investigation_run_id`, `query_fingerprint`),
    ADD KEY `idx_sre_evidence_run_captured` (`investigation_run_id`, `captured_at`);
