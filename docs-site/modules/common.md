# 公共底座（xiaou-common）

公共底座是全站所有业务模块的共享层。它不是面向用户的功能模块，而是把鉴权、统一响应、分页、异常、并发工具、Redis 操作、IP 解析、密码哈希、常量定义这些"每个模块都会用但不想重复写"的东西收拢到一起。

读这篇时建议带着一个问题：**业务模块依赖了公共底座的哪些能力？如果要改底座行为，影响面有多大？**

## 功能定位

| 能力 | 核心类 | 被谁使用 |
| --- | --- | --- |
| 统一响应 | `Result`、`ResultCode`、`PageResult` | 所有 Controller |
| 双端鉴权 | `StpUserUtil`、`StpAdminUtil`、`SaTokenConfig`、`StpInterfaceImpl` | 所有需要登录的接口 → 详见 [鉴权与用户体系](/modules/auth) |
| 管理端权限 | `@RequireAdmin`、`AdminAuthAspect` | 管理端接口 → 详见 [权限注解与角色边界索引](/reference/permission-boundaries) |
| 操作日志 | `@Log`、`LogAspect` | 管理端增删改接口 |
| 异常体系 | `BusinessException`、`GlobalExceptionHandler`、AI 异常族 | 所有 Service |
| 并发工具 | `ConcurrentUtils`、`ThreadPoolUtils` | 敏感词、通知、迁移等异步场景 |
| 缓存工具 | `RedisUtil`、`NotificationCacheUtil` | 通知未读数、系统设置 |
| 分页 | `PageRequest`、`PageResult`、`ManualPageHelper` | 所有列表接口 |
| 常量 | `Constants` | 全局配置默认值 |
| 密码 | `PasswordUtil` | 用户注册、管理员登录 |
| IP 解析 | `IPUtil` | 登录日志、操作日志 |
| 日期 | `DateHelper` | 多个模块时间格式化 |
| 用户上下文 | `SaTokenUserUtil` | 获取当前登录用户信息 |
| AI 配置 | `AiProperties` | AI 模块参数 |
| 静态资源 | `LocalFileResourceConfig` | 文件存储模块本地映射 |
| MyBatis 配置 | `MybatisPlusConfig` | 所有 Mapper |
| 跨域 | `CorsConfig` | 所有前端请求 |
| Redis 配置 | `RedisConfig` | 所有缓存操作 |
| SQL 日志 | `P6SpyLogger` | 开发环境 SQL 追踪 |

## 推荐学习顺序

1. 先看 `Result` + `ResultCode` + `GlobalExceptionHandler`，理解全站 API 响应格式和异常如何变成错误码。
2. 再看 `StpUserUtil` + `StpAdminUtil` + `SaTokenConfig`，理解双端鉴权的分仓设计（配合 [鉴权与用户体系](/modules/auth)）。
3. 接着看 `@RequireAdmin` + `AdminAuthAspect`，理解管理端权限是怎么切面拦截的（配合 [权限注解与角色边界索引](/reference/permission-boundaries)）。
4. 然后看 `ConcurrentUtils` 和 `ThreadPoolUtils`，理解全站并发工具的设计与边界。
5. 最后看 `Constants`、`PasswordUtil`、`IPUtil`、`DateHelper` 等小工具，补齐全局认知。

## 源码地图

