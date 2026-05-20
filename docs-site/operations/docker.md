# Docker 与服务部署

Code Nest 是一个多服务项目：Spring Boot 后端、两个 Vue 前端、VitePress 文档站、MySQL、Redis、LlamaIndex RAG sidecar、go-judge 判题沙箱和监控组件。部署时不要把它理解成“一个 jar 就完事”，而要按服务边界拆开。

如果部署问题已经影响到真实用户，或者你已经看到告警、5xx 飙升、服务不可用，先配合 [事故响应](/operations/incident-response) 使用。本页更适合梳理服务边界、部署顺序、变量和常见故障，不替代线上止损流程。

## 部署资料入口

| 路径 | 说明 |
| --- | --- |
| `docker/Dockerfile` | 主后端镜像，构建 `xiaou-application` 并运行 Java 17 |
| `docker/env/example.env` | 后端容器运行环境变量示例 |
| `docker/ai/docker-compose.yml` | AI 联调编排：MySQL、Redis、LlamaIndex、后端 |
| `docker/ai/README.md` | AI 联调启动说明 |
| `docker/go-judge/docker-compose.yml` | OJ 判题沙箱编排 |
| `docker/go-judge/README.md` | go-judge 部署说明 |
| `docker/monitoring/docker-compose.yml` | Prometheus + Grafana 编排 |
| `docker/monitoring/prometheus.yml` | Prometheus scrape 配置 |
| `docs-site/` | VitePress 文档站 |
| `vue3-admin-front/` | 管理端前端 |
| `vue3-user-front/` | 用户端前端 |

## 推荐服务拆分

生产环境建议至少拆成这些服务：

| 服务 | 端口/输出 | 说明 |
| --- | --- | --- |
| MySQL | `3306` | 主业务数据库 |
| Redis | `6379` | 缓存、限流、在线态、Sa-Token 持久化等 |
| Spring Boot API | `9999`，上下文 `/api` | 主后端 |
| 用户端静态资源 | Nginx 或对象存储 | `vue3-user-front` 构建产物 |
| 管理端静态资源 | Nginx 或内网静态服务 | `vue3-admin-front` 构建产物 |
| 文档站静态资源 | Nginx、GitHub Pages、对象存储等 | `docs-site/.vitepress/dist` |
| LlamaIndex sidecar | `18080` | RAG 文档和检索 |
| go-judge | `5050` | OJ 判题沙箱 |
| Prometheus | `9090` | 指标采集 |
| Grafana | `3000` | 指标看板 |

## 后端镜像

`docker/Dockerfile` 分两阶段：

1. 使用 `maven:3.9.11-eclipse-temurin-17` 构建。
2. 执行 `mvn -pl xiaou-application -am clean package -DskipTests`。
3. 使用 `eclipse-temurin:17-jre` 作为运行镜像。
4. 设置 `SPRING_PROFILES_ACTIVE=docker`。
5. 拷贝 `xiaou-application-*.jar` 为 `/app/app.jar`。
6. 暴露 `9999`。
7. 启动命令为 `java ${JAVA_OPTS} -jar /app/app.jar`。

单独构建示例：

```bash
docker build -f docker/Dockerfile -t code-nest-api:local .
```

运行时必须提供数据库、Redis、AI 等环境变量。可以参考 `docker/env/example.env`。

完整变量含义和本地、AI、OJ、监控、前端、文档站的配置关系见 [环境变量总表](/operations/env-vars)。

## 核心环境变量

