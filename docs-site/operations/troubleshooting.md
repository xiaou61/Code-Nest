# 常见问题排查

本文按症状分类，给出排查思路、诊断命令和常见修复方案。所有命令和配置路径都来自源码，可直接对照使用。

## 快速诊断

启动后第一步确认服务健康：

```powershell
# 后端健康检查
curl http://localhost:9999/api/actuator/health
# 期望: {"status":"UP"}

# MySQL 连通性
mysql -u root -p -e "SELECT 1"

# Redis 连通性
redis-cli ping
# 期望: PONG
```

| 端点 | 期望响应 | 含义 |
| --- | --- | --- |
| `/api/actuator/health` | `{"status":"UP"}` | 后端 + 所有依赖正常 |
| `/api/actuator/health` 含 components | `{"components":{"dataSource":{"status":"UP"},...}}` | 可以看到每个依赖的状态 |
| `/api/actuator/prometheus` | Prometheus 格式指标 | 运行时度量可采集 |

如果 health 返回 DOWN，看 components 里哪个组件挂了：

```text
{"status":"DOWN","components":{"diskSpace":{"status":"UP"},"redis":{"status":"UP"},"dataSource":{"status":"DOWN"}}}
```

上面这个例子说明 MySQL 挂了，Redis 和磁盘都正常。

## 后端启动问题

### MySQL 连接失败

**症状**：启动日志 `Communications link failure` 或 `Access denied`

```powershell
# 1. 检查 MySQL 是否运行
net start mysql          # Windows
systemctl status mysql   # Linux

# 2. 检查端口是否监听
netstat -an | findstr 3306   # Windows
ss -tlnp | grep 3306         # Linux

# 3. 手动连接测试
mysql -u root -p -h 127.0.0.1 -P 3306 code_nest

# 4. 检查配置文件
# xiaou-application/src/main/resources/application-dev.yml
# spring.datasource.url / username / password
```

| 原因 | 修复 |
| --- | --- |
| MySQL 未启动 | 启动 MySQL 服务 |
| 端口不是 3306 | 修改 `spring.datasource.url` 中的端口 |
| 密码不对 | 修改 `spring.datasource.password`，dev 默认是 `1234` |
| 数据库不存在 | `CREATE DATABASE code_nest CHARACTER SET utf8mb4;` 然后导入基线 |
| P6Spy URL 格式错误 | dev 环境下 URL 前缀应为 `jdbc:p6spy:mysql://`，不是 `jdbc:mysql://` |
| Docker 下 MySQL 还没健康 | 检查 `depends_on` + 健康检查配置 |

### Redis 连接失败

**症状**：启动日志 `Unable to connect to Redis` 或 `RedisConnectionException`

```powershell
# 检查 Redis 是否运行
redis-cli ping
# 期望: PONG

# 检查 Redisson 配置（业务 Redis）
# application-dev.yml → spring.data.redis.redisson.config
# address: "redis://127.0.0.1:6379"
# database: 3

# 检查 Sa-Token alone-redis 配置
# application-dev.yml → sa-token.alone-redis
# host: 127.0.0.1
# port: 6379
# database: 4
```

| 原因 | 修复 |
| --- | --- |
| Redis 未启动 | `redis-server` 启动 |
| 端口不是 6379 | 修改 `spring.data.redis.redisson.config` 中的 address |
| 需要密码 | 添加 `password` 配置 |
| db 索引不对 | 业务使用 db3，Sa-Token 使用 db4 |
| Docker 下地址不对 | Docker 内部网络用 `redis:6379`，宿主机用 `127.0.0.1:6379` |

### 端口冲突

**症状**：`Port 9999 already in use`

```powershell
# Windows: 查找占用端口的进程
netstat -ano | findstr :9999
taskkill /PID <PID> /F

# Linux/macOS
lsof -i :9999
kill -9 <PID>
```

或修改 `application.yml` 中的 `server.port`。

### 依赖编译失败

**症状**：`mvn clean install` 报错

```powershell
# 清理并重新构建
mvn clean install -DskipTests -U

# 如果某个模块失败，单独构建
cd xiaou-common && mvn clean install -DskipTests
cd xiaou-user-api && mvn clean install -DskipTests
```

