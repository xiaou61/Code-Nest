# 核心链路教程

本页列出 Code-Nest 的核心业务链路，每条链路都给出从注册/登录到完成操作的完整步骤。你可以按此页顺序做版本发布前的冒烟测试，也可以逐条排查某个模块的端到端问题。

如果你只想快速确认服务是否正常，先看 [常见问题排查](/operations/troubleshooting) 的快速诊断。如果你要完整搭建环境，先看 [本地开发环境搭建](/guide/local-dev)。

## 前置条件

执行以下链路前，确保：

1. MySQL 已启动，`code_nest` 数据库已导入基础表结构（142 张表左右）。
2. Redis 已启动，`redis-cli ping` 返回 `PONG`。
3. 后端已启动，`curl http://localhost:9999/api/actuator/health` 返回 `{"status":"UP"}`。
4. 前端至少启动了一端（用户端 3001 或管理端 3000）。

## 获取 Token

大部分链路需要先登录获取 Token。后端使用 Sa-Token，Header 格式为 `Authorization: Bearer <token>`。

### 用户端登录

```bash
# 步骤1：获取验证码
curl http://localhost:9999/api/captcha/image
# 返回: {"code":200,"data":{"captchaKey":"xxx","captchaImage":"base64..."}}

# 步骤2：登录（开发环境可跳过验证码校验或使用固定测试账号）
curl -X POST http://localhost:9999/api/user/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"test123","captchaKey":"xxx","captchaCode":"xxxx"}'
# 返回: {"code":200,"data":{"token":"xxx","userInfo":{...}}}
```

用户端 Token 使用 `StpUserUtil`（loginType="user"）管理，存储在 Redis db4。

### 管理端登录

```bash
curl -X POST http://localhost:9999/api/admin/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
# 返回: {"code":200,"data":{"token":"xxx"}}
```

管理端 Token 使用 `StpAdminUtil`（loginType="admin"）管理，同样存储在 Redis db4，但与用户端 Token 完全隔离。

> **重要**：用户端 Token 不能调用 `/api/admin/` 接口，管理端 Token 也不能调用用户端业务接口。跨端调用会返回 `{"code":703}`。

## 链路1：OJ 判题

OJ 是 Code-Nest 最核心的业务链路之一，涉及题目浏览、代码提交、判题执行和结果展示。

### 前置依赖

- go-judge 沙箱服务可用（本地 `http://127.0.0.1:5050` 或远端 `oj.judge.go-judge-url` 配置的地址）
- `oj_submission`、`oj_problem`、`oj_contest` 等表存在

### 操作步骤

1. **浏览题目列表**：用户端 `/oj` 页面，调用 `GET /api/user/oj/problem/list` 分页获取题目。
2. **查看题目详情**：用户端 `/oj/problem/:id` 页面，调用 `GET /api/user/oj/problem/:id` 获取题面、示例、限制。
3. **编写代码**：Monaco Editor 加载，选择语言（Java/C/C++/Python3/Go/Node.js）。
4. **自测运行**：调用 `POST /api/user/oj/run`，传入代码 + 自定义输入，go-judge 运行后返回输出。
5. **正式提交**：调用 `POST /api/user/oj/submit`，代码提交到 `oj_submission` 表，状态为 PENDING。
6. **判题执行**：后端异步轮询或推送提交到 go-judge，执行编译 + 多测试用例运行。
7. **结果展示**：状态变为 AC/WA/CE/TLE/MLE/RE，用户端 `/oj/submission/:id` 查看详情。

### 关键验证

```bash
# 获取题目列表（需要用户端 Token）
curl http://localhost:9999/api/user/oj/problem/list \
  -H "Authorization: Bearer <user-token>"
# 期望: {"code":200,"data":{"records":[...],"total":N}}

# 获取题目详情
curl http://localhost:9999/api/user/oj/problem/1 \
  -H "Authorization: Bearer <user-token>"
# 期望: {"code":200,"data":{"id":1,"title":"...","description":"...",...}}
```

### 常见问题

| 问题 | 原因 | 解决 |
| --- | --- | --- |
| 提交一直 JUDGING | go-judge 不可达 | 检查 `oj.judge.go-judge-url` 配置和 go-judge 容器状态 |
| 编译错误 CE | 编译器未安装 | `docker exec go-judge javac -version` 确认 |
| 运行超时 TLE | 题目 time_limit 过小或算法超时 | 检查题目 `time_limit` 设置 |
| 内存溢出 MLE | memory_limit 过小 | 检查题目 `memory_limit` 设置 |

