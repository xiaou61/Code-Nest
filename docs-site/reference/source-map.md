# 源码导航

本页提供 Code-Nest 项目的完整源码目录索引，帮助开发者快速定位文件。

## 项目根目录

```
Code-Nest/
├── xiaou-application/     # Spring Boot 启动模块 (9999 端口)
├── xiaou-common/          # 公共模块 (配置、工具、异常)
├── xiaou-user-api/        # 用户 API 模块 (跨模块调用契约)
├── xiaou-sensitive-api/   # 敏感词 API 模块 (跨模块调用契约)
├── xiaou-user/            # 用户管理模块
├── xiaou-system/          # 系统管理模块 (管理员、角色、日志)
├── xiaou-ai/              # AI 功能模块 (Spring AI + LangGraph4j)
├── xiaou-interview/       # 面试题库模块
├── xiaou-mock-interview/  # 模拟面试与求职模块
├── xiaou-oj/              # 在线判题模块
├── xiaou-plan/            # 计划打卡与成长驾驶舱模块
├── xiaou-team/            # 学习小组模块
├── xiaou-flashcard/       # 闪卡记忆模块
├── xiaou-knowledge/       # 知识图谱模块
├── xiaou-learning-asset/  # 学习资产模块
├── xiaou-sql-optimizer/   # SQL 优化工作台模块
├── xiaou-community/       # 技术社区模块
├── xiaou-moment/          # 朋友圈模块
├── xiaou-blog/            # 博客模块
├── xiaou-codepen/         # 代码工坊模块
├── xiaou-resume/          # 简历模块
├── xiaou-chat/            # IM 聊天模块
├── xiaou-points/          # 积分与抽奖模块
├── xiaou-notification/    # 通知中心模块
├── xiaou-filestorage/     # 文件存储模块
├── xiaou-sensitive/       # 敏感词风控模块
├── xiaou-moyu/            # 摸鱼工具模块
├── xiaou-version/         # 版本历史模块
├── vue3-user-front/       # 用户端 Vue3 前端 (3001 端口)
├── vue3-admin-front/      # 管理端 Vue3 前端 (3000 端口)
├── docs-site/             # VitePress 文档站 (5175 端口)
├── sql/                   # 数据库脚本
├── pom.xml                # Maven 父 POM
└── README.md
```

## 后端模块结构 (统一规范)

每个业务模块遵循相同的包结构规范：

```
xiaou-{module}/
├── pom.xml
├── pom-xml-flattened         # Maven Flatten 插件生成
└── src/main/
    ├── java/com/xiaou/{module}/
    │   ├── controller/
    │   │   ├── admin/        # 管理端 Controller (@RequireAdmin)
    │   │   └── user/         # 用户端 Controller
    │   ├── service/
    │   │   └── impl/         # Service 实现
    │   ├── mapper/           # MyBatis Mapper 接口
    │   ├── domain/           # 实体类 (对应数据库表)
    │   ├── dto/              # 数据传输对象
    │   │   ├── *Request.java # 请求 DTO
    │   │   └── *Response.java# 响应 DTO
    │   └── config/           # 模块内配置 (可选)
    └── resources/
        └── mapper/           # MyBatis XML
```

## 各模块关键文件

### xiaou-application (启动模块)

| 路径 | 说明 |
| --- | --- |
| `src/main/java/com/xiaou/CodeNestApplication.java` | Spring Boot 入口 |
| `src/main/resources/application.yml` | 主配置 (端口 9999, context-path=/api) |
| `src/main/resources/application-dev.yml` | 开发环境配置 |
| `src/main/resources/spy.properties` | P6Spy SQL 日志配置 |

### xiaou-common (公共模块)

| 路径 | 说明 |
| --- | --- |
| `config/SaTokenConfig.java` | Sa-Token 认证配置 |
| `config/RedisConfig.java` | Redis 连接配置 |
| `config/CorsConfig.java` | 跨域配置 |
| `config/AiProperties.java` | AI 功能配置属性 |
| `config/NotificationAsyncConfig.java` | 异步通知线程池配置 |
| `domain/R.java` | 统一响应体 Result |
| `domain/ResultCode.java` | 响应码枚举 |
| `exception/GlobalExceptionHandler.java` | 全局异常处理 |
| `annotation/RequireAdmin.java` | 管理员权限注解 |
| `aspect/AdminAuthAspect.java` | 管理员权限 AOP 切面 |
| `util/StpUserUtil.java` | 用户端 Sa-Token 工具 |
| `util/StpAdminUtil.java` | 管理端 Sa-Token 工具 |

