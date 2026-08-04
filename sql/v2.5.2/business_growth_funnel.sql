-- ===================================
-- Business Growth Funnel
-- 版本: v2.5.2
-- 说明: 增加成长主行动漏斗，并将投递进展关联到已有岗位准备事实。
-- ===================================

CREATE TABLE IF NOT EXISTS `growth_journey_event` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `event_type` VARCHAR(64) NOT NULL COMMENT 'PRIMARY_ACTION_SHOWN/PRIMARY_ACTION_STARTED',
    `tracking_id` VARCHAR(96) NOT NULL COMMENT '每日稳定的主行动追踪ID',
    `action_type` VARCHAR(64) NOT NULL COMMENT '主行动类型',
    `source` VARCHAR(64) NOT NULL COMMENT '主行动业务来源',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_growth_journey_user_event_tracking` (`user_id`, `event_type`, `tracking_id`),
    KEY `idx_growth_journey_type_time_user` (`event_type`, `create_time`, `user_id`),
    KEY `idx_growth_journey_source_time` (`source`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户成长主行动业务漏斗事件';

ALTER TABLE `career_application_record`
    ADD COLUMN `match_record_id` BIGINT DEFAULT NULL COMMENT '关联的岗位匹配记录ID' AFTER `session_id`,
    ADD COLUMN `plan_record_id` BIGINT DEFAULT NULL COMMENT '关联的补短板计划记录ID' AFTER `match_record_id`,
    ADD COLUMN `mock_interview_session_id` BIGINT DEFAULT NULL COMMENT '关联的模拟面试会话ID' AFTER `plan_record_id`;

CREATE INDEX `idx_career_application_match_record`
    ON `career_application_record` (`user_id`, `match_record_id`);

CREATE INDEX `idx_career_application_plan_record`
    ON `career_application_record` (`user_id`, `plan_record_id`);

CREATE INDEX `idx_career_application_mock_session`
    ON `career_application_record` (`user_id`, `mock_interview_session_id`);

CREATE INDEX `idx_career_application_analytics_time`
    ON `career_application_record` (`update_time`, `deleted`, `user_id`);

CREATE INDEX `idx_growth_coach_action_analytics_time`
    ON `growth_coach_action_run` (`action_id`, `created_at`, `status`);

CREATE INDEX `idx_growth_evidence_analytics_time`
    ON `growth_evidence` (`quality_level`, `observed_at`, `user_id`);
