# API 路由索引

本页把后端 Controller 的入口按业务域整理成索引。它不替代接口文档工具，主要用于快速判断“某个能力在哪个模块、走哪个前缀、读哪个 Controller”。

## 路由约定

| 前缀 | 使用场景 | 登录域 |
| --- | --- | --- |
| `/admin/**` | 管理后台接口，通常由 `vue3-admin-front` 调用 | 管理员登录态 |
| `/user/**` | 用户端需要登录的业务接口 | 用户登录态 |
| `/pub/**` | 可公开访问或弱登录访问的展示接口 | 按具体 Controller 判断 |
| `/community/**`、`/interview/**`、`/oj/**`、`/resume/**` | 历史模块公共入口，部分接口仍要求登录 | 按业务方法判断 |
| `/auth`、`/captcha`、`/file`、`/notification`、`/version` | 平台公共能力入口 | 按接口动作判断 |

## 账号与系统

| 能力 | 路由前缀 | Controller | 模块 |
| --- | --- | --- | --- |
| 管理端登录、退出、个人信息、登录日志 | `/auth` | `AuthController` | `xiaou-system` |
| 管理端仪表盘 | `/admin/dashboard` | `DashboardController` | `xiaou-system` |
| 登录日志、操作日志 | `/log` | `LogController` | `xiaou-system` |
| 用户注册、登录、退出、刷新、唯一性校验 | `/user/auth` | `UserAuthController` | `xiaou-user` |
| 用户资料、头像、密码 | `/user` | `UserController` | `xiaou-user` |
| 管理端用户 CRUD、状态、重置密码、统计 | `/admin/user` | `AdminUserController` | `xiaou-user` |
| 图形验证码 | `/captcha` | `CaptchaController` | `xiaou-user` |

## 学习成长

| 能力 | 路由前缀 | Controller | 模块 |
| --- | --- | --- | --- |
| 面试分类公开查询 | `/interview/categories` | `InterviewCategoryPublicController` | `xiaou-interview` |
| 题单、题目详情、随机抽题、搜索 | `/interview/question-sets` | `InterviewQuestionSetPublicController` | `xiaou-interview` |
| 收藏、取消收藏、我的收藏 | `/interview/favorites` | `InterviewFavoriteController` | `xiaou-interview` |
| 学习记录、题单进度 | `/interview/learn` | `InterviewLearnRecordController` | `xiaou-interview` |
| 掌握度、复习统计、热力图 | `/interview/mastery` | `InterviewMasteryController` | `xiaou-interview` |
| 管理端题库分类 | `/admin/interview/categories` | `InterviewCategoryController` | `xiaou-interview` |
| 管理端题单 | `/admin/interview/question-sets` | `InterviewQuestionSetController` | `xiaou-interview` |
| 管理端题目 | `/admin/interview/questions` | `InterviewQuestionController` | `xiaou-interview` |
| AI 模拟面试配置、历史、报告 | `/user/mock-interview` | `MockInterviewController` | `xiaou-mock-interview` |
| AI 模拟面试会话流 | `/user/mock-interview/session` | `MockInterviewSessionController` | `xiaou-mock-interview` |
| 管理端模拟面试方向和会话 | `/admin/mock-interview` | `AdminMockInterviewController` | `xiaou-mock-interview` |
| 求职作战台、JD 解析、简历匹配、计划生成 | `/user/job-battle` | `JobBattleController` | `xiaou-mock-interview` |
| 求职闭环中台 | `/user/career-loop` | `CareerLoopController` | `xiaou-mock-interview` |
| 学习成长驾驶舱 | `/user/learning-cockpit` | `LearningCockpitController` | `xiaou-application`、`xiaou-plan` |
| 计划打卡 | `/user/plan` | `UserPlanController` | `xiaou-plan` |
| 成长自动驾驶 | `/user/plan/autopilot` | `UserGrowthAutopilotController` | `xiaou-plan` |
| 学习小组 | `/user/team` | `UserTeamController` | `xiaou-team` |
| 学习资产用户流 | `/user/learning-assets` | `UserLearningAssetController` | `xiaou-learning-asset` |
| 学习资产审核台 | `/admin/learning-assets` | `AdminLearningAssetController` | `xiaou-learning-asset` |
| 闪卡公开卡组 | `/pub/flashcard/deck` | `PubFlashcardDeckController` | `xiaou-flashcard` |
| 闪卡卡组 | `/flashcard/deck` | `FlashcardDeckController` | `xiaou-flashcard` |
| 闪卡卡片 | `/flashcard/card` | `FlashcardController` | `xiaou-flashcard` |
| 闪卡学习记录 | `/flashcard/study` | `FlashcardStudyController` | `xiaou-flashcard` |
| 知识图谱公开查询 | `/pub/knowledge/maps` | `PubKnowledgeMapController` | `xiaou-knowledge` |
| 管理端知识图谱 | `/admin/knowledge/maps`、`/admin/knowledge` | `AdminKnowledgeMapController`、`AdminKnowledgeNodeController` | `xiaou-knowledge` |
| SQL 优化工作台 | `/user/sql-optimizer` | `SqlOptimizerController` | `xiaou-sql-optimizer` |

