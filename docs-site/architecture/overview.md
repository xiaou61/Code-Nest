# 整体架构

Code-Nest 是一个基于 Spring Boot 3.4.4 + Vue 3 的全栈学习成长平台，采用**单体模块化架构**（Modular Monolith）。根 POM 声明 36 个 Maven 子模块，由 `xiaou-bootstrap` 组装运行时并产出唯一可执行 JAR；`xiaou-application` 只承载跨领域应用编排。

## 技术栈总览

| 层级 | 技术 | 版本/说明 |
|------|------|-----------|
| 运行时 | Java | 17 |
| 后端框架 | Spring Boot | 3.4.4 |
| ORM | MyBatis-Plus | 3.5.x |
| 数据库 | MySQL | 8.0+，当前主库基线 161 张表 |
| 缓存 | Redis | 7.x + Redisson |
| 鉴权 | Sa-Token | 多端鉴权 (User + Admin) |
| AI 接入 | LangChain4j + LangGraph4j + LlamaIndex | Prompt、图执行、RAG 和结构化输出 |
| 文件存储 | 本地磁盘 / S3 兼容 | 可切换 |
| 实时通信 | Spring WebSocket | 原生 JSON 消息协议 + 一次性票据 |
| 前端 (用户端) | Vue 3 + Vite | Element Plus |
| 前端 (管理端) | Vue 3 + Vite | Element Plus |
| 文档站 | VitePress | docs-site/ |

## 架构全景图

```
                          ┌─────────────────────────────────────┐
                          │           Nginx / 反向代理            │
                          └──────────────┬──────────────────────┘
                                         │
                    ┌────────────────────┼────────────────────┐
                    │                    │                     │
            ┌───────▼──────┐    ┌───────▼──────┐    ┌────────▼───────┐
            │  vue3-user    │    │  vue3-admin  │    │  docs-site     │
            │  -front       │    │  -front      │    │  (VitePress)   │
            │  :3001        │    │  :3000       │    │  :5175         │
            └───────┬──────┘    └───────┬──────┘    └────────────────┘
                    │                    │
                    └────────┬───────────┘
                             │ HTTP / WebSocket
                    ┌────────▼────────────────────────────────┐
                    │        xiaou-bootstrap (Spring Boot)     │
                    │        CodeNestApplication.java          │
                    │        Port: 9999                        │
                    ├──────────────────────────────────────────┤
                    │                                         │
                    │  ┌─────────────┐  ┌──────────────────┐  │
                    │  │ Sa-Token     │  │ GlobalException  │  │
                    │  │ 双端鉴权     │  │ Handler          │  │
                    │  │ StpUserUtil  │  │ Result统一响应    │  │
                    │  │ StpAdminUtil │  │                  │  │
                    │  └─────────────┘  └──────────────────┘  │
                    │                                         │
                    │  ┌────────────────────────────────────┐  │
                    │  │         业务模块层 (24 modules)      │  │
                    │  │                                     │  │
                    │  │  ┌─ 核心基础 ─────────────────────┐│  │
                    │  │  │ common | system | user         ││  │
                    │  │  │ user-api | sensitive-api       ││  │
                    │  │  └────────────────────────────────┘│  │
                    │  │                                     │  │
                    │  │  ┌─ 学习成长 ─────────────────────┐│  │
                    │  │  │ plan | team | oj | interview   ││  │
                    │  │  │ mock-interview | flashcard     ││  │
                    │  │  │ knowledge | learning-asset     ││  │
                    │  │  └────────────────────────────────┘│  │
                    │  │                                     │  │
                    │  │  ┌─ 内容与社交 ───────────────────┐│  │
                    │  │  │ blog | community | moment      ││  │
                    │  │  │ codepen | chat | notification  ││  │
                    │  │  └────────────────────────────────┘│  │
                    │  │                                     │  │
                    │  │  ┌─ 平台能力 ─────────────────────┐│  │
                    │  │  │ ai | points | moyu | resume    ││  │
                    │  │  │ filestorage | sensitive        ││  │
                    │  │  │ version | sql-optimizer        ││  │
                    │  │  └────────────────────────────────┘│  │
                    │  └────────────────────────────────────┘  │
                    │                                         │
                    ├──────────────────────────────────────────┤
                    │            基础设施层                     │
                    │  MySQL (3306) │ Redis (6379, db3/db4) │ File (local/S3) │
                    └──────────────────────────────────────────┘
```

## 部署形态

项目以**单 JAR 部署**方式运行。`xiaou-bootstrap` 持有启动类、运行配置、日志配置和 Spring Boot 打包插件，并依赖 `xiaou-application` 完成业务模块组合：

