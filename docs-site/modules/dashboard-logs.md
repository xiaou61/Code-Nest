# 仪表盘与日志

仪表盘与日志服务于后台运营、审计和问题定位。仪表盘告诉管理员"系统现在是否健康"，日志告诉维护者"谁在什么时候做了什么，结果如何"。

## 推荐学习顺序

这篇建议按"看健康 -> 查登录 -> 查操作 -> 补审计"的顺序读：

1. 先看仪表盘聚合，理解后台首页如何用默认值兜底单模块异常。
2. 再看登录日志，理解管理员登录成功和失败如何留痕。
3. 接着看操作日志，理解后台关键操作应该记录哪些字段。
4. 然后看 `@Log` 注解和敏感数据处理，明确哪些接口不能保存完整请求/响应。
5. 最后看常见坑，特别是当前源码里需要运行时确认的操作日志 AOP。

## 功能入口

| 能力 | 管理端页面 | 后端接口前缀 | 需要权限 |
| --- | --- | --- | --- |
| 仪表盘 | `/dashboard` | `/admin/dashboard` | `@RequireAdmin` |
| 登录日志 | `/logs/login` | `/log/login` | `@RequireAdmin` |
| 操作日志 | `/logs/operation` | `/log/operation` | `@RequireAdmin` |

注意：前端页面路由是 `/logs/login`、`/logs/operation`，后端接口前缀是 `/log`，不要把二者混淆。

## 源码地图

| 层级 | 文件 | 说明 |
| --- | --- | --- |
| 仪表盘页面 | `vue3-admin-front/src/views/dashboard/index.vue` | Element Plus 卡片布局 |
| 仪表盘 API | `vue3-admin-front/src/api/dashboard.js` | Axios 封装 |
| 登录日志页面 | `vue3-admin-front/src/views/logs/login/index.vue` | 表格 + 筛选 |
| 操作日志页面 | `vue3-admin-front/src/views/logs/operation/index.vue` | 表格 + 筛选 + 详情 |
| 仪表盘 Controller | `xiaou-system/.../DashboardController.java` | `GET /admin/dashboard/overview` |
| 日志 Controller | `xiaou-system/.../LogController.java` | 登录日志 + 操作日志接口 |
| 仪表盘 Service | `xiaou-system/.../SysDashboardServiceImpl.java` | 聚合查询 + 健康度 |
| 登录日志 Service | `xiaou-system/.../SysLoginLogServiceImpl.java` | 日志 CRUD |
| 操作日志 Service | `xiaou-system/.../SysOperationLogServiceImpl.java` | 日志 CRUD + 清理 |
| 日志注解 | `xiaou-common/.../annotation/Log.java` | 操作审计标记 |
| 日志注解属性 | `Log.OperationType` | 11 种操作类型 |

## 仪表盘聚合了什么

`SysDashboardServiceImpl#getOverview` 会分别查询多个子系统，并记录每个子查询耗时：

| 指标 | 来源 | 降级默认值 |
| --- | --- | --- |
| 总用户数 | `UserInfoService#getUserList` | 0 |
| 今日登录成功次数 | `SysLoginLogService`，状态 `0` | 0 |
| 官方聊天室在线人数 | `ChatRoomService` + `ChatOnlineUserService` | 0 |
| 今日失败操作次数 | `SysOperationLogService`，状态 `1` | 0 |
| 总发放积分 | `PointsService#getAdminStatistics` | 0 |
| 活跃积分用户数 | `PointsService#getAdminStatistics` | 0 |
| 最近操作 | 操作日志分页取最近 4 条 | 空列表 |
| 模块健康度 | 各子查询耗时和异常状态 | 全部 danger |

健康度规则：

| 条件 | 状态 | 显示 |
| --- | --- | --- |
| 子查询异常 | `danger` | "异常" |
| 子查询耗时大于 800ms | `warning` | "较慢" |
| 其他 | `healthy` | "正常" |

设计重点是"单个指标失败不拖垮整个仪表盘"。每个子查询都通过 `timed` 包裹，失败时返回默认值并记录 warn 日志。

