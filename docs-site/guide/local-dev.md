# 本地开发环境搭建

本页详细说明在本地搭建 Code-Nest 开发环境的完整流程，包括后端、前端、数据库、缓存、AI 等全部依赖。所有配置项均来自源码，可直接对照修改。

## 环境要求

| 依赖 | 最低版本 | 推荐版本 | 验证命令 |
| --- | --- | --- | --- |
| JDK | 17 | 17+ | `java -version` |
| Maven | 3.8 | 3.9+ | `mvn -version` |
| Node.js | 18 | 20 LTS | `node -v` |
| npm | 9 | 10+ | `npm -v` |
| MySQL | 8.0 | 8.0+ | `mysql --version` |
| Redis | 7.0 | 7.2+ | `redis-cli ping` → `PONG` |
| Git | 2.30 | 2.40+ | `git --version` |

> **注意**：JDK 17 是硬性要求，Spring Boot 3.4.x 不支持 JDK 8/11。

## 数据库初始化

### 1. 创建数据库

```sql
CREATE DATABASE code_nest DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```

### 2. 导入基础表结构

```bash
mysql -u root -p code_nest < sql/MySql/code_nest.sql
```

基础脚本包含 136 张表，加上增量脚本后共 142 张表，是系统运行的最低要求。

### 3. 导入增量脚本（按版本顺序）

```bash
# 按版本号从小到大依次执行
for ver in v1.2.0 v1.3.0 v1.4.0 v1.5.0 v1.6.0 v1.7.0 v1.7.1 v1.8.0 v1.8.1 v1.8.2 v1.8.3 v1.8.4; do
  if [ -f "sql/$ver/$ver.sql" ]; then
    mysql -u root -p code_nest < "sql/$ver/$ver.sql"
  fi
done
```

> **提示**：增量脚本中有些是 ALTER TABLE，如果基础脚本已包含最新结构则可能报"列已存在"，可安全忽略。

### 4. 确认表数量

```sql
USE code_nest;
SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'code_nest';
-- 预期：142 张表左右（视版本可能有差异）
```

## Redis 配置

### 启动 Redis

```bash
# 前台启动（开发用）
redis-server

# 指定配置文件
redis-server /path/to/redis.conf
```

### 验证连接

```bash
redis-cli ping
# 应返回 PONG
```

Code-Nest 使用 Redis 做以下用途：

| 用途 | Redis DB | 说明 |
| --- | --- | --- |
| 业务缓存 | db3 | 接口缓存、验证码、Ticket、Redisson 业务数据 |
| Sa-Token 会话 | db4 | 用户端 + 管理端登录态（alone-redis）→ 详见 [鉴权与用户体系](/modules/auth) |

> **注意**：本地开发无需修改 Redis DB 编号，保持默认即可。生产环境建议不同模块使用不同 Redis 实例。

## 后端配置

### 配置文件总览

后端配置文件位于 `xiaou-application/src/main/resources/`：

| 文件 | 用途 | 何时加载 |
| --- | --- | --- |
| `application.yml` | 主配置（公共配置、Sa-Token、AI、监控） | 始终加载 |
| `application-dev.yml` | 本地开发（P6Spy、localhost 连接） | `profiles.active=dev` 时 |
| `application-docker.yml` | Docker 部署（环境变量注入） | `profiles.active=docker` 时 |
| `application-prod.yml` | 生产环境（占位） | `profiles.active=prod` 时 |
| `application-sec.yml` | 敏感配置（密钥、Token） | 可选，通过 `spring.config.import` 加载 |

### 1. 修改 application-dev.yml

这是本地开发最关键的配置文件，需要修改以下项：

```yaml
# application-dev.yml 实际结构
spring:
  datasource:
    # 开发环境使用 P6Spy 代理驱动（打印完整 SQL 日志）
    driver-class-name: com.p6spy.engine.spy.P6SpyDriver
    # P6Spy 格式 URL（在原 jdbc:mysql 前加 p6spy:）
    url: jdbc:p6spy:mysql://localhost:3306/code_nest?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf-8
    username: root
    password: 1234          # 改为你的数据库密码

  data:
    redis:
      redisson:
        config: |
          singleServerConfig:
            address: "redis://127.0.0.1:6379"
            database: 3           # 业务数据使用 db3
            connectionPoolSize: 64
            connectionMinimumIdleSize: 10

# Sa-Token 独立 Redis（与业务 Redis 隔离）
sa-token:
  alone-redis:
    host: 127.0.0.1
    port: 6379
    database: 4           # 登录态使用 db4
    timeout: 10000
    jedis:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0
        max-wait: 3000
```

