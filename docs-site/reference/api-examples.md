# API 调用示例

这页给本地联调和二次开发提供一组可复制的接口调用示例。完整接口分组看 [API 路由索引](/reference/api-routes)，错误码看 [响应体与错误码](/reference/response-errors)。

示例默认后端已经启动在：

```text
http://localhost:9999/api
```

如果你从前端代码里调用接口，两个前端的 axios `baseURL` 都是 `/api`，请求会通过 Vite 代理转发到后端。只有用 `curl`、Postman、Apifox 或脚本直连后端时，才需要写完整地址。

## 基础约定

| 项 | 说明 |
| --- | --- |
| 后端直连 base URL | `http://localhost:9999/api` |
| 前端代理路径 | `/api` |
| 用户端 Token | `Authorization: Bearer <USER_TOKEN>` |
| 管理端 Token | `Authorization: Bearer <ADMIN_TOKEN>` |
| JSON 请求头 | `Content-Type: application/json` |
| Swagger | `http://localhost:9999/api/swagger-ui.html` |

PowerShell 里建议使用 `curl.exe`，避免 `curl` 被解析成 `Invoke-WebRequest`。

```powershell
$BaseUrl = "http://localhost:9999/api"
```

统一成功响应通常是：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {},
  "timestamp": 1710000000000
}
```

前端响应拦截器会把 `code === 200` 的 `data` 拆出来给页面使用。自己写脚本时要先判断业务 `code`，不要只看 HTTP `200`。

## 获取验证码

用户注册、用户登录和管理端登录都会用到验证码字段。先调用：

```powershell
curl.exe -X GET "$BaseUrl/captcha/generate"
```

响应里的 `data` 会包含验证码标识和图片数据。验证码明文在本地开发日志中也会打印，便于联调。

登录或注册时把验证码标识填到 `captchaKey`，验证码内容填到 `captcha`。

## 用户端登录

```powershell
curl.exe -X POST "$BaseUrl/user/auth/login" `
  -H "Content-Type: application/json" `
  -d '{
    "username": "demo_user",
    "password": "123456",
    "captcha": "1234",
    "captchaKey": "captcha_xxx",
    "rememberMe": false
  }'
```

成功后重点看：

```json
{
  "accessToken": "<USER_TOKEN>",
  "tokenType": "Bearer",
  "expiresIn": 604800,
  "userInfo": {
    "id": 1,
    "username": "demo_user"
  }
}
```

后续用户端接口都带上：

```powershell
$UserToken = "<USER_TOKEN>"
```

```powershell
curl.exe -X GET "$BaseUrl/user/auth/info" `
  -H "Authorization: Bearer $UserToken"
```

## 用户注册

```powershell
curl.exe -X POST "$BaseUrl/user/auth/register" `
  -H "Content-Type: application/json" `
  -d '{
    "username": "demo_user",
    "password": "123456",
    "confirmPassword": "123456",
    "nickname": "演示用户",
    "email": "demo@example.com",
    "phone": "13800138000",
    "captcha": "1234",
    "captchaKey": "captcha_xxx"
  }'
```

注意：

| 字段 | 规则 |
| --- | --- |
| `username` | 3 到 20 位，只能包含字母、数字和下划线 |
| `password` | 6 到 20 位 |
| `email` | 必填且要符合邮箱格式 |
| `phone` | 可选，若填写需符合中国大陆手机号格式 |
| `captchaKey`、`captcha` | 来自验证码接口 |

## 管理端登录

管理端和用户端不是同一个登录域。管理端调用 `/auth/login`，用户端调用 `/user/auth/login`。

```powershell
curl.exe -X POST "$BaseUrl/auth/login" `
  -H "Content-Type: application/json" `
  -d '{
    "username": "admin",
    "password": "123456",
    "captcha": "1234",
    "captchaKey": "captcha_xxx",
    "rememberMe": false
  }'
```

成功后保存：

```powershell
$AdminToken = "<ADMIN_TOKEN>"
```

然后验证管理端登录态：

```powershell
curl.exe -X GET "$BaseUrl/auth/info" `
  -H "Authorization: Bearer $AdminToken"
```

如果返回 `701`、`702` 或 `703`，分别表示 Token 无效、Token 过期或权限不足。

## 分页接口

项目里很多列表接口使用 `POST` 加 JSON 查询条件，也有少量 `GET` 查询参数。具体以 Controller 为准。

示例：OJ 题目列表。

```powershell
curl.exe -X POST "$BaseUrl/oj/problems/list" `
  -H "Content-Type: application/json" `
  -d '{
    "pageNum": 1,
    "pageSize": 10,
    "keyword": "",
    "difficulty": null
  }'
```

分页响应常见结构：

```json
{
  "pageNum": 1,
  "pageSize": 10,
  "total": 100,
  "totalPages": 10,
  "records": []
}
```

如果你不确定某个列表到底是 `GET` 还是 `POST`，先看 [API 路由索引](/reference/api-routes)，再打开对应 Controller。

## 用户端业务示例

### 获取当前用户资料

```powershell
curl.exe -X GET "$BaseUrl/user/profile" `
  -H "Authorization: Bearer $UserToken"
```

### OJ 提交代码

```powershell
curl.exe -X POST "$BaseUrl/oj/submit" `
  -H "Authorization: Bearer $UserToken" `
  -H "Content-Type: application/json" `
  -d '{
    "problemId": 1,
    "language": "java",
    "code": "public class Main { public static void main(String[] args) { System.out.println(\"Hello\"); } }"
  }'