```text
java -jar code-nest.jar --spring.profiles.active=prod
```

| 特征 | 说明 |
|------|------|
| 部署方式 | 单 JAR + 外置配置 |
| 前端 | Nginx 托管静态资源，反向代理后端 API |
| 数据库 | 单 MySQL 实例，当前主库基线 161 张表 |
| 缓存 | Redis db3（业务缓存 + Redisson）+ db4（Sa-Token 会话） |
| 文件 | 本地磁盘存储（默认），可切换 S3 / MinIO |
| WebSocket | 与 HTTP 共用 9999 端口，端点 `/ws/chat`，Nginx 需配置 Upgrade 代理 |

## 双端鉴权架构

Code-Nest 采用 Sa-Token **多端鉴权**，用户端和管理端使用完全独立的登录体系：

| 维度 | 用户端 | 管理端 |
|------|--------|--------|
| Stp 工具 | `StpUserUtil` | `StpAdminUtil` |
| Token 前缀 | `user:` | `admin:` |
| 登录接口 | `/api/user/auth/login` | `/api/admin/auth/login` |
| Controller 包 | `controller.user` | `controller.admin` |
| 鉴权注解 | 无（默认需登录） | `@RequireAdmin` |
| 角色检查 | 无 | `StpAdminUtil.checkRole("admin")` |
| 会话隔离 | Redis 不同 key 前缀 | Redis 不同 key 前缀 |

详细鉴权机制见 [权限边界](/reference/permission-boundaries)。

## 统一响应体

所有接口返回 `Result<T>` 统一格式：

```json
{
  "code": 200,
  "msg": "success",
  "data": { ... }
}
```

- 业务错误码范围：600-899
- 登录态错误返回 HTTP 401 + 业务码 701/702，权限或账号禁用返回 HTTP 403 + 703/704
- 普通业务拒绝返回 HTTP 422 并保留原业务码；参数、路由和系统异常继续使用对应 4xx/5xx
- 完整错误码见 [响应与错误码](/reference/response-errors)

## 跨模块调用机制

在单体架构中，模块间通过以下方式协作：

### 1. API 接口模块（编译期契约）

| API 模块 | 提供的接口 | 消费方 |
|----------|-----------|--------|
| `xiaou-user-api` | `UserInfoApiService.getUserInfo(id)` → 用户昵称/头像 | chat, team, blog, community, oj, points, notification, plan ... |
| `xiaou-sensitive-api` | `SensitiveCheckService.check(text)` → 敏感词检测 | blog, community, moment, chat, team, plan, codepen ... |

### 2. Spring 事件总线

异步解耦场景使用 `ApplicationEventPublisher`：

| 事件 | 发布方 | 消费方 | 用途 |
|------|--------|--------|------|
| 用户注册事件 | system | points | 赠送初始积分 |
| 签到事件 | team/plan | points | 积分奖励 |
| 通知事件 | 各模块 | notification | 站内通知推送 |

### 3. 应用端口与适配器

跨领域组合用例在 `xiaou-application` 定义读取端口，持久化适配器负责把各领域 Mapper/Entity 转成应用模型。编排逻辑只依赖端口，不直接持有其他领域的持久化类型：

```text
application service
    → application port
        → persistence adapter
            → owning domain Mapper/Entity
```

通知调用使用 `xiaou-notification.api.NotificationPublisher`；缓存调用使用 `CacheStore` / `TextStateStore`。业务模块不得导入其他领域的 Mapper/Entity，也不得绕过这些公开接口依赖实现类。

## 模块依赖关系图

```text
xiaou-bootstrap (启动与运行配置)
    → xiaou-application (应用编排)
        → 业务领域模块
            → xiaou-common-core / 所需专项基础模块
            → xiaou-user-api / xiaou-sensitive-api 等公开契约

xiaou-common-web/security/cache/persistence → xiaou-common-core
xiaou-resilience → Spring Context（不依赖业务领域）
```

## 请求处理链路

一个典型请求从 Nginx 到数据库的完整链路：

```text
[Nginx] → [Spring DispatcherServlet]
    → [CorsFilter] 跨域处理
    → [Sa-Token Filter] Token 解析
        → StpUserUtil / StpAdminUtil 登录校验
        → @RequireAdmin AOP 管理端权限校验
    → [Controller] 参数校验 (@Valid)
    → [Service] 业务逻辑
        → [MyBatis-Plus Mapper] 数据访问
        → [Redis] 缓存读写
        → [跨模块调用] UserInfoApiService / SensitiveCheckService
    → [Result<T>] 统一响应封装
    → [GlobalExceptionHandler] 异常兜底
```

