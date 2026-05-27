# 故障响应与降级

本页是 Code-Nest 故障响应的操作手册，覆盖严重性定级、10 分钟止损动作、降级策略、恢复验证和复盘模板。

如果你在排查具体问题，先看 [常见问题排查](/operations/troubleshooting) 和 [问题定位流程](/operations/diagnosis-flow)。如果你要了解监控和告警，看 [监控与观测](/operations/monitoring)。

## 严重性定级

| 级别 | 定义 | 例子 | 响应目标 |
| --- | --- | --- | --- |
| P0 | 核心功能全量不可用，影响 > 50% 用户 | 后端宕机、MySQL 宕机、Redis 宕机 | 10 分钟内止损 |
| P1 | 核心功能部分不可用，影响 10%-50% 用户 | OJ 判题卡住、AI 服务超时、聊天室断连 | 30 分钟内止损 |
| P2 | 非核心功能异常，影响 < 10% 用户 | 通知延迟、积分统计偏差、文件上传偶发失败 | 2 小时内修复 |
| P3 | 体验问题，不影响功能正确性 | 页面加载慢、签到文案不更新、管理端搜索延迟 | 下个版本修复 |

## 10 分钟止损动作

发现 P0/P1 后，按以下顺序执行：

### 1. 确认故障范围（1 分钟）

```bash
# 快速检查后端健康
curl -s http://localhost:9999/api/actuator/health | python3 -m json.tool

# 检查数据库连通性
curl -s http://localhost:9999/api/actuator/health | grep -o '"db".*"status":"[^"]*"'
# 或直接连 MySQL
mysql -u root -p -e "SELECT 1; SHOW PROCESSLIST;" code_nest

# 检查 Redis 连通性
redis-cli ping
# 期望: PONG

# 检查 Sa-Token 登录态
redis-cli -n 4 keys "satoken:*" | wc -l
# 有数字说明有活跃登录态

# 检查业务缓存
redis-cli -n 3 dbsize
```

### 2. 通知团队（1 分钟）

在团队群发布：

```text
[故障通报]
时间：2026-XX-XX HH:MM
级别：P0/P1
现象：一句话描述
影响：哪些模块 + 多少用户
当前动作：正在排查
预计恢复：XX:XX
```

### 3. 快速定位模块（3 分钟）

```bash
# 查看后端最近错误日志
# Windows PowerShell:
Get-Content logs/xiaou.log -Tail 100 | Select-String "ERROR|Exception"

# Linux/Mac:
tail -100 logs/xiaou.log | grep -E "ERROR|Exception"

# 查看最近的 5xx 请求
tail -500 logs/xiaou.log | grep "5[0-9][0-9]"

# 检查 OJ 提交是否有卡住的
mysql -u root -p -e "SELECT COUNT(*) AS stuck FROM oj_submission WHERE status IN (0, 1) AND created_time < DATE_SUB(NOW(), INTERVAL 5 MINUTE);" code_nest

# 检查数据库慢查询
mysql -u root -p -e "SHOW FULL PROCESSLIST;" code_nest

# 检查 Redis 内存使用
redis-cli info memory | grep used_memory_human

# 检查连接池状态
redis-cli -n 3 info clients
redis-cli -n 4 info clients
```

### 4. 执行止损（5 分钟）