| 原因 | 修复 |
| --- | --- |
| 本地仓库缓存损坏 | `mvn dependency:purge-local-repository` |
| Java 版本不对 | `java -version` 确认 17+，JDK 17 是硬性要求 |
| Maven 版本不对 | `mvn -version` 确认 3.8+ |
| 版本号不一致 | 确认根目录 `pom.xml` 中的 `<revision>` 与子模块一致 |
| 网络问题下载依赖失败 | 检查 Maven 仓库设置（`~/.m2/settings.xml`），切换镜像 |

### P6Spy ClassNotFoundException

**症状**：`ClassNotFoundException: com.p6spy.engine.spy.P6SpyDriver`

| 原因 | 修复 |
| --- | --- |
| P6Spy 依赖被注释 | 确认 pom 里 p6spy 依赖未注释 |
| 不需要 P6Spy | 在 `application-dev.yml` 中改回标准驱动和 URL |

```yaml
# 不使用 P6Spy 时，改回标准驱动
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/code_nest?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf-8
```

## 认证与鉴权问题

### 登录返回 705

**含义**：`LOGIN_FAILED` — 用户名或密码错误

| 检查项 | 方法 |
| --- | --- |
| 用户是否存在 | `SELECT * FROM user_info WHERE username = ?` |
| 密码是否正确 | 检查 BCrypt 加密是否匹配 |
| 账号是否禁用 | `status` 字段是否为 1 |
| 验证码是否正确 | 检查 captchaKey 和 captchaCode 是否匹配 |

### 接口返回 701

**含义**：`TOKEN_INVALID` — Token 无效或缺失

| 场景 | 原因 | 修复 |
| --- | --- | --- |
| 请求未带 Token | 前端 Axios 拦截器未注入 Header | 检查前端 `request.js` 的 Token 注入逻辑 |
| Token 已过期 | 超过 `sa-token.timeout`（默认 7 天） | 重新登录 |
| 端类型错误 | 用户端 Token 访问了管理端接口 | 使用正确的登录接口和 StpUtil |
| Header 格式错误 | 应为 `Authorization: Bearer <token>` | 检查前端是否加了 Bearer 前缀 |

### 接口返回 702

**含义**：`TOKEN_EXPIRED` — Token 已过期

| 原因 | 修复 |
| --- | --- |
| 超过 `sa-token.timeout` | 重新登录，默认 7 天有效期 |
| Redis 重启导致登录态丢失 | 开启 Redis 持久化（AOF 或 RDB） |

### 接口返回 703

**含义**：`PERMISSION_DENIED` — 权限不足

| 场景 | 原因 |
| --- | --- |
| 用户端账号访问 `/api/admin/` | 管理端接口需要 `@RequireAdmin` → 详见 [权限注解与角色边界索引](/reference/permission-boundaries) |
| 管理端账号无 admin 角色 | 检查 `sys_admin` 表的 role 字段 |
| 资源不属于当前用户 | 用户端资源归属校验失败 |

### 接口返回 704

**含义**：`ACCOUNT_BANNED` — 账号被封禁

| 检查项 | 方法 |
| --- | --- |
| 账号状态 | `SELECT status FROM user_info WHERE id = ?`，status=1 表示禁用 |
| 封禁到期 | 检查是否有封禁到期时间字段，到期后自动解封 |

### Sa-Token 会话异常

**症状**：登录后偶尔掉线、Token 频繁失效

```powershell
# 检查 Redis 中 Sa-Token 数据
redis-cli -n 4 keys "satoken:*"

# 检查 Token 配置
# application.yml → sa-token.timeout / sa-token.activity-timeout
```

| 原因 | 修复 |
| --- | --- |
| Redis 内存不足 | 检查 `redis-cli info memory` |
| activity-timeout 过短 | 增大 `sa-token.activity-timeout`（当前为 -1，不启用） |
| 多端 Token 冲突 | 确认 User/Admin 端使用不同 Stp（StpUserUtil vs StpAdminUtil） |
| Redis 未持久化 | 开启 AOF：`redis-server --appendonly yes` |

## 数据库问题

### MyBatis 查询报错

**症状**：`Invalid bound statement` 或 SQL 语法错误

| 原因 | 修复 |
| --- | --- |
| Mapper XML 未扫描 | 确认 `@MapperScan("com.xiaou")` |
| XML namespace 不匹配 | 检查 XML 的 `namespace` 与 Mapper 接口全限定名一致 |
| 字段映射错误 | 检查 `@TableName` / `@TableField` 注解 |
| SQL 语法错误 | 开启 P6Spy 查看实际 SQL |

**开启 SQL 日志**（开发环境默认已开启 P6Spy）：

