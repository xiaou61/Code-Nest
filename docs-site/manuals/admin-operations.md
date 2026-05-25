# 管理端操作手册

本页记录管理端各模块的操作流程和对应的后端接口。所有接口前缀为 `/api/admin/`，需要管理端 Token（`Authorization: Bearer <admin-token>`）。

如果你需要用户端操作，看 [用户端操作手册](/manuals/user-operations)。如果你在排查问题，看 [常见问题排查](/operations/troubleshooting)。

## 登录

```bash
# 管理端登录
curl -X POST http://localhost:9999/api/admin/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
# 返回: {"code":200,"data":{"token":"xxx"}}
```

管理端使用 `StpAdminUtil`（loginType="admin"），Token 存储在 Redis db4，与用户端 Token 完全隔离。

Token Header 格式：`Authorization: Bearer <token>`

## 仪表盘

管理端首页仪表盘展示系统概览数据。

| 数据 | 接口 | 说明 |
| --- | --- | --- |
| 用户统计 | `GET /api/admin/dashboard/user-stats` | 注册数、活跃数、今日新增 |
| 内容统计 | `GET /api/admin/dashboard/content-stats` | 博客/社区/动态数量 |
| OJ 统计 | `GET /api/admin/dashboard/oj-stats` | 提交数、通过率 |
| 积分统计 | `GET /api/admin/dashboard/points-stats` | 发放总额、消耗总额 |

## 用户管理

### 用户列表

```bash
curl "http://localhost:9999/api/admin/user/list?page=1&size=20" \
  -H "Authorization: Bearer <admin-token>"
# 返回: {"code":200,"data":{"records":[...],"total":N}}
```

支持搜索和筛选参数：

| 参数 | 说明 |
| --- | --- |
| `keyword` | 搜索用户名或昵称 |
| `status` | 按状态筛选（0=正常，1=禁用） |
| `page` / `size` | 分页参数 |

### 封禁/解封

```bash
# 封禁用户
curl -X POST http://localhost:9999/api/admin/user/ban/123 \
  -H "Authorization: Bearer <admin-token>"
# 期望: {"code":200}

# 解封用户
curl -X POST http://localhost:9999/api/admin/user/unban/123 \
  -H "Authorization: Bearer <admin-token>"
# 期望: {"code":200}
```

封禁后用户登录返回 `{"code":704}`，所有业务接口也会被拦截。

## 聊天室管理

### 禁言

```bash
# 禁言用户（默认 30 分钟）
curl -X POST http://localhost:9999/api/admin/chat/mute/123 \
  -H "Authorization: Bearer <admin-token>" \
  -H "Content-Type: application/json" \
  -d '{"duration":30}'
# 期望: {"code":200}
```

禁言期间用户发送消息会被拒绝，但可以查看聊天内容。

### 踢出

```bash
# 踢出用户
curl -X POST http://localhost:9999/api/admin/chat/kick/123 \
  -H "Authorization: Bearer <admin-token>"
# 期望: {"code":200}
```

踢出后用户 WebSocket 连接会被断开，需要重新获取 ws-ticket 连接。

### 公告

```bash
# 发布聊天室公告
curl -X POST http://localhost:9999/api/admin/chat/announce \
  -H "Authorization: Bearer <admin-token>" \
  -H "Content-Type: application/json" \
  -d '{"content":"系统维护通知：今晚 22:00-23:00 进行例行维护"}'
# 期望: {"code":200}
```

公告会通过 WebSocket 推送到所有在线用户。

## 内容审核

### 博客审核

```bash
# 查看待审核博客列表
curl "http://localhost:9999/api/admin/blog/pending?page=1&size=20" \
  -H "Authorization: Bearer <admin-token>"

# 审核通过
curl -X POST http://localhost:9999/api/admin/blog/approve/456 \
  -H "Authorization: Bearer <admin-token>"

# 审核驳回
curl -X POST http://localhost:9999/api/admin/blog/reject/456 \
  -H "Authorization: Bearer <admin-token>" \
  -H "Content-Type: application/json" \
  -d '{"reason":"内容不符合规范"}'
```

### 社区帖子审核

```bash
# 查看待审核帖子
curl "http://localhost:9999/api/admin/community/pending?page=1&size=20" \
  -H "Authorization: Bearer <admin-token>"

# 审核通过
curl -X POST http://localhost:9999/api/admin/community/approve/789 \
  -H "Authorization: Bearer <admin-token>"

# 审核驳回
curl -X POST http://localhost:9999/api/admin/community/reject/789 \
  -H "Authorization: Bearer <admin-token>" \
  -H "Content-Type: application/json" \
  -d '{"reason":"内容违规"}'
```

