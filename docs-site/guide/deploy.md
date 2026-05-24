# 独立部署

本页覆盖 Code Nest 全栈部署：后端 Spring Boot 应用、两个前端 Vue 3 应用、文档站 VitePress 静态站、OJ 判题沙箱 go-judge、AI RAG sidecar，以及监控栈 Prometheus + Grafana。

文档站可以独立部署，不需要 Java 运行时、数据库和 Redis。后端和前端必须一起部署，且依赖 MySQL 和 Redis。

## 端口与目录总览

| 组件 | 默认端口 | 源码目录 | 产物目录 | 依赖 |
| --- | --- | --- | --- | --- |
| 后端聚合应用 | 9999 | `xiaou-application` | `xiaou-application/target/*.jar` | MySQL、Redis |
| 用户端 | 3001（dev）/ Nginx（prod） | `vue3-user-front` | `vue3-user-front/dist` | 后端 API |
| 管理端 | 3000（dev）/ Nginx（prod） | `vue3-admin-front` | `vue3-admin-front/dist` | 后端 API |
| 文档站 | 5174（dev）/ Nginx（prod） | `docs-site` | `docs-site/.vitepress/dist` | 无 |
| go-judge 沙箱 | 5050 | `docker/go-judge` | Docker 镜像 | 后端 OJ 模块 |
| RAG sidecar | 18080 | `llamaindex-service` | Docker 镜像 | 后端 AI 模块 |
| Prometheus | 9090 | `docker/monitoring` | Docker 容器 | 后端 Actuator |
| Grafana | 3000 | `docker/monitoring` | Docker 容器 | Prometheus |

## 后端构建与运行

### 本地开发

```bash
# 根目录构建全部模块
mvn clean package -DskipTests

# 或只构建启动模块（-am 会自动构建依赖模块）
mvn -pl xiaou-application -am clean package -DskipTests

# 运行（默认 dev profile）
java -jar xiaou-application/target/xiaou-application-*.jar
```

### Spring Profile

| Profile | 配置文件 | 用途 | 关键差异 |
| --- | --- | --- | --- |
| `dev`（默认） | `application-dev.yml` | 本地开发 | P6Spy SQL 日志、本地 MySQL/Redis |
| `docker` | `application-docker.yml` | Docker 容器 | 环境变量注入 MySQL/Redis 地址 |
| `prod` | `application-prod.yml` | 生产环境 | 关闭调试端点、外部化密钥 |
| `sec` | `application-sec.yml` | 密钥覆盖 | JWT 密钥、第三方 API Key（不提交仓库） |

Docker 环境下通过 `SPRING_PROFILES_ACTIVE=docker` 激活。

### 关键配置项

| 配置 | 默认值 | 环境变量 | 说明 |
| --- | --- | --- | --- |
| 服务端口 | 9999 | — | `server.port` |
| 上下文路径 | `/api` | — | `server.servlet.context-path` |
| MySQL URL | `localhost:3306/code_nest` | `XIAOU_MYSQL_URL` | Docker 下指向 `mysql:3306` |
| Redis 地址 | `127.0.0.1:6379` | `XIAOU_REDIS_ADDRESS` | 业务 DB=3，Sa-Token DB=4 |
| AI Provider | `openai-compatible` | `XIAOU_AI_PROVIDER` | 需配置 `XIAOU_AI_BASE_URL` 和 `XIAOU_AI_API_KEY` |
| RAG Endpoint | `localhost:18080` | `XIAOU_AI_RAG_ENDPOINT` | 默认关闭，`XIAOU_AI_RAG_ENABLED=true` 开启 |
| go-judge URL | 外部 IP:5050 | — | OJ 沙箱服务地址 |

### Docker 部署后端

主 Dockerfile 在 `docker/Dockerfile`，采用两阶段构建：

```dockerfile
# 阶段1：Maven 构建
FROM maven:3.9.11-eclipse-temurin-17 AS builder
# 阶段2：JRE 运行
FROM eclipse-temurin:17-jre
EXPOSE 9999
```

启动命令支持通过 `JAVA_OPTS` 传递 JVM 参数：

```bash
docker run --env-file docker/env/example.env -p 9999:9999 code-nest-backend
```

环境变量示例见 `docker/env/example.env`，启动时通过 `--env-file` 注入。

## 前端构建与部署

### 构建

```bash
# 用户端
cd vue3-user-front
npm ci
npm run build        # 产物 → dist/

# 管理端
cd vue3-admin-front
npm ci
npm run build        # 产物 → dist/
```

两个前端都使用 Vite + esbuild，产物输出到 `dist/`。代码分割策略通过 `manualChunks` 配置：

| chunk 名称 | 包含内容 | 前端 |
| --- | --- | --- |
| `vendor-element` | Element Plus | 两者 |
| `vendor-monaco` | Monaco Editor | 用户端 |
| `vendor-graph` | @antv/g6 | 两者 |
| `vendor-markdown` | markdown-it + highlight.js + DOMPurify | 两者 |
| `vendor-vue` | Vue 核心 | 两者 |
| `vendor-router` | vue-router | 两者 |
| `vendor-state` | Pinia + @vueuse | 两者 |

