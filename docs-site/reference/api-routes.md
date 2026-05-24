# API 路由索引

本页把后端 Controller 的入口按业务域整理成索引。它不替代接口文档工具，主要用于快速判断“某个能力在哪个模块、走哪个前缀、读哪个 Controller”。

如果你已经知道要调用哪个接口，想看 `curl`、Token、分页、文件上传、WebSocket 和 AI 调试的写法，直接看 [API 调用示例](/reference/api-examples)。

如果你现在卡住的不是“接口在哪”，而是“这个接口到底是公开、用户态、管理员态，还是还要继续做 owner 归属校验”，继续看 [权限注解与角色边界索引](/reference/permission-boundaries) 会更直接。

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
| 文件上传、下载、URL、删除、列表、存在性检查；上传/删除/列表等需要登录，公开文件可匿名读 | `/file` | `FileController` | `xiaou-filestorage` |
| 管理端文件、迁移、存储配置、系统设置 | `/admin/file`、`/admin/storage`、`/admin/system` | `AdminFileController`、`AdminMigrationController`、`AdminStorageController`、`AdminSystemController` | `xiaou-filestorage` |
| 通知用户侧 | `/notification` | `NotificationController` | `xiaou-notification` |
| 通知管理侧 | `/admin/notification` | `AdminNotificationController` | `xiaou-notification` |
| 聊天用户侧 REST 和 WebSocket 票据 | `/user/chat` | `ChatUserController` | `xiaou-chat` |
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
| Bug 商店管理 | `/admin/moyu/bug-store` | `AdminBugStoreController` | `xiaou-moyu` |

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

## 详细端点分表

下面按 Controller 列出各接口的方法、路径和用途。所有路径前缀为 `/api`。

### `/auth` — AuthController（管理端认证）

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| POST | `/auth/login` | 管理员登录，返回 Token |
| POST | `/auth/logout` | 管理员登出，Token 入黑名单 |
| POST | `/auth/refresh` | 刷新令牌 |
| GET | `/auth/info` | 获取当前管理员信息、角色和权限 |
| GET | `/auth/login-logs` | 分页查询登录日志 |
| GET | `/auth/login-logs/{id}` | 登录日志详情 |
| DELETE | `/auth/login-logs` | 清空登录日志 |
| PUT | `/auth/profile` | 更新管理员个人信息 |
| PUT | `/auth/password` | 修改管理员密码 |

### `/user/auth` — UserAuthController（用户端认证）

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| POST | `/user/auth/register` | 用户注册 |
| POST | `/user/auth/login` | 用户登录 |
| POST | `/user/auth/logout` | 用户登出 |
| POST | `/user/auth/refresh` | 刷新 Token |
| GET | `/user/auth/info` | 获取当前用户信息 |
| GET | `/user/auth/check-username` | 检查用户名是否可用 |
| GET | `/user/auth/check-email` | 检查邮箱是否可用 |
| GET | `/user/auth/check-phone` | 检查手机号是否可用 |

### `/user` — UserController（用户资料）

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| GET | `/user/{userId}` | 获取用户信息 |
| GET | `/user/profile` | 获取当前用户信息（Token） |
| PUT | `/user/{userId}` | 更新用户信息 |
| PUT | `/user/profile` | 更新当前用户信息（Token） |
| PUT | `/user/{userId}/password` | 修改密码 |
| PUT | `/user/password` | 修改当前用户密码（修改后强制登出） |
| POST | `/user/avatar/upload` | 上传头像（限 jpg/png/gif，5MB） |

### `/admin/user` — AdminUserController（用户管理）

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| GET | `/admin/user/list` | 分页查询用户列表 |
| GET | `/admin/user/all` | 获取所有用户（不分页） |
| GET | `/admin/user/{userId}` | 用户详情 |
| POST | `/admin/user/create` | 创建用户 |
| PUT | `/admin/user/{userId}` | 更新用户 |
| DELETE | `/admin/user/{userId}` | 逻辑删除用户 |
| DELETE | `/admin/user/batch` | 批量逻辑删除 |
| PUT | `/admin/user/{userId}/status` | 启用/禁用 |
| PUT | `/admin/user/{userId}/reset-password` | 重置密码 |
| GET | `/admin/user/statistics` | 用户统计（总数、活跃、禁用、已删除） |