### 学习资产审核

学习资产审核流程更复杂，涉及候选、发布、版本合并：

```bash
# 查看待审核学习资产
curl "http://localhost:9999/api/admin/learning-asset/pending?page=1&size=20" \
  -H "Authorization: Bearer <admin-token>"

# 审核通过
curl -X POST http://localhost:9999/api/admin/learning-asset/approve/101 \
  -H "Authorization: Bearer <admin-token>"

# 审核驳回
curl -X POST http://localhost:9999/api/admin/learning-asset/reject/101 \
  -H "Authorization: Bearer <admin-token>" \
  -H "Content-Type: application/json" \
  -d '{"reason":"内容质量不达标"}'

# 版本合并
curl -X POST http://localhost:9999/api/admin/learning-asset/merge/101 \
  -H "Authorization: Bearer <admin-token>"
```

学习资产状态流转：`CANDIDATE → DRAFT → PENDING_REVIEW → PUBLISHED` 或 `REJECTED → DRAFT`

## AI 配置管理

管理端 `/system/ai-config` 页面是 AI Runtime 的管理入口。

### 查看 Runtime 面板

```bash
# 获取 AI 配置（脱敏展示）
curl http://localhost:9999/api/admin/ai/config/runtime \
  -H "Authorization: Bearer <admin-token>"
# 返回: {"code":200,"data":{"provider":"openai-compatible","chatModel":"gpt-4o",...}}
# 注意：API Key 会被脱敏为 sk-****xx
```

### Schema Catalog

```bash
# 查看所有 AI 场景的结构化输出 Schema
curl http://localhost:9999/api/admin/ai/config/schema-catalog \
  -H "Authorization: Bearer <admin-token>"
# 返回: {"code":200,"data":[{"scene":"mock-interview","schema":{...}},...]}
```

### Prompt 调试

```bash
# 执行 Prompt 调试
curl -X POST http://localhost:9999/api/admin/ai/config/prompt/debug \
  -H "Authorization: Bearer <admin-token>" \
  -H "Content-Type: application/json" \
  -d '{"scene":"mock-interview","variables":{"position":"Java后端开发","type":"TECHNICAL"}}'
# 返回: {"code":200,"data":{"output":{...},"usage":{"promptTokens":N,"completionTokens":N}}}
```

### RAG 调试

```bash
# RAG 检索调试
curl -X POST http://localhost:9999/api/admin/ai/config/rag/debug \
  -H "Authorization: Bearer <admin-token>" \
  -H "Content-Type: application/json" \
  -d '{"query":"Java集合框架","topK":5}'
# 返回: {"code":200,"data":{"chunks":[{"content":"...","score":0.85},...]}}
```

### AI 回归测试

```bash
# 触发 AI 回归测试
curl -X POST http://localhost:9999/api/admin/ai/config/regression/run \
  -H "Authorization: Bearer <admin-token>"
# 返回: {"code":200,"data":{"totalCases":N,"passed":N,"failed":N}}

# 命令行运行 AI 回归
mvn -pl xiaou-ai -am "-Dtest=AiSceneRegressionEvalTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

## 积分管理

### 积分发放

```bash
# 给用户发放积分
curl -X POST http://localhost:9999/api/admin/points/grant \
  -H "Authorization: Bearer <admin-token>" \
  -H "Content-Type: application/json" \
  -d '{"userId":123,"amount":100,"reason":"活动奖励"}'
# 期望: {"code":200}
```

### 积分统计

```bash
# 查看积分系统概览
curl http://localhost:9999/api/admin/points/stats \
  -H "Authorization: Bearer <admin-token>"
# 返回: {"code":200,"data":{"totalGranted":N,"totalConsumed":N,"activeUsers":N}}
```

## 操作日志

管理端所有写操作都会记录到 `sys_operation_log` 表。

```bash
# 查看操作日志
curl "http://localhost:9999/api/admin/log/list?page=1&size=20" \
  -H "Authorization: Bearer <admin-token>"
# 返回: {"code":200,"data":{"records":[...],"total":N}}
```

直接查数据库：

```sql
SELECT * FROM sys_operation_log
WHERE created_time > DATE_SUB(NOW(), INTERVAL 24 HOUR)
ORDER BY created_time DESC
LIMIT 50;
```

## OJ 管理

### 题目管理

```bash
# 创建题目
curl -X POST http://localhost:9999/api/admin/oj/problem \
  -H "Authorization: Bearer <admin-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "title":"两数之和",
    "description":"给定一个整数数组...",
    "difficulty":"EASY",
    "timeLimit":2000,
    "memoryLimit":256,
    "testCases":[...]
  }'