| 位置 | 作用 |
| --- | --- |
| `xiaou-common/src/main/java/com/xiaou/common/core/domain/Result.java` | 统一响应包装 |
| `xiaou-common/src/main/java/com/xiaou/common/core/domain/ResultCode.java` | 响应码枚举 |
| `xiaou-common/src/main/java/com/xiaou/common/core/domain/PageRequest.java` | 分页请求基类 |
| `xiaou-common/src/main/java/com/xiaou/common/core/domain/PageResult.java` | 分页响应包装 |
| `xiaou-common/src/main/java/com/xiaou/common/exception/BusinessException.java` | 业务异常 |
| `xiaou-common/src/main/java/com/xiaou/common/exception/GlobalExceptionHandler.java` | 全局异常拦截 |
| `xiaou-common/src/main/java/com/xiaou/common/exception/ai/*.java` | AI 模块异常族 |
| `xiaou-common/src/main/java/com/xiaou/common/satoken/StpUserUtil.java` | 用户端 Sa-Token 工具 |
| `xiaou-common/src/main/java/com/xiaou/common/satoken/StpAdminUtil.java` | 管理端 Sa-Token 工具 |
| `xiaou-common/src/main/java/com/xiaou/common/satoken/SaTokenUserUtil.java` | 当前登录用户上下文 |
| `xiaou-common/src/main/java/com/xiaou/common/satoken/StpInterfaceImpl.java` | Sa-Token 权限接口实现 |
| `xiaou-common/src/main/java/com/xiaou/common/config/SaTokenConfig.java` | Sa-Token 配置 |
| `xiaou-common/src/main/java/com/xiaou/common/annotation/RequireAdmin.java` | 管理端权限注解 |
| `xiaou-common/src/main/java/com/xiaou/common/aspect/AdminAuthAspect.java` | 管理端权限切面 |
| `xiaou-common/src/main/java/com/xiaou/common/annotation/Log.java` | 操作日志注解 |
| `xiaou-common/src/main/java/com/xiaou/common/utils/ConcurrentUtils.java` | 并发工具集 |
| `xiaou-common/src/main/java/com/xiaou/common/utils/ThreadPoolUtils.java` | 线程池工具 |
| `xiaou-common/src/main/java/com/xiaou/common/utils/RedisUtil.java` | Redis 操作工具 |
| `xiaou-common/src/main/java/com/xiaou/common/utils/IPUtil.java` | IP 地址解析 |
| `xiaou-common/src/main/java/com/xiaou/common/utils/PasswordUtil.java` | 密码工具 |
| `xiaou-common/src/main/java/com/xiaou/common/utils/DateHelper.java` | 日期工具 |
| `xiaou-common/src/main/java/com/xiaou/common/utils/JsonUtils.java` | JSON 工具 |
| `xiaou-common/src/main/java/com/xiaou/common/utils/RelativeTimeUtil.java` | 相对时间显示 |
| `xiaou-common/src/main/java/com/xiaou/common/utils/AvatarUtils.java` | 头像工具 |
| `xiaou-common/src/main/java/com/xiaou/common/constant/Constants.java` | 系统常量 |
| `xiaou-common/src/main/java/com/xiaou/common/config/AiProperties.java` | AI 配置属性 |

## 统一响应格式

所有 API 都通过 `Result<T>` 包装响应：

```java
Result.success(data)       → { "code": 200, "message": "操作成功", "data": ... }
Result.error(ResultCode)   → { "code": xxx, "message": "错误描述", "data": null }
```

`ResultCode` 枚举定义了全站错误码。关键码值和 [响应与错误码](/reference/response-errors) 完全对应。

分页响应使用 `PageResult<T>`，在 `Result` 的基础上增加 `total`、`pageNum`、`pageSize` 字段。`PageRequest` 是分页请求基类，默认页码 1、默认页大小 10。

## 双端鉴权

项目使用 Sa-Token 实现用户端和管理端分仓鉴权（详见 [鉴权与用户体系](/modules/auth)）：

| 端 | 工具类 | Token 名 | Session 存储 | 登录方式 |
| --- | --- | --- | --- | --- |
| 用户端 | `StpUserUtil` | `userToken` | `StpUserUtil.login(userId)` | 手机号/邮箱/用户名 + 密码 |
| 管理端 | `StpAdminUtil` | `adminToken` | `StpAdminUtil.login(adminId)` | 用户名 + 密码 |

两端 Token 互相隔离——用户端 Token 不能访问管理端接口，反之亦然。

`StpInterfaceImpl` 实现 Sa-Token 的 `StpInterface` 接口，提供权限和角色加载。当前实现从数据库查询管理员的角色和权限列表，用户端直接返回空列表。细粒度权限边界见 [权限注解与角色边界索引](/reference/permission-boundaries)。

## 管理端权限拦截

`@RequireAdmin` 注解标注在 Controller 方法上，`AdminAuthAspect` 切面拦截（完整执行链路和分布见 [权限注解与角色边界索引](/reference/permission-boundaries)）：

1. 检查当前请求是否持有管理端登录态（`StpAdminUtil.isLogin()`）
2. 未登录 → 返回 `Result.error(ResultCode.UNAUTHORIZED)`
3. 已登录 → 放行

这个切面只检查登录态，不检查具体角色或权限。角色和权限的细粒度控制由 Sa-Token 的 `@SaCheckRole` 和 `@SaCheckPermission` 补充。

## 操作日志

`@Log` 注解标注在 Controller 方法上，`LogAspect` 切面拦截后异步写入 `sys_operation_log`。

注解属性：