```yaml
# application-dev.yml
spring:
  datasource:
    driver-class-name: com.p6spy.engine.spy.P6SpyDriver
    url: jdbc:p6spy:mysql://localhost:3306/code_nest
```

日志输出到控制台和 `logs/spy.log`。

### 表不存在

**症状**：`Table 'code_nest.xxx' doesn't exist`

```powershell
# 检查表数量
mysql -u root -p -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'code_nest'"
# 期望: 97 左右

# 检查特定表是否存在
mysql -u root -p -e "SHOW TABLES FROM code_nest LIKE 'oj_%'"
```

| 原因 | 修复 |
| --- | --- |
| 基础 SQL 未导入 | `mysql -u root -p code_nest < sql/MySql/code_nest.sql` |
| 增量 SQL 未执行 | 按版本顺序导入 `sql/v1.x.0/v1.x.0.sql` |
| 导入到了错误的数据库 | 确认 `USE code_nest` |

### 数据不一致

**症状**：数据显示异常、统计数据不准

```sql
-- 检查逻辑删除数据
SELECT COUNT(*) FROM user_info WHERE is_deleted = 1;

-- 检查状态异常数据
SELECT * FROM user_info WHERE status NOT IN (0, 1);

-- 检查 OJ 提交卡住
SELECT * FROM oj_submission WHERE status IN (0, 1)
  AND created_time < DATE_SUB(NOW(), INTERVAL 1 HOUR);

-- 检查积分余额不一致
SELECT u.id, u.nickname,
  (SELECT COALESCE(SUM(CASE WHEN type='EARN' THEN amount ELSE -amount END), 0)
   FROM user_points_detail WHERE user_id = u.id) AS calculated,
  b.balance AS stored
FROM user_info u
LEFT JOIN user_points_balance b ON u.id = b.user_id
HAVING calculated != stored;
```

更多状态排查 SQL 见 [状态机与枚举](/reference/module-state-machines)。

## 前端问题

### 白屏

| 原因 | 修复 |
| --- | --- |
| 后端未启动 | 先启动后端，确认 `/api/actuator/health` 返回 UP |
| 构建产物为空 | `npm run build` 检查是否有编译错误 |
| 路由配置错误 | 检查 Vue Router 配置 |
| API 请求失败 | 浏览器 DevTools → Network 查看请求状态 |
| CORS 拦截 | 后端 CorsConfig 添加前端 Origin |

### 接口 CORS 报错

**症状**：浏览器控制台 `Access-Control-Allow-Origin` 错误

| 检查项 | 方法 |
| --- | --- |
| 后端 CorsConfig | 确认前端 Origin 在 `XIAOU_CORS_ALLOWED_ORIGIN_PATTERNS` 白名单中 |
| Nginx 配置 | 检查 `add_header Access-Control-Allow-Origin` |
| Vite 代理 | 开发环境使用 `vite.config.js` 代理避免 CORS |
| WebSocket Origin | 聊天室 WebSocket 也使用同一组 Origin 白名单 |

> **重要**：CORS 白名单要填前端页面域名，不是后端 API 域名。

### 前端构建失败

```powershell
# 清理缓存重新构建
rm -rf node_modules package-lock.json
npm install
npm run build
```

| 原因 | 修复 |
| --- | --- |
| Node 版本不兼容 | 使用 Node 20 LTS，Node 22+ 可能需要 `--legacy-peer-deps` |
| npm 源不可达 | `npm config set registry https://registry.npmmirror.com` |
| peer deps 冲突 | `npm install --legacy-peer-deps` |
| TypeScript 类型错误 | 检查 import 路径和组件 props |

### Vite 代理 502

**症状**：`Proxy error: Could not proxy request /api/... from localhost:3001 to http://localhost:9999`

| 原因 | 修复 |
| --- | --- |
| 后端未启动 | 启动后端，等待 `/api/actuator/health` 返回 UP |
| 端口不对 | 检查 `vite.config.js` 中 proxy target 是否为 `http://localhost:9999` |
| 加了 rewrite | **不要加 rewrite**，后端 context-path 已包含 `/api` |

### 前端代理配置注意

两个前端的 Vite 代理配置**不加 rewrite**：

```javascript
// vue3-user-front/vite.config.js
proxy: {
  '/api': {
    target: 'http://localhost:9999',
    changeOrigin: true,
    secure: false,
    // 不要加 rewrite，因为后端 context-path 已经有 /api 前缀
  },
}
```

