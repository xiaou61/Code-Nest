# 权限与安全边界

这页回答两个核心问题：

1. **谁能做什么？** ——管理端和用户端的权限校验机制、粒度、差距。
2. **改权限相关代码后最低该回归什么？**

如果你只是想查"某个接口到底有没有鉴权"，可以跳到 [Controller 鉴权分布](#controller-鉴权分布)。

## 双端鉴权架构

项目使用 Sa-Token 实现用户端和管理端分仓鉴权：

| 端 | 工具类 | Token 名 | 登录方式 | Session 隔离 |
| --- | --- | --- | --- | --- |
| 用户端 | `StpUserUtil` | `userToken` | 手机号/邮箱/用户名 + 密码 | 独立 Session |
| 管理端 | `StpAdminUtil` | `adminToken` | 用户名 + 密码 | 独立 Session |

两端 Token 互相隔离——用户端 Token 不能访问管理端接口，反之亦然。同一个人可以同时持有两端的登录态。

## 管理端权限机制

### @RequireAdmin 注解

所有管理端权限校验统一通过 `@RequireAdmin` 注解实现。切面 `AdminAuthAspect` 的拦截逻辑：

1. `StpAdminUtil.checkLogin()` —— 检查管理端登录态
2. `StpAdminUtil.checkRole("admin")` —— 检查 admin 角色
3. 未通过 → 返回 `Result.error(ResultCode.UNAUTHORIZED)`

**注意**：这个切面只检查"管理端是否登录且拥有 admin 角色"，不检查细粒度的权限码。

### AdminAuthAspect 源码级拆解

`AdminAuthAspect` 是 `@RequireAdmin` 的实际执行切面，基于 `@Around` 环绕通知：

```text
拦截流程：
  @Pointcut("@annotation(com.xiaou.common.annotation.RequireAdmin)")
  → around(ProceedingJoinPoint)
  → 获取方法上的 RequireAdmin 注解（读取自定义 message）
  → try:
      StpAdminUtil.checkLogin()       → 管理端登录态校验
      StpAdminUtil.checkRole("admin") → admin 角色校验
      StpAdminUtil.getLoginIdAsLong() → 获取管理员 ID（用于日志）
      → 验证通过 → joinPoint.proceed() → 继续执行方法
  → catch:
      NotLoginException → throw BusinessException("请先登录")
      NotRoleException  → throw BusinessException(requireAdmin.message())
      BusinessException  → 直接抛出（不二次包装）
      Exception          → throw BusinessException("权限验证失败")
```

关键实现细节：

| 行为 | 实现 | 说明 |
| --- | --- | --- |
| 切点定义 | `@annotation(RequireAdmin.class)` | 只拦截标注了 `@RequireAdmin` 的方法 |
| 登录态校验 | `StpAdminUtil.checkLogin()` | 管理端独立 StpLogic，不检查用户端 |
| 角色校验 | `StpAdminUtil.checkRole("admin")` | 硬编码角色名 `"admin"` |
| 自定义 message | `requireAdmin.message()` | 注解支持 `message` 属性，用于权限不足时的提示文案 |
| 异常转换 | Sa-Token 异常 → `BusinessException` | 原始 Sa-Token 异常被包装为业务异常，最终被 `GlobalExceptionHandler` 以 HTTP 200 + 业务码 600 返回前端 |
| 日志 | `log.debug` 记录管理员 ID、类名、方法名 | 通过后记录验证通过信息 |

### 权限注解使用现状

| 注解 | 使用情况 | 说明 |
| --- | --- | --- |
| `@RequireAdmin` | 约 270 个方法 | 管理端接口统一入口守卫 |
| `@SaCheckRole` | **未使用** | Sa-Token 角色注解在源码中不存在 |
| `@SaCheckPermission` | **未使用** | Sa-Token 权限注解在源码中不存在 |

**关键结论**：当前项目不存在细粒度的后端 RBAC 校验。所有管理端接口只要持有管理员登录态即可访问，没有"运营只能看不能改"或"内容管理员只能管帖子不能管用户"这类角色区分。

### @RequireAdmin 方法分布

| 模块 | Controller 类 | 方法数 | 主要职责 |
| --- | --- | --- | --- |
| xiaou-user | AdminUserController | 10 | 用户 CRUD、禁用、密码重置、统计 |
| xiaou-blog | BlogAdminController | 13 | 文章、分类、标签管理 |
| xiaou-codepen | CodePenAdminController | 17 | 作品、模板、文件夹、标签管理 |
| xiaou-community | CommunityPostAdminController | 6 | 帖子管理 |
| xiaou-community | CommunityCommentAdminController | 3 | 评论管理 |
| xiaou-community | CommunityUserAdminController | 6 | 用户状态管理 |
| xiaou-community | AdminCommunityTagController | 5 | 标签管理 |
| xiaou-community | AdminCommunityCategoryController | 6 | 分类管理 |
| xiaou-interview | InterviewQuestionSetController | 8 | 题单管理 |
| xiaou-interview | InterviewQuestionController | 11 | 题目管理 |
| xiaou-interview | InterviewCategoryController | 5 | 分类管理 |
| xiaou-filestorage | AdminSystemController | 5 | 系统配置管理 |
| xiaou-filestorage | AdminStorageController | 10 | 存储配置管理 |
| xiaou-filestorage | AdminMigrationController | 7 | 迁移任务管理 |
| xiaou-filestorage | AdminFileController | 1 | 文件管理 |
| xiaou-moment | AdminMomentController | 5 | 动态管理 |
| xiaou-knowledge | AdminKnowledgeMapController | 7 | 图谱管理 |
| xiaou-knowledge | AdminKnowledgeNodeController | 7 | 节点管理 |
| xiaou-notification | AdminNotificationController | 9 | 通知管理 |
| xiaou-moyu | AdminDailyContentController | 11 | 每日内容管理 |
| xiaou-moyu | AdminDeveloperCalendarController | 9 | 开发者日历管理 |
| xiaou-moyu | AdminBugStoreController | 6 | Bug 库管理 |
| xiaou-chat | ChatAdminController | 8 | 聊天管理 |
| xiaou-points | AdminPointsController | 6 | 积分管理 |
| xiaou-points | AdminLotteryController | 22 | 抽奖配置、监控、风控、应急 |
| xiaou-sensitive | SensitiveWordAdminController | 2 | 敏感词管理 |
| xiaou-sensitive | SensitiveWhitelistController | 8 | 白名单管理 |
| xiaou-sensitive | SensitiveVersionController | 3 | 版本管理 |
| xiaou-sensitive | SensitiveStrategyController | 4 | 策略管理 |
| xiaou-version | VersionHistoryAdminController | 12 | 版本发布管理 |
| xiaou-system | DashboardController | 1 | 仪表盘总览 |
| xiaou-system | LogController | 8 | 日志查询、清理 |
| xiaou-system | AiConfigController | 20 | AI 配置、调试、回归、RAG 管理 |
| xiaou-ai | AiGovernanceController | 1 | AI 治理总览 |
| xiaou-oj | OjProblemController | 6 | 题目管理 |
| xiaou-oj | OjContestController | 6 | 赛事管理 |
| xiaou-oj | OjSolutionController | 3 | 题解管理 |
| xiaou-oj | OjTestCaseController | 4 | 测试用例管理 |
| xiaou-mock-interview | AdminMockInterviewController | 2 | 模拟面试管理 |
| xiaou-learning-asset | AdminLearningAssetController | 5 | 学习资产审核 |
| xiaou-resume | ResumeTemplateAdminController | 5 | 简历模板管理 |
| xiaou-resume | ResumeAnalyticsAdminController | 2 | 简历分析 |

## 用户端权限机制

### StpInterfaceImpl 实现

`StpInterfaceImpl` 实现 Sa-Token 的 `StpInterface` 接口：

| 端 | `getPermissionList()` | `getRoleList()` |
| --- | --- | --- |
| 管理端 (`loginType = "admin"`) | 返回硬编码 `["admin"]` | 返回硬编码 `["admin"]` |
| 用户端 (`loginType = "user"`) | 返回硬编码 `["user"]` | 返回硬编码 `["user"]` |

**关键发现**：两端都返回硬编码字符串列表，**没有数据库查询**。源码注释中标注了"可根据实际业务从数据库查询"的位置，但目前未实现。这意味着 `@SaCheckRole` 和 `@SaCheckPermission` 对任何端都只能做固定字符串比对，无法支持动态权限或角色分级。

### 用户端资源归属校验

用户端接口的"只能操作自己的数据"不是通过注解实现的，而是在 Service 层手动校验：

```java
// 典型模式
Long userId = StpUserUtil.getLoginIdAsLong();
UserPlan plan = planMapper.selectById(planId);
if (!plan.getUserId().equals(userId)) {
    throw new BusinessException(ResultCode.FORBIDDEN);
}
```

不同模块的归属校验严格程度不同：

| 模块 | 归属校验方式 | 严格程度 |
| --- | --- | --- |
| 计划 | `userId` 比对 | 严格 |
| 小组 | 成员表关联校验 | 严格 |
| 博客 | `userId` 比对 | 严格 |
| 社区帖子 | `userId` 比对 | 严格 |
| 代码工坊 | `userId` 比对 | 严格 |
| 闪卡 | `deckId` 归属 `userId` | 严格 |
| 聊天 | ws-ticket 签发绑定 `userId` | 严格 |
| 文件 | `is_public` 字段 + 归属判断 | 需逐接口审查 |

## Controller 鉴权分布

### 无需鉴权的公开接口

| 路径模式 | 说明 |
| --- | --- |
| `/captcha/**` | 验证码 |
| `/user/auth/login`、`/user/auth/register` | 用户登录注册 |
| `/auth/login` | 管理端登录 |
| `/pub/**` | 公开资源（博客列表、闪卡公开卡组、知识图谱公开图谱） |
| `/version`（GET） | 版本历史公开查看 |
| `/community/posts`（GET） | 社区帖子公开列表 |
| `/oj/problems`（GET） | OJ 题目公开列表 |

### 用户端鉴权接口

用户端接口通过 `StpUserUtil.checkLogin()` 或 `StpUserUtil.getLoginIdAsLong()` 校验。Sa-Token 配置了拦截器白名单，未登录访问受保护接口会触发 `NotLoginException`。

### SaTokenConfig 白名单配置

项目**不使用** `@SaIgnore` 注解，所有路径放行统一在 `SaTokenConfig.java` 中配置。白名单路径通过 `SaRouter.notMatch()` 和 `excludePathPatterns` 两处生效：

| 白名单路径 | 放行原因 | 涉及模块 |
| --- | --- | --- |
| `/captcha/**` | 验证码图片接口 | xiaou-user |
| `/user/auth/login`、`/user/auth/register` | 用户登录注册 | xiaou-user |
| `/auth/login` | 管理端登录 | xiaou-system |
| `/pub/**` | 公开资源（博客、闪卡公开卡组、知识图谱公开图谱） | 多模块 |
| `/version` | 版本历史公开查看 | xiaou-version |
| `/community/posts` | 社区帖子公开列表 | xiaou-community |
| `/oj/problems` | OJ 题目公开列表 | xiaou-oj |
| `/error` | Spring Boot 默认错误页 | 框架 |
| `/actuator/**` | 监控指标端点 | 运维 |
| `/swagger-ui/**`、`/v3/api-docs/**` | OpenAPI 文档 | 开发 |
| `/ws/chat` | WebSocket 端点（走 Interceptor 鉴权） | xiaou-chat |

> **新增加路径放行时**：只改 `SaTokenConfig.java`，不要在 Controller 上加 `@SaIgnore`。两端点 `/actuator` 和 `/swagger-ui` 在生产环境建议通过 profile 关闭。

### 管理端鉴权接口

所有 `/admin/**` 路径的写操作都标注了 `@RequireAdmin`。部分查询接口（如仪表盘统计）也标注了 `@RequireAdmin`，确保非管理员无法看到管理端数据。

## 操作日志

`@Log` 注解与 `@RequireAdmin` 经常成对出现，对管理端增删改操作做审计：

| 属性 | 说明 | 默认值 |
| --- | --- | --- |
| `module` | 操作模块 | `""` |
| `type` | SELECT / INSERT / UPDATE / DELETE / EXPORT / IMPORT / CLEAN / OTHER | OTHER |
| `description` | 操作描述 | `""` |
| `saveRequestData` | 是否保存请求参数 | true |
| `saveResponseData` | 是否保存响应数据 | true |

**敏感字段过滤**：`password`、`oldPassword`、`newPassword`、`confirmPassword`、`token`、`accessToken`、`secret`、`apiKey` 会被替换为 `******`。当前过滤字段列表硬编码在 `LogAspect` 中，不可配置。

## 安全风险与改进建议

### 当前已知风险

| 编号 | 风险 | 严重程度 | 说明 |
| --- | --- | --- | --- |
| RISK-1 | 管理端无角色分级 | 中 | 所有管理员等同，无运营/超管区分 |
| RISK-2 | 用户端无注解级权限 | 中 | `@SaCheckRole` / `@SaCheckPermission` 完全未使用 |
| RISK-3 | X-Forwarded-For 可伪造 | 中 | 攻击者可绕过基于 IP 的限流和审计 |
| RISK-4 | DEFAULT_PASSWORD = "123456" | 中 | 批量创建账号时弱密码风险 |
| RISK-5 | 敏感字段过滤硬编码 | 低 | 新增敏感参数需改切面代码 |
| RISK-6 | 日志异步写入无兜底 | 低 | DB 异常时操作日志丢失 |
| RISK-7 | StpInterfaceImpl 用户端空实现 | 低 | 未来补用户端权限时需改造 |
| RISK-8 | SaTokenUserUtil 跨端查询是 TODO 桩 | 低 | getUsernameById 等返回硬编码值 |

### 改进路径

| 改进 | 优先级 | 工作量 | 前置条件 |
| --- | --- | --- | --- |
| 管理端角色分级 | P1 | 大 | 需设计角色体系 + 菜单权限映射 |
| 补 `@SaCheckPermission` | P2 | 大 | 需定义权限码字典 + 改 StpInterfaceImpl |
| Nginx IP 信任配置 | P1 | 小 | 部署配置变更 |
| 敏感字段可配置化 | P3 | 小 | 改 LogAspect 读取配置 |
| 日志写入兜底 | P3 | 中 | 加本地文件或 MQ 备份 |
| SaTokenUserUtil 跨端查询实现 | P2 | 中 | 需跨模块调用用户服务 |

## 验证清单

| 验证点 | 怎么验证 | 预期结果 |
| --- | --- | --- |
| 双端鉴权隔离 | 用户端 Token 访问管理端接口 | 返回 401 |
| 管理端权限拦截 | 未登录访问 `@RequireAdmin` 接口 | 返回 401 |
| 非管理员角色 | 普通 admin 角色之外的账号访问管理端接口 | 取决于是否有 admin 角色 |
| 资源归属校验 | A 用户的 Token 操作 B 用户的数据 | 返回 403 或 404 |
| 操作日志记录 | 调用 `@Log` 标注的接口 | `sys_operation_log` 有对应记录 |
| 密码参数过滤 | 修改密码接口的日志记录 | 密码字段显示为 `******` |
| 公开接口 | 未登录访问 `/pub/**` | 正常返回数据 |

## 源码导航

| 想了解 | 读什么 |
| --- | --- |
| @RequireAdmin 注解 | `xiaou-common/src/main/java/com/xiaou/common/annotation/RequireAdmin.java` |
| 管理端权限切面 | `xiaou-common/src/main/java/com/xiaou/common/aspect/AdminAuthAspect.java` |
| @Log 注解 | `xiaou-common/src/main/java/com/xiaou/common/annotation/Log.java` |
| 操作日志切面 | `xiaou-common/src/main/java/com/xiaou/common/aspect/LogAspect.java` |
| Sa-Token 用户端 | `xiaou-common/src/main/java/com/xiaou/common/satoken/StpUserUtil.java` |
| Sa-Token 管理端 | `xiaou-common/src/main/java/com/xiaou/common/satoken/StpAdminUtil.java` |
| 用户上下文 | `xiaou-common/src/main/java/com/xiaou/common/satoken/SaTokenUserUtil.java` |
| 权限接口实现 | `xiaou-common/src/main/java/com/xiaou/common/satoken/StpInterfaceImpl.java` |
| Sa-Token 配置 | `xiaou-common/src/main/java/com/xiaou/common/config/SaTokenConfig.java` |
