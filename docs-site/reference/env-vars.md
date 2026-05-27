# 环境变量与配置索引

本页列出 Code Nest 后端所有可通过环境变量覆盖的配置项，以及各 profile 的差异。前端项目的环境变量见各 `vue3-*-front/` 目录下的 `.env*` 文件。

## 配置加载顺序

```text
application.yml                    ← 基础配置（port、MyBatis、Sa-Token 公共项、AI、社区、OJ）
  └─ application-{profile}.yml    ← profile 覆盖（MySQL/Redis 地址、凭据）
       └─ application-sec.yml     ← 密钥覆盖（JWT secret、AI API key 等）
```

当前默认 profile 为 `dev`。激活方式：

```bash
# 开发
java -jar app.jar

# Docker
java -jar app.jar --spring.profiles.active=docker

# 生产
java -jar app.jar --spring.profiles.active=prod

# 安全密钥
java -jar app.jar --spring.profiles.active=prod,sec
```

## 环境变量完整表

### 数据库与 Redis

| 变量名 | 对应配置 | 默认值（Docker profile） | 说明 |
| --- | --- | --- | --- |
| `XIAOU_MYSQL_URL` | `spring.datasource.url` | `jdbc:mysql://mysql:3306/code_nest?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf-8` | 含主机、端口、库名、时区、字符集 |
| `XIAOU_MYSQL_USERNAME` | `spring.datasource.username` | `root` | MySQL 用户名 |
| `XIAOU_MYSQL_PASSWORD` | `spring.datasource.password` | `123456` | **生产环境必须覆盖** |
| `XIAOU_REDIS_ADDRESS` | `spring.data.redis.redisson.config` → `address` | `redis://redis:6379` | Redisson 连接地址 |
| `XIAOU_REDIS_DATABASE` | `spring.data.redis.redisson.config` → `database` | `3` | 业务数据 Redis DB |
| `XIAOU_SA_TOKEN_REDIS_HOST` | `sa-token.alone-redis.host` | `redis` | Sa-Token 独立 Redis 主机 |
| `XIAOU_SA_TOKEN_REDIS_PORT` | `sa-token.alone-redis.port` | `6379` | Sa-Token 独立 Redis 端口 |
| `XIAOU_SA_TOKEN_REDIS_DATABASE` | `sa-token.alone-redis.database` | `4` | Sa-Token Redis DB（与业务 DB 隔离） |
| `XIAOU_SA_TOKEN_REDIS_TIMEOUT` | `sa-token.alone-redis.timeout` | `10000` | Sa-Token Redis 连接超时（ms） |
| `XIAOU_SA_TOKEN_REDIS_POOL_MAX_ACTIVE` | `sa-token.alone-redis.jedis.pool.max-active` | `8` | 连接池最大连接数 |
| `XIAOU_SA_TOKEN_REDIS_POOL_MAX_IDLE` | `sa-token.alone-redis.jedis.pool.max-idle` | `8` | 连接池最大空闲 |
| `XIAOU_SA_TOKEN_REDIS_POOL_MIN_IDLE` | `sa-token.alone-redis.jedis.pool.min-idle` | `0` | 连接池最小空闲 |
| `XIAOU_SA_TOKEN_REDIS_POOL_MAX_WAIT` | `sa-token.alone-redis.jedis.pool.max-wait` | `3000` | 连接池最大等待（ms） |

### JWT / Sa-Token 认证

| 变量名 | 对应配置 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `XIAOU_JWT_SECRET` | `jwt.secret` | `change-me-code-nest-jwt-secret-at-least-64-bytes-for-dev-only-2026` | **生产环境必须覆盖**，HS512 要求 ≥64 字节 |

> 注意：Sa-Token 的 `token-name`、`timeout`、`activity-timeout` 等配置当前在 `application.yml` 中硬编码，不支持环境变量覆盖。如需调整，需修改 YAML 或新增 `@Value` 入口。

### AI 模块

