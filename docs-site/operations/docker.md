# Docker 与服务部署

这页覆盖 Code Nest 的 Docker 部署方式，包括后端应用、AI 联调 Compose、OJ 判题沙箱和监控栈。每个 Compose 文件都可以独立启动，不需要一次性拉起所有服务。

如果你只想在本地跑起来而不想用 Docker，直接看 [本地开发环境搭建](/guide/local-dev)。如果你要部署到生产服务器，看 [独立部署](/guide/deploy)。

## Dockerfile 后端镜像

后端 Dockerfile 位于 `docker/Dockerfile`，采用两阶段构建：

```dockerfile
# ---- 阶段1：Maven 构建 ----
FROM maven:3.9.11-eclipse-temurin-17 AS builder
WORKDIR /build
COPY . .
RUN mvn -pl xiaou-application -am clean package -DskipTests

# ---- 阶段2：JRE 运行 ----
FROM eclipse-temurin:17-jre
WORKDIR /app
ENV TZ=Asia/Shanghai
ENV SPRING_PROFILES_ACTIVE=docker
COPY --from=builder /build/xiaou-application/target/xiaou-application-*.jar /app/app.jar
EXPOSE 9999
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /app/app.jar"]
```

关键点：

| 项 | 说明 |
| --- | --- |
| 构建基础镜像 | `maven:3.9.11-eclipse-temurin-17`，含 Maven + JDK 17 |
| 运行基础镜像 | `eclipse-temurin:17-jre`，只有 JRE 17，不含 Maven 和源码 |
| 构建命令 | `mvn -pl xiaou-application -am clean package -DskipTests`，和本地构建一致 |
| 激活 Profile | `SPRING_PROFILES_ACTIVE=docker`，所有配置从环境变量注入 |
| JVM 参数 | 通过 `JAVA_OPTS` 传入，如 `-Xms512m -Xmx1024m` |
| 暴露端口 | 9999 |
| 时区 | `Asia/Shanghai` |

### 构建与运行

```powershell
# 构建镜像
docker build -f docker/Dockerfile -t code-nest-api:local .

# 运行（需要提供环境变量）
docker run --env-file docker/env/example.env -p 9999:9999 code-nest-api:local

# 指定 JVM 参数
docker run --env-file docker/env/example.env -e JAVA_OPTS="-Xms512m -Xmx1024m" -p 9999:9999 code-nest-api:local
```

运行时必须通过环境变量提供 MySQL、Redis、AI 等配置。`docker` profile 不会使用任何 `application-dev.yml` 中的 localhost 默认值。

## AI 联调 Compose

`docker/ai/docker-compose.yml` 是最主要的联调 Compose，会启动后端 + MySQL + Redis + RAG sidecar 四个服务。

### 服务列表

| 服务 | 镜像 | 端口 | 说明 |
| --- | --- | --- | --- |
| `mysql` | `mysql:8.0` | 3306 | 首次启动自动导入 `code_nest.sql` 和 `code_nest_data.sql` |
| `redis` | `redis:7` | 6379 | 开启 AOF 持久化 |
| `llamaindex-service` | 本地构建 | 18080 | RAG sidecar，挂载样例知识文件 |
| `code-nest` | 本地构建 | 9999 | 后端容器，依赖前三个服务健康后启动 |

### MySQL 初始化

MySQL 容器首次启动时，会自动执行 `/docker-entrypoint-initdb.d/` 目录下的 SQL 文件。Compose 中挂载了：

```yaml
volumes:
  - ../../sql/MySql/code_nest.sql:/docker-entrypoint-initdb.d/01-schema.sql
  - ../../sql/MySql/code_nest_data.sql:/docker-entrypoint-initdb.d/02-data.sql
```

这意味着：
- 第一次 `docker compose up` 会自动建表和导入基础数据。
- 之后的启动不会再重复导入（MySQL 的 `docker-entrypoint-initdb.d` 只在数据库为空时执行）。
- 如果需要重新初始化，需要删除 MySQL volume 再启动。

### 服务依赖和健康检查

后端 `code-nest` 容器设置了 `depends_on`，确保 MySQL、Redis 和 RAG sidecar 都健康后才开始启动：

