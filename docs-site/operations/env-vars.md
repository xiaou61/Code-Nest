# 环境变量总表

这页列出 Code Nest 后端所有可通过环境变量覆盖的配置项。所有默认值来自源码中的 `application.yml` 和 `application-docker.yml`，可直接对照使用。

如果你在本地开发，大部分变量不需要设置，`application-dev.yml` 已有 localhost 默认值。如果你用 Docker 或生产部署，所有敏感配置和外部依赖地址都应通过环境变量注入。

## 通用运行时

| 变量 | 默认值 | 来源 | 说明 |
| --- | --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | `dev` | Spring Boot | 激活的 Spring Profile，Docker 下设为 `docker` |
| `JAVA_OPTS` | 空 | Dockerfile | JVM 参数，如 `-Xms512m -Xmx1024m` |
| `TZ` | `Asia/Shanghai` | Dockerfile | 容器时区 |
| `server.port` | `9999` | `application.yml` | 后端服务端口，一般不修改 |

## 数据库

| 变量 | 默认值 | 来源 | 说明 |
| --- | --- | --- | --- |
| `XIAOU_MYSQL_URL` | `jdbc:mysql://localhost:3306/code_nest?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf-8` | `application-dev.yml` | MySQL JDBC URL |
| `XIAOU_MYSQL_USERNAME` | `root` | `application-docker.yml` | MySQL 用户名 |
| `XIAOU_MYSQL_PASSWORD` | `1234` | `application-dev.yml` | MySQL 密码，生产必须覆盖 |
| `SPRING_DATASOURCE_URL` | 同 `XIAOU_MYSQL_URL` | Spring Boot 标准 | Spring Boot 标准变量，优先级高于自定义变量 |
| `SPRING_DATASOURCE_USERNAME` | 同 `XIAOU_MYSQL_USERNAME` | Spring Boot 标准 | Spring Boot 标准变量 |
| `SPRING_DATASOURCE_PASSWORD` | 同 `XIAOU_MYSQL_PASSWORD` | Spring Boot 标准 | Spring Boot 标准变量 |

> **注意**：`application-dev.yml` 使用 P6Spy 代理驱动，URL 前缀是 `jdbc:p6spy:mysql://`。`application-docker.yml` 不使用 P6Spy，URL 前缀是 `jdbc:mysql://`。

## Redis

| 变量 | 默认值 | 来源 | 说明 |
| --- | --- | --- | --- |
| `XIAOU_REDIS_ADDRESS` | `redis://127.0.0.1:6379` | `application-docker.yml` | 业务 Redis 地址（Redisson），使用 db3 |
| `XIAOU_REDIS_PASSWORD` | 空 | `application-docker.yml` | 业务 Redis 密码 |
| `XIAOU_REDIS_DATABASE` | `3` | `application.yml` | 业务数据使用 db3 |
| `XIAOU_SATOKEN_REDIS_HOST` | `127.0.0.1` | `application-dev.yml` | Sa-Token alone-redis 主机 |
| `XIAOU_SATOKEN_REDIS_PORT` | `6379` | `application-dev.yml` | Sa-Token alone-redis 端口 |
| `XIAOU_SATOKEN_REDIS_DATABASE` | `4` | `application-dev.yml` | Sa-Token 登录态使用 db4 |

Redis DB 编号分配：

| DB | 用途 | 配置位置 |
| --- | --- | --- |
| db3 | 业务缓存（接口缓存、验证码、Ticket、Redisson） | `spring.data.redis.redisson.config` |
| db4 | Sa-Token 会话（用户端 + 管理端登录态） | `sa-token.alone-redis.database` |

> **重要**：两个 Redis DB 必须隔离。如果共用同一个 DB，排障时无法区分业务缓存和登录数据，Sa-Token 的 key 还可能被业务缓存误覆盖。

## Sa-Token

