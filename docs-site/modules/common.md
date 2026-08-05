# 公共基础模块

`v2.5.6` 起，公共能力不再堆放在一个全能 `xiaou-common` 中，而是按调用者真正需要理解的接口拆成五个基础模块。`xiaou-common` 仍保留为迁移期兼容聚合，但不再接收新实现。

## 模块职责

| 模块 | 接口与职责 | 允许的内部依赖 |
| --- | --- | --- |
| `xiaou-common-core` | `Result`、`ResultCode`、`PageResult`、`BusinessException`、常量和纯工具 | 不依赖其他 `xiaou-*` 模块 |
| `xiaou-common-web` | 全局异常处理、`Result` 到 HTTP 状态映射、CORS、IP 与静态资源适配 | `xiaou-common-core` |
| `xiaou-common-security` | 用户/管理员 Sa-Token、`@RequireAdmin`、密码工具 | `xiaou-common-core` |
| `xiaou-common-cache` | `CacheStore`、`TextStateStore` 及 Redis 适配器 | `xiaou-common-core` |
| `xiaou-common-persistence` | PageHelper、MyBatis 和 SQL 日志适配 | `xiaou-common-core` |
| `xiaou-common` | 为尚未迁移的模块聚合上述依赖 | 五个基础模块 |

这组模块的设计目标是让业务模块只依赖所需能力。例如，只需要 `Result` 的领域不应被迫引入 Redis、Sa-Token、Web MVC 和 MyBatis。

## 依赖规则

```text
xiaou-common-web ──────────┐
xiaou-common-security ─────┤
xiaou-common-cache ────────┼──> xiaou-common-core
xiaou-common-persistence ──┘

legacy business module ──> xiaou-common (兼容聚合)
migrated business module ─> 实际需要的 xiaou-common-* 模块
```

架构门禁执行以下约束：

- `xiaou-common-core` 不得依赖内部业务模块。
- 其他基础模块只能依赖 `xiaou-common-core`，不得互相形成隐式耦合。
- 新模块不得新增对兼容聚合 `xiaou-common` 的依赖。
- Redis 客户端实现限制在 `xiaou-common-cache` 或显式领域适配器中。

## Core 接口

`xiaou-common-core` 提供后端最稳定、最小的调用面：

| 类型 | 用途 |
| --- | --- |
| `Result<T>` | 统一成功/失败响应体 |
| `ResultCode` | 稳定业务码，包括 701-705 登录与权限语义 |
| `PageRequest` / `PageResult<T>` | 分页输入输出 |
| `BusinessException` | 业务规则拒绝 |
| `JsonUtils`、`DateHelper` 等 | 不绑定 Web/Redis/MyBatis 的通用计算 |

这些 Java 类型与前端 `@code-nest/api-contract` 的 `ApiResponse<T>`、`PageResult<T>` 对齐。业务码是跨端稳定契约，HTTP 状态负责传输语义。

## Web 错误适配

`xiaou-common-web` 在两层处理错误：

1. `GlobalExceptionHandler` 把异常转成 `Result`。
2. `ResultHttpStatusAdvice` 使用 `ResultHttpStatusMapper` 为错误结果设置 HTTP 状态。

| 失败类型 | HTTP 状态 | 业务码示例 |
| --- | --- | --- |
| 未登录、Token 无效或过期 | 401 | 701、702 |
| 权限不足或账号禁用 | 403 | 703、704 |
| 参数校验失败 | 400 | 400、601 |
| 登录凭据校验失败 | 401 | 705（前端仍分类为 business） |
| 普通业务拒绝 | 422 | 600 及未单独映射的领域业务码 |
| 资源/方法/媒体类型错误 | 对应 404/405/406/413/415 | 对应业务码 |
| 未捕获异常 | 500 | 500 |

`705 LOGIN_FAILED` 只表示本次凭据校验失败，不等价于已有会话过期。前端共享错误分类会避免因此清理登录态。

## Security 接口

| 类型 | 用途 |
| --- | --- |
| `StpUserUtil` | 用户登录态、用户 ID 与角色检查 |
| `StpAdminUtil` | 管理员登录态与权限检查 |
| `SaTokenUserUtil` | 兼容的当前用户访问入口 |
| `@RequireAdmin` / `AdminAuthAspect` | 管理端方法权限适配 |
| `SaTokenConfig` | 双登录域配置 |

用户端和管理端保持独立 login type。调用方依赖安全接口，不直接操作 Sa-Token 存储结构。

## Cache 接口

| 接口 | 语义 | Redis 适配器 |
| --- | --- | --- |
| `CacheStore` | 类型化值、计数器、存在性和过期时间 | `RedisValueStore` |
| `TextStateStore` | 文本状态的读取、写入与删除 | `RedisTextStateStore` |

业务模块依赖接口，缓存键、序列化、TTL 和 Redis 异常处理集中在适配器。需要 Redisson 锁或专用数据结构的领域必须把它封装成显式领域适配器，不能在任意业务类中直接扩散客户端调用。

## Persistence 接口

`xiaou-common-persistence` 只提供数据访问的横向基础：

- `PageHelper` 统一分页查询返回结构。
- `P6SpyLogger` 负责开发环境 SQL 日志适配。
- MyBatis 与连接池依赖由该模块承载。

领域实体和 Mapper 始终归所属业务模块，不允许移动回公共基础模块。

## 聚合韧性

跨来源聚合使用独立的 `xiaou-resilience`：

| 类型 | 用途 |
| --- | --- |
| `ResilientExecutor` | 在统一并发上限和超时内执行来源读取 |
| `ResilientResult<T>` | 同时返回值、状态、耗时和降级原因 |
| `ResilientStatus` | 成功、超时、失败等有界状态 |

首页、学习驾驶舱、Growth Coach 和系统仪表盘通过同一接口表达部分降级，避免每个聚合模块复制 `CompletableFuture`、超时和兜底实现。

## 源码导航

| 路径 | 内容 |
| --- | --- |
| `xiaou-common-core/src/main/java/com/xiaou/common/` | 核心响应、异常和工具 |
| `xiaou-common-web/src/main/java/com/xiaou/common/` | HTTP 与 Web 适配 |
| `xiaou-common-security/src/main/java/com/xiaou/common/` | 鉴权与权限 |
| `xiaou-common-cache/src/main/java/com/xiaou/common/cache/` | 缓存接口和 Redis 适配器 |
| `xiaou-common-persistence/src/main/java/com/xiaou/common/` | 持久化基础 |
| `xiaou-resilience/src/main/java/com/xiaou/resilience/` | 聚合韧性接口与实现 |
| `scripts/check-architecture.py` | 依赖方向和实现泄漏门禁 |

## 验证

```bash
python scripts/check-architecture.py
mvn -pl xiaou-common-web,xiaou-common-cache,xiaou-resilience -am test
```

新增公共能力前先判断它属于 core、web、security、cache 还是 persistence。无法明确归类的能力通常应留在所属领域，而不是继续扩大兼容聚合。