### Vite 代理配置

开发模式下，两个前端都通过 Vite proxy 将 `/api` 请求转发到后端：

```js
// vite.config.js（两前端相同结构）
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:9999',
      changeOrigin: true,
      secure: false
    }
  }
}
```

生产环境不需要代理，由 Nginx 统一处理。

### Nginx 前端配置

```nginx
server {
    listen 80;
    server_name app.example.com;

    # 用户端
    location / {
        root /var/www/code-nest-user;
        try_files $uri $uri/ /index.html;
    }

    # API 代理到后端
    location /api/ {
        proxy_pass http://127.0.0.1:9999;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

管理端配置相同，只是 `root` 指向管理端产物目录。

## 文档站构建与部署

```bash
cd docs-site
npm ci
npm run build
```

产物目录：

```text
docs-site/.vitepress/dist
```

### Nginx 部署

```nginx
server {
    listen 80;
    server_name docs.example.com;

    root /var/www/code-nest-docs;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

将 `docs-site/.vitepress/dist` 中的文件同步到 `/var/www/code-nest-docs` 即可。

### 子路径部署

如果文档站部署在子路径，例如：

```text
https://example.com/code-nest-docs/
```

构建时设置：

```bash
VITEPRESS_BASE=/code-nest-docs/ npm run build
```

配置文件会读取 `VITEPRESS_BASE`，默认是 `/`。

## Docker Compose 服务

### go-judge（OJ 沙箱）

```bash
cd docker/go-judge
docker-compose up -d
```

| 配置 | 值 | 说明 |
| --- | --- | --- |
| 端口 | 5050 | 判题服务 |
| 并发 | 4 | `GOJUDGE_PARALLELISM` |
| 内存限制 | 1G | Docker 资源限制 |
| CPU 限制 | 2.0 | Docker 资源限制 |
| 支持语言 | Java、C/C++、Python3、Go、Node.js | 预装编译器 |

### 监控栈（Prometheus + Grafana）

```bash
cd docker/monitoring
docker-compose up -d
```

| 服务 | 端口 | 说明 |
| --- | --- | --- |
| Prometheus | 9090 | 指标采集和存储，保留 30 天 |
| Grafana | 3000 | 面板展示，默认 admin/admin123 |

Prometheus 采集后端的 `/api/actuator/prometheus` 端点。Grafana 推荐 Dashboard：

- Spring Boot 2.1 Statistics（ID: 11378）
- JVM Micrometer（ID: 4701）

### AI RAG sidecar

```bash
cd docker/ai
docker-compose up -d
```

独立 LlamaIndex 知询服务，后端通过 `XIAOU_AI_RAG_ENDPOINT` 连接。

## GitHub Pages 思路

这部分只是保留一种可选示例，**不是当前仓库默认启用的部署方式**。如果团队不打算让文档站自动部署到 Pages，可以完全不加这一段 workflow。

如果后面真的要接入，再按团队策略单独创建 workflow；下面只是一个最小构建示例：

```yaml
name: Docs Site

on:
  push:
    branches: ["your-docs-branch"]
    paths:
      - "docs-site/**"
      - ".github/workflows/docs-site.yml"

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: 20
          cache: npm
          cache-dependency-path: docs-site/package-lock.json
      - run: npm ci
        working-directory: docs-site
      - run: npm run build
        working-directory: docs-site
```

是否发布到 Pages、服务器或对象存储，可以按团队部署策略选择。

## 完整启动顺序

从零启动全部服务的推荐顺序：

1. **MySQL** — 导入 `sql/MySql/code_nest.sql`。
2. **Redis** — 默认端口 6379，无需特殊配置。
3. **后端** — `java -jar xiaou-application-*.jar`，等待健康检查通过。
4. **go-judge**（可选）— `docker/go-judge/docker-compose up -d`。
5. **RAG sidecar**（可选）— `docker/ai/docker-compose up -d`。
6. **前端** — Nginx 或 `npm run dev`。
7. **文档站** — Nginx 或 `npm run dev`。
8. **监控**（可选）— `docker/monitoring/docker-compose up -d`。

后端启动后可通过 Actuator 验证健康状态。

## 发布检查清单

发布前建议确认：

1. `mvn clean package -DskipTests` 在干净环境中通过。
2. `npm ci && npm run build` 在两个前端和文档站都通过。
3. 如果部署到子路径，`VITEPRESS_BASE` 与访问路径一致。
4. 静态服务器启用了 `try_files` 或等价的 SPA 回退规则。
5. `code-nest-mark.svg` 等静态资源在发布环境能访问。
6. 发布后打开首页、模块页、搜索和至少一个深层路由。
7. 后端健康检查返回 `UP`。
8. Redis 连接正常（Sa-Token 登录依赖 Redis）。
9. AI 配置已通过环境变量注入（`XIAOU_AI_API_KEY` 等）。
10. 敏感配置（JWT 密钥、数据库密码）已通过 `application-sec.yml` 或环境变量覆盖，不使用默认值。
