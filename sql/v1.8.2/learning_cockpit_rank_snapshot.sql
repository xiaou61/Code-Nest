-- ===================================
-- 学习成长驾驶舱排名快照表
-- 版本: v1.8.2
-- 创建时间: 2026-03-01
-- ===================================

CREATE TABLE IF NOT EXISTS `learning_cockpit_rank_snapshot` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `week_start` DATE NOT NULL COMMENT '周起始日期（周一）',
    `week_end` DATE NOT NULL COMMENT '周结束日期（周日）',
    `weekly_rank` INT DEFAULT NULL COMMENT '周榜排名',
    `all_rank` INT DEFAULT NULL COMMENT '总榜排名',
    `weekly_population` INT DEFAULT 0 COMMENT '周榜参与人数',
    `all_population` INT DEFAULT 0 COMMENT '总榜参与人数',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `uk_user_week_start` (`user_id`, `week_start`),
    KEY `idx_user_week` (`user_id`, `week_start` DESC),
    KEY `idx_week_start` (`week_start`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习驾驶舱排名快照';