# 查看题目列表
curl "http://localhost:9999/api/admin/oj/problem/list?page=1&size=20" \
  -H "Authorization: Bearer <admin-token>"
```

### 赛事管理

```bash
# 创建赛事
curl -X POST http://localhost:9999/api/admin/oj/contest \
  -H "Authorization: Bearer <admin-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "title":"周赛第 100 场",
    "ruleType":"ICPC",
    "startTime":"2026-05-01T20:00:00",
    "endTime":"2026-05-01T22:00:00",
    "problemIds":[1,2,3,4]
  }'

# 查看赛事列表
curl "http://localhost:9999/api/admin/oj/contest/list?page=1&size=20" \
  -H "Authorization: Bearer <admin-token>"
```

### 提交管理

```bash
# 查看提交记录
curl "http://localhost:9999/api/admin/oj/submission/list?page=1&size=20" \
  -H "Authorization: Bearer <admin-token>"

# 重置卡住的提交（谨慎操作）
# 直接用 SQL：
# UPDATE oj_submission SET status = 0 WHERE id = ?;
```

## 系统配置

### 文件存储配置

文件存储类型通过 `xiaou.file.storage-type` 控制：

| 类型 | 说明 | 配置项 |
| --- | --- | --- |
| `local` | 本地文件存储（默认） | `xiaou.file.local.upload-dir`、`xiaou.file.local.base-url` |
| `minio` | MinIO 对象存储 | `xiaou.file.minio.endpoint`、`xiaou.file.minio.bucket` 等 |
| `s3` | S3 兼容存储 | 类似 MinIO 配置 |

### CORS 配置

```bash
# 当前 CORS 配置来自环境变量 XIAOU_CORS_ALLOWED_ORIGIN_PATTERNS
# 默认值: http://localhost:3000,http://localhost:3001,http://localhost:5173,http://localhost:5175
# 注意：5173 为旧版 Vite 默认端口，仍保留在代码 @Value 回退值中；5175 为文档站开发端口
```

生产环境必须修改为真实前端域名，不能保留 localhost。详见 [环境变量总表 - CORS 配置](/operations/env-vars#cors-配置)。

### Actuator 端点

| 端点 | 地址 | 用途 |
| --- | --- | --- |
| Health | `http://localhost:9999/api/actuator/health` | 健康状态 |
| Info | `http://localhost:9999/api/actuator/info` | 应用信息 |
| Prometheus | `http://localhost:9999/api/actuator/prometheus` | Prometheus 指标 |

## 错误码速查

管理端接口常见错误码：

| 错误码 | 含义 | 常见原因 |
| --- | --- | --- |
| 701 | Token 无效或缺失 | 未带 Header、Token 格式错误 |
| 702 | Token 已过期 | 超过 7 天有效期 |
| 703 | 权限不足 | 用户端 Token 调管理端接口 |
| 704 | 账号被封禁 | 管理员账号被禁用 |
| 705 | 登录失败 | 用户名或密码错误 |
| 801 | 文件为空 | 上传文件为空 |
| 802 | 文件过大 | 超过 100MB 限制 |
| 803 | 文件类型不支持 | 文件扩展名不在白名单 |

## 注意事项

| 事项 | 说明 |
| --- | --- |
| 管理端 Token 与用户端 Token 完全隔离 | 使用 `StpAdminUtil`（loginType="admin"），不能用用户端 Token |
| 所有管理端接口以 `/api/admin/` 开头 | 被 `@RequireAdmin` 注解保护，AOP 拦截 |
| 写操作记录到操作日志 | `sys_operation_log` 表可查 |
| AI 配置页脱敏展示 | API Key 只显示 `sk-****xx`，不会泄露完整 Key |
| 文件上传限制 100MB | `spring.servlet.multipart.max-file-size=100MB` |
| CORS 白名单要填前端域名 | 不是后端 API 域名 |

## 验证清单

发布前管理端至少验证以下操作：

| # | 操作 | 验证方法 | 期望 |
| --- | --- | --- | --- |
| 1 | 管理端登录 | `POST /api/admin/login` | 返回 Token |
| 2 | 用户列表 | `GET /api/admin/user/list` | 返回分页数据 |
| 3 | 封禁/解封 | `POST /api/admin/user/ban/:id` | 封禁后用户端返回 704 |
| 4 | 内容审核 | `GET /api/admin/blog/pending` | 返回待审核列表 |
| 5 | AI 配置面板 | 打开管理端 AI 配置页 | Runtime 信息脱敏展示 |
| 6 | 操作日志 | `GET /api/admin/log/list` | 返回操作记录 |

更多管理端截图和详细操作见 [验证记录与已知问题](/manuals/verified-scenarios)。
