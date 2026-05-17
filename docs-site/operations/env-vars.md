# 环境变量总表

这页汇总 Code Nest 本地、Docker、AI、OJ、前端和文档站常用环境变量。它的作用是让部署者知道“变量从哪里来、控制什么、什么时候必须改”。

相关源码和示例文件：

| 路径 | 说明 |
| --- | --- |
| `xiaou-application/src/main/resources/application.yml` | 后端通用配置，包含 JWT、AI、RAG、监控、OJ 默认值 |
| `xiaou-application/src/main/resources/application-dev.yml` | 本地开发数据库和 Redis 默认配置 |
| `xiaou-application/src/main/resources/application-docker.yml` | Docker profile 的数据库、Redis、Sa-Token Redis 环境变量 |
| `docker/env/example.env` | 后端容器运行示例 |
| `docker/ai/.env.example` | AI compose 联调示例 |
| `docker/ai/docker-compose.yml` | AI、MySQL、Redis、RAG、后端组合编排 |
| `docker/go-judge/docker-compose.yml` | OJ 判题沙箱变量 |
| `docker/monitoring/docker-compose.yml` | Prometheus 和 Grafana 变量 |

生产环境不要直接使用示例里的 `123456`、`sk-xxxx` 或开发默认密钥。

## Profile 与启动

| 变量 | 默认或示例 | 作用 | 什么时候改 |
| --- | --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | `dev`、Docker 示例为 `docker` | 决定加载 `application-dev.yml` 还是 `application-docker.yml` | 容器部署时设为 `docker` |
| `JAVA_OPTS` | 空 | 传给 `java -jar` 的 JVM 参数 | 需要设置内存、GC、时区或诊断参数时 |

示例：

```text
SPRING_PROFILES_ACTIVE=docker
JAVA_OPTS=-Xms512m -Xmx1024m -Duser.timezone=Asia/Shanghai
```

## 数据库

Docker profile 使用这些变量：

| 变量 | 默认或示例 | 作用 | 来源 |
| --- | --- | --- | --- |
| `XIAOU_MYSQL_URL` | `jdbc:mysql://mysql:3306/code_nest?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf-8` | 后端 JDBC URL | `application-docker.yml` |
| `XIAOU_MYSQL_USERNAME` | `root` | MySQL 用户名 | `application-docker.yml` |
| `XIAOU_MYSQL_PASSWORD` | `123456` | MySQL 密码 | `application-docker.yml` |
| `MYSQL_ROOT_PASSWORD` | `123456` | AI compose 内 MySQL root 密码 | `docker/ai/.env.example` |
| `MYSQL_DATABASE` | `code_nest` | AI compose 首次初始化数据库名 | `docker/ai/.env.example` |

本地开发 `application-dev.yml` 默认直连：

```text
jdbc:p6spy:mysql://localhost:3306/code_nest
```

开发环境使用 P6Spy 便于 SQL 日志观察，Docker profile 使用标准 MySQL driver。

## Redis 与 Sa-Token Redis

项目把业务 Redis 和 Sa-Token Redis 分开配置。默认 database 也不同。

| 变量 | 默认或示例 | 作用 |
| --- | --- | --- |
| `XIAOU_REDIS_ADDRESS` | `redis://redis:6379` | Redisson 业务 Redis 地址 |
| `XIAOU_REDIS_DATABASE` | `3` | 业务 Redis database |
| `XIAOU_SA_TOKEN_REDIS_HOST` | `redis` | Sa-Token 独立 Redis host |
| `XIAOU_SA_TOKEN_REDIS_PORT` | `6379` | Sa-Token Redis 端口 |
| `XIAOU_SA_TOKEN_REDIS_DATABASE` | `4` | Sa-Token Redis database |
| `XIAOU_SA_TOKEN_REDIS_TIMEOUT` | `10000` | 连接超时毫秒 |
| `XIAOU_SA_TOKEN_REDIS_POOL_MAX_ACTIVE` | `8` | Jedis 池最大连接数 |
| `XIAOU_SA_TOKEN_REDIS_POOL_MAX_IDLE` | `8` | 最大空闲连接 |
| `XIAOU_SA_TOKEN_REDIS_POOL_MIN_IDLE` | `0` | 最小空闲连接 |
| `XIAOU_SA_TOKEN_REDIS_POOL_MAX_WAIT` | `3000` | 最大阻塞等待毫秒 |

