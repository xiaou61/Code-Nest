# 常见问题排查

本文按症状分类，给出排查思路、诊断命令和常见修复方案。

## 快速诊断

启动后第一步确认服务健康：

```bash
# 后端健康检查
curl http://localhost:9999/api/actuator/health

# MySQL 连通性
mysql -u root -p -e "SELECT 1"

# Redis 连通性
redis-cli ping
```

| 端点 | 期望响应 | 含义 |
|------|----------|------|
| `/api/actuator/health` | `{"status":"UP"}` | 后端+依赖全部正常 |
| `/api/actuator/info` | 应用信息 | 构建版本等 |
| `/api/actuator/prometheus` | Prometheus 指标 | 运行时度量 |

## 后端启动问题

### MySQL 连接失败

**症状**：启动日志 `Communications link failure` 或 `Access denied`

```bash
# 1. 检查 MySQL 是否运行
net start mysql   # Windows
systemctl status mysql  # Linux

# 2. 检查连接参数
mysql -u root -p -h 127.0.0.1 -P 3306 code_nest

# 3. 检查配置文件
# xiaou-application/src/main/resources/application-dev.yml
# spring.datasource.url / username / password
```

| 原因 | 修复 |
|------|------|
| MySQL 未启动 | 启动 MySQL 服务 |
| 端口不是 3306 | 修改 `spring.datasource.url` 中的端口 |
| 密码不对 | 修改 `spring.datasource.password` |
| 数据库不存在 | `CREATE DATABASE code_nest CHARACTER SET utf8mb4;` 然后导入基线 |
| 连接超时 | 检查防火墙、MySQL `max_connections` |

### Redis 连接失败

**症状**：启动日志 `Unable to connect to Redis` 或 `RedisConnectionException`

```bash
# 检查 Redis 是否运行
redis-cli ping
# 期望: PONG

# 检查 Redisson 配置
# application-dev.yml → spring.data.redis.redisson.config
```

| 原因 | 修复 |
|------|------|
| Redis 未启动 | `redis-server` 启动 |
| 端口不是 6379 | 修改 `spring.data.redis.redisson.config` |
| 需要密码 | 添加 `password` 配置 |
| db 索引不对 | Sa-Token 使用 db4，业务使用 db3 |

### 端口冲突

**症状**：`Port 9999 already in use`

```bash
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

```bash
# 清理并重新构建
mvn clean install -DskipTests -U

# 如果某个模块失败，单独构建
cd xiaou-common && mvn clean install -DskipTests
cd xiaou-user-api && mvn clean install -DskipTests
```

| 原因 | 修复 |
|------|------|
| 本地仓库缓存损坏 | `mvn dependency:purge-local-repository` |
| Java 版本不对 | `java -version` 确认 17+ |
| Maven 版本不对 | `mvn -version` 确认 3.8+ |

## 认证与鉴权问题

### 登录返回 705

**含义**：`LOGIN_FAILED` — 用户名或密码错误

| 检查项 | 方法 |
|--------|------|
| 用户是否存在 | `SELECT * FROM user_info WHERE username = ?` |
| 密码是否正确 | 检查 BCrypt 加密是否匹配 |
| 账号是否禁用 | `status` 字段是否为 1 |

### 接口返回 701

**含义**：`TOKEN_INVALID` — Token 无效或缺失

| 场景 | 原因 | 修复 |
|------|------|------|
| 请求未带 Token | 前端 Axios 拦截器未注入 Header | 检查前端 `request.js` 的 Token 注入逻辑 |
| Token 已过期 | 超过 `sa-token.timeout` | 重新登录 |
| 端类型错误 | 用户端 Token 访问了管理端接口 | 使用正确的登录接口 |

### 接口返回 703

**含义**：`PERMISSION_DENIED` — 权限不足

| 场景 | 原因 |
|------|------|
| 用户端账号访问 `/api/admin/` | 管理端接口需要 `@RequireAdmin` |
| 管理端账号无 admin 角色 | 检查 `sys_admin` 表的 role 字段 |
| 资源不属于当前用户 | 用户端资源归属校验失败 |

### Sa-Token 会话异常

**症状**：登录后偶尔掉线、Token 频繁失效

```bash
# 检查 Redis 中 Sa-Token 数据
redis-cli -n 4 keys "satoken:*"

