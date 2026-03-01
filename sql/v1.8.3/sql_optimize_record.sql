-- 慢SQL优化分析记录表
-- 用于存储用户的SQL优化分析历史

CREATE TABLE IF NOT EXISTS `sql_optimize_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `original_sql` TEXT NOT NULL COMMENT '原始SQL',
  `explain_result` TEXT COMMENT 'EXPLAIN结果',
  `explain_format` VARCHAR(20) DEFAULT 'TABLE' COMMENT 'EXPLAIN格式（TABLE/JSON）',
  `table_structures` TEXT COMMENT '表结构JSON',
  `mysql_version` VARCHAR(20) DEFAULT '8.0' COMMENT 'MySQL版本',
  `analysis_result` MEDIUMTEXT COMMENT '分析结果JSON',
  `score` INT DEFAULT NULL COMMENT '性能评分（0-100）',
  `is_favorite` TINYINT DEFAULT 0 COMMENT '是否收藏（0-否 1-是）',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除（0-未删除 1-已删除）',
  PRIMARY KEY (`id`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_create_time` (`create_time`),
  INDEX `idx_score` (`score`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='慢SQL优化分析记录表';