### xiaou-user-api / xiaou-sensitive-api (API 模块)

| 路径 | 说明 |
| --- | --- |
| `api/UserInfoApiService.java` | 跨模块用户信息查询契约 |
| `api/SensitiveCheckService.java` | 跨模块敏感词校验契约 |

### xiaou-ai (AI 模块)

| 路径 | 说明 |
| --- | --- |
| `controller/user/AiInterviewUserController.java` | 面试 AI 接口 |
| `controller/user/AiOjUserController.java` | OJ AI 接口 |
| `controller/user/AiJobBattleUserController.java` | 求职 AI 接口 |
| `controller/user/AiResumeUserController.java` | 简历 AI 接口 |
| `controller/user/AiKnowledgeUserController.java` | 知识图谱 AI 接口 |
| `controller/user/AiGrowthUserController.java` | 成长 AI 接口 |
| `controller/user/AiLearningUserController.java` | 学习 AI 接口 |
| `controller/user/AiCommunityUserController.java` | 社区 AI 接口 |
| `controller/admin/AiAdminController.java` | AI 配置管理接口 |
| `controller/AiGovernanceController.java` | AI 治理接口 |
| `prompt/AiPromptSpec.java` | Prompt 模板注册枚举 |
| `structured/AiStructuredOutputSpec.java` | 结构化输出注册枚举 |
| `graph/interview/InterviewState.java` | 面试状态机 |
| `graph/jobbattle/JobBattleState.java` | 求职状态机 |
| `graph/sql/SqlOptimizeState.java` | SQL 优化状态机 |
| `dto/interview/` | 面试 Schema DTO |
| `dto/oj/` | OJ Schema DTO |
| `dto/jobbattle/` | 求职 Schema DTO |
| `dto/resume/` | 简历 Schema DTO |
| `dto/knowledge/` | 知识图谱 Schema DTO |
| `dto/growth/` | 成长 Schema DTO |
| `dto/learning/` | 学习 Schema DTO |
| `dto/sql/` | SQL Schema DTO |
| `dto/community/` | 社区 Schema DTO |
| `dto/governance/` | AI 治理 DTO |

### xiaou-interview (面试题库)

| 路径 | 说明 |
| --- | --- |
| `controller/user/InterviewUserController.java` | 用户端面试题接口 |
| `controller/admin/InterviewAdminController.java` | 管理端面试题接口 |
| `service/impl/InterviewQuestionServiceImpl.java` | 题目 CRUD |
| `service/impl/InterviewLearnRecordServiceImpl.java` | 学习记录 |
| `service/impl/InterviewFavoriteServiceImpl.java` | 收藏管理 |
| `service/impl/InterviewMasteryServiceImpl.java` | 掌握度追踪 |
| `mapper/InterviewQuestionMapper.java` | 题目 Mapper |
| `domain/InterviewQuestion.java` | 题目实体 |

### xiaou-mock-interview (模拟面试与求职)

| 路径 | 说明 |
| --- | --- |
| `controller/user/MockInterviewUserController.java` | 模拟面试接口 |
| `controller/user/CareerLoopUserController.java` | 求职闭环接口 |
| `controller/user/JobBattleUserController.java` | 求职作战接口 |
| `controller/admin/MockInterviewAdminController.java` | 管理端面试配置 |
| `service/impl/MockInterviewSessionServiceImpl.java` | 面试会话管理 |
| `domain/MockInterviewSession.java` | 面试会话实体 |

### xiaou-oj (在线判题)

| 路径 | 说明 |
| --- | --- |
| `controller/user/OjUserController.java` | 用户端 OJ 接口 |
| `controller/admin/OjAdminController.java` | 管理端 OJ 接口 |
| `service/impl/OjProblemServiceImpl.java` | 题目管理 |
| `service/impl/OjSubmissionServiceImpl.java` | 提交与判题 |
| `service/impl/OjContestServiceImpl.java` | 赛事管理 |
| `service/impl/OjJudgeServiceImpl.java` | 判题核心逻辑 |

