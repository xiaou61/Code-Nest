# 本地完整启动剧本

这页是“把 Code Nest 在本地跑起来”的执行剧本。它比 [本地开发](/guide/local-dev) 更偏实操：按目标分成最小启动、业务联调、AI 联调、OJ 联调和全量验证。

如果只是改文档，只需要启动文档站。如果要理解业务功能，至少要启动 MySQL、Redis、后端、管理端和用户端。如果要验证 AI、RAG、OJ 或监控，再启动对应 sidecar。

## 启动模式怎么选

| 模式 | 适合场景 | 需要启动 |
| --- | --- | --- |
| 文档模式 | 只改 `docs-site/**` | 文档站 |
| 最小业务模式 | 看页面、调普通接口 | MySQL、Redis、后端、管理端、用户端 |
| AI 联调模式 | 模拟面试、SQL 优化、AI 配置、RAG | 最小业务模式 + RAG sidecar + AI 配置 |
| OJ 联调模式 | 题目运行、自测、正式提交 | 最小业务模式 + go-judge |
| 运维观测模式 | 看健康检查、Prometheus、Grafana | 最小业务模式 + monitoring compose |
| 全量模式 | 发布前完整烟测 | 上面全部按需启动 |

## 0. 前置检查

先确认这些基础工具可用：

```powershell
java -version
mvn -version
node -v
npm -v
docker version
```

推荐版本：

| 工具 | 建议 |
| --- | --- |
| Java | 17+ |
| Maven | 能解析根 `pom.xml` 多模块 |
| Node.js | 18+ |
| npm | 9+ |
| Docker | 需要跑 AI compose、go-judge 或监控时使用 |

如果 `java -version` 在 PowerShell 里不可用，先确认 `JAVA_HOME` 和 PATH。历史验证记录里也提到过当前环境可能需要显式设置 `JAVA_HOME`。

## 1. 启动文档站

文档站独立运行，不依赖后端。

```powershell
cd docs-site
npm install
npm run dev
```

默认会打印本地访问地址。构建验证：

```powershell
npm run build
```

如果只改文档，这是最低验证项。

## 2. 启动最小业务模式

最小业务模式用于验证登录、普通页面、后台管理、文件上传之外的大多数基础接口。

### 2.1 准备 MySQL 和 Redis

需要先准备：

1. MySQL 数据库。
2. Redis 服务。
3. 导入 `sql/MySql/code_nest.sql`。
4. 按需要导入 `sql/MySql` 下版本增量脚本。

如果你用 `docker/ai/docker-compose.yml`，MySQL 首次启动会自动导入 `code_nest.sql` 和 `code_nest_data.sql`。如果自己维护本地 MySQL，要手动确认表结构已经存在。

### 2.2 启动后端

后端启动入口是：

```text
xiaou-application/src/main/java/com/xiaou/CodeNestApplication.java
```

常用启动命令：

```powershell
mvn -pl xiaou-application -am spring-boot:run
```

如果 `spring-boot:run` 在当前环境不稳定，可以先构建，再运行 jar：

```powershell
mvn -pl xiaou-application -am clean package -DskipTests
```

后端默认地址：

```text
http://localhost:9999/api
```

健康检查：

```text
http://localhost:9999/api/actuator/health
```

### 2.3 启动管理端

```powershell
cd vue3-admin-front
npm install
npm run dev
```

管理端默认端口是 `3000`。Vite 代理会把 `/api` 转发到 `http://localhost:9999`，不需要手动去掉 `/api`。

### 2.4 启动用户端

```powershell
cd vue3-user-front
npm install
npm run dev2
```

用户端默认端口是 `3001`。聊天室会先调用：

```text
POST /user/chat/ws-ticket
```

再连接：

```text
ws://localhost:9999/api/ws/chat?ticket=...
```

## 3. 启动 AI 联调模式

AI 联调有两种方式：脚本方式和 Docker Compose 方式。

### 3.1 PowerShell 脚本方式

只启动 RAG sidecar：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/ai/start-llamaindex-service.ps1 -InstallDependencies
```

启动 RAG sidecar 并启动 Java 主服务：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/ai/start-ai-dev-stack.ps1 -InstallRagDependencies
```

