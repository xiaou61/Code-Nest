# 鉴权与用户体系

Code Nest 使用 Sa-Token 做鉴权，但不是“一套登录态走天下”。项目里有两个独立登录域：管理端使用 `admin`，用户端使用 `user`。这样做的目的很简单：后台管理员 Token 不能拿去当普通用户 Token 用，普通用户 Token 也不能误打误撞访问后台接口。

## 推荐学习顺序

鉴权模块要先学“边界”，再学“流程”：

1. 先看 `StpAdminUtil` 和 `StpUserUtil`，确认项目里确实有两套 Sa-Token 登录域。
2. 再看 `SaTokenConfig`，理解哪些路径被拦截、哪些路径被放行。
3. 然后看 `AuthController.login` 和 `UserAuthController.login`，比较管理员登录和普通用户登录的差异。
4. 接着看两个前端的 `utils/request.js` 和 store，理解 Token 保存在哪里、请求头如何拼。
5. 最后看 `@RequireAdmin` 和 `AdminAuthAspect`，明确后台接口为什么不能只依赖前端菜单隐藏。

## 读源码先看哪里

| 位置 | 作用 |
| --- | --- |
| `xiaou-common/src/main/java/com/xiaou/common/config/SaTokenConfig.java` | 注册 Sa-Token 拦截器，定义哪些路径需要登录 |
| `xiaou-common/src/main/java/com/xiaou/common/satoken/StpAdminUtil.java` | 管理端登录工具，`loginType = "admin"` |
| `xiaou-common/src/main/java/com/xiaou/common/satoken/StpUserUtil.java` | 用户端登录工具，`loginType = "user"` |
| `xiaou-common/src/main/java/com/xiaou/common/aspect/AdminAuthAspect.java` | `@RequireAdmin` 管理员权限切面 |
| `xiaou-system/src/main/java/com/xiaou/system/controller/AuthController.java` | 管理端登录、退出、刷新、个人资料 |
| `xiaou-user/src/main/java/com/xiaou/user/controller/UserAuthController.java` | 用户注册、登录、退出、唯一性校验 |
| `vue3-admin-front/src/utils/request.js` | 管理端 Axios 请求/响应拦截器 |
| `vue3-user-front/src/utils/request.js` | 用户端 Axios 请求/响应拦截器 |

## 两套登录域

| 登录域 | 后端工具 | 前端 Token key | 常见接口 |
| --- | --- | --- | --- |
| 管理端 | `StpAdminUtil` | `token` | `/auth/**`、`/admin/**`、`/log/**` |
| 用户端 | `StpUserUtil` | `user_token` | `/user/**`、用户侧业务接口 |

`StpAdminUtil` 和 `StpUserUtil` 都封装了 Sa-Token 的 `StpLogic`，但登录类型不同。登录类型是区分会话空间的关键。可以把它理解成两张门禁卡：卡面看起来都叫 Token，但后台门禁和用户门禁查的是不同名单。

## 一次请求如何通过鉴权

| 步骤 | 管理端 | 用户端 |
| --- | --- | --- |
| 1. 前端取 Token | `vue3-admin-front/src/stores/user.js` 读取 `token` | `vue3-user-front/src/stores/user.js` 读取 `user_token` |
| 2. 请求头 | `Authorization: Bearer <token>` | `Authorization: Bearer <token>` |
| 3. 后端拦截 | `/auth/**` 和方法级 `@RequireAdmin` | `/user/**` 和业务接口内 `StpUserUtil` |
| 4. 会话校验 | `StpAdminUtil.checkLogin()` | `StpUserUtil.checkLogin()` |
| 5. 角色校验 | `StpAdminUtil.checkRole("admin")` | 普通用户通常只校验登录态 |
| 6. 业务取人 | 管理员 ID | 用户 ID |

排查鉴权问题时先确认“用了哪张卡”。后台接口拿用户端 `user_token` 调用，表面上也是 Bearer Token，但登录域不同，后端会认为未登录。

## 后端拦截规则

`SaTokenConfig` 注册了全局拦截器，主要规则如下：

| 路径 | 规则 |
| --- | --- |
| `/auth/**` | 除 `/auth/login`、`/auth/register`、`/auth/refresh` 外，需要管理员登录 |
| `/user/**` | 除 `/user/auth/login`、`/user/auth/register`、`/user/auth/refresh`、用户名/邮箱检查外，需要用户登录 |
| `/captcha/**` | 放行 |
| `/v3/api-docs/**`、`/swagger-ui/**` | 放行 |

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
| 8 | `LoginResponse` | 返回 `accessToken`、`refreshToken`、`expiresIn`、角色和权限 |