### xiaou-plan (计划与成长)

| 路径 | 说明 |
| --- | --- |
| `controller/user/UserPlanController.java` | 计划打卡接口 |
| `controller/user/UserGrowthAutopilotController.java` | 成长自动驾驶接口 |
| `service/impl/PlanServiceImpl.java` | 计划核心逻辑 |
| `dto/PlanCreateRequest.java` | 创建计划请求 |
| `dto/PlanCheckinRequest.java` | 打卡请求 |
| `dto/GrowthAutopilotGenerateRequest.java` | 自动驾驶生成请求 |

### xiaou-team (学习小组)

| 路径 | 说明 |
| --- | --- |
| `controller/user/UserTeamController.java` | 小组接口 |
| `service/impl/StudyTeamServiceImpl.java` | 小组管理 |
| `service/impl/TeamCheckinServiceImpl.java` | 打卡 |
| `service/impl/TeamDiscussionServiceImpl.java` | 讨论 |
| `service/impl/TeamMemberServiceImpl.java` | 成员管理 |
| `service/impl/TeamRankServiceImpl.java` | 排行 |
| `service/impl/TeamStatsServiceImpl.java` | 统计 |
| `service/impl/TeamTaskServiceImpl.java` | 任务 |
| `mapper/StudyTeamMapper.java` | 小组 Mapper |
| `mapper/StudyTeamCheckinMapper.java` | 打卡 Mapper |
| `mapper/StudyTeamDiscussionMapper.java` | 讨论 Mapper |
| `mapper/StudyTeamMemberMapper.java` | 成员 Mapper |
| `mapper/StudyTeamTaskMapper.java` | 任务 Mapper |
| `mapper/StudyTeamDiscussionLikeMapper.java` | 讨论点赞 Mapper |

### xiaou-blog (博客)

| 路径 | 说明 |
| --- | --- |
| `controller/user/BlogUserController.java` | 用户端博客接口 |
| `controller/admin/BlogAdminController.java` | 管理端博客接口 |
| `service/impl/BlogArticleServiceImpl.java` | 文章核心逻辑 |
| `service/impl/BlogTagServiceImpl.java` | 标签管理 |
| `mapper/BlogArticleMapper.java` | 文章 Mapper |
| `dto/ArticleListRequest.java` | 文章列表请求 |
| `dto/AdminArticleListRequest.java` | 管理端文章列表请求 |

### xiaou-codepen (代码工坊)

| 路径 | 说明 |
| --- | --- |
| `controller/user/CodePenUserController.java` | 用户端代码工坊接口 |
| `controller/admin/CodePenAdminController.java` | 管理端代码工坊接口 |
| `service/impl/CodePenServiceImpl.java` | 代码核心逻辑 |
| `service/impl/CodePenFolderServiceImpl.java` | 文件夹管理 |
| `service/impl/CodePenTagServiceImpl.java` | 标签管理 |
| `mapper/CodePenMapper.java` | 代码 Mapper |
| `mapper/CodePenForkTransactionMapper.java` | Fork 事务 Mapper |
| `dto/CodePenTagCreateRequest.java` | 标签创建请求 |
| `dto/CodePenTagUpdateRequest.java` | 标签更新请求 |
| `dto/FolderCreateRequest.java` | 文件夹创建请求 |
| `dto/FolderUpdateRequest.java` | 文件夹更新请求 |

### xiaou-chat (IM 聊天)

| 路径 | 说明 |
| --- | --- |
| `controller/user/ChatUserController.java` | 用户端聊天接口 |
| `dto/ChatHistoryRequest.java` | 历史消息请求 |
| `dto/ChatRecallRequest.java` | 消息撤回请求 |

### xiaou-points (积分与抽奖)