| 属性 | 说明 | 默认值 |
| --- | --- | --- |
| `module` | 操作模块 | `""` |
| `type` | 操作类型（SELECT/INSERT/UPDATE/DELETE/EXPORT/IMPORT/CLEAN/OTHER） | `OTHER` |
| `description` | 操作描述 | `""` |
| `saveRequestData` | 是否保存请求参数 | `true` |
| `saveResponseData` | 是否保存响应数据 | `true` |

请求参数中的 `password`、`oldPassword`、`newPassword`、`confirmPassword`、`token`、`accessToken`、`secret`、`apiKey` 会被替换为 `******`。

## 异常体系

```
RuntimeException
  └── BusinessException              — 业务异常，携带 ResultCode
  └── AiConfigurationException       — AI 配置异常
  └── AiInvocationException          — AI 调用异常
  └── AiRetrievalException           — AI 检索异常
  └── AiGraphExecutionException      — AI 图执行异常
  └── AiStructuredOutputException    — AI 结构化输出异常
```

`GlobalExceptionHandler` 拦截所有未捕获异常：

| 异常类型 | 响应码 | 说明 |
| --- | --- | --- |
| `BusinessException` | 异常携带的 ResultCode | 业务逻辑错误 |
| `MethodArgumentNotValidException` | PARAM_ERROR | 参数校验失败 |
| `HttpRequestMethodNotSupportedException` | METHOD_NOT_ALLOWED | HTTP 方法错误 |
| 其他 `Exception` | INTERNAL_ERROR | 兜底 500 |

## 密码工具

`PasswordUtil` 基于 Hutool 的 BCrypt 实现：

| 方法 | 说明 |
| --- | --- |
| `encode(rawPassword)` | BCrypt 哈希，默认 10 轮 |
| `encode(rawPassword, rounds)` | 自定义轮数（4-31，推荐 10-12） |
| `matches(rawPassword, encodedPassword)` | 验证密码 |

`encode(null)` 或 `encode("")` 抛 `IllegalArgumentException`；`matches(null, ...)` 返回 `false`。

## IP 解析

`IPUtil.getIpAddress(request)` 按 Header 优先级提取真实 IP：

1. `X-Forwarded-For`（取第一个，逗号分隔）
2. `Proxy-Client-IP`
3. `WL-Proxy-Client-IP`
4. `HTTP_CLIENT_IP`
5. `HTTP_X_FORWARDED_FOR`
6. `request.getRemoteAddr()`

`X-Forwarded-For` 可被客户端伪造。生产环境应配置 Nginx `set_real_ip_from` 只信任代理添加的值。

## 系统常量

`Constants` 定义了全站硬编码常量。关键值：

| 常量 | 值 | 说明 |
| --- | --- | --- |
| `SYSTEM_NAME` | `"Code-Nest"` | 系统名称 |
| `SYSTEM_VERSION` | `"1.0.0"` | 版本号（已过时，项目已到 v2.2.x） |
| `DEFAULT_PASSWORD` | `"123456"` | 默认密码 |
| `TOKEN_EXPIRE_TIME` | `7200L` | Token 有效期 2 小时 |
| `CACHE_PREFIX` | `"code_nest:"` | Redis 缓存前缀 |
| `MAX_FILE_SIZE` | `104857600` | 单文件最大 100MB |
| `DEFAULT_PAGE_SIZE` | `10` | 默认分页大小 |
| `MAX_PAGE_SIZE` | `1000` | 最大分页大小 |
| `REGEX_PASSWORD` | `^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$` | 密码正则：需小写+大写+数字，最少 8 位 |

## 日期工具

`DateHelper` 封装了 Hutool `DateUtil` 的常用操作。每次调用 `parseDateTime` / `parseDate` 都创建新的 `SimpleDateFormat` 实例，避免了共享实例的线程安全问题。

`isExpired(expireDate)` 方法：当 `expireDate` 为 `null` 时返回 `false`。这意味着"没有过期时间 → 未过期"，语义合理但可能掩盖数据缺失问题。

## 验证清单

| 验证点 | 怎么验证 | 预期结果 |
| --- | --- | --- |
| 统一响应格式 | 调用任意 API | 返回 `{ code, message, data }` 结构 |
| 双端鉴权隔离 | 用户端 Token 访问管理端接口 | 返回 401 |
| 管理端权限拦截 | 未登录访问 `@RequireAdmin` 接口 | 返回 401 |
| 操作日志记录 | 调用 `@Log` 标注的接口 | `sys_operation_log` 有对应记录 |
| 密码参数过滤 | 修改密码接口的日志记录 | 密码字段显示为 `******` |
| 密码哈希 | 注册后查数据库 | 密码字段是 BCrypt 哈希值 |
| IP 解析 | 通过 Nginx 代理访问 | 日志中 IP 不是 `127.0.0.1` |

