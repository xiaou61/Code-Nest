# 数据表索引

主库基线脚本位于 `sql/MySql/code_nest.sql`。版本增量脚本位于 `sql/v* 目录`，用于追溯模块从 v1.x 到 v2.x 的演进。

如果你已经定位到具体表，想理解 `status`、`user_id`、`is_deleted`、`file_id`、积分、通知、OJ、AI 等字段怎么读，继续看 [数据库字段阅读指南](/reference/database-field-guide)。

## 表统计总览

基线脚本包含 136 张表，加上增量脚本后共 142 张表，按业务域分布：

| 业务域 | 表数量 | 模块 | 表前缀 |
| --- | --- | --- | --- |
| 账号与权限 | 8 | xiaou-user, xiaou-system | `user_info`, `sys_*` |
| 面试题库 | 8 | xiaou-interview | `interview_*` |
| 模拟面试与求职 | 9 | xiaou-mock-interview | `mock_interview_*`, `career_loop_*`, `job_battle_*` |
| 成长与计划 | 7 | xiaou-plan | `growth_autopilot_*`, `user_plan`, `plan_*`, `learning_cockpit_*` |
| 学习小组 | 11 | xiaou-team | `study_team_*` |
| 闪卡 | 8 | xiaou-flashcard | `flashcard_*` |
| 学习资产与知识图谱 | 6 | xiaou-learning-asset, xiaou-knowledge, xiaou-sql-optimizer | `learning_asset_*`, `knowledge_*`, `sql_*` |
| OJ | 11 | xiaou-oj | `oj_*` |
| 社区 | 10 | xiaou-community | `community_*` |
| 动态 | 5 | xiaou-moment | `moment*` |
| 博客 | 4 | xiaou-blog | `blog_*` |
| 代码工坊 | 8 | xiaou-codepen | `code_pen_*` |
| 简历 | 6 | xiaou-resume | `resume_*` |
| 文件存储 | 6 | xiaou-filestorage | `file_*`, `storage_config` |
| 通知 | 4 | xiaou-notification | `notification_*` |
| 聊天 | 5 | xiaou-chat | `chat_*` |
| 积分与抽奖 | 7 | xiaou-points | `user_points_*`, `user_checkin_*`, `lottery_*`, `user_lottery_*` |
| 敏感词 | 11 | xiaou-sensitive | `sensitive_*` |
| 摸鱼工具 | 7 | xiaou-moyu | `developer_calendar_*`, `daily_content`, `user_calendar_*`, `user_salary_*`, `work_record`, `bug_*`, `user_bug_*` |
| 版本历史 | 1 | xiaou-version | `version_history` |

## 索引策略

项目使用 MyBatis-Plus，主键索引由 `BIGINT AUTO_INCREMENT` 自动创建。额外索引策略：

| 索引类型 | 适用场景 | 典型表 |
| --- | --- | --- |
| `user_id` 单列索引 | 所有含 `user_id` 的表，用于按用户查询 | 几乎所有业务表 |
| `user_id + question_id` 联合唯一索引 | 一对一关系表，防止重复记录 | `interview_mastery_record`, `interview_favorite` |
| `user_id + moment_id` 联合唯一索引 | 互动关系表，防止重复点赞/收藏 | `moment_likes`, `moment_favorites` |
| `user_id + post_id` 联合唯一索引 | 社区互动关系表 | `community_post_like`, `community_post_collect` |
| `status` 单列索引 | 状态筛选（已发布/草稿/删除） | `community_post`, `blog_article`, `code_pen`, `version_history` |
| `category_id` 单列索引 | 分类筛选 | `community_post`, `interview_question_set` |
| `created_time` 单列索引 | 时间范围查询和排序 | `chat_messages`, `sys_operation_log`, `moment_views` |
| `room_id` 单列索引 | 聊天室消息查询 | `chat_messages` |
| `problem_id` 单列索引 | OJ 提交和测试用例查询 | `oj_submission`, `oj_test_case` |

> **新增表时**：至少为 `user_id`、`status`、`created_time` 三个字段创建索引（如果表中有这些字段）。互动关系表（点赞、收藏等）必须为 `user_id + 目标_id` 创建联合唯一索引。

## 使用方式