| 变量 | 默认值 | 来源 | 说明 |
| --- | --- | --- | --- |
| `XIAOU_JWT_SECRET` | 开发默认占位值 | `application.yml` | JWT 签名密钥，生产必须覆盖 |
| `XIAOU_SATOKEN_TOKEN_NAME` | `Authorization` | `application.yml` | Token Header 名称 |
| `XIAOU_SATOKEN_TIMEOUT` | `604800` | `application.yml` | Token 有效期（秒），默认 7 天 |

Sa-Token 公共配置（`application.yml`）：

| 配置 | 值 | 说明 |
| --- | --- | --- |
| `token-name` | `Authorization` | 前端提交 Header 名 |
| `timeout` | `604800` | Token 有效期 7 天（秒） |
| `activity-timeout` | `-1` | 不启用临时过期 |
| `is-concurrent` | `true` | 允许同一账号并发登录 |
| `is-share` | `false` | 每次登录新建 Token |
| `token-style` | `uuid` | Token 格式 |
| `is-read-cookie` | `false` | 不从 Cookie 读取 |
| `is-read-header` | `true` | 从请求头读取 |
| `token-prefix` | `Bearer` | 前端提交时加 Bearer 前缀 |

前端请求时 Header 格式：`Authorization: Bearer <token>`

## AI 配置

| 变量 | 默认值 | 来源 | 说明 |
| --- | --- | --- | --- |
| `XIAOU_AI_PROVIDER` | `openai-compatible` | `application.yml` | AI Provider 类型 |
| `XIAOU_AI_BASE_URL` | 空 | `application.yml` | AI API Base URL，必填 |
| `XIAOU_AI_API_KEY` | 空 | `application.yml` | AI API Key，必填 |
| `XIAOU_AI_CHAT_MODEL` | `gpt-5.4` | `application.yml` | 聊天模型名称 |
| `XIAOU_AI_EMBEDDING_MODEL` | `text-embedding-3-small` | `application.yml` | Embedding 模型名称 |
| `XIAOU_AI_TIMEOUT_CONNECT_MS` | `10000` | `application.yml` | 连接超时 ms |
| `XIAOU_AI_TIMEOUT_READ_MS` | `60000` | `application.yml` | 读取超时 ms |
| `XIAOU_AI_RETRY_MAX_ATTEMPTS` | `2` | `application.yml` | 最大重试次数 |
| `XIAOU_AI_RETRY_BACKOFF_MS` | `1000` | `application.yml` | 重试间隔 ms |

## RAG 配置

| 变量 | 默认值 | 来源 | 说明 |
| --- | --- | --- | --- |
| `XIAOU_AI_RAG_ENABLED` | `false` | `application.yml` | 是否启用 RAG |
| `XIAOU_AI_RAG_ENDPOINT` | `http://localhost:18080` | `application.yml` | RAG sidecar 地址 |
| `XIAOU_AI_RAG_API_KEY` | 空 | `application.yml` | RAG sidecar API Key |

RAG 默认关闭。开启后需要同时启动 `llamaindex-service` 容器，并确保 `XIAOU_AI_RAG_ENDPOINT` 指向正确地址。

如果 RAG 不可用，AI 功能会降级返回，不影响其他业务。

## OJ 判题

| 变量 | 默认值 | 来源 | 说明 |
| --- | --- | --- | --- |
| `XIAOU_OJ_GO_JUDGE_URL` | `http://154.222.18.220:5050` | `application.yml` | go-judge 沙箱地址 |
| `XIAOU_OJ_MAX_COMPILE_TIME` | `10000` | `application.yml` | 最大编译时间 ms |
| `XIAOU_OJ_DEFAULT_TIME_LIMIT` | `2000` | `application.yml` | 默认运行时间限制 ms |
| `XIAOU_OJ_DEFAULT_MEMORY_LIMIT` | `256` | `application.yml` | 默认内存限制 MB |

> **注意**：默认 `go-judge-url` 指向远端示例地址。本地部署 go-judge 时需要覆盖为 `http://go-judge:5050`（Docker 内部）或 `http://127.0.0.1:5050`（宿主机）。

