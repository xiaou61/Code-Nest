# 全功能覆盖矩阵

本页是 v2.2.0 文档站的“覆盖检查表”。每一行至少要能回答：用户从哪里进、后台从哪里管、后端走哪个前缀、核心数据落在哪些表、文档写在哪里。

## 账号与平台基础

| 功能 | 用户入口 | 管理入口 | API 前缀 | 核心表 | 文档 |
| --- | --- | --- | --- | --- | --- |
| 用户注册登录 | `/login`、`/register` | 无 | `/user/auth`、`/captcha` | `user_info` | [鉴权与用户体系](/modules/auth) |
| 用户个人中心 | `/profile` | `/user` | `/user`、`/admin/user` | `user_info` | [用户账户与个人中心](/modules/user-account) |
| 管理端登录 | 无 | `/login`、`/profile/*` | `/auth` | `sys_admin`、`sys_login_log` | [鉴权与用户体系](/modules/auth) |
| 权限与角色 | 无 | 后续菜单扩展 | `/auth`、权限注解 | `sys_role`、`sys_permission`、`sys_admin_role`、`sys_role_permission` | [系统运营后台](/modules/system-ops) |
| 仪表盘 | 无 | `/dashboard` | `/admin/dashboard` | 多业务聚合 | [仪表盘与日志](/modules/dashboard-logs) |
| 日志审计 | 无 | `/logs/login`、`/logs/operation` | `/log` | `sys_login_log`、`sys_operation_log` | [仪表盘与日志](/modules/dashboard-logs) |

## 学习成长

| 功能 | 用户入口 | 管理入口 | API 前缀 | 核心表 | 文档 |
| --- | --- | --- | --- | --- | --- |
| 面试题分类 | `/interview` | `/interview/categories` | `/interview/categories`、`/admin/interview/categories` | `interview_category` | [面试题库](/modules/interview) |
| 题单学习 | `/interview/question-sets/:id` | `/interview/question-sets` | `/interview/question-sets`、`/admin/interview/question-sets` | `interview_question_set`、`interview_question` | [面试题库](/modules/interview) |
| 题目学习 | `/interview/questions/:setId/:questionId` | `/interview/questions` | `/interview/question-sets`、`/admin/interview/questions` | `interview_question`、`interview_learn_record` | [题库与成长闭环](/modules/interview-and-growth) |
| 收藏和复习 | `/interview/favorites`、`/interview/review` | 无 | `/interview/favorites`、`/interview/mastery` | `interview_favorite`、`interview_mastery_record`、`interview_mastery_history` | [题库与成长闭环](/modules/interview-and-growth) |
| AI 模拟面试 | `/mock-interview/*` | `/mock-interview/sessions`、`/mock-interview/directions` | `/user/mock-interview`、`/admin/mock-interview` | `mock_interview_direction`、`mock_interview_session`、`mock_interview_qa`、`mock_interview_user_stats` | [模拟面试与求职作战台](/modules/mock-interview-job-battle) |
| 求职作战台 | `/job-battle`、`/job-match-engine` | `/system/ai-config`、`/system/ai-governance` | `/user/job-battle`、`/admin/ai/config` | `job_battle_plan_record` | [模拟面试与求职作战台](/modules/mock-interview-job-battle) |
| 求职闭环 | `/career-loop` | 无 | `/user/career-loop` | `career_loop_session`、`career_loop_stage_log`、`career_loop_action`、`career_loop_snapshot` | [模拟面试与求职作战台](/modules/mock-interview-job-battle) |
| 学习驾驶舱 | `/learning-cockpit` | 无 | `/user/learning-cockpit` | `learning_cockpit_rank_snapshot` | [题库与成长闭环](/modules/interview-and-growth) |
| 成长自动驾驶 | `/growth-autopilot` 重定向到驾驶舱页签 | 无 | `/user/plan/autopilot` | `growth_autopilot_goal`、`growth_autopilot_task`、`growth_autopilot_event` | [题库与成长闭环](/modules/interview-and-growth) |
| 学习资产 | `/learning-assets` | `/learning-assets/review`、`/learning-assets/statistics` | `/user/learning-assets`、`/admin/learning-assets` | `learning_asset_record`、`learning_asset_candidate`、`learning_asset_publish_log` | [学习资产](/modules/learning-assets) |
| SQL 优化 | `/sql-optimizer/workbench` | `/system/ai-config` | `/user/sql-optimizer`、`/admin/ai/config` | `sql_monitor_log`、`sql_optimize_record` | [SQL 优化工作台](/modules/sql-optimizer) |
| 计划打卡 | `/plan` | 无 | `/user/plan` | `user_plan`、`plan_checkin_record`、`plan_remind_task` | [计划与学习小组](/modules/plan-team) |
| 学习小组 | `/team/*` | 后续可扩展 | `/user/team` | `study_team_*` | [计划与学习小组](/modules/plan-team) |
| 闪卡 | `/flashcard/*` | 后续可扩展审核 | `/pub/flashcard/deck`、`/flashcard/deck`、`/flashcard/card`、`/flashcard/study` | `flashcard_*` | [闪卡](/modules/flashcard) |
| 知识图谱 | `/knowledge`、`/knowledge/maps/:id` | `/knowledge/maps`、`/knowledge/maps/:id/edit` | `/pub/knowledge/maps`、`/admin/knowledge` | `knowledge_map`、`knowledge_node` | [知识图谱](/modules/knowledge) |