| 路径 | 说明 |
| --- | --- |
| `controller/admin/AdminLotteryController.java` | 抽奖管理接口 |
| `controller/admin/AdminPointsController.java` | 积分管理接口 |
| `service/impl/LotteryServiceImpl.java` | 抽奖核心逻辑 |
| `service/impl/LotteryAdminServiceImpl.java` | 抽奖管理逻辑 |
| `dto/lottery/LotteryDrawResponse.java` | 抽奖结果 |
| `dto/lottery/LotteryStatisticsResponse.java` | 抽奖统计 |
| `dto/lottery/LotteryRecordQueryRequest.java` | 记录查询请求 |
| `dto/lottery/RiskUserQueryRequest.java` | 风控用户查询 |
| `dto/PointsDetailQueryRequest.java` | 积分明细查询 |
| `dto/UserPointsListRequest.java` | 用户积分列表 |

### xiaou-sensitive (敏感词)

| 路径 | 说明 |
| --- | --- |
| `controller/admin/SensitiveAdminController.java` | 管理端敏感词接口 |
| `service/impl/SensitiveWordServiceImpl.java` | 敏感词核心逻辑 |

### xiaou-moyu (摸鱼工具)

| 路径 | 说明 |
| --- | --- |
| `controller/admin/AdminBugStoreController.java` | Bug 商店管理 |
| `service/impl/BugStoreServiceImpl.java` | Bug 商店逻辑 |
| `dto/BugItemQueryRequest.java` | Bug 查询请求 |

### xiaou-system (系统管理)

| 路径 | 说明 |
| --- | --- |
| `controller/AuthController.java` | 认证接口 (登录/注册) |

### xiaou-user (用户管理)

| 路径 | 说明 |
| --- | --- |
| `controller/UserAuthController.java` | 用户认证接口 |
| `controller/UserController.java` | 用户信息接口 |
| `controller/AdminUserController.java` | 管理端用户接口 |

## 前端项目结构

### vue3-user-front (用户端)

```
vue3-user-front/
├── package.json              # code-nest-user-desktop v2.2.1
├── vite.config.js            # Vite 配置 (端口 3001, 代理 /api→9999)
├── electron/                 # Electron 打包配置
├── src/
│   ├── App.vue               # 根组件 (RouterView + 页面切换动画)
│   ├── main.js               # 入口 (Vue3 + Element Plus + Pinia + Router)
│   ├── router/index.js       # 路由定义 + 守卫 (908 行, ~60 路由)
│   ├── stores/               # Pinia Store
│   │   └── user.js           # 用户状态 (Token, isLoggedIn, userInfo)
│   ├── utils/
│   │   └── request.js        # Axios 封装 (baseURL=/api, Token 注入)
│   ├── config/
│   │   └── navigation.js     # 命令面板导航配置
│   ├── api/                  # API 服务文件 (按模块分文件)
│   │   ├── interview.js
│   │   ├── oj.js
│   │   ├── community.js
│   │   ├── blog.js
│   │   ├── codepen.js
│   │   ├── resume.js
│   │   ├── chat.js
│   │   ├── points.js
│   │   ├── plan.js
│   │   ├── team.js
│   │   ├── flashcard.js
│   │   ├── knowledge.js
│   │   ├── notification.js
│   │   ├── learning-asset.js
│   │   ├── sql-optimizer.js
│   │   ├── moyu.js
│   │   └── user.js
│   └── views/                # 页面组件 (按功能模块分目录)
│       ├── interview/
│       ├── oj/
│       ├── community/
│       ├── blog/
│       ├── codepen/
│       ├── resume/
│       ├── chat/
│       ├── points/
│       ├── lottery/
│       ├── plan/
│       ├── team/
│       ├── flashcard/
│       ├── knowledge/
│       ├── notification/
│       ├── learning/
│       ├── sql-optimizer/
│       ├── moyu-tools/
│       ├── dev-tools/
│       └── profile/
└── public/
```

### vue3-admin-front (管理端)

