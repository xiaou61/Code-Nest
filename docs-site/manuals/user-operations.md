# 用户端操作手册

本页记录用户端各模块的操作流程和对应的后端接口。用户端接口前缀为 `/api/user/`，需要用户端 Token（`Authorization: Bearer <user-token>`）。

如果你需要管理端操作，看 [管理端操作手册](/manuals/admin-operations)。如果你在排查问题，看 [常见问题排查](/operations/troubleshooting)。

## 登录与注册

### 注册

```bash
# 获取验证码
curl http://localhost:9999/api/captcha/image
# 返回: {"code":200,"data":{"captchaKey":"xxx","captchaImage":"base64..."}}

# 注册
curl -X POST http://localhost:9999/api/user/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"newuser","password":"pass123","captchaKey":"xxx","captchaCode":"1234"}'
# 期望: {"code":200}
```

### 登录

```bash
curl -X POST http://localhost:9999/api/user/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"test123"}'
# 返回: {"code":200,"data":{"token":"xxx","userInfo":{...}}}
```

用户端使用 `StpUserUtil`（loginType="user"），Token 存储在 Redis db4。

> **提示**：开发环境验证码明文会出现在后端日志中，可用关键字 `生成验证码成功` 查找。

## 个人中心

### 查看资料

```bash
curl http://localhost:9999/api/user/profile \
  -H "Authorization: Bearer <user-token>"
# 返回: {"code":200,"data":{"id":123,"username":"test","nickname":"...","avatar":"...",...}}
```

### 修改资料

```bash
curl -X PUT http://localhost:9999/api/user/profile \
  -H "Authorization: Bearer <user-token>" \
  -H "Content-Type: application/json" \
  -d '{"nickname":"新昵称","bio":"个人简介"}'
# 期望: {"code":200}
```

### 修改头像

```bash
# 上传头像文件
curl -X POST http://localhost:9999/api/file \
  -H "Authorization: Bearer <user-token>" \
  -F "file=@avatar.png"
# 返回: {"code":200,"data":{"url":"/api/files/xxx.png"}}

# 更新头像 URL
curl -X PUT http://localhost:9999/api/user/profile \
  -H "Authorization: Bearer <user-token>" \
  -H "Content-Type: application/json" \
  -d '{"avatar":"/api/files/xxx.png"}'
```

## 面试题学习

### 浏览题单

```bash
curl "http://localhost:9999/api/user/interview/question-sets?page=1&size=20" \
  -H "Authorization: Bearer <user-token>"
# 返回: {"code":200,"data":{"records":[...],"total":N}}
```

### 标记掌握度

```bash
curl -X POST http://localhost:9999/api/user/interview/mastery/mark \
  -H "Authorization: Bearer <user-token>" \
  -H "Content-Type: application/json" \
  -d '{"questionId":456,"level":"FAMILIAR"}'
# 期望: {"code":200}
```

### 收藏题目

```bash
curl -X POST http://localhost:9999/api/user/interview/favorite \
  -H "Authorization: Bearer <user-token>" \
  -H "Content-Type: application/json" \
  -d '{"questionId":456}'
# 期望: {"code":200}
```

## OJ 刷题

### 浏览题目

```bash
curl "http://localhost:9999/api/user/oj/problem/list?page=1&size=20" \
  -H "Authorization: Bearer <user-token>"
# 返回: {"code":200,"data":{"records":[...],"total":N}}
```

### 查看题目详情

```bash
curl http://localhost:9999/api/user/oj/problem/1 \
  -H "Authorization: Bearer <user-token>"
# 返回: {"code":200,"data":{"id":1,"title":"...","description":"...",...}}
```

### 提交代码

```bash
curl -X POST http://localhost:9999/api/user/oj/submit \
  -H "Authorization: Bearer <user-token>" \
  -H "Content-Type: application/json" \
  -d '{"problemId":1,"language":"JAVA","code":"public class Solution {...}"}'
# 返回: {"code":200,"data":{"submissionId":"xxx"}}
```

