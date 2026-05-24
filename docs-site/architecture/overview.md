# 整体架构

Code-Nest 是一个基于 Spring Boot 3.4.4 + Vue 3 的全栈学习成长平台，采用**单体模块化架构**（Modular Monolith），将业务拆分为 24 个独立 Maven 子模块，最终由 `xiaou-application` 聚合启动为一个 JAR。

## 技术栈总览

| 层级 | 技术 | 版本/说明 |
|------|------|-----------|
| 运行时 | Java | 17 |
| 后端框架 | Spring Boot | 3.4.4 |
| ORM | MyBatis-Plus | 3.5.x |
| 数据库 | MySQL | 8.0+ |
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
            │  :5173        │    │  :5174       │    │  :5175         │
            └───────┬──────┘    └───────┬──────┘    └────────────────┘
                    │                    │
                    └────────┬───────────┘
                             │ HTTP / WebSocket
                    ┌────────▼────────────────────────────────┐
                    │        xiaou-application (Spring Boot)   │
                    │        CodeNestApplication.java          │
                    │        Port: 8080                        │
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
                    │  MySQL  │  Redis  │  Local/S3 File       │
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
| 数据库 | 单 MySQL 实例，共 37 张表 |
| 缓存 | Redis 用于 Sa-Token 会话 + 业务缓存 |
| 文件 | 本地磁盘存储，可切换 S3 |
| WebSocket | 与 HTTP 共用 8080 端口，STOMP 协议 |

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
| 构建产物 | Nginx 托管 | Nginx 托管 |

## 关键设计决策

| 决策 | 选择 | 理由 |
|------|------|------|
| 单体 vs 微服务 | 模块化单体 | 团队规模小，模块化足以隔离，避免分布式复杂度 |
| 多端鉴权 | Sa-Token 多 Stp | 用户端与管理端完全隔离，Token 不互通 |
| ORM 选择 | MyBatis-Plus | 灵活 SQL + 代码生成，适合复杂查询场景 |
| AI 接入 | Spring AI + LangGraph4j | Prompt 编排 + RAG + 图执行，支持复杂 AI 流程 |
| 文件存储 | 抽象接口 + 本地默认 | 最小依赖，可扩展 S3/OSS |
| API 模块 | 接口与实现分离 | 防止循环依赖，明确模块间契约 |

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