```yaml
depends_on:
  mysql:
    condition: service_healthy
  redis:
    condition: service_healthy
  llamaindex-service:
    condition: service_healthy
```

健康检查配置：

| 服务 | 检查方式 | 间隔 | 超时 | 重试 |
| --- | --- | --- | --- | --- |
| MySQL | `mysqladmin ping` | 5s | 3s | 10 |
| Redis | `redis-cli ping` | 5s | 3s | 10 |
| RAG sidecar | `curl /health` | 10s | 5s | 5 |

### 使用步骤

```powershell
# 1. 复制环境变量模板
cp docker/ai/.env.example docker/ai/.env

# 2. 编辑 .env，填写 AI 配置
#    XIAOU_AI_BASE_URL=https://your-openai-proxy.example.com/v1
#    XIAOU_AI_API_KEY=sk-xxxx
#    XIAOU_AI_CHAT_MODEL=gpt-4o

# 3. 在仓库根目录启动（注意 --env-file 指向 .env）
docker compose -f docker/ai/docker-compose.yml --env-file docker/ai/.env up -d --build

# 4. 查看日志
docker compose -f docker/ai/docker-compose.yml logs -f code-nest

# 5. 验证
curl http://127.0.0.1:9999/api/actuator/health
# 期望: {"status":"UP"}
```

常用地址：

| 地址 | 说明 |
| --- | --- |
| `http://127.0.0.1:9999/api` | 后端 API |
| `http://127.0.0.1:9999/api/swagger-ui.html` | Swagger 文档 |
| `http://127.0.0.1:9999/api/actuator/health` | 健康检查 |
| `http://127.0.0.1:18080/health` | RAG sidecar 健康检查 |

### 环境变量

AI 联调 Compose 需要的环境变量（在 `docker/ai/.env` 中填写）：

| 变量 | 必填 | 说明 |
| --- | --- | --- |
| `XIAOU_AI_BASE_URL` | 是 | AI API Base URL |
| `XIAOU_AI_API_KEY` | 是 | AI API Key |
| `XIAOU_AI_CHAT_MODEL` | 否 | 聊天模型，默认 `gpt-5.4` |
| `XIAOU_AI_RAG_ENABLED` | 否 | 是否启用 RAG，默认 `true`（联调环境下建议开启） |
| `XIAOU_JWT_SECRET` | 否 | JWT 签名密钥，默认开发占位值 |
| `XIAOU_CORS_ALLOWED_ORIGIN_PATTERNS` | 否 | CORS 域名，默认 localhost |

完整环境变量表见 [环境变量总表](/operations/env-vars)。

### 清理

```powershell
# 停止并删除容器
docker compose -f docker/ai/docker-compose.yml --env-file docker/ai/.env down

# 停止并删除容器 + volumes（MySQL 数据、Redis 数据、RAG 数据全删，谨慎使用）
docker compose -f docker/ai/docker-compose.yml --env-file docker/ai/.env down -v
```

## go-judge（OJ 判题沙箱）

`docker/go-judge/docker-compose.yml` 提供 OJ 判题沙箱服务。

### 服务配置

| 配置 | 值 | 说明 |
| --- | --- | --- |
| 端口 | 5050 | 判题服务监听端口 |
| 并发 | 4 | `GOJUDGE_PARALLELISM`，最多同时执行 4 个判题任务 |
| 内存限制 | 1G | Docker 内存限制 |
| CPU 限制 | 2.0 | Docker CPU 限制 |
| privileged | true | 沙箱需要特权模式来隔离用户代码执行 |
| 支持语言 | Java、C/C++、Python3、Go、Node.js | 预装编译器 |

### 启动

```powershell
cd docker/go-judge
docker compose up -d --build
```

### 验证编译器

启动后确认各语言编译器可用：

```powershell
docker exec go-judge javac -version
# javac 17.x.x

docker exec go-judge gcc --version
# gcc (GCC) x.x.x

docker exec go-judge python3 --version
# Python 3.x.x

docker exec go-judge go version
# go version go1.x.x linux/amd64

docker exec go-judge node --version
# v20.x.x
```

