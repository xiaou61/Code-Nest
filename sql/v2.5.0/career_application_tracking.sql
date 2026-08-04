-- v2.5.0：求职闭环的用户自报投递记录。
-- 记录只服务于用户本人的求职过程跟踪，不表示平台已验证的投递、Offer 或能力结果。

CREATE TABLE IF NOT EXISTS `career_application_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `session_id` BIGINT NOT NULL COMMENT '求职闭环会话ID',
    `company_name` VARCHAR(120) NOT NULL COMMENT '公司名称',
    `position_name` VARCHAR(120) NOT NULL COMMENT '岗位名称',
    `status` VARCHAR(24) NOT NULL COMMENT 'PREPARING/APPLIED/INTERVIEWING/OFFER/REJECTED/WITHDRAWN',
    `applied_date` DATE DEFAULT NULL COMMENT '投递日期',
    `next_follow_up_date` DATE DEFAULT NULL COMMENT '下次跟进日期',
    `note` VARCHAR(500) DEFAULT NULL COMMENT '用户备注',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_career_application_user_status` (`user_id`, `deleted`, `status`, `update_time`),
    KEY `idx_career_application_follow_up` (`user_id`, `deleted`, `next_follow_up_date`),
    KEY `idx_career_application_evidence` (`user_id`, `update_time`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户求职投递记录';
