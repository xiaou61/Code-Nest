-- Durable administrator-agent workflow control plane.
-- Apply after v2.5.9. The worker remains disabled until this migration is verified.

SET @code_nest_schema = DATABASE();
SET @workflow_context_column_count = (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = @code_nest_schema
    AND table_name = 'sys_agent_task'
    AND column_name = 'workflow_context_json'
);
SET @workflow_context_sql = IF(
  @workflow_context_column_count = 0,
  'ALTER TABLE sys_agent_task ADD COLUMN workflow_context_json TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT ''Bounded operator context for task planner'' AFTER session_id',
  'SELECT 1'
);
PREPARE workflow_context_statement FROM @workflow_context_sql;
EXECUTE workflow_context_statement;
DEALLOCATE PREPARE workflow_context_statement;

CREATE TABLE IF NOT EXISTS `sys_agent_task_event` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_id` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `event_type` varchar(80) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `step_order` int NULL DEFAULT NULL,
  `actor_type` varchar(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `actor_id` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `from_status` varchar(32) CHARACTER SET ascii COLLATE ascii_bin NULL DEFAULT NULL,
  `to_status` varchar(32) CHARACTER SET ascii COLLATE ascii_bin NULL DEFAULT NULL,
  `detail_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_agent_task_event_task_cursor` (`task_id` ASC, `id` ASC) USING BTREE,
  INDEX `idx_agent_task_event_type_created` (`event_type` ASC, `created_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci
  COMMENT = 'Durable administrator-agent workflow event timeline' ROW_FORMAT = Dynamic;