## 常见坑

| 问题 | 原因 | 建议 |
| --- | --- | --- |
| 修改底座行为后其他模块没生效 | 依赖版本未更新 | `xiaou-common` 改完后确认其他模块引用的是最新版本 |
| 日志里出现密码明文 | `saveRequestData=true` 但切面未覆盖新字段 | 检查 `LogAspect` 的敏感字段列表 |
| `SYSTEM_VERSION` 与实际版本不符 | 常量过时 | 使用 README 或 pom.xml 中的版本号，不要依赖此常量 |
| `isExpired(null)` 返回 false | 设计如此 | 如果业务期望"无过期时间 = 永不过期"，这是正确的 |

## 相关模块

| 模块 | 关系 | 说明 |
| --- | --- | --- |
| [鉴权与用户体系](/modules/auth) | 强依赖 | 公共底座提供 Sa-Token 双端鉴权基础设施 |
| [用户账户与个人中心](/modules/user-account) | 被依赖 | 用户上下文查询依赖用户账户数据 |
| [敏感词风控](/modules/sensitive) | 被依赖 | 敏感词检测使用公共底座的并发工具 |
| [通知中心](/modules/notification) | 被依赖 | 通知未读数缓存使用 RedisUtil |
| [系统运营后台](/modules/system-ops) | 被依赖 | 操作日志和权限注解由公共底座提供 |

---

## 公共底座模块深度拆解

> 以下内容基于 `xiaou-common` 全部源码逐行拆解，覆盖 2 个并发工具类、6 个核心域对象、7 个异常类、2 个注解 + 2 个切面、4 个 Sa-Token 相关类、3 个配置类、8 个工具类、1 个常量类。

### 一、并发工具集 ConcurrentUtils 深度分析

**源码**：`ConcurrentUtils.java`（1043 行）

这是全站最大的工具类，提供限流、熔断、锁、计数器、延迟值等并发原语。

#### 1.1 RateLimiter（令牌桶限流）

```
RateLimiter(permitsPerSecond):
  - maxPermits = permitsPerSecond
  - storedPermits = 0
  - nextFreeTicketMicros = 系统微秒时间

tryAcquire(timeoutMs):
  1. sync() — 补充令牌
  2. storedPermits >= 1 → 消费一个, return true
  3. 等待时间 ≤ timeout → sleep 等待, return true
  4. 等待时间 > timeout → return false

acquire():
  1. sync()
  2. storedPermits >= 1 → 消费, return
  3. sleep(等待时间) → busy-wait
  4. 循环直到获取

executeWithLimit(rateLimiter, supplier, timeoutMs):
  tryAcquire(timeoutMs) → 执行 supplier → 否则 throw
```

**关键发现 1**：`acquire()` 方法在令牌不足时使用 `Thread.sleep(1)` 循环等待（busy-wait）。这会持续占用 CPU，在令牌恢复间隔较长时浪费严重。更好的实现是精确计算等待时间后一次性 sleep。

**关键发现 2**：`sync()` 方法使用 `synchronized(this)` 同步。这意味着同一个 RateLimiter 实例的所有 `tryAcquire` / `acquire` 调用会串行化。如果多个线程使用同一个限流器，高并发下会有锁竞争。

**关键发现 3**：`storedPermits` 是 `double` 类型，支持小数令牌。令牌补充计算公式是 `(已过微秒 / 1_000_000) * maxPermits`，精确但增加了计算复杂度。

#### 1.2 SlidingWindowRateLimiter（滑动窗口限流）

```
SlidingWindowRateLimiter(permitsPerSecond, windowSizeMs):
  - windowSize = windowSizeMs
  - windows = AtomicLong[windowSize]  ← 每毫秒一个计数器
  - maxRequests = permitsPerSecond * windowSizeMs / 1000

tryAcquire():
  1. 计算当前窗口位置: index = (currentTime % windowSize)
  2. slideWindow() — 重置过期窗口
  3. 统计当前窗口总请求数
  4. < maxRequests → 当前窗口计数+1, return true
  5. ≥ maxRequests → return false

slideWindow():
  1. 计算 lastWindow 与当前窗口的距离
  2. 距离 > windowSize → 清空所有窗口
  3. 距离 ≤ windowSize → 逐个清空中间窗口
  4. 使用 CAS (compareAndSet) 更新
```

**关键发现**：窗口数组使用 `AtomicLong[]`，每个元素是一个独立的计数器。当 `windowSizeMs = 1000` 时，数组长度为 1000。这意味着 1 秒精度需要 1000 个 AtomicLong 对象。如果需要更细粒度或更长时间窗口，内存开销会线性增长。