```javascript
// vue3-admin-front/vite.config.js
proxy: {
  '/api': {
    target: 'http://localhost:9999',
    changeOrigin: true,
    secure: false,
  },
}
```

## AI 功能问题

### AI 接口调用失败

| 原因 | 修复 |
| --- | --- |
| API Key 未配置 | 检查 `XIAOU_AI_API_KEY` 环境变量或 `application-sec.yml` |
| API Base URL 错误 | 确认 `XIAOU_AI_BASE_URL`，注意要包含 `/v1` 后缀 |
| 模型名称不匹配 | 确认 `XIAOU_AI_CHAT_MODEL`，默认 `gpt-5.4` |
| 网络不通 | 检查代理设置或防火墙 |
| 读取超时 | 增大 `xiaou.ai.timeout.read-ms`，默认 60000ms |
| AI 功能未开启 | 确认 `xiaou.ai.enabled=true` |

> **提示**：不配置 AI 也能正常启动，只是 AI 相关接口会返回降级响应。

### Prompt 注册失败

| 原因 | 修复 |
| --- | --- |
| Prompt 目录不存在 | 检查 `resources/prompts/` 目录 |
| YAML 格式错误 | 检查 Prompt 定义文件语法 |
| Schema 与输出不匹配 | 确认 Schema 定义与实际输出字段一致 |

### RAG 调用失败

| 原因 | 修复 |
| --- | --- |
| RAG 未启用 | 设置 `XIAOU_AI_RAG_ENABLED=true` |
| RAG sidecar 未启动 | 启动 `llamaindex-service` 容器 |
| Endpoint 地址错误 | 确认 `XIAOU_AI_RAG_ENDPOINT`，Docker 内部用 `http://llamaindex-service:18080` |
| API Key 不匹配 | 确认 `XIAOU_AI_RAG_API_KEY` 与 sidecar 配置一致 |

```powershell
# 检查 RAG sidecar 健康
curl http://127.0.0.1:18080/health
# 期望: {"status":"ok"}
```

详见 [AI 运行时](/modules/ai-runtime)。

## OJ 判题问题

### 提交一直处于 JUDGING

```sql
-- 查看卡住的提交
SELECT id, problem_id, user_id, status, created_time
FROM oj_submission
WHERE status IN (0, 1)
  AND created_time < DATE_SUB(NOW(), INTERVAL 30 MINUTE)
ORDER BY created_time;

-- 手动重置为 PENDING (谨慎操作)
UPDATE oj_submission SET status = 0 WHERE id = ?;
```

| 原因 | 修复 |
| --- | --- |
| go-judge 服务不可达 | 检查 `oj.judge.go-judge-url` 配置和 go-judge 容器状态 |
| 执行超时 | 检查题目 `time_limit` 设置是否合理 |
| 内存溢出 | 检查 `memory_limit` 设置 |
| 编译器未安装 | `docker exec go-judge javac -version` 确认 |
| privileged 未开启 | go-judge 容器需要 `privileged: true` |

### go-judge 连接失败

```powershell
# 检查 go-judge 容器状态
docker ps | findstr go-judge

# 检查 go-judge 日志
docker logs go-judge

# 检查编译器
docker exec go-judge javac -version
docker exec go-judge gcc --version
docker exec go-judge python3 --version
```

## 聊天室 WebSocket 问题

### WebSocket 连接失败

**症状**：用户端聊天页面无法连接，控制台显示 WebSocket 错误

| 原因 | 修复 |
| --- | --- |
| 未先获取 ws-ticket | 先调用 `/api/chat/ws-ticket` 获取一次性 ticket → 详见 [WebSocket 协议](/reference/websocket) |
| ticket 已使用 | ws-ticket 只能用一次，每次连接需要新 ticket → 详见 [WebSocket 协议](/reference/websocket) |
| Nginx 未配置 Upgrade | 添加 `/api/ws/` 的 WebSocket Upgrade 代理 |
| Origin 不在白名单 | 确认 `XIAOU_CORS_ALLOWED_ORIGIN_PATTERNS` 包含前端域名 |
| 后端未启动 | 确认 `/api/actuator/health` 返回 UP |

Nginx WebSocket 代理配置：

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

### 消息发送后无 ACK

| 原因 | 修复 |
| --- | --- |
| WebSocket 连接已断开 | 检查网络和心跳配置 |
| 消息格式错误 | 确认发送的 JSON 格式符合 WebSocket 协议 |
| 禁言状态 | 检查用户是否被禁言 |

## 文件上传问题

