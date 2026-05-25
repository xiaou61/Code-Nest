# 整体架构

Code-Nest 是一个基于 Spring Boot 3.4.4 + Vue 3 的全栈学习成长平台，采用**单体模块化架构**（Modular Monolith），将业务拆分为 24 个独立 Maven 子模块，最终由 `xiaou-application` 聚合启动为一个 JAR。

## 技术栈总览

| 层级 | 技术 | 版本/说明 |
|------|------|-----------|
| 运行时 | Java | 17 |
| 后端框架 | Spring Boot | 3.4.4 |
| ORM | MyBatis-Plus | 3.5.x |
| 数据库 | MySQL | 8.0+，共 142 张表 |
| 缓存 | Redis | 7.x + Redisson |
| 鉴权 | Sa-Token | 多端鉴权 (User + Admin) |
| AI 接入 | Spring AI + LangGraph4j | Prompt 编排 / RAG / 图执行 |
| 文件存储 | 本地磁盘 / S3 兼容 | 可切换 |
| 实时通信 | Spring WebSocket | STOMP 协议 |
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
                    │        xiaou-application (Spring Boot)   │
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

项目以**单 JAR 部署**方式运行，所有模块聚合在 `xiaou-application` 中，由 `CodeNestApplication.java` 启动：

```text
java -jar code-nest.jar --spring.profiles.active=prod
```

| 特征 | 说明 |
|------|------|
| 部署方式 | 单 JAR + 外置配置 |
| 前端 | Nginx 托管静态资源，反向代理后端 API |
| 数据库 | 单 MySQL 实例，共 142 张表 |
| 缓存 | Redis db3（业务缓存 + Redisson）+ db4（Sa-Token 会话） |
| 文件 | 本地磁盘存储（默认），可切换 S3 / MinIO |
| WebSocket | 与 HTTP 共用 9999 端口，STOMP 协议，Nginx 需单独配置 Upgrade 代理 |

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
- Sa-Token 异常返回 HTTP 200 + 业务码 701/702/703
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

### 3. 直接 Service 调用

同一进程内的模块间直接 `@Autowired` 注入，依赖方向遵循：

```text
xiaou-application (聚合)
    ├── xiaou-ai, xiaou-oj, xiaou-chat, ...
    │       └── xiaou-user-api (接口契约)
    │       └── xiaou-sensitive-api (接口契约)
    │       └── xiaou-common (公共工具)
    └── xiaou-user (用户核心, 实现 user-api)
    └── xiaou-sensitive (敏感词, 实现 sensitive-api)
    └── xiaou-system (系统管理)
```

**关键约束**：业务模块只依赖 `xiaou-user-api`（接口），不依赖 `xiaou-user`（实现），避免循环依赖。

## 模块依赖关系图

```text
                        xiaou-application (启动入口)
                               │
          ┌────────────────────┼────────────────────────┐
          │                    │                         │
    xiaou-common          xiaou-user-api          xiaou-sensitive-api
    (全局公共)             (用户信息接口)            (敏感词接口)
          │                    │                         │
    ┌─────┴──────┐        ┌────┴─────┐             ┌────┴─────┐
    │            │        │          │             │          │
  xiaou-user  xiaou-system  xiaou-sensitive    (各业务模块)
  (实现)       (系统管理)     (实现)           依赖接口不依赖实现
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

Code-Nest 当前包含 24 个 Maven 子模块，最终由 `xiaou-application` 聚合启动：

| 分组 | 模块 | 说明 |
|------|------|------|
| **核心基础** | `xiaou-common` | 全局配置、工具、异常处理、鉴权、CORS |
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
| **启动聚合** | `xiaou-application` | 聚合所有模块，Spring Boot 启动入口 |

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
| AI 接入 | Spring AI + LangGraph4j | Prompt 编排 + RAG + 图执行，支持复杂 AI 流程 |
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
| `xiaou-application/.../CodeNestApplication.java` | Spring Boot 启动类 |
| `xiaou-application/.../application.yml` | 主配置文件 |
| `xiaou-application/.../application-dev.yml` | 开发环境配置 |
| `xiaou-common/.../config/SaTokenConfig.java` | Sa-Token 双端鉴权配置 |
| `xiaou-common/.../config/RedisConfig.java` | Redis + Redisson 配置 |
| `xiaou-common/.../config/CorsConfig.java` | CORS 跨域配置 |
| `xiaou-common/.../config/MybatisPlusConfig.java` | MyBatis-Plus 分页插件配置 |
| `xiaou-common/.../satoken/AdminAuthAspect.java` | @RequireAdmin AOP 切面 |
| `xiaou-common/.../exception/GlobalExceptionHandler.java` | 全局异常处理 |
| `xiaou-user-api/.../UserInfoApiService.java` | 跨模块用户信息接口 |
| `xiaou-sensitive-api/.../SensitiveCheckService.java` | 跨模块敏感词检测接口 |