#### 1.3 CircuitBreaker（熔断器）

```
CircuitBreaker(failureThreshold, resetTimeoutMs):
  状态: CLOSED / OPEN / HALF_OPEN
  failureCount: AtomicInteger
  state: volatile String

allowRequest():
  CLOSED → true
  OPEN → 检查是否超过 resetTimeout → 是: state=HALF_OPEN, true / 否: false
  HALF_OPEN → true

onSuccess():
  HALF_OPEN → state=CLOSED, failureCount=0
  CLOSED → 不操作

onFailure():
  failureCount+1
  CLOSED 且 failureCount ≥ threshold → state=OPEN, 记录 openTime
  HALF_OPEN → state=OPEN, 记录 openTime

execute(supplier):
  allowRequest() → 执行 → onSuccess() / onFailure(throw)

executeWithFallback(supplier, fallback):
  allowRequest() → 执行 → onSuccess() / onFailure → fallback
```

**关键发现 1**：`state` 使用 `volatile String`，`failureCount` 使用 `AtomicInteger`。这两个字段不是原子联动——在 `onFailure()` 中先 `failureCount+1` 再判断阈值改状态，存在竞态。极端情况下两个线程可能同时到达阈值，都尝试切换到 OPEN，但最终结果是正确的（幂等）。

**关键发现 2**：`allowRequest()` 中从 OPEN 切换到 HALF_OPEN 使用 `state = "HALF_OPEN"`，没有 CAS。两个线程可能同时通过 `allowRequest()` 进入 HALF_OPEN，然后都执行请求。这允许超过预期的请求通过熔断器——但在大多数场景下，多放一两个请求比完全串行化更可接受。

**关键发现 3**：HALF_OPEN 状态下，`onSuccess()` 直接切换到 CLOSED，不经过"逐步放量"阶段。这意味着熔断恢复是"一关就开"而不是"慢启动"。

#### 1.4 StripedLock（分段锁）

```
StripedLock(stripes):
  - locks = ReentrantLock[powerOf2(stripes)]
  - mask = locks.length - 1

lock(key):
  locks[key.hashCode() & mask].lock()

tryLock(key, timeout, unit):
  locks[key.hashCode() & mask].tryLock(timeout, unit)
```

**关键设计**：分段锁用 key 的 hashCode 对 2 的幂取模来选择锁实例。这减少了锁竞争——不同 key 通常会分散到不同锁上。但 hashCode 冲突时，不相关的 key 会共享同一把锁。

**关键发现**：`stripes` 参数会向上取整到最近的 2 的幂。传 10 实际创建 16 把锁。

#### 1.5 其他并发原语

| 原语 | 实现 | 说明 |
| --- | --- | --- |
| `Counter` | `LongAdder` | 高性能计数器 |
| `Statistics` | CAS 更新 count/sum/min/max | 统计信息收集器 |
| `Lazy<T>` | `volatile` + `synchronized` 双检锁 | 线程安全延迟初始化 |
| `ExpiringValue<T>` | `synchronized` + TTL | 带过期时间的缓存值 |
| `Once` | `AtomicInteger` + `CountDownLatch` | 只执行一次保证 |
| `computeIfAbsent` | `ConcurrentMap` 安全计算 | Null-safe 的 putIfAbsent |

### 二、线程池工具 ThreadPoolUtils 深度分析

**源码**：`ThreadPoolUtils.java`（1206 行）

#### 2.1 预配置线程池

| 池名 | 核心线程 | 最大线程 | 队列 | 适用场景 |
| --- | --- | --- | --- | --- |
| COMMON | CPU 核数 | CPU×2 | 1000 | 通用计算 |
| IO | CPU×2 | CPU×4 | 2000 | IO 密集（网络、磁盘） |
| CPU | CPU 核数 | CPU 核数 | 500 | CPU 密集计算 |
| SCHEDULER | 2 | 4 | 延迟队列 | 定时任务 |

所有池使用 `CallerRunsPolicy` 拒绝策略——队列满时由调用方线程执行，保证任务不丢失。

#### 2.2 虚拟线程支持

```
isVirtualThreadAvailable():
  try:
    Method method = Executors.class.getMethod("newVirtualThreadPerTaskExecutor")
    method.invoke(null)  // 测试创建
    return true
  catch:
    return false
```

