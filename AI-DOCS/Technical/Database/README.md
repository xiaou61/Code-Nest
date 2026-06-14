# 数据库文档

## 目录说明

本目录存放数据库设计文档，包括表结构、索引设计、数据字典等。

## 数据库概述

- **数据库类型**: MySQL 8.0
- **字符集**: utf8mb4
- **排序规则**: utf8mb4_unicode_ci
- **存储引擎**: InnoDB
- **数据库名**: code_nest

## 表结构清单

### 1. 用户相关表

| 表名 | 说明 | 主要字段 |
|------|------|---------|
| user_info | 用户信息表 | id, username, nickname, avatar, email, phone |
| user_auth | 用户认证表 | id, user_id, password, salt |
| user_role | 用户角色表 | id, user_id, role_id |

### 2. 博客模块表

| 表名 | 说明 | 主要字段 |
|------|------|---------|
| blog_article | 博客文章表 | id, user_id, title, content, status |
| blog_category | 文章分类表 | id, category_name, sort_order |
| blog_tag | 文章标签表 | id, tag_name |
| blog_comment | 文章评论表 | id, article_id, user_id, content |
| blog_config | 博客配置表 | id, user_id, blog_name |

### 3. 社区模块表

| 表名 | 说明 | 主要字段 |
|------|------|---------|
| community_post | 社区帖子表 | id, user_id, title, content, status |
| community_comment | 帖子评论表 | id, post_id, user_id, content |
| community_category | 帖子分类表 | id, category_name |
| community_tag | 帖子标签表 | id, tag_name |
| community_post_tag | 帖子标签关联表 | post_id, tag_id |

### 4. 面试模块表

| 表名 | 说明 | 主要字段 |
|------|------|---------|
| interview_question | 面试题表 | id, title, content, category_id |
| interview_category | 题目分类表 | id, category_name |
| interview_question_set | 题单表 | id, name, description |
| interview_learn_record | 学习记录表 | id, user_id, question_id, status |

### 5. OJ模块表

| 表名 | 说明 | 主要字段 |
|------|------|---------|
| oj_problem | OJ题目表 | id, title, difficulty, time_limit |
| oj_contest | OJ竞赛表 | id, title, start_time, end_time |
| oj_submission | 提交记录表 | id, user_id, problem_id, status |
| oj_test_case | 测试用例表 | id, problem_id, input, output |

### 6. 积分模块表

| 表名 | 说明 | 主要字段 |
|------|------|---------|
| points_account | 积分账户表 | id, user_id, balance |
| points_record | 积分记录表 | id, user_id, points, type |
| points_sign_in | 签到记录表 | id, user_id, sign_date |

### 7. 抽奖模块表

| 表名 | 说明 | 主要字段 |
|------|------|---------|
| lottery_prize | 奖品表 | id, name, points, probability |
| lottery_record | 抽奖记录表 | id, user_id, prize_id |
| lottery_pool | 奖池表 | id, name, status |

### 8. 聊天模块表

| 表名 | 说明 | 主要字段 |
|------|------|---------|
| chat_message | 聊天消息表 | id, sender_id, content, type |
| chat_room | 聊天房间表 | id, name, type |
| chat_room_member | 房间成员表 | room_id, user_id |

### 9. 知识图谱表

| 表名 | 说明 | 主要字段 |
|------|------|---------|
| knowledge_map | 知识图谱表 | id, name, description |
| knowledge_node | 知识节点表 | id, map_id, content |
| knowledge_edge | 知识边表 | id, source_id, target_id |

### 10. 简历模块表

| 表名 | 说明 | 主要字段 |
|------|------|---------|
| resume_template | 简历模板表 | id, name, content |
| resume_user | 用户简历表 | id, user_id, template_id |
| resume_section | 简历章节表 | id, resume_id, type |

### 11. 计划模块表

| 表名 | 说明 | 主要字段 |
|------|------|---------|
| plan_task | 计划任务表 | id, user_id, title, status |
| plan_check_in | 打卡记录表 | id, task_id, check_date |
| plan_template | 计划模板表 | id, name, content |

### 12. 团队模块表

| 表名 | 说明 | 主要字段 |
|------|------|---------|
| study_team | 学习小组表 | id, name, description |
| team_member | 小组成员表 | team_id, user_id, role |
| team_discussion | 小组讨论表 | id, team_id, user_id, content |

### 13. 闪卡模块表