| 任务 | 推荐入口 |
| --- | --- |
| 新环境初始化 | 先执行 `sql/MySql/code_nest.sql` |
| 查看模块历史变更 | 读取对应 `sql/v*/*.sql` |
| 新功能加表 | 先加版本脚本，再同步主库基线 |
| 写模块文档 | 本页定位表，再回到对应 Mapper XML 和 Service |
| 查看表结构 | `DESCRIBE table_name` 或 `SHOW CREATE TABLE table_name` |
| 查看表数据量 | `SELECT COUNT(*) FROM table_name` |
| 查看索引 | `SHOW INDEX FROM table_name` |

## 通用字段约定

所有表遵循以下通用字段约定：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | BIGINT AUTO_INCREMENT | 主键 |
| `create_time` | DATETIME | 创建时间，默认 CURRENT_TIMESTAMP |
| `update_time` | DATETIME | 更新时间，ON UPDATE CURRENT_TIMESTAMP |
| `is_deleted` | TINYINT(1) | 逻辑删除标记：0=正常，1=删除 |
| `user_id` | BIGINT | 关联 `user_info.id` |

> **注意**：部分表使用 `create_by` / `update_by` 替代 `user_id`，语义相同。

## 账号、权限与日志

| 业务域 | 表 | 关键字段 | Mapper 模块 |
| --- | --- | --- | --- |
| 用户账户 | `user_info` | username, email, avatar_url, status, role | xiaou-user |
| 管理员 | `sys_admin` | username, password, role_id, status | xiaou-system |
| 角色 | `sys_role` | name, code, description | xiaou-system |
| 权限 | `sys_permission` | name, code, type, parent_id | xiaou-system |
| 管理员-角色 | `sys_admin_role` | admin_id, role_id | xiaou-system |
| 角色-权限 | `sys_role_permission` | role_id, permission_id | xiaou-system |
| 登录日志 | `sys_login_log` | username, ip, location, browser, status | xiaou-system |
| 操作日志 | `sys_operation_log` | module, operation, method, params, user_id | xiaou-system |

## 学习成长

### 面试题库

| 表 | 关键字段 | Mapper 模块 |
| --- | --- | --- |
| `interview_category` | name, parent_id, sort_order | xiaou-interview |
| `interview_question_set` | title, category_id, difficulty, question_count | xiaou-interview |
| `interview_question` | title, content, answer, set_id, difficulty, type | xiaou-interview |
| `interview_learn_record` | user_id, question_id, status, score | xiaou-interview |
| `interview_favorite` | user_id, question_id | xiaou-interview |
| `interview_mastery_record` | user_id, question_id, mastery_level | xiaou-interview |
| `interview_mastery_history` | mastery_id, old_level, new_level | xiaou-interview |
| `interview_daily_stats` | user_id, date, questions_count, correct_count | xiaou-interview |

### AI 模拟面试与求职

| 表 | 关键字段 | Mapper 模块 |
| --- | --- | --- |
| `mock_interview_direction` | name, description, sort_order | xiaou-mock-interview |
| `mock_interview_session` | user_id, direction, level, status, total_score | xiaou-mock-interview |
| `mock_interview_qa` | session_id, question, answer, score, follow_up | xiaou-mock-interview |
| `mock_interview_user_stats` | user_id, direction, total_sessions, avg_score | xiaou-mock-interview |
| `career_loop_session` | user_id, company, role, status | xiaou-mock-interview |
| `career_loop_stage_log` | session_id, stage, action, result | xiaou-mock-interview |
| `career_loop_action` | session_id, title, type, deadline, status | xiaou-mock-interview |
| `career_loop_snapshot` | session_id, stage, snapshot_json | xiaou-mock-interview |
| `job_battle_plan_record` | user_id, jd_text, plan_json, status | xiaou-mock-interview |

### 成长与计划

| 表 | 关键字段 | Mapper 模块 |
| --- | --- | --- |
| `growth_autopilot_goal` | user_id, goal_text, priority, status | xiaou-plan |
| `growth_autopilot_task` | user_id, goal_id, task_text, deadline, status | xiaou-plan |
| `growth_autopilot_event` | user_id, event_type, event_data | xiaou-plan |
| `learning_cockpit_rank_snapshot` | user_id, rank, score, snapshot_date | xiaou-plan |
| `user_plan` | user_id, title, type, start_date, end_date | xiaou-plan |
| `plan_checkin_record` | plan_id, user_id, checkin_date, content | xiaou-plan |
| `plan_remind_task` | plan_id, remind_time, status | xiaou-plan |

### 学习小组