### 后端配置

后端通过 `oj.judge.go-judge-url` 配置项指向 go-judge 地址。配置在 `application.yml`：

```yaml
oj:
  judge:
    go-judge-url: http://154.222.18.220:5050   # 默认远端示例
    max-compile-time: 10000                      # 最大编译时间 ms
    default-time-limit: 2000                     # 默认运行时间限制 ms
    default-memory-limit: 256                    # 默认内存限制 MB
```

如果 go-judge 部署在本地，需要覆盖为 `http://go-judge:5050`（Docker 内部网络）或 `http://127.0.0.1:5050`（宿主机网络）。

### 安全注意事项

| 项 | 说明 |
| --- | --- |
| privileged 模式 | go-judge 需要特权模式来创建沙箱命名空间，这是正常需求 |
| 资源限制 | 内存和 CPU 限制防止用户代码无限消耗资源 |
| 独立部署 | go-judge 不应和主后端共享同一个高权限容器，应独立部署 |
| 网络隔离 | 建议将 go-judge 放在独立的 Docker 网络中，只对后端暴露 |

## 监控栈（Prometheus + Grafana）

`docker/monitoring/docker-compose.yml` 提供指标采集和面板展示。

### 服务列表

| 服务 | 镜像 | 端口 | 说明 |
| --- | --- | --- | --- |
| Prometheus | `prom/prometheus` | 9090 | 指标采集和存储 |
| Grafana | `grafana/grafana` | 3000 | 面板展示 |

### Prometheus 配置

Prometheus 采集后端的 `/api/actuator/prometheus` 端点。配置文件在 `docker/monitoring/prometheus.yml`：

```yaml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'code-nest-api'
    metrics_path: '/api/actuator/prometheus'
    static_configs:
      - targets: ['host.docker.internal:9999']
```

关键配置项：

| 配置 | 默认值 | 说明 |
| --- | --- | --- |
| `scrape_interval` | 15s | 采集间隔 |
| `evaluation_interval` | 15s | 规则评估间隔 |
| 数据保留 | 30d | Prometheus 数据保留天数 |

如果后端不在 `host.docker.internal:9999`，修改 `targets` 为后端实际地址。

### Grafana 配置

| 配置 | 默认值 | 说明 |
| --- | --- | --- |
| 登录用户 | admin | 首次登录会要求修改密码 |
| 登录密码 | admin123 | 在 `GF_SECURITY_ADMIN_PASSWORD` 中配置 |
| 数据源 | Prometheus | 自动配置，指向同网络内的 Prometheus |
| Dashboard 存储 | Docker volume | 持久化到 `grafana-storage` |

推荐的 Grafana Dashboard：

| Dashboard | ID | 用途 |
| --- | --- | --- |
| Spring Boot 2.1 Statistics | 11378 | Spring Boot 请求、连接池、JVM |
| JVM Micrometer | 4701 | JVM 内存、GC、线程 |

### 启动

```powershell
cd docker/monitoring
docker compose up -d
```

启动后访问：
- Grafana：`http://127.0.0.1:3000`
- Prometheus：`http://127.0.0.1:9090`

## RAG sidecar（LlamaIndex）

RAG sidecar 位于 `llamaindex-service/` 目录，由 AI 联调 Compose 自动构建和启动。

### 架构角色

```text
用户端 → 后端 API → RAG sidecar (18080)
                    ↗
         AI Provider (OpenAI Compatible)
```

后端通过 `XIAOU_AI_RAG_ENDPOINT` 指向 RAG sidecar。RAG sidecar 再调用 AI Provider 的 Embedding API 构建向量索引和检索。

### 关键配置

| 配置 | 默认值 | 环境变量 |
| --- | --- | --- |
| 端口 | 18080 | — |
| 知识文件目录 | `/app/knowledge` | Compose 中挂载 `../../llamaindex-service/knowledge` |
| AI Provider | — | 需要配置 Base URL 和 API Key |

### 健康检查

```powershell
curl http://127.0.0.1:18080/health
# 期望: {"status":"ok"}
```

如果 RAG sidecar 不可用，后端 AI 功能会降级返回，不影响其他业务。