```

完整判题依赖 go-judge。接口返回成功只说明提交创建成功，还要继续查看提交详情：

```powershell
curl.exe -X GET "$BaseUrl/oj/submissions/1" `
  -H "Authorization: Bearer $UserToken"
```

### 积分签到

```powershell
curl.exe -X POST "$BaseUrl/user/points/checkin" `
  -H "Authorization: Bearer $UserToken"
```

签到类接口要同时检查余额和积分流水：

```powershell
curl.exe -X GET "$BaseUrl/user/points/balance" `
  -H "Authorization: Bearer $UserToken"
```

```powershell
curl.exe -X POST "$BaseUrl/user/points/detail" `
  -H "Authorization: Bearer $UserToken" `
  -H "Content-Type: application/json" `
  -d '{
    "pageNum": 1,
    "pageSize": 10
  }'
```

## 文件上传示例

单文件上传：

```powershell
curl.exe -X POST "$BaseUrl/file/upload/single" `
  -H "Authorization: Bearer $UserToken" `
  -F "file=@D:\temp\avatar.png" `
  -F "businessType=avatar"
```

获取文件 URL：

```powershell
curl.exe -X GET "$BaseUrl/file/url/1" `
  -H "Authorization: Bearer $UserToken"
```

文件模块有特殊边界：

| 动作 | 登录要求 |
| --- | --- |
| 上传、删除、列表、存在性检查 | 需要登录 |
| 读取公开文件 | 可匿名 |
| 读取私有文件 | 需要用户端或管理端登录态 |

更多规则见 [文件存储](/modules/file-storage) 和 [权限与安全边界](/guide/security-boundaries)。

## 聊天 WebSocket 示例

聊天室连接不是直接把 Token 放到 WebSocket query。正确流程是：

1. 先用用户 Token 获取一次性 ticket。
2. 再连接 `/ws/chat?ticket=...`。

```powershell
curl.exe -X POST "$BaseUrl/user/chat/ws-ticket" `
  -H "Authorization: Bearer $UserToken"
```

返回的 `data` 是 ticket。连接地址示例：

```text
ws://localhost:9999/api/ws/chat?ticket=<WS_TICKET>
```

浏览器里发送消息的事件结构通常是：

```json
{
  "type": "MESSAGE",
  "data": {
    "content": "大家好",
    "tempId": "temp_1710000000000_demo"
  }
}
```

客户端要处理 `MESSAGE_ACK`、`ERROR`、`RATE_LIMITED`、`MESSAGE_REJECTED`、`KICK_OUT` 等失败态。协议细节见 [WebSocket 协议](/reference/websocket)。

## 管理端示例

### 管理端用户列表

```powershell
curl.exe -X GET "$BaseUrl/admin/user/list?pageNum=1&pageSize=10" `
  -H "Authorization: Bearer $AdminToken"
```

### 管理端 OJ 题目列表

```powershell
curl.exe -X POST "$BaseUrl/admin/oj/problems/list" `
  -H "Authorization: Bearer $AdminToken" `
  -H "Content-Type: application/json" `
  -d '{
    "pageNum": 1,
    "pageSize": 10,
    "keyword": ""
  }'
```

### AI Runtime 健康和配置

```powershell
curl.exe -X GET "$BaseUrl/admin/ai/config/runtime" `
  -H "Authorization: Bearer $AdminToken"
```

```powershell
curl.exe -X GET "$BaseUrl/admin/ai/config/rag-service/health" `
  -H "Authorization: Bearer $AdminToken"
```

RAG 健康接口依赖 `llamaindex-service`。如果 sidecar 没启动，接口可能返回不可用或降级信息，这不是前端问题。

## 前端调用写法

用户端和管理端都封装了 axios 请求工具。页面里不要直接写完整后端域名，应该使用相对路径。

```js
import request from '@/utils/request'

export function getProfile() {
  return request.get('/user/profile')
}
```

原因：

| 做法 | 结果 |
| --- | --- |
| `request.get('/user/profile')` | 浏览器请求 `/api/user/profile`，走 Vite 或 Nginx 代理 |
| `request.get('http://localhost:9999/api/user/profile')` | 本地可用，部署后容易 CORS 或域名错误 |

## 排错速查

| 现象 | 优先检查 |
| --- | --- |
| HTTP `404` | 是否漏了后端上下文 `/api`，或接口 method 不对 |
| 业务 `701`、`702` | Token 是否为空、过期、登录域是否混用 |
| 业务 `703` | 管理端权限或 `@RequireAdmin` |
| 文件 `403` | 文件是否私有，当前是否有登录态 |
| WebSocket 连不上 | 是否先获取 ws-ticket，Origin 是否在白名单 |
| AI 调试失败 | `XIAOU_AI_*` 和 RAG sidecar 是否配置 |
| OJ `system_error` | go-judge 是否可达，编译器是否可用 |

## 新增接口时怎么补示例

如果新增接口面向二次开发或联调，建议同时补：

1. API 路由索引里的 Controller 前缀。
2. 本页的代表性调用示例。
3. 响应体与错误码里的新增错误。
4. 模块页里的业务流程和验证清单。


## 相关文档

| 文档 | 说明 |
| --- | --- |
| [API 路由索引](/reference/api-routes) | 完整接口清单 |
| [响应体与错误码](/reference/response-errors) | 返回格式说明 |
| [鉴权与用户体系](/modules/auth) | Token 获取方式 |
| [本地开发](/guide/local-dev) | 开发环境配置 |