| 表 | 关键字段 | Mapper 模块 |
| --- | --- | --- |
| `study_team` | name, description, creator_id, max_members, status | xiaou-team |
| `study_team_member` | team_id, user_id, role, join_time | xiaou-team |
| `study_team_application` | team_id, user_id, status, message | xiaou-team |
| `study_team_task` | team_id, title, deadline, assignee_id, status | xiaou-team |
| `study_team_checkin` | team_id, user_id, content, checkin_date | xiaou-team |
| `study_team_checkin_like` | checkin_id, user_id | xiaou-team |
| `study_team_checkin_comment` | checkin_id, user_id, content | xiaou-team |
| `study_team_discussion` | team_id, user_id, title, content | xiaou-team |
| `study_team_discussion_like` | discussion_id, user_id | xiaou-team |
| `study_team_daily_stats` | team_id, date, checkin_count, active_count | xiaou-team |

### 闪卡

| 表 | 关键字段 | Mapper 模块 |
| --- | --- | --- |
| `flashcard_deck` | user_id, title, description, is_public | xiaou-flashcard |
| `flashcard` | deck_id, front_content, back_content, sort_order | xiaou-flashcard |
| `flashcard_daily_stats` | user_id, date, cards_reviewed, correct_count | xiaou-flashcard |
| `flashcard_review_log` | user_id, card_id, quality, review_time | xiaou-flashcard |
| `flashcard_study_history` | user_id, card_id, next_review, interval | xiaou-flashcard |
| `flashcard_study_record` | user_id, deck_id, start_time, end_time | xiaou-flashcard |
| `flashcard_user_deck` | user_id, deck_id, last_studied | xiaou-flashcard |
| `flashcard_deck_like` | deck_id, user_id | xiaou-flashcard |

### 学习资产与知识图谱

| 表 | 关键字段 | Mapper 模块 |
| --- | --- | --- |
| `learning_asset_record` | user_id, asset_type, source_id, status | xiaou-learning-asset |
| `learning_asset_candidate` | record_id, candidate_type, content | xiaou-learning-asset |
| `learning_asset_publish_log` | record_id, target_module, publish_time | xiaou-learning-asset |
| `knowledge_map` | user_id, title, description, is_public | xiaou-knowledge |
| `knowledge_node` | map_id, label, parent_id, category, sort_order | xiaou-knowledge |
| `sql_monitor_log` | user_id, sql_text, execution_time | xiaou-sql-optimizer |
| `sql_optimize_record` | user_id, original_sql, optimized_sql, score | xiaou-sql-optimizer |

## OJ

| 表 | 关键字段 | Mapper 模块 |
| --- | --- | --- |
| `oj_problem` | title, description, difficulty, time_limit, memory_limit | xiaou-oj |
| `oj_problem_tag` | name, color | xiaou-oj |
| `oj_problem_tag_relation` | problem_id, tag_id | xiaou-oj |
| `oj_test_case` | problem_id, input, expected_output, is_sample | xiaou-oj |
| `oj_submission` | problem_id, user_id, code, language, status, score | xiaou-oj |
| `oj_solution` | problem_id, user_id, title, content, language | xiaou-oj |
| `oj_contest` | title, start_time, end_time, status, creator_id | xiaou-oj |
| `oj_contest_problem` | contest_id, problem_id, score | xiaou-oj |
| `oj_contest_participant` | contest_id, user_id, score, rank | xiaou-oj |
| `oj_problem_comment` | problem_id, user_id, content, parent_id | xiaou-oj |
| `oj_problem_comment_like` | comment_id, user_id | xiaou-oj |

## 内容社区

