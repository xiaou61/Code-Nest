# 鉴权与用户体系

Code Nest 使用 Sa-Token 做鉴权，但不是"一套登录态走天下"。项目里有两个独立登录域：管理端使用 `admin`，用户端使用 `user`。这样做的目的很简单：后台管理员 Token 不能拿去当普通用户 Token 用，普通用户 Token 也不能误打误撞访问后台接口。

## 推荐学习顺序

鉴权模块要先学"边界"，再学"流程"：

1. 先看 `StpAdminUtil` 和 `StpUserUtil`，确认项目里确实有两套 Sa-Token 登录域。
2. 再看 `SaTokenConfig`，理解哪些路径被拦截、哪些路径被放行。
3. 然后看 `AuthController.login` 和 `UserAuthController.login`，比较管理员登录和普通用户登录的差异。
4. 接着看两个前端的 `utils/request.js` 和 store，理解 Token 保存在哪里、请求头如何拼。
5. 最后看 `@RequireAdmin` 和 `AdminAuthAspect`，明确后台接口为什么不能只依赖前端菜单隐藏。

## 源码地图

| 位置 | 作用 | 行数 |
| --- | --- | --- |
| `xiaou-common/src/main/java/com/xiaou/common/config/SaTokenConfig.java` | 注册 Sa-Token 拦截器，定义放行/拦截路径 | ~50 |
| `xiaou-common/src/main/java/com/xiaou/common/satoken/StpAdminUtil.java` | 管理端登录工具，`loginType = "admin"` | ~30 |
| `xiaou-common/src/main/java/com/xiaou/common/satoken/StpUserUtil.java` | 用户端登录工具，`loginType = "user"` | ~30 |
| `xiaou-common/src/main/java/com/xiaou/common/aspect/AdminAuthAspect.java` | `@RequireAdmin` 管理员权限切面 | ~40 |
| `xiaou-common/src/main/java/com/xiaou/common/annotation/RequireAdmin.java` | 管理员权限注解定义 | ~15 |
| `xiaou-common/src/main/java/com/xiaou/common/annotation/Log.java` | 操作日志注解定义 | ~20 |
| `xiaou-system/src/main/java/com/xiaou/system/controller/AuthController.java` | 管理端登录、退出、刷新、个人资料 | ~80 |
| `xiaou-user/src/main/java/com/xiaou/user/controller/UserAuthController.java` | 用户注册、登录、退出、唯一性校验 | ~60 |
| `vue3-admin-front/src/utils/request.js` | 管理端 Axios 请求/响应拦截器 | ~50 |
| `vue3-user-front/src/utils/request.js` | 用户端 Axios 请求/响应拦截器 | ~50 |
| `vue3-admin-front/src/stores/user.js` | 管理端用户状态（Token 刷新 + 跨 Tab 广播） | ~80 |
| `vue3-user-front/src/stores/user.js` | 用户端用户状态 | ~60 |

## StpLogic 实现细节

`StpAdminUtil` 和 `StpUserUtil` 都封装了 Sa-Token 的 `StpLogic`，核心差异只有一个：`loginType`。

| 工具类 | `loginType` | StpLogic 实例 | 源码位置 |
|--------|------------|-------------|---------|
| `StpAdminUtil` | `"admin"` | `new StpLogic("admin")` | `xiaou-common/satoken/StpAdminUtil.java` |
| `StpUserUtil` | `"user"` | `new StpLogic("user")` | `xiaou-common/satoken/StpUserUtil.java` |

两个工具类提供完全相同的方法签名，但操作的 Redis 命名空间不同：

| 方法 | StpAdminUtil 行为 | StpUserUtil 行为 |
|------|-----------------|----------------|
| `login(id)` | 在 admin 命名空间创建会话 | 在 user 命名空间创建会话 |
| `checkLogin()` | 检查 admin 命名空间 | 检查 user 命名空间 |
| `getLoginIdAsLong()` | 从 admin 命名空间取 ID | 从 user 命名空间取 ID |
| `kickout(loginId)` | 踢 admin 命名空间会话 | 踢 user 命名空间会话 |
| `disable(id, seconds)` | 在 admin 命名空间封禁 | 在 user 命名空间封禁 |
| `set(key, value)` | 写 admin Session | 写 user Session |

**额外方法**：`StpUserUtil` 多了 `getTokenTimeout()` 方法（获取 Token 剩余有效期），`StpAdminUtil` 没有此方法。

## Sa-Token 配置项

Sa-Token 使用独立 Redis 连接池（`sa-token-alone-redis` + Jedis），不影响业务 Redisson 配置。

