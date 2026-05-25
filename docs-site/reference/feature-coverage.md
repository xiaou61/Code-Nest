# 全功能覆盖矩阵

本页是 v2.2.0 文档站的"覆盖检查表"。每一行至少要能回答：用户从哪里进、后台从哪里管、后端走哪个前缀、核心数据落在哪些表、文档写在哪里。

## API 端点统计总览

基于源码扫描，项目共有 **757 个 REST 端点**（@GetMapping/@PostMapping/@PutMapping/@DeleteMapping/@PatchMapping），分布在 **97 个 Controller 类**中。加上 WebSocket 端点和 IndexController，总计约 760 个入口。

| 模块 | 用户端端点 | 管理端端点 | 公开端点 | 合计 | Controller 数 |
| --- | --- | --- | --- | --- | --- |
| xiaou-user | 17 | 10 | 2 | 29 | 4 |
| xiaou-system | 9 | 30 | 0 | 39 | 4 |
| xiaou-interview | 27 | 24 | 2 | 53 | 7 |
| xiaou-mock-interview | 39 | 8 | 0 | 47 | 5 |
| xiaou-plan | 17 | 0 | 0 | 17 | 2 |
| xiaou-team | 61 | 0 | 0 | 61 | 1 |
| xiaou-flashcard | 17 | 0 | 3 | 20 | 4 |
| xiaou-oj | 23 | 21 | 0 | 44 | 8 |
| xiaou-community | 26 | 24 | 2 | 52 | 10 |
| xiaou-moment | 13 | 5 | 0 | 18 | 2 |
| xiaou-blog | 16 | 13 | 0 | 29 | 2 |
| xiaou-codepen | 34 | 19 | 0 | 53 | 2 |
| xiaou-resume | 10 | 7 | 0 | 17 | 4 |
| xiaou-filestorage | 9 | 22 | 0 | 31 | 5 |
| xiaou-notification | 6 | 9 | 0 | 15 | 2 |
| xiaou-chat | 5 | 8 | 0 | 13 | 2 |
| xiaou-points | 11 | 32 | 0 | 43 | 4 |
| xiaou-sensitive | 0 | 12 | 51 | 63 | 9 |
| xiaou-moyu | 30 | 26 | 0 | 56 | 7 |
| xiaou-version | 5 | 12 | 0 | 17 | 2 |
| xiaou-knowledge | 5 | 16 | 0 | 21 | 3 |
| xiaou-learning-asset | 8 | 7 | 0 | 15 | 2 |
| xiaou-sql-optimizer | 11 | 0 | 0 | 11 | 1 |
| xiaou-ai | 0 | 1 | 0 | 1 | 1 |
| xiaou-application | 1 | 0 | 0 | 1 | 2 |

> 端点数统计不含 @RequestMapping 类级注解，只统计方法级 @Get/Post/Put/Delete/PatchMapping。WebSocket 端点 /ws/chat 未计入。

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

## @RequireAdmin 方法分布

基于源码扫描，项目当前所有管理端权限校验统一通过 `@RequireAdmin` 注解实现。**没有使用 `@SaCheckRole` 和 `@SaCheckPermission`**。