| 表 | 关键字段 | Mapper 模块 |
| --- | --- | --- |
| `community_category` | name, icon, sort_order | xiaou-community |
| `community_tag` | name, color, usage_count | xiaou-community |
| `community_post` | user_id, title, content, category_id, view_count, like_count | xiaou-community |
| `community_post_tag` | post_id, tag_id | xiaou-community |
| `community_post_like` | post_id, user_id | xiaou-community |
| `community_post_collect` | post_id, user_id | xiaou-community |
| `community_comment` | post_id, user_id, content, parent_id | xiaou-community |
| `community_comment_like` | comment_id, user_id | xiaou-community |
| `community_user_status` | user_id, post_count, like_count | xiaou-community |
| `moments` | user_id, content, image_urls, view_count | xiaou-moment |
| `moment_comments` | moment_id, user_id, content | xiaou-moment |
| `moment_likes` | moment_id, user_id | xiaou-moment |
| `moment_favorites` | moment_id, user_id | xiaou-moment |
| `moment_views` | moment_id, user_id, ip | xiaou-moment |
| `blog_config` | user_id, blog_name, description, theme | xiaou-blog |
| `blog_article` | user_id, title, content, category_id, status | xiaou-blog |
| `blog_category` | user_id, name, sort_order | xiaou-blog |
| `blog_tag` | name, usage_count | xiaou-blog |
| `code_pen` | user_id, title, code, language, is_public | xiaou-codepen |
| `code_pen_like` | pen_id, user_id | xiaou-codepen |
| `code_pen_collect` | pen_id, user_id | xiaou-codepen |
| `code_pen_folder` | user_id, name, parent_id | xiaou-codepen |
| `code_pen_comment` | pen_id, user_id, content | xiaou-codepen |
| `code_pen_tag` | name, usage_count | xiaou-codepen |
| `code_pen_fork_transaction` | source_pen_id, target_pen_id, user_id | xiaou-codepen |
| `resume_templates` | name, preview_url, template_data, is_active | xiaou-resume |
| `resume_info` | user_id, template_id, title | xiaou-resume |
| `resume_sections` | resume_id, section_type, section_data, sort_order | xiaou-resume |
| `resume_versions` | resume_id, version_number, snapshot_json | xiaou-resume |
| `resume_shares` | resume_id, share_token, view_count | xiaou-resume |
| `resume_analytics` | resume_id, event_type, event_data | xiaou-resume |

## 平台能力

| 表 | 关键字段 | Mapper 模块 |
| --- | --- | --- |
| `file_info` | user_id, original_name, file_path, file_size, mime_type | xiaou-filestorage |
| `file_access` | file_id, access_time, ip, user_id | xiaou-filestorage |
| `file_storage` | storage_type, endpoint, bucket, access_key | xiaou-filestorage |
| `storage_config` | type, config_json, is_default | xiaou-filestorage |
| `file_migration` | source_storage, target_storage, status | xiaou-filestorage |
| `file_system_setting` | key, value, description | xiaou-filestorage |
| `notification` | user_id, type, title, content, is_read | xiaou-notification |
| `notification_template` | code, title_template, content_template | xiaou-notification |
| `notification_config` | user_id, type, enabled | xiaou-notification |
| `notification_user_read_record` | user_id, notification_id, read_time | xiaou-notification |
| `chat_rooms` | name, type, creator_id, max_members | xiaou-chat |
| `chat_messages` | room_id, sender_id, content, type, is_read | xiaou-chat |
| `chat_online_users` | user_id, last_active_time | xiaou-chat |
| `chat_user_bans` | user_id, room_id, reason, end_time | xiaou-chat |
| `chat_sensitive_word` | word, level | xiaou-chat |
| `user_points_balance` | user_id, balance, total_earned | xiaou-points |
| `user_points_detail` | user_id, type, amount, source_id, description | xiaou-points |
| `user_checkin_bitmap` | user_id, year_month, bitmap_data | xiaou-points |
| `lottery_prize_config` | name, probability, stock, points_cost | xiaou-points |
| `lottery_draw_record` | user_id, prize_id, draw_time | xiaou-points |
| `lottery_statistics_daily` | date, total_draws, total_wins | xiaou-points |
| `user_lottery_limit` | user_id, daily_draws, daily_limit | xiaou-points |
| `lottery_adjust_history` | prize_id, field, old_value, new_value, operator | xiaou-points |
| `sensitive_category` | name, description, is_enabled | xiaou-sensitive |
| `sensitive_word` | category_id, word, level, is_enabled | xiaou-sensitive |
| `sensitive_log` | user_id, content, hit_words, source_type | xiaou-sensitive |
| `sensitive_whitelist` | word, reason, created_by | xiaou-sensitive |
| `sensitive_strategy` | name, action, config_json | xiaou-sensitive |
| `sensitive_source` | name, url, sync_interval | xiaou-sensitive |
| `sensitive_version` | version, word_count, checksum | xiaou-sensitive |
| `sensitive_homophone` | original, homophone | xiaou-sensitive |
| `sensitive_similar_char` | original, similar | xiaou-sensitive |
| `sensitive_hit_statistics` | word_id, hit_count, last_hit_time | xiaou-sensitive |
| `sensitive_user_violation` | user_id, violation_type, penalty | xiaou-sensitive |
| `version_history` | version, title, content, release_date, status | xiaou-version |
| `developer_calendar_event` | date, title, type, description | xiaou-moyu |
| `daily_content` | date, type, content, source | xiaou-moyu |
| `user_calendar_preference` | user_id, types_enabled | xiaou-moyu |
| `user_calendar_collection` | user_id, event_id | xiaou-moyu |
| `user_salary_config` | user_id, monthly_salary, work_hours | xiaou-moyu |
| `work_record` | user_id, date, start_time, end_time | xiaou-moyu |
| `bug_item` | title, description, severity, status | xiaou-moyu |
| `user_bug_history` | user_id, bug_id, action | xiaou-moyu |