如果需要导入样例知识：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/ai/import-sample-knowledge.ps1 -Replace
```

RAG 健康检查：

```text
http://127.0.0.1:18080/health
```

### 3.2 Docker Compose 方式

复制环境变量模板：

```powershell
Copy-Item docker/ai/.env.example docker/ai/.env
```

在 `docker/ai/.env` 中填写：

```text
XIAOU_AI_BASE_URL
XIAOU_AI_API_KEY
XIAOU_AI_CHAT_MODEL
XIAOU_AI_RAG_API_KEY
```

启动：

```powershell
docker compose -f docker/ai/docker-compose.yml --env-file docker/ai/.env up -d --build
```

查看状态：

```powershell
docker compose -f docker/ai/docker-compose.yml --env-file docker/ai/.env ps
docker compose -f docker/ai/docker-compose.yml --env-file docker/ai/.env logs -f code-nest
```

常用地址：

| 服务 | 地址 |
| --- | --- |
| Java 后端 | `http://127.0.0.1:9999/api` |
| Swagger | `http://127.0.0.1:9999/api/swagger-ui.html` |
| RAG Health | `http://127.0.0.1:18080/health` |

## 4. 启动 OJ 联调模式

go-judge 位于：

```text
docker/go-judge
```

启动：

```powershell
cd docker/go-judge
docker compose up -d --build
```

验证编译器：

```powershell
docker exec go-judge javac -version
docker exec go-judge gcc --version
docker exec go-judge python3 --version
docker exec go-judge go version
docker exec go-judge node --version
```

默认端口：

```text
http://localhost:5050
```

后端配置项是 `oj.judge.goJudgeUrl`。如果 OJ 提交一直停在 `pending` 或 `judging`，优先回到 [OJ 判题系统](/modules/oj) 和 [问题定位流程](/operations/diagnosis-flow) 排查。

## 5. 启动监控模式

监控编排位于：

```text
docker/monitoring
```

启动：

```powershell
cd docker/monitoring
docker compose up -d
```

地址：

| 服务 | 地址 | 默认账号 |
| --- | --- | --- |
| Prometheus | `http://localhost:9090` | 无 |
| Grafana | `http://localhost:3000` | `admin / admin123` |

Prometheus Targets：

```text
http://localhost:9090/targets
```

确认 `code-nest` 任务为 `UP`。如果本机不是 Windows，`host.docker.internal` 可能不可用，需要改 `docker/monitoring/prometheus.yml` 的 target。

## 6. 启动后做一轮最小验证

| 链路 | 怎么验证 |
| --- | --- |
| 后端健康 | 打开 `/api/actuator/health` |
| 管理端 | 打开管理端登录页，确认能请求后端 |
| 用户端 | 打开用户端首页和登录页 |
| 文件访问 | 上传头像或文件后访问 `/api/files/**` |
| WebSocket | 登录用户端后进入 `/chat`，确认先拿 ws-ticket |
| AI | 管理端 `/system/ai-config` 看 Runtime/RAG 健康 |
| OJ | 打开题目详情，运行或提交一次样例 |
| 文档站 | `docs-site npm run build` 通过 |

如果某条依赖没启动，就在验证记录里写“未验证原因”。例如“go-judge 未启动，本次只验证 OJ 页面展示，不验证判题结果”。

## 7. 常见启动分叉

| 问题 | 先看 |
| --- | --- |
| 后端启动失败 | [常见问题排查](/operations/troubleshooting) 的“后端无法启动” |
| 前端接口 401 | [鉴权与用户体系](/modules/auth) |
| CORS 或 Origin 错误 | [Docker 与服务部署](/operations/docker)、[本地开发](/guide/local-dev) |
| WebSocket 连不上 | [WebSocket 协议](/reference/websocket) |
| 文件 URL 打不开 | [文件存储](/modules/file-storage) |
| AI 调用失败 | [AI Runtime](/modules/ai-runtime) |
| OJ 判题失败 | [OJ 判题系统](/modules/oj) |
| 不知道问题归属 | [问题定位流程](/operations/diagnosis-flow) |

## 停止服务

普通前后端服务用对应终端 `Ctrl+C` 停止。

Docker 服务停止：

```powershell
docker compose -f docker/ai/docker-compose.yml --env-file docker/ai/.env down
docker compose -f docker/go-judge/docker-compose.yml down
docker compose -f docker/monitoring/docker-compose.yml down
```

如果要清理 volume，先确认不会丢失本地数据，再使用：

```powershell
docker compose -f docker/ai/docker-compose.yml --env-file docker/ai/.env down -v
docker compose -f docker/go-judge/docker-compose.yml down -v
docker compose -f docker/monitoring/docker-compose.yml down -v
```


## 相关文档

| 文档 | 说明 |
| --- | --- |
| [快速开始](/guide/quick-start) | 快速搭建指南 |
| [本地开发](/guide/local-dev) | 开发环境配置 |
| [环境变量总表](/operations/env-vars) | 配置项说明 |
| [Docker 与服务部署](/operations/docker) | Docker 环境 |