根据定位结果选择对应的降级策略（见下方 [降级策略](#降级策略)）。

## 降级策略

按模块分类，每个模块的降级方式、恢复条件和操作步骤。

### 后端整体不可用

| 项 | 内容 |
| --- | --- |
| **触发条件** | Health 探针返回 `DOWN` 或连续 3 次超时 |
| **降级方式** | 前端显示维护提示页，Nginx 返回 503 + 静态维护页 |
| **恢复条件** | Health 返回 `UP` + 登录接口可调通 |
| **操作步骤** | 1. 部署维护页 2. 检查后端日志 3. 重启服务 4. 验证恢复 |

Nginx 维护页配置：

```nginx
server {
    listen 9999;
    location / {
        return 503 "System under maintenance. Please try again later.";
    }
}
```

重启后端：

```bash
# 方式一：重新运行 jar
kill $(pgrep -f xiaou-application)
java -jar xiaou-application/target/xiaou-application-*.jar &

# 方式二：Docker 重启
docker compose -f docker/docker-compose.yml restart code-nest

# 验证
curl http://localhost:9999/api/actuator/health
# 期望: {"status":"UP"}
```

### MySQL 不可用

| 项 | 内容 |
| --- | --- |
| **触发条件** | 连接超时、连接池耗尽、磁盘满 |
| **降级方式** | 只读模式（关闭写入口），前端隐藏发布/提交按钮 |
| **恢复条件** | `SELECT 1` 正常 + 连接数恢复 + 无慢查询 |
| **操作步骤** | 1. 检查 MySQL 进程 2. 释放锁/连接 3. 扩容磁盘 4. 验证读写 |

```bash
# 检查 MySQL 进程
mysqladmin -u root -p ping
# 期望: mysqld is alive

# 检查连接数
mysql -u root -p -e "SHOW STATUS LIKE 'Threads_connected';" code_nest
# 对比 max_connections
mysql -u root -p -e "SHOW VARIABLES LIKE 'max_connections';" code_nest

# 检查慢查询
mysql -u root -p -e "SHOW FULL PROCESSLIST;" code_nest

# 检查磁盘空间
mysql -u root -p -e "SELECT table_schema AS 'Database', ROUND(SUM(data_length + index_length) / 1024 / 1024, 2) AS 'Size (MB)' FROM information_schema.tables WHERE table_schema = 'code_nest' GROUP BY table_schema;" code_nest

# 查看最大的表
mysql -u root -p -e "SELECT table_name, ROUND((data_length + index_length) / 1024 / 1024, 2) AS 'Size (MB)' FROM information_schema.tables WHERE table_schema = 'code_nest' ORDER BY (data_length + index_length) DESC LIMIT 10;" code_nest

# Kill 长时间运行的查询（谨慎操作）
mysql -u root -p -e "SELECT Id, Time, Info FROM information_schema.PROCESSLIST WHERE Time > 30 AND Command = 'Query';" code_nest
# 然后 KILL <Id>;
```

### Redis 不可用

| 项 | 内容 |
| --- | --- |
| **触发条件** | `redis-cli ping` 超时或返回错误 |
| **降级方式** | 业务降级（缓存穿透到 DB），登录态失效需重新登录 |
| **恢复条件** | `redis-cli ping` 返回 `PONG` + 两个 db 都可读写 |
| **操作步骤** | 1. 重启 Redis 2. 检查内存 3. 清理过期 key 4. 验证读写 |

```bash
# 检查 Redis 状态
redis-cli ping
redis-cli info server
redis-cli info memory | grep used_memory_human

# 检查各 db 的 key 数量
redis-cli -n 3 dbsize  # 业务缓存
redis-cli -n 4 dbsize  # Sa-Token 登录态

# 检查客户端连接数
redis-cli info clients | grep connected_clients

# 如果内存不足，清理大 key
redis-cli -n 3 --bigkeys
redis-cli -n 4 --bigkeys

# 强制清理过期 key（通常不需要，Redis 会自动清理）
redis-cli -n 3 scan 0 count 1000

# 重启 Redis（谨慎，登录态会丢失）
# systemctl restart redis
# 或
# redis-cli shutdown && redis-server /path/to/redis.conf
```

> **注意**：Redis 重启后，所有用户的登录态会丢失（db4 中的 Sa-Token 数据清空），用户需要重新登录。业务缓存（db3）丢失只会导致短暂的缓存穿透，不影响数据正确性。

### OJ 判题故障

| 项 | 内容 |
| --- | --- |
| **触发条件** | 提交一直 JUDGING 超过 5 分钟，或 go-judge 返回连接错误 |
| **降级方式** | 暂停提交入口，显示"判题服务维护中"提示 |
| **恢复条件** | go-judge 可达 + 测试提交能正常返回结果 |
| **操作步骤** | 1. 检查 go-judge 状态 2. 重置卡住的提交 3. 重启 go-judge 4. 验证 |

```bash
# 检查 go-judge 可达性
curl -s http://127.0.0.1:5050/version
# 或检查远端
curl -s http://<oj.judge.go-judge-url>:5050/version

# 查看 JUDGING 状态的提交数量
mysql -u root -p -e "SELECT status, COUNT(*) AS cnt FROM oj_submission GROUP BY status;" code_nest

# 查看卡住的提交（JUDGING 超过 5 分钟）
mysql -u root -p -e "SELECT id, problem_id, user_id, status, created_time FROM oj_submission WHERE status = 1 AND created_time < DATE_SUB(NOW(), INTERVAL 5 MINUTE) ORDER BY created_time DESC LIMIT 20;" code_nest
# status: 0=PENDING, 1=JUDGING, 2=ACCEPTED, 3=WRONG_ANSWER, 4=COMPILE_ERROR, 5=TIME_LIMIT_EXCEEDED, 6=MEMORY_LIMIT_EXCEEDED, 7=RUNTIME_ERROR

# 重置卡住的提交为 PENDING（谨慎操作，确认 go-judge 已恢复后再执行）
mysql -u root -p -e "UPDATE oj_submission SET status = 0 WHERE status = 1 AND created_time < DATE_SUB(NOW(), INTERVAL 5 MINUTE);" code_nest

# 检查赛事期间提交
mysql -u root -p -e "SELECT c.title AS contest, COUNT(s.id) AS submissions, SUM(CASE WHEN s.status = 1 THEN 1 ELSE 0 END) AS judging FROM oj_contest c LEFT JOIN oj_submission s ON s.contest_id = c.id WHERE c.end_time > NOW() GROUP BY c.id;" code_nest
```

### 聊天室故障

| 项 | 内容 |
| --- | --- |
| **触发条件** | ws-ticket 接口超时、WebSocket 连不上、消息无 ACK |
| **降级方式** | 关闭聊天入口，降级为 HTTP 轮询或显示"聊天服务维护中" |
| **恢复条件** | ws-ticket 可获取 + WebSocket 可连接 + 消息有 ACK |
| **操作步骤** | 1. 检查 Redis db3 2. 检查 ws-ticket 接口 3. 检查 WebSocket 配置 4. 验证消息收发 |

```bash
# 检查 ws-ticket 接口
curl -s http://localhost:9999/api/user/chat/ws-ticket \
  -H "Authorization: Bearer <user-token>"
# 期望: {"code":200,"data":{"ticket":"xxx"}}

# 检查 Redis 中 ws-ticket 相关 key
redis-cli -n 3 keys "ws:ticket:*"
# 应该看到 ticket key，TTL 60 秒

# 检查限流相关 key
redis-cli -n 3 keys "chat:rate:*"

# 检查在线用户数
redis-cli -n 3 keys "chat:online:*"
redis-cli -n 3 keys "chat:session:*" | wc -l

# 检查 Nginx WebSocket 代理配置（生产环境）
# 确保 /api/ws/ 路径有 Upgrade 头
```

Nginx WebSocket 代理配置：

```nginx
location /api/ws/ {
    proxy_pass http://127.0.0.1:9999;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_read_timeout 3600s;  # WebSocket 长连接超时
}
```

### AI 服务故障

| 项 | 内容 |
| --- | --- |
| **触发条件** | AI 接口超时、返回 5xx、结构化输出异常 |
| **降级方式** | AI 功能自动降级，返回默认提示，不影响其他业务 |
| **恢复条件** | AI 接口可调通 + Prompt 调试返回正常结果 |
| **操作步骤** | 1. 检查 AI 配置 2. 检查 Provider 可达性 3. 运行回归测试 4. 验证 |

```bash
# 检查 AI Runtime 配置（脱敏展示）
curl -s http://localhost:9999/api/admin/ai/config/runtime \
  -H "Authorization: Bearer <admin-token>"
# 期望: {"code":200,"data":{"provider":"openai-compatible",...}}

# 检查 AI Provider 可达性（手动测试）
curl -s https://your-openai-proxy.example.com/v1/models \
  -H "Authorization: Bearer sk-xxxx"
# 期望: 返回模型列表

# 执行 Prompt 调试
curl -s -X POST http://localhost:9999/api/admin/ai/config/prompt/debug \
  -H "Authorization: Bearer <admin-token>" \
  -H "Content-Type: application/json" \
  -d '{"scene":"mock-interview","variables":{"position":"Java后端开发","type":"TECHNICAL"}}'
# 期望: {"code":200,"data":{"output":{...},"usage":{...}}}

# 运行 AI 回归测试
curl -s -X POST http://localhost:9999/api/admin/ai/config/regression/run \
  -H "Authorization: Bearer <admin-token>"
# 期望: {"code":200,"data":{"totalCases":N,"passed":N,"failed":0}}

# 命令行运行 AI 回归
mvn -pl xiaou-ai -am "-Dtest=AiSceneRegressionEvalTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

AI 降级行为说明：

| 功能 | AI 可用时 | AI 不可用时（降级） |
| --- | --- | --- |
| 模拟面试 | AI 生成问题 + 评估答案 + 追问 | 创建会话成功，但返回默认提示 |
| 面试报告 | AI 生成评分 + 总结 | 报告生成失败，不影响历史记录 |
| SQL 优化 | AI 分析 + 建议 | 返回"AI 服务暂不可用" |
| RAG 检索 | 向量检索 + 上下文增强 | RAG 检索返回空结果 |

### 通知故障

| 项 | 内容 |
| --- | --- |
| **触发条件** | 通知未送达、未读数不准、通知列表为空 |
| **降级方式** | 通知为异步组件，降级不影响核心业务，用户手动刷新 |
| **恢复条件** | 未读数正确 + 通知列表正常 + 新通知实时到达 |
| **操作步骤** | 1. 检查通知接口 2. 检查数据库通知记录 3. 检查 WebSocket 推送 |

```bash
# 检查未读通知接口
curl -s http://localhost:9999/api/user/notification/unread-count \
  -H "Authorization: Bearer <user-token>"

# 检查通知列表
curl -s "http://localhost:9999/api/user/notification/list?page=1&size=20" \
  -H "Authorization: Bearer <user-token>"

# 检查数据库中最近的通知
mysql -u root -p -e "SELECT id, user_id, type, title, is_read, created_time FROM user_notification ORDER BY created_time DESC LIMIT 20;" code_nest

# 检查某用户未读数
mysql -u root -p -e "SELECT COUNT(*) AS unread FROM user_notification WHERE user_id = <uid> AND is_read = 0;" code_nest

# 检查通知触发是否正常（最近 1 小时的通知）
mysql -u root -p -e "SELECT type, COUNT(*) AS cnt FROM user_notification WHERE created_time > DATE_SUB(NOW(), INTERVAL 1 HOUR) GROUP BY type;" code_nest
```

### 文件上传故障

| 项 | 内容 |
| --- | --- |
| **触发条件** | 上传返回 801/802/803 错误，或上传成功但 URL 不可访问 |
| **降级方式** | 关闭图片/文件上传入口，文本功能继续可用 |
| **恢复条件** | 上传成功 + 文件 URL 可访问 |
| **操作步骤** | 1. 检查存储配置 2. 检查磁盘空间 3. 检查文件权限 |

```bash
# 检查文件存储配置
# 查看 application.yml 中 xiaou.file.storage-type
# local: 检查 xiaou.file.local.upload-dir 和 xiaou.file.local.base-url
# minio: 检查 xiaou.file.minio.* 配置

# 检查本地存储磁盘空间（local 模式）
df -h /path/to/upload-dir

# 测试文件上传
curl -X POST http://localhost:9999/api/file \
  -H "Authorization: Bearer <user-token>" \
  -F "file=@test.txt"
# 期望: {"code":200,"data":{"url":"/api/files/xxx.txt"}}

# 验证文件可访问
curl -I http://localhost:9999/api/files/xxx.txt
# 期望: HTTP 200

# 检查 Nginx 文件代理配置（生产环境）
# 确保 /api/files/ 路径正确代理到后端或静态目录
```

### 积分异常

| 项 | 内容 |
| --- | --- |
| **触发条件** | 积分余额与流水不一致、签到不生效、抽奖扣分异常 |
| **降级方式** | 暂停抽奖入口，签到和查询保持可用 |
| **恢复条件** | 余额与流水一致 + 签到正常 + 抽奖扣分正确 |
| **操作步骤** | 1. 检查积分余额 2. 对比流水 3. 人工补偿 |

```bash
# 检查用户积分余额
curl -s http://localhost:9999/api/user/points/balance \
  -H "Authorization: Bearer <user-token>"

# 检查积分流水
curl -s "http://localhost:9999/api/user/points/detail?page=1&size=20" \
  -H "Authorization: Bearer <user-token>"

# 积分余额一致性校验 SQL
mysql -u root -p code_nest -e "
SELECT u.id, u.nickname,
  (SELECT COALESCE(SUM(CASE WHEN type='EARN' THEN amount ELSE -amount END), 0)
   FROM user_points_detail WHERE user_id = u.id) AS calculated,
  b.balance AS stored
FROM user_info u
LEFT JOIN user_points_balance b ON u.id = b.user_id
HAVING calculated != stored;
"
# 无结果说明一致

# 查看最近的积分变动
mysql -u root -p -e "SELECT * FROM user_points_detail ORDER BY created_time DESC LIMIT 20;" code_nest

# 管理端发放积分补偿
curl -X POST http://localhost:9999/api/admin/points/grant \
  -H "Authorization: Bearer <admin-token>" \
  -H "Content-Type: application/json" \
  -d '{"userId":123,"amount":100,"reason":"故障补偿"}'
```

## 恢复验证步骤

每种场景恢复后的验证步骤不同。按优先级排列：

### 基础验证（必须先通过）

| 模块 | 验证方法 | 期望结果 |
| --- | --- | --- |
| 后端健康 | `curl http://localhost:9999/api/actuator/health` | `{"status":"UP"}` |
| 用户端登录 | `POST /api/user/auth/login` | 返回 Token |
| 管理端登录 | `POST /api/admin/login` | 返回 Token |
| 数据库 | `SELECT 1` + `SHOW PROCESSLIST` | 查询正常，连接数合理 |
| Redis | `redis-cli ping` | `PONG` |

### 业务验证（基础通过后）

| 模块 | 验证方法 | 期望结果 | 依赖 |
| --- | --- | --- | --- |
| 社区/博客 | 打开列表页 + 发帖/发布 | 列表展示 + 写操作成功 + 管理端可见 | MySQL |
| OJ 判题 | `GET /api/user/oj/problem/list` + 提交代码 | 题面展示 + 提交状态推进 | go-judge |
| 聊天室 | `GET /api/user/chat/ws-ticket` + ws 连接 | ticket 获取 + CONNECT + 消息 ACK | Redis db3 |
| AI 功能 | `GET /api/admin/ai/config/runtime` + Prompt 调试 | 配置脱敏展示 + 调试返回结果 | AI Provider |
| 通知 | `GET /api/user/notification/unread-count` | 未读数正确 | MySQL + WebSocket |
| 文件上传 | `POST /api/file` + 访问文件 URL | 上传成功 + URL 可访问 | 存储服务 |
| 积分 | `GET /api/user/points/balance` | 余额正确 | MySQL |

### 运维验证（可异步检查）

| 模块 | 验证方法 | 期望结果 |
| --- | --- | --- |
| Prometheus | `curl http://localhost:9999/api/actuator/prometheus` | 返回指标 |
| 仪表盘 | 管理端首页 | 数据正常展示 |
| 操作日志 | `GET /api/admin/log/list` | 有记录 |
| Sa-Token | `redis-cli -n 4 keys "satoken:*" \| wc -l` | 有活跃登录态 |

> **验证顺序建议**：先验证登录和数据库（其他验证的基础），再验证 OJ、聊天、AI（需要外部依赖），最后验证监控、通知、积分（可异步检查）。

## 快速诊断命令速查

| 场景 | 命令 | 说明 |
| --- | --- | --- |
| 后端健康 | `curl http://localhost:9999/api/actuator/health` | 整体健康状态 |
| MySQL 连通 | `mysql -u root -p -e "SELECT 1"` | 数据库可用 |
| MySQL 连接数 | `mysql -u root -p -e "SHOW STATUS LIKE 'Threads_connected'"` | 检查连接池 |
| MySQL 慢查询 | `mysql -u root -p -e "SHOW FULL PROCESSLIST"` | 找到阻塞查询 |
| Redis 连通 | `redis-cli ping` | 缓存可用 |
| Redis 内存 | `redis-cli info memory \| grep used_memory_human` | 内存使用 |
| Sa-Token 登录态 | `redis-cli -n 4 keys "satoken:*" \| wc -l` | 活跃会话数 |
| 业务缓存 | `redis-cli -n 3 dbsize` | 缓存 key 数量 |
| OJ 卡住提交 | `SELECT COUNT(*) FROM oj_submission WHERE status=1 AND created_time < DATE_SUB(NOW(), INTERVAL 5 MINUTE)` | 判题队列积压 |
| 积分一致性 | 见上方积分异常 SQL | 余额与流水对比 |
| 通知积压 | `SELECT COUNT(*) FROM user_notification WHERE is_read=0 AND created_time > DATE_SUB(NOW(), INTERVAL 1 HOUR)` | 未处理通知数 |
| go-judge 可达 | `curl http://127.0.0.1:5050/version` | 判题沙箱可用 |
| AI Provider | `curl $XIAOU_AI_BASE_URL/models -H "Authorization: Bearer $XIAOU_AI_API_KEY"` | AI 服务可达 |
| 后端错误日志 | `tail -100 logs/xiaou.log \| grep ERROR` | 最近错误 |
| 磁盘空间 | `df -h` | 磁盘充足 |

## 复盘模板

故障恢复后 24 小时内完成复盘，记录到项目 Wiki 或文档站。

```text
事故标题：[模块+现象+影响]
严重级别：P0/P1/P2/P3
影响范围：[模块+用户数+持续时间]
时间线：
  - HH:MM 发现（怎么发现的）
  - HH:MM 确认（确认了什么）
  - HH:MM 止损（做了什么）
  - HH:MM 恢复（怎么恢复的）
  - HH:MM 验证（验证了什么）
  - HH:MM 关闭（确认完全恢复）
根因：[最底层原因，不是表面现象]
止损动作：[具体做了什么，耗时多久]
修复方案：[代码/配置/数据修复内容]
预防措施：[防止再发的具体动作]
待办项：
  - [ ] [具体待办+负责人+截止日期]
文档更新：[哪些文档需要更新]
```

### 时间线示例

```text
事故标题：OJ 判题全量卡住 - JUDGING 状态不推进
严重级别：P1
影响范围：OJ 模块，约 30 个活跃用户，持续 25 分钟
时间线：
  - 14:00 发现 — 用户反馈提交一直 JUDGING
  - 14:02 确认 — 查 oj_submission 表，15 条记录 status=1 超过 5 分钟
  - 14:03 止损 — 前端 OJ 页面显示"判题服务维护中"
  - 14:05 定位 — curl go-judge:5050 超时，确认 go-judge 宕机
  - 14:10 修复 — 重启 go-judge 容器
  - 14:15 验证 — 测试提交返回 ACCEPTED，重置卡住提交为 PENDING
  - 14:25 关闭 — 所有提交状态已推进，前端恢复入口
根因：go-judge 容器 OOM 被杀，原因是测试用例内存限制配置过大
止损动作：前端显示维护提示 + 重启 go-judge，耗时 10 分钟
修复方案：调小默认 memory_limit 256→128，增加 go-judge 容器内存限制
预防措施：
  - [ ] 增加 go-judge 容器内存到 2GB @张三 2026-05-30
  - [ ] 添加 go-judge 健康检查到 Prometheus @李四 2026-06-01
  - [ ] OJ 提交超时自动重置机制 @王五 2026-06-05
文档更新：更新 OJ 模块文档 + 故障响应页添加 go-judge 章节
```

## 推荐阅读

| 继续看什么 | 为什么 |
| --- | --- |
| [监控与观测](/operations/monitoring) | 看指标、告警和阈值 |
| [问题定位流程](/operations/diagnosis-flow) | 把问题缩小到具体模块 |
| [常见问题排查](/operations/troubleshooting) | 查本地和部署层常见故障 |
| [Docker 与服务部署](/operations/docker) | 看部署和代理配置 |
| [响应体与错误码](/reference/response-errors) | 看失败是怎么表达出来的 |
| [事件、通知与回流索引](/reference/event-backflow-index) | 看成功后该回流什么 |
| [幂等、回滚与补偿索引](/reference/idempotency-rollbacks-compensation) | 看恢复和补偿怎么做 |
| [核心链路教程](/manuals/core-workflows) | 看完整业务链路操作步骤 |
| [用户端操作手册](/manuals/user-operations) | 知用户端接口和错误码 |
| [管理端操作手册](/manuals/admin-operations) | 知管理端接口和验证步骤 |


## 相关文档

| 文档 | 说明 |
| --- | --- |
| [监控与观测](/operations/monitoring) | 监控配置 |
| [告警 Runbook](/operations/alert-runbooks) | 告警处理 |
| [问题定位流程](/operations/diagnosis-flow) | 问题排查 |
| [常见问题排查](/operations/troubleshooting) | 常见问题 |