| 变量 | 说明 |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | 容器环境一般使用 `docker` |
| `XIAOU_MYSQL_URL` | MySQL JDBC URL |
| `XIAOU_MYSQL_USERNAME` | MySQL 用户名 |
| `XIAOU_MYSQL_PASSWORD` | MySQL 密码 |
| `XIAOU_REDIS_ADDRESS` | Redisson 地址，例如 `redis://redis:6379` |
| `XIAOU_REDIS_DATABASE` | 业务 Redis DB |
| `XIAOU_SA_TOKEN_REDIS_HOST` | Sa-Token 独立 Redis host |
| `XIAOU_SA_TOKEN_REDIS_PORT` | Sa-Token Redis 端口 |
| `XIAOU_SA_TOKEN_REDIS_DATABASE` | Sa-Token Redis DB |
| `XIAOU_AI_PROVIDER` | AI provider，当前按 openai-compatible 使用 |
| `XIAOU_AI_BASE_URL` | AI API base URL |
| `XIAOU_AI_API_KEY` | AI API Key |
| `XIAOU_AI_CHAT_MODEL` | 聊天模型 |
| `XIAOU_AI_RAG_ENABLED` | 是否启用 RAG |
| `XIAOU_AI_RAG_ENDPOINT` | RAG sidecar 地址 |
| `XIAOU_AI_RAG_API_KEY` | RAG sidecar key |
| `XIAOU_CORS_ALLOWED_ORIGIN_PATTERNS` | HTTP CORS 和 WebSocket Origin 白名单，对应 `xiaou.cors.allowed-origin-patterns` |

生产环境不要使用模板里的 `123456`、`sk-xxxx` 或默认 JWT/AI 配置。

## AI 联调 Compose

`docker/ai/docker-compose.yml` 会启动：

| 服务 | 说明 |
| --- | --- |
| `mysql` | MySQL 8.0，首次启动导入 `code_nest.sql` 和 `code_nest_data.sql` |
| `redis` | Redis 7，开启 AOF |
| `llamaindex-service` | RAG sidecar，挂载样例知识文件 |
| `code-nest` | 后端容器，依赖前三个服务健康后启动 |

使用步骤：

```bash
cp docker/ai/.env.example docker/ai/.env
```

在 `docker/ai/.env` 中填写真实 AI 配置后，在仓库根目录执行：

```bash
docker compose -f docker/ai/docker-compose.yml --env-file docker/ai/.env up -d --build
```

查看状态：

```bash
docker compose -f docker/ai/docker-compose.yml --env-file docker/ai/.env ps
docker compose -f docker/ai/docker-compose.yml --env-file docker/ai/.env logs -f code-nest
```

常用地址：

| 地址 | 说明 |
| --- | --- |
| `http://127.0.0.1:9999/api` | 后端 API |
| `http://127.0.0.1:9999/api/swagger-ui.html` | Swagger |
| `http://127.0.0.1:18080/health` | RAG sidecar 健康检查 |

清理：

```bash
docker compose -f docker/ai/docker-compose.yml --env-file docker/ai/.env down
docker compose -f docker/ai/docker-compose.yml --env-file docker/ai/.env down -v
```

`down -v` 会删除 MySQL、Redis、RAG volume，谨慎使用。

## go-judge 判题沙箱

OJ 判题依赖 go-judge。部署文件在 `docker/go-judge`。

关键配置：

| 配置 | 值 |
| --- | --- |
| 容器端口 | `5050` |
| `GOJUDGE_PARALLELISM` | `4` |
| 文件存储 | `/tmp/gojudge` |
| privileged | `true` |
| 内存限制 | `1G` |
| CPU 限制 | `2.0` |

启动：

```bash
cd docker/go-judge
docker compose up -d --build
```

验证编译器：

```bash
docker exec go-judge javac -version
docker exec go-judge gcc --version
docker exec go-judge python3 --version
docker exec go-judge go version
docker exec go-judge node --version
```

后端配置项是 `oj.judge.goJudgeUrl`。开发环境里要确认它指向可访问的 `http://<host>:5050`。

## 前端部署

用户端和管理端都是 Vite 应用。一般流程：

```bash
cd vue3-user-front
npm ci
npm run build
```

```bash
cd vue3-admin-front
npm ci
npm run build
```

构建产物分别在各自的 `dist/`。部署时要注意：

| 项 | 说明 |
| --- | --- |
| API 代理 | 前端请求一般要转发到后端 `/api` |
| WebSocket | 聊天实际入口是 `/api/ws/chat?ticket=...`，反向代理要支持 Upgrade |
| History 路由 | Nginx 要把未知路径回退到 `index.html` |
| CORS | 生产环境显式配置 `XIAOU_CORS_ALLOWED_ORIGIN_PATTERNS`，不要依赖默认本地白名单 |
| 文件访问 | 本地文件可走后端 `/api/files/**`，也可在 Nginx 单独 alias `/files/` 到上传目录 |
| 静态缓存 | HTML 不强缓存，JS/CSS 可长缓存 |

