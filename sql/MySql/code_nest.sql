/*
 Navicat Premium Data Transfer

 Source Server         : 本机的
 Source Server Type    : MySQL
 Source Server Version : 80026
 Source Host           : localhost:3306
 Source Schema         : code_nest

 Target Server Type    : MySQL
 Target Server Version : 80026
 File Encoding         : 65001

 Date: 01/03/2026 15:25:39
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for blog_article
-- ----------------------------
DROP TABLE IF EXISTS `blog_article`;
CREATE TABLE `blog_article`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '文章ID',
  `user_id` bigint NOT NULL COMMENT '作者用户ID',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文章标题',
  `cover_image` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '文章封面图',
  `summary` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '文章摘要',
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文章内容（Markdown格式）',
  `category_id` bigint NULL DEFAULT NULL COMMENT '文章分类ID',
  `tags` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '文章标签，JSON数组格式',
  `status` tinyint NULL DEFAULT 0 COMMENT '文章状态：0-草稿 1-已发布 2-已下架 3-已删除',
  `is_original` tinyint NULL DEFAULT 1 COMMENT '是否原创：1-原创 0-转载',
  `is_top` tinyint NULL DEFAULT 0 COMMENT '是否置顶：1-置顶 0-普通',
  `top_expire_time` datetime NULL DEFAULT NULL COMMENT '置顶过期时间',
  `points_cost` int NULL DEFAULT 20 COMMENT '发布消耗积分',
  `publish_time` datetime NULL DEFAULT NULL COMMENT '发布时间',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_category_id`(`category_id` ASC) USING BTREE,
  INDEX `idx_status_publish_time`(`status` ASC, `publish_time` DESC) USING BTREE,
  INDEX `idx_create_time`(`create_time` DESC) USING BTREE,
  INDEX `idx_is_top`(`is_top` ASC, `publish_time` DESC) USING BTREE,
  FULLTEXT INDEX `ft_title_content`(`title`, `content`) WITH PARSER `ngram`
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '博客文章表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for blog_category
-- ----------------------------
DROP TABLE IF EXISTS `blog_category`;
CREATE TABLE `blog_category`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `category_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分类名称',
  `category_icon` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '分类图标URL',
  `category_description` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '分类描述',
  `sort_order` int NULL DEFAULT 0 COMMENT '排序权重',
  `article_count` int NULL DEFAULT 0 COMMENT '文章数量',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：1-启用 0-禁用',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_category_name`(`category_name` ASC) USING BTREE,
  INDEX `idx_sort_order`(`sort_order` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '文章分类表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for blog_config
-- ----------------------------
DROP TABLE IF EXISTS `blog_config`;
CREATE TABLE `blog_config`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `blog_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '博客名称',
  `blog_description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '博客简介',
  `blog_avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '博客头像URL',
  `blog_cover` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '博客背景图URL',
  `blog_notice` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '博客公告',
  `personal_tags` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '个人标签，JSON格式',
  `social_links` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '社交链接，JSON格式',
  `is_public` tinyint NULL DEFAULT 1 COMMENT '是否公开：1-公开 0-私密',
  `total_articles` int NULL DEFAULT 0 COMMENT '文章总数',
  `points_cost` int NULL DEFAULT 50 COMMENT '开通消耗积分',
  `status` tinyint NULL DEFAULT 1 COMMENT '博客状态：1-正常 0-封禁',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_total_articles`(`total_articles` DESC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '博客配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for blog_tag
-- ----------------------------
DROP TABLE IF EXISTS `blog_tag`;
CREATE TABLE `blog_tag`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '标签ID',
  `tag_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标签名称',
  `use_count` int NULL DEFAULT 0 COMMENT '使用次数',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_tag_name`(`tag_name` ASC) USING BTREE,
  INDEX `idx_use_count`(`use_count` DESC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '文章标签表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for bug_item
-- ----------------------------
DROP TABLE IF EXISTS `bug_item`;
CREATE TABLE `bug_item`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Bug ID',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'Bug标题',
  `phenomenon` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'Bug现象描述',
  `cause_analysis` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '原因分析',
  `solution` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '解决方案',
  `tech_tags` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '技术标签（多个标签用逗号分隔）',
  `difficulty_level` tinyint NOT NULL DEFAULT 1 COMMENT '难度等级：1-初级，2-中级，3-高级，4-专家级',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `sort_order` int NULL DEFAULT 0 COMMENT '排序值（数字越大越靠前）',
  `created_by` bigint NOT NULL COMMENT '创建者ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_difficulty_level`(`difficulty_level` ASC) USING BTREE,
  INDEX `idx_created_by`(`created_by` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_sort_order`(`sort_order` ASC) USING BTREE,
  INDEX `idx_tech_tags`(`tech_tags`(255) ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Bug条目表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for career_loop_action
-- ----------------------------
DROP TABLE IF EXISTS `career_loop_action`;
CREATE TABLE `career_loop_action`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `session_id` bigint NOT NULL COMMENT '会话ID',
  `stage` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '所属阶段',
  `action_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '动作类型',
  `title` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '动作标题',
  `description` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '动作说明',
  `priority` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'P1' COMMENT '优先级',
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'todo' COMMENT '状态:todo/doing/done',
  `due_date` date NULL DEFAULT NULL COMMENT '截止日期',
  `source` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'rule' COMMENT '来源',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_session_status`(`session_id` ASC, `status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '求职闭环动作清单' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for career_loop_session
-- ----------------------------
DROP TABLE IF EXISTS `career_loop_session`;
CREATE TABLE `career_loop_session`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `target_role` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '目标岗位',
  `target_company_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '目标公司类型',
  `weekly_hours` int NULL DEFAULT 8 COMMENT '每周投入时长(小时)',
  `current_stage` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '当前阶段',
  `health_score` int NOT NULL DEFAULT 60 COMMENT '健康分(0-100)',
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'active' COMMENT '状态:active/completed/archived',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_status`(`user_id` ASC, `status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '求职闭环会话' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for career_loop_snapshot
-- ----------------------------
DROP TABLE IF EXISTS `career_loop_snapshot`;
CREATE TABLE `career_loop_snapshot`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `session_id` bigint NOT NULL COMMENT '会话ID',
  `plan_progress` int NULL DEFAULT 0 COMMENT '计划进度(0-100)',
  `mock_count` int NULL DEFAULT 0 COMMENT '模拟面试次数',
  `latest_mock_score` int NULL DEFAULT NULL COMMENT '最近模拟面试分数',
  `review_count` int NULL DEFAULT 0 COMMENT '复盘次数',
  `resume_updated_at` datetime NULL DEFAULT NULL COMMENT '简历最近更新时间',
  `risk_flags_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '风险标记JSON',
  `next_suggestion_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '下一步建议JSON',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_session_id`(`session_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '求职闭环快照' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for career_loop_stage_log
-- ----------------------------
DROP TABLE IF EXISTS `career_loop_stage_log`;
CREATE TABLE `career_loop_stage_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `session_id` bigint NOT NULL COMMENT '会话ID',
  `from_stage` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '原阶段',
  `to_stage` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '目标阶段',
  `trigger_source` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '触发源',
  `trigger_ref_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '触发引用ID',
  `note` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_session_create_time`(`session_id` ASC, `create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '求职闭环阶段日志' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for chat_messages
-- ----------------------------
DROP TABLE IF EXISTS `chat_messages`;
CREATE TABLE `chat_messages`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `room_id` bigint NOT NULL COMMENT '聊天室ID',
  `user_id` bigint NOT NULL COMMENT '发送者用户ID',
  `message_type` tinyint NOT NULL COMMENT '消息类型：1文本 2图片 3系统消息',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消息内容',
  `image_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '图片URL（消息类型为图片时）',
  `reply_to_id` bigint NULL DEFAULT NULL COMMENT '回复的消息ID',
  `reply_to_content` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '被回复消息内容摘要',
  `reply_to_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '被回复者昵称',
  `mentions` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '@提及的用户ID,逗号分隔',
  `is_deleted` tinyint NULL DEFAULT 0 COMMENT '是否删除：0否 1是',
  `ip_address` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '发送者IP地址',
  `device_info` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '设备信息',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_room_id`(`room_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` DESC) USING BTREE,
  INDEX `idx_room_time`(`room_id` ASC, `create_time` DESC) USING BTREE,
  INDEX `idx_reply_to_id`(`reply_to_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '聊天消息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for chat_online_users
-- ----------------------------
DROP TABLE IF EXISTS `chat_online_users`;
CREATE TABLE `chat_online_users`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `room_id` bigint NOT NULL COMMENT '聊天室ID',
  `session_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'WebSocket会话ID',
  `ip_address` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '用户IP地址',
  `device_info` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '设备信息',
  `connect_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '连接时间',
  `last_heartbeat_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后心跳时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_session_id`(`session_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_room_id`(`room_id` ASC) USING BTREE,
  INDEX `idx_heartbeat`(`last_heartbeat_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 116 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '在线用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for chat_rooms
-- ----------------------------
DROP TABLE IF EXISTS `chat_rooms`;
CREATE TABLE `chat_rooms`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '聊天室ID',
  `room_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '聊天室名称',
  `room_type` tinyint NULL DEFAULT 1 COMMENT '类型：1官方群组',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '聊天室描述',
  `max_users` int NULL DEFAULT 0 COMMENT '最大人数限制，0表示不限制',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：1正常 0禁用',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '聊天室表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for chat_sensitive_word
-- ----------------------------
DROP TABLE IF EXISTS `chat_sensitive_word`;
CREATE TABLE `chat_sensitive_word`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '敏感词ID',
  `word` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '敏感词',
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'default' COMMENT '分类',
  `match_type` tinyint NULL DEFAULT 1 COMMENT '匹配类型:1精确,2模糊',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态:0禁用,1启用',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_word`(`word` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_category`(`category` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '敏感词表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for chat_user_bans
-- ----------------------------
DROP TABLE IF EXISTS `chat_user_bans`;
CREATE TABLE `chat_user_bans`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '禁言记录ID',
  `user_id` bigint NOT NULL COMMENT '被禁言用户ID',
  `room_id` bigint NOT NULL COMMENT '聊天室ID',
  `ban_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '禁言原因',
  `ban_start_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '禁言开始时间',
  `ban_end_time` datetime NULL DEFAULT NULL COMMENT '禁言结束时间，NULL表示永久',
  `operator_id` bigint NULL DEFAULT NULL COMMENT '操作人ID',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：1生效中 0已解除',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_room_id`(`room_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_end_time`(`ban_end_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户禁言表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for code_pen
-- ----------------------------
DROP TABLE IF EXISTS `code_pen`;
CREATE TABLE `code_pen`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '作品ID',
  `user_id` bigint NOT NULL COMMENT '作者用户ID',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '作品标题',
  `description` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '作品描述',
  `html_code` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT 'HTML代码',
  `css_code` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT 'CSS代码',
  `js_code` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT 'JavaScript代码',
  `preview_image` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '预览图URL',
  `external_css` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '外部CSS库链接，JSON数组格式',
  `external_js` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '外部JS库链接，JSON数组格式',
  `preprocessor_config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '预处理器配置，JSON格式',
  `tags` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '标签，JSON数组格式',
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '分类（动画、组件、游戏、工具等）',
  `is_public` tinyint NULL DEFAULT 1 COMMENT '可见性：1-公开 0-私密',
  `is_free` tinyint NULL DEFAULT 1 COMMENT '是否免费：1-免费 0-付费',
  `fork_price` int NULL DEFAULT 0 COMMENT 'Fork价格（积分），0表示免费，1-1000表示付费',
  `is_template` tinyint NULL DEFAULT 0 COMMENT '是否系统模板：1-是 0-否',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：0-草稿 1-已发布 2-已下架 3-已删除',
  `is_recommend` tinyint NULL DEFAULT 0 COMMENT '是否推荐：1-推荐 0-普通',
  `recommend_expire_time` datetime NULL DEFAULT NULL COMMENT '推荐过期时间',
  `forked_from` bigint NULL DEFAULT NULL COMMENT 'Fork来源作品ID',
  `fork_count` int NULL DEFAULT 0 COMMENT 'Fork次数',
  `like_count` int NULL DEFAULT 0 COMMENT '点赞数',
  `comment_count` int NULL DEFAULT 0 COMMENT '评论数',
  `collect_count` int NULL DEFAULT 0 COMMENT '收藏数',
  `view_count` int NULL DEFAULT 0 COMMENT '浏览数',
  `points_reward` int NULL DEFAULT 10 COMMENT '创建奖励积分',
  `total_income` int NULL DEFAULT 0 COMMENT '累计收益积分（通过付费Fork获得）',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `publish_time` datetime NULL DEFAULT NULL COMMENT '发布时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_status_publish_time`(`status` ASC, `publish_time` DESC) USING BTREE,
  INDEX `idx_is_recommend`(`is_recommend` ASC, `publish_time` DESC) USING BTREE,
  INDEX `idx_category`(`category` ASC) USING BTREE,
  INDEX `idx_like_count`(`like_count` DESC) USING BTREE,
  INDEX `idx_view_count`(`view_count` DESC) USING BTREE,
  INDEX `idx_is_free`(`is_free` ASC) USING BTREE,
  INDEX `idx_fork_price`(`fork_price` ASC) USING BTREE,
  FULLTEXT INDEX `ft_title_description`(`title`, `description`) WITH PARSER `ngram`
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '代码作品表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for code_pen_collect
-- ----------------------------
DROP TABLE IF EXISTS `code_pen_collect`;
CREATE TABLE `code_pen_collect`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
  `pen_id` bigint NOT NULL COMMENT '作品ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `folder_id` bigint NULL DEFAULT NULL COMMENT '收藏夹ID',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_pen_user`(`pen_id` ASC, `user_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_pen_id`(`pen_id` ASC) USING BTREE,
  INDEX `idx_folder_id`(`folder_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '作品收藏表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for code_pen_comment
-- ----------------------------
DROP TABLE IF EXISTS `code_pen_comment`;
CREATE TABLE `code_pen_comment`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  `pen_id` bigint NOT NULL COMMENT '作品ID',
  `user_id` bigint NOT NULL COMMENT '评论用户ID',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '评论内容',
  `parent_id` bigint NULL DEFAULT NULL COMMENT '父评论ID（回复）',
  `reply_to_user_id` bigint NULL DEFAULT NULL COMMENT '回复目标用户ID',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：1-正常 2-已隐藏 3-已删除',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_pen_id`(`pen_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_parent_id`(`parent_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` DESC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '作品评论表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for code_pen_folder
-- ----------------------------
DROP TABLE IF EXISTS `code_pen_folder`;
CREATE TABLE `code_pen_folder`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '收藏夹ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `folder_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '收藏夹名称',
  `folder_description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '收藏夹描述',
  `collect_count` int NULL DEFAULT 0 COMMENT '收藏数量',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '收藏夹表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for code_pen_fork_transaction
-- ----------------------------
DROP TABLE IF EXISTS `code_pen_fork_transaction`;
CREATE TABLE `code_pen_fork_transaction`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '交易ID',
  `original_pen_id` bigint NOT NULL COMMENT '原作品ID',
  `forked_pen_id` bigint NOT NULL COMMENT 'Fork后的作品ID',
  `original_author_id` bigint NOT NULL COMMENT '原作者ID',
  `fork_user_id` bigint NOT NULL COMMENT 'Fork用户ID',
  `fork_price` int NULL DEFAULT 0 COMMENT 'Fork价格（积分），0表示免费',
  `transaction_type` tinyint NULL DEFAULT 0 COMMENT '交易类型：0-免费Fork 1-付费Fork',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '交易时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_original_pen_id`(`original_pen_id` ASC) USING BTREE,
  INDEX `idx_forked_pen_id`(`forked_pen_id` ASC) USING BTREE,
  INDEX `idx_original_author_id`(`original_author_id` ASC) USING BTREE,
  INDEX `idx_fork_user_id`(`fork_user_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` DESC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Fork交易记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for code_pen_like
-- ----------------------------
DROP TABLE IF EXISTS `code_pen_like`;
CREATE TABLE `code_pen_like`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '点赞ID',
  `pen_id` bigint NOT NULL COMMENT '作品ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_pen_user`(`pen_id` ASC, `user_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_pen_id`(`pen_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '作品点赞表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for code_pen_tag
-- ----------------------------
DROP TABLE IF EXISTS `code_pen_tag`;
CREATE TABLE `code_pen_tag`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '标签ID',
  `tag_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标签名称',
  `tag_description` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '标签描述',
  `use_count` int NULL DEFAULT 0 COMMENT '使用次数',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_tag_name`(`tag_name` ASC) USING BTREE,
  INDEX `idx_use_count`(`use_count` DESC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '作品标签表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for community_category
-- ----------------------------
DROP TABLE IF EXISTS `community_category`;
CREATE TABLE `community_category`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分类名称',
  `description` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '分类描述',
  `sort_order` int NULL DEFAULT 0 COMMENT '排序顺序，数字越小越靠前',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
  `post_count` int NULL DEFAULT 0 COMMENT '该分类下的帖子数量',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator_id` bigint NULL DEFAULT NULL COMMENT '创建者ID（管理员）',
  `creator_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '创建者名称',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_name`(`name` ASC) USING BTREE,
  INDEX `idx_status_sort`(`status` ASC, `sort_order` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '社区帖子分类表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for community_comment
-- ----------------------------
DROP TABLE IF EXISTS `community_comment`;
CREATE TABLE `community_comment`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  `post_id` bigint NOT NULL COMMENT '帖子ID',
  `parent_id` bigint NULL DEFAULT 0 COMMENT '父评论ID，0表示顶级评论',
  `reply_to_id` bigint NULL DEFAULT NULL COMMENT '回复的评论ID，NULL表示一级评论',
  `reply_to_user_id` bigint NULL DEFAULT NULL COMMENT '回复的用户ID',
  `reply_to_user_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '回复的用户名',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '评论内容',
  `author_id` bigint NOT NULL COMMENT '评论者用户ID',
  `author_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '评论者用户名',
  `like_count` int NULL DEFAULT 0 COMMENT '点赞数',
  `reply_count` int NULL DEFAULT 0 COMMENT '回复数量（仅一级评论有效）',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：1-正常，2-删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_post_id`(`post_id` ASC) USING BTREE,
  INDEX `idx_parent_id`(`parent_id` ASC) USING BTREE,
  INDEX `idx_author_id`(`author_id` ASC) USING BTREE,
  INDEX `idx_status_create`(`status` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_post_parent_time`(`post_id` ASC, `parent_id` ASC, `create_time` DESC) USING BTREE,
  INDEX `idx_reply_to_id`(`reply_to_id` ASC) USING BTREE,
  INDEX `idx_author_time`(`author_id` ASC, `create_time` DESC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '社区评论表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for community_comment_like
-- ----------------------------
DROP TABLE IF EXISTS `community_comment_like`;
CREATE TABLE `community_comment_like`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '点赞ID',
  `comment_id` bigint NOT NULL COMMENT '评论ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `user_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_comment_user`(`comment_id` ASC, `user_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '评论点赞表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for community_post
-- ----------------------------
DROP TABLE IF EXISTS `community_post`;
CREATE TABLE `community_post`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '帖子ID',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '帖子标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '帖子内容',
  `ai_summary` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT 'AI生成的摘要',
  `ai_keywords` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'AI提取的关键词，逗号分隔',
  `ai_generate_time` datetime NULL DEFAULT NULL COMMENT 'AI生成时间',
  `category_id` bigint NULL DEFAULT NULL COMMENT '分类ID',
  `author_id` bigint NOT NULL COMMENT '作者用户ID',
  `author_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '作者用户名',
  `view_count` int NULL DEFAULT 0 COMMENT '浏览次数',
  `like_count` int NULL DEFAULT 0 COMMENT '点赞数',
  `comment_count` int NULL DEFAULT 0 COMMENT '评论数',
  `collect_count` int NULL DEFAULT 0 COMMENT '收藏数',
  `is_top` tinyint NULL DEFAULT 0 COMMENT '是否置顶：0-否，1-是',
  `top_expire_time` datetime NULL DEFAULT NULL COMMENT '置顶过期时间',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：1-正常，2-下架，3-删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_author_id`(`author_id` ASC) USING BTREE,
  INDEX `idx_status_top_create`(`status` ASC, `is_top` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_category_id`(`category_id` ASC) USING BTREE,
  INDEX `idx_status_top_time`(`status` ASC, `is_top` DESC, `create_time` DESC) USING BTREE,
  INDEX `idx_category_status_time`(`category_id` ASC, `status` ASC, `create_time` DESC) USING BTREE,
  INDEX `idx_author_status_time`(`author_id` ASC, `status` ASC, `create_time` DESC) USING BTREE,
  INDEX `idx_hot_score`(`status` ASC, `create_time` ASC, `like_count` ASC, `comment_count` ASC) USING BTREE,
  FULLTEXT INDEX `ft_title_content`(`title`, `content`) WITH PARSER `ngram`,
  CONSTRAINT `fk_post_category` FOREIGN KEY (`category_id`) REFERENCES `community_category` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 173 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '社区帖子表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for community_post_collect
-- ----------------------------
DROP TABLE IF EXISTS `community_post_collect`;
CREATE TABLE `community_post_collect`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
  `post_id` bigint NOT NULL COMMENT '帖子ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `user_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_post_user`(`post_id` ASC, `user_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_user_time`(`user_id` ASC, `create_time` DESC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '帖子收藏表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for community_post_like
-- ----------------------------
DROP TABLE IF EXISTS `community_post_like`;
CREATE TABLE `community_post_like`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '点赞ID',
  `post_id` bigint NOT NULL COMMENT '帖子ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `user_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_post_user`(`post_id` ASC, `user_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_user_time`(`user_id` ASC, `create_time` DESC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 167 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '帖子点赞表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for community_post_tag
-- ----------------------------
DROP TABLE IF EXISTS `community_post_tag`;
CREATE TABLE `community_post_tag`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '关联ID',
  `post_id` bigint NOT NULL COMMENT '帖子ID',
  `tag_id` bigint NOT NULL COMMENT '标签ID',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_post_tag`(`post_id` ASC, `tag_id` ASC) USING BTREE,
  INDEX `idx_post_id`(`post_id` ASC) USING BTREE,
  INDEX `idx_tag_id`(`tag_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '帖子标签关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for community_tag
-- ----------------------------
DROP TABLE IF EXISTS `community_tag`;
CREATE TABLE `community_tag`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '标签ID',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标签名称',
  `description` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '标签描述',
  `color` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '#409EFF' COMMENT '标签颜色',
  `icon` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '标签图标',
  `post_count` int NULL DEFAULT 0 COMMENT '帖子数量',
  `follow_count` int NULL DEFAULT 0 COMMENT '关注数量',
  `sort_order` int NULL DEFAULT 0 COMMENT '排序顺序',
  `status` int NULL DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_name`(`name` ASC) USING BTREE,
  INDEX `idx_post_count`(`post_count` DESC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_sort_order`(`sort_order` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 20 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '社区标签表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for community_user_status
-- ----------------------------
DROP TABLE IF EXISTS `community_user_status`;
CREATE TABLE `community_user_status`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '状态ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `user_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名',
  `is_banned` tinyint NULL DEFAULT 0 COMMENT '是否封禁：0-否，1-是',
  `ban_reason` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '封禁原因',
  `ban_expire_time` datetime NULL DEFAULT NULL COMMENT '封禁过期时间',
  `post_count` int NULL DEFAULT 0 COMMENT '发帖数',
  `comment_count` int NULL DEFAULT 0 COMMENT '评论数',
  `like_count` int NULL DEFAULT 0 COMMENT '点赞数',
  `collect_count` int NULL DEFAULT 0 COMMENT '收藏数',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户社区状态表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for daily_content
-- ----------------------------
DROP TABLE IF EXISTS `daily_content`;
CREATE TABLE `daily_content`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '内容ID',
  `content_type` tinyint NOT NULL COMMENT '内容类型：1-编程格言，2-技术小贴士，3-代码片段，4-历史上的今天',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '内容标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '内容正文',
  `author` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '作者',
  `programming_language` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '相关编程语言',
  `tags` json NULL COMMENT '标签数组JSON格式',
  `difficulty_level` tinyint NULL DEFAULT NULL COMMENT '难度等级：1-初级，2-中级，3-高级',
  `source_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '来源链接',
  `view_count` int NOT NULL DEFAULT 0 COMMENT '查看次数',
  `like_count` int NOT NULL DEFAULT 0 COMMENT '点赞数',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_content_type`(`content_type` ASC) USING BTREE,
  INDEX `idx_programming_language`(`programming_language` ASC) USING BTREE,
  INDEX `idx_difficulty_level`(`difficulty_level` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '每日内容表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for developer_calendar_event
-- ----------------------------
DROP TABLE IF EXISTS `developer_calendar_event`;
CREATE TABLE `developer_calendar_event`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '事件ID',
  `event_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '事件名称',
  `event_date` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '事件日期(MM-dd格式，用于每年循环)',
  `event_type` tinyint NOT NULL DEFAULT 1 COMMENT '事件类型：1-程序员节日，2-技术纪念日，3-开源节日',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '事件描述',
  `icon` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '图标标识',
  `color` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '#1890ff' COMMENT '标记颜色',
  `is_major` tinyint NOT NULL DEFAULT 0 COMMENT '是否重要节日：0-普通，1-重要',
  `blessing_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '节日祝福语',
  `related_links` json NULL COMMENT '相关链接JSON格式',
  `sort_order` int NULL DEFAULT 0 COMMENT '排序值',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_event_date`(`event_date` ASC) USING BTREE,
  INDEX `idx_event_type`(`event_type` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_is_major`(`is_major` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '程序员日历事件表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for file_access
-- ----------------------------
DROP TABLE IF EXISTS `file_access`;
CREATE TABLE `file_access`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '访问记录ID',
  `file_id` bigint NOT NULL COMMENT '文件ID',
  `access_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '访问时间',
  `access_ip` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '访问IP',
  `module_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '访问模块',
  `user_id` bigint NULL DEFAULT NULL COMMENT '访问用户ID',
  `access_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'VIEW' COMMENT '访问类型: VIEW,DOWNLOAD',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_file_id`(`file_id` ASC) USING BTREE,
  INDEX `idx_access_time`(`access_time` ASC) USING BTREE,
  INDEX `idx_module`(`module_name` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '文件访问记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for file_info
-- ----------------------------
DROP TABLE IF EXISTS `file_info`;
CREATE TABLE `file_info`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '文件ID',
  `original_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '原始文件名',
  `stored_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '存储文件名',
  `file_size` bigint NOT NULL DEFAULT 0 COMMENT '文件大小(字节)',
  `content_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '文件MIME类型',
  `md5_hash` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文件MD5校验值',
  `module_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '所属模块名称',
  `business_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '业务类型',
  `upload_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  `access_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '访问URL',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '文件状态: 0=已删除, 1=正常',
  `is_public` tinyint NOT NULL DEFAULT 0 COMMENT '是否公开访问: 0=私有, 1=公开',
  `delete_time` datetime NULL DEFAULT NULL COMMENT '删除时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_md5`(`md5_hash` ASC) USING BTREE,
  INDEX `idx_module_business`(`module_name` ASC, `business_type` ASC) USING BTREE,
  INDEX `idx_upload_time`(`upload_time` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '文件信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for file_migration
-- ----------------------------
DROP TABLE IF EXISTS `file_migration`;
CREATE TABLE `file_migration`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '迁移任务ID',
  `task_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '任务名称',
  `source_storage_id` bigint NOT NULL COMMENT '源存储配置ID',
  `target_storage_id` bigint NOT NULL COMMENT '目标存储配置ID',
  `migration_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '迁移类型: FULL,INCREMENTAL,TIME_RANGE,FILE_TYPE',
  `filter_params` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '筛选参数JSON',
  `total_files` int NOT NULL DEFAULT 0 COMMENT '总文件数',
  `success_count` int NOT NULL DEFAULT 0 COMMENT '成功数量',
  `fail_count` int NOT NULL DEFAULT 0 COMMENT '失败数量',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING' COMMENT '任务状态: PENDING,RUNNING,COMPLETED,FAILED,STOPPED',
  `start_time` datetime NULL DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime NULL DEFAULT NULL COMMENT '结束时间',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '错误信息',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_source_target`(`source_storage_id` ASC, `target_storage_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '文件迁移任务表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for file_storage
-- ----------------------------
DROP TABLE IF EXISTS `file_storage`;
CREATE TABLE `file_storage`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '存储记录ID',
  `file_id` bigint NOT NULL COMMENT '文件ID',
  `storage_config_id` bigint NOT NULL COMMENT '存储配置ID',
  `storage_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '存储路径',
  `is_primary` tinyint NOT NULL DEFAULT 1 COMMENT '是否主存储: 0=备份, 1=主存储',
  `sync_status` tinyint NOT NULL DEFAULT 1 COMMENT '同步状态: 0=同步中, 1=已同步, 2=同步失败',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_file_storage`(`file_id` ASC, `storage_config_id` ASC) USING BTREE,
  INDEX `idx_file_id`(`file_id` ASC) USING BTREE,
  INDEX `idx_storage_config`(`storage_config_id` ASC) USING BTREE,
  INDEX `idx_primary`(`is_primary` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '文件存储记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for file_system_setting
-- ----------------------------
DROP TABLE IF EXISTS `file_system_setting`;
CREATE TABLE `file_system_setting`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '设置ID',
  `setting_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '设置键',
  `setting_value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '设置值',
  `setting_desc` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '设置描述',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_setting_key`(`setting_key` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 25 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '文件系统设置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for flashcard
-- ----------------------------
DROP TABLE IF EXISTS `flashcard`;
CREATE TABLE `flashcard`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '闪卡ID',
  `deck_id` bigint NOT NULL COMMENT '所属卡组ID',
  `front_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '正面内容(问题)',
  `back_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '反面内容(答案)',
  `content_type` tinyint NULL DEFAULT 1 COMMENT '内容类型: 1-文本 2-Markdown 3-代码',
  `tags` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '标签',
  `source_question_id` bigint NULL DEFAULT NULL COMMENT '关联题库题目ID',
  `sort_order` int NULL DEFAULT 0 COMMENT '排序序号',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NULL DEFAULT 0 COMMENT '删除标志: 0-正常 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_deck_id`(`deck_id` ASC) USING BTREE,
  INDEX `idx_source_question_id`(`source_question_id` ASC) USING BTREE,
  INDEX `idx_del_flag`(`del_flag` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '闪卡表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for flashcard_daily_stats
-- ----------------------------
DROP TABLE IF EXISTS `flashcard_daily_stats`;
CREATE TABLE `flashcard_daily_stats`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '统计ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `stat_date` date NOT NULL COMMENT '统计日期',
  `new_cards` int NULL DEFAULT 0 COMMENT '新学卡片数',
  `review_cards` int NULL DEFAULT 0 COMMENT '复习卡片数',
  `correct_cards` int NULL DEFAULT 0 COMMENT '正确卡片数',
  `study_duration` int NULL DEFAULT 0 COMMENT '学习时长(秒)',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_date`(`user_id` ASC, `stat_date` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_stat_date`(`stat_date` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '闪卡每日学习统计' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for flashcard_deck
-- ----------------------------
DROP TABLE IF EXISTS `flashcard_deck`;
CREATE TABLE `flashcard_deck`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '卡组ID',
  `user_id` bigint NOT NULL COMMENT '创建者ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '卡组名称',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '卡组描述',
  `cover_image` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '封面图片',
  `tags` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '标签，逗号分隔',
  `is_public` tinyint(1) NULL DEFAULT 0 COMMENT '是否公开: 0-私有 1-公开',
  `card_count` int NULL DEFAULT 0 COMMENT '卡片数量',
  `study_count` int NULL DEFAULT 0 COMMENT '学习人数',
  `fork_count` int NULL DEFAULT 0 COMMENT '复制次数',
  `source_deck_id` bigint NULL DEFAULT NULL COMMENT '复制来源ID',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NULL DEFAULT 0 COMMENT '删除标志: 0-正常 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_is_public`(`is_public` ASC) USING BTREE,
  INDEX `idx_del_flag`(`del_flag` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '闪卡卡组表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for flashcard_deck_like
-- ----------------------------
DROP TABLE IF EXISTS `flashcard_deck_like`;
CREATE TABLE `flashcard_deck_like`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `deck_id` bigint NOT NULL COMMENT '卡片组ID',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_deck`(`user_id` ASC, `deck_id` ASC) USING BTREE,
  INDEX `idx_deck_id`(`deck_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '闪卡卡片组点赞表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for flashcard_review_log
-- ----------------------------
DROP TABLE IF EXISTS `flashcard_review_log`;
CREATE TABLE `flashcard_review_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `card_id` bigint NOT NULL COMMENT '闪卡ID',
  `quality` int NOT NULL COMMENT '评分(0-3)',
  `ease_factor_before` decimal(4, 2) NULL DEFAULT NULL COMMENT '复习前EF',
  `ease_factor_after` decimal(4, 2) NULL DEFAULT NULL COMMENT '复习后EF',
  `interval_before` int NULL DEFAULT NULL COMMENT '复习前间隔',
  `interval_after` int NULL DEFAULT NULL COMMENT '复习后间隔',
  `review_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '复习时间',
  `time_spent_ms` int NULL DEFAULT 0 COMMENT '思考时长(毫秒)',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_card_id`(`card_id` ASC) USING BTREE,
  INDEX `idx_review_time`(`review_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 16 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '闪卡复习详情日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for flashcard_study_history
-- ----------------------------
DROP TABLE IF EXISTS `flashcard_study_history`;
CREATE TABLE `flashcard_study_history`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '历史ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `card_id` bigint NOT NULL COMMENT '卡片ID',
  `deck_id` bigint NOT NULL COMMENT '卡片组ID',
  `quality` int NOT NULL COMMENT '评分（0-5）',
  `time_spent_seconds` int NULL DEFAULT NULL COMMENT '本次用时（秒）',
  `easiness_factor` decimal(4, 2) NULL DEFAULT NULL COMMENT '本次计算后的EF值',
  `interval_days` int NULL DEFAULT NULL COMMENT '本次计算后的间隔',
  `review_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '复习时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_card_id`(`card_id` ASC) USING BTREE,
  INDEX `idx_review_time`(`review_time` ASC) USING BTREE,
  INDEX `idx_user_date`(`user_id` ASC, `review_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '闪卡学习历史表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for flashcard_study_record
-- ----------------------------
DROP TABLE IF EXISTS `flashcard_study_record`;
CREATE TABLE `flashcard_study_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `card_id` bigint NOT NULL COMMENT '闪卡ID',
  `deck_id` bigint NOT NULL COMMENT '卡组ID',
  `repetitions` int NULL DEFAULT 0 COMMENT '连续正确次数',
  `ease_factor` decimal(4, 2) NULL DEFAULT 2.50 COMMENT '难度因子(EF)，范围1.3-2.5+',
  `interval_days` int NULL DEFAULT 0 COMMENT '当前间隔天数',
  `mastery_level` tinyint NULL DEFAULT 1 COMMENT '掌握度: 1-新卡 2-学习中 3-已掌握',
  `last_review_time` datetime NULL DEFAULT NULL COMMENT '上次复习时间',
  `next_review_time` datetime NULL DEFAULT NULL COMMENT '下次复习时间',
  `total_reviews` int NULL DEFAULT 0 COMMENT '总复习次数',
  `correct_count` int NULL DEFAULT 0 COMMENT '正确次数',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_card`(`user_id` ASC, `card_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_deck_id`(`deck_id` ASC) USING BTREE,
  INDEX `idx_next_review`(`user_id` ASC, `next_review_time` ASC) USING BTREE,
  INDEX `idx_mastery`(`user_id` ASC, `mastery_level` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '闪卡学习记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for flashcard_user_deck
-- ----------------------------
DROP TABLE IF EXISTS `flashcard_user_deck`;
CREATE TABLE `flashcard_user_deck`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '关联ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `deck_id` bigint NOT NULL COMMENT '卡组ID',
  `is_owner` tinyint(1) NULL DEFAULT 0 COMMENT '是否为创建者: 0-否 1-是',
  `last_study_time` datetime NULL DEFAULT NULL COMMENT '最后学习时间',
  `total_cards` int NULL DEFAULT 0 COMMENT '总卡片数',
  `learned_cards` int NULL DEFAULT 0 COMMENT '已学习卡片数',
  `mastered_cards` int NULL DEFAULT 0 COMMENT '已掌握卡片数',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_deck`(`user_id` ASC, `deck_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_deck_id`(`deck_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户卡组关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for interview_category
-- ----------------------------
DROP TABLE IF EXISTS `interview_category`;
CREATE TABLE `interview_category`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分类名称',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '分类描述',
  `sort_order` int NULL DEFAULT 0 COMMENT '排序序号',
  `question_set_count` int NULL DEFAULT 0 COMMENT '题单数量',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态 (0-禁用 1-启用)',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_name`(`name` ASC) USING BTREE,
  INDEX `idx_status_sort`(`status` ASC, `sort_order` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '面试题分类表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for interview_daily_stats
-- ----------------------------
DROP TABLE IF EXISTS `interview_daily_stats`;
CREATE TABLE `interview_daily_stats`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `stat_date` date NOT NULL COMMENT '统计日期',
  `learn_count` int NOT NULL DEFAULT 0 COMMENT '新学习题目数',
  `review_count` int NOT NULL DEFAULT 0 COMMENT '复习题目数',
  `total_count` int NOT NULL DEFAULT 0 COMMENT '总学习题目数（新学习+复习）',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_date`(`user_id` ASC, `stat_date` ASC) USING BTREE,
  INDEX `idx_user_date_range`(`user_id` ASC, `stat_date` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 19 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '面试题每日学习统计表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for interview_favorite
-- ----------------------------
DROP TABLE IF EXISTS `interview_favorite`;
CREATE TABLE `interview_favorite`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `target_type` tinyint NOT NULL COMMENT '收藏类型 (1-题单 2-题目)',
  `target_id` bigint NOT NULL COMMENT '目标ID（题单ID或题目ID）',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_target`(`user_id` ASC, `target_type` ASC, `target_id` ASC) USING BTREE,
  INDEX `idx_target`(`target_type` ASC, `target_id` ASC) USING BTREE,
  INDEX `idx_user_type`(`user_id` ASC, `target_type` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户收藏表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for interview_learn_record
-- ----------------------------
DROP TABLE IF EXISTS `interview_learn_record`;
CREATE TABLE `interview_learn_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `question_set_id` bigint NOT NULL COMMENT '题单ID',
  `question_id` bigint NOT NULL COMMENT '题目ID',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '学习时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_question`(`user_id` ASC, `question_id` ASC) USING BTREE,
  INDEX `idx_user_set`(`user_id` ASC, `question_set_id` ASC) USING BTREE,
  INDEX `idx_question_id`(`question_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 13 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '面试题学习记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for interview_mastery_history
-- ----------------------------
DROP TABLE IF EXISTS `interview_mastery_history`;
CREATE TABLE `interview_mastery_history`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `question_id` bigint NOT NULL COMMENT '题目ID',
  `mastery_level` tinyint NOT NULL COMMENT '掌握度等级',
  `is_review` tinyint NOT NULL DEFAULT 0 COMMENT '是否复习 0-首次学习 1-复习',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_time`(`user_id` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_question_time`(`question_id` ASC, `create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 19 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '面试题掌握度变更历史表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for interview_mastery_record
-- ----------------------------
DROP TABLE IF EXISTS `interview_mastery_record`;
CREATE TABLE `interview_mastery_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `question_id` bigint NOT NULL COMMENT '题目ID',
  `question_set_id` bigint NOT NULL COMMENT '题单ID（冗余，方便查询）',
  `mastery_level` tinyint NOT NULL DEFAULT 1 COMMENT '掌握度等级 1-不会 2-模糊 3-熟悉 4-已掌握',
  `review_count` int NOT NULL DEFAULT 0 COMMENT '复习次数',
  `last_review_time` datetime NULL DEFAULT NULL COMMENT '上次复习时间',
  `next_review_time` datetime NULL DEFAULT NULL COMMENT '下次应复习时间',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '首次学习时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_question`(`user_id` ASC, `question_id` ASC) USING BTREE,
  INDEX `idx_user_next_review`(`user_id` ASC, `next_review_time` ASC) USING BTREE,
  INDEX `idx_user_mastery`(`user_id` ASC, `mastery_level` ASC) USING BTREE,
  INDEX `idx_user_set`(`user_id` ASC, `question_set_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '面试题掌握度记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for interview_question
-- ----------------------------
DROP TABLE IF EXISTS `interview_question`;
CREATE TABLE `interview_question`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '题目ID',
  `question_set_id` bigint NOT NULL COMMENT '所属题单ID',
  `title` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '题目标题',
  `answer` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '参考答案 (Markdown格式)',
  `sort_order` int NULL DEFAULT 0 COMMENT '题单内排序',
  `view_count` int NULL DEFAULT 0 COMMENT '浏览次数',
  `favorite_count` int NULL DEFAULT 0 COMMENT '收藏次数',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_question_set_id`(`question_set_id` ASC) USING BTREE,
  INDEX `idx_sort_order`(`question_set_id` ASC, `sort_order` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  FULLTEXT INDEX `ft_title_answer`(`title`, `answer`),
  CONSTRAINT `fk_question_question_set` FOREIGN KEY (`question_set_id`) REFERENCES `interview_question_set` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 158 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '面试题目表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for interview_question_set
-- ----------------------------
DROP TABLE IF EXISTS `interview_question_set`;
CREATE TABLE `interview_question_set`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '题单ID',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '题单标题',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '题单描述',
  `category_id` bigint NOT NULL COMMENT '分类ID',
  `type` tinyint NULL DEFAULT 2 COMMENT '类型 (1-官方 2-用户创建)',
  `visibility` tinyint NULL DEFAULT 1 COMMENT '可见性 (1-公开 2-私有) 仅用户创建题单有效',
  `question_count` int NULL DEFAULT 0 COMMENT '题目数量',
  `view_count` int NULL DEFAULT 0 COMMENT '浏览次数',
  `favorite_count` int NULL DEFAULT 0 COMMENT '收藏次数',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态 (0-草稿 1-发布 2-下线)',
  `creator_id` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `creator_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '创建人姓名',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_category_id`(`category_id` ASC) USING BTREE,
  INDEX `idx_creator_id`(`creator_id` ASC) USING BTREE,
  INDEX `idx_type_status`(`type` ASC, `status` ASC) USING BTREE,
  INDEX `idx_visibility_status`(`visibility` ASC, `status` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  FULLTEXT INDEX `ft_title_description`(`title`, `description`),
  CONSTRAINT `fk_question_set_category` FOREIGN KEY (`category_id`) REFERENCES `interview_category` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '面试题单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for job_battle_plan_record
-- ----------------------------
DROP TABLE IF EXISTS `job_battle_plan_record`;
CREATE TABLE `job_battle_plan_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `plan_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '计划名称',
  `total_days` int NULL DEFAULT NULL COMMENT '计划总天数（AI输出）',
  `target_days` int NULL DEFAULT NULL COMMENT '目标天数（用户输入）',
  `weekly_hours` int NULL DEFAULT NULL COMMENT '每周投入小时',
  `preferred_learning_mode` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '学习偏好',
  `next_interview_date` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '下一场面试日期',
  `gaps_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '差距项JSON',
  `plan_result_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '计划完整结果JSON',
  `is_fallback` tinyint(1) NULL DEFAULT 0 COMMENT '是否降级结果：0-否 1-是',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_create_time`(`user_id` ASC, `create_time` DESC) USING BTREE,
  INDEX `idx_user_plan_name`(`user_id` ASC, `plan_name` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '求职作战台计划历史' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for knowledge_map
-- ----------------------------
DROP TABLE IF EXISTS `knowledge_map`;
CREATE TABLE `knowledge_map`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '图谱ID',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '图谱标题',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '图谱描述',
  `cover_image` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '封面图片URL',
  `user_id` bigint NOT NULL COMMENT '创建用户ID(管理员)',
  `node_count` int NULL DEFAULT 0 COMMENT '节点总数',
  `view_count` int NULL DEFAULT 0 COMMENT '查看次数',
  `status` tinyint(1) NULL DEFAULT 0 COMMENT '状态: 0-草稿 1-已发布 2-已隐藏',
  `sort_order` int NULL DEFAULT 0 COMMENT '排序权重',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_sort_order`(`sort_order` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '知识图谱表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for knowledge_node
-- ----------------------------
DROP TABLE IF EXISTS `knowledge_node`;
CREATE TABLE `knowledge_node`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '节点ID',
  `map_id` bigint NOT NULL COMMENT '所属图谱ID',
  `parent_id` bigint NULL DEFAULT 0 COMMENT '父节点ID，0为根节点',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '节点标题',
  `url` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '飞书云文档链接',
  `node_type` tinyint(1) NULL DEFAULT 1 COMMENT '节点类型: 1-普通 2-重点 3-难点',
  `sort_order` int NULL DEFAULT 0 COMMENT '同级排序序号',
  `level_depth` int NULL DEFAULT 1 COMMENT '层级深度',
  `is_expanded` tinyint(1) NULL DEFAULT 1 COMMENT '是否默认展开',
  `view_count` int NULL DEFAULT 0 COMMENT '查看次数',
  `last_view_time` datetime NULL DEFAULT NULL COMMENT '最后查看时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_map_id`(`map_id` ASC) USING BTREE,
  INDEX `idx_parent_id`(`parent_id` ASC) USING BTREE,
  INDEX `idx_sort_order`(`sort_order` ASC) USING BTREE,
  INDEX `idx_level_depth`(`level_depth` ASC) USING BTREE,
  CONSTRAINT `knowledge_node_ibfk_1` FOREIGN KEY (`map_id`) REFERENCES `knowledge_map` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '知识节点表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for lottery_adjust_history
-- ----------------------------
DROP TABLE IF EXISTS `lottery_adjust_history`;
CREATE TABLE `lottery_adjust_history`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `prize_id` bigint NOT NULL COMMENT '奖品ID',
  `adjust_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '调整类型：AUTO-自动 MANUAL-手动',
  `old_probability` decimal(10, 8) NOT NULL COMMENT '调整前概率',
  `new_probability` decimal(10, 8) NOT NULL COMMENT '调整后概率',
  `old_return_rate` decimal(6, 4) NULL DEFAULT NULL COMMENT '调整前回报率',
  `new_return_rate` decimal(6, 4) NULL DEFAULT NULL COMMENT '调整后回报率',
  `adjust_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '调整原因',
  `operator` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '操作人（SYSTEM或管理员名称）',
  `operator_id` bigint NULL DEFAULT NULL COMMENT '操作人ID（管理员ID）',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '调整时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_prize_id`(`prize_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` DESC) USING BTREE,
  INDEX `idx_adjust_type`(`adjust_type` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 62 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '概率调整历史表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for lottery_draw_record
-- ----------------------------
DROP TABLE IF EXISTS `lottery_draw_record`;
CREATE TABLE `lottery_draw_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `prize_id` bigint NOT NULL COMMENT '奖品ID',
  `prize_level` tinyint NOT NULL COMMENT '奖品等级',
  `prize_points` int NOT NULL COMMENT '获得积分',
  `cost_points` int NOT NULL COMMENT '消耗积分',
  `actual_probability` decimal(10, 8) NULL DEFAULT NULL COMMENT '实际中奖概率',
  `draw_strategy` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '使用的抽奖策略',
  `draw_ip` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '抽奖IP',
  `draw_device` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '抽奖设备',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：1-成功 2-失败 3-已补偿',
  `cost_detail_id` bigint NULL DEFAULT NULL COMMENT '积分明细ID（扣减）',
  `reward_detail_id` bigint NULL DEFAULT NULL COMMENT '积分明细ID（奖励）',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '抽奖时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` DESC) USING BTREE,
  INDEX `idx_user_time`(`user_id` ASC, `create_time` DESC) USING BTREE,
  INDEX `idx_prize_level`(`prize_level` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_prize_id`(`prize_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '抽奖记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for lottery_prize_config
-- ----------------------------
DROP TABLE IF EXISTS `lottery_prize_config`;
CREATE TABLE `lottery_prize_config`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '奖品ID',
  `prize_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '奖品名称',
  `prize_level` tinyint NOT NULL COMMENT '奖品等级：1-特等奖 2-一等奖 3-二等奖 4-三等奖 5-四等奖 6-五等奖 7-六等奖 8-未中奖',
  `prize_points` int NOT NULL COMMENT '奖励积分',
  `base_probability` decimal(10, 8) NOT NULL COMMENT '基础中奖概率（0-1之间）',
  `current_probability` decimal(10, 8) NOT NULL COMMENT '当前动态概率（0-1之间）',
  `target_return_rate` decimal(6, 4) NULL DEFAULT 0.0100 COMMENT '目标回报率（如0.0100=1%）',
  `max_return_rate` decimal(6, 4) NULL DEFAULT 0.0150 COMMENT '最大回报率阈值（如0.0150=1.5%）',
  `min_return_rate` decimal(6, 4) NULL DEFAULT 0.0050 COMMENT '最小回报率阈值（如0.0050=0.5%）',
  `actual_return_rate` decimal(6, 4) NULL DEFAULT 0.0000 COMMENT '实际回报率',
  `total_draw_count` int NULL DEFAULT 0 COMMENT '总抽奖次数（作为分母）',
  `total_win_count` int NULL DEFAULT 0 COMMENT '总中奖次数',
  `today_draw_count` int NULL DEFAULT 0 COMMENT '今日抽奖次数',
  `today_win_count` int NULL DEFAULT 0 COMMENT '今日中奖次数',
  `daily_stock` int NULL DEFAULT -1 COMMENT '每日库存（-1表示无限制）',
  `total_stock` int NULL DEFAULT -1 COMMENT '总库存（-1表示无限制）',
  `current_stock` int NULL DEFAULT 0 COMMENT '当前剩余库存',
  `display_order` int NULL DEFAULT 0 COMMENT '显示顺序',
  `prize_icon` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '奖品图标',
  `prize_desc` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '奖品描述',
  `is_active` tinyint NULL DEFAULT 1 COMMENT '是否启用：0-禁用 1-启用',
  `is_suspended` tinyint NULL DEFAULT 0 COMMENT '是否暂停：0-正常 1-暂停（回报率超标）',
  `suspend_reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '暂停原因',
  `suspend_until` datetime NULL DEFAULT NULL COMMENT '暂停至某时间',
  `adjust_strategy` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'AUTO' COMMENT '调整策略：AUTO-自动 MANUAL-手动 FIXED-固定',
  `last_adjust_time` datetime NULL DEFAULT NULL COMMENT '最后调整时间',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_active`(`is_active` ASC) USING BTREE,
  INDEX `idx_level`(`prize_level` ASC) USING BTREE,
  INDEX `idx_probability`(`current_probability` DESC) USING BTREE,
  INDEX `idx_return_rate`(`actual_return_rate` DESC) USING BTREE,
  INDEX `idx_suspended`(`is_suspended` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '抽奖奖品配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for lottery_statistics_daily
-- ----------------------------
DROP TABLE IF EXISTS `lottery_statistics_daily`;
CREATE TABLE `lottery_statistics_daily`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '统计ID',
  `stat_date` date NOT NULL COMMENT '统计日期',
  `total_draw_count` int NULL DEFAULT 0 COMMENT '总抽奖次数',
  `total_cost_points` bigint NULL DEFAULT 0 COMMENT '总消耗积分',
  `total_reward_points` bigint NULL DEFAULT 0 COMMENT '总奖励积分',
  `actual_return_rate` decimal(5, 2) NULL DEFAULT NULL COMMENT '实际回报率（%）',
  `profit_points` bigint NULL DEFAULT 0 COMMENT '平台利润积分',
  `unique_user_count` int NULL DEFAULT 0 COMMENT '参与用户数',
  `special_prize_count` int NULL DEFAULT 0 COMMENT '特等奖中奖次数',
  `first_prize_count` int NULL DEFAULT 0 COMMENT '一等奖中奖次数',
  `second_prize_count` int NULL DEFAULT 0 COMMENT '二等奖中奖次数',
  `third_prize_count` int NULL DEFAULT 0 COMMENT '三等奖中奖次数',
  `fourth_prize_count` int NULL DEFAULT 0 COMMENT '四等奖中奖次数',
  `fifth_prize_count` int NULL DEFAULT 0 COMMENT '五等奖中奖次数',
  `sixth_prize_count` int NULL DEFAULT 0 COMMENT '六等奖中奖次数',
  `no_prize_count` int NULL DEFAULT 0 COMMENT '未中奖次数',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_stat_date`(`stat_date` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` DESC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '抽奖每日统计表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for mock_interview_direction
-- ----------------------------
DROP TABLE IF EXISTS `mock_interview_direction`;
CREATE TABLE `mock_interview_direction`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `direction_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '方向代码（java/frontend等）',
  `direction_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '方向名称',
  `icon` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '图标',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '描述',
  `category_ids` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '关联的题库分类ID（逗号分隔）',
  `sort_order` int NULL DEFAULT 0 COMMENT '排序',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_direction_code`(`direction_code` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '模拟面试方向配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for mock_interview_qa
-- ----------------------------
DROP TABLE IF EXISTS `mock_interview_qa`;
CREATE TABLE `mock_interview_qa`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `session_id` bigint NOT NULL COMMENT '会话ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `question_id` bigint NULL DEFAULT NULL COMMENT '关联的题库题目ID（可为空）',
  `question_order` int NOT NULL COMMENT '题目序号',
  `question_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '问题内容',
  `question_type` tinyint NULL DEFAULT 1 COMMENT '问题类型：1-主问题 2-追问',
  `parent_qa_id` bigint NULL DEFAULT NULL COMMENT '父问答ID（追问时关联主问题）',
  `user_answer` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '用户回答',
  `answer_duration_seconds` int NULL DEFAULT NULL COMMENT '回答用时（秒）',
  `score` int NULL DEFAULT NULL COMMENT '得分（0-10）',
  `ai_feedback` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT 'AI反馈（JSON格式）',
  `reference_answer` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '参考答案',
  `knowledge_points` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '考察知识点（逗号分隔）',
  `status` tinyint NULL DEFAULT 0 COMMENT '状态：0-待回答 1-已回答 2-已跳过',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_session_id`(`session_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_question_id`(`question_id` ASC) USING BTREE,
  INDEX `idx_parent_qa_id`(`parent_qa_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 66 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '模拟面试问答记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for mock_interview_session
-- ----------------------------
DROP TABLE IF EXISTS `mock_interview_session`;
CREATE TABLE `mock_interview_session`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `direction` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '面试方向（java/frontend/python等）',
  `level` tinyint NOT NULL COMMENT '难度级别：1-初级 2-中级 3-高级',
  `interview_type` tinyint NULL DEFAULT 1 COMMENT '面试类型：1-技术 2-综合 3-专项',
  `style` tinyint NULL DEFAULT 2 COMMENT 'AI风格：1-温和 2-标准 3-压力',
  `question_count` int NOT NULL COMMENT '题目数量',
  `question_mode` tinyint NULL DEFAULT 2 COMMENT '出题模式：1-本地题库 2-AI出题',
  `duration_minutes` int NULL DEFAULT NULL COMMENT '预计时长（分钟）',
  `status` tinyint NULL DEFAULT 0 COMMENT '状态：0-进行中 1-已完成 2-已中断',
  `total_score` int NULL DEFAULT NULL COMMENT '总分（满分100）',
  `knowledge_score` int NULL DEFAULT NULL COMMENT '知识得分',
  `depth_score` int NULL DEFAULT NULL COMMENT '深度得分',
  `expression_score` int NULL DEFAULT NULL COMMENT '表达得分',
  `adaptability_score` int NULL DEFAULT NULL COMMENT '应变得分',
  `ai_summary` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT 'AI总体评价',
  `ai_suggestion` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT 'AI改进建议',
  `current_question_order` int NULL DEFAULT 0 COMMENT '当前题目序号',
  `start_time` datetime NULL DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime NULL DEFAULT NULL COMMENT '结束时间',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_user_status`(`user_id` ASC, `status` ASC) USING BTREE,
  INDEX `idx_direction`(`direction` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` DESC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '模拟面试会话表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for mock_interview_user_stats
-- ----------------------------
DROP TABLE IF EXISTS `mock_interview_user_stats`;
CREATE TABLE `mock_interview_user_stats`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '统计ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `total_interviews` int NULL DEFAULT 0 COMMENT '总面试次数',
  `completed_interviews` int NULL DEFAULT 0 COMMENT '完成面试次数',
  `avg_score` decimal(5, 2) NULL DEFAULT 0.00 COMMENT '平均分',
  `highest_score` int NULL DEFAULT 0 COMMENT '最高分',
  `total_questions` int NULL DEFAULT 0 COMMENT '总回答题数',
  `correct_questions` int NULL DEFAULT 0 COMMENT '高分题数（>=7分）',
  `interview_streak` int NULL DEFAULT 0 COMMENT '连续面试天数',
  `max_streak` int NULL DEFAULT 0 COMMENT '最长连续天数',
  `last_interview_date` date NULL DEFAULT NULL COMMENT '最后面试日期',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户面试统计表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for moment_comments
-- ----------------------------
DROP TABLE IF EXISTS `moment_comments`;
CREATE TABLE `moment_comments`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  `moment_id` bigint NOT NULL COMMENT '动态ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '评论内容(最多200字)',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：1正常 0删除',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_moment_id`(`moment_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 448 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '动态评论表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for moment_favorites
-- ----------------------------
DROP TABLE IF EXISTS `moment_favorites`;
CREATE TABLE `moment_favorites`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
  `moment_id` bigint NOT NULL COMMENT '动态ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_moment_user`(`moment_id` ASC, `user_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_moment_id`(`moment_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` DESC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '动态收藏表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for moment_likes
-- ----------------------------
DROP TABLE IF EXISTS `moment_likes`;
CREATE TABLE `moment_likes`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '点赞ID',
  `moment_id` bigint NOT NULL COMMENT '动态ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_moment_user`(`moment_id` ASC, `user_id` ASC) USING BTREE,
  INDEX `idx_moment_id`(`moment_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '动态点赞表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for moment_views
-- ----------------------------
DROP TABLE IF EXISTS `moment_views`;
CREATE TABLE `moment_views`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '浏览记录ID',
  `moment_id` bigint NOT NULL COMMENT '动态ID',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID（未登录为NULL）',
  `ip` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'IP地址',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '浏览时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_moment_id`(`moment_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '动态浏览记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for moments
-- ----------------------------
DROP TABLE IF EXISTS `moments`;
CREATE TABLE `moments`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '动态ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '动态内容(最多100字)',
  `images` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '图片URLs，JSON格式存储',
  `like_count` int NULL DEFAULT 0 COMMENT '点赞数',
  `comment_count` int NULL DEFAULT 0 COMMENT '评论数',
  `view_count` int NULL DEFAULT 0 COMMENT '浏览数',
  `favorite_count` int NULL DEFAULT 0 COMMENT '收藏数',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：1正常 0删除 2审核中',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` DESC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_status_time`(`status` ASC, `create_time` DESC) USING BTREE,
  INDEX `idx_hot_score`(`status` ASC, `create_time` ASC, `like_count` ASC, `comment_count` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '动态表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for notification
-- ----------------------------
DROP TABLE IF EXISTS `notification`;
CREATE TABLE `notification`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消息标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '消息内容',
  `type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'PERSONAL' COMMENT '消息类型：PERSONAL(个人消息)/ANNOUNCEMENT(系统公告)/COMMUNITY_INTERACTION(社区互动)/SYSTEM(系统通知)',
  `priority` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'LOW' COMMENT '优先级：HIGH(高)/MEDIUM(中)/LOW(低)',
  `sender_id` bigint NULL DEFAULT 0 COMMENT '发送者ID（系统消息为0）',
  `receiver_id` bigint NULL DEFAULT NULL COMMENT '接收者ID（为NULL表示全站公告）',
  `source_module` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '来源模块：community/interview/system等',
  `source_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '来源数据ID，如帖子ID、题目ID等',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'UNREAD' COMMENT '状态：UNREAD(未读)/read(已读)/DELETED(已删除)',
  `read_time` datetime NULL DEFAULT NULL COMMENT '阅读时间',
  `created_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_receiver_status`(`receiver_id` ASC, `status` ASC) USING BTREE,
  INDEX `idx_created_time`(`created_time` ASC) USING BTREE,
  INDEX `idx_type`(`type` ASC) USING BTREE,
  INDEX `idx_priority`(`priority` ASC) USING BTREE,
  INDEX `idx_source`(`source_module` ASC, `source_id` ASC) USING BTREE,
  INDEX `idx_notification_user_unread`(`receiver_id` ASC, `status` ASC, `created_time` DESC) USING BTREE,
  INDEX `idx_notification_announcement`(`receiver_id` ASC, `type` ASC, `created_time` DESC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '消息通知表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for notification_config
-- ----------------------------
DROP TABLE IF EXISTS `notification_config`;
CREATE TABLE `notification_config`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消息类型：PERSONAL/ANNOUNCEMENT/COMMUNITY_INTERACTION/SYSTEM等',
  `is_enabled` tinyint NULL DEFAULT 1 COMMENT '是否启用接收：0(禁用)/1(启用)',
  `created_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_type`(`user_id` ASC, `type` ASC) USING BTREE,
  INDEX `idx_user_enabled`(`user_id` ASC, `is_enabled` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '消息配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for notification_template
-- ----------------------------
DROP TABLE IF EXISTS `notification_template`;
CREATE TABLE `notification_template`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '模板ID',
  `code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模板代码',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模板名称',
  `title_template` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标题模板',
  `content_template` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '内容模板',
  `is_enabled` tinyint NULL DEFAULT 1 COMMENT '是否启用：0(禁用)/1(启用)',
  `created_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_code`(`code` ASC) USING BTREE,
  INDEX `idx_enabled`(`is_enabled` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '消息模板表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for notification_user_read_record
-- ----------------------------
DROP TABLE IF EXISTS `notification_user_read_record`;
CREATE TABLE `notification_user_read_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `notification_id` bigint NOT NULL COMMENT '通知ID',
  `read_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '阅读时间',
  `created_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_notification`(`user_id` ASC, `notification_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_notification_id`(`notification_id` ASC) USING BTREE,
  INDEX `idx_read_time`(`read_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户通知阅读记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for oj_contest
-- ----------------------------
DROP TABLE IF EXISTS `oj_contest`;
CREATE TABLE `oj_contest`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '赛事ID',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '赛事标题',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '赛事描述',
  `contest_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'weekly' COMMENT '赛事类型(weekly/challenge)',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态(0=草稿,1=即将开始,2=进行中,3=已结束)',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime NOT NULL COMMENT '结束时间',
  `created_by` bigint NOT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_contest_type`(`contest_type` ASC) USING BTREE,
  INDEX `idx_time_window`(`start_time` ASC, `end_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'OJ赛事表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for oj_contest_participant
-- ----------------------------
DROP TABLE IF EXISTS `oj_contest_participant`;
CREATE TABLE `oj_contest_participant`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '报名ID',
  `contest_id` bigint NOT NULL COMMENT '赛事ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `join_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '报名时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_contest_user`(`contest_id` ASC, `user_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'OJ赛事参赛者表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for oj_contest_problem
-- ----------------------------
DROP TABLE IF EXISTS `oj_contest_problem`;
CREATE TABLE `oj_contest_problem`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '关联ID',
  `contest_id` bigint NOT NULL COMMENT '赛事ID',
  `problem_id` bigint NOT NULL COMMENT '题目ID',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序号',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_contest_problem`(`contest_id` ASC, `problem_id` ASC) USING BTREE,
  INDEX `idx_problem_id`(`problem_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'OJ赛事题目关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for oj_problem
-- ----------------------------
DROP TABLE IF EXISTS `oj_problem`;
CREATE TABLE `oj_problem`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '题目ID',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '题目标题',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '题目描述 (Markdown)',
  `difficulty` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'easy' COMMENT '难度 (easy/medium/hard)',
  `time_limit` int NOT NULL DEFAULT 2000 COMMENT '时间限制 (ms)',
  `memory_limit` int NOT NULL DEFAULT 256 COMMENT '内存限制 (MB)',
  `input_description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '输入说明',
  `output_description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '输出说明',
  `sample_input` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '示例输入',
  `sample_output` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '示例输出',
  `accepted_count` int NOT NULL DEFAULT 0 COMMENT '通过数',
  `submit_count` int NOT NULL DEFAULT 0 COMMENT '提交数',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态 (0=隐藏, 1=公开)',
  `create_user_id` bigint NULL DEFAULT NULL COMMENT '创建者用户ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_difficulty`(`difficulty` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'OJ题目表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for oj_problem_tag
-- ----------------------------
DROP TABLE IF EXISTS `oj_problem_tag`;
CREATE TABLE `oj_problem_tag`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '标签ID',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标签名称',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_name`(`name` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 21 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'OJ题目标签表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for oj_problem_tag_relation
-- ----------------------------
DROP TABLE IF EXISTS `oj_problem_tag_relation`;
CREATE TABLE `oj_problem_tag_relation`  (
  `problem_id` bigint NOT NULL COMMENT '题目ID',
  `tag_id` bigint NOT NULL COMMENT '标签ID',
  PRIMARY KEY (`problem_id`, `tag_id`) USING BTREE,
  INDEX `idx_tag_id`(`tag_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'OJ题目-标签关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for oj_solution
-- ----------------------------
DROP TABLE IF EXISTS `oj_solution`;
CREATE TABLE `oj_solution`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '题解ID',
  `problem_id` bigint NOT NULL COMMENT '题目ID',
  `language` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '编程语言',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '题解标题',
  `code` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标准答案代码',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '题解说明 (Markdown)',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序号',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_problem_id`(`problem_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'OJ标准答案表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for oj_submission
-- ----------------------------
DROP TABLE IF EXISTS `oj_submission`;
CREATE TABLE `oj_submission`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '提交ID',
  `problem_id` bigint NOT NULL COMMENT '题目ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `language` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '编程语言',
  `code` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '提交的源代码',
  `status` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending' COMMENT '判题状态',
  `time_used` bigint NULL DEFAULT NULL COMMENT '耗时 (ms)',
  `memory_used` bigint NULL DEFAULT NULL COMMENT '内存使用 (KB)',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '错误信息',
  `pass_count` int NULL DEFAULT NULL COMMENT '通过用例数',
  `total_count` int NULL DEFAULT NULL COMMENT '总用例数',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  `contest_id` bigint NULL DEFAULT NULL COMMENT '赛事ID(为空表示普通提交)',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_problem_id`(`problem_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_user_problem`(`user_id` ASC, `problem_id` ASC) USING BTREE,
  INDEX `idx_contest_id`(`contest_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'OJ提交记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for oj_test_case
-- ----------------------------
DROP TABLE IF EXISTS `oj_test_case`;
CREATE TABLE `oj_test_case`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用例ID',
  `problem_id` bigint NOT NULL COMMENT '题目ID',
  `input` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '输入数据',
  `expected_output` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '期望输出',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序号',
  `is_sample` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否为示例用例',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_problem_id`(`problem_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 56 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'OJ测试用例表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for plan_checkin_record
-- ----------------------------
DROP TABLE IF EXISTS `plan_checkin_record`;
CREATE TABLE `plan_checkin_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `plan_id` bigint NOT NULL COMMENT '计划ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `checkin_date` date NOT NULL COMMENT '打卡日期',
  `checkin_time` datetime NOT NULL COMMENT '打卡时间',
  `complete_value` int NULL DEFAULT NULL COMMENT '完成数量',
  `complete_content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '完成内容描述',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '心得备注',
  `is_supplement` tinyint NULL DEFAULT 0 COMMENT '是否补卡：0-否 1-是',
  `points_earned` int NULL DEFAULT 0 COMMENT '获得积分',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_plan_date`(`plan_id` ASC, `checkin_date` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_plan_id`(`plan_id` ASC) USING BTREE,
  INDEX `idx_checkin_date`(`checkin_date` ASC) USING BTREE,
  INDEX `idx_user_date`(`user_id` ASC, `checkin_date` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '计划打卡记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for plan_remind_task
-- ----------------------------
DROP TABLE IF EXISTS `plan_remind_task`;
CREATE TABLE `plan_remind_task`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `plan_id` bigint NOT NULL COMMENT '计划ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `remind_type` tinyint NOT NULL COMMENT '提醒类型：1-开始提醒 2-截止提醒',
  `remind_date` date NOT NULL COMMENT '提醒日期',
  `remind_time` datetime NOT NULL COMMENT '提醒时间',
  `status` tinyint NULL DEFAULT 0 COMMENT '状态：0-待发送 1-已发送 2-已取消',
  `send_time` datetime NULL DEFAULT NULL COMMENT '实际发送时间',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_remind_time`(`remind_time` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_plan_id`(`plan_id` ASC) USING BTREE,
  INDEX `idx_remind_date_status`(`remind_date` ASC, `status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '计划提醒任务表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for resume_analytics
-- ----------------------------
DROP TABLE IF EXISTS `resume_analytics`;
CREATE TABLE `resume_analytics`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '统计ID',
  `resume_id` bigint NOT NULL COMMENT '简历ID',
  `view_count` bigint NULL DEFAULT 0 COMMENT '浏览次数',
  `export_count` bigint NULL DEFAULT 0 COMMENT '导出次数',
  `share_count` bigint NULL DEFAULT 0 COMMENT '分享次数',
  `unique_visitors` bigint NULL DEFAULT 0 COMMENT '访客数',
  `last_access_time` datetime NULL DEFAULT NULL COMMENT '最近访问时间',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `resume_id`(`resume_id` ASC) USING BTREE,
  INDEX `idx_update_time`(`update_time` DESC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '简历访问统计表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for resume_info
-- ----------------------------
DROP TABLE IF EXISTS `resume_info`;
CREATE TABLE `resume_info`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '简历ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `resume_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '简历名称',
  `template_id` bigint NOT NULL COMMENT '模板ID',
  `summary` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '个人概述',
  `status` tinyint NULL DEFAULT 0 COMMENT '状态：0-草稿 1-发布',
  `visibility` tinyint NULL DEFAULT 0 COMMENT '可见性：0-私密 1-公开',
  `version` int NULL DEFAULT 1 COMMENT '版本号',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_template`(`template_id` ASC) USING BTREE,
  INDEX `idx_update_time`(`update_time` DESC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '简历主体信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for resume_sections
-- ----------------------------
DROP TABLE IF EXISTS `resume_sections`;
CREATE TABLE `resume_sections`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '模块ID',
  `resume_id` bigint NOT NULL COMMENT '简历ID',
  `section_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '模块类型（PROFILE/WORK等）',
  `title` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模块标题',
  `content` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '模块内容',
  `sort_order` int NULL DEFAULT 0 COMMENT '排序',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：1-启用 0-禁用',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_resume_id`(`resume_id` ASC) USING BTREE,
  INDEX `idx_sort`(`resume_id` ASC, `sort_order` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '简历模块内容表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for resume_shares
-- ----------------------------
DROP TABLE IF EXISTS `resume_shares`;
CREATE TABLE `resume_shares`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '分享ID',
  `resume_id` bigint NOT NULL COMMENT '简历ID',
  `share_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '分享口令',
  `share_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '分享链接',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：0-关闭 1-启用',
  `access_count` int NULL DEFAULT 0 COMMENT '访问次数',
  `expire_time` datetime NULL DEFAULT NULL COMMENT '过期时间',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_share_resume`(`resume_id` ASC, `status` ASC) USING BTREE,
  INDEX `idx_share_code`(`share_code` ASC) USING BTREE,
  INDEX `idx_expire_time`(`expire_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '简历分享记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for resume_templates
-- ----------------------------
DROP TABLE IF EXISTS `resume_templates`;
CREATE TABLE `resume_templates`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '模板ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模板名称',
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '模板分类（前端/后端/全栈等）',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '模板简介',
  `cover_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '封面地址',
  `preview_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '预览地址',
  `tags` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '标签，逗号分隔',
  `tech_stack` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '推荐技术栈',
  `experience_level` tinyint NULL DEFAULT 1 COMMENT '经验层级（1-5）',
  `rating` decimal(3, 2) NULL DEFAULT 0.00 COMMENT '评分',
  `rating_count` int NULL DEFAULT 0 COMMENT '评分次数',
  `download_count` int NULL DEFAULT 0 COMMENT '下载次数',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：0-下线 1-启用',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `updated_by` bigint NULL DEFAULT NULL COMMENT '最后修改人',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_category`(`category` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_experience`(`experience_level` ASC) USING BTREE,
  FULLTEXT INDEX `ft_template_keyword`(`name`, `description`, `tags`) WITH PARSER `ngram`
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '简历模板表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for resume_versions
-- ----------------------------
DROP TABLE IF EXISTS `resume_versions`;
CREATE TABLE `resume_versions`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '版本ID',
  `resume_id` bigint NOT NULL COMMENT '简历ID',
  `version_number` int NOT NULL COMMENT '版本号',
  `snapshot` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '快照数据（JSON）',
  `change_log` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '变更说明',
  `created_by` bigint NULL DEFAULT NULL COMMENT '操作人',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_resume_version`(`resume_id` ASC, `version_number` DESC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '简历版本快照表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sensitive_category
-- ----------------------------
DROP TABLE IF EXISTS `sensitive_category`;
CREATE TABLE `sensitive_category`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分类名称',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '分类描述',
  `sort_order` int NULL DEFAULT 0 COMMENT '排序',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态 0-禁用 1-启用',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '敏感词分类表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sensitive_hit_statistics
-- ----------------------------
DROP TABLE IF EXISTS `sensitive_hit_statistics`;
CREATE TABLE `sensitive_hit_statistics`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `stat_date` date NOT NULL COMMENT '统计日期',
  `word` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '敏感词',
  `category_id` int NULL DEFAULT NULL COMMENT '分类ID',
  `hit_count` int NULL DEFAULT 0 COMMENT '命中次数',
  `module` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '业务模块',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_date_word_module`(`stat_date` ASC, `word` ASC, `module` ASC) USING BTREE,
  INDEX `idx_date`(`stat_date` ASC) USING BTREE,
  INDEX `idx_hit_count`(`hit_count` DESC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '敏感词命中统计表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sensitive_homophone
-- ----------------------------
DROP TABLE IF EXISTS `sensitive_homophone`;
CREATE TABLE `sensitive_homophone`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `original_char` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '原始字符',
  `homophone_chars` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '同音字（逗号分隔）',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态 0-禁用 1-启用',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_original`(`original_char` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '同音字映射表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sensitive_log
-- ----------------------------
DROP TABLE IF EXISTS `sensitive_log`;
CREATE TABLE `sensitive_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `module` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '模块名称',
  `business_id` bigint NULL DEFAULT NULL COMMENT '业务ID',
  `original_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '原始文本',
  `hit_words` json NULL COMMENT '命中的敏感词',
  `action` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '执行的动作',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_time`(`user_id` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_module_time`(`module` ASC, `create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '敏感词检测日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sensitive_similar_char
-- ----------------------------
DROP TABLE IF EXISTS `sensitive_similar_char`;
CREATE TABLE `sensitive_similar_char`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `original_char` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '原始字符',
  `similar_chars` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '形似字（逗号分隔）',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态 0-禁用 1-启用',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_original`(`original_char` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '形似字映射表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sensitive_source
-- ----------------------------
DROP TABLE IF EXISTS `sensitive_source`;
CREATE TABLE `sensitive_source`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `source_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '词库来源名称',
  `source_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '来源类型（local/api/github）',
  `api_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'API地址',
  `api_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'API密钥',
  `sync_interval` int NULL DEFAULT 24 COMMENT '同步间隔（小时）',
  `last_sync_time` datetime NULL DEFAULT NULL COMMENT '最后同步时间',
  `sync_status` tinyint NULL DEFAULT NULL COMMENT '同步状态 0-失败 1-成功',
  `word_count` int NULL DEFAULT 0 COMMENT '词汇数量',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态 0-禁用 1-启用',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '敏感词来源管理表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sensitive_strategy
-- ----------------------------
DROP TABLE IF EXISTS `sensitive_strategy`;
CREATE TABLE `sensitive_strategy`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `strategy_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '策略名称',
  `module` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务模块（community/interview/moment等）',
  `level` tinyint NOT NULL COMMENT '风险等级 1-低 2-中 3-高',
  `action` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '处理动作（replace/reject/warn）',
  `notify_admin` tinyint NULL DEFAULT 0 COMMENT '是否通知管理员 0-否 1-是',
  `limit_user` tinyint NULL DEFAULT 0 COMMENT '是否限制用户 0-否 1-是',
  `limit_duration` int NULL DEFAULT NULL COMMENT '限制时长（分钟）',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '策略描述',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态 0-禁用 1-启用',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_module_level`(`module` ASC, `level` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '敏感词处理策略表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sensitive_user_violation
-- ----------------------------
DROP TABLE IF EXISTS `sensitive_user_violation`;
CREATE TABLE `sensitive_user_violation`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `stat_date` date NOT NULL COMMENT '统计日期',
  `violation_count` int NULL DEFAULT 0 COMMENT '违规次数',
  `last_violation_time` datetime NULL DEFAULT NULL COMMENT '最后违规时间',
  `is_restricted` tinyint NULL DEFAULT 0 COMMENT '是否被限制 0-否 1-是',
  `restrict_end_time` datetime NULL DEFAULT NULL COMMENT '限制结束时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_date`(`user_id` ASC, `stat_date` ASC) USING BTREE,
  INDEX `idx_violation_count`(`violation_count` DESC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户违规统计表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sensitive_version
-- ----------------------------
DROP TABLE IF EXISTS `sensitive_version`;
CREATE TABLE `sensitive_version`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `version_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '版本号（如v1.0.1）',
  `change_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '变更类型（add/update/delete/import）',
  `change_count` int NULL DEFAULT NULL COMMENT '变更数量',
  `change_detail` json NULL COMMENT '变更详情',
  `operator_id` bigint NULL DEFAULT NULL COMMENT '操作人ID',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '敏感词版本历史表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sensitive_whitelist
-- ----------------------------
DROP TABLE IF EXISTS `sensitive_whitelist`;
CREATE TABLE `sensitive_whitelist`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `word` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '白名单词汇',
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '分类（专业术语/成语/人名/品牌等）',
  `reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '加入白名单的原因',
  `scope` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'global' COMMENT '作用范围（global-全局/module-模块级）',
  `module_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '模块名称（scope=module时有效）',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态 0-禁用 1-启用',
  `creator_id` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_word_scope`(`word` ASC, `scope` ASC, `module_name` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '敏感词白名单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sensitive_word
-- ----------------------------
DROP TABLE IF EXISTS `sensitive_word`;
CREATE TABLE `sensitive_word`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `word` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '敏感词',
  `word_type` tinyint NULL DEFAULT 1 COMMENT '词类型 1-普通词 2-正则表达式',
  `pinyin` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '拼音（用于拼音检测）',
  `category_id` int NOT NULL COMMENT '分类ID',
  `level` tinyint NULL DEFAULT 1 COMMENT '风险等级 1-低 2-中 3-高',
  `action` tinyint NULL DEFAULT 1 COMMENT '处理动作 1-替换 2-拒绝 3-审核',
  `enable_variant_check` tinyint NULL DEFAULT 1 COMMENT '启用变形词检测 0-否 1-是',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态 0-禁用 1-启用',
  `creator_id` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_category_status`(`category_id` ASC, `status` ASC) USING BTREE,
  INDEX `idx_update_time`(`update_time` ASC) USING BTREE,
  INDEX `idx_word`(`word` ASC) USING BTREE,
  INDEX `idx_word_type`(`word_type` ASC) USING BTREE,
  INDEX `idx_pinyin`(`pinyin`(100) ASC) USING BTREE,
  CONSTRAINT `sensitive_word_ibfk_1` FOREIGN KEY (`category_id`) REFERENCES `sensitive_category` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 2417 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '敏感词表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sql_monitor_log
-- ----------------------------
DROP TABLE IF EXISTS `sql_monitor_log`;
CREATE TABLE `sql_monitor_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `trace_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '跟踪ID',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `user_type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '用户类型 (admin/user)',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '用户名',
  `request_ip` varchar(45) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '请求IP',
  `request_uri` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '请求URI',
  `http_method` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'HTTP方法',
  `module` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '操作模块',
  `method` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '操作方法',
  `mapper_method` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'MyBatis Mapper方法',
  `sql_statement` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'SQL语句',
  `sql_type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'SQL类型 (SELECT/INSERT/UPDATE/DELETE)',
  `sql_params` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT 'SQL参数',
  `execution_time` bigint NOT NULL COMMENT '执行时间(毫秒)',
  `affected_rows` int NULL DEFAULT 0 COMMENT '影响行数',
  `success` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否成功',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '错误信息',
  `slow_sql` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否慢SQL',
  `execute_time` datetime NOT NULL COMMENT '执行时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_trace_id`(`trace_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_user_type`(`user_type` ASC) USING BTREE,
  INDEX `idx_username`(`username` ASC) USING BTREE,
  INDEX `idx_module`(`module` ASC) USING BTREE,
  INDEX `idx_sql_type`(`sql_type` ASC) USING BTREE,
  INDEX `idx_slow_sql`(`slow_sql` ASC) USING BTREE,
  INDEX `idx_success`(`success` ASC) USING BTREE,
  INDEX `idx_execute_time`(`execute_time` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_execution_time`(`execution_time` ASC) USING BTREE,
  INDEX `idx_mapper_method`(`mapper_method`(100) ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5432 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'SQL监控日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for storage_config
-- ----------------------------
DROP TABLE IF EXISTS `storage_config`;
CREATE TABLE `storage_config`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `storage_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '存储类型: LOCAL,OSS,COS,KODO,OBS',
  `config_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '配置名称',
  `config_params` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '配置参数JSON',
  `is_default` tinyint NOT NULL DEFAULT 0 COMMENT '是否默认存储: 0=否, 1=是',
  `is_enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用: 0=禁用, 1=启用',
  `test_status` tinyint NULL DEFAULT NULL COMMENT '测试状态: 0=失败, 1=成功, NULL=未测试',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_config_name`(`config_name` ASC) USING BTREE,
  INDEX `idx_storage_type`(`storage_type` ASC) USING BTREE,
  INDEX `idx_enabled`(`is_enabled` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '存储配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for study_team
-- ----------------------------
DROP TABLE IF EXISTS `study_team`;
CREATE TABLE `study_team`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '小组ID',
  `team_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '小组名称',
  `team_desc` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '小组简介',
  `team_avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '小组头像',
  `team_type` tinyint NOT NULL DEFAULT 1 COMMENT '类型：1目标型 2学习型 3打卡型',
  `tags` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '标签，逗号分隔',
  `max_members` int NOT NULL DEFAULT 20 COMMENT '最大成员数',
  `current_members` int NOT NULL DEFAULT 1 COMMENT '当前成员数',
  `join_type` tinyint NOT NULL DEFAULT 1 COMMENT '加入方式：1公开 2申请 3邀请',
  `invite_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '邀请码',
  `goal_title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '目标标题',
  `goal_desc` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '目标描述',
  `goal_start_date` date NULL DEFAULT NULL COMMENT '目标开始日期',
  `goal_end_date` date NULL DEFAULT NULL COMMENT '目标结束日期',
  `daily_target` int NULL DEFAULT NULL COMMENT '每日目标量',
  `total_checkins` int NULL DEFAULT 0 COMMENT '总打卡次数',
  `total_discussions` int NULL DEFAULT 0 COMMENT '总讨论数',
  `active_days` int NULL DEFAULT 0 COMMENT '活跃天数',
  `creator_id` bigint NOT NULL COMMENT '创建者ID',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0已解散 1正常 2已满员',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_invite_code`(`invite_code` ASC) USING BTREE,
  INDEX `idx_creator_id`(`creator_id` ASC) USING BTREE,
  INDEX `idx_team_type`(`team_type` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '学习小组表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for study_team_application
-- ----------------------------
DROP TABLE IF EXISTS `study_team_application`;
CREATE TABLE `study_team_application`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '申请ID',
  `team_id` bigint NOT NULL COMMENT '小组ID',
  `user_id` bigint NOT NULL COMMENT '申请人ID',
  `apply_reason` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '申请理由',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0待审核 1已通过 2已拒绝 3已取消',
  `reviewer_id` bigint NULL DEFAULT NULL COMMENT '审核人ID',
  `review_time` datetime NULL DEFAULT NULL COMMENT '审核时间',
  `reject_reason` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '拒绝原因',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_team_id`(`team_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '小组申请表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for study_team_checkin
-- ----------------------------
DROP TABLE IF EXISTS `study_team_checkin`;
CREATE TABLE `study_team_checkin`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '打卡ID',
  `team_id` bigint NOT NULL COMMENT '小组ID',
  `task_id` bigint NOT NULL COMMENT '任务ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `checkin_date` date NOT NULL COMMENT '打卡日期',
  `complete_value` int NOT NULL COMMENT '完成数量',
  `content` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '学习内容',
  `images` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '图片URL，逗号分隔',
  `feeling` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '心得感悟',
  `related_question_ids` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '关联题目ID',
  `is_supplement` tinyint NULL DEFAULT 0 COMMENT '是否补卡',
  `like_count` int NULL DEFAULT 0 COMMENT '点赞数',
  `comment_count` int NULL DEFAULT 0 COMMENT '评论数',
  `duration` int NULL DEFAULT NULL COMMENT '学习时长（分钟）',
  `related_link` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '相关链接',
  `is_deleted` tinyint NULL DEFAULT 0 COMMENT '是否删除：0否 1是',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '打卡时间',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_task_user_date`(`task_id` ASC, `user_id` ASC, `checkin_date` ASC) USING BTREE,
  INDEX `idx_team_id`(`team_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_checkin_date`(`checkin_date` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '小组打卡记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for study_team_checkin_comment
-- ----------------------------
DROP TABLE IF EXISTS `study_team_checkin_comment`;
CREATE TABLE `study_team_checkin_comment`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  `checkin_id` bigint NOT NULL COMMENT '打卡ID',
  `user_id` bigint NOT NULL COMMENT '评论用户ID',
  `parent_id` bigint NULL DEFAULT 0 COMMENT '父评论ID',
  `reply_user_id` bigint NULL DEFAULT NULL COMMENT '回复用户ID',
  `content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '评论内容',
  `like_count` int NULL DEFAULT 0 COMMENT '点赞数',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：0已删除 1正常',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_checkin_id`(`checkin_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_parent_id`(`parent_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '打卡评论表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for study_team_checkin_like
-- ----------------------------
DROP TABLE IF EXISTS `study_team_checkin_like`;
CREATE TABLE `study_team_checkin_like`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '点赞ID',
  `checkin_id` bigint NOT NULL COMMENT '打卡ID',
  `user_id` bigint NOT NULL COMMENT '点赞用户ID',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_checkin_user`(`checkin_id` ASC, `user_id` ASC) USING BTREE,
  INDEX `idx_checkin_id`(`checkin_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '打卡点赞表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for study_team_daily_stats
-- ----------------------------
DROP TABLE IF EXISTS `study_team_daily_stats`;
CREATE TABLE `study_team_daily_stats`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '统计ID',
  `team_id` bigint NOT NULL COMMENT '小组ID',
  `stat_date` date NOT NULL COMMENT '统计日期',
  `checkin_count` int NULL DEFAULT 0 COMMENT '打卡人数',
  `member_count` int NULL DEFAULT 0 COMMENT '当日成员数',
  `checkin_rate` decimal(5, 2) NULL DEFAULT 0.00 COMMENT '打卡率',
  `discussion_count` int NULL DEFAULT 0 COMMENT '讨论数',
  `new_member_count` int NULL DEFAULT 0 COMMENT '新加入成员',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_team_date`(`team_id` ASC, `stat_date` ASC) USING BTREE,
  INDEX `idx_team_id`(`team_id` ASC) USING BTREE,
  INDEX `idx_stat_date`(`stat_date` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '小组每日统计表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for study_team_discussion
-- ----------------------------
DROP TABLE IF EXISTS `study_team_discussion`;
CREATE TABLE `study_team_discussion`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '讨论ID',
  `team_id` bigint NOT NULL COMMENT '小组ID',
  `user_id` bigint NOT NULL COMMENT '发布者ID',
  `category` tinyint NOT NULL DEFAULT 5 COMMENT '分类：1公告 2问题 3笔记 4经验 5闲聊',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '内容',
  `images` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '图片URL',
  `is_pinned` tinyint NULL DEFAULT 0 COMMENT '是否置顶',
  `is_essence` tinyint NULL DEFAULT 0 COMMENT '是否精华',
  `view_count` int NULL DEFAULT 0 COMMENT '浏览数',
  `like_count` int NULL DEFAULT 0 COMMENT '点赞数',
  `comment_count` int NULL DEFAULT 0 COMMENT '评论数',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：0已删除 1正常',
  `is_deleted` tinyint NULL DEFAULT 0 COMMENT '是否删除：0否 1是',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `last_reply_time` datetime NULL DEFAULT NULL COMMENT '最后回复时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_team_id`(`team_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_category`(`category` ASC) USING BTREE,
  INDEX `idx_is_pinned`(`is_pinned` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '小组讨论表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for study_team_member
-- ----------------------------
DROP TABLE IF EXISTS `study_team_member`;
CREATE TABLE `study_team_member`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `team_id` bigint NOT NULL COMMENT '小组ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role` tinyint NOT NULL DEFAULT 3 COMMENT '角色：1组长 2管理员 3成员',
  `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '组内昵称',
  `join_reason` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '加入理由',
  `total_checkins` int NULL DEFAULT 0 COMMENT '总打卡次数',
  `current_streak` int NULL DEFAULT 0 COMMENT '当前连续打卡',
  `max_streak` int NULL DEFAULT 0 COMMENT '最长连续打卡',
  `total_likes_received` int NULL DEFAULT 0 COMMENT '获得点赞数',
  `contribution_points` int NULL DEFAULT 0 COMMENT '贡献积分',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0已退出 1正常 2禁言中',
  `mute_end_time` datetime NULL DEFAULT NULL COMMENT '禁言结束时间',
  `join_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  `last_active_time` datetime NULL DEFAULT NULL COMMENT '最后活跃时间',
  `last_checkin_time` datetime NULL DEFAULT NULL COMMENT '最后打卡时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_team_user`(`team_id` ASC, `user_id` ASC) USING BTREE,
  INDEX `idx_team_id`(`team_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_role`(`role` ASC) USING BTREE,
  INDEX `idx_join_time`(`join_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '小组成员表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for study_team_task
-- ----------------------------
DROP TABLE IF EXISTS `study_team_task`;
CREATE TABLE `study_team_task`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `team_id` bigint NOT NULL COMMENT '小组ID',
  `task_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '任务名称',
  `task_desc` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '任务描述',
  `task_type` tinyint NOT NULL DEFAULT 1 COMMENT '类型：1刷题 2学习时长 3阅读 4自定义',
  `target_value` int NOT NULL COMMENT '目标数量',
  `target_unit` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '目标单位',
  `repeat_type` tinyint NOT NULL DEFAULT 1 COMMENT '重复：1每日 2工作日 3自定义',
  `repeat_days` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '自定义重复日',
  `require_content` tinyint NULL DEFAULT 0 COMMENT '是否必须附带内容',
  `require_image` tinyint NULL DEFAULT 0 COMMENT '是否必须附带图片',
  `start_date` date NOT NULL COMMENT '开始日期',
  `end_date` date NULL DEFAULT NULL COMMENT '结束日期',
  `creator_id` bigint NOT NULL COMMENT '创建人 ID',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0已结束 1进行中',
  `is_deleted` tinyint NULL DEFAULT 0 COMMENT '是否删除：0否 1是',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_team_id`(`team_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_start_date`(`start_date` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '小组打卡任务表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_admin
-- ----------------------------
DROP TABLE IF EXISTS `sys_admin`;
CREATE TABLE `sys_admin`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '管理员ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码（加密后）',
  `real_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '真实姓名',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '手机号',
  `avatar` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '头像URL',
  `gender` tinyint NULL DEFAULT 0 COMMENT '性别：0-未知，1-男，2-女',
  `status` tinyint NULL DEFAULT 0 COMMENT '状态：0-正常，1-禁用，2-删除',
  `last_login_time` datetime NULL DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '最后登录IP',
  `login_count` int NULL DEFAULT 0 COMMENT '登录次数',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_username`(`username` ASC) USING BTREE,
  UNIQUE INDEX `uk_email`(`email` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '管理员用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_admin_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_admin_role`;
CREATE TABLE `sys_admin_role`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `admin_id` bigint NOT NULL COMMENT '管理员ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_admin_role`(`admin_id` ASC, `role_id` ASC) USING BTREE,
  INDEX `idx_admin_id`(`admin_id` ASC) USING BTREE,
  INDEX `idx_role_id`(`role_id` ASC) USING BTREE,
  CONSTRAINT `fk_admin_role_admin` FOREIGN KEY (`admin_id`) REFERENCES `sys_admin` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_admin_role_role` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '管理员角色关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_login_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_login_log`;
CREATE TABLE `sys_login_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `admin_id` bigint NULL DEFAULT NULL COMMENT '管理员ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '用户名',
  `login_ip` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '登录IP',
  `login_location` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '登录地点',
  `browser` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '浏览器',
  `os` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '操作系统',
  `login_status` tinyint NULL DEFAULT 0 COMMENT '登录状态：0-成功，1-失败',
  `login_message` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '登录消息',
  `login_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_admin_id`(`admin_id` ASC) USING BTREE,
  INDEX `idx_login_time`(`login_time` ASC) USING BTREE,
  INDEX `idx_login_status`(`login_status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 58 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '登录日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_operation_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_operation_log`;
CREATE TABLE `sys_operation_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `operation_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '操作ID',
  `module` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '操作模块',
  `operation_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'OTHER' COMMENT '操作类型',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '操作描述',
  `method` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '请求方法',
  `request_uri` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '请求URI',
  `request_method` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'GET' COMMENT 'HTTP请求方法',
  `request_params` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '请求参数',
  `response_data` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '响应数据',
  `operator_id` bigint NULL DEFAULT NULL COMMENT '操作人ID',
  `operator_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '操作人姓名',
  `operator_ip` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '操作IP',
  `operation_location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '操作地点',
  `browser` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '浏览器类型',
  `os` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '操作系统',
  `status` tinyint(1) NULL DEFAULT 0 COMMENT '操作状态：0-成功，1-失败',
  `error_msg` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '错误消息',
  `operation_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `cost_time` bigint NULL DEFAULT 0 COMMENT '耗时（毫秒）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_operation_id`(`operation_id` ASC) USING BTREE,
  INDEX `idx_operator_id`(`operator_id` ASC) USING BTREE,
  INDEX `idx_module`(`module` ASC) USING BTREE,
  INDEX `idx_operation_type`(`operation_type` ASC) USING BTREE,
  INDEX `idx_operation_time`(`operation_time` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 658 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '操作日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_permission
-- ----------------------------
DROP TABLE IF EXISTS `sys_permission`;
CREATE TABLE `sys_permission`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '权限ID',
  `parent_id` bigint NULL DEFAULT 0 COMMENT '父权限ID，0表示顶级权限',
  `permission_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '权限名称',
  `permission_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '权限编码',
  `permission_type` tinyint NULL DEFAULT 0 COMMENT '权限类型：0-菜单，1-按钮，2-接口',
  `path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '路由路径',
  `component` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '组件路径',
  `icon` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '图标',
  `sort_order` int NULL DEFAULT 0 COMMENT '排序',
  `status` tinyint NULL DEFAULT 0 COMMENT '状态：0-正常，1-禁用',
  `description` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '权限描述',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_permission_code`(`permission_code` ASC) USING BTREE,
  INDEX `idx_parent_id`(`parent_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 135 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '权限表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色名称',
  `role_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色编码',
  `description` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '角色描述',
  `status` tinyint NULL DEFAULT 0 COMMENT '状态：0-正常，1-禁用',
  `sort_order` int NULL DEFAULT 0 COMMENT '排序',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_role_code`(`role_code` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '角色表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_role_permission
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_permission`;
CREATE TABLE `sys_role_permission`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `permission_id` bigint NOT NULL COMMENT '权限ID',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_role_permission`(`role_id` ASC, `permission_id` ASC) USING BTREE,
  INDEX `idx_role_id`(`role_id` ASC) USING BTREE,
  INDEX `idx_permission_id`(`permission_id` ASC) USING BTREE,
  CONSTRAINT `fk_role_permission_permission` FOREIGN KEY (`permission_id`) REFERENCES `sys_permission` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_role_permission_role` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 52 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '角色权限关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user_bug_history
-- ----------------------------
DROP TABLE IF EXISTS `user_bug_history`;
CREATE TABLE `user_bug_history`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '历史记录ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `bug_id` bigint NOT NULL COMMENT 'Bug ID',
  `view_time` datetime NOT NULL COMMENT '浏览时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_bug`(`user_id` ASC, `bug_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_bug_id`(`bug_id` ASC) USING BTREE,
  INDEX `idx_view_time`(`view_time` ASC) USING BTREE,
  INDEX `idx_user_view_time`(`user_id` ASC, `view_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户Bug浏览历史表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user_calendar_collection
-- ----------------------------
DROP TABLE IF EXISTS `user_calendar_collection`;
CREATE TABLE `user_calendar_collection`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `collection_type` tinyint NOT NULL COMMENT '收藏类型：1-事件，2-内容',
  `target_id` bigint NOT NULL COMMENT '目标ID（事件ID或内容ID）',
  `collection_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0-取消收藏，1-已收藏',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_type_target`(`user_id` ASC, `collection_type` ASC, `target_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_collection_type`(`collection_type` ASC) USING BTREE,
  INDEX `idx_target_id`(`target_id` ASC) USING BTREE,
  CONSTRAINT `fk_user_calendar_collection_user_id` FOREIGN KEY (`user_id`) REFERENCES `user_info` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户日历收藏表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user_calendar_preference
-- ----------------------------
DROP TABLE IF EXISTS `user_calendar_preference`;
CREATE TABLE `user_calendar_preference`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '偏好ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `event_reminder` tinyint NOT NULL DEFAULT 1 COMMENT '事件提醒：0-关闭，1-开启',
  `daily_content_push` tinyint NOT NULL DEFAULT 1 COMMENT '每日内容推送：0-关闭，1-开启',
  `preferred_languages` json NULL COMMENT '偏好编程语言数组JSON格式',
  `preferred_content_types` json NULL COMMENT '偏好内容类型数组JSON格式',
  `difficulty_preference` tinyint NULL DEFAULT 2 COMMENT '难度偏好：1-初级，2-中级，3-高级',
  `notification_time` time NULL DEFAULT '09:00:00' COMMENT '通知时间',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  CONSTRAINT `fk_user_calendar_preference_user_id` FOREIGN KEY (`user_id`) REFERENCES `user_info` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户日历偏好设置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user_checkin_bitmap
-- ----------------------------
DROP TABLE IF EXISTS `user_checkin_bitmap`;
CREATE TABLE `user_checkin_bitmap`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `year_month` varchar(7) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '年月格式YYYY-MM',
  `checkin_bitmap` bigint NULL DEFAULT 0 COMMENT '打卡位图每位代表当月某天',
  `continuous_days` int NULL DEFAULT 0 COMMENT '当前连续打卡天数',
  `last_checkin_date` date NULL DEFAULT NULL COMMENT '最后打卡日期',
  `total_checkin_days` int NULL DEFAULT 0 COMMENT '当月总打卡天数',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_year_month`(`user_id` ASC, `year_month` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_last_checkin`(`last_checkin_date` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户打卡位图表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user_info
-- ----------------------------
DROP TABLE IF EXISTS `user_info`;
CREATE TABLE `user_info`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码（加密后）',
  `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '昵称',
  `real_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '真实姓名',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '手机号',
  `avatar` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '头像URL',
  `gender` tinyint NULL DEFAULT 0 COMMENT '性别：0-未知，1-男，2-女',
  `birthday` date NULL DEFAULT NULL COMMENT '生日',
  `status` tinyint NULL DEFAULT 0 COMMENT '状态：0-正常，1-禁用，2-删除',
  `last_login_time` datetime NULL DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '最后登录IP',
  `register_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_username`(`username` ASC) USING BTREE,
  UNIQUE INDEX `uk_email`(`email` ASC) USING BTREE,
  UNIQUE INDEX `uk_phone`(`phone` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_register_time`(`register_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user_lottery_limit
-- ----------------------------
DROP TABLE IF EXISTS `user_lottery_limit`;
CREATE TABLE `user_lottery_limit`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `today_draw_count` int NULL DEFAULT 0 COMMENT '今日抽奖次数',
  `week_draw_count` int NULL DEFAULT 0 COMMENT '本周抽奖次数',
  `month_draw_count` int NULL DEFAULT 0 COMMENT '本月抽奖次数',
  `total_draw_count` int NULL DEFAULT 0 COMMENT '总抽奖次数',
  `today_win_count` int NULL DEFAULT 0 COMMENT '今日中奖次数',
  `total_win_count` int NULL DEFAULT 0 COMMENT '总中奖次数',
  `max_continuous_no_win` int NULL DEFAULT 0 COMMENT '最大连续未中奖次数',
  `current_continuous_no_win` int NULL DEFAULT 0 COMMENT '当前连续未中奖次数',
  `last_draw_time` datetime NULL DEFAULT NULL COMMENT '最后抽奖时间',
  `last_win_time` datetime NULL DEFAULT NULL COMMENT '最后中奖时间',
  `is_blacklist` tinyint NULL DEFAULT 0 COMMENT '是否黑名单：0-否 1-是',
  `risk_level` tinyint NULL DEFAULT 0 COMMENT '风险等级：0-正常 1-低风险 2-中风险 3-高风险',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_blacklist`(`is_blacklist` ASC) USING BTREE,
  INDEX `idx_risk_level`(`risk_level` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户抽奖限制表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user_plan
-- ----------------------------
DROP TABLE IF EXISTS `user_plan`;
CREATE TABLE `user_plan`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '计划ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `plan_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '计划名称',
  `plan_desc` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '计划描述',
  `plan_type` tinyint NOT NULL COMMENT '计划类型：1-刷题 2-学习 3-阅读 4-运动 5-自定义',
  `target_type` tinyint NOT NULL DEFAULT 1 COMMENT '目标类型：1-数量 2-时长 3-次数',
  `target_value` int NOT NULL DEFAULT 1 COMMENT '目标值',
  `target_unit` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '次' COMMENT '目标单位（道/小时/次）',
  `start_date` date NOT NULL COMMENT '开始日期',
  `end_date` date NULL DEFAULT NULL COMMENT '结束日期（NULL表示长期）',
  `daily_start_time` time NULL DEFAULT NULL COMMENT '每日开始时间',
  `daily_end_time` time NULL DEFAULT NULL COMMENT '每日截止时间',
  `repeat_type` tinyint NOT NULL DEFAULT 1 COMMENT '重复类型：1-每日 2-工作日 3-周末 4-自定义',
  `repeat_days` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '自定义重复日（如：1,2,3,4,5 表示周一到周五）',
  `remind_before` int NULL DEFAULT 30 COMMENT '提前提醒分钟数',
  `remind_deadline` int NULL DEFAULT 10 COMMENT '截止提醒分钟数',
  `remind_enabled` tinyint NULL DEFAULT 1 COMMENT '是否启用提醒：0-否 1-是',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0-已删除 1-进行中 2-已暂停 3-已完成 4-已过期',
  `total_checkin_days` int NULL DEFAULT 0 COMMENT '累计打卡天数',
  `current_streak` int NULL DEFAULT 0 COMMENT '当前连续打卡天数',
  `max_streak` int NULL DEFAULT 0 COMMENT '最长连续打卡天数',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_user_status`(`user_id` ASC, `status` ASC) USING BTREE,
  INDEX `idx_start_date`(`start_date` ASC) USING BTREE,
  INDEX `idx_end_date`(`end_date` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户计划表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user_points_balance
-- ----------------------------
DROP TABLE IF EXISTS `user_points_balance`;
CREATE TABLE `user_points_balance`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `total_points` int NULL DEFAULT 0 COMMENT '总积分余额',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_points_desc`(`total_points` DESC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户积分余额表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user_points_detail
-- ----------------------------
DROP TABLE IF EXISTS `user_points_detail`;
CREATE TABLE `user_points_detail`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `points_change` int NOT NULL COMMENT '积分变动数量（正数为增加）',
  `points_type` tinyint NOT NULL COMMENT '积分类型：1-后台发放 2-打卡积分',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '变动描述/原因',
  `balance_after` int NOT NULL COMMENT '变动后余额',
  `admin_id` bigint NULL DEFAULT NULL COMMENT '操作管理员ID（后台发放时记录）',
  `continuous_days` int NULL DEFAULT NULL COMMENT '连续打卡天数（打卡积分时记录）',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` DESC) USING BTREE,
  INDEX `idx_user_time`(`user_id` ASC, `create_time` DESC) USING BTREE,
  INDEX `idx_points_type`(`points_type` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 31 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '积分明细表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user_salary_config
-- ----------------------------
DROP TABLE IF EXISTS `user_salary_config`;
CREATE TABLE `user_salary_config`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `monthly_salary` decimal(10, 2) NOT NULL COMMENT '月薪（元）',
  `work_days_per_month` tinyint NOT NULL DEFAULT 22 COMMENT '每月工作天数',
  `work_hours_per_day` decimal(4, 2) NOT NULL DEFAULT 8.00 COMMENT '每日工作小时数',
  `hourly_rate` decimal(10, 2) GENERATED ALWAYS AS ((`monthly_salary` / (`work_days_per_month` * `work_hours_per_day`))) STORED COMMENT '时薪（自动计算）' NULL,
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  CONSTRAINT `fk_user_salary_config_user_id` FOREIGN KEY (`user_id`) REFERENCES `user_info` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户薪资配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for version_history
-- ----------------------------
DROP TABLE IF EXISTS `version_history`;
CREATE TABLE `version_history`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `version_number` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '版本号',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '更新标题',
  `update_type` tinyint NOT NULL DEFAULT 1 COMMENT '更新类型: 1-重大更新, 2-功能更新, 3-修复更新, 4-其他',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '版本更新简要描述',
  `prd_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'PRD文档链接',
  `release_time` datetime NOT NULL COMMENT '发布时间',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态: 0-草稿, 1-已发布, 2-已隐藏',
  `sort_order` int NULL DEFAULT 0 COMMENT '排序权重(数字越大越靠前)',
  `view_count` int NULL DEFAULT 0 COMMENT '查看次数',
  `is_featured` tinyint NULL DEFAULT 0 COMMENT '是否重点推荐: 0-否, 1-是',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  `created_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint NULL DEFAULT NULL COMMENT '更新人ID',
  `updated_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_version_number`(`version_number` ASC, `deleted` ASC) USING BTREE,
  INDEX `idx_version_number`(`version_number` ASC) USING BTREE,
  INDEX `idx_release_time`(`release_time` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_update_type`(`update_type` ASC) USING BTREE,
  INDEX `idx_sort_order`(`sort_order` ASC) USING BTREE,
  INDEX `idx_created_time`(`created_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 16 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '版本更新历史表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for work_record
-- ----------------------------
DROP TABLE IF EXISTS `work_record`;
CREATE TABLE `work_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `work_date` date NOT NULL COMMENT '工作日期',
  `work_hours` decimal(4, 2) NOT NULL DEFAULT 0.00 COMMENT '工作小时数',
  `daily_earnings` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '当日收入（元）',
  `start_time` datetime NULL DEFAULT NULL COMMENT '开始工作时间',
  `end_time` datetime NULL DEFAULT NULL COMMENT '结束工作时间',
  `pause_start_time` datetime NULL DEFAULT NULL COMMENT '暂停开始时间',
  `total_pause_minutes` int NULL DEFAULT 0 COMMENT '累计暂停时长（分钟）',
  `work_status` tinyint NOT NULL DEFAULT 0 COMMENT '工作状态：0-未开始，1-进行中，2-暂停中，3-已完成',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0-无效，1-有效',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_date`(`user_id` ASC, `work_date` ASC) USING BTREE,
  INDEX `idx_work_date`(`work_date` ASC) USING BTREE,
  INDEX `idx_user_id_work_date`(`user_id` ASC, `work_date` ASC) USING BTREE,
  INDEX `idx_work_status`(`work_status` ASC) USING BTREE,
  CONSTRAINT `fk_work_record_user_id` FOREIGN KEY (`user_id`) REFERENCES `user_info` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '工作记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- View structure for v_user_unread_notifications
-- ----------------------------
DROP VIEW IF EXISTS `v_user_unread_notifications`;
CREATE ALGORITHM = UNDEFINED SQL SECURITY DEFINER VIEW `v_user_unread_notifications` AS select `n`.`id` AS `id`,`n`.`title` AS `title`,`n`.`content` AS `content`,`n`.`type` AS `type`,`n`.`priority` AS `priority`,`n`.`sender_id` AS `sender_id`,`n`.`receiver_id` AS `receiver_id`,`n`.`source_module` AS `source_module`,`n`.`source_id` AS `source_id`,`n`.`status` AS `status`,`n`.`read_time` AS `read_time`,`n`.`created_time` AS `created_time`,`n`.`updated_time` AS `updated_time` from (`notification` `n` left join `notification_user_read_record` `r` on((`n`.`id` = `r`.`notification_id`))) where ((`n`.`status` <> 'DELETED') and (((`n`.`receiver_id` is not null) and (`n`.`status` = 'UNREAD')) or ((`n`.`receiver_id` is null) and (`r`.`id` is null))));

SET FOREIGN_KEY_CHECKS = 1;