### `/user/blog` — BlogUserController（博客用户端）

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| POST | `/user/blog/open` | 开通博客（扣 50 积分） |
| GET | `/user/blog/check-status` | 检查开通状态 |
| GET | `/user/blog/config/{userId}` | 获取博客配置 |
| POST | `/user/blog/config/update` | 更新博客配置 |
| POST | `/user/blog/article/create` | 创建草稿 |
| POST | `/user/blog/article/publish` | 发布文章（扣 20 积分） |
| POST | `/user/blog/article/update/{id}` | 更新文章 |
| DELETE | `/user/blog/article/{id}` | 删除文章 |
| GET | `/user/blog/article/{id}` | 文章详情 |
| POST | `/user/blog/article/list` | 文章列表 |
| POST | `/user/blog/article/my-list` | 我的文章 |
| POST | `/user/blog/article/draft-list` | 我的草稿 |
| POST | `/user/blog/article/by-category` | 按分类查询 |
| GET | `/user/blog/categories` | 所有分类 |
| GET | `/user/blog/tags` | 所有标签 |
| GET | `/user/blog/tags/hot` | 热门标签 |

### `/admin/blog` — BlogAdminController（博客管理端）

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| GET | `/admin/blog/statistics` | 博客统计 |
| POST | `/admin/blog/article/list` | 管理端文章列表 |
| POST | `/admin/blog/article/top` | 置顶 |
| POST | `/admin/blog/article/cancel-top` | 取消置顶 |
| POST | `/admin/blog/article/update-status` | 更新状态 |
| DELETE | `/admin/blog/article/{id}` | 删除文章 |
| GET | `/admin/blog/category/list` | 分类列表 |
| POST | `/admin/blog/category/create` | 创建分类 |
| POST | `/admin/blog/category/update` | 更新分类 |
| DELETE | `/admin/blog/category/{id}` | 删除分类 |
| GET | `/admin/blog/tag/list` | 标签列表 |
| POST | `/admin/blog/tag/merge` | 合并标签 |
| DELETE | `/admin/blog/tag/{id}` | 删除标签 |

### `/user/code-pen` — CodePenUserController（代码工坊用户端，37 个端点）

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| POST | `/user/code-pen/create` | 创建作品（首次发布奖 10 积分） |
| POST | `/user/code-pen/save` | 保存作品 |
| POST | `/user/code-pen/update` | 更新作品 |
| DELETE | `/user/code-pen/{id}` | 删除作品 |
| POST | `/user/code-pen/fork` | Fork 作品（免费或付费） |
| GET | `/user/code-pen/{id}` | 作品详情 |
| POST | `/user/code-pen/my-list` | 我的作品 |
| POST | `/user/code-pen/draft-list` | 我的草稿 |
| POST | `/user/code-pen/check-fork-price` | 查询 Fork 价格 |
| POST | `/user/code-pen/income-stats` | 收益统计 |
| POST | `/user/code-pen/list` | 作品广场 |
| POST | `/user/code-pen/recommend-list` | 推荐作品 |
| GET | `/user/code-pen/hot` | 热门作品 |
| POST | `/user/code-pen/search` | 搜索 |
| POST | `/user/code-pen/by-tag` | 按标签筛选 |
| POST | `/user/code-pen/by-category` | 按分类筛选 |
| POST | `/user/code-pen/by-user` | 按用户筛选 |
| POST | `/user/code-pen/like` | 点赞 |
| POST | `/user/code-pen/unlike` | 取消点赞 |
| POST | `/user/code-pen/collect` | 收藏 |
| POST | `/user/code-pen/uncollect` | 取消收藏 |
| POST | `/user/code-pen/view` | 增加浏览数 |
| POST | `/user/code-pen/comment` | 发表评论 |
| DELETE | `/user/code-pen/comment/{id}` | 删除评论 |
| POST | `/user/code-pen/comment-list` | 评论列表 |
| POST | `/user/code-pen/folder/create` | 创建收藏夹 |
| POST | `/user/code-pen/folder/update` | 更新收藏夹 |
| DELETE | `/user/code-pen/folder/{id}` | 删除收藏夹 |
| POST | `/user/code-pen/folder/list` | 收藏夹列表 |
| POST | `/user/code-pen/folder/items` | 收藏夹内容 |
| GET | `/user/code-pen/templates` | 系统模板列表 |
| GET | `/user/code-pen/template/{id}` | 模板详情 |
| GET | `/user/code-pen/tags` | 所有标签 |
| GET | `/user/code-pen/tags/hot` | 热门标签 |