## OJ 判题

| 能力 | 路由前缀 | Controller | 模块 |
| --- | --- | --- | --- |
| 题目列表、详情、标签、排行榜、每日一题 | `/oj` | `OjProblemPublicController` | `xiaou-oj` |
| 运行、测试、提交、提交记录 | `/oj` | `OjSubmissionController` | `xiaou-oj` |
| 评论和点赞 | `/oj` | `OjCommentController` | `xiaou-oj` |
| 赛事列表、详情、报名、榜单 | `/oj/contests` | `OjContestPublicController` | `xiaou-oj` |
| 管理端题目 | `/admin/oj/problems` | `OjProblemController` | `xiaou-oj` |
| 管理端测试用例 | `/admin/oj/test-cases` | `OjTestCaseController` | `xiaou-oj` |
| 管理端题解 | `/admin/oj/solutions` | `OjSolutionController` | `xiaou-oj` |
| 管理端赛事 | `/admin/oj/contests` | `OjContestController` | `xiaou-oj` |

## 内容社区

| 能力 | 路由前缀 | Controller | 模块 |
| --- | --- | --- | --- |
| 社区初始化、用户状态、热词 | `/community` | `CommunityPublicController` | `xiaou-community` |
| 帖子列表、详情、发布、点赞、收藏、热门 | `/community/posts` | `CommunityPostController` | `xiaou-community` |
| 帖子 AI 摘要 | `/community/posts` | `CommunityAiController` | `xiaou-community`、`xiaou-ai` |
| 评论、回复、评论点赞 | `/community` | `CommunityCommentController` | `xiaou-community` |
| 分类 | `/community/categories` | `CommunityCategoryController` | `xiaou-community` |
| 标签 | `/community/tags` | `CommunityTagController` | `xiaou-community` |
| 我的收藏、评论、帖子 | `/community/user` | `CommunityUserController` | `xiaou-community` |
| 用户主页 | `/community/users` | `CommunityUserProfileController` | `xiaou-community` |
| 管理端社区分类、标签、帖子、评论、用户 | `/admin/community/**` | 多个 `Community*AdminController` | `xiaou-community` |
| 动态广场用户流 | `/user/moments` | `UserMomentController` | `xiaou-moment` |
| 动态广场管理 | `/admin/moments` | `AdminMomentController` | `xiaou-moment` |
| 博客用户流 | `/user/blog` | `BlogUserController` | `xiaou-blog` |
| 博客管理 | `/admin/blog` | `BlogAdminController` | `xiaou-blog` |
| 代码工坊用户流 | `/user/code-pen` | `CodePenUserController` | `xiaou-codepen` |
| 代码工坊管理 | `/admin/code-pen` | `CodePenAdminController` | `xiaou-codepen` |
| 简历用户流 | `/resume`、`/resume/templates` | `ResumeUserController`、`ResumeTemplateController` | `xiaou-resume` |
| 简历管理和数据 | `/admin/resume`、`/admin/resume/templates` | `ResumeAnalyticsAdminController`、`ResumeTemplateAdminController` | `xiaou-resume` |

