---
artifact: spec
status: active
scope: 系统管理与用户账号
---

# 系统管理与用户账号

`xiaou-system` 名义上是"系统管理模块"，实际由两条差异极大的主线拼成：一条是传统管理端底座（管理员登录、登录/操作日志、仪表板），另一条是"管理员 AI 智能体运行时 + AI 配置/RAG/回归调试台"，后者占了该模块约九成文件量。`xiaou-user` 是面向 C 端的账号域（注册、登录、验证码、资料、管理端用户管理），并把用户资料查询能力以 SPI 形式注册给 `xiaou-common`。

## 模块总览

| 模块 | 职责一句话 | 关键入口（包/类/文件） |
| --- | --- | --- |
| xiaou-system | 管理端身份底座（登录/日志/仪表板）+ 管理员 AI 智能体运行时 + AI 配置与 RAG 调试台 | `controller/AuthController`、`controller/LogController`、`controller/DashboardController`、`controller/AgentChatController`、`controller/AiConfigController`、`agent/`、`service/impl/SysAiConfigServiceImpl.java` |
| xiaou-user | C 端账号域：注册/登录/验证码/资料维护/头像上传 + 管理端用户管理 + 用户资料 SPI 供其他模块复用 | `controller/UserAuthController`、`controller/UserController`、`controller/CaptchaController`、`controller/AdminUserController`、`config/CommonUserProfileResolverConfig.java` |

### xiaou-system 205 个 Java 文件的构成（重点追查结论）

| 位置 | 文件数 | 说明 |
| --- | --- | --- |
| `src/main/java/**/dto` | 59 | AI 配置/RAG/回归 39 + 智能体聊天与审计 11 + 认证与日志 9 |
| `src/main/java/**/agent` | 31 | 智能体核心：注册表、策略引擎、计划解析（确定性/LLM）、会话上下文仓库（内存/Redis/DB）、指标记录 |
| `src/main/java/**/agent/tools` | 29 | 单个 `AgentTool` 实现（审计、策略/计划/请求 dry-run、运行观测、日志清理、聊天封禁、抽奖监控、SRE RCA 等） |
| `src/main/java/**/service` + `service/impl` + `support` | 13 | 6 个接口 + 6 个实现 + `AiRegressionRunStateRepository` |
| `src/main/java/**/domain` | 7 | sys_admin / sys_role / sys_permission / sys_login_log / sys_operation_log / sys_agent_audit / sys_agent_session_context |
| `src/main/java/**/mapper` | 7 | 与 domain 一一对应（无关联表 Mapper） |
| `src/main/java/**/controller` | 5 | 见下 |
| `src/main/java/**/utils`、`config` | 3 | `JwtKeyGenerator`、`PasswordGenerator`、`OpenApiConfig` |
| `src/test/java/**` | 51 | 集中覆盖 `agent`、`agent/tools`、`controller`、`service/impl` |

**不存在的内容**：无代码生成器、无字典管理、无定时任务调度、无 `sys_config` 系统参数、无独立监控模块、无角色/权限管理 Controller。该模块 5 个 Controller 之所以撑起 205 个文件，是因为"智能体运行时"以工具类逐文件膨胀（60 个文件）+ 调试台 DTO 成对展开（59 个）。

## 模块详情

## xiaou-system（xiaou-system）

**职责与边界**

- 管理端身份：管理员登录/登出/刷新/资料/改密（`controller/AuthController.java`）。
- 管理端日志：登录日志与操作日志的查询、删除、清理（`controller/LogController.java`）。
- 管理端仪表板总览（`controller/DashboardController.java`）。
- 管理员智能体唯一深接口 `/admin/agent/chat`（`controller/AgentChatController.java` → `agent/AgentChatOrchestrator.java`）。
- AI 配置/RAG/回归调试台（`controller/AiConfigController.java` → `service/impl/SysAiConfigServiceImpl.java`，1653 行）。
- 边界：不承载业务域的增删改；`xiaou-system` **依赖** `xiaou-user`、`xiaou-chat`、`xiaou-points`、`xiaou-ai`、`xiaou-sre`（`xiaou-system/pom.xml` 第 48-80、59-63 行）做仪表板聚合与 AI 能力，反向无依赖。

**核心领域对象**