**关键发现**：虚拟线程检测使用反射，兼容 Java 17（无虚拟线程）和 Java 21+（有虚拟线程）。反射调用在类加载时执行一次，结果缓存到 `volatile boolean`。但如果 JVM 版本不匹配或模块系统限制反射访问，检测会静默失败并退回平台线程。

#### 2.3 并行执行模式

| 方法 | 并发控制 | 失败策略 | 适用场景 |
| --- | --- | --- | --- |
| `parallel()` | 无限制 | 单个失败不影响其他 | 独立任务 |
| `parallelMap()` | 无限制 | 单个失败不影响其他 | 批量映射 |
| `parallelMapWithLimit()` | Semaphore 控制并发数 | 单个失败不影响其他 | 限制资源消耗 |
| `parallelMapFailFast()` | AtomicInteger 标记 | 第一个失败后其余快速返回 | 要求全部成功 |
| `parallelMapWithTimeout()` | 单任务超时 | 超时返回默认值 | 限时执行 |
| `parallelMapVirtual()` | 虚拟线程 | 单个失败不影响其他 | IO 密集 + Java 21+ |

**关键发现**：`parallelMapFailFast` 使用 `AtomicInteger failedFlag` 实现快速失败。一旦某个任务设置 `failedFlag=1`，其余任务在开始前检查 flag 并直接跳过。但已经开始执行的任务不会被中断——它们会继续运行到完成或异常。

#### 2.4 重试机制

```
executeWithRetry(task, maxRetries, intervalMs):
  for i in 0..maxRetries:
    try: return task.get()
    catch: if last retry → throw, else sleep(intervalMs)

executeWithExponentialBackoff(task, maxRetries, initialDelay):
  delay = initialDelay
  for i in 0..maxRetries:
    try: return task.get()
    catch: if last retry → throw
    jitter = delay * 0.2 * Math.random()
    sleep(delay + jitter)
    delay *= 2  ← 指数退避

executeWithConditionalRetry(task, maxRetries, shouldRetry):
  for i in 0..maxRetries:
    try: return task.get()
    catch e: if !shouldRetry.test(e) → throw, else sleep
```

**关键发现**：指数退避包含 20% 的随机抖动（jitter），防止多个重试请求同时发出（"惊群效应"）。这是分布式系统的最佳实践。

#### 2.5 自定义线程池缓存

```
POOL_CACHE: ConcurrentHashMap<String, ExecutorService>
```

**关键发现**：`POOL_CACHE` 没有清理机制。通过 `createPool(name, config)` 创建的自定义线程池会被缓存，但只有显式调用 `shutdownPool(name)` 才会移除。如果代码创建了很多不同名称的池但从不关闭，会导致线程泄漏。

#### 2.6 AsyncChain 异步链

```
AsyncChain.supply(() -> fetchFromDb())
  .thenApply(data -> transform(data))
  .thenCompose(transformed -> saveToCache(transformed))
  .thenAccept(result -> log.info("done"))
  .onError(e -> log.error("failed", e))
  .timeout(5000)
  .execute()
```

**关键发现**：`timeout` 使用 `future.get(timeout, TimeUnit)` 实现。超时后会调用 `future.cancel(true)`，但 `cancel(true)` 只设置中断标志，不会强制停止线程。如果任务不检查中断，超时后任务仍会继续运行。

### 三、Sa-Token 用户上下文深度分析

**源码**：`SaTokenUserUtil.java`（264 行）

```
获取当前管理员信息:
  getCurrentAdmin()    → StpAdminUtil.getSession().getModel("userInfo", SysAdmin.class)
  getCurrentAdminId()  → StpAdminUtil.getLoginIdAsLong()
  isAdmin()            → StpAdminUtil.isLogin()

获取当前用户信息:
  getCurrentUser()     → StpUserUtil.getSession().getModel("userInfo", UserInfo.class)
  getCurrentUserId()   → StpUserUtil.getLoginIdAsLong()
  isUser()             → StpUserUtil.isLogin()

跨端信息查询（TODO 桩）:
  getUsernameById(userId)    → return "用户"  ← 未实现
  getUserAvatarById(userId)  → return defaultValue  ← 未实现
  getUserBioById(userId)     → return defaultValue  ← 未实现
```

**关键发现 1**：`getUsernameById`、`getUserAvatarById`、`getUserBioById` 是 TODO 桩方法，直接返回硬编码默认值。这意味着所有依赖这些方法获取其他用户信息的场景都拿不到真实数据。当前其他模块（如通知中心需要显示发送者名称）可能需要自行查询用户服务。

