-- 学习资产转化引擎（V1）表结构

CREATE TABLE IF NOT EXISTS `learning_asset_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '发起用户ID',
  `source_type` varchar(32) NOT NULL COMMENT '来源类型：blog/community/codepen/mock_interview',
  `source_id` bigint NOT NULL COMMENT '来源内容ID',
  `source_title` varchar(255) DEFAULT NULL COMMENT '来源标题',
  `source_author_id` bigint DEFAULT NULL COMMENT '来源作者ID',
  `source_snapshot` longtext COMMENT '来源快照JSON',
  `transform_mode` varchar(32) DEFAULT NULL COMMENT '转化模式',
  `target_types` varchar(255) DEFAULT NULL COMMENT '目标资产类型，逗号分隔',
  `status` varchar(32) NOT NULL DEFAULT 'PENDING_CONFIRM' COMMENT '状态',
  `source_hash` varchar(64) DEFAULT NULL COMMENT '来源内容哈希',
  `summary_text` text COMMENT '转化摘要',
  `fail_reason` varchar(500) DEFAULT NULL COMMENT '失败原因',
  `total_candidates` int NOT NULL DEFAULT 0 COMMENT '候选总数',
  `published_candidates` int NOT NULL DEFAULT 0 COMMENT '已发布数量',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_status` (`user_id`, `status`),
  KEY `idx_source` (`source_type`, `source_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习资产转化记录';

CREATE TABLE IF NOT EXISTS `learning_asset_candidate` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `record_id` bigint NOT NULL COMMENT '转化记录ID',
  `user_id` bigint NOT NULL COMMENT '所属用户ID',
  `asset_type` varchar(32) NOT NULL COMMENT '资产类型',
  `title` varchar(255) NOT NULL COMMENT '候选标题',
  `content_json` longtext NOT NULL COMMENT '结构化内容JSON',
  `tags` varchar(255) DEFAULT NULL COMMENT '标签',
  `difficulty` varchar(32) DEFAULT NULL COMMENT '难度建议',
  `confidence_score` decimal(5,2) DEFAULT NULL COMMENT '置信分',
  `dedupe_key` varchar(128) DEFAULT NULL COMMENT '去重Key',
  `target_module` varchar(64) DEFAULT NULL COMMENT '目标模块',
  `target_id` bigint DEFAULT NULL COMMENT '目标记录ID',
  `status` varchar(32) NOT NULL DEFAULT 'DRAFT' COMMENT '候选状态',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序序号',
  `review_note` varchar(500) DEFAULT NULL COMMENT '审核说明',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_record_status` (`record_id`, `status`),
  KEY `idx_user_asset_type` (`user_id`, `asset_type`),
  KEY `idx_dedupe_key` (`dedupe_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习资产候选项';

CREATE TABLE IF NOT EXISTS `learning_asset_publish_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `candidate_id` bigint NOT NULL COMMENT '候选项ID',
  `publish_type` varchar(32) NOT NULL COMMENT '发布类型',
  `target_module` varchar(64) NOT NULL COMMENT '目标模块',
  `target_id` bigint DEFAULT NULL COMMENT '目标记录ID',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人ID',
  `publish_result` varchar(32) NOT NULL COMMENT '发布结果',
  `message` varchar(500) DEFAULT NULL COMMENT '结果说明',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_candidate_create_time` (`candidate_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习资产发布日志';
