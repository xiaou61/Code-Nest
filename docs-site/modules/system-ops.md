# 系统运营后台

系统运营后台是管理员每天看平台状态、查日志、管配置、处理内容风险和发布版本信息的地方。它不是一个单独后端模块，而是多个底座能力在管理端的集合。

## 功能入口

| 功能 | 管理端入口 | 后端模块 |
| --- | --- | --- |
| 仪表盘 | `/dashboard` | `xiaou-system` |
| 用户管理 | `/user` | `xiaou-user`、`xiaou-system` |
| 登录日志 | `/logs/login` | `xiaou-system` |
| 操作日志 | `/logs/operation` | `xiaou-system` |
| 通知管理 | `/notification` | `xiaou-notification` |
| 敏感词 | `/sensitive/*` | `xiaou-sensitive` |
| 文件管理 | `/filestorage/*` | `xiaou-filestorage` |
| AI 配置 | `/system/ai-config` | `xiaou-system`、`xiaou-ai` |
| AI 治理 | `/system/ai-governance` | `xiaou-ai` |
| 版本管理 | `/system/version` | `xiaou-version` |

## 推荐学习顺序

系统运营后台适合按“每天会用什么”来学，不要把它当成一个单独业务模块。它更像一张后台工作台，把用户、日志、文件、通知、AI、版本这些模块拉到同一个管理视角里。

1. 先看仪表盘，理解管理员每天打开后台后第一眼看到什么。
2. 再看登录日志和操作日志，掌握排查“谁在什么时候做了什么”的基础证据。
3. 接着看文件、通知、敏感词这些平台能力，因为很多业务问题最终会落到这些底座。
4. 然后看 AI 配置与治理，理解 Prompt、RAG、回归和指标为什么要放进后台。
5. 最后看版本管理和新后台功能接入清单，学会把新功能纳入运营后台的统一治理。

读这篇时可以带着一个真实问题走：例如“用户说图片打不开”或“AI 回答质量下降”。先从仪表盘看异常，再去对应模块查明细，最后用日志还原操作时间线。

## 源码地图

| 位置 | 作用 |
| --- | --- |
| `xiaou-system/src/main/java/com/xiaou/system/controller/DashboardController.java` | 仪表盘接口 |
| `xiaou-system/src/main/java/com/xiaou/system/service/impl/SysDashboardServiceImpl.java` | 仪表盘数据聚合 |
| `xiaou-system/src/main/java/com/xiaou/system/controller/LogController.java` | 登录日志和操作日志查询/清理 |
| `xiaou-system/src/main/java/com/xiaou/system/service/impl/SysLoginLogServiceImpl.java` | 登录日志服务 |
| `xiaou-system/src/main/java/com/xiaou/system/service/impl/SysOperationLogServiceImpl.java` | 操作日志服务 |
| `xiaou-common/src/main/java/com/xiaou/common/annotation/Log.java` | 操作日志注解 |
| `xiaou-system/src/main/java/com/xiaou/system/controller/AiConfigController.java` | AI 配置、RAG、回归和指标 |
| `xiaou-version/src/main/java/com/xiaou/version/service/impl/VersionHistoryServiceImpl.java` | 版本历史业务 |

## 仪表盘总览

仪表盘接口是：

```text
GET /admin/dashboard/overview
```

它聚合这些数据：

| 指标 | 来源 |
| --- | --- |
| 用户总量 | `UserInfoService.getUserList` |
| 今日登录数 | `SysLoginLogService.getLoginLogPage` |
| 在线会话数 | 官方聊天室在线人数 |
| 今日失败操作数 | `SysOperationLogService.getOperationLogPage` |
| 累计积分发放 | `PointsService.getAdminStatistics` |
| 活跃积分用户数 | `PointsService.getAdminStatistics` |
| 模块健康 | 用户、积分、聊天室、登录日志、操作日志子查询耗时 |
| 最近操作 | 最近 4 条操作日志 |

`SysDashboardServiceImpl` 会给每个子查询计时。超过 800ms 标为 `warning`，查询异常标为 `danger`。所以仪表盘不只是数据看板，也能暴露后台依赖是否变慢。

## 登录日志

登录日志表是 `sys_login_log`，记录管理员登录：

