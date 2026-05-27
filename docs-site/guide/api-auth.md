# API 认证指南

本文档说明如何获取 Token、刷新 Token 以及调用需要认证的接口。

## 认证架构

Code Nest 使用 Sa-Token 实现双端鉴权：

| 登录域 | Token 名称 | 存储位置 | 适用接口 |
| --- | --- | --- | --- |
| 管理端 | `token` | localStorage | `/auth/**`、`/admin/**`、`/log/**` |
| 用户端 | `user_token` | localStorage | `/user/**`、业务接口 |

**重要**：两端 Token 互相隔离，不能混用。

## 用户端认证

### 1. 获取验证码

```http
GET /api/captcha/register?timestamp=1710000000000
```

返回验证码图片（Base64）和验证码 key。

### 2. 用户注册

```http
POST /api/user/auth/register
Content-Type: application/json

{
  "username": "testuser",
  "email": "test@example.com",
  "password": "Test@123456",
  "captchaCode": "abcd",
  "captchaKey": "captcha:xxx"
}
```

**密码规则**：需包含小写、大写、数字，最少 8 位。

### 3. 用户登录

```http
POST /api/user/auth/login
Content-Type: application/json

{
  "username": "testuser",
  "password": "Test@123456",
  "captchaCode": "abcd",
  "captchaKey": "captcha:xxx"
}
```

**支持**：用户名或邮箱登录。

**响应**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "accessToken": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 7200,
    "userInfo": {
      "id": 1,
      "username": "testuser",
      "nickname": "测试用户",
      "avatar": "/files/avatar/default.png"
    }
  }
}
```

### 4. 使用 Token 调用接口

```http
GET /api/user/points/balance
Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...
```

### 5. 刷新 Token

```http
POST /api/user/auth/refresh
Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...
```

返回新的 Token，旧 Token 立即失效。

### 6. 退出登录

```http
POST /api/user/auth/logout
Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...
```

## 管理端认证

### 1. 管理员登录

```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "123456"
}
```

**响应**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "accessToken": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...",
    "expiresIn": 7200,
    "roles": ["admin"],
    "permissions": ["system:user:list", "system:user:add", ...]
  }
}
```

### 2. 使用 Token 调用管理端接口

```http
GET /api/admin/user/list
Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...
```

### 3. 刷新 Token

```http
POST /api/auth/refresh
Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...
```

### 4. 获取个人信息

```http
GET /api/auth/info
Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...
```

## 错误码说明

| 错误码 | 说明 | 处理方式 |
| --- | --- | --- |
| `200` | 成功 | 正常处理 |
| `701` | Token 无效 | 重新登录 |
| `702` | Token 过期 | 刷新 Token 或重新登录 |
| `703` | 权限不足 | 检查角色和权限 |
| `704` | 账号禁用 | 联系管理员 |

## 前端集成示例

### Axios 请求拦截器

```javascript
import axios from 'axios'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    // 管理端使用 token，用户端使用 user_token
    const token = localStorage.getItem('token') || localStorage.getItem('user_token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  response => {
    const { code, message, data } = response.data
    if (code === 200) {
      return data
    }
    // Token 无效或过期
    if (code === 701 || code === 702) {
      // 清除登录状态
      localStorage.removeItem('token')
      localStorage.removeItem('user_token')
      // 跳转登录页
      window.location.href = '/login'
    }
    return Promise.reject(new Error(message))
  },
  error => {
    return Promise.reject(error)
  }
)

export default request
```

### Vue 组件调用示例

```javascript
import request from '@/utils/request'

// 获取积分余额
async function getPointsBalance() {
  try {
    const data = await request.get('/user/points/balance')
    console.log('积分余额:', data.totalPoints)
  } catch (error) {
    console.error('获取失败:', error.message)
  }
}

// 用户登录
async function login(username, password, captchaCode, captchaKey) {
  try {
    const data = await request.post('/user/auth/login', {
      username,
      password,
      captchaCode,
      captchaKey
    })
    // 保存 Token
    localStorage.setItem('user_token', data.accessToken)
    localStorage.setItem('user_info', JSON.stringify(data.userInfo))
    return data
  } catch (error) {
    console.error('登录失败:', error.message)
    throw error
  }
}
```

## cURL 调用示例

### 用户端

```bash
# 登录
curl -X POST http://localhost:9999/api/user/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"Test@123456","captchaCode":"abcd","captchaKey":"captcha:xxx"}'

# 获取积分余额
curl http://localhost:9999/api/user/points/balance \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

### 管理端

```bash
# 登录
curl -X POST http://localhost:9999/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'

# 获取用户列表
curl http://localhost:9999/api/admin/user/list \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

## Swagger 文档

启动后端后，访问以下地址查看完整 API 文档：

- Swagger UI: `http://localhost:9999/api/swagger-ui.html`
- OpenAPI JSON: `http://localhost:9999/api/v3/api-docs`

## 安全建议

1. **Token 存储**：生产环境建议使用 HttpOnly Cookie
2. **HTTPS**：生产环境必须使用 HTTPS
3. **Token 有效期**：默认 2 小时，可根据需求调整
4. **密码安全**：使用 BCrypt 哈希，不存储明文
5. **验证码**：登录和注册必须使用验证码

## 相关文档

| 文档 | 说明 |
| --- | --- |
| [鉴权与用户体系](/modules/auth) | Sa-Token 双端鉴权详解 |
| [响应体与错误码](/reference/response-errors) | 完整错误码列表 |
| [权限注解与角色边界索引](/reference/permission-boundaries) | 权限控制详解 |
| [API 路由索引](/reference/api-routes) | 完整接口清单 |
| [API 调用示例](/reference/api-examples) | 更多调用示例 |
