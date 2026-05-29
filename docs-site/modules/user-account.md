# 用户账户与个人中心

用户账户模块负责普通用户从“注册进来”到“维护资料”的整条链路，也提供后台管理员对用户的运营能力。它和验证码、Sa-Token、积分账户、通知中心、文件存储都有联动。

## 功能入口

| 端 | 页面或接口 | 说明 |
| --- | --- | --- |
| 用户端 | `/login`、`/register`、`/profile` | 登录、注册、个人资料 |
| 管理端 | `/user`、`/profile/index`、`/profile/edit`、`/profile/change-password` | 用户管理和管理员个人资料 |
| 后端 | `xiaou-user` | 普通用户认证、资料、后台用户管理 |
| 公共能力 | `xiaou-common`、`xiaou-filestorage`、`xiaou-notification`、`xiaou-points` | Token、头像上传、通知、积分账户 |

## 推荐学习顺序

用户账户模块最适合按“用户生命周期”阅读：

1. 先读验证码生成和校验，因为注册、登录、改密码都会经过它。
2. 再读注册流程，理解 `user_info`、积分账户和欢迎通知之间的关系。
3. 接着读登录流程，重点看统一错误提示、用户状态和 Sa-Token 会话创建。
4. 然后读个人资料、修改密码和头像上传，理解当前登录用户接口为什么比传 `userId` 更安全。
5. 最后读后台用户管理，确认禁用、删除、重置密码和统计对用户端行为的影响。

## 源码地图

| 位置 | 作用 |
| --- | --- |
| `xiaou-user/src/main/java/com/xiaou/user/controller/UserAuthController.java` | 注册、登录、退出、唯一性校验 |
| `xiaou-user/src/main/java/com/xiaou/user/controller/UserController.java` | 用户资料、密码、头像 |
| `xiaou-user/src/main/java/com/xiaou/user/controller/AdminUserController.java` | 后台用户管理 |
| `xiaou-user/src/main/java/com/xiaou/user/service/impl/UserInfoServiceImpl.java` | 用户核心业务逻辑 |
| `xiaou-user/src/main/java/com/xiaou/user/service/impl/CaptchaServiceImpl.java` | 验证码生成和验证 |
| `xiaou-user/src/main/java/com/xiaou/user/domain/UserInfo.java` | 用户字段、状态和校验规则 |
| `sql/MySql/code_nest.sql` | `user_info` 表结构 |

## 用户端接口

| 能力 | 接口 | 说明 |
| --- | --- | --- |
| 注册 | `POST /user/auth/register` | 创建用户账号 |
| 登录 | `POST /user/auth/login` | 获取用户端 Token |
| 退出 | `POST /user/auth/logout` | 清理当前会话 |
| 刷新 | `POST /user/auth/refresh` | 返回当前 Token 和剩余时间 |
| 当前用户 | `GET /user/auth/info` | 获取登录用户资料 |
| 用户名校验 | `GET /user/auth/check-username` | 注册前唯一性检查 |
| 邮箱校验 | `GET /user/auth/check-email` | 注册前唯一性检查 |
| 手机校验 | `GET /user/auth/check-phone` | 注册前唯一性检查 |
| 个人资料 | `GET /user/profile`、`PUT /user/profile` | 查询和更新当前用户 |
| 修改密码 | `PUT /user/password` | 校验旧密码、验证码和确认密码 |
| 头像上传 | `POST /user/avatar/upload` | 上传头像并更新用户资料 |

用户端也保留了 `GET /user/{userId}`、`PUT /user/{userId}`、`PUT /user/{userId}/password` 这类带用户 ID 的接口。开发新功能时，面向当前登录用户的场景应优先使用 `/user/profile` 和 `/user/password`，避免前端传错或越权传入别人的 `userId`。

注意：`check-phone` Controller 接口已经存在，但当前 `SaTokenConfig` 只放行了 `check-username` 和 `check-email`。如果注册页要在未登录状态实时校验手机号，需要把 `/user/auth/check-phone` 也加入放行列表。

## 当前登录用户接口优先级

| 业务场景 | 推荐接口 | 不推荐的原因 |
| --- | --- | --- |
| 查看自己的资料 | `GET /user/profile` | `GET /user/{userId}` 需要前端传用户 ID |
| 修改自己的资料 | `PUT /user/profile` | `PUT /user/{userId}` 更容易被误用成越权入口 |
| 修改自己的密码 | `PUT /user/password` | `PUT /user/{userId}/password` 是历史带 ID 形式 |
| 上传自己的头像 | `POST /user/avatar/upload` | 头像必须绑定当前登录用户 |

