-- ===================================
-- AI Growth Coach: CodePen AI review evidence
-- 版本: v2.5.0
-- 说明: 只保存用户自有 CodePen 已保存版本的哈希和结构化审查，不保存源码副本。
-- ===================================

CREATE TABLE IF NOT EXISTS `growth_code_review_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `code_pen_id` BIGINT NOT NULL,
    `previous_review_id` BIGINT DEFAULT NULL,
    `source_hash` CHAR(64) NOT NULL,
    `score` INT NOT NULL,
    `critical_finding_count` INT NOT NULL DEFAULT 0,
    `high_finding_count` INT NOT NULL DEFAULT 0,
    `medium_finding_count` INT NOT NULL DEFAULT 0,
    `summary` VARCHAR(240) DEFAULT NULL,
    `result_json` LONGTEXT DEFAULT NULL,
    `status` VARCHAR(24) NOT NULL,
    `source_observed_at` DATETIME NOT NULL,
    `reviewed_at` DATETIME NOT NULL,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_growth_code_review_user_pen_time` (`user_id`, `code_pen_id`, `deleted`, `reviewed_at`),
    KEY `idx_growth_code_review_evidence_change` (`update_time`, `user_id`),
    KEY `idx_growth_code_review_previous` (`previous_review_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户自有CodePen结构化AI审查记录';