> **重要**：业务 Redis 使用 db3（Redisson），Sa-Token 登录态使用 db4（alone-redis）。两者必须隔离，否则排障时无法区分业务缓存和登录数据。

### 2. Sa-Token 主配置（application.yml）

Sa-Token 的公共配置在 `application.yml` 中（双端鉴权设计详见 [鉴权与用户体系](/modules/auth)）：

```yaml
sa-token:
  token-name: Authorization
  timeout: 604800          # Token 有效期 7 天（秒）
  activity-timeout: -1     # 不启用临时过期（-1 表示不限）
  is-concurrent: true      # 允许同一账号并发登录
  is-share: false          # 每次登录新建 Token
  token-style: uuid
  is-read-cookie: false    # 不从 Cookie 读取 Token
  is-read-header: true     # 从请求头读取 Token
  token-prefix: Bearer     # 前端提交时加 Bearer 前缀
```

前端请求时 Header 格式：`Authorization: Bearer <token>`

### 3. AI 功能配置（按需）

AI 配置在 `application.yml` 中，通过环境变量覆盖：

```yaml
xiaou:
  ai:
    enabled: true
    provider: ${XIAOU_AI_PROVIDER:openai-compatible}
    base-url: ${XIAOU_AI_BASE_URL:}
    api-key: ${XIAOU_AI_API_KEY:}
    model:
      chat: ${XIAOU_AI_CHAT_MODEL:gpt-5.4}
      embedding: ${XIAOU_AI_EMBEDDING_MODEL:text-embedding-3-small}
    timeout:
      connect-ms: 10000     # 连接超时 10s
      read-ms: 60000        # 读取超时 60s
    retry:
      max-attempts: 2       # 最大重试次数
      backoff-ms: 1000      # 重试间隔 ms
    rag:
      enabled: ${XIAOU_AI_RAG_ENABLED:false}
      endpoint: ${XIAOU_AI_RAG_ENDPOINT:http://localhost:18080}
      api-key: ${XIAOU_AI_RAG_API_KEY:}
```

本地开发设置 AI 最简单的方式是设置环境变量：

```bash
export XIAOU_AI_BASE_URL=https://your-openai-proxy.example.com/v1
export XIAOU_AI_API_KEY=sk-xxxx
export XIAOU_AI_CHAT_MODEL=gpt-4o
```

> **提示**：不配置 AI 也能正常启动，只是 AI 相关接口会返回降级响应。

### 4. P6Spy SQL 日志（开发用）

开发环境默认启用 P6Spy 打印完整 SQL，配置文件在 `xiaou-application/src/main/resources/spy.properties`。

P6Spy 的作用：
- 拦截所有 JDBC 调用，打印完整 SQL（含参数值）
- 不需要手动拼接 `?` 占位符，直接看到实际执行的 SQL
- 日志输出到控制台和 `logs/spy.log`

如果不需要 P6Spy，在 `application-dev.yml` 中改回标准驱动：

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/code_nest?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf-8
```

### 5. OJ 判题配置

OJ 判题依赖 go-judge 沙箱，配置在 `application.yml`：

```yaml
oj:
  judge:
    go-judge-url: http://154.222.18.220:5050   # 默认指向远端示例
    max-compile-time: 10000                      # 最大编译时间 ms
    default-time-limit: 2000                     # 默认运行时间限制 ms
    default-memory-limit: 256                    # 默认内存限制 MB
```

本地开发时如果要跑判题，需要启动本地 go-judge 并覆盖地址。详见 [本地完整启动剧本](/guide/startup-playbook) 的 OJ 联调模式。

### 6. 文件上传配置

```yaml
# application.yml 中已配置
spring:
  servlet:
    multipart:
      max-file-size: 100MB
      max-request-size: 100MB
```

文件存储类型通过 `xiaou.file.storage-type` 控制，本地开发默认使用 `local`。

## 后端启动

### 方式一：Maven 命令行

```bash
# 在项目根目录执行（-am 会自动构建依赖模块）
mvn -pl xiaou-application -am clean package -DskipTests