这条规则很重要：用户端功能应该以 Token 里的用户 ID 为准，而不是相信前端传来的 `userId`。

## 管理端接口

| 能力 | 接口 | 说明 |
| --- | --- | --- |
| 用户列表 | `GET /admin/user/list` | 分页筛选用户 |
| 全量用户 | `GET /admin/user/all` | 不分页，谨慎使用 |
| 用户详情 | `GET /admin/user/{userId}` | 查看单个用户 |
| 创建用户 | `POST /admin/user/create` | 后台创建账号 |
| 更新用户 | `PUT /admin/user/{userId}` | 修改资料 |
| 删除用户 | `DELETE /admin/user/{userId}` | 逻辑删除 |
| 批量删除 | `DELETE /admin/user/batch` | 批量逻辑删除 |
| 启用/禁用 | `PUT /admin/user/{userId}/status` | 只接受 `0` 或 `1` |
| 重置密码 | `PUT /admin/user/{userId}/reset-password` | 默认新密码 `123456` |
| 用户统计 | `GET /admin/user/statistics` | 统计总量、正常、禁用、删除 |

这些接口都应该由管理员调用，Controller 上使用 `@RequireAdmin` 做权限保护。

## 注册流程

| 步骤 | 说明 |
| --- | --- |
| 1 | 前端先调用 `/captcha/generate` 获取 `captchaKey` 和验证码图片 |
| 2 | 注册时提交用户名、密码、确认密码、邮箱、手机号和验证码 |
| 3 | 后端调用 `CaptchaServiceImpl.verifyCaptcha`，验证码错误或过期直接失败 |
| 4 | 检查两次密码是否一致 |
| 5 | 检查用户名、邮箱、手机号是否已存在 |
| 6 | 使用 `PasswordUtil.encode` 加密密码 |
| 7 | 插入 `user_info`，默认 `status = 0` |
| 8 | 调用 `PointsService.createPointsAccountForNewUser` 创建积分账户 |
| 9 | 调用 `NotificationUtil.sendSystemMessage` 发送欢迎消息 |

积分账户和欢迎消息失败只会记录警告，不会回滚注册。这是一个重要设计：注册是主流程，积分和通知是附属流程。

## 注册后的副作用

| 副作用 | 失败影响 | 排查位置 |
| --- | --- | --- |
| 创建积分账户 | 不回滚注册 | 积分模块日志、积分账户表 |
| 发送欢迎通知 | 不回滚注册 | 通知模块日志、`notification` |
| 写用户主表 | 主流程，失败则注册失败 | `user_info` |

因此“用户注册成功但没有积分/通知”不是同一个问题。先确认 `user_info` 是否写入，再分别排查积分和通知。

## 登录流程

| 步骤 | 说明 |
| --- | --- |
| 1 | 验证验证码，成功后删除 Redis 中的验证码 |
| 2 | 根据用户名或邮箱查用户 |
| 3 | 用户不存在、密码错误、删除账号统一返回“用户名或密码错误” |
| 4 | `status = 1` 返回账号禁用提示 |
| 5 | 密码通过后更新最后登录时间和 IP |
| 6 | `StpUserUtil.login(user.getId())` 创建用户会话 |
| 7 | 写入 Session：`userInfo` 和 `username` |
| 8 | 返回 `accessToken`、`tokenType = Bearer`、`expiresIn = 604800` |

这里统一错误提示是为了降低账号枚举风险。对外不要暴露“邮箱存在但密码错”这类细节。

## 验证码规则

| 项 | 规则 |
| --- | --- |
| Redis key | `user:captcha:{captchaKey}` |
| 有效期 | 5 分钟 |
| 字符长度 | 4 位 |
| 图片尺寸 | 120 x 40 |
| 校验方式 | 忽略大小写 |
| 成功后 | 删除验证码，防止重复使用 |

验证码当前用于注册、登录和修改密码。它是免登录接口，所以后续如果暴露到公网，要结合网关、Nginx 或后端限流保护。

## 用户状态和字段