如果用户端和管理端分别部署在不同域名或端口，例如：

```text
https://www.example.com
https://admin.example.com
```

后端应配置：

```text
XIAOU_CORS_ALLOWED_ORIGIN_PATTERNS=https://www.example.com,https://admin.example.com
```

这个配置同时影响普通 HTTP 接口和聊天室 WebSocket 握手。只写 API 域名没有意义，浏览器发送的 Origin 是前端页面所在的域名。

## 文档站部署

文档站是独立 VitePress 模块：

```bash
cd docs-site
npm ci
npm run build
```

输出目录是：

```text
docs-site/.vitepress/dist
```

可以部署到 Nginx、对象存储、GitHub Pages、内网静态服务等。如果部署在子路径，需要设置 VitePress `base`。

## 监控部署

监控资料在 `docker/monitoring`，包含 Prometheus 和 Grafana。详细说明见 [监控与观测](/operations/monitoring)。

快速启动：

```bash
cd docker/monitoring
docker compose up -d
```

默认地址：

| 服务 | 地址 |
| --- | --- |
| Prometheus | `http://localhost:9090` |
| Grafana | `http://localhost:3000` |

## 生产上线检查

| 检查项 | 说明 |
| --- | --- |
| 数据库 | 已导入基线 SQL，账号权限最小化，开启备份 |
| Redis | 开启持久化或明确可丢数据范围 |
| 后端密钥 | JWT、Sa-Token、AI Key、RAG Key 不在仓库明文 |
| 文件上传 | `uploads/` 或对象存储持久化 |
| WebSocket | 反向代理支持 Upgrade |
| go-judge | 沙箱独立部署，限制 CPU/内存，避免和主后端共享高权限容器 |
| AI | 模型服务可访问，RAG 可关闭降级 |
| 日志 | 应用日志和容器日志有保留策略 |
| 监控 | `/api/actuator/prometheus` 可采集 |
| 文档站 | `docs-site/.vitepress/dist` 可单独部署和回滚 |

## 常见故障

| 问题 | 排查 |
| --- | --- |
| 后端容器启动失败 | 看 `logs code-nest`，重点查 MySQL/Redis/配置变量 |
| 首次 MySQL 没有表 | 只有空 volume 首次启动才自动导入 SQL，已有 volume 不会重复导入 |
| RAG 调试失败 | 查 `llamaindex-service` 健康和 `XIAOU_AI_RAG_API_KEY` |
| OJ 一直 system_error | 查 go-judge 地址、容器 privileged、编译器是否安装 |
| 前端接口 404 | 查 Nginx `/api` 代理和后端 context-path，后端上下文是 `/api` |
| 聊天 WebSocket 连不上 | 查 `/api/ws/chat` 代理 Upgrade、`ws-ticket` 是否先获取、Origin 是否在白名单 |
| 上传图片打不开 | 查返回 URL 是 `/api/files/**` 还是 Nginx `/files/**`，确认 `uploads/` 持久化路径一致 |
| 文档站资源 404 | 查 VitePress `base` 和静态资源路径 |
| Prometheus target down | 查 `host.docker.internal:9999` 是否适合当前系统 |

如果这些故障已经从“部署问题”升级成“线上事故”，不要只在本页继续排查，先回到 [事故响应](/operations/incident-response) 做影响面判断、止损和记录。

## 推荐部署顺序

1. 先部署 MySQL 和 Redis。
2. 启动后端，确认 `/api/actuator/health` 正常。
3. 部署用户端和管理端，确认登录、接口代理、WebSocket。
4. 如果需要 AI，启动 LlamaIndex sidecar 并完成 AI 配置测试。
5. 如果需要 OJ，启动 go-judge 并提交一道样例题。
6. 部署文档站。
7. 启动 Prometheus/Grafana，确认 target 为 UP。
8. 做一轮 [验证记录与已知问题](/manuals/verified-scenarios) 中的核心链路。