# 运行（默认 dev profile）
java -jar xiaou-application/target/xiaou-application-*.jar
```

### 方式二：IDE 启动

1. 用 IntelliJ IDEA 导入项目（选择 Maven 导入）
2. 找到 `xiaou-application/src/main/java/com/xiaou/CodeNestApplication.java`
3. 右键 → Run

### 启动验证

| 检查项 | 方法 | 期望结果 |
| --- | --- | --- |
| 服务启动 | 查看控制台 | Banner + Started CodeNestApplication |
| HTTP 端口 | `curl http://localhost:9999/api/actuator/health` | `{"status":"UP"}` |
| 数据库连接 | 查看启动日志 | "HikariPool-1 - Start completed" |
| Redis 连接 | 查看启动日志 | 无 Redis 连接错误 |
| Swagger | `curl http://localhost:9999/api/swagger-ui.html` | 返回 Swagger 页面 |
| Prometheus | `curl http://localhost:9999/api/actuator/prometheus` | 返回指标数据 |

> **注意**：后端启动在 9999 端口，context-path 为 `/api`。所有接口前缀都是 `/api/...`。

## 前端启动

### 用户端

```bash
cd vue3-user-front
npm install
npm run dev
```

启动后访问 `http://localhost:3001`，Vite 开发服务器自动代理 `/api` 到 `localhost:9999`。

### 管理端

```bash
cd vue3-admin-front
npm install
npm run dev
```

启动后访问 `http://localhost:3000`，同样代理 `/api` 到 `localhost:9999`。

### Vite 代理配置（源码）

用户端 `vue3-user-front/vite.config.js`：

```javascript
server: {
  port: 3001,
  open: true,
  proxy: {
    '/api': {
      target: 'http://localhost:9999',
      changeOrigin: true,
      secure: false,
    },
  },
},
```

管理端 `vue3-admin-front/vite.config.js`：

```javascript
server: {
  port: 3000,
  open: true,
  proxy: {
    '/api': {
      target: 'http://localhost:9999',
      changeOrigin: true,
      secure: false,
      // 不使用 rewrite，因为后端 context-path 已经有 /api 前缀
    },
  },
},
```

如果后端不在 9999 端口，修改 `target` 即可。**注意不要加 rewrite**，因为后端的 `server.servlet.context-path=/api` 已经包含了 `/api` 前缀。

### 代码分割策略

两个前端都使用 `manualChunks` 配置，将大型依赖拆分成独立 chunk：

| chunk 名称 | 包含内容 | 前端 |
| --- | --- | --- |
| `vendor-element` | Element Plus | 两者 |
| `vendor-monaco` | Monaco Editor | 用户端 |
| `vendor-graph` | @antv/g6 | 两者 |
| `vendor-charts` | ECharts、D3 | 管理端 |
| `vendor-markdown` | markdown-it + highlight.js + DOMPurify | 两者 |
| `vendor-vue` | Vue 核心 | 两者 |
| `vendor-router` | vue-router | 两者 |
| `vendor-state` | Pinia + @vueuse | 两者 |
| `vendor-utils` | axios + lodash + js-cookie + nprogress | 两者 |

### 常见前端问题

| 问题 | 原因 | 解决 |
| --- | --- | --- |
| `npm install` 失败 | 网络问题 / Node 版本 | 切换 npm 镜像：`npm config set registry https://registry.npmmirror.com` |
| 页面白屏 | 后端未启动 | 先启动后端 |
| 接口 404 | context-path 不匹配 | 确认后端 `server.servlet.context-path=/api`，前端代理不加 rewrite |
| 接口 CORS 错误 | Vite 代理未生效 | 检查 `vite.config.js` 中的 proxy 配置 |
| Monaco Editor 加载慢 | chunk 较大 | 正常现象，首次加载约 2-3MB |
| Node 22+ 安装失败 | peer deps 冲突 | 使用 `npm install --legacy-peer-deps` |

## 文档站启动

```bash
cd docs-site
npm install
npm run dev
```

启动后访问 `http://localhost:5175`（VitePress 默认端口）。

## 完整启动顺序

```
1. MySQL     → 确保 code_nest 数据库已创建，表结构已导入
2. Redis     → 确保 Redis 服务已启动（redis-cli ping → PONG）
3. 后端      → mvn -pl xiaou-application -am spring-boot:run
4. 用户端    → cd vue3-user-front && npm run dev    (port 3001)
5. 管理端    → cd vue3-admin-front && npm run dev    (port 3000)
6. 文档站    → cd docs-site && npm run dev           (port 5175)
```

