# 仪表盘与日志

仪表盘与日志服务于后台运营、审计和问题定位。仪表盘告诉管理员“系统现在是否健康”，日志告诉维护者“谁在什么时候做了什么，结果如何”。

## 功能入口

| 能力 | 管理端页面 | 后端接口 |
| --- | --- | --- |
| 仪表盘 | `/dashboard` | `GET /admin/dashboard/overview` |
| 登录日志 | `/logs/login` | `/log/login` |
| 操作日志 | `/logs/operation` | `/log/operation` |

源码位置：

| 层级 | 文件 |
| --- | --- |
| 仪表盘页面 | `vue3-admin-front/src/views/dashboard/index.vue` |
| 仪表盘 API | `vue3-admin-front/src/api/dashboard.js` |
| 日志页面 | `vue3-admin-front/src/views/logs/` |
| 仪表盘 Controller | `xiaou-system/src/main/java/com/xiaou/system/controller/DashboardController.java` |
| 日志 Controller | `xiaou-system/src/main/java/com/xiaou/system/controller/LogController.java` |
| 仪表盘 Service | `xiaou-system/src/main/java/com/xiaou/system/service/impl/SysDashboardServiceImpl.java` |
| 登录日志 Service | `xiaou-system/src/main/java/com/xiaou/system/service/impl/SysLoginLogServiceImpl.java` |
| 操作日志 Service | `xiaou-system/src/main/java/com/xiaou/system/service/impl/SysOperationLogServiceImpl.java` |
| 日志注解 | `xiaou-common/src/main/java/com/xiaou/common/annotation/Log.java` |

注意：前端页面路由是 `/logs/login`、`/logs/operation`，后端接口前缀是 `/log`，不要把二者混淆。

## 仪表盘聚合了什么

`SysDashboardServiceImpl#getOverview` 会分别查询多个子系统，并记录每个子查询耗时：

| 指标 | 来源 |
| --- | --- |
| 总用户数 | `UserInfoService#getUserList` |
| 今日登录成功次数 | `SysLoginLogService#getLoginLogPage`，状态 `0` |
| 官方聊天室在线人数 | `ChatRoomService#getOfficialRoom` + `ChatOnlineUserService#getOnlineCount` |
| 今日失败操作次数 | `SysOperationLogService#getOperationLogPage`，状态 `1` |
| 总发放积分 | `PointsService#getAdminStatistics` |
| 活跃积分用户数 | `PointsService#getAdminStatistics` |
| 最近操作 | 操作日志分页取最近 4 条 |
| 模块健康度 | 用户、积分、聊天室、登录日志、操作日志的耗时和异常状态 |

健康度规则：

| 条件 | 状态 |
| --- | --- |
| 子查询异常 | `danger`，显示“异常” |
| 子查询耗时大于 800ms | `warning`，显示“较慢” |
| 其他 | `healthy`，显示“正常” |

这里的设计重点是“单个指标失败不拖垮整个仪表盘”。每个子查询都通过 `timed` 包裹，失败时返回默认值并记录 warn 日志。

## 登录日志

登录日志实体 `SysLoginLog` 记录管理员登录行为：

| 字段 | 说明 |
| --- | --- |
| `adminId` | 管理员 ID |
| `username` | 登录用户名 |
| `loginIp` | 登录 IP |
| `loginLocation` | 登录地点 |
| `browser` | 浏览器 |
| `os` | 操作系统 |
| `loginStatus` | 0 成功，1 失败 |
| `loginMessage` | 登录结果描述 |
| `loginTime` | 登录时间 |

接口：

| 接口 | 方法 | 说明 |
| --- | --- | --- |
| `/log/login` | `POST` | 分页查询，支持用户名、IP、状态、时间范围 |
| `/log/login/{id}` | `GET` | 查询详情 |
| `/log/login` | `DELETE` | 清空登录日志 |

所有日志查询和清理接口都标注 `@RequireAdmin`，需要管理员权限。

## 操作日志

操作日志实体 `SysOperationLog` 记录管理端关键操作：

| 字段 | 说明 |
| --- | --- |
| `operationId` | 操作唯一 ID |
| `module` | 操作模块 |
| `operationType` | 操作类型 |
| `description` | 操作描述 |
| `method` | Java 方法 |
| `requestUri` | 请求 URI |
| `requestMethod` | HTTP 方法 |
| `requestParams` | 请求参数 |
| `responseData` | 响应数据 |
| `operatorId` | 操作人 ID |
| `operatorName` | 操作人姓名 |
| `operatorIp` | 操作 IP |
| `operationLocation` | 操作地点 |
| `browser` | 浏览器 |
| `os` | 操作系统 |
| `status` | 0 成功，1 失败 |
| `errorMsg` | 错误信息 |
| `operationTime` | 操作时间 |
| `costTime` | 耗时毫秒 |

接口：

| 接口 | 方法 | 说明 |
| --- | --- | --- |
| `/log/operation` | `POST` | 分页查询，支持模块、类型、操作人、状态、时间范围 |
| `/log/operation/{id}` | `GET` | 查询详情 |
| `/log/operation` | `DELETE` | 批量删除 |
| `/log/operation/all` | `DELETE` | 清空 |
| `/log/operation/clean/{days}` | `DELETE` | 清理指定天数之前的日志 |

`SysOperationLogServiceImpl#saveOperationLog` 采用“失败不影响业务”的策略：插入操作日志失败时只记录错误日志，不回滚业务操作。

## `@Log` 注解

`@Log` 用来标注需要审计的接口：

| 属性 | 说明 |
| --- | --- |
| `module` | 模块名称 |
| `type` | 操作类型 |
| `description` | 操作说明 |
| `saveRequestData` | 是否保存请求参数，默认 true |
| `saveResponseData` | 是否保存响应数据，默认 true |

操作类型：

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

当前源码可以看到注解、日志表、日志服务和日志查询接口。需要特别说明的是，当前源码搜索没有找到 `LogAspect` 或类似的“读取 `@Log` 并写入 `SysOperationLog`”的 AOP 实现。如果运行时操作日志没有入库，优先检查这个切面是否缺失、是否在别的分支、或是否未被 Spring 扫描。

## 敏感数据处理

以下接口不应该保存完整请求或响应：

| 场景 | 建议 |
| --- | --- |
| 登录 | `saveResponseData = false`，避免记录 Token |
| 修改密码 | `saveRequestData = false` |
| AI Prompt 调试 | 请求和响应都关闭 |
| RAG 文档导入 | 请求和响应都关闭 |
| 文件导入导出 | 至少不要记录完整文件内容 |
| Token、密钥、验证码 | 不写日志，或脱敏后写摘要 |

新增后台接口时，如果请求里有密码、Token、模型提示词、用户隐私、文件内容，就不要依赖默认值。

## 排查问题时怎么用

| 问题 | 看哪里 |
| --- | --- |
| 管理员说登录失败 | 查登录日志的 `loginStatus`、`loginIp`、`loginMessage` |
| 某条数据被误删 | 查操作日志的 `module`、`operationType = DELETE`、`operatorName`、`requestUri` |
| 仪表盘显示模块异常 | 看模块健康列表和后端 warn 日志 |
| 最近操作为空 | 确认操作日志表有数据、AOP 是否生效 |
| 清理日志后仍然很多 | 检查清理接口使用的是保留天数还是删除天数 |

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