| 字段 | 说明 |
| --- | --- |
| `username` | 3 到 20 位，只允许字母、数字和下划线 |
| `password` | 存储加密后的密码 |
| `nickname` | 默认可以等于用户名 |
| `email` | 唯一索引 |
| `phone` | 唯一索引，格式要求中国大陆手机号 |
| `gender` | `0` 未知、`1` 男、`2` 女 |
| `status` | `0` 正常、`1` 禁用、`2` 删除 |
| `last_login_time` | 最后登录时间 |
| `last_login_ip` | 最后登录 IP |
| `register_time` | 注册时间 |

## 头像上传

头像上传在 `UserController.uploadAvatar` 中完成：

1. 必须登录，从 `StpUserUtil` 获取当前用户 ID。
2. 文件不能为空。
3. 只允许 `jpg`、`jpeg`、`png`、`gif`。
4. 文件大小不能超过 5MB。
5. 调用 `FileStorageService.uploadSingle(file, "user", "avatar")`。
6. 上传成功后把返回的 `accessUrl` 写入用户头像字段。

头像不是直接写磁盘路径，而是走文件存储模块。这样将来从本地存储迁移到 OSS/COS 时，用户模块不用重写上传逻辑。

## 资料和密码变更通知

`UserInfoServiceImpl` 会在两个场景触发通知：

| 场景 | 通知 |
| --- | --- |
| 注册成功 | 欢迎加入 Code Nest |
| 邮箱或手机号变更 | 账户信息变更通知 |
| 密码修改成功 | 密码修改成功通知 |

这些通知失败不会阻断主流程。排查时看业务日志和通知表即可。

## 核心数据表

| 表 | 说明 |
| --- | --- |
| `user_info` | 用户主表 |
| `points_account` | 新用户注册时创建积分账户，见积分模块 |
| `notification` | 注册欢迎、资料变更、密码变更通知 |
| `file_info` | 用户头像上传记录 |

## 排查路径

| 现象 | 优先检查 |
| --- | --- |
| 验证码总是错误 | Redis 中 `user:captcha:{captchaKey}` 是否存在，是否被成功校验后删除 |
| 注册提示用户名/邮箱/手机号存在 | `user_info` 唯一字段是否已有记录，删除用户是否仍占用唯一值 |
| 登录提示用户名或密码错误 | 用户不存在、密码错、删除状态都会统一提示 |
| 登录提示账号禁用 | `user_info.status = 1` |
| 修改密码后接口 401 | 修改密码成功后会主动退出当前会话 |
| 头像上传后不显示 | 文件存储返回的 `accessUrl` 是否可访问 |
| 注册页手机号预校验被拦截 | `/user/auth/check-phone` 是否在 Sa-Token 放行列表 |

## 常见坑

| 问题 | 原因 | 建议 |
| --- | --- | --- |
| 注册成功但没有积分账户 | 积分模块异常被捕获为 warning | 查应用日志，不要误以为注册失败 |
| 修改密码后前端还停留在旧页面 | `/user/password` 成功后会 `StpUserUtil.logout()` | 前端应跳登录页 |
| 用户资料接口能传 `userId` | 历史接口保留 | 新业务优先用当前登录用户接口 |
| 注册页手机号校验未登录时报错 | `SaTokenConfig` 未放行 `/user/auth/check-phone` | 补充放行路径或前端改为提交注册时统一校验 |
| 头像 URL 访问失败 | 文件存储配置或本地 `/files/**` 映射异常 | 查文件存储模块和 `LocalFileResourceConfig` |
| 后台重置密码暴露新密码 | 当前接口返回“新密码: xxx” | 生产环境建议改为只提示成功，并通过安全渠道告知用户 |

## 验证清单

| 场景 | 预期 |
| --- | --- |
| 生成验证码 | 返回 `captchaKey` 和 base64 图片 |
| 错误验证码注册 | 注册失败，不写 `user_info` |
| 正确注册 | 写入 `user_info`，尝试创建积分账户并发送欢迎通知 |
| 使用用户名登录 | 返回用户端 Token 和用户资料 |
| 使用邮箱登录 | 同样返回用户端 Token |
| 禁用用户登录 | 返回账号禁用提示 |
| 修改个人资料 | 写入当前登录用户，不需要前端传用户 ID |
| 修改密码成功 | 当前会话退出，前端应跳登录 |
| 上传头像 | 文件存储有记录，用户头像字段更新为访问 URL |

---

## 用户模块深度拆解

以下内容来自对 xiaou-user 模块全部源码的逐行阅读，覆盖 3 个 ServiceImpl、4 个 Controller、1 个 Domain、1 个 Mapper。

### 一、认证架构详解