### `/admin/code-pen` — CodePenAdminController（代码工坊管理端）

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| POST | `/admin/code-pen/list` | 作品列表 |
| GET | `/admin/code-pen/{id}` | 作品详情（管理员可看代码） |
| POST | `/admin/code-pen/update-status` | 更新状态 |
| POST | `/admin/code-pen/recommend` | 设置推荐 |
| POST | `/admin/code-pen/cancel-recommend` | 取消推荐 |
| DELETE | `/admin/code-pen/{id}` | 删除作品 |
| POST | `/admin/code-pen/template/create` | 创建系统模板 |
| POST | `/admin/code-pen/template/update` | 更新模板 |
| POST | `/admin/code-pen/template/list` | 模板列表 |
| DELETE | `/admin/code-pen/template/{id}` | 删除模板 |
| POST | `/admin/code-pen/tag/create` | 创建标签 |
| POST | `/admin/code-pen/tag/update` | 更新标签 |
| DELETE | `/admin/code-pen/tag/{id}` | 删除标签 |
| POST | `/admin/code-pen/tag/merge` | 合并标签 |
| POST | `/admin/code-pen/tag/list` | 标签列表 |
| POST | `/admin/code-pen/comment/list` | 评论列表 |
| POST | `/admin/code-pen/comment/hide` | 隐藏评论 |
| DELETE | `/admin/code-pen/comment/{id}` | 删除评论 |
| GET | `/admin/code-pen/statistics` | 统计数据 |

### `/user/chat` — ChatUserController（聊天用户端）

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| POST | `/user/chat/ws-ticket` | 申请 WebSocket 一次性票据 |
| POST | `/user/chat/history` | 获取历史消息（分页） |
| POST | `/user/chat/online-count` | 在线人数 |
| POST | `/user/chat/online-users` | 在线用户列表 |
| POST | `/user/chat/message/recall` | 撤回消息（并通知 WebSocket） |

### `/admin/chat` — ChatAdminController（聊天管理端）

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| POST | `/admin/chat/messages/list` | 消息列表 |
| DELETE | `/admin/chat/messages/{id}` | 删除消息 |
| POST | `/admin/chat/messages/batch-delete` | 批量删除 |
| POST | `/admin/chat/users/online` | 在线用户 |
| POST | `/admin/chat/users/kick` | 踢人 |
| POST | `/admin/chat/users/ban` | 封禁 |
| POST | `/admin/chat/users/unban` | 解封 |
| POST | `/admin/chat/announcement` | 发布公告 |

### `/user/plan` — UserPlanController（计划打卡）

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| POST | `/user/plan/create` | 创建计划 |
| PUT | `/user/plan/update/{planId}` | 更新计划 |
| DELETE | `/user/plan/{planId}` | 删除计划 |
| GET | `/user/plan/{planId}` | 计划详情 |
| POST | `/user/plan/list` | 计划列表 |
| PUT | `/user/plan/{planId}/pause` | 暂停 |
| PUT | `/user/plan/{planId}/resume` | 恢复 |
| GET | `/user/plan/today-tasks` | 今日任务 |
| POST | `/user/plan/checkin` | 打卡 |
| GET | `/user/plan/{planId}/checkin/list` | 打卡记录 |
| GET | `/user/plan/stats/overview` | 统计概览 |

