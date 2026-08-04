-- ===================================
-- AI Growth Coach: GitHub OAuth account connection
-- 版本: v2.5.0
-- 说明: OAuth 访问令牌仅以 AES-256-GCM 密文保存；解绑时物理删除凭据。
-- ===================================

CREATE TABLE IF NOT EXISTS `growth_github_connection` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `github_user_id` BIGINT NOT NULL,
    `github_login` VARCHAR(100) NOT NULL,
    `github_name` VARCHAR(255) DEFAULT NULL,
    `avatar_url` VARCHAR(1024) DEFAULT NULL,
    `access_token_ciphertext` VARCHAR(4096) NOT NULL,
    `connected_at` DATETIME NOT NULL,
    `token_updated_at` DATETIME NOT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_growth_github_connection_user` (`user_id`),
    UNIQUE KEY `uk_growth_github_connection_account` (`github_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Growth Coach GitHub OAuth 账号连接';

ALTER TABLE `growth_code_artifact`
    ADD COLUMN `github_author_id` BIGINT DEFAULT NULL COMMENT 'GitHub API 返回的作者用户 ID' AFTER `ownership_verified`,
    ADD COLUMN `github_committer_id` BIGINT DEFAULT NULL COMMENT 'GitHub API 返回的提交者用户 ID' AFTER `github_author_id`;
