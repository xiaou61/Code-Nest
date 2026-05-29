# 账号与系统 API

本文档详细说明账号与系统模块的 API 接口，包括请求参数、响应格式、权限要求和调用示例。

## 路由约定

| 前缀 | 使用场景 | 登录域 |
| --- | --- | --- |
| `/auth` | 管理端认证接口 | 管理员登录态 |
| `/user/auth` | 用户端认证接口 | 用户登录态 |
| `/user` | 用户端业务接口 | 用户登录态 |
| `/admin` | 管理端业务接口 | 管理员登录态 |
| `/captcha` | 验证码接口 | 无需登录 |
| `/log` | 日志接口 | 管理员登录态 |

---

## 一、管理端认证（AuthController）

**模块**：`xiaou-system`
**路由前缀**：`/auth`
**权限要求**：除 login/register/refresh 外需管理员登录

### 1.1 管理员登录

```
POST /auth/login
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | String | 是 | 用户名 |
| password | String | 是 | 密码 |

**请求示例**：

```json
{
  "username": "admin",
  "password": "123456"
}
```

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "accessToken": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...",
    "expiresIn": 604800,
    "userInfo": {
      "id": 1,
      "username": "admin",
      "realName": "管理员",
      "email": "admin@example.com",
      "avatar": "/files/avatar/default.png",
      "lastLoginTime": "2024-01-01 12:00:00",
      "roles": ["admin"],
      "permissions": ["system:user:list", "system:user:add"]
    }
  },
  "timestamp": 1710000000000
}
```

**错误码**：

| 错误码 | 说明 |
|--------|------|
| 705 | 用户名或密码错误 |
| 704 | 账户已被禁用 |
| 500 | 系统内部错误 |

**curl 示例**：

```bash
curl -X POST http://localhost:9999/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'
```

### 1.2 管理员退出

```
POST /auth/logout
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": null,
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl -X POST http://localhost:9999/api/auth/logout \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

### 1.3 刷新 Token

```
POST /auth/refresh
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...",
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl -X POST http://localhost:9999/api/auth/refresh \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

### 1.4 获取个人信息

```
GET /auth/info
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "username": "admin",
    "realName": "管理员",
    "email": "admin@example.com",
    "avatar": "/files/avatar/default.png",
    "lastLoginTime": "2024-01-01 12:00:00",
    "roles": ["admin"],
    "permissions": ["system:user:list", "system:user:add"]
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl http://localhost:9999/api/auth/info \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

### 1.5 修改个人信息

```
PUT /auth/profile
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| realName | String | 否 | 真实姓名 |
| email | String | 否 | 邮箱 |
| phone | String | 否 | 手机号 |
| avatar | String | 否 | 头像 URL |
| gender | Integer | 否 | 性别：0-未知 1-男 2-女 |
| remark | String | 否 | 备注 |

**请求示例**：

```json
{
  "realName": "管理员",
  "email": "admin@example.com",
  "phone": "13800138000",
  "avatar": "/files/avatar/1.png",
  "gender": 1
}
```

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": null,
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl -X PUT http://localhost:9999/api/auth/profile \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"realName":"管理员","email":"admin@example.com"}'
```

### 1.6 修改密码

```
PUT /auth/password
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| oldPassword | String | 是 | 原密码 |
| newPassword | String | 是 | 新密码 |
| confirmPassword | String | 是 | 确认密码 |

**请求示例**：

```json
{
  "oldPassword": "123456",
  "newPassword": "NewPass@123",
  "confirmPassword": "NewPass@123"
}
```

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": null,
  "timestamp": 1710000000000
}
```

**错误码**：

| 错误码 | 说明 |
|--------|------|
| 601 | 原密码错误 |
| 601 | 新密码与确认密码不一致 |
| 601 | 新密码不能与原密码相同 |

**curl 示例**：

```bash
curl -X PUT http://localhost:9999/api/auth/password \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"oldPassword":"123456","newPassword":"NewPass@123","confirmPassword":"NewPass@123"}'
```

---

## 二、用户端认证（UserAuthController）

**模块**：`xiaou-user`
**路由前缀**：`/user/auth`
**权限要求**：除 login/register/refresh/check-username/check-email 外需用户登录

### 2.1 用户注册

```
POST /user/auth/register
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | String | 是 | 用户名（3-20位，字母数字下划线） |
| email | String | 是 | 邮箱 |
| password | String | 是 | 密码（8位以上，包含大小写字母和数字） |
| confirmPassword | String | 是 | 确认密码 |
| captcha | String | 是 | 验证码 |
| captchaKey | String | 是 | 验证码 Key |
| nickname | String | 否 | 昵称（默认为用户名） |
| phone | String | 否 | 手机号 |

