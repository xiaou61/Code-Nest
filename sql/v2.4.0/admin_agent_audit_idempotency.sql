-- 管理员端超级智能体写入动作幂等键增量迁移。
-- 说明：admin_agent_audit.sql 只负责新库建表；已部署库需要通过本脚本补齐字段和唯一索引。

SET @agent_audit_table_exists := (
  SELECT COUNT(1)
  FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_agent_audit'
);

SET @agent_audit_idempotency_column_exists := (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_agent_audit'
    AND COLUMN_NAME = 'idempotency_key'
);

SET @agent_audit_idempotency_column_sql := IF(
  @agent_audit_table_exists = 1 AND @agent_audit_idempotency_column_exists = 0,
  'ALTER TABLE `sys_agent_audit` ADD COLUMN `idempotency_key` varchar(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT ''写入动作幂等键'' AFTER `confirmation_id`',
  'SELECT ''sys_agent_audit.idempotency_key already exists or sys_agent_audit is missing'' AS info'
);

PREPARE agent_audit_idempotency_column_stmt FROM @agent_audit_idempotency_column_sql;
EXECUTE agent_audit_idempotency_column_stmt;
DEALLOCATE PREPARE agent_audit_idempotency_column_stmt;

SET @agent_audit_idempotency_index_exists := (
  SELECT COUNT(1)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_agent_audit'
    AND INDEX_NAME = 'uk_agent_idempotency_key'
);

SET @agent_audit_idempotency_column_exists := (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_agent_audit'
    AND COLUMN_NAME = 'idempotency_key'
);

SET @agent_audit_idempotency_index_sql := IF(
  @agent_audit_table_exists = 1
    AND @agent_audit_idempotency_column_exists = 1
    AND @agent_audit_idempotency_index_exists = 0,
  'ALTER TABLE `sys_agent_audit` ADD UNIQUE INDEX `uk_agent_idempotency_key` (`idempotency_key` ASC) USING BTREE',
  'SELECT ''sys_agent_audit.uk_agent_idempotency_key already exists or sys_agent_audit is missing'' AS info'
);

PREPARE agent_audit_idempotency_index_stmt FROM @agent_audit_idempotency_index_sql;
EXECUTE agent_audit_idempotency_index_stmt;
DEALLOCATE PREPARE agent_audit_idempotency_index_stmt;