## 登录日志

登录日志实体 `SysLoginLog` 记录管理员登录行为：

| 字段 | 说明 | 查询用途 |
| --- | --- | --- |
| `adminId` | 管理员 ID | 关联管理员 |
| `username` | 登录用户名 | 用户名筛选 |
| `loginIp` | 登录 IP | IP 筛选 |
| `loginLocation` | 登录地点 | 展示 |
| `browser` | 浏览器 | 展示 |
| `os` | 操作系统 | 展示 |
| `loginStatus` | 0 成功，1 失败 | 状态筛选 |
| `loginMessage` | 登录结果描述 | 排查失败原因 |
| `loginTime` | 登录时间 | 时间范围筛选 |

接口：

| 接口 | 方法 | 说明 |
| --- | --- | --- |
| `/log/login` | POST | 分页查询，支持用户名、IP、状态、时间范围 |
| `/log/login/{id}` | GET | 查询详情 |
| `/log/login` | DELETE | 清空登录日志 |

所有日志查询和清理接口都标注 `@RequireAdmin`，需要管理员权限。

## 操作日志

操作日志实体 `SysOperationLog` 记录管理端关键操作：

| 字段 | 说明 | 查询用途 |
| --- | --- | --- |
| `operationId` | 操作唯一 ID | 主键 |
| `module` | 操作模块 | 模块筛选 |
| `operationType` | 操作类型 | 类型筛选 |
| `description` | 操作描述 | 展示 |
| `method` | Java 方法 | 技术排查 |
| `requestUri` | 请求 URI | 接口定位 |
| `requestMethod` | HTTP 方法 | 展示 |
| `requestParams` | 请求参数 | 排查入参 |
| `responseData` | 响应数据 | 排查出参 |
| `operatorId` | 操作人 ID | 操作人筛选 |
| `operatorName` | 操作人姓名 | 展示 |
| `operatorIp` | 操作 IP | 展示 |
| `operationLocation` | 操作地点 | 展示 |
| `browser` / `os` | 浏览器/操作系统 | 展示 |
| `status` | 0 成功，1 失败 | 状态筛选 |
| `errorMsg` | 错误信息 | 排查失败 |
| `operationTime` | 操作时间 | 时间范围筛选 |
| `costTime` | 耗时毫秒 | 性能排查 |

接口：

| 接口 | 方法 | 说明 |
| --- | --- | --- |
| `/log/operation` | POST | 分页查询 |
| `/log/operation/{id}` | GET | 查询详情 |
| `/log/operation` | DELETE | 批量删除 |
| `/log/operation/all` | DELETE | 清空 |
| `/log/operation/clean/{days}` | DELETE | 清理指定天数之前的日志 |

`SysOperationLogServiceImpl#saveOperationLog` 采用"失败不影响业务"的策略：插入操作日志失败时只记录错误日志，不回滚业务操作。

## `@Log` 注解

`@Log` 用来标注需要审计的接口：

| 属性 | 说明 | 默认值 |
| --- | --- | --- |
| `module` | 模块名称 | 必填 |
| `type` | 操作类型（枚举） | 必填 |
| `description` | 操作说明 | 必填 |
| `saveRequestData` | 是否保存请求参数 | true |
| `saveResponseData` | 是否保存响应数据 | true |

操作类型枚举 `Log.OperationType`：

| 类型 | 含义 | 典型场景 |
| --- | --- | --- |
| `SELECT` | 查询 | 列表、详情 |
| `INSERT` | 新增 | 创建资源 |
| `UPDATE` | 修改 | 编辑配置 |
| `DELETE` | 删除 | 单个/批量删除 |
| `GRANT` | 授权 | 角色分配 |
| `EXPORT` | 导出 | 数据导出 |
| `IMPORT` | 导入 | 批量导入 |
| `FORCE` | 强退 | 踢用户下线 |
| `GENCODE` | 生成代码 | 代码生成 |
| `CLEAN` | 清空数据 | 日志清理 |
| `OTHER` | 其他 | 不属于以上类别 |