更多排查见 [常见问题排查 - OJ 判题问题](/operations/troubleshooting#oj-判题问题)。

## 链路2：模拟面试

模拟面试是最复杂的 AI 交互链路，涉及 Graph Runner、结构化输出、RAG 和多轮对话。

### 前置依赖

- AI Provider 已配置（`XIAOU_AI_BASE_URL` 和 `XIAOU_AI_API_KEY`）
- AI 功能已开启（`xiaou.ai.enabled=true`）

### 操作步骤

1. **创建会话**：用户端 `/mock-interview` 页面，选择面试类型和岗位，调用 `POST /api/user/mock-interview/session`。
2. **AI 生成首题**：后端调用 AI Graph Runner，根据面试类型和岗位生成第一个面试问题。
3. **用户作答**：在聊天界面输入回答，调用 `POST /api/user/mock-interview/answer`。
4. **AI 追问/反馈**：AI 评估回答质量，生成追问或反馈，可能调用 RAG 检索相关知识。
5. **提前结束**：用户可随时调用 `POST /api/user/mock-interview/end` 提前结束会话。
6. **查看报告**：会话结束后，AI 生成总结报告，调用 `GET /api/user/mock-interview/report/:id`。
7. **转学习资产**：报告可转化为学习资产，进入 [链路3：学习资产](#链路3学习资产)。

### 关键验证

```bash
# 创建模拟面试会话
curl -X POST http://localhost:9999/api/user/mock-interview/session \
  -H "Authorization: Bearer <user-token>" \
  -H "Content-Type: application/json" \
  -d '{"interviewType":"TECHNICAL","position":"Java后端开发"}'
# 期望: {"code":200,"data":{"sessionId":"xxx",...}}

# 查看会话报告
curl http://localhost:9999/api/user/mock-interview/report/xxx \
  -H "Authorization: Bearer <user-token>"
# 期望: {"code":200,"data":{"summary":"...","score":N,...}}
```

### AI 降级处理

如果 AI 不可用（未配置或超时），模拟面试接口会返回降级响应，不会阻塞其他业务。降级时：

- 创建会话可成功，但 AI 生成问题会返回默认提示。
- 作答反馈为空或提示"AI 服务暂不可用"。
- 报告生成失败但不影响历史记录。

## 链路3：学习资产

学习资产是从模拟面试、博客、CodePen 等来源转化而来的结构化学习内容。

### 操作步骤

1. **生成候选**：从模拟面试报告或其他来源，调用 `POST /api/user/learning-asset/candidate/generate` 生成资产候选。
2. **查看候选**：用户端 `/learning-asset/candidates` 页面，查看待发布的资产列表。
3. **用户发布**：编辑候选内容后，调用 `POST /api/user/learning-asset/publish/:id` 发布。
4. **管理端审核**：管理端 `/learning-asset/review` 页面，调用 `GET /api/admin/learning-asset/pending` 查看待审核列表。
5. **审核通过**：调用 `POST /api/admin/learning-asset/approve/:id`，资产状态变为 PUBLISHED。
6. **审核驳回**：调用 `POST /api/admin/learning-asset/reject/:id`，附带驳回原因，资产回到 DRAFT。
7. **版本合并**：如果有新版本，调用 `POST /api/admin/learning-asset/merge/:id` 合并。

### 关键验证

```bash
# 查看资产候选列表
curl http://localhost:9999/api/user/learning-asset/candidate/list \
  -H "Authorization: Bearer <user-token>"
# 期望: {"code":200,"data":{"records":[...]}}

# 管理端查看待审核
curl http://localhost:9999/api/admin/learning-asset/pending \
  -H "Authorization: Bearer <admin-token>"
# 期望: {"code":200,"data":{"records":[...]}}
```

### 状态流转

```text
CANDIDATE → DRAFT → PENDING_REVIEW → PUBLISHED
                     ↓
                   REJECTED → DRAFT（可重新提交）
```

## 链路4：聊天室

聊天室是 Code-Nest 最容易出问题的链路，因为涉及 ws-ticket + WebSocket + ACK 三步。

### 前置依赖

- Redis db3 可用（存储 ws-ticket）
- WebSocket 连接不被 Nginx/防火墙阻断

### 操作步骤

1. **获取 ws-ticket**：

```bash
curl -X GET http://localhost:9999/api/user/chat/ws-ticket \
  -H "Authorization: Bearer <user-token>"
# 返回: {"code":200,"data":{"ticket":"xxx"}}
```

ws-ticket 为 32 字节 SecureRandom，Base64 URL-safe 编码，Redis TTL 60 秒，一次性使用。

2. **WebSocket 连接**：

```text
ws://localhost:9999/api/ws/chat?ticket=<ticket>
```

连接成功后收到 `CONNECT` 帧。

3. **发送消息**：

```json
{
  "type": "MESSAGE",
  "content": "Hello!",
  "roomId": "general"
}
```

4. **收到 ACK**：

```json
{
  "type": "MESSAGE_ACK",
  "messageId": "xxx",
  "timestamp": 1713945600000
}
```

5. **图片消息**：上传图片获取 URL，消息中 `type=IMAGE`，`content` 为图片 URL。

6. **撤回消息**：`type=RECALL`，`messageId` 为要撤回的消息 ID。

7. **公告/禁言/踢出**：管理员操作，分别对应 `type=ANNOUNCEMENT`、`MUTE`、`KICK`。

### 关键验证

| 步骤 | 验证方法 | 期望 |
| --- | --- | --- |
| ws-ticket 获取 | curl GET + Token | 返回 ticket 字符串 |
| WebSocket 连接 | ws client 连接 | 收到 CONNECT |
| 发送文本 | 发送 MESSAGE | 收到 MESSAGE_ACK |
| 发送图片 | 先上传再发 IMAGE | 收到 MESSAGE_ACK |
| 撤回 | 发送 RECALL | 消息标记已撤回 |
| ticket 复用 | 同一 ticket 连两次 | 第二次被拒绝 |

### Nginx 配置（生产环境）

生产环境需要在 Nginx 添加 WebSocket Upgrade 代理：

```nginx
location /api/ws/ {
    proxy_pass http://127.0.0.1:9999;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
}
```

更多排查见 [常见问题排查 - WebSocket 问题](/operations/troubleshooting#聊天室-websocket-问题)。

## 链路5：积分与抽奖

积分系统贯穿多个业务模块，签到、AI 使用、内容创作都可能涉及积分变动。

### 操作步骤

1. **签到**：用户端 `/points` 页面，调用 `POST /api/user/points/sign-in`，获取签到积分。
2. **后台发放**：管理端调用 `POST /api/admin/points/grant`，给指定用户发放积分。
3. **积分流水**：调用 `GET /api/user/points/detail` 查看收支明细（EARN/SPEND）。
4. **抽奖扣分**：用户端 `/lottery` 页面，调用 `POST /api/user/lottery/draw`，自动扣减积分。
5. **抽奖记录**：调用 `GET /api/user/lottery/records` 查看中奖记录。

### 关键验证

```bash
# 签到
curl -X POST http://localhost:9999/api/user/points/sign-in \
  -H "Authorization: Bearer <user-token>"
# 期望: {"code":200,"data":{"earned":10,...}}

# 查看积分余额
curl http://localhost:9999/api/user/points/balance \
  -H "Authorization: Bearer <user-token>"
# 期望: {"code":200,"data":{"balance":N}}
```

### 积分不一致排查

```sql
-- 检查积分余额是否与流水一致
SELECT u.id, u.nickname,
  (SELECT COALESCE(SUM(CASE WHEN type='EARN' THEN amount ELSE -amount END), 0)
   FROM user_points_detail WHERE user_id = u.id) AS calculated,
  b.balance AS stored
FROM user_info u
LEFT JOIN user_points_balance b ON u.id = b.user_id
HAVING calculated != stored;
```

## 链路6：内容创作

内容创作涵盖博客、社区、动态、CodePen 和简历等多个模块。

### 博客

1. 用户端 `/blog/write` 页面，编写 Markdown 内容。
2. 调用 `POST /api/user/blog/publish` 发布。
3. 用户端 `/blog` 列表页可见，管理端 `/blog/review` 可审核。

### 社区

1. 用户端 `/community/write` 页面，编写帖子。
2. 调用 `POST /api/user/community/post` 发布。
3. 管理端 `/community/review` 可审核。

### 动态

1. 用户端 `/moment/write` 页面，编写动态。
2. 调用 `POST /api/user/moment/publish` 发布。
3. 动态出现在个人主页和信息流。

### CodePen

1. 用户端 `/codepen/new` 页面，编写 HTML/CSS/JS 代码。
2. 调用 `POST /api/user/codepen/save` 保存。
3. 实时预览，支持分享。

### 简历

1. 用户端 `/resume/edit` 页面，填写简历信息。
2. 调用 `POST /api/user/resume/save` 保存。
3. 管理端简历模板管理。

## 链路7：赛事

赛事是 OJ 的扩展功能，支持限时比赛和排名。

### 操作步骤

1. **浏览赛事**：用户端 `/oj/contest` 页面，查看进行中和即将开始的赛事。
2. **报名**：调用 `POST /api/user/oj/contest/register/:id` 报名参赛。
3. **进入比赛**：赛事开始后，进入赛事题目列表。
4. **提交代码**：与普通 OJ 提交相同，但受赛事规则约束（如 ICPC/IOI 评分规则）。
5. **查看排名**：赛事期间实时排名，调用 `GET /api/user/oj/contest/rank/:id`。
6. **赛后总结**：赛事结束后查看最终排名和个人表现。

### 赛事评分规则

| 规则 | 说明 |
| --- | --- |
| ICPC | 按通过题数排名，相同题数按罚时排名 |
| IOI | 按总分排名，每题按测试点得分汇总 |

## 链路8：通知回流

通知系统是连接各个模块的胶水层，几乎每个写操作都可能触发通知。

### 通知来源

| 触发场景 | 通知类型 | 目标用户 |
| --- | --- | --- |
| 模拟面试报告生成 | 面试完成 | 面试用户 |
| 学习资产审核结果 | 审核通知 | 资产作者 |
| 赛事开始/结束 | 赛事通知 | 报名用户 |
| 评论/点赞 | 互动通知 | 内容作者 |
| 系统公告 | 系统通知 | 全体用户 |
| 积分变动 | 积分通知 | 对应用户 |

### 验证步骤

```bash
# 查看未读通知数
curl http://localhost:9999/api/user/notification/unread-count \
  -H "Authorization: Bearer <user-token>"
# 期望: {"code":200,"data":3}

# 查看通知列表
curl http://localhost:9999/api/user/notification/list \
  -H "Authorization: Bearer <user-token>"
# 期望: {"code":200,"data":{"records":[...]}}

# 标记已读
curl -X POST http://localhost:9999/api/user/notification/read/xxx \
  -H "Authorization: Bearer <user-token>"
# 期望: {"code":200}
```

## 管理端核心操作

管理端的核心操作主要围绕审核、配置和运营展开。

### AI 配置管理

管理端 `/system/ai-config` 页面是 AI Runtime 的管理入口：

1. **查看 Runtime 面板**：当前 AI Provider、模型、超时、重试配置（脱敏展示）。
2. **Prompt 调试**：选择场景，输入变量，执行 Prompt，查看结构化输出。
3. **RAG 调试**：输入查询，查看召回内容和相似度。
4. **Schema Catalog**：查看所有 AI 场景的结构化输出 Schema。
5. **回归测试**：运行 AI 回归测试集合，查看结果。

### 用户管理

1. **用户列表**：`GET /api/admin/user/list`，支持搜索和筛选。
2. **封禁/解封**：`POST /api/admin/user/ban/:id` / `POST /api/admin/user/unban/:id`。
3. **禁言**：`POST /api/admin/chat/mute/:userId`。

### 内容审核

1. **博客审核**：`GET /api/admin/blog/pending` → `POST /api/admin/blog/approve/:id` / `reject/:id`。
2. **社区审核**：类似博客审核流程。
3. **学习资产审核**：见 [链路3：学习资产](#链路3学习资产)。

## 错误码速查

在验证链路时，以下错误码表示鉴权/权限问题，不是业务逻辑 bug：

| 错误码 | 含义 | 说明 |
| --- | --- | --- |
| 701 | Token 无效或缺失 | 检查 Header `Authorization: Bearer <token>` |
| 702 | Token 已过期 | 重新登录，默认 7 天有效期 |
| 703 | 权限不足 | 用户端 Token 不能调管理端接口 |
| 704 | 账号被封禁 | 联系管理员解封 |
| 705 | 登录失败 | 用户名或密码错误 |

文件上传相关错误码：

| 错误码 | 含义 |
| --- | --- |
| 801 | 文件为空 |
| 802 | 文件过大（超过 100MB） |
| 803 | 文件类型不支持 |
| 804 | 存储空间不足 |
| 805 | 上传路径不合法 |

## 验证记录

每条链路验证后，建议按以下格式记录到 [验证记录与已知问题](/manuals/verified-scenarios)：

```text
验证范围：
日期：
版本：
已执行：
通过项：
未验证：
依赖状态：
```

如果这次改动会影响通知、排行、积分或日志回流，也建议先对照 [事件、通知与回流索引](/reference/event-backflow-index) 确认副作用。

更多排查流程见 [常见问题排查](/operations/troubleshooting) 和 [问题定位流程](/operations/diagnosis-flow)。


## 相关文档

| 文档 | 说明 |
| --- | --- |
| [端到端业务链路图](/manuals/business-flow-map) | 业务链路图 |
| [用户端操作手册](/manuals/user-operations) | 用户端操作 |
| [管理端操作手册](/manuals/admin-operations) | 管理端操作 |
| [验证记录与已知问题](/manuals/verified-scenarios) | 验证记录 |