**关键发现 2**：所有方法都有 `try-catch` 返回 `null`。这意味着 Sa-Token Session 过期或异常时不会抛异常，而是静默返回 `null`。调用方必须处理 `null` 返回值。

**关键发现 3**：`isAdmin()` 和 `isUser()` 分别检查不同端的登录态。同一个人可以同时持有两个端的登录态（双端登录），`isAdmin() && isUser()` 可能为 `true`。

### 四、StpInterfaceImpl 权限加载

**源码**：`StpInterfaceImpl.java`（68 行）

```
getPermissionList(loginId, loginType):
  loginType == "admin" → adminPermissionService.getPermissionsByAdminId(loginId)
  loginType == "user"  → Collections.emptyList()

getRoleList(loginId, loginType):
  loginType == "admin" → adminRoleService.getRolesByAdminId(loginId)
  loginType == "user"  → Collections.emptyList()
```

**关键发现**：用户端始终返回空权限和空角色列表。这意味着 `@SaCheckRole` 和 `@SaCheckPermission` 对用户端无效——用户端的所有权限控制必须由业务代码自行实现。只有管理端支持 RBAC 权限模型。

### 五、AI 异常族设计

5 个 AI 异常类全部继承 `RuntimeException`，结构统一：

```
AiConfigurationException     — AI 配置缺失或无效（模型名、API Key、温度等）
AiInvocationException        — AI 调用失败（网络、超时、模型返回错误）
AiRetrievalException         — RAG 检索失败（向量库不可用、文档不存在）
AiGraphExecutionException    — LangGraph4j 图执行异常（节点执行失败、状态不一致）
AiStructuredOutputException  — 结构化输出解析失败（JSON Schema 不匹配、模型返回格式错误）
```

**关键设计**：所有 AI 异常都有 `(String message)` 和 `(String message, Throwable cause)` 两个构造器。异常会被 `GlobalExceptionHandler` 兜底捕获并返回 `INTERNAL_ERROR`，但 AI 模块内部应该有更细粒度的异常处理——比如配置异常返回明确提示、调用异常触发重试。

### 六、IP 解析安全风险

**源码**：`IPUtil.java`（47 行）

```
getIpAddress(request):
  1. X-Forwarded-For → 取第一个 IP（逗号分隔）
  2. null/unknown/空 → 尝试下一个 Header
  3. Proxy-Client-IP → ...
  4. WL-Proxy-Client-IP → ...
  5. HTTP_CLIENT_IP → ...
  6. HTTP_X_FORWARDED_FOR → ...
  7. request.getRemoteAddr() → 最后兜底
```

**关键发现**：X-Forwarded-For 可以被客户端伪造。攻击者可以在请求头中设置 `X-Forwarded-For: 1.2.3.4`，绕过基于 IP 的限流或审计。生产环境必须配置 Nginx `set_real_ip_from` 指定可信代理，并使用 `$realip_remote_addr` 而非原始 Header。

**与 SysAdminServiceImpl 的重复**：`SysAdminServiceImpl.getIpAddress()` 有完全相同的逻辑。这是代码重复——应该统一使用 `IPUtil`。

### 七、系统常量过时分析

**源码**：`Constants.java`（128 行）

| 常量 | 当前值 | 问题 | 建议 |
| --- | --- | --- | --- |
| `SYSTEM_VERSION` | `"1.0.0"` | 项目已到 v2.2.x，常量严重过时 | 从 pom.xml 读取或删除此常量 |
| `DEFAULT_PASSWORD` | `"123456"` | 安全风险：默认密码太弱 | 至少改为随机生成或强制首次修改 |
| `TOKEN_EXPIRE_TIME` | `7200L` | 2 小时可能对管理端太长 | 管理端考虑缩短到 30 分钟 |
| `REGEX_PASSWORD` | 需要 `小写+大写+数字，≥8位` | 不要求特殊字符 | 考虑增加特殊字符要求 |

### 八、深度发现与坑点

#### 8.1 已确认的代码问题