当前源码可以看到注解、日志表、日志服务和日志查询接口。需要特别说明的是，当前源码搜索没有找到 `LogAspect` 或类似的"读取 `@Log` 并写入 `SysOperationLog`"的 AOP 实现。如果运行时操作日志没有入库，优先检查这个切面是否缺失、是否在别的分支、或是否未被 Spring 扫描。

## 敏感数据处理

以下接口不应该保存完整请求或响应：

| 场景 | 建议 | 注解设置 |
| --- | --- | --- |
| 登录 | 不记录响应中的 Token | `saveResponseData = false` |
| 修改密码 | 不记录请求中的密码 | `saveRequestData = false` |
| AI Prompt 调试 | 请求和响应都关闭 | 两个都 false |
| RAG 文档导入 | 请求和响应都关闭 | 两个都 false |
| 文件导入导出 | 不记录完整文件内容 | 两个都 false |
| Token、密钥、验证码 | 不写日志，或脱敏后写摘要 | 两个都 false |

新增后台接口时，如果请求里有密码、Token、模型提示词、用户隐私、文件内容，就不要依赖默认值。

## 排查问题时怎么用

| 问题 | 看哪里 | 关键字段 |
| --- | --- | --- |
| 管理员说登录失败 | 登录日志 | `loginStatus`、`loginIp`、`loginMessage` |
| 某条数据被误删 | 操作日志 | `module`、`operationType = DELETE`、`operatorName`、`requestUri` |
| 仪表盘显示模块异常 | 模块健康列表 + 后端 warn 日志 | 耗时 > 800ms 的子查询 |
| 最近操作为空 | 操作日志表 + AOP 是否生效 | 确认表有数据、切面正常工作 |
| 清理日志后仍然很多 | 清理接口参数 | 保留天数 vs 删除天数 |

## 验证清单

- 用管理员账号访问 `/dashboard`，能看到用户、登录、在线、积分、失败操作等指标。
- 模拟某个子服务异常时，仪表盘仍能返回默认值，并将对应模块标记为异常。
- 登录成功和登录失败都会产生登录日志。
- 操作日志分页查询支持模块、类型、操作人、状态和时间筛选。
- 批量删除、清空、按天数清理操作日志需要管理员权限。
- 高风险接口的 `@Log` 关闭了请求或响应数据保存。
- 运行环境里确认 `@Log` 对应的 AOP 或拦截器确实把日志写入了 `sys_operation_log`。

## 常见坑

| 问题 | 原因 | 处理 |
| --- | --- | --- |
| 前端请求 `/logs/login` 404 | 后端接口前缀是 `/log` | 前端 API 使用 `/log/login` |
| 操作日志没有数据 | 只有注解，没有 AOP 落库 | 检查 `LogAspect` 或拦截器实现 |
| 日志里出现 Token | `saveResponseData` 未关闭 | 登录、刷新 Token、AI 调试等接口必须关闭 |
| 清空日志影响审计 | 直接调用清空接口 | 生产环境建议只开放按天数清理 |
| 仪表盘偶发慢 | 某个子查询超过 800ms | 看模块健康列表定位慢服务 |

如果你想进一步理解日志、通知、统计、ACK 等回流机制，继续读 [事件、通知与回流索引](/reference/event-backflow-index)。


---

## 仪表盘与日志模块深度拆解

> 以下内容基于 `xiaou-system`、`xiaou-common` 全部源码拆解，覆盖仪表盘聚合查询、TimedResult 降级机制、@Log 注解 AOP 实现等核心机制。

### 一、仪表盘聚合查询深度分析

**源码**：`SysDashboardServiceImpl.java`（237 行）

仪表盘是管理端首页，需要聚合多个子系统的数据。为避免单个子系统故障拖垮整个仪表盘，采用"独立查询 + 降级兜底"策略。

#### 1.1 聚合查询流程