## 前端架构

| 维度 | 用户端 (vue3-user-front) | 管理端 (vue3-admin-front) |
|------|-------------------------|--------------------------|
| 框架 | Vue 3 + Vite | Vue 3 + Vite |
| UI 库 | Element Plus | Element Plus |
| 路由 | Vue Router 4 | Vue Router 4 |
| 状态管理 | Pinia | Pinia |
| HTTP 客户端 | Axios | Axios |
| API 前缀 | `/api/user/` | `/api/admin/` |
| 开发端口 | 3001 | 3000 |
| 认证方式 | StpUserUtil (用户 Token) | StpAdminUtil (管理 Token) |
| 构建产物 | Nginx 托管 | Nginx 托管 |

## 端点命名规则

所有接口前缀为 `/api`（由 `server.servlet.context-path` 配置）：

| 前缀 | 认证方式 | 说明 |
|------|----------|------|
| `/api/user/*` | `StpUserUtil`（loginType="user"） | 用户端业务接口 |
| `/api/admin/*` | `StpAdminUtil`（loginType="admin"） | 管理端接口，`@RequireAdmin` AOP 拦截 |
| `/api/captcha/*` | 无 | 公开验证码接口 |
| `/api/ws/*` | ws-ticket 一次性凭证 | WebSocket 连接入口 |
| `/api/file*` / `/api/files/*` | 用户端 Token | 文件上传与访问 |
| `/api/actuator/*` | 无（开发环境） | Spring Boot Actuator 端点 |

> **重要**：用户端 Token 调管理端接口返回 `{"code":703}`，管理端 Token 调用户端接口同样返回 703。两端 Token 存储在 Redis db4，但通过 loginType 完全隔离。

## 外部依赖

| 外部服务 | 说明 | 默认地址 | 是否必须 |
|----------|------|----------|----------|
| go-judge | OJ 判题沙箱，特权容器运行 | 远端配置 | OJ 判题必须 |
| AI Provider | OpenAI 兼容 API，提供 LLM 能力 | 环境变量配置 | 模拟面试/AI 功能必须 |
| RAG Sidecar | 知识库检索增强 | `localhost:18080` | 可选，AI 高级功能需要 |
| Prometheus + Grafana | 监控指标采集与展示 | `localhost:9090` | 生产监控推荐 |

> **降级说明**：go-judge 不可用时，OJ 提交会卡在 JUDGING 状态。AI Provider 不可用时，相关功能自动降级返回默认提示。RAG Sidecar 不可用时，AI 检索功能降级为空结果。

## Maven 模块清单

Code-Nest 当前包含 36 个 Maven 子模块，最终由 `xiaou-bootstrap` 组装启动：

| 分组 | 模块 | 说明 |
|------|------|------|
| **核心基础** | `xiaou-common-core` | 统一响应、分页、业务异常、常量和纯工具 |
| | `xiaou-common-web` | 全局异常、HTTP 状态映射、CORS 和资源映射 |
| | `xiaou-common-security` | Sa-Token 双端鉴权、权限切面和密码工具 |
| | `xiaou-common-cache` | 缓存接口与 Redis 适配器 |
| | `xiaou-common-persistence` | MyBatis/PageHelper 与 SQL 日志配置 |
| | `xiaou-common` | 旧模块迁移期兼容聚合，不承载新实现 |
| | `xiaou-resilience` | 聚合查询的有界执行、超时、降级和来源状态 |
| | `xiaou-system` | 管理员、角色、权限、操作日志、仪表盘 |
| | `xiaou-user` | 用户信息、注册、登录、个人中心（实现 user-api） |
| | `xiaou-user-api` | 跨模块用户信息接口契约（不包含实现） |
| | `xiaou-sensitive` | 敏感词检测、DFA 匹配、策略、统计（实现 sensitive-api） |
| | `xiaou-sensitive-api` | 跨模块敏感词接口契约（不包含实现） |
| **学习成长** | `xiaou-plan` | 计划、签到、成长自动导航 |
| | `xiaou-team` | 学习小组、成员、讨论、任务 |
| | `xiaou-oj` | OJ 题目、提交、判题、赛事 |
| | `xiaou-interview` | 面试题库、题单、掌握度、收藏 |
| | `xiaou-mock-interview` | 模拟面试会话、方向、QA、求职作战台 |
| | `xiaou-flashcard` | 闪卡、卡组、学习记录、复习 |
| | `xiaou-knowledge` | 知识图谱节点与关系 |
| | `xiaou-learning-asset` | 学习资产候选、发布、审核、版本合并 |
| **内容与社交** | `xiaou-blog` | 博客文章、分类、标签 |
| | `xiaou-community` | 社区帖子、评论、分类、标签 |
| | `xiaou-moment` | 动态、评论、点赞、收藏 |
| | `xiaou-codepen` | 代码工坊、保存、分享 |
| | `xiaou-chat` | 聊天室、WebSocket、ws-ticket、禁言、踢出 |
| | `xiaou-notification` | 站内通知、未读数、推送 |
| **平台能力** | `xiaou-ai` | AI Runtime、Prompt、Graph Runner、RAG、回归测试 |
| | `xiaou-points` | 积分余额、流水、签到奖励、抽奖 |
| | `xiaou-moyu` | 程序员日历、热榜、Bug 商店、薪资计算 |
| | `xiaou-resume` | 简历、模板、版本、导出 |
| | `xiaou-filestorage` | 文件上传、存储策略、迁移 |
| | `xiaou-version` | 版本历史、发布、隐藏 |
| | `xiaou-sql-optimizer` | SQL 优化建议、AI 分析 |
| | `xiaou-sre` | 告警摄取、Incident、只读 RCA、评测队列和运行指标 |
| **应用编排** | `xiaou-application` | 首页、学习驾驶舱、Growth Coach 等跨领域组合用例 |
| **启动聚合** | `xiaou-bootstrap` | 唯一启动类、运行配置和可执行 JAR |

