-- v2.5.0：AI 成长教练高风险周节奏的站内提醒。
-- 同一用户同一周只有一条同类提醒，提醒只引导用户查看复盘和确认式调整。

CREATE TABLE IF NOT EXISTS `growth_coach_nudge` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `nudge_key` VARCHAR(120) NOT NULL,
    `nudge_type` VARCHAR(40) NOT NULL,
    `level` VARCHAR(32) NOT NULL,
    `title` VARCHAR(160) NOT NULL,
    `content` VARCHAR(500) NOT NULL,
    `route_path` VARCHAR(255) DEFAULT NULL,
    `status` VARCHAR(16) NOT NULL,
    `notification_id` BIGINT DEFAULT NULL,
    `sent_at` DATETIME DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_growth_coach_nudge_user_key` (`user_id`, `nudge_key`),
    KEY `idx_growth_coach_nudge_status_time` (`status`, `update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI成长教练主动提醒幂等记录';