关键配置在 `application.yml` 的 `sa-token` 节点：

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `sa-token.token-name` | Token 名称（读取请求头的 key） | `satoken` |
| `sa-token.timeout` | Token 有效期（秒），-1 表示永不过期 | 默认配置 |
| `sa-token.active-timeout` | Token 活跃有效期（秒），-1 表示不限 | -1 |
| `sa-token.is-concurrent` | 是否允许同一账号多端登录 | 默认 |
| `sa-token.is-share` | 是否共享 Token | 默认 |
| `sa-token.token-style` | Token 风格（uuid/simple-uuid/random-32/random-64/random-128/tik） | uuid |
| `sa-token.alone-redis.host` | Sa-Token 独立 Redis 地址 | `127.0.0.1` |
| `sa-token.alone-redis.port` | Sa-Token 独立 Redis 端口 | `6379` |
| `sa-token.alone-redis.database` | Sa-Token 独立 Redis 数据库编号 | 默认（db4） |
| `sa-token.alone-redis.password` | Sa-Token 独立 Redis 密码 | 无 |

**Redis 数据库隔离**：Sa-Token 使用 `alone-redis`（独立 Redis 连接池），配置中的 `database` 决定 Token 数据存储的 Redis DB。两套登录域共享同一个 Redis 连接池，但通过 `loginType` 区分命名空间（`satoken:admin:*` vs `satoken:user:*`），数据互不干扰。

## SaTokenConfig 放行路径清单

`SaTokenConfig` 中的拦截规则：

```text
拦截规则:
  /auth/**         → 需管理员登录（排除 login/register/refresh）
  /user/**         → 需用户登录（排除 auth/login/register/refresh/check-username/check-email）

放行规则:
  /captcha/**      → 验证码接口，无需认证
  /v3/api-docs/**  → Swagger API 文档
  /swagger-ui/**   → Swagger UI
  /swagger-ui.html → Swagger HTML
  /error           → Spring Boot 错误页
  /favicon.ico     → 网站图标
```

**关键放行接口**：

| 放行路径 | 原因 | 模块 |
|---------|------|------|
| `/auth/login` | 管理端登录本身不能要求已登录 | AuthController |
| `/auth/register` | 管理端注册 | AuthController |
| `/auth/refresh` | Token 刷新 | AuthController |
| `/user/auth/login` | 用户端登录 | UserAuthController |
| `/user/auth/register` | 用户端注册 | UserAuthController |
| `/user/auth/refresh` | 用户端 Token 刷新 | UserAuthController |
| `/user/auth/check-username` | 注册页实时校验，用户未登录 | UserAuthController |
| `/user/auth/check-email` | 注册页实时校验，用户未登录 | UserAuthController |
| `/captcha/**` | 验证码获取，用户未登录 | CaptchaController |
| `/v3/api-docs/**` | Swagger 文档 | SpringDoc |
| `/community/**` | 社区浏览接口（部分需要登录） | CommunityController |
| `/blog/**` | 博客浏览接口（部分需要登录） | BlogController |

**注意**：项目不使用 `@SaIgnore` 注解，所有放行通过 `SaTokenConfig` 的 `SaRouter.notMatch()` 和 `excludePathPatterns` 配置实现。新增放行路径时必须修改 `SaTokenConfig`。

## 两套登录域

| 登录域 | 后端工具 | 前端 Token key | Cookie key | Redis DB | 常见接口 |
| --- | --- | --- | --- | --- | --- |
| 管理端 | `StpAdminUtil` | `token` | `token` | db4 (Sa-Token) | `/auth/**`、`/admin/**`、`/log/**` |
| 用户端 | `StpUserUtil` | `user_token` | `user_token` | db4 (Sa-Token) | `/user/**`、业务接口 |

`StpAdminUtil` 和 `StpUserUtil` 都封装了 Sa-Token 的 `StpLogic`，但登录类型不同。登录类型是区分会话空间的关键。可以把它理解成两张门禁卡：卡面看起来都叫 Token，但后台门禁和用户门禁查的是不同名单。

## 一次请求如何通过鉴权

| 步骤 | 管理端 | 用户端 |
| --- | --- | --- |
| 1. 前端取 Token | `stores/user.js` 读取 localStorage `token` | `stores/user.js` 读取 localStorage `user_token` |
| 2. 请求头 | `Authorization: Bearer &lt;token&gt;` | `Authorization: Bearer &lt;token&gt;` |
| 3. 后端拦截 | `/auth/**` 路径拦截 + `@RequireAdmin` 方法级校验 | `/user/**` 路径拦截 + 业务接口内 `StpUserUtil` |
| 4. 会话校验 | `StpAdminUtil.checkLogin()` | `StpUserUtil.checkLogin()` |
| 5. 角色校验 | `StpAdminUtil.checkRole("admin")` | 普通用户通常只校验登录态 |
| 6. 业务取人 | `StpAdminUtil.getLoginIdAsLong()` | `StpUserUtil.getLoginIdAsLong()` |