## 开发常用命令速查

| 操作 | 命令 |
| --- | --- |
| 后端全量构建 | `mvn clean install -DskipTests` |
| 后端单模块构建 | `mvn install -pl xiaou-ai -am -DskipTests` |
| 后端启动 | `cd xiaou-application && mvn spring-boot:run` |
| 后端跳过测试 | `mvn spring-boot:run -DskipTests` |
| 用户端启动 | `cd vue3-user-front && npm run dev` |
| 管理端启动 | `cd vue3-admin-front && npm run dev` |
| 用户端构建 | `cd vue3-user-front && npm run build` |
| 管理端构建 | `cd vue3-admin-front && npm run build` |
| Electron 打包(用户) | `cd vue3-user-front && npm run dist:win` |
| Electron 打包(管理) | `cd vue3-admin-front && npm run dist:win` |
| 文档站启动 | `cd docs-site && npm run dev` |
| 文档站构建 | `cd docs-site && npm run build` |
| 查看 SQL 日志 | 检查控制台 P6Spy 输出或 `logs/spy.log` |
| 查看 Redis 数据 | `redis-cli -n 3 keys "*"`（业务）、`redis-cli -n 4 keys "satoken:*"`（登录态） |

## IDE 配置建议

### IntelliJ IDEA

1. **Lombok 插件** — 安装 Lombok 插件，启用 Annotation Processing
2. **Maven 导入** — 使用 Maven 导入项目，自动识别模块
3. **编码设置** — UTF-8 全局编码
4. **JDK 配置** — Project SDK 选择 JDK 17
5. **Run Configuration** — Main class: `com.xiaou.CodeNestApplication`，VM options: `-Dspring.profiles.active=dev`

### VS Code（前端）

1. **ESLint 插件** — 代码检查
2. **Prettier 插件** — 代码格式化
3. **Vue Language Features (Volar)** — Vue 3 语法支持
4. **TypeScript Vue Plugin** — TS 支持

## 环境变量

| 变量 | 说明 | 默认值 |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | Spring Profile | dev |
| `JAVA_OPTS` | JVM 参数（Docker 部署用） | 空 |
| `XIAOU_AI_PROVIDER` | AI Provider | openai-compatible |
| `XIAOU_AI_BASE_URL` | AI API Base URL | 空 |
| `XIAOU_AI_API_KEY` | AI API Key | 空 |
| `XIAOU_AI_CHAT_MODEL` | 聊天模型 | gpt-5.4 |
| `XIAOU_AI_RAG_ENABLED` | 是否启用 RAG | false |
| `XIAOU_AI_RAG_ENDPOINT` | RAG sidecar 地址 | `http://localhost:18080` |
| `XIAOU_JWT_SECRET` | JWT 签名密钥 | 开发默认占位值 |

完整环境变量表见 [环境变量总表](/operations/env-vars)。

## 源码导航

| 文件 | 说明 |
| --- | --- |
| `xiaou-application/src/main/resources/application.yml` | 主配置文件（Sa-Token、AI、监控、OJ） |
| `xiaou-application/src/main/resources/application-dev.yml` | 开发环境配置（P6Spy、localhost） |
| `xiaou-application/src/main/resources/application-docker.yml` | Docker 配置（环境变量注入） |
| `vue3-user-front/vite.config.js` | 用户端 Vite 配置（端口 3001、代理、代码分割） |
| `vue3-admin-front/vite.config.js` | 管理端 Vite 配置（端口 3000、代理、代码分割） |
| `vue3-user-front/src/utils/request.js` | 用户端 Axios 实例 |
| `vue3-admin-front/src/utils/request.js` | 管理端 Axios 实例 |
| `sql/MySql/code_nest.sql` | 基础建表脚本 |
| `sql/` | 增量版本脚本目录 |
| `docker/Dockerfile` | 后端 Docker 镜像（两阶段构建） |
| `docker/env/example.env` | Docker 环境变量示例 |


## 相关文档

| 文档 | 说明 |
| --- | --- |
| [快速开始](/guide/quick-start) | 快速搭建指南 |
| [本地完整启动剧本](/guide/startup-playbook) | 完整启动步骤 |
| [架构总览](/architecture/overview) | 技术栈说明 |
| [Docker 与服务部署](/operations/docker) | Docker 环境 |