提交后状态为 PENDING，异步判题完成后变为 AC/WA/CE 等。

### 自测运行

```bash
curl -X POST http://localhost:9999/api/user/oj/run \
  -H "Authorization: Bearer <user-token>" \
  -H "Content-Type: application/json" \
  -d '{"language":"JAVA","code":"public class Main {...}","input":"1 2"}'
# 返回: {"code":200,"data":{"output":"3","exitCode":0}}
```

### 查看提交记录

```bash
curl http://localhost:9999/api/user/oj/submission/xxx \
  -H "Authorization: Bearer <user-token>"
# 返回: {"code":200,"data":{"status":"ACCEPTED","timeUsed":100,...}}
```

### 赛事报名

```bash
# 报名参赛
curl -X POST http://localhost:9999/api/user/oj/contest/register/1 \
  -H "Authorization: Bearer <user-token>"
# 期望: {"code":200}

# 查看赛事排名
curl http://localhost:9999/api/user/oj/contest/rank/1 \
  -H "Authorization: Bearer <user-token>"
```

## 模拟面试

### 创建会话

```bash
curl -X POST http://localhost:9999/api/user/mock-interview/session \
  -H "Authorization: Bearer <user-token>" \
  -H "Content-Type: application/json" \
  -d '{"interviewType":"TECHNICAL","position":"Java后端开发","difficulty":"MEDIUM"}'
# 返回: {"code":200,"data":{"sessionId":"xxx"}}
```

### 作答

```bash
curl -X POST http://localhost:9999/api/user/mock-interview/answer \
  -H "Authorization: Bearer <user-token>" \
  -H "Content-Type: application/json" \
  -d '{"sessionId":"xxx","questionId":"q1","answer":"HashMap基于数组+链表+红黑树..."}'
# 返回: {"code":200,"data":{"feedback":"...","nextQuestion":{...}}}
```

AI 会评估答案质量并决定是否追问。

### 查看报告

```bash
curl http://localhost:9999/api/user/mock-interview/report/xxx \
  -H "Authorization: Bearer <user-token>"
# 返回: {"code":200,"data":{"summary":"...","score":85,"suggestions":[...]}}
```

## 聊天室

### 获取 ws-ticket

```bash
curl http://localhost:9999/api/user/chat/ws-ticket \
  -H "Authorization: Bearer <user-token>"
# 返回: {"code":200,"data":{"ticket":"xxx"}}
```

ws-ticket 一次性使用，60 秒过期，32 字节 SecureRandom Base64 URL-safe 编码。

### 连接 WebSocket

```text
ws://localhost:9999/api/ws/chat?ticket=<ticket>
```

连接成功后收到 `CONNECT` 帧。

### 消息协议

| 操作 | type 字段 | 方向 | 说明 |
| --- | --- | --- | --- |
| 发送文本 | `MESSAGE` | C→S→C | content 为文本内容 |
| 发送图片 | `IMAGE` | C→S→C | content 为图片 URL |
| 消息确认 | `MESSAGE_ACK` | S→C | 包含 messageId 和 timestamp |
| 撤回 | `RECALL` | C→S→C | 携带 messageId |
| 输入中 | `TYPING` | C→S→C | 提示其他用户正在输入 |
| 系统消息 | `SYSTEM` | S→C | 公告、禁言通知等 |
| 错误 | `ERROR` | S→C | 包含 code 和 message |

### 限流

连续快速发送消息会触发限流，收到 `ERROR` 事件：

```json
{
  "type": "ERROR",
  "code": "RATE_LIMITED",
  "message": "发送过于频繁，请稍后再试"
}
```

此时前端应将乐观消息标记为失败态。

## 积分系统

### 签到

```bash
curl -X POST http://localhost:9999/api/user/points/sign-in \
  -H "Authorization: Bearer <user-token>"
# 返回: {"code":200,"data":{"earned":10,"total":110}}
```

> **已知问题**：签到按钮文案可能不更新，因为 `todayCheckedIn` 和 `hasCheckedToday` 字段不一致。