#### 1.1 Sa-Token 双端认证体系

项目使用 Sa-Token 实现双端认证：用户端（`StpUserUtil`）和管理端（`StpAdminUtil`）使用独立的 Token 存储：

| 端 | 工具类 | Login Type | Token 前缀 | 用途 |
| --- | --- | --- | --- | --- |
| 用户端 | `StpUserUtil` | `user` | `user:` | 普通用户操作 |
| 管理端 | `StpAdminUtil` | `admin` | `admin:` | 后台管理操作 |

**双端隔离**：用户 Token 和管理端 Token 存储在不同的 Redis 命名空间下，互不干扰。管理员可以同时以用户身份和管理员身份登录。

#### 1.2 登录安全设计

**防用户枚举**：登录失败时统一返回"用户名或密码错误"，不区分"用户不存在"和"密码错误"。已删除用户（status=2）也返回相同的模糊提示。

```
if (user == null):         throw "用户名或密码错误"
if (user.status == 1):     throw "账户已被禁用"     ← 唯一区分的场景
if (user.status == 2):     throw "用户名或密码错误"  ← 已删除不暴露
if (!passwordMatch):       throw "用户名或密码错误"
```

**验证码一次性消费**：验证成功后立即从 Redis 删除，防止重放攻击。

**密码修改强制重登**：`/user/password` 成功后调用 `StpUserUtil.logout()`，前端应跳转登录页。

#### 1.3 验证码服务

**源码**：`CaptchaServiceImpl`

```
generateCaptcha():
  1. Hutool LineCaptcha: 120×40, 4字符, 5条干扰线
  2. 生成 UUID captchaKey
  3. Redis SET user:captcha:{key} → code, TTL=300s
  4. 返回 { captchaKey, captchaImage(base64), expiresIn }

verifyCaptcha(key, input):
  1. Redis GET user:captcha:{key}
  2. 不存在 → false (过期)
  3. equalsIgnoreCase 比较
  4. 成功 → 删除 Redis key (一次性)
  5. 失败 → key 不删除, 可重试
```

**关键发现**：验证码输入错误时不会删除 Redis key，允许在有效期内多次尝试。如果需要防暴力破解，应添加错误次数限制。

### 二、注册流程深度分析

```
register(request):
  1. verifyCaptcha → 失败 throw
  2. password == confirmPassword → 不一致 throw
  3. username 唯一性检查
  4. email 唯一性检查
  5. phone 唯一性检查 (如果提供了手机号)
  6. PasswordUtil.encode(password)  // BCrypt 加密
  7. nickname 默认 = username
  8. status = 0 (正常)
  9. INSERT user_info
  10. try { pointsService.createPointsAccountForNewUser(userId) } catch { warn }
  11. try { NotificationUtil.sendSystemMessage(...) } catch { warn }
```

**事务边界**：`@Transactional` 覆盖步骤 1-9。步骤 10-11 是 try-catch 静默处理，失败不回滚注册。

**唯一性竞态**：步骤 3-5 检查与步骤 9 INSERT 之间存在时间窗口，并发注册可能通过唯一性检查但 INSERT 时违反唯一约束。此时依赖数据库唯一索引抛异常，被外层 catch 转为"注册失败，请稍后重试"。

### 三、管理端能力详解

#### 3.1 用户状态管理

| 操作 | 方法 | 说明 |
| --- | --- | --- |
| 启用 | `updateUserStatus(userId, 0)` | 恢复正常状态 |
| 禁用 | `updateUserStatus(userId, 1)` | 不踢出已登录会话 |
| 删除 | `deleteUser(userId)` | 逻辑删除（status=2），不踢出会话 |
| 重置密码 | `resetUserPassword(userId, newPassword)` | 默认 `123456` |

**关键发现 1**：禁用/删除用户不会强制其退出已登录的 Sa-Token 会话。被禁用的用户在 Token 过期前（7 天）仍可操作。

**关键发现 2**：管理员重置密码的默认新密码是 `123456`，且通过 URL 参数传递（`@RequestParam`），这意味着密码会出现在服务器访问日志和浏览器历史中。

#### 3.2 用户统计实现

```java
getUserStatistics():
  // 执行 4 次分页查询，每次只取总数
  totalUsers = getUserList(pageSize=1).total
  activeUsers = getUserList(status=0, pageSize=1).total
  disabledUsers = getUserList(status=1, pageSize=1).total
  deletedUsers = getUserList(status=2, pageSize=1).total
```