排查鉴权问题时先确认"用了哪张卡"。后台接口拿用户端 `user_token` 调用，表面上也是 Bearer Token，但登录域不同，后端会认为未登录。

## 后端拦截规则

`SaTokenConfig` 注册了全局拦截器，主要规则如下：

| 路径 | 规则 | 说明 |
| --- | --- | --- |
| `/auth/**` | 除 login/register/refresh 外需管理员登录 | 管理端基础接口 |
| `/admin/**` | 依赖 `@RequireAdmin` | 业务后台接口 |
| `/user/**` | 除 auth 子路径外需用户登录 | 用户端接口 |
| `/captcha/**` | 放行 | 验证码 |
| `/v3/api-docs/**`、`/swagger-ui/**` | 放行 | API 文档 |

需要特别注意：很多后台业务接口是 `/admin/**`，它们主要依赖方法上的 `@RequireAdmin`。这个注解会进入 `AdminAuthAspect`，切面里先执行 `StpAdminUtil.checkLogin()`，再执行 `StpAdminUtil.checkRole("admin")`。所以新写后台接口时不要只改前端菜单，后端方法也要补 `@RequireAdmin`。

## 管理端登录流程

| 步骤 | 代码位置 | 说明 |
| --- | --- | --- |
| 1 | `AuthController.login` | 接收用户名、密码 |
| 2 | `SysAdminServiceImpl.login` | 查询 `sys_admin` |
| 3 | `SysAdminServiceImpl.login` | 检查状态，`1` 表示禁用 |
| 4 | `PasswordUtil.matches` | 校验密码 |
| 5 | `StpAdminUtil.login(admin.getId())` | 创建管理员会话 |
| 6 | `StpAdminUtil.set("userInfo", admin)` | 写入 Sa-Token Session |
| 7 | `SysLoginLogMapper.insert` | 写登录成功/失败日志 |
| 8 | `LoginResponse` | 返回 accessToken、refreshToken、expiresIn、角色和权限 |

管理端登录失败会记录 `sys_login_log`，包括用户名、IP、浏览器、操作系统、状态和失败原因。这是排查后台账号问题的第一现场。

## 用户端登录流程

| 步骤 | 代码位置 | 说明 |
| --- | --- | --- |
| 1 | `CaptchaServiceImpl.verifyCaptcha` | 先校验 Redis 里的验证码，key 前缀 `user:captcha:`，5 分钟有效 |
| 2 | `UserInfoMapper.selectByUsernameOrEmail` | 支持用户名或邮箱登录 |
| 3 | `UserInfoServiceImpl.login` | 账号不存在或密码错误都返回统一提示，避免用户枚举 |
| 4 | `UserInfoServiceImpl.login` | `status = 1` 禁用，`status = 2` 删除 |
| 5 | `StpUserUtil.login(user.getId())` | 创建用户会话 |
| 6 | `StpUserUtil.set("userInfo", user)` | 写入用户 Session |
| 7 | `updateLastLoginInfo` | 更新最后登录时间和 IP |
| 8 | `UserLoginResponse` | 返回 accessToken、tokenType、expiresIn、用户资料 |

## 前端 Token 存储

| 端 | localStorage | Cookie | 自动刷新 | 跨 Tab 同步 |
| --- | --- | --- | --- | --- |
| 管理端 | `token`、`userInfo`、`tokenExpireTime` | `token` (Sa-Token 写入) | 30 分钟自动刷新 | storage 事件广播登出 |
| 用户端 | `user_token`、`user_info` | `user_token` (Sa-Token 写入) | 无自动刷新 | 无 |

两个前端的请求拦截器都把 Token 放到请求头：

```text
Authorization: Bearer &lt;token&gt;
```

响应拦截器会处理统一业务码：

| 码值 | 含义 | 前端行为 |
| --- | --- | --- |
| `200` | 成功 | 返回 `data` |
| `701` | Token 无效 | 弹窗并跳登录 |
| `702` | Token 过期 | 弹窗并跳登录 |
| `703` | 权限不足 | 提示错误 |
| `704` | 账号禁用 | 清理登录态 |

因此后端新增鉴权失败时，应优先使用现有 `ResultCode` 和统一异常处理，不要随意返回新的业务码，否则前端拦截器不会自动处理。

## 权限和角色

管理端登录响应会返回：

| 字段 | 来源 | 用途 |
| --- | --- | --- |
| `roles` | `SysRoleMapper.selectRolesByAdminId` | 判断角色，如 `admin` |
| `permissions` | `SysPermissionMapper.selectPermissionsByAdminId` | 菜单、按钮、接口权限 |

当前 `@RequireAdmin` 切面检查的是管理员登录和 `admin` 角色。更细的按钮级权限主要体现在前端菜单和返回的 permission 列表里。如果要新增强权限接口，建议按下面顺序处理：

