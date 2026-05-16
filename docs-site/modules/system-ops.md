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

## 读源码先看哪里

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