## OJ

| 功能 | 用户入口 | 管理入口 | API 前缀 | 核心表 | 文档 |
| --- | --- | --- | --- | --- | --- |
| OJ 题库 | `/oj`、`/oj/problem/:id` | `/oj/problems`、`/oj/problems/create`、`/oj/problems/:id/edit` | `/oj`、`/admin/oj/problems` | `oj_problem`、`oj_problem_tag`、`oj_problem_tag_relation` | [OJ 判题系统](/modules/oj) |
| 测试用例 | 题目详情内运行 | 题目编辑弹窗 | `/admin/oj/test-cases` | `oj_test_case` | [OJ 判题系统](/modules/oj) |
| 提交和判题 | `/oj/my-submissions`、`/oj/submission/:id` | 无 | `/oj/run`、`/oj/test`、`/oj/submit` | `oj_submission` | [OJ 判题系统](/modules/oj) |
| 题解 | 题目详情 | 后台接口 | `/oj/problems/{id}/solutions`、`/admin/oj/solutions` | `oj_solution` | [OJ 判题系统](/modules/oj) |
| 赛事 | `/oj/contests`、`/oj/contests/:id` | `/oj/contests`、赛事编辑 | `/oj/contests`、`/admin/oj/contests` | `oj_contest`、`oj_contest_problem`、`oj_contest_participant` | [OJ 判题系统](/modules/oj) |
| 评论 | 题目详情 | 后续可扩展审核 | `/oj` | `oj_problem_comment`、`oj_problem_comment_like` | [OJ 判题系统](/modules/oj) |

## 内容与创作

| 功能 | 用户入口 | 管理入口 | API 前缀 | 核心表 | 文档 |
| --- | --- | --- | --- | --- | --- |
| 社区帖子 | `/community`、`/community/posts/:id`、`/community/create` | `/community/posts` | `/community/posts`、`/admin/community/posts` | `community_post`、`community_post_like`、`community_post_collect` | [社区帖子](/modules/community) |
| 社区评论 | 帖子详情 | `/community/comments` | `/community`、`/admin/community/comments` | `community_comment`、`community_comment_like` | [社区帖子](/modules/community) |
| 社区分类标签 | 社区首页筛选 | `/community/categories`、`/community/tags` | `/community/categories`、`/community/tags`、`/admin/community/**` | `community_category`、`community_tag`、`community_post_tag` | [社区与内容矩阵](/modules/community-content) |
| 社区用户状态 | `/community/users/:userId` | `/community/users` | `/community/users`、`/admin/community/users` | `community_user_status` | [社区帖子](/modules/community) |
| 动态广场 | `/moments`、`/moments/user/:userId`、`/moments/my-favorites` | `/moments/list`、`/moments/comments`、`/moments/statistics` | `/user/moments`、`/admin/moments` | `moments`、`moment_comments`、`moment_likes`、`moment_favorites`、`moment_views` | [动态广场](/modules/moments) |
| 博客 | `/blog`、`/blog/editor`、公开博客页 | `/blog/articles`、`/blog/categories`、`/blog/tags` | `/user/blog`、`/admin/blog` | `blog_config`、`blog_article`、`blog_category`、`blog_tag` | [博客](/modules/blog) |
| 代码工坊 | `/codepen`、`/codepen/editor`、`/codepen/my`、`/codepen/:id` | `/codepen/pens`、`/codepen/templates`、`/codepen/tags`、`/codepen/statistics` | `/user/code-pen`、`/admin/code-pen` | `code_pen*` | [代码工坊](/modules/codepen) |
| 简历 | `/resume`、`/resume/templates`、`/resume/editor` | `/resume/templates`、`/resume/analytics`、`/resume/reports` | `/resume`、`/resume/templates`、`/admin/resume` | `resume_*` | [简历系统](/modules/resume) |