注意：

1. 业务缓存和登录态不要混同一个 database，排障时更容易定位。
2. 如果 Redis 重启且没有持久化，Sa-Token 登录态会丢失。
3. 容器里通常写 `redis`，宿主机本地开发通常写 `127.0.0.1`。

## 鉴权、CORS 与 WebSocket

| 变量 | 默认或示例 | 作用 |
| --- | --- | --- |
| `XIAOU_JWT_SECRET` | 开发默认占位密钥 | 兼容旧 JWT 代码的签名密钥 |
| `XIAOU_CORS_ALLOWED_ORIGIN_PATTERNS` | 本地默认允许 `localhost:3000`、`3001`、`5173` 等 | HTTP CORS 和 WebSocket Origin 白名单 |

生产配置示例：

```text
XIAOU_CORS_ALLOWED_ORIGIN_PATTERNS=https://www.example.com,https://admin.example.com
```

这里要填浏览器页面所在的前端域名，不是后端 API 域名。聊天室 WebSocket 也使用同一组 Origin 白名单。

## AI Runtime

这些变量来自 `xiaou.ai.*` 配置。

| 变量 | 默认或示例 | 作用 |
| --- | --- | --- |
| `XIAOU_AI_PROVIDER` | `openai-compatible` | AI provider 类型 |
| `XIAOU_AI_BASE_URL` | 空或 `https://your-openai-proxy.example.com/v1` | OpenAI Compatible API base URL |
| `XIAOU_AI_API_KEY` | 空或 `sk-xxxx` | 模型 API Key |
| `XIAOU_AI_CHAT_MODEL` | `gpt-5.4` | 聊天模型 |
| `XIAOU_AI_EMBEDDING_MODEL` | `text-embedding-3-small` | Embedding 模型 |
| `XIAOU_AI_PRICING_CURRENCY` | `USD` | 成本估算币种 |
| `XIAOU_AI_INPUT_PRICE_PER_MILLION` | `0` | 输入 token 百万价格 |
| `XIAOU_AI_OUTPUT_PRICE_PER_MILLION` | `0` | 输出 token 百万价格 |
| `XIAOU_AI_METRICS_PERSIST_ENABLED` | `true` | AI Runtime 指标是否持久化到 Redis |
| `XIAOU_AI_METRICS_REDIS_KEY` | `xiaou:ai:runtime:metrics` | 指标持久化 Redis key |

如果只想先启动主业务，可以保持 AI Key 为空，但 AI 调试、模拟面试、SQL 优化、RAG 相关链路不会完整可用。

## RAG Sidecar

| 变量 | 默认或示例 | 作用 | 运行位置 |
| --- | --- | --- | --- |
| `XIAOU_AI_RAG_ENABLED` | `false`，AI compose 示例为 `true` | 后端是否启用 RAG | 后端 |
| `XIAOU_AI_RAG_ENDPOINT` | `http://localhost:18080` 或 `http://llamaindex-service:18080` | RAG sidecar 地址 | 后端 |
| `XIAOU_AI_RAG_API_KEY` | 空或 `rag-local-key` | 后端访问 sidecar 的 key | 后端和 compose |
| `LLAMAINDEX_SERVICE_API_KEY` | 取自 `XIAOU_AI_RAG_API_KEY` | sidecar 自己的鉴权 key | `llamaindex-service` |
| `LLAMAINDEX_DATA_FILE` | `/app/data/knowledge-base.json` | sidecar 知识库数据文件 | `llamaindex-service` |

本地直跑后端时，RAG endpoint 通常是：

```text
XIAOU_AI_RAG_ENDPOINT=http://localhost:18080
```

Docker compose 内后端访问 sidecar 时，endpoint 应该是：

```text
XIAOU_AI_RAG_ENDPOINT=http://llamaindex-service:18080
```

## OJ 判题沙箱

后端当前配置项是 `oj.judge.go-judge-url`。在 `application.yml` 中默认指向远端示例地址，开发和生产建议显式覆盖到自己的 go-judge。