## Redis 数据分布

Redis 使用多个 database 索引隔离不同数据：

| Database | 用途 | 主要 Key 模式 | 管理 |
|----------|------|---------------|------|
| db3 | 业务缓存（Redisson） | `xiaou:*`、分布式锁、限流计数器、ws-ticket、在线用户 | Redisson 连接 |
| db4 | Sa-Token 会话 | `satoken:*`，用户端和管理端 Token 存储 | Sa-Token alone-redis 连接 |

**Redisson 主要用途**：

| 用途 | 说明 |
|------|------|
| 分布式锁 | 防止重复签到、重复提交、抽奖并发 |
| 限流 | API 限流、聊天室消息限流、抽奖限流 |
| 实时状态 | 在线用户列表、WebSocket 会话 |
| 缓存 | 热门帖子、用户信息缓存 |
| AI 运行观测 | `xiaou:ai:runtime:metrics` |

> **注意**：Redis 重启后 db4 中的 Sa-Token 数据会丢失，所有用户和管理员需要重新登录。db3 业务缓存丢失只会导致短暂的缓存穿透，不影响数据正确性。

## 关键配置

后端配置通过 `application.yml` + Profile + 环境变量三层覆盖：

| 配置 | 默认值 | 环境变量 | 说明 |
|------|--------|----------|------|
| 服务端口 | 9999 | — | `server.port` |
| 上下文路径 | `/api` | — | `server.servlet.context-path`，所有接口前缀 |
| MySQL URL | `localhost:3306/code_nest` | `XIAOU_MYSQL_URL` | Docker 下指向容器内 MySQL |
| Redis 地址 | `127.0.0.1:6379` | `XIAOU_REDIS_ADDRESS` | db3 业务 + db4 会话 |
| AI Provider | `openai-compatible` | `XIAOU_AI_PROVIDER` | 需配置 BASE_URL 和 API_KEY |
| RAG Endpoint | `localhost:18080` | `XIAOU_AI_RAG_ENDPOINT` | 默认关闭 |
| go-judge URL | 远端地址 | — | OJ 判题沙箱 |
| JWT Secret | 开发占位值 | `XIAOU_JWT_SECRET` | **生产必须覆盖** |
| CORS Origins | localhost:3000,3001 等 | `XIAOU_CORS_ALLOWED_ORIGIN_PATTERNS` | HTTP + WebSocket 共用 |

完整配置项见 [环境变量总表](/operations/env-vars)。

## 健康检查

后端通过 Spring Boot Actuator 提供健康检查端点：

```bash
# 基础健康状态
curl http://localhost:9999/api/actuator/health
# 期望: {"status":"UP"}

# 详细组件状态
curl http://localhost:9999/api/actuator/health | python3 -m json.tool
# 返回 db、redis、disk 等组件状态

# Prometheus 指标
curl http://localhost:9999/api/actuator/prometheus
# 返回 JVM、HTTP、HikariCP 等指标
```