### `/user/plan/autopilot` — UserGrowthAutopilotController（成长自动驾驶）

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| GET | `/user/plan/autopilot/dashboard` | 自动驾驶看板 |
| POST | `/user/plan/autopilot/generate` | 生成本周计划 |
| POST | `/user/plan/autopilot/replan` | 一键重排 |
| POST | `/user/plan/autopilot/tasks/{taskId}/complete` | 完成任务 |
| POST | `/user/plan/autopilot/tasks/today/complete` | 批量完成今日任务 |
| POST | `/user/plan/autopilot/tasks/{taskId}/postpone` | 任务顺延一天 |

### `/user/team` — UserTeamController（学习小组，约 40 个端点）

小组 CRUD（7）、邀请码（3）、成员管理（11）、任务管理（7）、打卡（9）、排行榜（5）、讨论（8）、统计（4）。

核心路径：

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| POST | `/user/team/create` | 创建小组 |
| PUT | `/user/team/{teamId}` | 更新小组 |
| DELETE | `/user/team/{teamId}` | 解散小组 |
| GET | `/user/team/{teamId}` | 小组详情 |
| POST | `/user/team/list` | 小组广场 |
| GET | `/user/team/my` | 我的小组 |
| GET | `/user/team/created` | 我创建的 |
| GET | `/user/team/recommend` | 推荐小组 |
| GET | `/user/team/{teamId}/invite-code` | 邀请码 |
| POST | `/user/team/{teamId}/invite-code/refresh` | 刷新邀请码 |
| GET | `/user/team/by-code/{inviteCode}` | 按邀请码查小组 |
| POST | `/user/team/{teamId}/join` | 申请加入 |
| POST | `/user/team/join-by-code` | 邀请码加入 |
| POST | `/user/team/{teamId}/quit` | 退出 |
| GET | `/user/team/{teamId}/members` | 成员列表 |
| DELETE | `/user/team/{teamId}/member/{targetUserId}` | 移除成员 |
| PUT | `/user/team/{teamId}/member/{targetUserId}/role` | 设置角色 |
| PUT | `/user/team/{teamId}/transfer` | 转让组长 |
| POST | `/user/team/{teamId}/member/{targetUserId}/mute` | 禁言 |
| DELETE | `/user/team/{teamId}/member/{targetUserId}/mute` | 解除禁言 |
| POST | `/user/team/{teamId}/task` | 创建任务 |
| GET | `/user/team/{teamId}/tasks` | 任务列表 |
| POST | `/user/team/{teamId}/checkin` | 打卡 |
| POST | `/user/team/{teamId}/checkin/supplement` | 补卡 |
| GET | `/user/team/{teamId}/checkin/streak` | 连续打卡天数 |
| GET | `/user/team/{teamId}/rank/checkin` | 打卡次数排行 |
| GET | `/user/team/{teamId}/rank/streak` | 连续打卡排行 |
| GET | `/user/team/{teamId}/rank/duration` | 学习时长排行 |
| GET | `/user/team/{teamId}/rank/contribution` | 贡献值排行 |
| POST | `/user/team/{teamId}/discussion` | 创建讨论 |
| PUT | `/user/team/discussion/{id}/top` | 置顶/取消置顶 |
| PUT | `/user/team/discussion/{id}/essence` | 设为精华 |
| GET | `/user/team/{teamId}/stats` | 小组统计概览 |
| GET | `/user/team/{teamId}/stats/weekly` | 每周统计 |
| GET | `/user/team/{teamId}/stats/monthly` | 每月统计 |