| 类 | 关键字段与语义 |
| --- | --- |
| `domain/SysAdmin.java` | id/username/password/realName/email/phone/avatar/gender/status(0 正常 1 禁用 2 删除)/lastLoginTime/lastLoginIp/loginCount；`roles`、`permissions` 为不落库的运行时装配字段 |
| `domain/SysRole.java` | roleName/roleCode(`^[A-Z_]+$`)/status/sortOrder；`permissions` 不落库 |
| `domain/SysPermission.java` | parentId(0 为顶级)/permissionName/permissionCode/permissionType(0 菜单 1 按钮 2 接口)/path/component/icon/sortOrder/status；`children` 不落库 |
| `domain/SysLoginLog.java` | adminId/username/loginIp/loginLocation/browser/os/loginStatus(0 成功 1 失败)/loginMessage/loginTime |
| `domain/SysOperationLog.java` | operationId/module/operationType/description/method/requestUri/requestMethod/requestParams/responseData/operatorId/operatorName/operatorIp/browser/os/status/errorMsg/operationTime/costTime |
| `domain/SysAgentAudit.java` | auditId/confirmationId/idempotencyKey/intent/actionId/route/riskLevel/riskCategory/status/expectedStatus(不落库)/payloadJson/diffJson/planJson/resultJson/operatorId |
| `domain/SysAgentSessionContext.java` | sessionId/turnsJson/createdTime/updatedTime |

**对外接口**