| 变量或配置 | 默认或示例 | 作用 |
| --- | --- | --- |
| `oj.judge.go-judge-url` | `http://154.222.18.220:5050` | 后端访问 go-judge 的地址 |
| `GOJUDGE_PARALLELISM` | `4` | go-judge 并发执行数量 |
| `GOJUDGE_FILE_STORE` | `/tmp/gojudge` | go-judge 文件存储路径 |

`docker/go-judge/docker-compose.yml` 还限制了内存 `1G`、CPU `2.0`，并要求 `privileged: true`。生产环境建议把判题沙箱和主后端隔离部署。

## 前端与文档站

| 变量 | 默认或示例 | 作用 |
| --- | --- | --- |
| `VITE_WS_URL` | `ws://localhost:9999/api` | 用户端聊天室 WebSocket base URL |
| `BASE_URL` | Vite 内置 | Vue Router history base，通常由构建工具注入 |
| `ELECTRON_RENDERER_URL` | 开发时可用 | Electron 开发模式加载渲染进程 URL |
| `VITEPRESS_BASE` | `/` | 文档站部署子路径，对应 VitePress `base` |

普通 Web 部署时，前端接口请求通常不靠环境变量改后端地址，而是通过 Nginx 把 `/api` 代理到后端。

聊天室如果通过同域 Nginx 转发，常见入口是：

```text
wss://www.example.com/api/ws/chat?ticket=...
```

这时 `VITE_WS_URL` 应该和部署路径匹配。

## 监控

后端暴露：

```text
/api/actuator/health
/api/actuator/prometheus
```

监控 compose 里 Grafana 相关变量：

| 变量 | 默认或示例 | 作用 |
| --- | --- | --- |
| `GF_SECURITY_ADMIN_USER` | `admin` | Grafana 管理员账号 |
| `GF_SECURITY_ADMIN_PASSWORD` | `admin123` | Grafana 管理员密码 |
| `GF_AUTH_ANONYMOUS_ENABLED` | `false` | 是否允许匿名访问 |
| `TZ` | `Asia/Shanghai` | Grafana 容器时区 |

生产环境必须修改 Grafana 默认密码。

## 最小本地配置

只跑后端最小业务，通常准备：

```text
SPRING_PROFILES_ACTIVE=dev
```

并保证：

| 依赖 | 默认 |
| --- | --- |
| MySQL | `localhost:3306/code_nest`，用户 `root`，密码 `1234` |
| Redis | `127.0.0.1:6379` |
| 业务 Redis DB | `3` |
| Sa-Token Redis DB | `4` |

如果你不想使用 `application-dev.yml` 的默认密码，建议创建本地不提交的 `application-sec.yml` 覆盖敏感配置。

## AI compose 配置示例

复制：

```bash
cp docker/ai/.env.example docker/ai/.env
```

示例：

```text
MYSQL_ROOT_PASSWORD=change-me
MYSQL_DATABASE=code_nest

XIAOU_AI_PROVIDER=openai-compatible
XIAOU_AI_BASE_URL=https://your-openai-proxy.example.com/v1
XIAOU_AI_API_KEY=sk-xxxx
XIAOU_AI_CHAT_MODEL=gpt-5.4

XIAOU_AI_RAG_ENABLED=true
XIAOU_AI_RAG_API_KEY=rag-local-key
```

启动：

```bash
docker compose -f docker/ai/docker-compose.yml --env-file docker/ai/.env up -d --build
```

## 生产配置检查

上线前至少检查：

1. `XIAOU_MYSQL_PASSWORD`、`MYSQL_ROOT_PASSWORD`、`XIAOU_AI_API_KEY`、`XIAOU_AI_RAG_API_KEY`、`XIAOU_JWT_SECRET` 都不是示例值。
2. `XIAOU_CORS_ALLOWED_ORIGIN_PATTERNS` 只包含真实前端域名。
3. Redis 是否开启持久化，登录态丢失是否可接受。
4. 文件上传目录或对象存储是否持久化。
5. AI 和 RAG 可以关闭降级，还是必须可用。
6. go-judge 是否独立部署并限制资源。
7. Grafana 默认密码是否修改。
8. 文档站如果部署在子路径，是否设置 `VITEPRESS_BASE`。