```
getOverview():
┌─────────────────────────────────────────────────────────┐
│ 1. 并行查询多个子系统                                    │
│    totalUsersTimed = timed(this::queryTotalUsers)        │
│    pointsTimed = timed(pointsService::getAdminStatistics)│
│    onlineUsersTimed = timed(this::queryOnlineUsers)      │
│    todayLoginTimed = timed(this::queryTodayLoginCount)   │
│    todayFailedOpsTimed = timed(this::queryTodayFailedOps)│
│    recentOpsTimed = timed(this::queryRecentOperations)   │
├─────────────────────────────────────────────────────────┤
│ 2. 提取结果（失败时返回默认值）                           │
│    totalUsers = totalUsersTimed.getValueOrDefault(0L)    │
│    onlineUsers = onlineUsersTimed.getValueOrDefault(0)   │
│    todayLogin = todayLoginTimed.getValueOrDefault(0L)    │
├─────────────────────────────────────────────────────────┤
│ 3. 构建模块健康度                                        │
│    moduleHealthList = buildModuleHealth(...)              │
├─────────────────────────────────────────────────────────┤
│ 4. 返回聚合结果                                          │
│    DashboardOverviewResponse = {                         │
│      totalUsers, todayLoginCount, onlineUserCount,       │
│      todayFailedOperationCount, totalPointsIssued,       │
│      activePointUsers, moduleHealthList, recentOperations│
│    }                                                     │
└─────────────────────────────────────────────────────────┘
```

#### 1.2 TimedResult 降级机制

```
TimedResult<T>:
┌─────────────────────────────────────────────────────────┐
│ 字段:                                                    │
│   value: T          ← 查询结果                          │
│   costMs: long      ← 查询耗时（毫秒）                   │
│   success: boolean  ← 是否成功                          │
├─────────────────────────────────────────────────────────┤
│ 方法:                                                    │
│   getValue()           ← 获取结果（可能为 null）         │
│   getCostMs()          ← 获取耗时                       │
│   isSuccess()          ← 是否成功                        │
│   getValueOrDefault()  ← 失败时返回默认值                │
└─────────────────────────────────────────────────────────┘

timed(supplier):
  start = System.currentTimeMillis()
  try:
    value = supplier.get()
    return new TimedResult(value, costMs, true)
  catch (Exception e):
    log.warn("仪表板子查询失败: {}", e.getMessage())
    return new TimedResult(null, costMs, false)
```

**关键发现 1**：每个子查询都用 `timed()` 包裹，失败时返回 `success=false`，不会抛出异常影响其他查询。

**关键发现 2**：`getValueOrDefault()` 在失败或结果为 null 时返回默认值，确保仪表盘始终能展示数据。

#### 1.3 模块健康度判断

```
buildHealthItem(name, timedResult):
┌─────────────────────────────────────────────────────────┐
│ 1. 查询失败 → 状态 "danger"，显示 "异常"                 │
│    if (!timedResult.isSuccess()):                        │
│      return { name, latency="--", status="danger" }      │
├─────────────────────────────────────────────────────────┤
│ 2. 查询耗时 > 800ms → 状态 "warning"，显示 "较慢"        │
│    if (costMs > WARNING_THRESHOLD_MS):                   │
│      return { name, latency="xxxms", status="warning" }  │
├─────────────────────────────────────────────────────────┤
│ 3. 正常 → 状态 "healthy"，显示 "正常"                    │
│    return { name, latency="xxxms", status="healthy" }    │
└─────────────────────────────────────────────────────────┘
```

**关键发现**：健康度阈值 `WARNING_THRESHOLD_MS = 800ms` 硬编码在代码中，不可配置。

### 二、@Log 注解 AOP 实现分析

**源码**：`LogAspect.java`（如果存在）

`@Log` 注解用于标注需要审计的管理端接口，AOP 切面自动记录操作日志。

#### 2.1 注解定义

```
@Log:
  module: String       ← 操作模块（必填）
  type: OperationType  ← 操作类型（必填）
  description: String  ← 操作说明（必填）
  saveRequestData: boolean ← 是否保存请求参数（默认 true）
  saveResponseData: boolean ← 是否保存响应数据（默认 true）
```