| 编号 | 问题 | 位置 | 影响 |
| --- | --- | --- | --- |
| BUG-1 | RateLimiter.acquire() 使用 busy-wait | `ConcurrentUtils.RateLimiter.acquire` | CPU 空转，高并发时浪费 |
| BUG-2 | CircuitBreaker 状态切换非原子 | `ConcurrentUtils.CircuitBreaker.onFailure` | 允许少量额外请求通过 |
| BUG-3 | POOL_CACHE 无清理机制 | `ThreadPoolUtils.POOL_CACHE` | 自定义池永不释放 |
| BUG-4 | SaTokenUserUtil 跨端查询是 TODO 桩 | `SaTokenUserUtil.getUsernameById` 等 | 返回硬编码值而非真实数据 |
| BUG-5 | Constants.SYSTEM_VERSION 过时 | `Constants.SYSTEM_VERSION = "1.0.0"` | 与实际版本不符 |
| BUG-6 | IPUtil 与 SysAdminServiceImpl 代码重复 | 两处 `getIpAddress()` | 维护时可能改一处忘另一处 |
| BUG-7 | 用户端权限始终返回空 | `StpInterfaceImpl.getPermissionList` | 用户端无法使用 RBAC |
| BUG-8 | AsyncChain timeout 不强制停止任务 | `ThreadPoolUtils.AsyncChain.timeout` | 超时后任务仍在后台运行 |

#### 8.2 设计层面的潜在风险

| 编号 | 风险 | 说明 |
| --- | --- | --- |
| RISK-1 | X-Forwarded-For 可伪造 | 攻击者可绕过基于 IP 的限流和审计 |
| RISK-2 | DEFAULT_PASSWORD = "123456" | 如果有批量创建账号场景，弱密码风险 |
| RISK-3 | 管理端角色/权限无缓存 | 每次请求查数据库，高频访问时有压力 |
| RISK-4 | 日志异步写入无兜底 | DB 异常时操作日志丢失，无本地文件或 MQ 备份 |
| RISK-5 | 虚拟线程反射检测 | JVM 版本或模块系统变化可能导致检测失败 |
| RISK-6 | RateLimiter synchronized 全局锁 | 高并发下单限流器实例的吞吐受限 |
| RISK-7 | HALF_OPEN 直接全量恢复 | 熔断恢复无慢启动，可能立即再次熔断 |
| RISK-8 | 敏感字段过滤硬编码 | 新增敏感参数需改切面代码，不可配置 |

#### 8.3 架构设计亮点

| 编号 | 亮点 | 说明 |
| --- | --- | --- |
| H-1 | 双端鉴权分仓 | 用户端和管理端 Token 完全隔离，互不影响 |
| H-2 | StripedLock 分段锁 | 不同 key 分散到不同锁，减少锁竞争 |
| H-3 | 指数退避 + 抖动 | 防止重试惊群，分布式最佳实践 |
| H-4 | 预配置多类型线程池 | COMMON/IO/CPU/SCHEDULER 按场景选用 |
| H-5 | 虚拟线程兼容检测 | Java 17 正常运行，Java 21+ 自动启用虚拟线程 |
| H-6 | CallerRunsPolicy | 拒绝策略保证任务不丢失 |
| H-7 | parallelMapFailFast | 快速失败模式避免浪费资源 |
| H-8 | 操作日志敏感参数过滤 | 密码类字段自动替换为星号 |
| H-9 | AI 异常细分 | 5 种异常区分配置、调用、检索、图执行、输出解析 |
| H-10 | Result 统一响应 | 所有 API 响应格式一致，前端处理逻辑统一 |

#### 8.4 源码导航速查

| 想了解 | 读什么 |
| --- | --- |
| 限流 | `ConcurrentUtils.java` — RateLimiter + SlidingWindowRateLimiter |
| 熔断 | `ConcurrentUtils.java` — CircuitBreaker 三状态转换 |
| 线程池 | `ThreadPoolUtils.java` — 4 个预配置池 + 自定义池 + 并行执行 |
| 重试 | `ThreadPoolUtils.java` — 指数退避 + 条件重试 |
| 双端鉴权 | `StpUserUtil.java` + `StpAdminUtil.java` + `SaTokenConfig.java` → 详见 [鉴权与用户体系](/modules/auth) |
| 权限加载 | `StpInterfaceImpl.java` — 管理端 RBAC + 用户端空列表 → 详见 [权限注解与角色边界索引](/reference/permission-boundaries) |
| 管理端拦截 | `RequireAdmin.java` + `AdminAuthAspect.java` → 详见 [权限注解与角色边界索引](/reference/permission-boundaries) |
| 操作日志 | `Log.java` + `LogAspect.java` — 异步写入 + 敏感过滤 |
| 统一响应 | `Result.java` + `ResultCode.java` + `PageResult.java` |
| 异常体系 | `BusinessException.java` + `GlobalExceptionHandler.java` + `ai/*.java` |
| 密码 | `PasswordUtil.java` — BCrypt + 自定义轮数 |
| IP 解析 | `IPUtil.java` — 6 层 Header 优先级 |
| 常量 | `Constants.java` — 版本号、缓存前缀、正则、默认值 |