| 字段 | 说明 |
| --- | --- |
| `admin_id` | 管理员 ID |
| `username` | 登录用户名 |
| `login_ip` | 登录 IP |
| `browser` | 浏览器 |
| `os` | 操作系统 |
| `login_status` | `0` 成功、`1` 失败 |
| `login_message` | 成功或失败原因 |
| `login_time` | 登录时间 |

接口：

| 接口 | 说明 |
| --- | --- |
| `POST /log/login` | 分页查询登录日志 |
| `GET /log/login/{id}` | 登录日志详情 |
| `DELETE /log/login` | 清空登录日志 |
| `GET /auth/login-logs` | 认证控制器里保留的查询入口 |

登录失败排查优先看这张表。用户名不存在、密码错误、账号禁用都会写入日志。

## 操作日志

操作日志表是 `sys_operation_log`，记录后台操作：

| 字段 | 说明 |
| --- | --- |
| `operation_id` | 操作 ID |
| `module` | 操作模块 |
| `operation_type` | `SELECT`、`INSERT`、`UPDATE`、`DELETE`、`EXPORT`、`IMPORT`、`CLEAN` 等 |
| `description` | 操作描述 |
| `method` | Java 方法 |
| `request_uri` | 请求 URI |
| `request_method` | HTTP 方法 |
| `request_params` | 请求参数 |
| `response_data` | 响应数据 |
| `operator_id` | 操作人 ID |
| `operator_name` | 操作人 |
| `operator_ip` | 操作 IP |
| `status` | `0` 成功、`1` 失败 |
| `error_msg` | 错误信息 |
| `cost_time` | 耗时毫秒 |

接口：

| 接口 | 说明 |
| --- | --- |
| `POST /log/operation` | 分页查询操作日志 |
| `GET /log/operation/{id}` | 操作日志详情 |
| `DELETE /log/operation` | 批量删除 |
| `DELETE /log/operation/all` | 清空 |
| `DELETE /log/operation/clean/{days}` | 清理指定天数前日志 |

新增后台高风险操作时，应使用 `@Log` 标注模块、类型和描述。涉及密码、Token、AI prompt、RAG 文档等敏感数据时，把 `saveRequestData` 或 `saveResponseData` 设为 `false`。

## `@Log` 操作类型

| 类型 | 含义 |
| --- | --- |
| `SELECT` | 查询 |
| `INSERT` | 新增 |
| `UPDATE` | 修改 |
| `DELETE` | 删除 |
| `GRANT` | 授权 |
| `EXPORT` | 导出 |
| `IMPORT` | 导入 |
| `FORCE` | 强退 |
| `GENCODE` | 生成代码 |
| `CLEAN` | 清空数据 |
| `OTHER` | 其他 |

日志的价值不是“多存几行数据”，而是事故发生时能回答三个问题：谁操作的、操作了什么、结果怎样。

## AI 配置与治理

AI 管理接口集中在：

```text
/admin/ai/config/**
```

主要能力：

| 能力 | 接口 |
| --- | --- |
| 运行配置摘要 | `GET /admin/ai/config/runtime` |
| Prompt/RAG/Schema 清单 | `GET /admin/ai/config/schema-catalog` |
| 回归用例目录 | `GET /admin/ai/config/regression/cases` |
| 最近一次回归结果 | `GET /admin/ai/config/regression/latest` |
| 回归历史 | `GET /admin/ai/config/regression/history` |
| 场景健康 | `GET /admin/ai/config/regression/scenario-health` |
| 执行回归 | `POST /admin/ai/config/regression/run` |
| Prompt 调试 | `POST /admin/ai/config/prompt-debug` |
| RAG 调试 | `POST /admin/ai/config/rag-debug` |
| RAG 服务健康 | `GET /admin/ai/config/rag-service/health` |
| RAG 文档列表 | `GET /admin/ai/config/rag-service/documents` |
| RAG 文档导入/导出/删除 | `/rag-service/documents/**` |
| AI 运行指标 | `GET /admin/ai/config/metrics` |
| 清空 AI 指标 | `DELETE /admin/ai/config/metrics` |
| 测试 AI 配置 | `POST /admin/ai/config/test` |

这些接口都需要管理员权限。执行回归、Prompt 调试、RAG 调试、文档导入导出、清空指标等接口都关闭了请求或响应日志保存，避免把大段 Prompt、文档内容或模型响应写入操作日志。

## 版本管理

版本历史模块服务于用户端版本时间线和后台发布管理。

