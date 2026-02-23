-- =============================================
-- OJ 赛事系统建表 SQL
-- 版本：v1.8.1
-- =============================================

-- 赛事主表
CREATE TABLE IF NOT EXISTS `oj_contest` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '赛事ID',
    `title` VARCHAR(255) NOT NULL COMMENT '赛事标题',
    `description` TEXT COMMENT '赛事描述',
    `contest_type` VARCHAR(32) NOT NULL DEFAULT 'weekly' COMMENT '赛事类型(weekly/challenge)',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态(0=草稿,1=即将开始,2=进行中,3=已结束)',
    `start_time` DATETIME NOT NULL COMMENT '开始时间',
    `end_time` DATETIME NOT NULL COMMENT '结束时间',
    `created_by` BIGINT NOT NULL COMMENT '创建人ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_contest_type` (`contest_type`),
    INDEX `idx_time_window` (`start_time`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OJ赛事表';

-- 赛事题目关联表
CREATE TABLE IF NOT EXISTS `oj_contest_problem` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '关联ID',
    `contest_id` BIGINT NOT NULL COMMENT '赛事ID',
    `problem_id` BIGINT NOT NULL COMMENT '题目ID',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序号',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_contest_problem` (`contest_id`, `problem_id`),
    INDEX `idx_problem_id` (`problem_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OJ赛事题目关联表';

-- 赛事报名表
CREATE TABLE IF NOT EXISTS `oj_contest_participant` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '报名ID',
    `contest_id` BIGINT NOT NULL COMMENT '赛事ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `join_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '报名时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_contest_user` (`contest_id`, `user_id`),
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OJ赛事参赛者表';

-- 提交记录增加赛事ID
ALTER TABLE `oj_submission` ADD COLUMN `contest_id` BIGINT NULL COMMENT '赛事ID(为空表示普通提交)';
ALTER TABLE `oj_submission` ADD INDEX `idx_contest_id` (`contest_id`);
