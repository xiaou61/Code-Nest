-- =============================================
-- OJ 在线判题模块建表 SQL
-- =============================================

-- 题目表
CREATE TABLE IF NOT EXISTS `oj_problem` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '题目ID',
    `title` VARCHAR(255) NOT NULL COMMENT '题目标题',
    `description` TEXT COMMENT '题目描述 (Markdown)',
    `difficulty` VARCHAR(20) NOT NULL DEFAULT 'easy' COMMENT '难度 (easy/medium/hard)',
    `time_limit` INT NOT NULL DEFAULT 2000 COMMENT '时间限制 (ms)',
    `memory_limit` INT NOT NULL DEFAULT 256 COMMENT '内存限制 (MB)',
    `input_description` TEXT COMMENT '输入说明',
    `output_description` TEXT COMMENT '输出说明',
    `sample_input` TEXT COMMENT '示例输入',
    `sample_output` TEXT COMMENT '示例输出',
    `accepted_count` INT NOT NULL DEFAULT 0 COMMENT '通过数',
    `submit_count` INT NOT NULL DEFAULT 0 COMMENT '提交数',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态 (0=隐藏, 1=公开)',
    `create_user_id` BIGINT COMMENT '创建者用户ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_difficulty` (`difficulty`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OJ题目表';

-- 题目标签表
CREATE TABLE IF NOT EXISTS `oj_problem_tag` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '标签ID',
    `name` VARCHAR(50) NOT NULL COMMENT '标签名称',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OJ题目标签表';

-- 题目-标签关联表
CREATE TABLE IF NOT EXISTS `oj_problem_tag_relation` (
    `problem_id` BIGINT NOT NULL COMMENT '题目ID',
    `tag_id` BIGINT NOT NULL COMMENT '标签ID',
    PRIMARY KEY (`problem_id`, `tag_id`),
    INDEX `idx_tag_id` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OJ题目-标签关联表';

-- 测试用例表
CREATE TABLE IF NOT EXISTS `oj_test_case` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用例ID',
    `problem_id` BIGINT NOT NULL COMMENT '题目ID',
    `input` TEXT COMMENT '输入数据',
    `expected_output` TEXT COMMENT '期望输出',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序号',
    `is_sample` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否为示例用例',
    PRIMARY KEY (`id`),
    INDEX `idx_problem_id` (`problem_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OJ测试用例表';

-- 提交记录表
CREATE TABLE IF NOT EXISTS `oj_submission` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '提交ID',
    `problem_id` BIGINT NOT NULL COMMENT '题目ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `language` VARCHAR(20) NOT NULL COMMENT '编程语言',
    `code` TEXT NOT NULL COMMENT '提交的源代码',
    `status` VARCHAR(30) NOT NULL DEFAULT 'pending' COMMENT '判题状态',
    `time_used` BIGINT DEFAULT NULL COMMENT '耗时 (ms)',
    `memory_used` BIGINT DEFAULT NULL COMMENT '内存使用 (KB)',
    `error_message` TEXT COMMENT '错误信息',
    `pass_count` INT DEFAULT NULL COMMENT '通过用例数',
    `total_count` INT DEFAULT NULL COMMENT '总用例数',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    PRIMARY KEY (`id`),
    INDEX `idx_problem_id` (`problem_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_user_problem` (`user_id`, `problem_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OJ提交记录表';

-- =============================================
-- 初始标签数据
-- =============================================
INSERT IGNORE INTO `oj_problem_tag` (`name`) VALUES
('数组'), ('字符串'), ('哈希表'), ('双指针'), ('链表'),
('栈'), ('队列'), ('树'), ('图'), ('排序'),
('二分查找'), ('动态规划'), ('贪心'), ('回溯'), ('递归'),
('DFS'), ('BFS'), ('数学'), ('位运算'), ('模拟');
