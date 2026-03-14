-- ===================================
-- 岗位匹配引擎2.0分析记录表
-- 版本: v1.8.2
-- 创建时间: 2026-03-04
-- ===================================

CREATE TABLE IF NOT EXISTS `job_battle_match_record` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `analysis_name` VARCHAR(255) NOT NULL COMMENT '分析名称',
    `target_count` INT NOT NULL DEFAULT 0 COMMENT '目标岗位数量',
    `best_score` INT NOT NULL DEFAULT 0 COMMENT '最佳匹配分',
    `average_score` INT NOT NULL DEFAULT 0 COMMENT '平均匹配分',
    `fallback_count` INT NOT NULL DEFAULT 0 COMMENT '降级岗位数量',
    `best_target_role` VARCHAR(128) DEFAULT NULL COMMENT '最佳岗位名称',
    `resume_text` MEDIUMTEXT COMMENT '简历文本快照',
    `project_highlights` TEXT COMMENT '项目亮点快照',
    `target_company_type` VARCHAR(64) DEFAULT NULL COMMENT '目标公司类型',
    `result_json` LONGTEXT COMMENT '完整分析结果JSON',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_user_create_time` (`user_id`, `create_time` DESC),
    INDEX `idx_user_best_score` (`user_id`, `best_score` DESC),
    INDEX `idx_user_analysis_name` (`user_id`, `analysis_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='岗位匹配引擎2.0分析记录';