**关键发现**：统计通过 4 次分页查询实现，每次只取 `pageSize=1` 来获取 total。这比直接 COUNT SQL 效率低，因为每次都需要执行完整的查询逻辑（包括 DTO 转换），只是丢弃了实际数据。

### 四、深度发现与坑点

#### 4.1 已确认的代码问题

| 编号 | 问题 | 位置 | 影响 |
| --- | --- | --- | --- |
| BUG-1 | 禁用/删除不踢出已登录会话 | `updateUserStatus`/`deleteUser` | 被禁用用户在 Token 过期前仍可操作 |
| BUG-2 | 重置密码通过 URL 参数传递 | `AdminUserController.resetPassword` | 密码出现在服务器日志 |
| BUG-3 | 验证码错误不限制重试次数 | `CaptchaServiceImpl.verifyCaptcha` | 可暴力尝试 4 位验证码 |
| BUG-4 | 统计使用 4 次分页查询 | `AdminUserController.getUserStatistics` | 性能差，应用 COUNT SQL |
| BUG-5 | `/user/{userId}` 接口无权限校验 | `UserController.getUserInfo` | 任何登录用户可查看任意用户信息 |
| BUG-6 | `getAllUsers` 使用 `Integer.MAX_VALUE` | `AdminUserController.getAllUsers` | 用户量大时 OOM 风险 |

#### 4.2 设计层面的潜在风险

| 编号 | 风险 | 说明 |
| --- | --- | --- |
| RISK-1 | 唯一性检查与 INSERT 竞态 | 并发注册可能依赖数据库异常而非业务校验 |
| RISK-2 | 默认密码 123456 | 用户可能永远不修改，账号安全性低 |
| RISK-3 | 用户端 ID 路径接口越权 | `/user/{userId}` 未校验当前登录用户是否等于路径 userId |
| RISK-4 | 验证码日志打印明文 | `CaptchaServiceImpl.generateCaptcha` 打印 code | 泄露到日志文件 |

#### 4.3 架构设计亮点

| 编号 | 亮点 | 说明 |
| --- | --- | --- |
| H-1 | 防用户枚举 | 统一错误提示，不暴露用户存在性 |
| H-2 | 注册副作用容错 | 积分/通知失败不回滚注册 |
| H-3 | 密码修改强制重登 | 修改后立即 logout，安全性好 |
| H-4 | 文件存储抽象 | 头像上传走 FileStorageService，可无缝迁移到 OSS |
| H-5 | 验证码一次性消费 | 成功后删除 Redis key |
| H-6 | 默认头像生成 | `AvatarUtils.getUserAvatar` 为无头像用户生成基于 ID 的默认头像 |

#### 4.4 源码导航速查

| 想了解 | 读什么 |
| --- | --- |
| 注册流程 | `UserInfoServiceImpl.register` — 完整 9 步 |
| 登录流程 | `UserInfoServiceImpl.login` — 防枚举+会话创建 |
| 验证码 | `CaptchaServiceImpl` — Redis 存储+一次性消费 |
| 用户 CRUD | `UserInfoServiceImpl` — 管理端/用户端全部操作 |
| 密码修改 | `UserInfoServiceImpl.changePassword` — 旧密码校验+强制重登 |
| 头像上传 | `UserController.uploadAvatar` — 文件校验+FileStorageService |
| 管理端 API | `AdminUserController` — CRUD+状态+密码重置+统计 |

## 相关模块

| 模块 | 关系 | 说明 |
| --- | --- | --- |
| [公共底座](/modules/common) | 强依赖 | 用户账户依赖公共底座的 Sa-Token 鉴权、密码工具和统一响应 |
| [鉴权与用户体系](/modules/auth) | 强依赖 | 用户登录、Token 管理依赖鉴权模块 |
| [积分与抽奖](/modules/points) | 强依赖 | 用户注册时自动创建积分账户 |
| [通知中心](/modules/notification) | 强依赖 | 注册欢迎通知、资料变更通知 |
| [文件存储](/modules/file-storage) | 强依赖 | 用户头像上传和存储 |
| [社区帖子](/modules/community) | 被依赖 | 用户主页、发帖统计依赖用户信息 |
| [IM 聊天室](/modules/chat) | 被依赖 | 聊天用户信息展示依赖用户账户 |
| [系统运营后台](/modules/system-ops) | 被依赖 | 用户管理界面在管理端 |