| 变量名 | 对应配置 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `XIAOU_AI_PROVIDER` | `xiaou.ai.provider` | `openai-compatible` | AI 提供商标识 |
| `XIAOU_AI_BASE_URL` | `xiaou.ai.base-url` | 空 | AI API 基础 URL |
| `XIAOU_AI_API_KEY` | `xiaou.ai.api-key` | 空 | **生产环境必须覆盖** |
| `XIAOU_AI_CHAT_MODEL` | `xiaou.ai.model.chat` | `gpt-5.4` | Chat 模型名 |
| `XIAOU_AI_EMBEDDING_MODEL` | `xiaou.ai.model.embedding` | `text-embedding-3-small` | Embedding 模型名 |
| `XIAOU_AI_PRICING_CURRENCY` | `xiaou.ai.pricing.currency` | `USD` | 定价货币 |
| `XIAOU_AI_INPUT_PRICE_PER_MILLION` | `xiaou.ai.pricing.input-per-million` | `0` | 输入单价（每百万 token） |
| `XIAOU_AI_OUTPUT_PRICE_PER_MILLION` | `xiaou.ai.pricing.output-per-million` | `0` | 输出单价（每百万 token） |
| `XIAOU_AI_METRICS_PERSIST_ENABLED` | `xiaou.ai.metrics.persistence.enabled` | `true` | AI 指标持久化开关 |
| `XIAOU_AI_METRICS_REDIS_KEY` | `xiaou.ai.metrics.persistence.redis-key` | `xiaou:ai:runtime:metrics` | AI 指标 Redis Key |
| `XIAOU_AI_RAG_ENABLED` | `xiaou.ai.rag.enabled` | `false` | RAG 开关 |
| `XIAOU_AI_RAG_ENDPOINT` | `xiaou.ai.rag.endpoint` | `http://localhost:18080` | RAG 服务端点 |
| `XIAOU_AI_RAG_API_KEY` | `xiaou.ai.rag.api-key` | 空 | RAG API Key |

### @Value 注入的应用配置

以下配置通过 Java `@Value` 注入，支持环境变量覆盖但不在 YAML 中声明默认值：

| 配置键 | 注入位置 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `server.port` | StartupApplicationListener | `8080` | `@Value` 回退默认值，实际由 `application.yml` 覆盖为 `9999`（dev）或 `8080`（Docker/prod） |
| `server.servlet.context-path` | StartupApplicationListener | `/api` | 启动日志读取上下文路径 |
| `spring.profiles.active` | StartupApplicationListener | `default` | 启动日志读取 profile |
| `xiaou.cors.allowed-origin-patterns` | CorsConfig / WebSocketConfig | `http://localhost:3000,http://localhost:3001,...` | CORS 允许的来源 |
| `xiaou.chat.rate-limit.enabled` | ChatRateLimitService | `true` | 聊天限流开关 |
| `xiaou.chat.rate-limit.message-limit` | ChatRateLimitService | `8` | 消息限流阈值 |
| `xiaou.chat.rate-limit.message-window-seconds` | ChatRateLimitService | `10` | 消息限流窗口 |
| `xiaou.chat.rate-limit.typing-limit` | ChatRateLimitService | `12` | 输入中限流阈值 |
| `xiaou.chat.rate-limit.typing-window-seconds` | ChatRateLimitService | `10` | 输入中限流窗口 |
| `xiaou.chat.message.max-text-length` | ChatMessageServiceImpl | `1000` | 消息文本最大长度 |
| `xiaou.chat.message.max-image-url-length` | ChatMessageServiceImpl | `1024` | 图片 URL 最大长度 |
| `xiaou.sensitive.source-sync.max-retries` | SensitiveSourceSyncScheduler | `2` | 同步最大重试 |
| `xiaou.sensitive.source-sync.retry-delay-ms` | SensitiveSourceSyncScheduler | `2000` | 重试间隔（ms） |
| `xiaou.sensitive.source-sync.alert-threshold` | SensitiveSourceSyncScheduler | `3` | 告警阈值 |
| `xiaou.sensitive.source-sync.alert-webhook` | SensitiveSourceSyncScheduler | 空 | 告警 Webhook URL |
| `hot-topic.api.base-url` | HotTopicServiceImpl | `http://113.44.190.45:9996/api` | 热榜 API 地址 |

## Profile 差异对比