## 文件存储

| 变量 | 默认值 | 来源 | 说明 |
| --- | --- | --- | --- |
| `XIAOU_FILE_STORAGE_TYPE` | `local` | `application.yml` | 文件存储类型：`local`、`minio`、`s3` |
| `XIAOU_FILE_LOCAL_UPLOAD_DIR` | `./uploads` | `application.yml` | 本地存储上传目录 |
| `XIAOU_FILE_LOCAL_BASE_URL` | `http://localhost:9999` | `application.yml` | 本地存储访问基础 URL |
| `XIAOU_FILE_MINIO_ENDPOINT` | 空 | `application.yml` | MinIO endpoint |
| `XIAOU_FILE_MINIO_ACCESS_KEY` | 空 | `application.yml` | MinIO Access Key |
| `XIAOU_FILE_MINIO_SECRET_KEY` | 空 | `application.yml` | MinIO Secret Key |
| `XIAOU_FILE_MINIO_BUCKET` | 空 | `application.yml` | MinIO Bucket |
| `spring.servlet.multipart.max-file-size` | `100MB` | `application.yml` | 单文件最大大小 |
| `spring.servlet.multipart.max-request-size` | `100MB` | `application.yml` | 请求最大大小 |

## CORS 配置

| 变量 | 默认值 | 来源 | 说明 |
| --- | --- | --- | --- |
| `XIAOU_CORS_ALLOWED_ORIGIN_PATTERNS` | `http://localhost:3000,http://localhost:3001,http://localhost:5173,http://localhost:5175` | `application.yml` | CORS 允许的前端域名；5173 为旧版 Vite 默认端口仍保留在代码回退值中，5175 为文档站开发端口 |

这个配置同时影响：
- 普通 HTTP 接口的 CORS 响应头
- 聊天室 WebSocket 握手的 Origin 校验

生产配置示例：

```text
XIAOU_CORS_ALLOWED_ORIGIN_PATTERNS=https://www.example.com,https://admin.example.com
```

> **重要**：这里要填浏览器页面所在的前端域名，不是后端 API 域名。如果用户端和管理端分别在不同域名，两个域名都要写进去。

## Actuator 与监控

| 变量 | 默认值 | 来源 | 说明 |
| --- | --- | --- | --- |
| `management.endpoints.web.exposure.include` | `health,info,prometheus` | `application.yml` | 暴露的 Actuator 端点 |
| `management.endpoints.web.base-path` | `/actuator` | Spring Boot | Actuator 基础路径（context-path 下） |
| `management.server.port` | 同 `server.port` | Spring Boot | Actuator 端口，默认和主服务共用 |

Actuator 端点地址：

| 端点 | 地址 | 说明 |
| --- | --- | --- |
| Health | `http://localhost:9999/api/actuator/health` | 健康状态 |
| Info | `http://localhost:9999/api/actuator/info` | 应用信息 |
| Prometheus | `http://localhost:9999/api/actuator/prometheus` | Prometheus 指标 |

> **注意**：所有 Actuator 端点都在 context-path `/api` 下，完整路径是 `/api/actuator/...`。

## Docker 专用变量

以下变量在 `application-docker.yml` 中使用，本地开发不需要设置：

| 变量 | 说明 | Docker Compose 中如何设置 |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | 设为 `docker` | Dockerfile 中 `ENV SPRING_PROFILES_ACTIVE=docker` |
| `SPRING_DATASOURCE_URL` | MySQL URL | `jdbc:mysql://mysql:3306/code_nest?...`（Docker 内部网络用服务名 `mysql`） |
| `SPRING_DATASOURCE_USERNAME` | MySQL 用户名 | 通常 `root` |
| `SPRING_DATASOURCE_PASSWORD` | MySQL 密码 | 通过 `.env` 注入 |
| `SPRING_DATA_REDIS_REDICON_CONFIG` | Redisson 配置 | `singleServerConfig.address: "redis://redis:6379"`（Docker 内部网络用服务名 `redis`） |
| `SA_TOKEN_ALONE_REDIS_HOST` | Sa-Token Redis 主机 | `redis`（Docker 内部网络用服务名） |