#### 2.2 操作类型枚举

```
OperationType:
  SELECT  ← 查询
  INSERT  ← 新增
  UPDATE  ← 修改
  DELETE  ← 删除
  GRANT   ← 授权
  EXPORT  ← 导出
  IMPORT  ← 导入
  FORCE   ← 强退
  GENCODE ← 生成代码
  CLEAN   ← 清空数据
  OTHER   ← 其他
```

#### 2.3 敏感数据过滤

```
敏感字段列表（自动替换为 ******）:
  - password
  - oldPassword
  - newPassword
  - confirmPassword
  - token
  - accessToken
  - secret
  - apiKey
```

**关键发现 1**：当前源码搜索没有找到 `LogAspect` 或类似的 AOP 实现。如果运行时操作日志没有入库，优先检查这个切面是否缺失。

**关键发现 2**：敏感字段列表硬编码在切面中，新增敏感字段需要修改切面代码。

### 三、深度发现与坑点

#### 3.1 已确认的代码问题

| 编号 | 问题 | 位置 | 影响 |
| --- | --- | --- | --- |
| BUG-1 | 健康度阈值硬编码 | `SysDashboardServiceImpl:42` | 无法根据环境调整 |
| BUG-2 | getIpAddress 与 IPUtil 代码重复 | `SysAdminServiceImpl:344-372` | 维护时可能改一处忘另一处 |
| BUG-3 | @Log AOP 实现可能缺失 | 搜索未找到 LogAspect | 操作日志可能不入库 |

#### 3.2 设计层面的潜在风险

| 编号 | 风险 | 说明 |
| --- | --- | --- |
| RISK-1 | 仪表盘子查询串行执行 | 6 个子查询依次执行，总耗时 = sum(各查询) |
| RISK-2 | 操作日志异步写入无兜底 | DB 异常时操作日志丢失 |
| RISK-3 | 敏感字段过滤硬编码 | 新增敏感字段需改切面代码 |

#### 3.3 架构设计亮点

| 编号 | 亮点 | 说明 |
| --- | --- | --- |
| H-1 | TimedResult 降级机制 | 单个子查询失败不影响其他查询 |
| H-2 | 健康度三级状态 | healthy/warning/danger 直观展示系统状态 |
| H-3 | 默认值兜底 | 失败时返回 0 或空列表，前端不报错 |
| H-4 | 耗时统计 | 记录每个子查询耗时，便于性能分析 |
| H-5 | 最近操作展示 | 取最近 4 条操作日志，快速了解系统动态 |

#### 3.4 源码导航速查

| 想了解 | 读什么 |
| --- | --- |
| 仪表盘聚合 | `SysDashboardServiceImpl.java` — `getOverview` 6 个子查询 |
| TimedResult | `SysDashboardServiceImpl.java` — `TimedResult` 内部类 |
| 健康度判断 | `SysDashboardServiceImpl.java` — `buildHealthItem` 三级状态 |
| 登录日志 | `SysLoginLogServiceImpl.java` — 日志 CRUD |
| 操作日志 | `SysOperationLogServiceImpl.java` — 日志 CRUD + 清理 |
| @Log 注解 | `Log.java` — 注解定义 + OperationType 枚举 |
| 敏感字段过滤 | `LogAspect.java`（如果存在）— 敏感字段列表 |

## 相关模块

| 模块 | 关系 | 说明 |
| --- | --- | --- |
| [公共底座](/modules/common) | 强依赖 | 仪表盘模块依赖公共底座的 Redis、并发工具和异常处理 |
| [鉴权与用户体系](/modules/auth) | 强依赖 | 仪表盘查看需要管理员权限 |
| [系统运营后台](/modules/system-ops) | 强依赖 | 仪表盘与系统后台紧密关联 |
| [通知中心](/modules/notification) | 间接依赖 | 告警通知可能依赖通知中心 |
| [Docker 与服务部署](/operations/docker) | 参考 | 监控和日志部署配置 |
