# 本地开发环境搭建

本页详细说明在本地搭建 Code-Nest 开发环境的完整流程，包括后端、前端、数据库、缓存、AI 等全部依赖。

## 环境要求

| 依赖 | 最低版本 | 推荐版本 | 验证命令 |
| --- | --- | --- | --- |
| JDK | 17 | 17+ | `java -version` |
| Maven | 3.8 | 3.9+ | `mvn -version` |
| Node.js | 18 | 20 LTS | `node -v` |
| npm | 9 | 10+ | `npm -v` |
| MySQL | 8.0 | 8.0+ | `mysql --version` |
| Redis | 7.0 | 7.2+ | `redis-server --version` |
| Git | 2.30 | 2.40+ | `git --version` |

> **注意**：JDK 17 是硬性要求，Spring Boot 3.4.x 不支持 JDK 8/11。

## 数据库初始化

### 1. 创建数据库

```sql
CREATE DATABASE code_nest DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. 导入基础表结构

```bash
mysql -u root -p code_nest < sql/MySql/code_nest.sql
```

基础脚本包含 97 张表 + 1 个视图，是系统运行的最低要求。

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

### 4. 创建数据库用户（可选）

```sql
CREATE USER 'code_nest'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON code_nest.* TO 'code_nest'@'localhost';
FLUSH PRIVILEGES;
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
| 业务缓存 | db3 | 接口缓存、验证码、Ticket 等 |
| Sa-Token 会话 | db4 | 用户端 + 管理端登录态 |
| 消息队列 | db3 | WebSocket 消息中转 |

> **注意**：本地开发无需修改 Redis DB 编号，保持默认即可。生产环境建议不同模块使用不同 Redis 实例。

## 后端配置

### 1. 修改 application-dev.yml

配置文件位于 `xiaou-application/src/main/resources/application-dev.yml`，需要修改以下项：

```yaml
# MySQL 连接（必改）
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/code_nest?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Shanghai
    username: root          # 改为你的数据库用户名
    password: 123456        # 改为你的数据库密码

# Redis 连接（按需改）
  data:
    redis:
      host: localhost       # Redis 地址
      port: 6379            # Redis 端口
      password:             # Redis 密码（无密码留空）
      database: 3           # 默认 db3

# Sa-Token 配置（按需改）
sa-token:
  token-name: Authorization
  timeout: 86400            # Token 有效期（秒），默认 24h
  activity-timeout: 1800    # 活跃超时（秒），默认 30min
```

### 2. AI 功能配置（按需）

如果需要使用 AI 功能，需要配置大模型 API Key：

```yaml
ai:
  models:
    default:
      provider: openai       # 支持 openai / zhipu / dashscope
      model: gpt-4o
      api-key: sk-xxx        # 你的 API Key
      base-url: https://api.openai.com/v1  # 按需修改
  timeout:
    connect: 10000           # 连接超时 10s
    read: 60000              # 读取超时 60s
  retry:
    max-attempts: 2          # 最大重试次数
    backoff: 1000            # 重试间隔 ms
```

> **提示**：不配置 AI 也能正常启动，只是 AI 相关接口会返回"未配置"错误。

### 3. P6Spy SQL 日志（开发用）

开发环境默认启用 P6Spy 打印完整 SQL，配置文件在 `xiaou-application/src/main/resources/spy.properties`。如果觉得日志太多，可以在 `application-dev.yml` 中注释掉 P6Spy 的 driver 和 url 配置。

### 4. 文件上传配置

```yaml
file:
  storage-type: local        # 本地存储（开发用）
  local:
    path: /tmp/code-nest     # 本地存储路径
  max-size: 10MB             # 文件大小限制
```

## 后端启动

### 方式一：Maven 命令行

```bash
# 在项目根目录执行
mvn clean install -DskipTests
cd xiaou-application
mvn spring-boot:run
```

### 方式二：IDE 启动

1. 用 IntelliJ IDEA 导入项目（选择 Maven 导入）
2. 找到 `xiaou-application/src/main/java/com/xiaou/CodeNestApplication.java`
3. 右键 → Run

### 启动验证

| 检查项 | 方法 | 期望结果 |
| --- | --- | --- |
| 服务启动 | 查看控制台 | Banner + Started CodeNestApplication |
| HTTP 端口 | `curl localhost:9999/api` | 返回 200 或 401 |
| 数据库连接 | 查看启动日志 | "HikariPool-1 - Start completed" |
| Redis 连接 | 查看启动日志 | 无 Redis 连接错误 |
| Actuator | `curl localhost:9999/api/actuator/health` | {"status":"UP"} |

> **注意**：后端启动在 9999 端口，context-path 为 `/api`。

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

### Vite 代理配置

两端均在 `vite.config.js` 中配置代理：

```js
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:9999',
      changeOrigin: true
    }
  }
}
```

如果后端不在 9999 端口，需要同步修改前端的 proxy 配置。

### 常见前端问题

| 问题 | 原因 | 解决 |
| --- | --- | --- |
| `npm install` 失败 | 网络问题 / Node 版本 | 切换 npm 镜像：`npm config set registry https://registry.npmmirror.com` |
| 页面白屏 | 后端未启动 | 先启动后端 |
| 接口 404 | context-path 不匹配 | 确认后端 `server.servlet.context-path=/api` |
| 接口 CORS 错误 | Vite 代理未生效 | 检查 `vite.config.js` 中的 proxy 配置 |
| Monaco Editor 加载慢 | chunk 较大 | 正常现象，首次加载约 2-3MB |

## 文档站启动

```bash
cd docs-site
npm install
npm run docs:dev
```

启动后访问 `http://localhost:5174`。

## 完整启动顺序

```
1. MySQL     → 确保 code_nest 数据库已创建
2. Redis     → 确保 Redis 服务已启动
3. 后端      → cd xiaou-application && mvn spring-boot:run
4. 用户端    → cd vue3-user-front && npm run dev
5. 管理端    → cd vue3-admin-front && npm run dev
6. 文档站    → cd docs-site && npm run docs:dev
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
| 文档站启动 | `cd docs-site && npm run docs:dev` |
| 文档站构建 | `cd docs-site && npm run docs:build` |
| 查看 SQL 日志 | 检查控制台 P6Spy 输出 |
| 查看 Redis 数据 | `redis-cli -n 3 keys "*"` |

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
| `SERVER_PORT` | 后端端口 | 9999 |
| `MYSQL_HOST` | MySQL 地址 | localhost |
| `MYSQL_PORT` | MySQL 端口 | 3306 |
| `REDIS_HOST` | Redis 地址 | localhost |
| `REDIS_PORT` | Redis 端口 | 6379 |

## 源码导航

| 文件 | 说明 |
| --- | --- |
| `xiaou-application/src/main/resources/application.yml` | 主配置文件 |
| `xiaou-application/src/main/resources/application-dev.yml` | 开发环境配置 |
| `vue3-user-front/vite.config.js` | 用户端 Vite 配置 |
| `vue3-admin-front/vite.config.js` | 管理端 Vite 配置 |
| `vue3-user-front/src/utils/request.js` | 用户端 Axios 实例 |
| `vue3-admin-front/src/utils/request.js` | 管理端 Axios 实例 |
| `sql/MySql/code_nest.sql` | 基础建表脚本 |
| `sql/` | 增量版本脚本目录 |