**请求示例**：

```json
{
  "username": "testuser",
  "email": "test@example.com",
  "password": "Test@123456",
  "confirmPassword": "Test@123456",
  "captcha": "abcd",
  "captchaKey": "captcha:xxx",
  "nickname": "测试用户"
}
```

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "username": "testuser",
    "nickname": "测试用户",
    "email": "test@example.com",
    "avatar": "/files/avatar/default.png"
  },
  "timestamp": 1710000000000
}
```

**错误码**：

| 错误码 | 说明 |
|--------|------|
| 601 | 验证码错误或已过期 |
| 601 | 两次输入的密码不一致 |
| 603 | 用户名已存在 |
| 603 | 邮箱已被注册 |

**curl 示例**：

```bash
curl -X POST http://localhost:9999/api/user/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","email":"test@example.com","password":"Test@123456","confirmPassword":"Test@123456","captcha":"abcd","captchaKey":"captcha:xxx"}'
```

### 2.2 用户登录

```
POST /user/auth/login
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | String | 是 | 用户名或邮箱 |
| password | String | 是 | 密码 |
| captcha | String | 是 | 验证码 |
| captchaKey | String | 是 | 验证码 Key |

**请求示例**：

```json
{
  "username": "testuser",
  "password": "Test@123456",
  "captcha": "abcd",
  "captchaKey": "captcha:xxx"
}
```

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "accessToken": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 604800,
    "userInfo": {
      "id": 1,
      "username": "testuser",
      "nickname": "测试用户",
      "email": "test@example.com",
      "avatar": "/files/avatar/default.png",
      "gender": 0,
      "lastLoginTime": "2024-01-01 12:00:00"
    }
  },
  "timestamp": 1710000000000
}
```

**错误码**：

| 错误码 | 说明 |
|--------|------|
| 601 | 验证码错误或已过期 |
| 705 | 用户名或密码错误 |
| 704 | 账户已被禁用 |

**curl 示例**：

```bash
curl -X POST http://localhost:9999/api/user/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"Test@123456","captcha":"abcd","captchaKey":"captcha:xxx"}'
```

### 2.3 用户退出

```
POST /user/auth/logout
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": null,
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl -X POST http://localhost:9999/api/user/auth/logout \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

### 2.4 刷新 Token

```
POST /user/auth/refresh
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...",
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl -X POST http://localhost:9999/api/user/auth/refresh \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

### 2.5 检查用户名是否存在

```
GET /user/auth/check-username
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | String | 是 | 用户名 |

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": true,
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl "http://localhost:9999/api/user/auth/check-username?username=testuser"
```

### 2.6 检查邮箱是否存在

```
GET /user/auth/check-email
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| email | String | 是 | 邮箱 |

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": false,
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl "http://localhost:9999/api/user/auth/check-email?email=test@example.com"
```

---

## 三、用户信息（UserController）

**模块**：`xiaou-user`
**路由前缀**：`/user`
**权限要求**：需用户登录

### 3.1 获取当前用户信息

```
GET /user/info
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "username": "testuser",
    "nickname": "测试用户",
    "email": "test@example.com",
    "phone": "13800138000",
    "avatar": "/files/avatar/default.png",
    "gender": 0,
    "status": 0,
    "registerTime": "2024-01-01 12:00:00",
    "lastLoginTime": "2024-01-01 12:00:00",
    "lastLoginIp": "127.0.0.1"
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl http://localhost:9999/api/user/info \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

### 3.2 修改个人信息

```
PUT /user/profile
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| nickname | String | 否 | 昵称 |
| phone | String | 否 | 手机号 |
| avatar | String | 否 | 头像 URL |
| gender | Integer | 否 | 性别：0-未知 1-男 2-女 |

**请求示例**：

```json
{
  "nickname": "新昵称",
  "phone": "13800138000",
  "avatar": "/files/avatar/1.png",
  "gender": 1
}
```

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": null,
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl -X PUT http://localhost:9999/api/user/profile \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"nickname":"新昵称","gender":1}'
```

### 3.3 上传头像

```
POST /user/avatar
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |
| Content-Type | multipart/form-data |

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| file | File | 是 | 头像文件（jpg/png/gif，最大 5MB） |

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "url": "/files/avatar/1.png"
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl -X POST http://localhost:9999/api/user/avatar \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -F "file=@/path/to/avatar.png"
```

### 3.4 修改密码

