# 后端模块详解

Code-Nest 后端由 24 个 Maven 子模块组成，按职责分为**基础设施模块**、**业务模块**和**API 契约模块**三类。

## 模块分类与依赖方向

```text
xiaou-application (聚合启动)
    │
    ├── 基础设施层
    │   ├── xiaou-common          ← 全局工具、配置、异常、鉴权
    │   ├── xiaou-user-api        ← 用户信息接口契约
    │   └── xiaou-sensitive-api   ← 敏感词检测接口契约
    │
    ├── 核心实现层
    │   ├── xiaou-user            ← 用户/认证 (实现 user-api)
    │   ├── xiaou-sensitive       ← 敏感词 (实现 sensitive-api)
    │   └── xiaou-system          ← 系统管理/日志/仪表盘
    │
    └── 业务功能层
        ├── xiaou-ai              ← AI Prompt/RAG/Graph
        ├── xiaou-oj              ← 在线判题
        ├── xiaou-interview       ← 面试题库
        ├── xiaou-mock-interview  ← 模拟面试/对战
        ├── xiaou-plan            ← 学习计划/成长自动导航
        ├── xiaou-team            ← 学习小组/签到/讨论
        ├── xiaou-flashcard       ← 闪卡记忆
        ├── xiaou-knowledge       ← 知识库
        ├── xiaou-learning-asset  ← 学习资源
        ├── xiaou-blog            ← 博客
        ├── xiaou-community       ← 社区问答
        ├── xiaou-moment          ← 朋友圈动态
        ├── xiaou-chat            ← 实时聊天/WebSocket
        ├── xiaou-codepen         ← 代码工坊
        ├── xiaou-notification    ← 站内通知
        ├── xiaou-points          ← 积分/抽奖
        ├── xiaou-moyu            ← 摸鱼小游戏
        ├── xiaou-resume          ← 简历
        ├── xiaou-filestorage     ← 文件上传下载
        ├── xiaou-version         ← 版本历史
        └── xiaou-sql-optimizer   ← SQL 优化器
```

**依赖方向规则**：
- 业务模块 → `xiaou-common`（必须）
- 业务模块 → `xiaou-user-api`（需用户信息时）
- 业务模块 → `xiaou-sensitive-api`（需内容审查时）
- **禁止**：业务模块 → 其他业务模块的实现（防循环依赖）

## 模块详情

### 基础设施模块

#### xiaou-common

全局公共模块，所有业务模块的基础依赖。

| 包 | 职责 |
|----|------|
| `config/` | SaTokenConfig, RedisConfig, CorsConfig, MybatisPlusConfig, RestTemplateConfig, AiProperties, NotificationAsyncConfig, LocalFileResourceConfig |
| `satoken/` | AdminAuthAspect (@RequireAdmin 切面) → 详见 [权限注解与角色边界索引](/reference/permission-boundaries) |
| `exception/` | GlobalExceptionHandler, BusinessException, ResultCode → 详见 [响应与错误码](/reference/response-errors) |
| `enums/` | StatusEnum, NotificationStatusEnum |
| `domain/` | Result 统一响应体 → 详见 [响应与错误码](/reference/response-errors) |
| `utils/` | 通用工具类 |

#### xiaou-user-api

用户信息接口契约模块，定义跨模块调用的用户信息接口。

```text
com.xiaou.user.api
└── UserInfoApiService    ← 接口：getUserInfo(id) → 昵称/头像
```

**消费方**：chat, team, blog, community, oj, points, notification, plan, moment, codepen, interview, mock-interview, flashcard, knowledge, resume, moyu

#### xiaou-sensitive-api

敏感词检测接口契约模块（实现详见 [敏感词风控](/modules/sensitive)）。

```text
com.xiaou.sensitive.api
└── SensitiveCheckService ← 接口：check(text) → 敏感词检测结果
```

**消费方**：blog, community, moment, chat, team, plan, codepen, interview, mock-interview

### 核心实现模块

#### xiaou-user

| 维度 | 说明 |
|------|------|
| API 前缀 | `/api/user/auth` (认证), `/api/user` (用户信息), `/api/admin/user` (管理), `/api/captcha` (验证码) |
| Controller | UserAuthController, UserController, AdminUserController |
| 核心功能 | 注册/登录/登出、个人信息管理、管理员用户管理 |
| 实现接口 | `xiaou-user-api` 的 UserInfoApiService |
| 关键表 | `user`, `user_role` |

#### xiaou-system

| 维度 | 说明 |
|------|------|
| API 前缀 | `/api/auth` (管理端认证), `/api/admin/dashboard` (仪表盘), `/api/log` (日志), `/api/admin/ai/config` (AI 配置管理) |
| Controller | AuthController (管理端认证), DashboardController, LogController, AiConfigController |
| 核心功能 | 管理员登录、仪表盘统计、操作日志、AI 配置管理 |
| 关键表 | `system_log` |
| @RequireAdmin 方法 | 29 个 (Log×8 + Dashboard×1 + AiConfig×20) |

