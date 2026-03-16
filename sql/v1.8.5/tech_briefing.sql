CREATE TABLE IF NOT EXISTS `tech_briefing_source` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `source_name` VARCHAR(64) NOT NULL COMMENT '来源名称',
    `source_type` VARCHAR(16) NOT NULL COMMENT '来源类型：DOMESTIC/GLOBAL',
    `fetch_type` VARCHAR(16) NOT NULL COMMENT '抓取类型：RSS/API/PARSER',
    `base_url` VARCHAR(255) DEFAULT NULL COMMENT '来源首页',
    `rss_url` VARCHAR(500) DEFAULT NULL COMMENT 'RSS地址',
    `status` VARCHAR(16) NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED/DISABLED',
    `weight` INT NOT NULL DEFAULT 0 COMMENT '来源权重',
    `cron_expr` VARCHAR(64) DEFAULT NULL COMMENT '自定义cron',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tech_briefing_source_name` (`source_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='科技热点来源表';

CREATE TABLE IF NOT EXISTS `tech_briefing_article` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `source_id` BIGINT NOT NULL COMMENT '来源ID',
    `source_name` VARCHAR(64) NOT NULL COMMENT '来源名称快照',
    `title` VARCHAR(512) NOT NULL COMMENT '原始标题',
    `title_zh` VARCHAR(512) DEFAULT NULL COMMENT '中文标题',
    `summary` TEXT DEFAULT NULL COMMENT '原始摘要',
    `summary_zh` TEXT DEFAULT NULL COMMENT '中文摘要',
    `source_url` VARCHAR(1000) NOT NULL COMMENT '原文链接',
    `cover_image` VARCHAR(1000) DEFAULT NULL COMMENT '封面图',
    `language` VARCHAR(16) DEFAULT NULL COMMENT '内容语言',
    `region_type` VARCHAR(16) NOT NULL COMMENT '区域：DOMESTIC/GLOBAL',
    `category` VARCHAR(64) DEFAULT NULL COMMENT '主分类',
    `tags_json` TEXT DEFAULT NULL COMMENT '标签JSON',
    `hot_score` INT NOT NULL DEFAULT 0 COMMENT '热度分',
    `publish_time` DATETIME DEFAULT NULL COMMENT '发布时间',
    `status` VARCHAR(16) NOT NULL DEFAULT 'READY' COMMENT '状态：READY/OFFLINE',
    `is_pinned` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否置顶',
    `translation_status` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT '翻译状态',
    `ai_summary_status` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'AI摘要状态',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tech_briefing_article_source_url` (`source_url`(255)),
    KEY `idx_tech_briefing_article_source_id` (`source_id`),
    KEY `idx_tech_briefing_article_publish_time` (`publish_time`),
    KEY `idx_tech_briefing_article_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='科技热点文章表';

CREATE TABLE IF NOT EXISTS `tech_briefing_article_content` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `article_id` BIGINT NOT NULL COMMENT '文章ID',
    `raw_content` MEDIUMTEXT DEFAULT NULL COMMENT '原始正文',
    `translated_content_zh` MEDIUMTEXT DEFAULT NULL COMMENT '中文正文',
    `content_extract_status` VARCHAR(16) NOT NULL DEFAULT 'READY' COMMENT '正文提取状态',
    `translation_status` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT '翻译状态',
    `token_usage` INT NOT NULL DEFAULT 0 COMMENT 'token消耗',
    `copyright_mode` VARCHAR(16) NOT NULL DEFAULT 'SUMMARY_ONLY' COMMENT '版权模式',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tech_briefing_article_content_article_id` (`article_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='科技热点文章内容表';

CREATE TABLE IF NOT EXISTS `tech_briefing_article_ai` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `article_id` BIGINT NOT NULL COMMENT '文章ID',
    `summary_json` TEXT DEFAULT NULL COMMENT '摘要JSON',
    `keywords_json` TEXT DEFAULT NULL COMMENT '关键词JSON',
    `why_important` TEXT DEFAULT NULL COMMENT '为什么重要',
    `impact_scope` TEXT DEFAULT NULL COMMENT '影响范围',
    `model_name` VARCHAR(64) DEFAULT NULL COMMENT '模型名称',
    `status` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT '状态',
    `fail_reason` VARCHAR(255) DEFAULT NULL COMMENT '失败原因',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tech_briefing_article_ai_article_id` (`article_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='科技热点AI结果表';