```
PUT /user/password
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| oldPassword | String | 是 | 原密码 |
| newPassword | String | 是 | 新密码 |
| confirmPassword | String | 是 | 确认密码 |

**请求示例**：

```json
{
  "oldPassword": "Test@123456",
  "newPassword": "NewPass@123",
  "confirmPassword": "NewPass@123"
}
```

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": null,
  "timestamp": 1710000000000
}
```

**错误码**：

| 错误码 | 说明 |
|--------|------|
| 601 | 原密码错误 |
| 601 | 新密码与确认密码不一致 |
| 601 | 新密码不能与原密码相同 |

**curl 示例**：

```bash
curl -X PUT http://localhost:9999/api/user/password \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"oldPassword":"Test@123456","newPassword":"NewPass@123","confirmPassword":"NewPass@123"}'
```

---

## 四、管理端用户管理（AdminUserController）

**模块**：`xiaou-user`
**路由前缀**：`/admin/user`
**权限要求**：需管理员登录

### 4.1 获取用户列表

```
POST /admin/user/list
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | Integer | 否 | 页码（默认 1） |
| pageSize | Integer | 否 | 每页大小（默认 10，最大 100） |
| username | String | 否 | 用户名筛选 |
| email | String | 否 | 邮箱筛选 |
| status | Integer | 否 | 状态筛选：0-正常 1-禁用 2-删除 |
| startDate | String | 否 | 开始日期（yyyy-MM-dd） |
| endDate | String | 否 | 结束日期（yyyy-MM-dd） |

**请求示例**：

```json
{
  "pageNum": 1,
  "pageSize": 10,
  "status": 0
}
```

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "pageNum": 1,
    "pageSize": 10,
    "total": 100,
    "totalPages": 10,
    "records": [
      {
        "id": 1,
        "username": "testuser",
        "nickname": "测试用户",
        "email": "test@example.com",
        "phone": "13800138000",
        "avatar": "/files/avatar/default.png",
        "gender": 0,
        "status": 0,
        "statusDesc": "正常",
        "registerTime": "2024-01-01 12:00:00",
        "lastLoginTime": "2024-01-01 12:00:00"
      }
    ],
    "hasNext": true,
    "hasPrevious": false
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl -X POST http://localhost:9999/api/admin/user/list \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"pageNum":1,"pageSize":10,"status":0}'
```

### 4.2 获取用户详情

```
GET /admin/user/{id}
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 用户 ID |

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "username": "testuser",
    "nickname": "测试用户",
    "email": "test@example.com",
    "phone": "13800138000",
    "avatar": "/files/avatar/default.png",
    "gender": 0,
    "status": 0,
    "statusDesc": "正常",
    "registerTime": "2024-01-01 12:00:00",
    "lastLoginTime": "2024-01-01 12:00:00",
    "lastLoginIp": "127.0.0.1",
    "loginCount": 10
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl http://localhost:9999/api/admin/user/1 \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

### 4.3 更新用户状态

```
PUT /admin/user/{id}/status
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 用户 ID |

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| status | Integer | 是 | 状态：0-正常 1-禁用 |

**请求示例**：

```json
{
  "status": 1
}
```

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": null,
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl -X PUT http://localhost:9999/api/admin/user/1/status \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"status":1}'
```

### 4.4 重置密码

```
PUT /admin/user/{id}/password
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 用户 ID |

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| newPassword | String | 是 | 新密码 |

**请求示例**：

```json
{
  "newPassword": "123456"
}
```

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": null,
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl -X PUT http://localhost:9999/api/admin/user/1/password \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"newPassword":"123456"}'
```

### 4.5 获取用户统计

```
GET /admin/user/statistics
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "totalUsers": 1000,
    "todayNewUsers": 10,
    "activeUsers": 500,
    "disabledUsers": 50
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl http://localhost:9999/api/admin/user/statistics \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

---

## 五、验证码（CaptchaController）

**模块**：`xiaou-user`
**路由前缀**：`/captcha`
**权限要求**：无需登录

### 5.1 获取验证码