管理端（`/auth/**`、`/admin/**`，除登录/注册/刷新外均需 `StpAdminUtil` 登录）：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/auth/login` | 管理员登录，免登录白名单 |
| POST | `/auth/logout` | `StpAdminUtil.logout()` |
| POST | `/auth/refresh` | 白名单；仅回显当前 token 与登录态 |
| GET | `/auth/info` | 当前管理员资料 + 角色 + 权限 |
| GET | `/auth/login-logs` | 分页登录日志（`@RequireAdmin`） |
| GET | `/auth/login-logs/{id}` | 登录日志详情 |
| DELETE | `/auth/login-logs` | 清空登录日志 |
| PUT | `/auth/profile` | 更新本人资料 |
| PUT | `/auth/password` | 修改本人密码 |
| POST | `/log/login`、`GET /log/login/{id}`、`DELETE /log/login` | 与 `/auth/login-logs` 同能力的第二套入口 |
| POST | `/log/operation`、`GET /log/operation/{id}` | 操作日志分页与详情 |
| DELETE | `/log/operation`、`/log/operation/all`、`/log/operation/clean/{days}` | 批量删除、清空、按天数清理 |
| GET | `/admin/dashboard/overview` | 仪表板总览 |
| POST | `/admin/agent/chat` | 管理员智能体统一入口 |
| GET | `/admin/ai/config/runtime`、`/schema-catalog` | AI 运行时配置摘要与调试清单 |
| GET | `/admin/ai/config/regression/cases`、`/latest`、`/history`、`/scenario-health` | AI 回归目录与历史 |
| POST | `/admin/ai/config/regression/run`、`/prompt-debug`、`/rag-debug`、`/test` | 执行回归、Prompt/RAG 在线调试、连通性测试 |
| GET/DELETE | `/admin/ai/config/metrics` | AI 运行观测读取/清空 |
| GET | `/admin/ai/config/rag-service/health`、`/documents`、`/documents/export` | RAG 服务健康、文档列表、导出 |
| POST/DELETE | `/admin/ai/config/rag-service/sample-import`、`/documents/import`、`/documents`、`/documents/batch-delete` | RAG 样例/自定义文档导入与删除 |

公共：`config/OpenApiConfig.java` 提供 OpenAPI 分组；`/v3/api-docs/**`、`/swagger-ui/**` 免登录（`SaTokenConfig` 第 59 行）。

**数据表**（依据 `src/main/resources/mapper/*.xml`）

| 表 | 归属 Mapper | 访问方式 |
| --- | --- | --- |
| `sys_admin` | `SysAdminMapper.xml` | 全量 CRUD；`deleteById` 为 `UPDATE` 逻辑删除 |
| `sys_role` | `SysRoleMapper.xml` | XML 含完整 CRUD，但仅 `selectRolesByAdminId` 被调用 |
| `sys_permission` | `SysPermissionMapper.xml` | XML 含完整 CRUD，但仅 `selectPermissionsByAdminId` 被调用 |
| `sys_admin_role` | 无独立 Mapper | 仅作为 JOIN 表被读（`SysRoleMapper.xml` 第 85 行、`SysPermissionMapper.xml` 第 100 行） |
| `sys_role_permission` | 无独立 Mapper | 仅作为 JOIN 表被读（`SysPermissionMapper.xml` 第 99 行） |
| `sys_login_log` | `SysLoginLogMapper.xml` | 插入、分页、详情、`TRUNCATE TABLE` |
| `sys_operation_log` | `SysOperationLogMapper.xml` | 插入、分页、详情、批量删除、清空、按时间删除 |
| `sys_agent_audit` | `SysAgentAuditMapper.xml` | 插入、按 auditId 乐观更新、分页 |
| `sys_agent_session_context` | `SysAgentSessionContextMapper.xml` | 按 sessionId 读写 turnsJson |

**关键流程**

- 管理员登录（`SysAdminServiceImpl.login`，第 50-158 行）：解析 UA → 先落一条失败态登录日志 → 查 `selectByUsername` → 校验 `status==1` 抛 `ACCOUNT_DISABLED`(704) → `PasswordUtil.matches` 校验密码，失败抛 `LOGIN_FAILED`(705) → `StpAdminUtil.login(id)` → 更新 lastLoginTime/IP/loginCount → `StpAdminUtil.set("userInfo"/"username")` → 写成功日志 → 返回 `LoginResponse`（`refreshToken` 与 `accessToken` 同值，`expiresIn` 硬编码 `604800L`）。
- 管理端鉴权（`AdminAuthAspect`）：`@RequireAdmin` 切点 → `StpAdminUtil.checkLogin()` + `StpAdminUtil.checkRole("admin")`。
- 权限装配：`getAdminRoles`/`getAdminPermissions` 走 `sys_admin_role`/`sys_role_permission` 关联查询，仅用于 `/auth/info` 响应与 `AgentOperator` 构造。
- 智能体（`AgentChatOrchestrator.chat`）：按 `auditId` 是否为空分流"继续已审计动作"或"发起新动作" → 记录会话上下文 → `AgentPolicyEngine.evaluate` 做输入 schema / 权限 / 角色 / 同租户校验，写类工具必须带 `confirmationText` → `SysAgentAuditServiceImpl` 以 `PREVIEW → CONFIRMED/CANCELLED → EXECUTED/FAILED` 状态机配合 `expectedStatus` 乐观锁（影响行数必须为 1，否则抛异常）。
- 审计落库：`SysAgentAuditServiceImpl` 是唯一写 `sys_agent_audit` 的生产代码路径。
- 审计（操作日志）：`@Log` 注解在全仓库大量使用，但**没有消费它的切面**（`xiaou-system/src/main/java/com/xiaou/system/service/SysOperationLogServiceImpl.java` 的 `saveOperationLog` 在生产代码中无调用点）。

**依赖关系**：`xiaou-common-core/web/security/persistence/cache`、`xiaou-resilience`、`micrometer-core`、`xiaou-ai`、`xiaou-sre`、`xiaou-user`、`xiaou-chat`、`xiaou-points`、jjwt（注释标注"已由 Sa-Token 替代"）、spring-boot-starter-aop、aspectjweaver/rt、langchain4j-open-ai。

**约束与注意事项**

- `service/impl/SysAdminServiceImpl.page()` 忽略 `pageNum/pageSize`，直接返回 `selectList` 全量（第 182-185 行，注释自述"暂时返回全部"）。
- `utils/JwtKeyGenerator.java`、`utils/PasswordGenerator.java` 在仓库内无调用点，属遗留死代码。
- `AgentChatController` 构造 `AgentOperator` 时 `tenantId` 传空串（第 51-57 行），而 `AgentPolicyEngine.validateAccess` 对 `SAME_TENANT` 工具要求 operator 与输入 `tenantId` 均非空且相等（第 106-112 行）——若存在 `tenantScope=SAME_TENANT` 的工具，将必然被拒。是否确有此类工具：**未确认**。
- 管理端登录接口**不校验验证码**；验证码仅用于 C 端（`UserInfoServiceImpl` 第 48、129、400 行）。

## xiaou-user（xiaou-user）

**职责与边界**

- C 端账号：注册、登录、登出、刷新、资料读写、改密、头像上传。
- 公共验证码（`/captcha/**`，免登录）。
- 管理端用户管理（`/admin/user/**`，`@RequireAdmin`）。
- 对外 SPI：实现 `com.xiaou.user.api.UserInfoApiService` 并把资料解析器注册给 `SaTokenUserUtil`，供社区、消息等场景复用，避免反向依赖。
- 边界：不感知角色与权限模型（`user_info` 无角色字段）；不含用户端权限拦截，仅做"本人操作"校验。

**核心领域对象**：`domain/UserInfo.java` —— id/username(`^[a-zA-Z0-9_]+$`)/password/nickname/realName/email/phone(`^1[3-9]\d{9}$`)/avatar/gender/birthday/status(0 正常 1 禁用 2 删除)/lastLoginTime/lastLoginIp/registerTime/remark/createTime/updateTime/createBy/updateBy。

**对外接口**

公共（免登录白名单）：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/captcha/generate` | Hutool `LineCaptcha` 4 位字符、5 条干扰线，Redis key 前缀 `user:captcha:`，TTL 5 分钟，返回 Base64 图片 |
| POST | `/captcha/verify` | 忽略大小写比对，成功后删除（一次性） |

用户端（`/user/**`，除显式 notMatch 外需 `StpUserUtil` 登录）：

| 方法 | 路径 | 免登录 |
| --- | --- | --- |
| POST | `/user/auth/register` | 是 |
| POST | `/user/auth/login` | 是 |
| POST | `/user/auth/refresh` | 是 |
| GET | `/user/auth/check-username`、`/check-email`、`/check-phone` | 是 |
| POST | `/user/auth/logout` | 否 |
| GET | `/user/auth/info` | 否 |
| GET/PUT | `/user/profile` | 否 |
| PUT | `/user/password` | 否 |
| GET/PUT | `/user/{userId}` | 否，`checkCurrentUser` 校验本人 |
| PUT | `/user/{userId}/password` | 否，改密后 `StpUserUtil.logout()` |
| POST | `/user/avatar/upload` | 否 |

管理端（`/admin/user/**`，全部 `@RequireAdmin`）：`GET /list`、`GET /all`、`GET /{userId}`、`POST /create`、`PUT /{userId}`、`DELETE /{userId}`、`DELETE /batch`、`PUT /{userId}/status`、`PUT /{userId}/reset-password`、`GET /statistics`。

**数据表**：`user_info`（`src/main/resources/mapper/UserInfoMapper.xml`，该目录下仅此一个 XML）。`deleteById`/`deleteByIds` 为 `UPDATE` 逻辑删除（将 status 置为删除态）。

**关键流程**

- 注册（`UserInfoServiceImpl.register`）：校验验证码 → 两次密码一致 → 用户名/邮箱/手机号查重 → `PasswordUtil.encode` → `insert` → `pointsService.createPointsAccountForNewUser`（失败仅告警）→ `notificationPublisher.publish` 欢迎消息（失败仅告警）→ 返回前清空 password。
- 登录（`login`）：校验验证码 → `selectByUsernameOrEmail` → 用户不存在/status==2 统一返回"用户名或密码错误"（防枚举）→ status==1 返回"账户已被禁用" → `PasswordUtil.matches` → 更新最后登录 → `StpUserUtil.login(id)` + `set("userInfo"/"username")` → `expiresIn=604800L`。
- 改密：必须通过验证码；校验原密码、新旧不同；成功后由 Controller 调 `StpUserUtil.logout()` 强制重登。
- 头像上传：扩展名白名单 `jpg|jpeg|png|gif` + ≤5MB → `fileStorageService.uploadSingle(file,"user","avatar")` → 回写 avatar 字段。
- 资料 SPI：`CommonUserProfileResolverConfig` 在 `@PostConstruct` 注册、`@PreDestroy` 注销 `SaTokenUserUtil.UserProfileResolver`，内部调 `UserInfoApiServiceImpl.getSimpleUserInfo`。

**依赖关系**：`xiaou-common-core/web/security/persistence/cache`、`xiaou-common`、`xiaou-notification`、`xiaou-user-api`、`xiaou-filestorage`、`xiaou-points`、hutool-captcha、spring-boot-starter-web/validation/data-redis、lombok。

**约束与注意事项**

- `AdminUserController.resetPassword` 的 `newPassword` 带硬编码 `@RequestParam(defaultValue = ...)` 弱口令，即管理端不传参即把目标用户密码重置为该固定值（值见源码，此处不复制）。
- `/admin/user/all` 以 `pageSize = Integer.MAX_VALUE` 走分页查询取全量，非独立查询路径。
- avatars 上传只校验扩展名与大小，未见 MIME/魔数校验。
- `UserInfoServiceImpl` 中积分开户与通知均为 try/catch 吞异常，注册整体 `@Transactional` 不会因它们回滚。

## 跨模块观察

**共性模式**

- 统一返回 `Result<T>`，Controller 层普遍 `try/catch` 包一层并降级为 `Result.error("...失败")`（`AuthController`、`LogController`、`UserAuthController`、`CaptchaController` 最明显），与 `@RestControllerAdvice` 全局异常语义重叠。
- 校验风格分两派：`xiaou-system` 用 `@Valid + jakarta.validation`（`SysAdmin` domain 内嵌约束），`xiaou-user` 的 Service 内大量手写 `if (...) throw new BusinessException("...")` 字符串码，业务码语义不统一。
- Mapper 全部为 XML 手写 SQL，分页统一走 `com.xiaou.common.utils.PageHelper.doPage`。
- 两个模块都用 `PasswordUtil.encode/matches` 处理口令，并都用 `StpXxx.set("userInfo"/"username")` 冗余写会话。

**重复代码**

- 登录日志能力存在两套入口：`AuthController` 的 `/auth/login-logs*` 与 `LogController` 的 `/log/login*`，复用同一 `SysLoginLogService`。
- `AdminUserController` 与 `UserController` 共用 `UserInfoService.updateUserInfo`，但前者无"本人"校验、后者有。
- 登录失败日志写入逻辑在 `SysAdminServiceImpl.login` 内以 5 处重复的 `loginLogMapper.insert` 展开，未抽方法。

**潜在风险（按严重度）**

1. **权限模型与鉴权实际脱钩**：`StpInterfaceImpl.getRoleList/getPermissionList` 对 `loginType=admin` 恒返回 `["admin"]`（第 56-58 行），对 `user` 恒返回 `["user"]`（第 60-62 行）。`@RequireAdmin` 经 `AdminAuthAspect` 只做 `checkRole("admin")`，因此**任何已登录管理员都通过全部管理端鉴权**；`sys_role`/`sys_permission`/`sys_admin_role`/`sys_role_permission` 不参与鉴权，仅用于 `/auth/info` 回显与智能体 `AgentOperator` 权限判断。两条权限判定路径（Sa-Token 与 DB 查询）结论可能不一致：智能体工具要求 `permissions` 含具体编码，而 `/auth/info` 返回的权限来自 DB，`StpInterfaceImpl` 却给不出这些编码。
2. **角色/权限管理无 REST 入口**：`SysRoleMapper`/`SysPermissionMapper` 含完整 CRUD 接口（insert/update/deleteById/deleteByIds/updateStatus/checkXxx），但全仓库检索只有 `xiaou-system/src/main/java/com/xiaou/system/service/impl/SysAdminServiceImpl.java` 第 316、327 行两处调用（均为 select）。`sys_role_permission`、`sys_admin_role` 连 Mapper 都没有，无法通过应用写授权关系。
3. **`sys_operation_log` 在生产路径无写入者**：全仓库唯一的 `@Aspect` 是 `AdminAuthAspect`（只管 `@RequireAdmin`），不存在消费 `@Log` 的切面；`SysOperationLogService.saveOperationLog` 除接口、实现与测试外无调用点。这使 `LogController` 的操作日志页面与 `SysDashboardServiceImpl.queryTodayFailedOperationCount`（status=1 计数）在生产上很可能长期为 0。
4. **管理端登录无验证码**，而 C 端登录/注册/改密强制验证码；管理端是暴力破解成本更低的一侧。
5. **业务码缺口**：项目规则只声明 200/701/702/703/704，但 `ResultCode` 实际含 `LOGIN_FAILED(705)`，管理端登录失败即返回 705（`AuthController` 的 `@ApiResponse(responseCode="705")` 亦如此标注）。前端拦截器若只处理 701-704，705 会落入默认分支。
6. **智能体租户校验空串**：`AgentChatController` 传入 `tenantId=""`，`SAME_TENANT` 工具将必然被拒（前提是存在此类工具，**未确认**）。
7. `SysAdminServiceImpl.page()` 无视分页参数返回全量管理员列表。

**与 `.agent/rules/always.md` 的一致性**

- 一致：端口 9999、context-path `/api`、`Result<T>` 与 701-704 语义、`StpAdminUtil`/`StpUserUtil`/`@RequireAdmin` 复用、`/auth/**`+`/admin/**`+`/user/**`+`/captcha/**` 路由分区、免登录路径、业务逻辑落在业务模块（`xiaou-system`、`xiaou-user` 未塞入 bootstrap/application）、Mapper XML 位于模块 `resources/mapper`。
- 需回修规则：`always.md` 第 43 行的业务码清单遗漏 `705 LOGIN_FAILED`（且 `ResultCode` 还有 600-604、801-805 两段）；第 44 行免登录清单未提 `/swagger-ui.html`，也未记录 `/user/**` 下学习小组一批匿名 `notMatch` 例外（`SaTokenConfig` 第 44-52 行）。
- 无冲突项。

## 铁律

1. 本文件所有结论均可指回路径与行号；未读到的文件一律标注"未确认"，不作推测性补全。
2. 单段代码引用不超过 5 行。
3. 除本文件外未创建/修改/删除任何文件；未执行 git 命令。
4. 未读写 `.agent/` 下除 `rules/always.md` 以外的文件。
5. 全文中文 UTF-8。