| 接口域 | 说明 |
| --- | --- |
| `/version/**` | 用户端查看已发布版本 |
| `/admin/version/**` | 管理端创建、更新、发布、隐藏、删除 |

版本状态：

| 状态 | 含义 |
| --- | --- |
| `0` | 草稿 |
| `1` | 已发布 |
| `2` | 已隐藏 |

更新类型：

| 类型 | 含义 |
| --- | --- |
| `1` | 重大更新 |
| `2` | 功能更新 |
| `3` | 修复更新 |
| `4` | 其他 |

版本号在 `version_number + deleted` 上有唯一约束。创建和更新时会检查版本号冲突，发布时间要求 `yyyy-MM-dd HH:mm:ss` 格式。

## 系统运营模块怎么协作

| 场景 | 相关模块 |
| --- | --- |
| 管理员登录异常 | 鉴权、登录日志 |
| 用户反馈收不到消息 | 通知中心、用户账户 |
| 图片无法访问 | 文件存储、静态资源映射 |
| 内容发布被拒 | 敏感词、内容模块 |
| AI 功能质量下降 | AI 配置、AI 回归、运行指标 |
| 后台操作追责 | 操作日志、管理员账号 |
| 新版本发布 | 版本管理、通知公告 |

运营后台的一个好习惯是：先用仪表盘发现异常，再进入对应模块看明细，最后回到日志确认操作人和时间线。

## 高风险操作清单

| 操作 | 风险 | 建议 |
| --- | --- | --- |
| 清空登录日志 | 失去登录追踪 | 只在测试环境或归档后执行 |
| 清空操作日志 | 失去审计证据 | 生产环境需要二次确认 |
| 强制删除文件 | 存储对象和数据库记录都可能消失 | 操作前确认业务引用 |
| 设置默认存储 | 影响后续所有上传 | 先测试连接 |
| 导入 RAG 文档 | 影响 AI 检索质量 | 先小批量验证 |
| 执行 AI 回归 | 消耗模型资源 | 控制用例范围 |
| 发布版本 | 用户端可见 | 确认发布时间、状态和排序 |
| 同步敏感词来源 | 影响内容发布 | 先测试连接和预览变更 |

## 常见坑

| 问题 | 原因 | 建议 |
| --- | --- | --- |
| 仪表盘某模块显示异常 | 子查询抛异常，被标为 `danger` | 看应用日志里的“仪表板子查询失败” |
| 操作日志没有记录某个接口 | 方法未加 `@Log`，或日志切面未覆盖 | 新增后台接口时补日志注解 |
| 日志里出现敏感信息 | `saveRequestData` 或 `saveResponseData` 未关闭 | 密码、Token、Prompt、RAG 内容必须关闭 |
| 版本号已存在 | `version_number + deleted` 唯一约束冲突 | 换版本号或检查逻辑删除状态 |
| RAG 服务健康失败 | sidecar 未启动或鉴权配置不一致 | 先看 `/rag-service/health` |
| 后台接口被拒绝 | 缺少管理员登录或 `admin` 角色 | 检查 Token、角色和 `@RequireAdmin` |

## 新后台功能接入清单

1. 页面入口加入管理端路由和菜单。
2. 后端 Controller 方法加 `@RequireAdmin`。
3. 增删改、导入导出、清空类接口加 `@Log`。
4. 敏感请求/响应不要写入操作日志。
5. 如果会影响用户，考虑发通知或版本公告。
6. 如果会影响内容发布，补敏感词或审核策略说明。
7. 文档同步更新模块页、API 索引和操作手册。

## 验证清单

| 验证点 | 怎么验证 | 预期结果 |
| --- | --- | --- |
| 仪表盘聚合 | 管理员登录后请求 `GET /admin/dashboard/overview` | 返回用户、积分、在线人数、日志和模块健康数据 |
| 子模块异常隔离 | 临时让一个依赖查询失败或超时 | 仪表盘应标记对应模块异常，不应导致整个概览不可用 |
| 登录日志 | 执行一次成功登录和一次失败登录 | `sys_login_log` 记录状态、IP、浏览器、失败原因 |
| 操作日志查询 | 访问带 `@Log` 的后台增删改接口 | 能在操作日志中看到模块、类型、URI、操作者和耗时 |
| 敏感日志保护 | 调用修改密码、Prompt 调试、RAG 调试等接口 | 请求或响应大字段不应被完整写入操作日志 |
| AI 配置健康 | 请求 AI 配置、回归、RAG 健康接口 | 管理端能区分配置缺失、sidecar 不通和回归失败 |
| 版本发布 | 创建草稿后发布，再到用户端查看版本时间线 | 只有已发布版本对用户可见，隐藏后用户端不展示 |
| 高风险清理 | 在测试环境执行日志清理或文件强删 | 操作前有确认，操作后日志或文件状态符合预期 |