## 平台能力

| 能力 | 路由前缀 | Controller | 模块 |
| --- | --- | --- | --- |
| 文件上传、下载、URL、删除、列表、存在性检查 | `/file` | `FileController` | `xiaou-filestorage` |
| 管理端文件、迁移、存储配置、系统设置 | `/admin/file`、`/admin/storage`、`/admin/system` | `AdminFileController`、`AdminMigrationController`、`AdminStorageController`、`AdminSystemController` | `xiaou-filestorage` |
| 通知用户侧 | `/notification` | `NotificationController` | `xiaou-notification` |
| 通知管理侧 | `/admin/notification` | `AdminNotificationController` | `xiaou-notification` |
| 聊天用户侧 REST | `/user/chat` | `ChatUserController` | `xiaou-chat` |
| 聊天管理侧 REST | `/admin/chat` | `ChatAdminController` | `xiaou-chat` |
| 积分用户侧 | `/user/points` | `UserPointsController` | `xiaou-points` |
| 抽奖用户侧 | `/user/lottery` | `UserLotteryController` | `xiaou-points` |
| 积分管理侧 | `/admin/points` | `AdminPointsController` | `xiaou-points` |
| 抽奖管理侧 | `/admin/lottery` | `AdminLotteryController` | `xiaou-points` |
| 敏感词检查和管理扩展 | `/sensitive/**`、`/admin/sensitive` | 多个 `Sensitive*Controller` | `xiaou-sensitive` |
| 版本历史展示 | `/version` | `VersionHistoryController` | `xiaou-version` |
| 版本历史管理 | `/admin/version` | `VersionHistoryAdminController` | `xiaou-version` |
| 摸鱼工具用户侧 | `/moyu/**` | 多个 Moyu Controller | `xiaou-moyu` |
| 摸鱼工具管理侧 | `/admin/moyu/**` | 多个 Admin Moyu Controller | `xiaou-moyu` |

## AI Runtime 管理接口

| 能力 | 路由 | 说明 |
| --- | --- | --- |
| Runtime 配置 | `GET /admin/ai/config/runtime` | 模型、RAG、密钥脱敏、默认参数 |
| Prompt、RAG、Schema 清单 | `GET /admin/ai/config/schema-catalog` | 管理端调试台的入口数据 |
| 回归用例、最新结果、历史、健康 | `/admin/ai/config/regression/**` | AI 场景回归验证 |
| Prompt 调试 | `POST /admin/ai/config/prompt-debug` | 渲染变量并发起模型调用 |
| RAG 调试 | `POST /admin/ai/config/rag-debug` | 按 profile 检索知识库 |
| RAG 文档健康、列表、导入、导出、删除 | `/admin/ai/config/rag-service/**` | 对接 LlamaIndex sidecar |
| Runtime 指标 | `GET/DELETE /admin/ai/config/metrics` | 查询或清空调用指标 |
| AI 治理概览 | `GET /admin/ai/governance/overview` | 汇总 Prompt、Schema、回归、指标 |

## 新增接口时的登记要求

1. 新 Controller 必须同步登记到本页。
2. 新模块必须在 [源码地图](/reference/source-map) 中补模块职责。
3. 新数据表必须在 [数据表索引](/reference/database-tables) 中补业务域。
4. 如果接口返回新的错误码或 WebSocket 事件，分别更新 [响应体与错误码](/reference/response-errors) 和 [WebSocket 协议](/reference/websocket)。