CREATE TABLE IF NOT EXISTS `tech_briefing_subscription` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT DEFAULT NULL COMMENT '用户ID',
    `channel_type` VARCHAR(16) NOT NULL COMMENT '渠道类型：FEISHU/DINGTALK',
    `webhook_url` VARCHAR(1000) NOT NULL COMMENT 'Webhook地址',
    `webhook_secret` VARCHAR(255) DEFAULT NULL COMMENT 'Webhook密钥',
    `target_name` VARCHAR(128) DEFAULT NULL COMMENT '目标名称',
    `config_hash` VARCHAR(64) NOT NULL COMMENT '配置摘要',
    `topic_preferences` VARCHAR(512) DEFAULT NULL COMMENT '订阅主题',
    `frequency` VARCHAR(16) NOT NULL DEFAULT 'DAILY' COMMENT '订阅频率',
    `status` VARCHAR(16) NOT NULL DEFAULT 'ENABLED' COMMENT '状态',
    `source` VARCHAR(32) NOT NULL DEFAULT 'PAGE' COMMENT '来源',
    `last_push_at` DATETIME DEFAULT NULL COMMENT '最近推送时间',
    `last_test_at` DATETIME DEFAULT NULL COMMENT '最近测试时间',
    `last_push_status` VARCHAR(32) DEFAULT NULL COMMENT '最近推送状态',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tech_briefing_subscription_hash` (`config_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='科技热点订阅表';

CREATE TABLE IF NOT EXISTS `tech_briefing_fetch_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `source_id` BIGINT DEFAULT NULL COMMENT '来源ID',
    `task_type` VARCHAR(32) NOT NULL COMMENT '任务类型',
    `success_count` INT NOT NULL DEFAULT 0 COMMENT '成功数量',
    `fail_count` INT NOT NULL DEFAULT 0 COMMENT '失败数量',
    `status` VARCHAR(16) NOT NULL DEFAULT 'SUCCESS' COMMENT '任务状态',
    `message` VARCHAR(500) DEFAULT NULL COMMENT '执行消息',
    `start_time` DATETIME DEFAULT NULL COMMENT '开始时间',
    `end_time` DATETIME DEFAULT NULL COMMENT '结束时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_tech_briefing_fetch_log_source_id` (`source_id`),
    KEY `idx_tech_briefing_fetch_log_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='科技热点抓取日志表';

INSERT INTO `tech_briefing_source` (`source_name`, `source_type`, `fetch_type`, `base_url`, `rss_url`, `status`, `weight`, `remark`)
SELECT 'TechCrunch', 'GLOBAL', 'RSS', 'https://techcrunch.com', 'https://techcrunch.com/feed/', 'ENABLED', 100, '默认全球科技源'
WHERE NOT EXISTS (
    SELECT 1 FROM `tech_briefing_source` WHERE `source_name` = 'TechCrunch'
);

INSERT INTO `tech_briefing_source` (`source_name`, `source_type`, `fetch_type`, `base_url`, `rss_url`, `status`, `weight`, `remark`)
SELECT 'Ars Technica', 'GLOBAL', 'RSS', 'https://arstechnica.com', 'https://arstechnica.com/feed/', 'ENABLED', 90, '默认全球科技源'
WHERE NOT EXISTS (
    SELECT 1 FROM `tech_briefing_source` WHERE `source_name` = 'Ars Technica'
);

INSERT INTO `tech_briefing_source` (`source_name`, `source_type`, `fetch_type`, `base_url`, `rss_url`, `status`, `weight`, `remark`)
SELECT 'VentureBeat', 'GLOBAL', 'RSS', 'https://venturebeat.com', 'https://venturebeat.com/feed/', 'ENABLED', 85, '默认全球科技源'
WHERE NOT EXISTS (
    SELECT 1 FROM `tech_briefing_source` WHERE `source_name` = 'VentureBeat'
);

INSERT INTO `tech_briefing_source` (`source_name`, `source_type`, `fetch_type`, `base_url`, `rss_url`, `status`, `weight`, `remark`)
SELECT '爱范儿', 'DOMESTIC', 'RSS', 'https://www.ifanr.com', 'https://www.ifanr.com/feed', 'ENABLED', 80, '默认国内科技源'
WHERE NOT EXISTS (
    SELECT 1 FROM `tech_briefing_source` WHERE `source_name` = '爱范儿'
);

INSERT INTO `tech_briefing_source` (`source_name`, `source_type`, `fetch_type`, `base_url`, `rss_url`, `status`, `weight`, `remark`)
SELECT 'InfoQ 中文', 'DOMESTIC', 'RSS', 'https://www.infoq.cn', 'https://www.infoq.cn/feed', 'ENABLED', 75, '默认国内科技源'
WHERE NOT EXISTS (
    SELECT 1 FROM `tech_briefing_source` WHERE `source_name` = 'InfoQ 中文'
);