| 模块 | Controller 类 | 方法数 | 主要职责 |
| --- | --- | --- | --- |
| xiaou-user | AdminUserController | 10 | 用户 CRUD、禁用、密码重置、统计 |
| xiaou-blog | BlogAdminController | 13 | 文章、分类、标签管理 |
| xiaou-codepen | CodePenAdminController | 17 | 作品、模板、文件夹、标签管理 |
| xiaou-community | CommunityPostAdminController | 6 | 帖子管理 |
| xiaou-community | CommunityCommentAdminController | 3 | 评论管理 |
| xiaou-community | CommunityUserAdminController | 6 | 用户状态管理 |
| xiaou-community | AdminCommunityTagController | 5 | 标签管理 |
| xiaou-community | AdminCommunityCategoryController | 6 | 分类管理 |
| xiaou-interview | InterviewQuestionSetController | 8 | 题单管理 |
| xiaou-interview | InterviewQuestionController | 11 | 题目管理 |
| xiaou-interview | InterviewCategoryController | 5 | 分类管理 |
| xiaou-filestorage | AdminSystemController | 5 | 系统配置管理 |
| xiaou-filestorage | AdminStorageController | 10 | 存储配置管理 |
| xiaou-filestorage | AdminMigrationController | 7 | 迁移任务管理 |
| xiaou-filestorage | AdminFileController | 1 | 文件管理 |
| xiaou-moment | AdminMomentController | 5 | 动态管理 |
| xiaou-knowledge | AdminKnowledgeMapController | 7 | 图谱管理 |
| xiaou-knowledge | AdminKnowledgeNodeController | 7 | 节点管理 |
| xiaou-notification | AdminNotificationController | 9 | 通知管理 |
| xiaou-moyu | AdminDailyContentController | 11 | 每日内容管理 |
| xiaou-moyu | AdminDeveloperCalendarController | 9 | 开发者日历管理 |
| xiaou-moyu | AdminBugStoreController | 6 | Bug 库管理 |
| xiaou-chat | ChatAdminController | 8 | 聊天管理 |
| xiaou-points | AdminPointsController | 6 | 积分管理 |
| xiaou-points | AdminLotteryController | 22 | 抽奖配置、监控、风控、应急 |
| xiaou-sensitive | SensitiveWordAdminController | 2 | 敏感词管理 |
| xiaou-sensitive | SensitiveWhitelistController | 8 | 白名单管理 |
| xiaou-sensitive | SensitiveVersionController | 3 | 版本管理 |
| xiaou-sensitive | SensitiveStrategyController | 4 | 策略管理 |
| xiaou-sensitive | SensitiveHomophoneController | 若干 | 同音字管理 |
| xiaou-sensitive | SensitiveSimilarCharController | 若干 | 形近字管理 |
| xiaou-sensitive | SensitiveSourceController | 若干 | 来源管理 |
| xiaou-sensitive | SensitiveStatisticsController | 若干 | 统计管理 |
| xiaou-version | VersionHistoryAdminController | 12 | 版本发布管理 |
| xiaou-system | DashboardController | 1 | 仪表盘总览 |
| xiaou-system | LogController | 8 | 日志查询、清理 |
| xiaou-system | AiConfigController | 20 | AI 配置、调试、回归、RAG 管理 |
| xiaou-ai | AiGovernanceController | 1 | AI 治理总览 |
| xiaou-oj | OjProblemController | 6 | 题目管理 |
| xiaou-oj | OjContestController | 6 | 赛事管理 |
| xiaou-oj | OjSolutionController | 3 | 题解管理 |
| xiaou-oj | OjTestCaseController | 4 | 测试用例管理 |
| xiaou-mock-interview | AdminMockInterviewController | 2 | 模拟面试管理 |
| xiaou-learning-asset | AdminLearningAssetController | 5 | 学习资产审核 |
| xiaou-resume | ResumeTemplateAdminController | 5 | 简历模板管理 |
| xiaou-resume | ResumeAnalyticsAdminController | 2 | 简历分析 |

**合计约 270 个 `@RequireAdmin` 方法**，分布在 42+ 个 Admin Controller 中。

### 关键发现

1. **无 `@SaCheckRole` / `@SaCheckPermission`**：源码中完全没有使用 Sa-Token 的角色/权限注解，所有管理端权限控制统一走 `@RequireAdmin`。这意味着当前不存在细粒度的后端 RBAC 校验。
2. **`@RequireAdmin` 只做登录域 + admin 角色校验**：切面实现是先 `StpAdminUtil.checkLogin()` 再 `StpAdminUtil.checkRole("admin")`，并不按 `permission_code` 鉴权。
3. **部分模块注解有自定义 message**：如 `AdminUserController` 和 `AdminPointsController` 带有 `message = "...需要管理员权限"`，大部分模块直接使用默认 message。
4. **`@Log` 注解伴随高频**：高风险写操作（批量删除、清空、导入导出）同时标注 `@Log`。

## Controller 层级分布

项目 Controller 按包路径分为三类：

| 层级 | 包路径 | 鉴权方式 | 典型类 |
| --- | --- | --- | --- |
| admin | `controller/admin/` | `@RequireAdmin` | `AdminUserController`、`BlogAdminController` |
| user | `controller/user/` | `StpUserUtil.checkLogin()` + 归属校验 | `UserPlanController`、`CodePenUserController` |
| pub | `controller/pub/` | SaTokenConfig 白名单放行 | `CommunityPostController`、`OjProblemPublicController` |
| api | `controller/api/` | 服务间调用，不走用户鉴权 | `SensitiveWordController` |
| 根 | `controller/` | 混合，部分需登录部分公开 | `DeveloperCalendarController`、`DailyContentController` |

> **新增 Controller 时**：管理端放 `controller/admin/`，用户端放 `controller/user/`，公开接口放 `controller/pub/`，不要混用。

## 未覆盖和高风险功能

| 功能 | 风险描述 | 建议 |
| --- | --- | --- |
| 用户端细粒度权限 | `@SaCheckRole` / `@SaCheckPermission` 完全未使用，用户端无法通过注解做权限控制 | 需要时补注解 + `StpInterfaceImpl` 实现 |
| 资源归属校验覆盖率 | 不确定所有"我的数据"接口都做了 owner 校验 | 逐 Controller 检查 `StpUserUtil.getLoginIdAsLong()` 使用情况 |
| 管理端角色分级 | 所有管理员等同，无运营/超管区分 | 如需分级需补角色逻辑 |
| 文件访问权限 | `is_public` 字段和业务归属可能不同步 | 审计文件读写和业务逻辑的一致性 |
| WebSocket 鉴权 | ws-ticket 签发后的连接鉴权依赖 Redis TTL | 确保 ticket 一次性且 TTL 合理 |

这页解决的是"功能落在哪里"。如果你接下来要判断"改完某个模块最低该回归什么"，继续看 [模块最小回归矩阵](/reference/module-regression-matrix) 会更直接。