```
GET /captcha/register
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| timestamp | Long | 否 | 时间戳（防止缓存） |

**响应**：返回验证码图片（Base64 编码）

**curl 示例**：

```bash
curl "http://localhost:9999/api/captcha/register?timestamp=1710000000000" --output captcha.png
```

---

## 六、仪表盘（DashboardController）

**模块**：`xiaou-system`
**路由前缀**：`/admin/dashboard`
**权限要求**：需管理员登录

### 6.1 获取仪表盘概览

```
GET /admin/dashboard/overview
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "totalUsers": 1000,
    "todayLoginCount": 50,
    "onlineUserCount": 20,
    "todayFailedOperationCount": 5,
    "totalPointsIssued": 100000,
    "activePointUsers": 500,
    "moduleHealthList": [
      {
        "name": "用户服务",
        "latency": "10ms",
        "status": "healthy",
        "statusText": "正常",
        "statusType": "success"
      },
      {
        "name": "积分服务",
        "latency": "50ms",
        "status": "healthy",
        "statusText": "正常",
        "statusType": "success"
      }
    ],
    "recentOperations": [
      {
        "id": 1,
        "time": "12:00",
        "title": "用户管理 · 新增用户",
        "desc": "新增用户 testuser"
      }
    ]
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl http://localhost:9999/api/admin/dashboard/overview \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

---

## 七、日志管理（LogController）

**模块**：`xiaou-system`
**路由前缀**：`/log`
**权限要求**：需管理员登录

### 7.1 获取登录日志列表

```
POST /log/login
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | Integer | 否 | 页码（默认 1） |
| pageSize | Integer | 否 | 每页大小（默认 10） |
| username | String | 否 | 用户名筛选 |
| loginIp | String | 否 | 登录 IP 筛选 |
| loginStatus | Integer | 否 | 登录状态：0-成功 1-失败 |
| startTime | String | 否 | 开始时间（yyyy-MM-dd HH:mm:ss） |
| endTime | String | 否 | 结束时间（yyyy-MM-dd HH:mm:ss） |

**请求示例**：

```json
{
  "pageNum": 1,
  "pageSize": 10,
  "loginStatus": 0
}
```

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "pageNum": 1,
    "pageSize": 10,
    "total": 100,
    "totalPages": 10,
    "records": [
      {
        "id": 1,
        "adminId": 1,
        "username": "admin",
        "loginIp": "127.0.0.1",
        "loginLocation": "本地",
        "browser": "Chrome",
        "os": "Windows",
        "loginStatus": 0,
        "loginMessage": "登录成功",
        "loginTime": "2024-01-01 12:00:00"
      }
    ],
    "hasNext": true,
    "hasPrevious": false
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl -X POST http://localhost:9999/api/log/login \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"pageNum":1,"pageSize":10,"loginStatus":0}'
```

### 7.2 获取操作日志列表

```
POST /log/operation
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | Integer | 否 | 页码（默认 1） |
| pageSize | Integer | 否 | 每页大小（默认 10） |
| module | String | 否 | 模块筛选 |
| operationType | String | 否 | 操作类型筛选 |
| operatorName | String | 否 | 操作人筛选 |
| status | Integer | 否 | 状态：0-成功 1-失败 |
| startTime | String | 否 | 开始时间（yyyy-MM-dd HH:mm:ss） |
| endTime | String | 否 | 结束时间（yyyy-MM-dd HH:mm:ss） |

**请求示例**：

```json
{
  "pageNum": 1,
  "pageSize": 10,
  "module": "用户管理"
}
```

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "pageNum": 1,
    "pageSize": 10,
    "total": 100,
    "totalPages": 10,
    "records": [
      {
        "id": 1,
        "module": "用户管理",
        "operationType": "INSERT",
        "operationTypeText": "新增",
        "description": "新增用户 testuser",
        "method": "com.xiaou.user.controller.AdminUserController.save",
        "requestUri": "/admin/user",
        "requestMethod": "POST",
        "requestParams": "{\"username\":\"testuser\"}",
        "responseData": "{\"code\":200}",
        "operatorId": 1,
        "operatorName": "admin",
        "operatorIp": "127.0.0.1",
        "operationLocation": "本地",
        "browser": "Chrome",
        "os": "Windows",
        "status": 0,
        "errorMsg": null,
        "operationTime": "2024-01-01 12:00:00",
        "costTime": 100
      }
    ],
    "hasNext": true,
    "hasPrevious": false
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl -X POST http://localhost:9999/api/log/operation \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"pageNum":1,"pageSize":10,"module":"用户管理"}'
```

---

## 相关文档

| 文档 | 说明 |
|------|------|
| [鉴权与用户体系](/modules/auth) | Sa-Token 双端鉴权详解 |
| [用户账户与个人中心](/modules/user-account) | 用户模块详解 |
| [系统运营后台](/modules/system-ops) | 系统管理详解 |
| [仪表盘与日志](/modules/dashboard-logs) | 仪表盘和日志详解 |
| [响应体与错误码](/reference/response-errors) | 完整错误码列表 |
| [API 路由索引](/reference/api-routes) | 完整接口清单 |
