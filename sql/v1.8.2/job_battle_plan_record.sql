-- ===================================
-- 求职作战台计划历史表
-- 版本: v1.8.2
-- 创建时间: 2026-02-28
-- ===================================

CREATE TABLE IF NOT EXISTS `job_battle_plan_record` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `plan_name` VARCHAR(255) NOT NULL COMMENT '计划名称',
    `total_days` INT DEFAULT NULL COMMENT '计划总天数（AI输出）',
    `target_days` INT DEFAULT NULL COMMENT '目标天数（用户输入）',
    `weekly_hours` INT DEFAULT NULL COMMENT '每周投入小时',
    `preferred_learning_mode` VARCHAR(64) DEFAULT NULL COMMENT '学习偏好',
    `next_interview_date` VARCHAR(32) DEFAULT NULL COMMENT '下一场面试日期',
    `gaps_json` TEXT COMMENT '差距项JSON',
    `plan_result_json` LONGTEXT COMMENT '计划完整结果JSON',
    `is_fallback` TINYINT(1) DEFAULT 0 COMMENT '是否降级结果：0-否 1-是',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_user_create_time` (`user_id`, `create_time` DESC),
    INDEX `idx_user_plan_name` (`user_id`, `plan_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='求职作战台计划历史';

