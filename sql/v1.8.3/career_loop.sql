-- 求职闭环中台（V1）表结构

CREATE TABLE IF NOT EXISTS `career_loop_session` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `target_role` varchar(128) DEFAULT NULL COMMENT '目标岗位',
  `target_company_type` varchar(64) DEFAULT NULL COMMENT '目标公司类型',
  `current_stage` varchar(32) NOT NULL COMMENT '当前阶段',
  `health_score` int NOT NULL DEFAULT 60 COMMENT '健康分(0-100)',
  `status` varchar(16) NOT NULL DEFAULT 'active' COMMENT '状态:active/completed/archived',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_status` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='求职闭环会话';

CREATE TABLE IF NOT EXISTS `career_loop_stage_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `session_id` bigint NOT NULL COMMENT '会话ID',
  `from_stage` varchar(32) NOT NULL COMMENT '原阶段',
  `to_stage` varchar(32) NOT NULL COMMENT '目标阶段',
  `trigger_source` varchar(32) NOT NULL COMMENT '触发源',
  `trigger_ref_id` varchar(64) DEFAULT NULL COMMENT '触发引用ID',
  `note` varchar(512) DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_session_create_time` (`session_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='求职闭环阶段日志';

CREATE TABLE IF NOT EXISTS `career_loop_action` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `session_id` bigint NOT NULL COMMENT '会话ID',
  `stage` varchar(32) NOT NULL COMMENT '所属阶段',
  `action_type` varchar(32) NOT NULL COMMENT '动作类型',
  `title` varchar(128) NOT NULL COMMENT '动作标题',
  `description` varchar(512) DEFAULT NULL COMMENT '动作说明',
  `priority` varchar(8) NOT NULL DEFAULT 'P1' COMMENT '优先级',
  `status` varchar(16) NOT NULL DEFAULT 'todo' COMMENT '状态:todo/doing/done',
  `due_date` date DEFAULT NULL COMMENT '截止日期',
  `source` varchar(32) NOT NULL DEFAULT 'rule' COMMENT '来源',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_session_status` (`session_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='求职闭环动作清单';

CREATE TABLE IF NOT EXISTS `career_loop_snapshot` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `session_id` bigint NOT NULL COMMENT '会话ID',
  `plan_progress` int DEFAULT 0 COMMENT '计划进度(0-100)',
  `mock_count` int DEFAULT 0 COMMENT '模拟面试次数',
  `latest_mock_score` int DEFAULT NULL COMMENT '最近模拟面试分数',
  `review_count` int DEFAULT 0 COMMENT '复盘次数',
  `resume_updated_at` datetime DEFAULT NULL COMMENT '简历最近更新时间',
  `risk_flags_json` text COMMENT '风险标记JSON',
  `next_suggestion_json` text COMMENT '下一步建议JSON',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_id` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='求职闭环快照';