### 上传失败 801-805

| 错误码 | 含义 | 修复 |
| --- | --- | --- |
| 801 | 文件为空 | 检查上传文件是否正确 |
| 802 | 文件过大 | 调整 `spring.servlet.multipart.max-file-size`，默认 100MB |
| 803 | 文件类型不支持 | 检查白名单配置 |
| 804 | 存储空间不足 | 检查磁盘空间 |
| 805 | 上传路径不合法 | 检查文件名是否包含特殊字符 |

### 文件无法访问

| 原因 | 修复 |
| --- | --- |
| LocalFileResourceConfig 未映射 | 确认 `addResourceHandlers` 中的路径映射 |
| 文件不存在 | 检查文件是否已存储到磁盘 |
| 权限不足 | 检查文件系统读写权限 |
| 文件是私有的 | 检查 `is_public` 字段，私有文件需要权限校验 |
| 上传目录不持久化 | Docker 下 `uploads/` 需要 volume 挂载 |

## 日志与监控

### 查看后端日志

```powershell
# 实时查看日志
tail -f logs/code-nest.log

# 搜索异常
grep -i "exception" logs/code-nest.log | tail -20

# 搜索特定请求
grep "POST /api/oj" logs/code-nest.log

# 查看 P6Spy SQL 日志
tail -f logs/spy.log
```

### 查看 Docker 容器日志

```powershell
# 后端容器
docker logs -f code-nest-app

# MySQL 容器
docker logs -f code-nest-mysql

# RAG sidecar
docker logs -f code-nest-llamaindex
```

### 查看操作日志

管理端 `/api/admin/log/` 接口可查询系统操作日志，或直接查数据库：

```sql
SELECT * FROM sys_operation_log
WHERE created_time > DATE_SUB(NOW(), INTERVAL 24 HOUR)
ORDER BY created_time DESC
LIMIT 50;
```

### Spring Boot Actuator

| 端点 | 地址 | 用途 |
| --- | --- | --- |
| Health | `http://localhost:9999/api/actuator/health` | 健康检查 |
| Info | `http://localhost:9999/api/actuator/info` | 应用信息 |
| Prometheus | `http://localhost:9999/api/actuator/prometheus` | Prometheus 指标 |

> **注意**：所有 Actuator 端点都在 context-path `/api` 下，完整路径是 `/api/actuator/...`。

### 查看 Redis 数据

```powershell
# 业务数据（db3）
redis-cli -n 3 keys "*"

# Sa-Token 登录态（db4）
redis-cli -n 4 keys "satoken:*"

# 查看特定 key 的值
redis-cli -n 4 get "satoken:login:token:xxx"
```

## Docker 问题

### 容器启动失败

| 症状 | 原因 | 修复 |
| --- | --- | --- |
| MySQL 容器退出 | 端口 3306 被占用 | 停掉本地 MySQL 或改 Compose 端口映射 |
| Redis 容器退出 | 端口 6379 被占用 | 停掉本地 Redis 或改 Compose 端口映射 |
| 后端容器 `Communications link failure` | MySQL 还没健康就启动了后端 | 检查 `depends_on` 和健康检查 |
| 后端容器 Redis 连接失败 | Redis 地址不对 | Docker 内部用 `redis:6379`，宿主机用 `127.0.0.1:6379` |

### MySQL 数据没有自动导入

MySQL 的 `docker-entrypoint-initdb.d` 只在数据库为空时执行初始化脚本。如果 volume 已有数据，不会重复导入。

```powershell
# 重新初始化：删除 volume 再启动
docker compose -f docker/ai/docker-compose.yml down -v
docker compose -f docker/ai/docker-compose.yml up -d --build
```

## 问题升级

如果上述排查无法解决：

1. 搜索 [GitHub Issues](https://github.com/your-org/code-nest/issues) 是否有类似问题
2. 在项目内部讨论组提问，附上：错误日志、复现步骤、环境信息
3. 严重问题按 [事故响应流程](/operations/incident-response) 处理

更多排查流程见 [问题定位流程](/operations/diagnosis-flow) 和 [告警 Runbook](/operations/alert-runbooks)。

## 相关文档

| 文档 | 说明 |
| --- | --- |
| [问题定位流程](/operations/diagnosis-flow) | 排查流程 |
| [监控与观测](/operations/monitoring) | 监控配置 |
| [告警 Runbook](/operations/alert-runbooks) | 告警处理 |
| [事故响应](/operations/incident-response) | 事故处理 |