#### xiaou-sensitive

| 维度 | 说明 |
|------|------|
| API 前缀 | `/api/sensitive` (检测), `/api/sensitive/strategy` (策略), `/api/sensitive/whitelist` (白名单), `/api/sensitive/stats` (统计), `/api/admin/sensitive` (管理) |
| Controller | SensitiveCheckController, SensitiveStrategyController, SensitiveWhitelistController, SensitiveStatsController, AdminSensitiveController |
| 核心功能 | 敏感词检测（DFA 算法）、敏感词库管理 |
| 实现接口 | `xiaou-sensitive-api` 的 SensitiveCheckService |

### 业务功能模块

#### xiaou-ai — AI 能力

| 维度 | 说明 |
|------|------|
| API 前缀 | `/api/admin/ai/config` (管理), `/api/admin/ai/governance` (治理) |
| Controller | AiConfigController (在 xiaou-system), AiGovernanceController |
| 核心功能 | Prompt 编排与注册、RAG 知识检索、LangGraph4j 图执行、结构化输出 |
| 包结构 | `controller/`, `service/`, `dto/`, `domain/`, `mapper/`, `rag/`, `graph/`, `prompt/`, `schema/`, `runner/` |
| 特色 | 目录式 Prompt 注册、RAG Profile 配置、AI 回归测试框架 (18 用例) |
| @RequireAdmin 方法 | 1 个 (AiGovernanceController) |

#### xiaou-oj — 在线判题

| 维度 | 说明 |
|------|------|
| API 前缀 | `/api/oj` (用户/公开), `/api/oj/contests` (赛事), `/api/admin/oj/problems` (题目管理), `/api/admin/oj/contests` (赛事管理) |
| Controller | OjUserController, OjContestUserController, OjProblemAdminController, OjContestAdminController, OjSubmissionAdminController |
| 核心功能 | 题目 CRUD、代码提交判题、Judge0 沙箱执行、提交记录查询、赛事管理 |
| 包结构 | `controller/`, `service/`, `dto/`, `domain/`, `mapper/`, `enums/` |
| 状态机 | SubmissionStatus: PENDING → JUDGING → AC/WA/TLE/MLE/CE/RE/SE |
| @RequireAdmin 方法 | OjAdminController 中 |

#### xiaou-chat — 实时聊天

| 维度 | 说明 |
|------|------|
| API 前缀 | `/api/user/chat` (用户), `/api/admin/chat` (管理) |
| Controller | ChatUserController, ChatAdminController |
| 核心功能 | 一对一/群组聊天、消息收发、历史记录、消息撤回、WebSocket 实时推送 |
| 包结构 | `controller/`, `service/`, `dto/`, `domain/`, `mapper/`, `config/`, `websocket/` |
| 特色 | STOMP + WebSocket 实时通信，聊天记录分页查询 |
| 关键配置 | WebSocketConfig (STOMP 端点 `/ws`) |

#### xiaou-plan — 学习计划

| 维度 | 说明 |
|------|------|
| API 前缀 | `/api/user/plan` (用户), `/api/user/plan/autopilot` (自动驾驶) |
| Controller | UserPlanController, UserGrowthAutopilotController |
| 核心功能 | 计划创建/编辑/删除、计划签到、成长自动导航 (AI 生成) |
| 包结构 | `controller/`, `service/`, `dto/`, `domain/`, `mapper/` |
| 特色 | GrowthAutopilot 调用 AI 模块自动生成学习计划 |
| @RequireAdmin 方法 | 无 (纯用户端) |

#### xiaou-team — 学习小组

| 维度 | 说明 |
|------|------|
| API 前缀 | `/api/user/team` (用户) |
| Controller | UserTeamController |
| 核心功能 | 小组创建/加入/退出、签到、讨论、任务、排行榜、统计 |
| 包结构 | `controller/`, `service/`, `dto/`, `domain/`, `mapper/` |
| 子服务 | TeamMemberService, TeamCheckinService, TeamDiscussionService, TeamTaskService, TeamRankService, TeamStatsService |
| @RequireAdmin 方法 | 无 (纯用户端) |

#### xiaou-blog — 博客

| 维度 | 说明 |
|------|------|
| API 前缀 | `/api/user/blog` (用户), `/api/admin/blog` (管理) |
| Controller | BlogUserController, BlogAdminController |
| 核心功能 | 文章发布/编辑、分类管理、标签管理、文章列表/搜索 |
| @RequireAdmin 方法 | BlogAdminController 中 |