# 检查 Token 配置
# application.yml → sa-token.timeout / sa-token.activity-timeout
```

| 原因 | 修复 |
|------|------|
| Redis 内存不足 | 检查 `redis-cli info memory` |
| activity-timeout 过短 | 增大 `sa-token.activity-timeout` |
| 多端 Token 冲突 | 确认 User/Admin 端使用不同 Stp |

## 数据库问题

### MyBatis 查询报错

**症状**：`Invalid bound statement` 或 SQL 语法错误

| 原因 | 修复 |
|------|------|
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

日志输出到 `logs/spy.log`。

### 数据不一致

**症状**：数据显示异常、统计数据不准

```sql
-- 检查逻辑删除数据
SELECT COUNT(*) FROM user_info WHERE is_deleted = 1;

-- 检查状态异常数据
SELECT * FROM user_info WHERE status NOT IN (0, 1);

-- 检查 OJ 提交卡住
SELECT * FROM oj_submission WHERE status IN (0, 1) -- PENDING/JUDGING
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
|------|------|
| 构建产物为空 | `npm run build` 检查是否有编译错误 |
| 路由配置错误 | 检查 Vue Router 配置 |
| API 请求失败 | 浏览器 DevTools → Network 查看请求状态 |
| CORS 拦截 | 后端 CorsConfig 添加前端 Origin |

### 接口 CORS 报错

**症状**：浏览器控制台 `Access-Control-Allow-Origin` 错误

| 检查项 | 方法 |
|--------|------|
| 后端 CorsConfig | 确认前端 Origin 在白名单中 |
| Nginx 配置 | 检查 `add_header Access-Control-Allow-Origin` |
| Vite 代理 | 开发环境使用 `vite.config.js` 代理避免 CORS |

### 前端构建失败

```bash
# 清理缓存重新构建
rm -rf node_modules package-lock.json
npm install
npm run build
```

## AI 功能问题

### AI 接口调用失败

| 原因 | 修复 |
|------|------|
| API Key 未配置 | 检查 `application-sec.yml` 或环境变量 |
| API Base URL 错误 | 确认 `xiaou.ai.base-url` |
| 模型名称不匹配 | 确认 `xiaou.ai.model` |
| 网络不通 | 检查代理设置或防火墙 |

### Prompt 注册失败

| 原因 | 修复 |
|------|------|
| Prompt 目录不存在 | 检查 `resources/prompts/` 目录 |
| YAML 格式错误 | 检查 Prompt 定义文件语法 |
| Schema 与输出不匹配 | 确认 Schema 定义与实际输出字段一致 |

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
|------|------|
| go-judge 服务不可达 | 检查沙箱服务配置和连通性 |
| 执行超时 | 检查题目 `time_limit` 设置是否合理 |
| 内存溢出 | 检查 `memory_limit` 设置 |

## 文件上传问题

### 上传失败 801-805

| 错误码 | 含义 | 修复 |
|--------|------|------|
| 801 | 文件为空 | 检查上传文件是否正确 |
| 802 | 文件过大 | 调整 `spring.servlet.multipart.max-file-size` |
| 803 | 文件类型不支持 | 检查白名单配置 |
| 804 | 存储空间不足 | 检查磁盘空间 |
| 805 | 上传路径不合法 | 检查文件名是否包含特殊字符 |

### 文件无法访问

| 原因 | 修复 |
|------|------|
| LocalFileResourceConfig 未映射 | 确认 `addResourceHandlers` 中的路径映射 |
| 文件不存在 | 检查文件是否已存储到磁盘 |
| 权限不足 | 检查文件系统读写权限 |

## 日志与监控

### 查看后端日志

```bash
# 实时查看日志
tail -f logs/code-nest.log

# 搜索异常
grep -i "exception" logs/code-nest.log | tail -20

# 搜索特定请求
grep "POST /api/oj" logs/code-nest.log
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

| 端点 | 用途 |
|------|------|
| `/actuator/health` | 健康检查 |
| `/actuator/info` | 应用信息 |
| `/actuator/prometheus` | Prometheus 指标 |
| `/actuator/metrics` | 应用度量 |

## 问题升级

如果上述排查无法解决：

1. 搜索 [GitHub Issues](https://github.com/your-org/code-nest/issues) 是否有类似问题
2. 在项目内部讨论组提问，附上：错误日志、复现步骤、环境信息
3. 严重问题按 [事件响应流程](/operations/incident-response) 处理