管理端登录失败会记录 `sys_login_log`，包括用户名、IP、浏览器、操作系统、状态和失败原因。这是排查后台账号问题的第一现场。

## 用户端登录流程

| 步骤 | 代码位置 | 说明 |
| --- | --- | --- |
| 1 | `CaptchaServiceImpl.verifyCaptcha` | 先校验 Redis 里的验证码 |
| 2 | `UserInfoMapper.selectByUsernameOrEmail` | 支持用户名或邮箱登录 |
| 3 | `UserInfoServiceImpl.login` | 账号不存在或密码错误都返回统一提示，避免用户枚举 |
| 4 | `UserInfoServiceImpl.login` | `status = 1` 禁用，`status = 2` 删除 |
| 5 | `StpUserUtil.login(user.getId())` | 创建用户会话 |
| 6 | `StpUserUtil.set("userInfo", user)` | 写入用户 Session |
| 7 | `updateLastLoginInfo` | 更新最后登录时间和 IP |
| 8 | `UserLoginResponse` | 返回 `accessToken`、`tokenType`、`expiresIn`、用户资料 |

验证码存储在 Redis，key 前缀是 `user:captcha:`，有效期 5 分钟，验证成功后会删除，不能重复使用。

## 前端如何携带 Token

两个前端的请求拦截器都把 Token 放到请求头：

```text
Authorization: Bearer <token>
```

响应拦截器会处理统一业务码：

| 码值 | 前端行为 |
| --- | --- |
| `200` | 返回 `data` |
| `701` | Token 无效，弹窗并跳登录 |
| `702` | Token 过期，弹窗并跳登录 |
| `703` | 权限不足，提示错误 |
| `704` | 账号禁用，清理登录态 |

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

## 核心数据表

| 表 | 说明 |
| --- | --- |
| `sys_admin` | 管理员账号，状态 `0` 正常、`1` 禁用、`2` 删除 |
| `sys_role` | 角色定义 |
| `sys_permission` | 菜单、按钮、接口权限 |
| `sys_admin_role` | 管理员和角色关系 |
| `sys_role_permission` | 角色和权限关系 |
| `sys_login_log` | 管理端登录成功/失败日志 |
| `user_info` | 普通用户账号，状态 `0` 正常、`1` 禁用、`2` 删除 |

## 排查路径

| 现象 | 优先检查 |
| --- | --- |
| 前端跳登录页 | 响应码是否是 `701` 或 `702`，Token 是否存在 |
| 后台提示权限不足 | 当前管理员是否有 `admin` 角色，接口是否命中 `@RequireAdmin` |
| 用户端登录成功但刷新后丢失 | `user_token` Cookie/localStorage 是否写入 |
| 管理端刷新 Token 失败 | `StpAdminUtil.isLogin()` 是否为 false，Token 是否已经过期 |
| 注册页唯一性校验异常 | 目标接口是否被 `SaTokenConfig` 放行 |
| 新后台接口裸奔 | Controller 方法是否漏了 `@RequireAdmin` |

## 常见坑

| 问题 | 原因 | 排查方式 |
| --- | --- | --- |
| 管理端登录后访问后台接口仍提示未登录 | 请求头没有带 `Authorization`，或用了用户端 `user_token` | 看浏览器 Network 请求头 |
| 用户端接口提示 Token 无效 | 前端没有从 `user_token` 读取，或 Cookie/localStorage 被清空 | 看 `vue3-user-front/src/stores/user.js` |
| `/admin/**` 新接口被普通用户打到 | 后端方法缺少 `@RequireAdmin` | 搜索新增 Controller 方法 |
| 改了密码但旧 Token 还能用 | 当前代码修改密码后用户端会 `logout` 当前会话，管理端提示重新登录；如需踢掉全部设备，要显式调用 Sa-Token kickout |
| 登录失败排查无头绪 | 管理端有 `sys_login_log`，用户端目前主要看业务日志 | 查日志表和应用日志 |

## 新增接口检查清单

1. 先判断接口属于管理端、用户端还是公共端。
2. 管理端接口加 `@RequireAdmin`，操作类接口加 `@Log`。
3. 用户端接口从 `StpUserUtil.getLoginIdAsLong()` 获取当前用户，不信任前端传入的用户 ID。
4. 前端调用统一走 `utils/request.js`，不要手写裸 `fetch` 绕过拦截器。
5. 文档同步更新 API 索引、模块页和操作手册。

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