### `/admin/lottery` — AdminLotteryController（抽奖管理，约 25 个端点）

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| POST | `/admin/lottery/prize/save` | 保存奖品配置 |
| GET | `/admin/lottery/prize/list` | 奖品列表 |
| POST | `/admin/lottery/prize/toggle-status` | 启用/禁用奖品 |
| POST | `/admin/lottery/prize/suspend` | 暂停/恢复奖品 |
| POST | `/admin/lottery/prize/adjust-probability` | 手动调整概率 |
| GET | `/admin/lottery/monitor/realtime` | 实时监控 |
| GET | `/admin/lottery/monitor/prize/{prizeId}` | 单品监控 |
| POST | `/admin/lottery/records` | 抽奖记录 |
| POST | `/admin/lottery/statistics/history` | 历史统计 |
| GET | `/admin/lottery/adjust-history` | 概率调整历史 |
| POST | `/admin/lottery/user/reset-limit` | 重置用户限制 |
| POST | `/admin/lottery/user/blacklist` | 设置黑名单 |
| POST | `/admin/lottery/prize/normalize` | 归一化概率 |
| GET | `/admin/lottery/prize/validate-probability` | 验证概率总和 |
| POST | `/admin/lottery/prize/batch-adjust` | 批量调整概率 |
| POST | `/admin/lottery/prize/batch-toggle` | 批量启用/禁用 |
| POST | `/admin/lottery/user/risk-list` | 风险用户列表 |
| POST | `/admin/lottery/user/evaluate-risk` | 评估风险等级 |
| POST | `/admin/lottery/user/detect-abnormal` | 检测异常行为 |
| POST | `/admin/lottery/cache/refresh` | 刷新缓存 |
| GET | `/admin/lottery/monitor/alerts` | 预警信息 |
| GET | `/admin/lottery/analysis/comprehensive` | 综合分析 |
| POST | `/admin/lottery/emergency/circuit-break` | 手动熔断 |
| POST | `/admin/lottery/emergency/resume` | 恢复服务 |
| POST | `/admin/lottery/emergency/degradation/enable` | 启用降级模式 |
| POST | `/admin/lottery/emergency/degradation/disable` | 禁用降级模式 |

### `/admin/points` — AdminPointsController（积分管理）

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| POST | `/admin/points/grant` | 发放积分 |
| POST | `/admin/points/batch-grant` | 批量发放 |
| POST | `/admin/points/detail-list` | 积分明细列表 |
| POST | `/admin/points/user-list` | 用户积分列表（排行） |
| GET | `/admin/points/statistics` | 积分统计 |
| GET | `/admin/points/user-info/{userId}` | 用户积分信息 |

### `/file` — FileController（文件存储）

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| POST | `/file/upload/single` | 单文件上传 |
| POST | `/file/upload/batch` | 批量上传 |
| GET | `/file/download/{id}` | 下载文件 |
| GET | `/file/info/{id}` | 文件信息 |
| GET | `/file/url/{id}` | 获取访问 URL |
| POST | `/file/urls` | 批量获取 URL |
| DELETE | `/file/{id}` | 删除文件 |
| GET | `/file/list` | 文件列表 |
| POST | `/file/exists` | 批量检查存在性 |

### `/sensitive/**` — 敏感词子模块

| Controller | 路由前缀 | 主要端点 |
| --- | --- | --- |
| `SensitiveWordController` | `/sensitive` | 词汇 CRUD 和检查 |
| `SensitiveWordAdminController` | `/admin/sensitive` | 管理端词汇管理 |
| `SensitiveStrategyController` | `/sensitive/strategy` | 策略 CRUD、按模块查询、刷新 |
| `SensitiveWhitelistController` | `/sensitive/whitelist` | 白名单 CRUD、批量删除、导入、刷新 |
| `SensitiveSourceController` | `/sensitive/source` | 词源 CRUD、测试连接、同步 |
| `SensitiveVersionController` | `/sensitive/version` | 版本列表、详情、回滚 |
| `SensitiveHomophoneController` | `/sensitive/homophone` | 同音字 CRUD、批量添加、刷新 |
| `SensitiveSimilarCharController` | `/sensitive/similar-char` | 形近字 CRUD、批量添加、刷新 |
| `SensitiveStatisticsController` | `/sensitive/statistics` | 概览、趋势、热词、分布、导出 |

### `/moyu/**` — 摸鱼工具用户端

| Controller | 路由前缀 | 用途 |
| --- | --- | --- |
| `DailyContentController` | `/moyu/daily-content` | 每日内容 |
| `BugStoreController` | `/moyu/bug-store` | Bug 商店 |
| `HotTopicController` | `/moyu/hot-topic` | 热榜 |
| `DeveloperCalendarController` | `/moyu/developer-calendar` | 开发者日历 |
| `SalaryCalculatorController` | `/moyu/salary-calculator` | 薪资计算器 |