## 数据库视图

| 视图名 | 说明 | 源表 |
| --- | --- | --- |
| `v_user_points_summary` | 用户积分汇总 | user_points_balance + user_points_detail |

## 版本脚本索引

| 版本目录 | 重点脚本 |
| --- | --- |
| `sql/v1.2.0` | 基础表结构 |
| `sql/v1.2.1` | 知识图谱、敏感词基础表 |
| `sql/v1.3.0` | Bug 商店、薪资计算器、版本历史、程序员日历 |
| `sql/v1.4.0` | 积分、IM 聊天 |
| `sql/v1.5.0` | 博客、敏感词升级、动态升级、抽奖、社区升级 |
| `sql/v1.6.0` | 简历、代码工坊 |
| `sql/v1.6.1` | 个人计划打卡 |
| `sql/v1.6.3` | AI 模拟面试 |
| `sql/v1.7.0` | 学习效果追踪、聊天升级 |
| `sql/v1.7.1` | 学习小组 |
| `sql/v1.7.2` | 闪卡 |
| `sql/v1.8.0` | OJ 基础表、题解 |
| `sql/v1.8.1` | OJ 赛事 |
| `sql/v1.8.2` | 成长自动驾驶、求职闭环、岗位匹配、OJ 评论、SQL 优化、驾驶舱排行 |
| `sql/v1.8.3` | 学习资产候选 |
| `sql/v1.8.4` | 学习资产转化 |

## Mapper 定位规则

1. Java Mapper 多数位于 `xiaou-*/src/main/java/**/mapper`。
2. XML Mapper 多数位于 `xiaou-*/src/main/resources/mapper`。
3. OJ 模块的部分 XML 和 Java Mapper 同目录，检查时不要只扫 `resources`。
4. 新表必须同步实体、Mapper、Service、Controller、模块文档和本页。

### 快速定位 Mapper

```bash
# 按 Java 类名找 Mapper 接口
find . -name "*Mapper.java" -path "*/xiaou-POINTS/*"

# 按 XML 名找 SQL
find . -name "*Mapper.xml" -path "*/xiaou-POINTS/*"

# 按表名搜 SQL
grep -r "user_points_balance" --include="*.xml" xiaou-*/
```

## 维护检查清单

新增表时：

1. 在对应模块的 `domain/` 下创建实体类
2. 在对应模块的 `mapper/` 下创建 Mapper 接口 + XML
3. 在对应模块的 `service/` 下创建 Service + Impl
4. 在对应模块的 `controller/` 下创建 Controller
5. 在 `sql/vX.Y.Z/` 下创建增量脚本
6. 同步更新 `sql/MySql/code_nest.sql` 基线
7. 更新本页的表索引
8. 更新 [数据库架构](/architecture/database) 的表前缀映射
9. 更新对应模块的功能文档

## 源码导航

| 文件 | 说明 |
| --- | --- |
| `sql/MySql/code_nest.sql` | 主库基线脚本（136 表，加上增量共 142 表） |
| `sql/v1.2.0/` ~ `sql/v1.8.4/` | 版本增量脚本 |
| `xiaou-*/src/main/java/**/domain/` | 实体类目录 |
| `xiaou-*/src/main/java/**/mapper/` | Mapper 接口目录 |
| `xiaou-*/src/main/resources/mapper/` | Mapper XML 目录 |


## 相关文档

| 文档 | 说明 |
| --- | --- |
| [数据库字段阅读指南](/reference/database-field-guide) | 命名规范和类型说明 |
| [数据库与脚本](/architecture/database) | 数据库设计和版本管理 |
| [模块依赖地图](/reference/module-dependencies) | 模块间依赖关系 |
| [源码地图](/reference/source-map) | Mapper 文件位置 |