#### xiaou-codepen — 代码工坊

| 维度 | 说明 |
|------|------|
| API 前缀 | `/api/user/code-pen` (用户), `/api/admin/code-pen` (管理) |
| Controller | CodePenUserController, CodePenAdminController |
| 核心功能 | 在线代码编辑、Fork、标签、文件夹管理 |
| 包结构 | `controller/`, `service/`, `dto/`, `domain/`, `mapper/` |
| 特色 | CodePenForkTransaction 记录 Fork 关系链 |

#### xiaou-community — 社区问答

| 维度 | 说明 |
|------|------|
| API 前缀 | `/api/community/posts` (帖子), `/api/community/comments` (评论), `/api/community/user` (用户行为), `/api/admin/community/posts` (管理) |
| 核心功能 | 问题发布、回答、采纳、点赞 |

#### xiaou-points — 积分与抽奖

| 维度 | 说明 |
|------|------|
| API 前缀 | `/api/user/points` (积分), `/api/user/lottery` (抽奖), `/api/admin/points` (积分管理), `/api/admin/lottery` (抽奖管理) |
| Controller | UserPointsController, UserLotteryController, AdminPointsController, AdminLotteryController |
| 核心功能 | 积分获取/消费/查询、抽奖、管理端风控 |
| @RequireAdmin 方法 | 28 个 (Points×6 + Lottery×22) |
| 特色 | 抽奖风控 (风险用户检测)、实时监控、奖品发放 |

#### xiaou-notification — 站内通知

| 维度 | 说明 |
|------|------|
| API 前缀 | `/api/notification` (用户), `/api/admin/notification` (管理) |
| 核心功能 | 通知创建、已读/未读、通知列表 |
| 状态 | UNREAD → READ → DELETED (单向流转) |

#### xiaou-moyu — 摸鱼小游戏

| 维度 | 说明 |
|------|------|
| API 前缀 | `/api/moyu/bug-store` 等 (用户), `/api/admin/moyu/bug-store` 等 (管理) |
| Controller | AdminBugStoreController |
| 核心功能 | Bug 商店 (小游戏)、题目管理 |
| @RequireAdmin 方法 | AdminBugStoreController 中 |

#### xiaou-filestorage — 文件存储

| 维度 | 说明 |
|------|------|
| API 前缀 | `/api/file` (文件操作), `/api/admin/file` (文件管理), `/api/admin/storage` (存储策略), `/api/admin/migration` (迁移) |
| 核心功能 | 文件上传/下载、本地存储/S3 兼容切换 |
| 配置 | LocalFileResourceConfig (本地文件资源映射) |

#### 其他模块

| 模块 | API 前缀 | 核心功能 |
|------|----------|----------|
| xiaou-interview | `/api/interview/categories` (公开), `/api/admin/interview/*` (管理) | 面试题库管理 |
| xiaou-mock-interview | `/api/user/mock-interview` (用户), `/api/user/job-battle` (求职作战台), `/api/user/career-loop` (求职闭环), `/api/admin/mock-interview` (管理) | 模拟面试/求职对战 |
| xiaou-moment | `/api/user/moments` (用户), `/api/admin/moments` (管理) | 朋友圈动态发布 |
| xiaou-flashcard | `/api/flashcard/deck` (卡组), `/api/flashcard/card` (卡片), `/api/flashcard/study` (学习), `/api/pub/flashcard/deck` (公开) | 闪卡记忆与复习 |
| xiaou-knowledge | `/api/pub/knowledge/maps` (公开), `/api/admin/knowledge/maps` (管理) | 知识库管理 |
| xiaou-learning-asset | `/api/user/learning-assets` (用户), `/api/admin/learning-assets` (管理) | 学习资源管理 |
| xiaou-resume | `/api/resume` (用户), `/api/admin/resume` (管理) | 简历编辑 |
| xiaou-version | `/api/version` (公开), `/api/admin/version` (管理) | 版本历史记录 |
| xiaou-sql-optimizer | `/api/user/sql-optimizer` (用户) | SQL 优化建议 |

## 统一包结构约定

每个业务模块遵循相同的包结构约定：

```text
com.xiaou.{module}
├── controller/
│   ├── user/           ← 用户端 API (@UserAuth 校验)
│   │   └── {Name}UserController.java
│   └── admin/          ← 管理端 API (@RequireAdmin)
│       └── Admin{Name}Controller.java
├── service/
│   ├── {Name}Service.java        ← 接口
│   └── impl/
│       └── {Name}ServiceImpl.java ← 实现
├── dto/                ← 请求/响应 DTO
│   ├── {Name}Request.java
│   └── {Name}Response.java
├── domain/             ← 实体类 (@TableName)
├── mapper/             ← MyBatis-Plus Mapper
│   └── {Name}Mapper.java
└── resources/mapper/   ← Mapper XML
    └── {Name}Mapper.xml
```