| 配置项 | dev | docker | prod |
| --- | --- | --- | --- |
| 服务端口 | 9999 | 8080（Dockerfile） | 8080 |
| MySQL 驱动 | P6Spy 代理 | 原生驱动 | 原生驱动 |
| MySQL 主机 | `localhost:3306` | `mysql:3306` | 需覆盖 |
| MySQL 密码 | `1234` | `123456` | 需覆盖 |
| Redis 主机 | `127.0.0.1` | `redis` | 需覆盖 |
| 业务 Redis DB | 3 | 3 | 3 |
| Sa-Token Redis DB | 4 | 4 | 4 |
| Spring Security | 禁用默认用户 | 禁用默认用户 | 禁用默认用户 |
| SQL 日志 | P6Spy 接管 | 无 P6Spy | 无 P6Spy |

## 生产部署必须覆盖的变量

以下变量在默认配置中使用弱密码或占位值，**上线前必须通过环境变量或 `application-sec.yml` 覆盖**：

| 变量 | 风险等级 | 说明 |
| --- | --- | --- |
| `XIAOU_JWT_SECRET` | **高** | 默认值硬编码在 YAML 中，攻击者可伪造 Token |
| `XIAOU_MYSQL_PASSWORD` | **高** | Docker 默认 `123456`，dev 默认 `1234` |
| `XIAOU_AI_API_KEY` | **高** | 默认为空，AI 功能不可用 |
| `XIAOU_MYSQL_URL` | 中 | 生产 MySQL 地址需覆盖 |
| `XIAOU_REDIS_ADDRESS` | 中 | 生产 Redis 地址需覆盖 |
| `XIAOU_SA_TOKEN_REDIS_HOST` | 中 | 生产 Sa-Token Redis 地址需覆盖 |
| `XIAOU_AI_RAG_API_KEY` | 中 | RAG 功能需 API Key |

## 安全密钥文件（application-sec.yml）

`application-sec.yml` 在 `.gitignore` 中被排除，不会被提交到仓库。它通过 `spring.config.import` 加载：

```yaml
# application-sec.yml 示例
xiaou:
  ai:
    provider: openai-compatible
    base-url: https://your-ai-endpoint/v1
    api-key: sk-your-real-key
    model:
      chat: gpt-5.4
```

> **不要把真实密钥写在 `application-dev.yml` 或 `application-docker.yml` 中**。这些文件会进入 Git 仓库。

## 新增配置项规则

1. 需要环境变量覆盖的配置：在 YAML 中使用 `${ENV_VAR:default}` 语法。
2. 仅需硬编码默认值的配置：直接写在 `application.yml` 或 profile 文件中。
3. 敏感配置（密码、密钥）：放在 `application-sec.yml`，不提交到仓库。
4. 新增 `@Value` 注入时：同步更新本页的"@Value 注入的应用配置"表。

## 源码导航

| 文件 | 说明 |
| --- | --- |
| `xiaou-application/src/main/resources/application.yml` | 基础配置（端口、Sa-Token、AI、社区、OJ） |
| `xiaou-application/src/main/resources/application-dev.yml` | 开发环境 MySQL/Redis |
| `xiaou-application/src/main/resources/application-docker.yml` | Docker 环境 MySQL/Redis |
| `xiaou-application/src/main/resources/application-prod.yml` | 生产占位（需覆盖） |
| `xiaou-application/src/main/resources/application-sec.yml` | 安全密钥（不提交） |
| `xiaou-common/src/main/java/**/config/CorsConfig.java` | CORS @Value 注入 |
| `xiaou-chat/.../config/WebSocketConfig.java` | WebSocket CORS @Value |
| `xiaou-chat/.../service/ChatRateLimitService.java` | 限流 @Value 注入 |
| `xiaou-chat/.../service/impl/ChatMessageServiceImpl.java` | 消息限制 @Value |
| `xiaou-sensitive/.../scheduler/SensitiveSourceSyncScheduler.java` | 同步 @Value |
| `xiaou-moyu/.../service/impl/HotTopicServiceImpl.java` | 热榜 API @Value |


## 相关文档

| 文档 | 说明 |
| --- | --- |
| [Docker 与服务部署](/operations/docker) | 部署配置 |
| [本地完整启动剧本](/guide/startup-playbook) | 本地启动配置 |
| [架构总览](/architecture/overview) | 技术栈说明 |
| [独立部署](/guide/deploy) | 生产环境部署 |
