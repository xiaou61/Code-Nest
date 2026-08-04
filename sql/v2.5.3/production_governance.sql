-- Code-Nest v2.5.3 production governance migration.
-- Applied by scripts/db-migrate.py after v2.5.0 and v2.5.2 business migrations.

CREATE TABLE IF NOT EXISTS `code_nest_schema_migration` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `migration_key` VARCHAR(255) NOT NULL,
    `version` VARCHAR(64) NOT NULL,
    `name` VARCHAR(190) NOT NULL,
    `checksum` CHAR(64) NOT NULL,
    `status` VARCHAR(16) NOT NULL DEFAULT 'APPLIED',
    `execution_ms` BIGINT NOT NULL DEFAULT 0,
    `applied_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `error_message` VARCHAR(500) NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_schema_migration_key` (`migration_key`),
    KEY `idx_schema_migration_version` (`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Code-Nest schema migration ledger';

ALTER TABLE `growth_journey_event`
    ADD COLUMN `schema_version` VARCHAR(16) NOT NULL DEFAULT '1' COMMENT '事件契约版本' AFTER `source`,
    ADD COLUMN `client_version` VARCHAR(32) NULL COMMENT '客户端版本' AFTER `schema_version`,
    ADD COLUMN `entry_page` VARCHAR(128) NULL COMMENT '事件入口页面' AFTER `client_version`;

CREATE INDEX `idx_growth_journey_schema_time`
    ON `growth_journey_event` (`schema_version`, `create_time`);