1. 后端方法加 `@RequireAdmin`。
2. 如果需要更细粒度权限，在 Service 里显式判断权限码。
3. 管理端菜单或按钮和 `sys_permission.permission_code` 保持一致。
4. 操作类接口加 `@Log`，方便后台审计。

这里还要补一条很关键的当前实现细节：Sa-Token 运行时的 `StpInterfaceImpl` 目前给 `admin` 登录域返回固定 `admin` 角色/权限，给 `user` 登录域返回固定 `user` 角色/权限。也就是说，后端当前最稳定的强校验主要还是"是不是管理员登录域"和"方法上有没有 `@RequireAdmin`"，并不是已经普遍按数据库里的 `permission_code` 做后端按钮级强鉴权。想系统看清这层差别，可以继续读 [权限注解与角色边界索引](/reference/permission-boundaries)。

## 核心数据表

| 表 | 说明 | 关键字段 |
| --- | --- | --- |
| `sys_admin` | 管理员账号 | id、username、password、status (0正常/1禁用/2删除) |
| `sys_role` | 角色定义 | id、role_name、role_key |
| `sys_permission` | 菜单、按钮、接口权限 | id、permission_name、permission_code、type |
| `sys_admin_role` | 管理员和角色关系 | admin_id、role_id |
| `sys_role_permission` | 角色和权限关系 | role_id、permission_id |
| `sys_login_log` | 管理端登录日志 | adminId、loginStatus、loginMessage |
| `user_info` | 普通用户账号 | id、username、email、status (0正常/1禁用/2删除) |

## 排查路径

| 现象 | 优先检查 | 排查方法 |
| --- | --- | --- |
| 前端跳登录页 | 响应码 `701`/`702`，Token 是否存在 | 看浏览器 Network 和 localStorage |
| 后台提示权限不足 | 当前管理员是否有 `admin` 角色，接口是否命中 `@RequireAdmin` | 看 `sys_admin_role` 表和后端日志 |
| 用户端登录成功但刷新后丢失 | `user_token` Cookie/localStorage 是否写入 | 看 `stores/user.js` 和浏览器 Application |
| 管理端刷新 Token 失败 | `StpAdminUtil.isLogin()` 是否为 false，Token 是否已过期 | 检查 Redis 中 Sa-Token 会话 |
| 注册页唯一性校验异常 | 目标接口是否被 `SaTokenConfig` 放行 | 看 `SaTokenConfig` 的 excludePathPatterns |
| 新后台接口裸奔 | Controller 方法是否漏了 `@RequireAdmin` | 搜索新增 Controller 方法 |

## 常见坑

| 问题 | 原因 | 排查方式 |
| --- | --- | --- |
| 管理端登录后访问后台接口仍提示未登录 | 请求头没有带 `Authorization`，或用了用户端 `user_token` | 看浏览器 Network 请求头 |
| 用户端接口提示 Token 无效 | 前端没有从 `user_token` 读取，或 Cookie/localStorage 被清空 | 看 `stores/user.js` |
| `/admin/**` 新接口被普通用户打到 | 后端方法缺少 `@RequireAdmin` | 搜索新增 Controller 方法 |
| 改了密码但旧 Token 还能用 | 当前代码修改密码后会 logout 当前会话；如需踢掉全部设备，显式调用 Sa-Token kickout | 检查 `changePassword` 实现 |
| 登录失败排查无头绪 | 管理端有 `sys_login_log`，用户端目前主要看业务日志 | 查日志表和应用日志 |

## 新增接口检查清单

1. 先判断接口属于管理端、用户端还是公共端。
2. 管理端接口加 `@RequireAdmin`，操作类接口加 `@Log`。
3. 用户端接口从 `StpUserUtil.getLoginIdAsLong()` 获取当前用户，不信任前端传入的用户 ID。
4. 前端调用统一走 `utils/request.js`，不要手写裸 `fetch` 绕过拦截器。
5. 文档同步更新 [API 索引](/reference/api-routes)、模块页和操作手册。

## 验证清单

| 场景 | 预期 |
| --- | --- |
| 管理员登录后台 | 返回管理端 Token、角色和权限 |
| 普通用户登录用户端 | 返回用户端 Token 和用户资料 |
| 用用户 Token 调后台接口 | 返回未登录或权限不足 |
| 用管理员 Token 调用户接口 | 不能当作普通用户会话使用 |
| 后台接口缺少 Token | 前端收到 `701/702` 后清理登录态并跳登录 |
| 普通管理员缺少 `admin` 角色 | `@RequireAdmin` 拦截 |
| 登录失败 | 管理端写入 `sys_login_log` |
| Token 自动刷新（管理端） | 30 分钟内请求自动续期 |
| 跨 Tab 登出广播（管理端） | 一个 Tab 登出，其他 Tab 同步清理 |