### `/admin/moyu/**` — 摸鱼工具管理端

| Controller | 路由前缀 | 用途 |
| --- | --- | --- |
| `AdminDailyContentController` | `/admin/moyu/daily-content` | 每日内容管理 |
| `AdminBugStoreController` | `/admin/moyu/bug-store` | Bug 商店 CRUD、批量导入 |
| `AdminDeveloperCalendarController` | `/admin/moyu/developer-calendar` | 开发者日历管理 |

## 通用请求模式

### 分页

需要分页的接口统一使用 `pageNum`（从 1 开始）和 `pageSize`。返回结构为 `PageResult`，包含 `records`、`total`、`pageNum`、`pageSize`。

```json
{
  "records": [...],
  "total": 100,
  "pageNum": 1,
  "pageSize": 20
}
```

### 统一响应体

所有接口返回 `Result` 包装：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": ...
}
```

非 200 的 `code` 见 [响应体与错误码](/reference/response-errors)。

### 当前登录用户

用户端接口通过 `StpUserUtil.getLoginIdAsLong()` 获取当前用户 ID，不从请求参数传入。管理端用 `StpAdminUtil.getLoginIdAsLong()`。

### 校验注解

所有 Controller 使用 `@Validated` + `@Valid` + Jakarta Validation 约束（`@Positive`、`@NotBlank`、`@Min`、`@Max`、`@Email`、`@Size`、`@Pattern`）。请求体校验在 DTO 类上标注，路径参数和查询参数校验直接在方法参数上标注。

### 操作日志

管理端写操作使用 `@Log` 注解，参数包括 `module`、`type`（INSERT/UPDATE/DELETE/SELECT）、`description`。敏感操作可设置 `saveRequestData = false` 或 `saveResponseData = false`。

### 管理端权限

管理端接口统一使用 `@RequireAdmin` 注解保护。这是唯一的权限注解，项目不使用 `@SaCheckRole` 或 `@SaCheckPermission`。详见 [权限边界](/reference/permission-boundaries)。

## 接口数量统计

| 模块 | 用户端端点 | 管理端端点 | 公开端点 | 总计 |
| --- | --- | --- | --- | --- |
| `xiaou-user` | 15 | 10 | 1 | 26 |
| `xiaou-blog` | 18 | 13 | 0 | 31 |
| `xiaou-chat` | 5 | 7 | 0 | 12 |
| `xiaou-codepen` | 37 | 19 | 0 | 56 |
| `xiaou-moment` | 14 | 5 | 0 | 19 |
| `xiaou-plan` | 17 | 0 | 0 | 17 |
| `xiaou-team` | ~40 | 0 | 0 | ~40 |
| `xiaou-points` | — | 31 | 0 | 31+ |
| `xiaou-filestorage` | 9 | ~20 | 0 | ~29 |
| `xiaou-sensitive` | 0 | ~40 | 1 | ~41 |
| `xiaou-moyu` | 5 | 5+ | 0 | 10+ |
| `xiaou-system` | 0 | 9 | 0 | 9 |
| `xiaou-ai` | 0 | 9 | 0 | 9 |
| 其余模块 | — | — | — | ~50 |
| **合计** | — | — | — | **~370** |

## 新增接口时的登记要求

1. 新 Controller 必须同步登记到本页。
2. 新模块必须在 [源码地图](/reference/source-map) 中补模块职责。
3. 新数据表必须在 [数据表索引](/reference/database-tables) 中补业务域。
4. 如果接口返回新的错误码或 WebSocket 事件，分别更新 [响应体与错误码](/reference/response-errors) 和 [WebSocket 协议](/reference/websocket)。
5. 新接口如果引入新的 `@RequireAdmin` 之外的权限模式，同步更新 [权限边界](/reference/permission-boundaries)。
6. 分页接口确保使用统一的 `PageResult` 返回结构，`pageNum` 从 1 开始。
