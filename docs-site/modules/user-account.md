# 用户账户与个人中心

用户账户体系覆盖注册、登录、资料维护、头像上传、密码修改和管理端用户运营。

## 功能入口

| 端 | 页面 |
| --- | --- |
| 用户端 | `/login`、`/register`、`/profile` |
| 管理端 | `/user`、`/profile/index`、`/profile/edit`、`/profile/change-password` |
| 后端模块 | `xiaou-user`、`xiaou-system`、`xiaou-common` |

## 用户端能力

| 能力 | 接口域 | 说明 |
| --- | --- | --- |
| 注册 | `/user/auth/register` | 创建用户账号 |
| 登录 | `/user/auth/login` | 获取用户端 Token |
| 退出 | `/user/auth/logout` | 清理登录态 |
| 刷新 | `/user/auth/refresh` | 刷新用户端 Token |
| 当前用户 | `/user/auth/info` | 获取登录用户信息 |
| 唯一性校验 | `/user/auth/check-username`、`/check-email`、`/check-phone` | 注册表单校验 |
| 资料 | `/user/profile` | 查询和更新个人资料 |
| 密码 | `/user/password` | 用户自行修改密码 |
| 头像 | `/user/avatar/upload` | 上传头像 |

## 管理端能力

| 能力 | 接口域 | 说明 |
| --- | --- | --- |
| 用户列表 | `/admin/user/list` | 分页筛选用户 |
| 用户详情 | `/admin/user/{userId}` | 查看用户资料 |
| 创建用户 | `/admin/user/create` | 后台创建账号 |
| 更新用户 | `/admin/user/{userId}` | 修改资料 |
| 删除用户 | `/admin/user/{userId}`、`/admin/user/batch` | 单个或批量删除 |
| 状态控制 | `/admin/user/{userId}/status` | 启用或禁用账号 |
| 重置密码 | `/admin/user/{userId}/reset-password` | 管理员重置密码 |
| 用户统计 | `/admin/user/statistics` | 后台统计卡片 |

## 验证码

验证码接口位于 `/captcha`：

- `/captcha/generate`
- `/captcha/verify`

验证码通常服务于注册、登录和安全校验，属于免登录但需要频率保护的入口。

## 开发注意

- 用户端 Token key 是 `user_token`。
- 管理端 Token key 是 `token`。
- 用户资料、头像、文件访问和个人内容要校验归属关系。
- 管理端用户操作要进入操作日志，便于追责。

