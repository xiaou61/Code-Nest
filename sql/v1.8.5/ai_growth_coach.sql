CREATE TABLE IF NOT EXISTS `ai_growth_coach_snapshot` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `scene_scope` VARCHAR(32) NOT NULL COMMENT '场景范围：LEARNING/CAREER/HYBRID',
  `snapshot_date` DATE DEFAULT NULL COMMENT '快照日期',
  `learning_score` INT NOT NULL DEFAULT 0 COMMENT '学习评分',
  `career_score` INT NOT NULL DEFAULT 0 COMMENT '求职评分',
  `execution_score` INT NOT NULL DEFAULT 0 COMMENT '执行评分',
  `overall_score` INT NOT NULL DEFAULT 0 COMMENT '综合评分',
  `risk_level` VARCHAR(32) DEFAULT 'MEDIUM' COMMENT '风险等级',
  `headline` VARCHAR(255) DEFAULT NULL COMMENT '顶部标题',
  `summary_json` LONGTEXT COMMENT '结构化摘要JSON',
  `source_digest_json` LONGTEXT COMMENT '数据源摘要JSON',
  `fallback_only` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否为本地兜底结果',
  `status` VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/GENERATING/READY/FAILED/EXPIRED',
  `fail_reason` VARCHAR(500) DEFAULT NULL COMMENT '失败原因',
  `generated_at` DATETIME DEFAULT NULL COMMENT '生成时间',
  `expire_at` DATETIME DEFAULT NULL COMMENT '过期时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_aigrowth_snapshot_user_scene` (`user_id`, `scene_scope`),
  KEY `idx_aigrowth_snapshot_status` (`status`),
  KEY `idx_aigrowth_snapshot_generated` (`generated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI成长教练快照表';

CREATE TABLE IF NOT EXISTS `ai_growth_coach_action` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `snapshot_id` BIGINT NOT NULL COMMENT '快照ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `title` VARCHAR(120) NOT NULL COMMENT '动作标题',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '动作描述',
  `priority` VARCHAR(16) NOT NULL DEFAULT 'P1' COMMENT '优先级：P0/P1/P2',
  `action_type` VARCHAR(64) DEFAULT NULL COMMENT '动作类型',
  `target_route` VARCHAR(255) DEFAULT NULL COMMENT '跳转路由',
  `reason` VARCHAR(500) DEFAULT NULL COMMENT '推荐原因',
  `expected_gain` VARCHAR(255) DEFAULT NULL COMMENT '预期收益',
  `estimated_minutes` INT NOT NULL DEFAULT 30 COMMENT '预估耗时（分钟）',
  `status` VARCHAR(32) NOT NULL DEFAULT 'TODO' COMMENT '动作状态：TODO/IN_PROGRESS/DONE/SKIPPED',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序值',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_aigrowth_action_snapshot` (`snapshot_id`),
  KEY `idx_aigrowth_action_user_status` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI成长教练动作表';

CREATE TABLE IF NOT EXISTS `ai_growth_coach_chat_session` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `snapshot_id` BIGINT NOT NULL COMMENT '快照ID',
  `scene_scope` VARCHAR(32) NOT NULL COMMENT '场景范围',
  `title` VARCHAR(120) DEFAULT NULL COMMENT '会话标题',
  `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '会话状态',
  `last_message_at` DATETIME DEFAULT NULL COMMENT '最后消息时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_aigrowth_chat_session_user` (`user_id`),
  KEY `idx_aigrowth_chat_session_snapshot` (`snapshot_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI成长教练对话会话表';

CREATE TABLE IF NOT EXISTS `ai_growth_coach_chat_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `session_id` BIGINT NOT NULL COMMENT '会话ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `role` VARCHAR(16) NOT NULL COMMENT '角色：user/assistant',
  `content` TEXT NOT NULL COMMENT '消息内容',
  `reference_json` LONGTEXT COMMENT '引用依据JSON',
  `status` VARCHAR(32) NOT NULL DEFAULT 'SUCCESS' COMMENT '消息状态',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_aigrowth_chat_message_session` (`session_id`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI成长教练对话消息表';

CREATE TABLE IF NOT EXISTS `ai_growth_coach_replan_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `snapshot_id` BIGINT NOT NULL COMMENT '快照ID',
  `scene_scope` VARCHAR(32) NOT NULL COMMENT '场景范围',
  `available_minutes` INT NOT NULL DEFAULT 0 COMMENT '用户预算分钟数',
  `original_total_minutes` INT NOT NULL DEFAULT 0 COMMENT '原始动作总分钟数',
  `selected_count` INT NOT NULL DEFAULT 0 COMMENT '保留动作数',
  `deferred_count` INT NOT NULL DEFAULT 0 COMMENT '延后动作数',
  `fallback_only` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否走规则兜底',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_aigrowth_replan_user` (`user_id`),
  KEY `idx_aigrowth_replan_snapshot` (`snapshot_id`),
  KEY `idx_aigrowth_replan_scene_time` (`scene_scope`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI成长教练时间压缩重排日志表';

CREATE TABLE IF NOT EXISTS `ai_growth_coach_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `config_key` VARCHAR(64) NOT NULL COMMENT '配置键',
  `config_value` VARCHAR(2000) DEFAULT NULL COMMENT '配置值',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `status` VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '配置状态',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_aigrowth_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI成长教练配置表';

INSERT INTO `ai_growth_coach_config` (`config_key`, `config_value`, `remark`, `status`)
VALUES
  ('snapshot_expire_hours', '12', '快照过期小时数', 'ENABLED'),
  ('max_actions', '4', '默认动作条数上限', 'ENABLED'),
  ('chat_suggested_questions', '为什么优先做这个?|如果我只有5小时怎么办?|这个建议主要基于哪些数据?|我现在更该补学习还是求职?', '默认推荐追问问题', 'ENABLED'),
  ('scene_enabled_learning', 'true', '学习场景是否启用', 'ENABLED'),
  ('scene_enabled_career', 'true', '求职场景是否启用', 'ENABLED'),
  ('scene_enabled_hybrid', 'true', '双场景是否启用', 'ENABLED')
ON DUPLICATE KEY UPDATE
  `config_value` = VALUES(`config_value`),
  `remark` = VALUES(`remark`),
  `status` = VALUES(`status`);