Docker 内部网络中，服务间用 Compose 服务名互访（如 `mysql:3306`、`redis:6379`）。如果后端需要访问宿主机上的服务，用 `host.docker.internal`。

## 完整 `.env` 示例

以下是一个用于 Docker 部署的 `.env` 文件模板：

```text
# ---- Spring Profile ----
SPRING_PROFILES_ACTIVE=docker

# ---- MySQL ----
SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/code_nest?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf-8
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=your_mysql_password

# ---- Redis ----
XIAOU_REDIS_ADDRESS=redis://redis:6379
XIAOU_REDIS_PASSWORD=

# ---- AI ----
XIAOU_AI_PROVIDER=openai-compatible
XIAOU_AI_BASE_URL=https://your-openai-proxy.example.com/v1
XIAOU_AI_API_KEY=sk-your-api-key
XIAOU_AI_CHAT_MODEL=gpt-4o
XIAOU_AI_EMBEDDING_MODEL=text-embedding-3-small

# ---- RAG ----
XIAOU_AI_RAG_ENABLED=true
XIAOU_AI_RAG_ENDPOINT=http://llamaindex-service:18080

# ---- JWT ----
XIAOU_JWT_SECRET=your-jwt-secret-at-least-32-characters

# ---- CORS ----
XIAOU_CORS_ALLOWED_ORIGIN_PATTERNS=http://localhost:3000,http://localhost:3001

# ---- OJ ----
XIAOU_OJ_GO_JUDGE_URL=http://go-judge:5050

# ---- JVM ----
JAVA_OPTS=-Xms512m -Xmx1024m
```

## 安全清单

| 变量 | 不安全的默认值 | 生产必须 |
| --- | --- | --- |
| `XIAOU_MYSQL_PASSWORD` / `SPRING_DATASOURCE_PASSWORD` | `1234` | 改为强密码 |
| `XIAOU_JWT_SECRET` | 开发占位值 | 改为至少 32 字符随机密钥 |
| `XIAOU_AI_API_KEY` | 空 | 填写真实 API Key |
| `XIAOU_CORS_ALLOWED_ORIGIN_PATTERNS` | 包含 localhost | 只保留真实前端域名 |
| `XIAOU_REDIS_PASSWORD` | 空 | Redis 如果暴露外网则必须设密码 |

## 变量优先级

Spring Boot 配置优先级从高到低：

1. 命令行参数（`--key=value`）
2. 环境变量（`SPRING_DATASOURCE_URL` 等）
3. `application-docker.yml` / `application-prod.yml`
4. `application-dev.yml`
5. `application.yml`

同一配置项，环境变量覆盖 YAML 文件中的值。`application-docker.yml` 中的 `${VARIABLE:default}` 语法表示：如果环境变量 `VARIABLE` 存在则使用其值，否则使用 `default`。

## 源码对照

| 文件 | 覆盖的变量域 |
| --- | --- |
| `xiaou-application/src/main/resources/application.yml` | 主配置、Sa-Token、AI、OJ、文件存储、CORS、Actuator |
| `xiaou-application/src/main/resources/application-dev.yml` | P6Spy、localhost MySQL/Redis、Sa-Token alone-redis |
| `xiaou-application/src/main/resources/application-docker.yml` | Docker 环境变量注入模板 |
| `xiaou-application/src/main/resources/application-prod.yml` | 生产占位配置 |
| `xiaou-application/src/main/resources/application-sec.yml` | 敏感配置（密钥、Token），不提交仓库 |
| `docker/ai/.env.example` | AI 联调 Compose 环境变量模板 |
| `docker/env/example.env` | Docker 运行环境变量模板 |

更多部署细节见 [Docker 与服务部署](/operations/docker) 和 [独立部署](/guide/deploy)。