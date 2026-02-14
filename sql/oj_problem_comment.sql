-- OJ题目评论表
CREATE TABLE IF NOT EXISTS `oj_problem_comment` (
    `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '评论ID',
    `problem_id`          BIGINT       NOT NULL COMMENT '题目ID',
    `parent_id`           BIGINT       NOT NULL DEFAULT 0 COMMENT '父评论ID，0表示顶级评论',
    `content`             TEXT         NOT NULL COMMENT '评论内容',
    `author_id`           BIGINT       NOT NULL COMMENT '评论者用户ID',
    `author_name`         VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '评论者用户名',
    `like_count`          INT          NOT NULL DEFAULT 0 COMMENT '点赞数',
    `reply_to_id`         BIGINT       NULL COMMENT '回复的评论ID',
    `reply_to_user_id`    BIGINT       NULL COMMENT '回复的用户ID',
    `reply_to_user_name`  VARCHAR(64)  NULL COMMENT '回复的用户名',
    `reply_count`         INT          NOT NULL DEFAULT 0 COMMENT '回复数量（仅一级评论有效）',
    `status`              TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1-正常，2-删除',
    `create_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_problem_id` (`problem_id`),
    INDEX `idx_author_id` (`author_id`),
    INDEX `idx_parent_id` (`parent_id`),
    INDEX `idx_reply_to_id` (`reply_to_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OJ题目评论表';

-- OJ题目评论点赞表
CREATE TABLE IF NOT EXISTS `oj_problem_comment_like` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `comment_id`  BIGINT   NOT NULL COMMENT '评论ID',
    `user_id`     BIGINT   NOT NULL COMMENT '用户ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_comment_user` (`comment_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OJ题目评论点赞表';