| 表名 | 说明 | 主要字段 |
|------|------|---------|
| flashcard_deck | 闪卡牌组表 | id, user_id, name |
| flashcard_card | 闪卡卡片表 | id, deck_id, front, back |
| flashcard_study | 学习记录表 | id, user_id, card_id, status |

### 14. 代码工坊表

| 表名 | 说明 | 主要字段 |
|------|------|---------|
| codepen_pen | 代码片段表 | id, user_id, title, html, css, js |
| codepen_tag | 标签表 | id, tag_name |
| codepen_comment | 评论表 | id, pen_id, user_id, content |

### 15. 摸鱼模块表

| 表名 | 说明 | 主要字段 |
|------|------|---------|
| moyu_bug_store | Bug仓库表 | id, title, content |
| moyu_calendar | 日历事件表 | id, user_id, title, event_date |
| moyu_salary | 薪资计算器表 | id, user_id, salary_data |

### 16. 文件存储表

| 表名 | 说明 | 主要字段 |
|------|------|---------|
| file_storage | 文件存储表 | id, filename, path, size |
| file_config | 存储配置表 | id, storage_type, config |

### 17. 敏感词表

| 表名 | 说明 | 主要字段 |
|------|------|---------|
| sensitive_word | 敏感词表 | id, word, category |
| sensitive_strategy | 敏感策略表 | id, name, action |
| sensitive_whitelist | 白名单表 | id, word |

### 18. 系统管理表

| 表名 | 说明 | 主要字段 |
|------|------|---------|
| sys_admin | 管理员表 | id, username, password |
| sys_role | 角色表 | id, role_name, permissions |
| sys_menu | 菜单表 | id, name, path, parent_id |
| sys_log | 系统日志表 | id, user_id, operation, ip |

### 19. 版本管理表

| 表名 | 说明 | 主要字段 |
|------|------|---------|
| version_history | 版本历史表 | id, version, content |
| version_config | 版本配置表 | id, key, value |

### 20. 通知模块表

| 表名 | 说明 | 主要字段 |
|------|------|---------|
| notification_message | 通知消息表 | id, user_id, title, content |
| notification_config | 通知配置表 | id, user_id, type, enabled |

## 索引设计规范

### 1. 主键索引

- 每个表必须有主键
- 主键使用自增ID
- 主键字段名统一为 `id`

### 2. 唯一索引

- 业务唯一字段需要创建唯一索引
- 唯一索引命名: `uk_{字段名}`
- 示例: `uk_username`, `uk_email`

### 3. 普通索引

- 查询频繁的字段需要创建索引
- 索引命名: `idx_{字段名}`
- 示例: `idx_user_id`, `idx_create_time`

### 4. 组合索引

- 多字段联合查询需要创建组合索引
- 组合索引命名: `idx_{字段1}_{字段2}`
- 示例: `idx_user_id_status`

### 5. 全文索引

- 需要全文搜索的字段创建全文索引
- 全文索引命名: `ft_{字段名}`
- 使用 ngram 解析器支持中文

## 数据字典

### 1. 通用字段

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | bigint | 主键ID |
| create_time | datetime | 创建时间 |
| update_time | datetime | 更新时间 |
| status | tinyint | 状态：0-禁用 1-启用 |
| is_deleted | tinyint | 是否删除：0-未删除 1-已删除 |

### 2. 用户相关字段

| 字段名 | 类型 | 说明 |
|--------|------|------|
| user_id | bigint | 用户ID |
| username | varchar(50) | 用户名 |
| nickname | varchar(50) | 昵称 |
| avatar | varchar(255) | 头像URL |
| email | varchar(100) | 邮箱 |
| phone | varchar(20) | 手机号 |

### 3. 内容相关字段

| 字段名 | 类型 | 说明 |
|--------|------|------|
| title | varchar(200) | 标题 |
| content | longtext | 内容 |
| summary | varchar(500) | 摘要 |
| cover_image | varchar(255) | 封面图 |
| tags | varchar(200) | 标签，JSON格式 |

### 4. 状态字段

| 字段名 | 类型 | 说明 |
|--------|------|------|
| status | tinyint | 状态 |
| type | tinyint | 类型 |
| level | tinyint | 等级 |
| priority | tinyint | 优先级 |

## 版本历史

| 版本 | 日期 | 更新内容 | 作者 |
|------|------|---------|------|
| v1.0.0 | 2026-05-29 | 初始版本 | AI Assistant |