**特殊包**（部分模块）：

| 模块 | 特殊包 | 用途 |
|------|--------|------|
| xiaou-ai | `prompt/`, `schema/`, `rag/`, `graph/`, `runner/` | AI Prompt 编排/RAG/图执行 |
| xiaou-chat | `config/`, `websocket/` | WebSocket 配置与处理 |
| xiaou-common | `satoken/`, `exception/`, `enums/` | 全局鉴权/异常/枚举 |
| xiaou-oj | `enums/` | SubmissionStatus 等判题枚举 |

## API 前缀分配

所有模块的 API 前缀由 `server.servlet.context-path=/api` 加上 Controller 的 `@RequestMapping` 组成。实际前缀按路由分层：

| 模式 | 完整前缀示例 | Controller 前缀 | 鉴权 | 说明 |
|------|------|------|------|------|
| 用户端 | `/api/user/chat`、`/api/user/blog`、`/api/user/plan` | `/user/{module}` | StpUserUtil | 需登录，资源归属校验 |
| 管理端 | `/api/admin/chat`、`/api/admin/blog`、`/api/admin/points` | `/admin/{module}` | @RequireAdmin → 详见 [权限注解与角色边界索引](/reference/permission-boundaries) | 需管理员角色 |
| 公开接口 | `/api/community`、`/api/oj`、`/api/interview/categories` | `/community`、`/oj`、`/interview/**` | 按方法判断 | 部分无需登录 |
| 用户认证 | `/api/user/auth` | `/user/auth` | 无 | 登录/注册 |
| 管理认证 | `/api/auth` | `/auth` | 无 | 管理员登录 |
| 验证码 | `/api/captcha` | `/captcha` | 无 | 图形验证码 |
| 平台公共能力 | `/api/file`、`/api/notification`、`/api/version` | `/file`、`/notification`、`/version` | 按方法判断 | 上传需登录，读取可匿名 |
| WebSocket | `/ws` | — | STOMP | 聊天实时通信 |

> 各模块的详细端点列表见 [API 路由索引](/reference/api-routes)。下表只列每个模块的主前缀。

## 模块间调用频次

以下是运行时最常见的跨模块调用场景：

| 调用方 | 接口 | 频次 | 场景 |
|--------|------|------|------|
| 几乎所有模块 | UserInfoApiService | 极高 | 展示用户昵称/头像 |
| 内容类模块 | SensitiveCheckService → 详见 [敏感词风控](/modules/sensitive) | 高 | 内容发布前审查 |
| team/plan | points Service | 中 | 签到积分奖励 |
| plan | ai Service | 低 | 成长自动导航 |
| 多模块 | notification Service | 中 | 站内通知推送 |

## 启动顺序

所有模块由 `xiaou-application` 聚合为单 JAR，启动时 Spring Boot 自动扫描：

```text
CodeNestApplication.java (@SpringBootApplication)
  → @ComponentScan("com.xiaou")  扫描所有包
  → @MapperScan("com.xiaou")     注册所有 Mapper
  → @EnableScheduling             启用定时任务
  → StartupApplicationListener    启动完成日志
```

**启动依赖**（必须先就绪）：

1. MySQL — 所有 Mapper 初始化
2. Redis — Sa-Token 会话存储
3. 本地文件目录 — 文件上传下载

## 源码导航

| 路径 | 说明 |
|------|------|
| `pom.xml` | 根 POM，定义 24 个子模块 |
| `xiaou-application/pom.xml` | 聚合所有模块的依赖 |
| `xiaou-application/.../CodeNestApplication.java` | Spring Boot 启动类 |
| `xiaou-application/.../application.yml` | 主配置 (端口/MyBatis/Sa-Token) |
| `xiaou-application/.../application-dev.yml` | 开发环境配置 (MySQL/Redis/AI) |
| `xiaou-common/src/main/java/com/xiaou/common/` | 全局公共代码 |
| `xiaou-user-api/src/main/java/com/xiaou/user/api/` | 用户信息接口契约 |
| `xiaou-sensitive-api/src/main/java/com/xiaou/sensitive/api/` | 敏感词检测接口契约 → 详见 [敏感词风控](/modules/sensitive) |


## 相关文档

| 文档 | 说明 |
| --- | --- |
| [架构总览](/architecture/overview) | 整体架构和部署拓扑 |
| [数据库与脚本](/architecture/database) | 数据库设计和版本管理 |
| [源码地图](/reference/source-map) | 全项目代码索引 |
| [模块依赖地图](/reference/module-dependencies) | 模块间依赖关系 |
| [模块总览](/modules/) | 各业务模块详解 |