当前源码里可以看到大量 `@Log` 注解、日志服务和日志表。验证时还要确认运行包里"注解 -> 写入 `sys_operation_log`"的切面链路确实生效；如果只加了注解但日志没有落库，这属于接入不完整。

---

## 系统运营模块深度拆解

以下内容来自对 xiaou-system 模块全部源码的逐行阅读，覆盖 5 个 ServiceImpl、4 个 Controller、5 个 Domain、5 个 Mapper、30+ 个 DTO。

### 一、仪表盘聚合算法详解

**源码**：`SysDashboardServiceImpl.java:238`

```
getOverview():
  1. queryTotalUsers()     → 分页查 user_info, pageSize=1, 取 total
  2. pointsService.getAdminStatistics() → Feign 调积分模块
  3. queryOnlineUsers()     → chatRoomService + chatOnlineUserService
  4. queryTodayLoginCount() → 分页查 sys_login_log, 按时间+状态筛选, pageSize=1
  5. queryTodayFailedOperationCount() → 分页查 sys_operation_log, 按时间+失败状态, pageSize=1
  6. queryRecentOperations() → 分页查 sys_operation_log, pageSize=4
```

**TimedResult 内部类**：每个子查询都被 `timed()` 包装，记录执行耗时。超过 800ms 标为 `warning`，异常标为 `danger`。这意味着仪表盘本身就是模块健康检测器。

**关键发现**：`queryTotalUsers()` 使用 `pageSize=1` 的分页查询获取 total，这与用户模块统计的 4 次分页查询有相同的性能问题。应改用 `SELECT COUNT(*)` 直接统计。

**模块健康判定逻辑**：

| 耗时 | status | statusText | statusType |
| --- | --- | --- | --- |
| 异常 | danger | 异常 | danger |
| > 800ms | warning | 较慢 | warning |
| ≤ 800ms | healthy | 正常 | success |

### 二、管理员认证架构

#### 2.1 管理员登录流程

**源码**：`SysAdminServiceImpl.java:447`

```
login(request):
  1. 解析 User-Agent → 浏览器、OS
  2. 创建 SysLoginLog 对象
  3. 查询管理员: selectByUsername(username)
     ├─ 不存在 → 写日志(用户不存在) → throw "用户名或密码错误"
  4. 检查 status:
     ├─ status=1 → 写日志(用户禁用) → throw "用户已被禁用"
  5. 验证密码: PasswordUtil.matches()
     ├─ 不匹配 → 写日志(密码错误) → throw "用户名或密码错误"
  6. StpAdminUtil.login(adminId) → 创建管理端 Sa-Token 会话
  7. 更新 lastLoginTime, lastLoginIp, loginCount
  8. 写入 Session: userInfo, username
  9. 写日志(登录成功)
  10. 构建响应: accessToken + userInfo(含 roles + permissions)
```

**防枚举设计**：管理员端与用户端一致，用户不存在和密码错误都返回"用户名或密码错误"。

**日志写入时机**：每个失败分支都会写日志，包括系统异常 catch 块也写日志。这意味着登录日志是完整的审计记录。

#### 2.2 RBAC 权限模型

```
SysAdmin → (多对多) SysRole → (多对多) SysPermission
    │                         │
    └─ sys_admin_role         └─ sys_role_permission
```

**权限获取方法**：

```java
getAdminRoles(adminId)    → roleMapper.selectRolesByAdminId(adminId)
                                .map(role -> role.getRoleCode())

getAdminPermissions(adminId) → permissionMapper.selectPermissionsByAdminId(adminId)
                                   .map(p -> p.getPermissionCode())
```

**关键发现**：每次请求 `/auth/info` 都会查询数据库获取角色和权限，没有缓存。频繁请求时会有性能压力。

#### 2.3 IP 获取算法

**源码**：`SysAdminServiceImpl.getIpAddress()`

