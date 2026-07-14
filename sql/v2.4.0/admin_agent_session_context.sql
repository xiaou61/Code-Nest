CREATE TABLE IF NOT EXISTS `sys_agent_session_context` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '会话上下文ID',
  `session_id` varchar(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '按管理员隔离的智能体会话存储键',
  `turns_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '最近对话窗口JSON',
  `created_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_agent_session_id` (`session_id` ASC) USING BTREE,
  INDEX `idx_agent_session_updated_time` (`updated_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '管理员智能体会话上下文表' ROW_FORMAT = Dynamic;