### 查看积分余额

```bash
curl http://localhost:9999/api/user/points/balance \
  -H "Authorization: Bearer <user-token>"
# 返回: {"code":200,"data":{"balance":110}}
```

### 抽奖

```bash
curl -X POST http://localhost:9999/api/user/lottery/draw \
  -H "Authorization: Bearer <user-token>"
# 返回: {"code":200,"data":{"prize":"xxx","pointsCost":50}}
```

## 通知中心

```bash
# 未读数量
curl http://localhost:9999/api/user/notification/unread-count \
  -H "Authorization: Bearer <user-token>"

# 通知列表
curl "http://localhost:9999/api/user/notification/list?page=1&size=20" \
  -H "Authorization: Bearer <user-token>"

# 标记已读
curl -X POST http://localhost:9999/api/user/notification/read/xxx \
  -H "Authorization: Bearer <user-token>"
```

## 内容创作

### 社区发帖

```bash
curl -X POST http://localhost:9999/api/user/community/post \
  -H "Authorization: Bearer <user-token>" \
  -H "Content-Type: application/json" \
  -d '{"title":"Spring Boot 3 新特性","content":"...","categoryId":1,"tagIds":[1,2]}'
```

### 博客发布

```bash
curl -X POST http://localhost:9999/api/user/blog/publish \
  -H "Authorization: Bearer <user-token>" \
  -H "Content-Type: application/json" \
  -d '{"title":"我的第一篇博客","contentMarkdown":"# Hello World\n...","categoryId":1}'
```

### CodePen 保存

```bash
curl -X POST http://localhost:9999/api/user/codepen/save \
  -H "Authorization: Bearer <user-token>" \
  -H "Content-Type: application/json" \
  -d '{"title":"CSS 动画","html":"<div>...</div>","css":"div { animation: ... }","js":"","isPublic":true}'
```

### 动态发布

```bash
curl -X POST http://localhost:9999/api/user/moment/publish \
  -H "Authorization: Bearer <user-token>" \
  -H "Content-Type: application/json" \
  -d '{"content":"今天学了 Java 集合框架","imageUrls":["/api/files/xxx.png"]}'
```

## 验证口径

1. 直达页面只证明路由和页面渲染正常。
2. 带参数详情页优先从列表点击进入，避免本地数据 ID 不一致。
3. 写操作以"接口返回成功、页面回流、数据库或后台可见"三者同时成立为完整闭环。
4. 依赖外部服务的功能需要额外确认 sidecar 或 go-judge 可用。
5. 内容类操作必须同时验证：用户端回流 + 管理端可见 + 通知到达。

## 安全注意

| 场景 | 风险 | 防护 |
| --- | --- | --- |
| userId 伪造 | 用户篡改请求中的 userId | 后端从 Sa-Token 会话取 userId，不信任前端传参 |
| 内容 XSS | Markdown/HTML 注入 | DOMPurify 净化 + 管理端渲染安全检查 |
| 文件类型绕持 | 上传 .exe/.sh | 文件类型白名单 + 后缀校验 |
| ws-ticket 复用 | 同一 ticket 连两次 | Redis 一次性消费，二次连接被拒绝 |
| Token 跨端 | 用户端 Token 调管理端 | loginType 隔离，返回 703 |

## 错误码速查

| 错误码 | 含义 | 说明 |
| --- | --- | --- |
| 701 | Token 无效或缺失 | 检查 Header 格式 |
| 702 | Token 已过期 | 重新登录 |
| 703 | 权限不足 | 跨端调用 |
| 704 | 账号被封禁 | 联系管理员 |
| 705 | 登录失败 | 用户名或密码错误 |
| 801 | 文件为空 | 检查上传文件 |
| 802 | 文件过大 | 超过 100MB |
| 803 | 文件类型不支持 | 检查白名单 |
| 805 | 上传路径不合法 | 检查文件名 |

更多排查流程见 [常见问题排查](/operations/troubleshooting) 和 [问题定位流程](/operations/diagnosis-flow)。