## 平台能力与工具

| 功能 | 用户入口 | 管理入口 | API 前缀 | 核心表 | 文档 |
| --- | --- | --- | --- | --- | --- |
| 文件存储 | 上传组件和聊天图片 | `/filestorage/*` | `/file`、`/admin/file`、`/admin/storage`、`/admin/system` | `file_*`、`storage_config` | [文件存储](/modules/file-storage) |
| 通知中心 | `/notification` | `/notification` | `/notification`、`/admin/notification` | `notification*` | [通知中心](/modules/notification) |
| IM 聊天室 | `/chat` | `/chat/messages`、`/chat/users` | `/user/chat`、`/admin/chat`、`/ws/chat` | `chat_*` | [IM 聊天室](/modules/chat) |
| 积分 | `/points` | `/points/index`、`/points/users`、`/points/details`、`/points/grant` | `/user/points`、`/admin/points` | `user_points_balance`、`user_points_detail`、`user_checkin_bitmap` | [积分与抽奖](/modules/points) |
| 抽奖 | `/lottery` | `/lottery` | `/user/lottery`、`/admin/lottery` | `lottery_*`、`user_lottery_limit` | [积分与抽奖](/modules/points) |
| 敏感词风控 | 内容发布链路内调用 | `/sensitive/*` | `/sensitive/**`、`/admin/sensitive` | `sensitive_*` | [敏感词风控](/modules/sensitive) |
| 版本历史 | `/version-history` | `/system/version` | `/version`、`/admin/version` | `version_history` | [版本历史](/modules/version-history) |
| 程序员工具 | `/dev-tools/*` | 无 | 前端本地能力 | 无 | [开发者工具](/modules/dev-tools) |
| 摸鱼工具 | `/moyu-tools/*` | `/moyu/*` | `/moyu/**`、`/admin/moyu/**` | `developer_calendar_event`、`daily_content`、`user_salary_config`、`work_record`、`bug_item`、`user_bug_history` | [摸鱼工具](/modules/moyu) |
| AI Runtime | 业务场景内调用 | `/system/ai-config`、`/system/ai-governance` | `/admin/ai/config`、`/admin/ai/governance` | Runtime 指标主要在 Redis，回归用例在资源文件 | [AI Runtime](/modules/ai-runtime) |

## 截图覆盖

| 端 | 目录 | 数量 | 手册 |
| --- | --- | --- | --- |
| 用户端 | `docs/manual-assets/2026-04-24/user` | 118 | [用户端操作手册](/manuals/user-operations) |
| 管理端 | `docs/manual-assets/2026-04-24/admin` | 81 | [管理端操作手册](/manuals/admin-operations) |

## 覆盖状态

| 类型 | 状态 |
| --- | --- |
| 模块文档 | 已覆盖所有主要业务模块首版 |
| API 索引 | 已按 Controller 前缀覆盖 |
| 前端路由 | 已覆盖用户端和管理端路由 |
| 数据表 | 已按业务域覆盖主库基线 |
| 操作手册 | 已整理截图素材和核心链路 |
| 待深化 | 单模块状态机、字段级 DTO、权限注解、更多异常路径 |
