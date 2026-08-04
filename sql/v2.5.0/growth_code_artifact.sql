-- ===================================
-- AI Growth Coach: Public GitHub code artifact evidence
-- 版本: v2.5.0
-- 说明: 仅保存公开 commit/PR 的最小可验证事实；不证明 Code Nest 用户拥有贡献归属。
-- ===================================

CREATE TABLE IF NOT EXISTS `growth_code_artifact` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `provider` VARCHAR(24) NOT NULL,
    `artifact_type` VARCHAR(24) NOT NULL,
    `repository` VARCHAR(255) NOT NULL,
    `external_id` VARCHAR(128) NOT NULL,
    `canonical_url` VARCHAR(512) NOT NULL,
    `changed_files` INT DEFAULT NULL,
    `additions` INT DEFAULT NULL,
    `deletions` INT DEFAULT NULL,
    `artifact_state` VARCHAR(32) DEFAULT NULL,
    `merged` TINYINT NOT NULL DEFAULT 0,
    `ownership_verified` TINYINT NOT NULL DEFAULT 0,
    `source_observed_at` DATETIME NOT NULL,
    `verified_at` DATETIME NOT NULL,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_growth_code_artifact_user_source` (
        `user_id`, `provider`, `artifact_type`, `repository`, `external_id`
    ),
    KEY `idx_growth_code_artifact_user_verified` (`user_id`, `deleted`, `verified_at`),
    KEY `idx_growth_code_artifact_evidence_change` (`update_time`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户公开代码来源最小事实';