## 网络与端口总览

| 组件 | 端口 | 协议 | 说明 |
| --- | --- | --- | --- |
| 后端 API | 9999 | HTTP | 所有 `/api/...` 接口 |
| 后端 WebSocket | 9999 | WS | `/api/ws/...` 聊天室 |
| MySQL | 3306 | TCP | 数据库 |
| Redis | 6379 | TCP | 缓存和会话 |
| RAG sidecar | 18080 | HTTP | AI RAG 服务 |
| go-judge | 5050 | HTTP | OJ 判题沙箱 |
| Prometheus | 9090 | HTTP | 指标采集 |
| Grafana | 3000 | HTTP | 面板展示 |

## 常见 Docker 问题

| 问题 | 原因 | 解决 |
| --- | --- | --- |
| MySQL 容器启动失败 | 端口 3306 被占用 | 停掉本地 MySQL 或改 Compose 中的端口映射 |
| Redis 容器启动失败 | 端口 6379 被占用 | 停掉本地 Redis 或改 Compose 中的端口映射 |
| 后端容器 `Communications link failure` | MySQL 还没健康就启动了后端 | 检查 `depends_on` 和健康检查配置 |
| 后端容器 Redis 连接失败 | Redis 地址不对 | Docker 内部网络用 `redis:6379`，宿主机用 `127.0.0.1:6379` |
| go-judge 判题返回错误 | 编译器未安装或资源不足 | `docker exec go-judge javac -version` 检查，增加内存/CPU 限制 |
| RAG sidecar 健康检查失败 | AI 配置缺失或 Provider 不可达 | 检查 `.env` 中的 `XIAOU_AI_BASE_URL` 和 `XIAOU_AI_API_KEY` |
| Prometheus 无数据 | 后端 Actuator 未暴露或地址不对 | 确认后端 `management.endpoints.web.exposure.include` 包含 `prometheus` |
| Grafana 无数据源 | Prometheus 未启动 | 先启动 Prometheus，Grafana 会自动重连 |
| MySQL volume 数据丢失 | 用了 `down -v` | 不需要保留数据时才用 `-v`，否则只用 `down` |
| Windows 下 `host.docker.internal` 不解析 | Docker 版本问题 | 升级 Docker Desktop 或手动加 `--add-host=host.docker.internal:host-gateway` |

## 部署策略选择

| 场景 | 推荐方式 | 说明 |
| --- | --- | --- |
| 本地开发 | 直接运行 | 后端 `java -jar`，前端 `npm run dev`，不需要 Docker |
| 本地联调 AI | AI Compose | `docker/ai/docker-compose.yml`，一键拉起 MySQL + Redis + RAG + 后端 |
| 本地联调 OJ | go-judge Compose | 只需 `docker/go-judge/docker-compose.yml` |
| 单机部署 | AI Compose + Nginx | 前端构建后放 Nginx，后端用 Docker |
| 生产部署 | 独立部署 | 参考 [独立部署](/guide/deploy)，每个服务独立管理 |

## 启动顺序

Docker Compose 的 `depends_on` + 健康检查会自动处理启动顺序，但如果你手动启动各服务，推荐顺序：

1. **MySQL** — 首次启动导入 SQL，等待 `mysqladmin ping` 成功。
2. **Redis** — 等待 `redis-cli ping` → `PONG`。
3. **RAG sidecar**（可选）— 等待 `/health` 返回 `ok`。
4. **后端** — 等待 `/api/actuator/health` 返回 `UP`。
5. **go-judge**（可选）— 独立启动即可。
6. **监控**（可选）— Prometheus + Grafana，独立启动即可。
7. **前端** — Nginx 或 `npm run dev`。

更多信息见 [独立部署](/guide/deploy) 和 [环境变量总表](/operations/env-vars)。

## 相关文档

| 文档 | 说明 |
| --- | --- |
| [独立部署](/guide/deploy) | 部署流程 |
| [环境变量总表](/operations/env-vars) | 配置项说明 |
| [监控与观测](/operations/monitoring) | 监控配置 |
| [架构总览](/architecture/overview) | 部署拓扑 |