```
vue3-admin-front/
├── package.json              # code-nest-admin-desktop v2.2.1
├── vite.config.js            # Vite 配置 (端口 3000, 代理 /api→9999)
├── electron/                 # Electron 打包配置
├── src/
│   ├── App.vue               # 根组件
│   ├── main.js               # 入口
│   ├── router/index.js       # 路由定义 + 守卫 (634 行, ~40 路由)
│   ├── layout/               # Layout 组件 (侧边栏 + 内容区)
│   ├── stores/
│   │   └── user.js           # 管理员状态 (Token 刷新, 跨 Tab 广播)
│   ├── utils/
│   │   └── request.js        # Axios 封装 (同用户端, Token 键名不同)
│   ├── api/                  # API 服务文件
│   └── views/                # 页面组件
│       ├── dashboard/
│       ├── user/
│       ├── interview/
│       ├── community/
│       ├── oj/
│       ├── blog/
│       ├── codepen/
│       ├── resume/
│       ├── points/
│       ├── lottery/
│       ├── moyu/
│       ├── sensitive/
│       ├── filestorage/
│       ├── system/
│       ├── logs/
│       └── profile/
└── public/
```

## SQL 脚本目录

```
sql/
├── MySql/
│   └── code_nest.sql         # 主库基线 (136 表，加上增量共 142 表)
├── v1.2.0/                   # 基础表结构
├── v1.2.1/                   # 知识图谱、敏感词
├── v1.3.0/                   # Bug 商店、薪资计算器
├── v1.4.0/                   # 积分、IM 聊天
├── v1.5.0/                   # 博客、抽奖、社区
├── v1.6.0/                   # 简历、代码工坊
├── v1.6.1/                   # 计划打卡
├── v1.6.3/                   # AI 模拟面试
├── v1.7.0/                   # 学习追踪、聊天升级
├── v1.7.1/                   # 学习小组
├── v1.7.2/                   # 闪卡
├── v1.8.0/                   # OJ 基础
├── v1.8.1/                   # OJ 赛事
├── v1.8.2/                   # 成长自动驾驶、求职闭环
├── v1.8.3/                   # 学习资产候选
└── v1.8.4/                   # 学习资产转化
```

## 文档站目录

```
docs-site/
├── .vitepress/config.mts     # VitePress 配置 (侧边栏 + 导航)
├── index.md                  # 首页
├── guide/                    # 使用指南
│   ├── quick-start.md
│   ├── local-dev.md
│   └── ...
├── architecture/             # 架构文档
│   ├── overview.md
│   ├── backend-modules.md
│   ├── frontend-apps.md
│   └── database.md
├── modules/                  # 模块文档 (25 个模块页)
├── operations/               # 运维文档
│   ├── troubleshooting.md
│   └── incident-response.md
└── reference/                # 参考文档
    ├── api-routes.md
    ├── frontend-routes.md
    ├── database-tables.md
    ├── ai-schemas.md
    ├── websocket.md
    ├── source-map.md
    └── ...
```

## 快速定位指南

| 我要找... | 去哪里找 |
| --- | --- |
| 某个接口的 Controller | `xiaou-{module}/controller/user/ 或 admin/` |
| 某个接口的业务逻辑 | `xiaou-{module}/service/impl/` |
| 某个查询的 SQL | `xiaou-{module}/src/main/resources/mapper/` |
| 某个表的实体定义 | `xiaou-{module}/domain/` |
| 某个请求的参数结构 | `xiaou-{module}/dto/*Request.java` |
| 某个响应的数据结构 | `xiaou-{module}/dto/*Response.java` |
| AI Prompt 模板 | `xiaou-ai/prompt/AiPromptSpec.java` |
| AI Schema 定义 | `xiaou-ai/dto/` + `xiaou-ai/structured/AiStructuredOutputSpec.java` |
| 认证拦截器 | `xiaou-common/aspect/AdminAuthAspect.java` |
| 全局异常处理 | `xiaou-common/exception/GlobalExceptionHandler.java` |
| 数据库建表脚本 | `sql/MySql/code_nest.sql` |
| 增量变更脚本 | `sql/v{version}/` |
| 前端路由定义 | `vue3-{app}/src/router/index.js` |
| 前端 API 调用 | `vue3-{app}/src/api/` |
| 前端页面组件 | `vue3-{app}/src/views/` |
| 前端状态管理 | `vue3-{app}/src/stores/` |
| Vite 代理配置 | `vue3-{app}/vite.config.js` |
| Axios 请求封装 | `vue3-{app}/src/utils/request.js` |