| 端点 | 地址 | 用途 |
|------|------|------|
| Health | `http://localhost:9999/api/actuator/health` | 健康状态，P0 止损第一步 |
| Info | `http://localhost:9999/api/actuator/info` | 应用信息 |
| Prometheus | `http://localhost:9999/api/actuator/prometheus` | 监控指标采集 |

## 关键设计决策

| 决策 | 选择 | 理由 |
|------|------|------|
| 单体 vs 微服务 | 模块化单体 | 团队规模小，模块化足以隔离，避免分布式复杂度 |
| 多端鉴权 | Sa-Token 多 Stp | 用户端与管理端完全隔离，Token 不互通，Redis db4 统一存储 |
| ORM 选择 | MyBatis-Plus | 灵活 SQL + 代码生成，适合复杂查询场景 |
| AI 接入 | LangChain4j + LangGraph4j + LlamaIndex | Prompt 编排 + RAG + 图执行，支持复杂 AI 流程 |
| AI 降级 | 自动降级 | AI 不可用时返回默认提示，不影响其他业务 |
| 文件存储 | 抽象接口 + 本地默认 | 最小依赖，可扩展 S3/OSS/MinIO |
| API 模块 | 接口与实现分离 | 防止循环依赖，明确模块间契约 |
| 外部沙箱 | go-judge 独立部署 | OJ 判题沙箱特权运行，不和主后端共享容器 |
| 缓存策略 | Redis 双 db | db3 业务缓存 + db4 会话存储，Sa-Token 重启会丢失登录态 |

## 快速诊断命令

排查任何架构问题时，按以下顺序检查：

```bash
# 1. 后端健康
curl -s http://localhost:9999/api/actuator/health
# 期望: {"status":"UP"}

# 2. MySQL 连通
mysql -u root -p -e "SELECT 1;" code_nest
# 期望: 1

# 3. Redis 连通
redis-cli ping
# 期望: PONG

# 4. Sa-Token 登录态
redis-cli -n 4 keys "satoken:*" | wc -l
# 有数字说明有活跃登录态

# 5. 业务缓存
redis-cli -n 3 dbsize
# 显示缓存 key 数量

# 6. go-judge 可达（如果使用 OJ）
curl -s http://127.0.0.1:5050/version
# 期望: 返回版本信息

# 7. AI Provider 可达（如果使用 AI）
curl -s $XIAOU_AI_BASE_URL/models -H "Authorization: Bearer $XIAOU_AI_API_KEY"
# 期望: 返回模型列表
```

更详细的诊断流程见 [问题定位流程](/operations/diagnosis-flow)，降级策略见 [事故响应](/operations/incident-response)。

## 源码导航

| 文件 | 说明 |
|------|------|
| `xiaou-bootstrap/src/main/java/com/xiaou/bootstrap/CodeNestApplication.java` | Spring Boot 启动类 |
| `xiaou-bootstrap/src/main/resources/application.yml` | 主运行配置 |
| `xiaou-application/src/main/java/com/xiaou/web/` | 跨领域应用编排与端口适配器 |
| `xiaou-bootstrap/.../application.yml` | 主配置文件 |
| `xiaou-bootstrap/.../application-dev.yml` | 开发环境配置 |
| `xiaou-common/.../config/SaTokenConfig.java` | Sa-Token 双端鉴权配置 |
| `xiaou-common/.../config/RedisConfig.java` | Redis + Redisson 配置 |
| `xiaou-common/.../config/CorsConfig.java` | CORS 跨域配置 |
| `xiaou-common/.../config/MybatisPlusConfig.java` | MyBatis-Plus 分页插件配置 |
| `xiaou-common/.../satoken/AdminAuthAspect.java` | @RequireAdmin AOP 切面 |
| `xiaou-common/.../exception/GlobalExceptionHandler.java` | 全局异常处理 |
| `xiaou-user-api/.../UserInfoApiService.java` | 跨模块用户信息接口 |
| `xiaou-sensitive-api/.../SensitiveCheckService.java` | 跨模块敏感词检测接口 |


## 相关文档

| 文档 | 说明 |
| --- | --- |
| [后端模块](/architecture/backend-modules) | 后端 Maven 子模块详解 |
| [前端应用](/architecture/frontend-apps) | 用户端和管理端架构 |
| [数据库与脚本](/architecture/database) | 数据库设计和版本管理 |
| [源码地图](/reference/source-map) | 全项目代码索引 |
| [模块依赖地图](/reference/module-dependencies) | 模块间依赖关系 |
| [快速开始](/guide/quick-start) | 本地环境搭建 |