```
1. X-Forwarded-For (取第一个, 逗号分隔)
2. Proxy-Client-IP
3. WL-Proxy-Client-IP
4. HTTP_CLIENT_IP
5. HTTP_X_FORWARDED_FOR
6. request.getRemoteAddr()
```

**安全提醒**：X-Forwarded-For 可被客户端伪造。在生产环境应配置 Nginx `set_real_ip_from`，只信任代理服务器添加的 X-Forwarded-For。

### 三、操作日志注解体系

#### 3.1 `@Log` 注解定义

| 属性 | 类型 | 说明 | 默认值 |
| --- | --- | --- | --- |
| module | String | 操作模块 | "" |
| type | OperationType | 操作类型 | OTHER |
| description | String | 操作描述 | "" |
| saveRequestData | boolean | 是否保存请求参数 | true |
| saveResponseData | boolean | 是否保存响应数据 | true |

#### 3.2 切面处理逻辑

```
@Around 切面:
  1. 记录开始时间
  2. 执行目标方法
  3. 计算耗时 costTime
  4. 构建 SysOperationLog:
     - module, operationType, description 来自注解
     - method, requestUri, requestMethod 来自 Request
     - requestParams: saveRequestData=true 时序列化请求参数 (排除敏感字段)
     - responseData: saveResponseData=true 时序列化响应
     - operatorId, operatorName, operatorIp 来自当前管理员
     - status=0 (成功), costTime
  5. 异常时: status=1, errorMsg=异常信息
  6. 异步写入数据库
```

**敏感字段过滤**：请求参数中的 `password`, `oldPassword`, `newPassword`, `confirmPassword`, `token`, `accessToken`, `secret`, `apiKey` 会被替换为 `******`。

**关键发现**：日志写入是异步的，如果数据库连接异常，日志可能丢失。没有本地文件备份或消息队列兜底。

### 四、深度发现与坑点

#### 4.1 已确认的代码问题

| 编号 | 问题 | 位置 | 影响 |
| --- | --- | --- | --- |
| BUG-1 | 仪表盘统计使用 pageSize=1 分页取 total | `SysDashboardServiceImpl` | 性能差于 COUNT SQL |
| BUG-2 | 角色/权限查询无缓存 | `SysAdminServiceImpl.getAdminRoles/getAdminPermissions` | 每次请求查数据库 |
| BUG-3 | 日志异步写入无兜底 | `@Log` 切面 | DB 异常时日志丢失 |
| BUG-4 | page() 方法未实现分页 | `SysAdminServiceImpl.page` 注释"暂时返回全部" | 管理员列表无法分页 |

#### 4.2 设计层面的潜在风险

| 编号 | 风险 | 说明 |
| --- | --- | --- |
| RISK-1 | X-Forwarded-For 可伪造 | IP 来源不可信，需 Nginx 层面限制 |
| RISK-2 | 刷新 Token 返回相同 Token | `refresh` 接口直接返回当前 Token，未真正刷新 |
| RISK-3 | 敏感字段过滤硬编码 | 新增敏感参数需修改切面代码，不可配置 |

#### 4.3 架构设计亮点

| 编号 | 亮点 | 说明 |
| --- | --- | --- |
| H-1 | 仪表盘子查询容错 | 任一模块失败不影响整体展示 |
| H-2 | 模块健康监测 | 800ms 阈值自动标黄，异常标红 |
| H-3 | 敏感参数自动过滤 | 密码类字段替换为星号 |
| H-4 | 登录全路径日志 | 成功/失败/异常都写登录日志表 |
| H-5 | RBAC 细粒度控制 | 角色 + 权限二级模型 |

#### 4.4 源码导航速查

| 想了解 | 读什么 |
| --- | --- |
| 仪表盘聚合 | `SysDashboardServiceImpl.java` — TimedResult 容错+计时 |
| 管理员登录 | `SysAdminServiceImpl.login` — 完整 10 步+日志 |
| 操作日志切面 | `@Log` 注解 + `LogAspect` — 异步写入+敏感过滤 |
| RBAC 权限 | `SysRoleMapper` + `SysPermissionMapper` — 多对多查询 |
| 管理端认证 | `AuthController.java` — 登录/登出/刷新/信息 |
| 日志管理 | `LogController.java` — 登录日志+操作日志查询/清理 |
