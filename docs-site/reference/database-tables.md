# 数据表索引

主库基线脚本位于 `sql/MySql/code_nest.sql`。版本增量脚本位于 `sql/v*` 目录，用于追溯模块从 v1.x 到 v2.x 的演进。

## 使用方式

| 任务 | 推荐入口 |
| --- | --- |
| 新环境初始化 | 先执行 `sql/MySql/code_nest.sql` |
| 查看模块历史变更 | 读取对应 `sql/v*/*.sql` |
| 新功能加表 | 先加版本脚本，再同步主库基线 |
| 写模块文档 | 本页定位表，再回到对应 Mapper XML 和 Service |

## 账号、权限与日志

| 业务域 | 表 |
| --- | --- |
| 用户账户 | `user_info` |
| 管理员和权限 | `sys_admin`、`sys_role`、`sys_permission`、`sys_admin_role`、`sys_role_permission` |
| 日志审计 | `sys_login_log`、`sys_operation_log` |

## 学习成长

| 业务域 | 表 |
| --- | --- |
| 面试题库 | `interview_category`、`interview_question_set`、`interview_question` |
| 面试学习 | `interview_learn_record`、`interview_favorite`、`interview_mastery_record`、`interview_mastery_history`、`interview_daily_stats` |
| AI 模拟面试 | `mock_interview_direction`、`mock_interview_session`、`mock_interview_qa`、`mock_interview_user_stats` |
| 求职闭环 | `career_loop_session`、`career_loop_stage_log`、`career_loop_action`、`career_loop_snapshot` |
| 求职作战台 | `job_battle_plan_record` |
| 成长自动驾驶 | `growth_autopilot_goal`、`growth_autopilot_task`、`growth_autopilot_event` |
| 学习驾驶舱 | `learning_cockpit_rank_snapshot` |
| 计划打卡 | `user_plan`、`plan_checkin_record`、`plan_remind_task` |
| 学习小组 | `study_team`、`study_team_member`、`study_team_application`、`study_team_task`、`study_team_checkin`、`study_team_checkin_like`、`study_team_checkin_comment`、`study_team_discussion`、`study_team_daily_stats` |
| 闪卡 | `flashcard_deck`、`flashcard`、`flashcard_daily_stats`、`flashcard_review_log`、`flashcard_study_history`、`flashcard_study_record`、`flashcard_user_deck`、`flashcard_deck_like` |
| 学习资产 | `learning_asset_record`、`learning_asset_candidate`、`learning_asset_publish_log` |
| 知识图谱 | `knowledge_map`、`knowledge_node` |
| SQL 优化 | `sql_monitor_log`、`sql_optimize_record` |

## OJ

| 业务域 | 表 |
| --- | --- |
| 题目 | `oj_problem`、`oj_problem_tag`、`oj_problem_tag_relation`、`oj_test_case` |
| 提交与题解 | `oj_submission`、`oj_solution` |
| 赛事 | `oj_contest`、`oj_contest_problem`、`oj_contest_participant` |
| 评论 | `oj_problem_comment`、`oj_problem_comment_like` |

## 内容社区

| 业务域 | 表 |
| --- | --- |
| 社区帖子 | `community_category`、`community_tag`、`community_post`、`community_post_tag`、`community_post_like`、`community_post_collect`、`community_comment`、`community_comment_like`、`community_user_status` |
| 动态广场 | `moments`、`moment_comments`、`moment_likes`、`moment_favorites`、`moment_views` |
| 博客 | `blog_config`、`blog_article`、`blog_category`、`blog_tag` |
| 代码工坊 | `code_pen`、`code_pen_like`、`code_pen_collect`、`code_pen_folder`、`code_pen_comment`、`code_pen_tag`、`code_pen_fork_transaction` |
| 简历 | `resume_templates`、`resume_info`、`resume_sections`、`resume_versions`、`resume_shares`、`resume_analytics` |

## 平台能力

| 业务域 | 表 |
| --- | --- |
| 文件存储 | `file_info`、`file_access`、`file_storage`、`storage_config`、`file_migration`、`file_system_setting` |
| 通知 | `notification`、`notification_template`、`notification_config`、`notification_user_read_record` |
| IM 聊天 | `chat_rooms`、`chat_messages`、`chat_online_users`、`chat_user_bans`、`chat_sensitive_word` |
| 积分 | `user_points_balance`、`user_points_detail`、`user_checkin_bitmap` |
| 抽奖 | `lottery_prize_config`、`lottery_draw_record`、`lottery_statistics_daily`、`user_lottery_limit`、`lottery_adjust_history` |
| 敏感词 | `sensitive_category`、`sensitive_word`、`sensitive_log`、`sensitive_whitelist`、`sensitive_strategy`、`sensitive_source`、`sensitive_version`、`sensitive_homophone`、`sensitive_similar_char`、`sensitive_hit_statistics`、`sensitive_user_violation` |
| 版本历史 | `version_history` |
| 摸鱼工具 | `developer_calendar_event`、`daily_content`、`user_calendar_preference`、`user_calendar_collection`、`user_salary_config`、`work_record`、`bug_item`、`user_bug_history` |

## 版本脚本索引

| 版本目录 | 重点脚本 |
| --- | --- |
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
| `sql/v1.8.4` | 学习资产转化 |

## Mapper 定位规则

1. Java Mapper 多数位于 `xiaou-*/src/main/java/**/mapper`。
2. XML Mapper 多数位于 `xiaou-*/src/main/resources/mapper`。
3. OJ 模块的部分 XML 和 Java Mapper 同目录，检查时不要只扫 `resources`。
4. 新表必须同步实体、Mapper、Service、Controller、模块文档和本页。
