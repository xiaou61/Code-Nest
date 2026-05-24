-- =============================================
-- 学习小组模块数据库表
-- 版本: v1.0.0
-- 创建时间: 2026-01-05
-- =============================================

-- ----------------------------
-- 1. 学习小组表
-- ----------------------------
DROP TABLE IF EXISTS `study_team`;
CREATE TABLE `study_team` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '小组ID',
    `team_name` VARCHAR(50) NOT NULL COMMENT '小组名称',
    `team_desc` VARCHAR(500) COMMENT '小组简介',
    `team_avatar` VARCHAR(255) COMMENT '小组头像',
    `team_type` TINYINT NOT NULL DEFAULT 1 COMMENT '类型：1目标型 2学习型 3打卡型',
    `tags` VARCHAR(255) COMMENT '标签，逗号分隔',
    `max_members` INT NOT NULL DEFAULT 20 COMMENT '最大成员数',
    `current_members` INT NOT NULL DEFAULT 1 COMMENT '当前成员数',
    `join_type` TINYINT NOT NULL DEFAULT 1 COMMENT '加入方式：1公开 2申请 3邀请',
    `invite_code` VARCHAR(20) COMMENT '邀请码',
    
    -- 小组目标
    `goal_title` VARCHAR(100) COMMENT '目标标题',
    `goal_desc` VARCHAR(500) COMMENT '目标描述',
    `goal_start_date` DATE COMMENT '目标开始日期',
    `goal_end_date` DATE COMMENT '目标结束日期',
    `daily_target` INT COMMENT '每日目标量',
    
    -- 统计数据
    `total_checkins` INT DEFAULT 0 COMMENT '总打卡次数',
    `total_discussions` INT DEFAULT 0 COMMENT '总讨论数',
    `active_days` INT DEFAULT 0 COMMENT '活跃天数',
    
    `creator_id` BIGINT NOT NULL COMMENT '创建者ID',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0已解散 1正常 2已满员',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    UNIQUE KEY `uk_invite_code` (`invite_code`),
    INDEX `idx_creator_id` (`creator_id`),
    INDEX `idx_team_type` (`team_type`),
    INDEX `idx_status` (`status`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习小组表';

-- ----------------------------
-- 2. 小组成员表
-- ----------------------------
DROP TABLE IF EXISTS `study_team_member`;
CREATE TABLE `study_team_member` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    `team_id` BIGINT NOT NULL COMMENT '小组ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `role` TINYINT NOT NULL DEFAULT 3 COMMENT '角色：1组长 2管理员 3成员',
    `nickname` VARCHAR(50) COMMENT '组内昵称',
    `join_reason` VARCHAR(200) COMMENT '加入理由',
    
    -- 个人统计
    `total_checkins` INT DEFAULT 0 COMMENT '总打卡次数',
    `current_streak` INT DEFAULT 0 COMMENT '当前连续打卡',
    `max_streak` INT DEFAULT 0 COMMENT '最长连续打卡',
    `total_likes_received` INT DEFAULT 0 COMMENT '获得点赞数',
    `contribution_points` INT DEFAULT 0 COMMENT '贡献积分',
    
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0已退出 1正常 2禁言中',
    `mute_end_time` DATETIME COMMENT '禁言结束时间',
    `join_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    `last_active_time` DATETIME COMMENT '最后活跃时间',
    `last_checkin_time` DATETIME COMMENT '最后打卡时间',
    
    UNIQUE KEY `uk_team_user` (`team_id`, `user_id`),
    INDEX `idx_team_id` (`team_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_role` (`role`),
    INDEX `idx_join_time` (`join_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='小组成员表';

-- ----------------------------
-- 3. 小组申请表
-- ----------------------------
DROP TABLE IF EXISTS `study_team_application`;
CREATE TABLE `study_team_application` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '申请ID',
    `team_id` BIGINT NOT NULL COMMENT '小组ID',
    `user_id` BIGINT NOT NULL COMMENT '申请人ID',
    `apply_reason` VARCHAR(200) COMMENT '申请理由',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0待审核 1已通过 2已拒绝 3已取消',
    `reviewer_id` BIGINT COMMENT '审核人ID',
    `review_time` DATETIME COMMENT '审核时间',
    `reject_reason` VARCHAR(200) COMMENT '拒绝原因',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    
    INDEX `idx_team_id` (`team_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='小组申请表';

-- ----------------------------
-- 4. 打卡任务表
-- ----------------------------
DROP TABLE IF EXISTS `study_team_task`;
CREATE TABLE `study_team_task` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '任务ID',
    `team_id` BIGINT NOT NULL COMMENT '小组ID',
    `task_name` VARCHAR(100) NOT NULL COMMENT '任务名称',
    `task_desc` VARCHAR(500) COMMENT '任务描述',
    `task_type` TINYINT NOT NULL DEFAULT 1 COMMENT '类型：1刷题 2学习时长 3阅读 4自定义',
    `target_value` INT NOT NULL COMMENT '目标数量',
    `target_unit` VARCHAR(20) COMMENT '目标单位',
    `repeat_type` TINYINT NOT NULL DEFAULT 1 COMMENT '重复：1每日 2工作日 3自定义',
    `repeat_days` VARCHAR(20) COMMENT '自定义重复日',
    `require_content` TINYINT DEFAULT 0 COMMENT '是否必须附带内容',
    `require_image` TINYINT DEFAULT 0 COMMENT '是否必须附带图片',
    `start_date` DATE NOT NULL COMMENT '开始日期',
    `end_date` DATE COMMENT '结束日期',
    `creator_id` BIGINT NOT NULL COMMENT '创建人 ID',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0已结束 1进行中',
    `is_deleted` TINYINT DEFAULT 0 COMMENT '是否删除：0否 1是',
    `create_by` BIGINT COMMENT '创建人',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` BIGINT COMMENT '更新人',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX `idx_team_id` (`team_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_start_date` (`start_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='小组打卡任务表';

-- ----------------------------
-- 5. 打卡记录表
-- ----------------------------
DROP TABLE IF EXISTS `study_team_checkin`;
CREATE TABLE `study_team_checkin` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '打卡ID',
    `team_id` BIGINT NOT NULL COMMENT '小组ID',
    `task_id` BIGINT NOT NULL COMMENT '任务ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `checkin_date` DATE NOT NULL COMMENT '打卡日期',
    `complete_value` INT NOT NULL COMMENT '完成数量',
    `content` VARCHAR(1000) COMMENT '学习内容',
    `images` VARCHAR(2000) COMMENT '图片URL，逗号分隔',
    `feeling` VARCHAR(500) COMMENT '心得感悟',
    `related_question_ids` VARCHAR(500) COMMENT '关联题目ID',
    `is_supplement` TINYINT DEFAULT 0 COMMENT '是否补卡',
    `like_count` INT DEFAULT 0 COMMENT '点赞数',
    `comment_count` INT DEFAULT 0 COMMENT '评论数',
    `duration` INT COMMENT '学习时长（分钟）',
    `related_link` VARCHAR(500) COMMENT '相关链接',
    `is_deleted` TINYINT DEFAULT 0 COMMENT '是否删除：0否 1是',
    `create_by` BIGINT COMMENT '创建人',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '打卡时间',
    `update_by` BIGINT COMMENT '更新人',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    UNIQUE KEY `uk_task_user_date` (`task_id`, `user_id`, `checkin_date`),
    INDEX `idx_team_id` (`team_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_checkin_date` (`checkin_date`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='小组打卡记录表';

-- ----------------------------
-- 6. 打卡点赞表
-- ----------------------------
DROP TABLE IF EXISTS `study_team_checkin_like`;
CREATE TABLE `study_team_checkin_like` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '点赞ID',
    `checkin_id` BIGINT NOT NULL COMMENT '打卡ID',
    `user_id` BIGINT NOT NULL COMMENT '点赞用户ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    
    UNIQUE KEY `uk_checkin_user` (`checkin_id`, `user_id`),
    INDEX `idx_checkin_id` (`checkin_id`),
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='打卡点赞表';

-- ----------------------------
-- 7. 打卡评论表
-- ----------------------------
DROP TABLE IF EXISTS `study_team_checkin_comment`;
CREATE TABLE `study_team_checkin_comment` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '评论ID',
    `checkin_id` BIGINT NOT NULL COMMENT '打卡ID',
    `user_id` BIGINT NOT NULL COMMENT '评论用户ID',
    `parent_id` BIGINT DEFAULT 0 COMMENT '父评论ID',
    `reply_user_id` BIGINT COMMENT '回复用户ID',
    `content` VARCHAR(500) NOT NULL COMMENT '评论内容',
    `like_count` INT DEFAULT 0 COMMENT '点赞数',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0已删除 1正常',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间',
    
    INDEX `idx_checkin_id` (`checkin_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='打卡评论表';

-- ----------------------------
-- 8. 小组讨论表
-- ----------------------------
DROP TABLE IF EXISTS `study_team_discussion`;
CREATE TABLE `study_team_discussion` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '讨论ID',
    `team_id` BIGINT NOT NULL COMMENT '小组ID',
    `user_id` BIGINT NOT NULL COMMENT '发布者ID',
    `category` TINYINT NOT NULL DEFAULT 5 COMMENT '分类：1公告 2问题 3笔记 4经验 5闲聊',
    `title` VARCHAR(100) NOT NULL COMMENT '标题',
    `content` TEXT NOT NULL COMMENT '内容',
    `images` VARCHAR(2000) COMMENT '图片URL',
    `is_pinned` TINYINT DEFAULT 0 COMMENT '是否置顶',
    `is_essence` TINYINT DEFAULT 0 COMMENT '是否精华',
    `view_count` INT DEFAULT 0 COMMENT '浏览数',
    `like_count` INT DEFAULT 0 COMMENT '点赞数',
    `comment_count` INT DEFAULT 0 COMMENT '评论数',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0已删除 1正常',
    `is_deleted` TINYINT DEFAULT 0 COMMENT '是否删除：0否 1是',
    `create_by` BIGINT COMMENT '创建人',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    `update_by` BIGINT COMMENT '更新人',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `last_reply_time` DATETIME COMMENT '最后回复时间',
    
    INDEX `idx_team_id` (`team_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_category` (`category`),
    INDEX `idx_is_pinned` (`is_pinned`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='小组讨论表';

-- ----------------------------
-- 9. 讨论点赞表
-- ----------------------------
DROP TABLE IF EXISTS `study_team_discussion_like`;
CREATE TABLE `study_team_discussion_like` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '点赞ID',
    `discussion_id` BIGINT NOT NULL COMMENT '讨论ID',
    `user_id` BIGINT NOT NULL COMMENT '点赞用户ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',

    UNIQUE KEY `uk_discussion_user` (`discussion_id`, `user_id`),
    INDEX `idx_discussion_id` (`discussion_id`),
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='讨论点赞表';

-- ----------------------------
-- 10. 小组每日统计表
-- ----------------------------
DROP TABLE IF EXISTS `study_team_daily_stats`;
CREATE TABLE `study_team_daily_stats` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '统计ID',
    `team_id` BIGINT NOT NULL COMMENT '小组ID',
    `stat_date` DATE NOT NULL COMMENT '统计日期',
    `checkin_count` INT DEFAULT 0 COMMENT '打卡人数',
    `member_count` INT DEFAULT 0 COMMENT '当日成员数',
    `checkin_rate` DECIMAL(5,2) DEFAULT 0 COMMENT '打卡率',
    `discussion_count` INT DEFAULT 0 COMMENT '讨论数',
    `new_member_count` INT DEFAULT 0 COMMENT '新加入成员',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    UNIQUE KEY `uk_team_date` (`team_id`, `stat_date`),
    INDEX `idx_team_id` (`team_id`),
    INDEX `idx_stat_date` (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='小组每日统计表';
