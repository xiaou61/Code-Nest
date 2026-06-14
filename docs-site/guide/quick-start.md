# 快速开始

这页帮你从零把 Code Nest 跑起来。如果你已经跑过项目但遇到了问题，直接跳到 [常见启动问题](#常见启动问题)。

如果你想了解整体架构再动手，先看 [后端架构](/architecture/backend-modules) 和 [前端架构](/architecture/frontend-apps)。如果准备开始写第一个功能，看 [功能开发流程](/guide/feature-development) 和 [首个真实任务接入指南](/guide/first-real-task)。

## 前置依赖

| 依赖 | 最低版本 | 推荐版本 | 验证命令 |
| --- | --- | --- | --- |
| Java JDK | 17 | 17 | `java -version` |
| Maven | 3.8+ | 3.9+ | `mvn -version` |
| Node.js | 18 | 20 LTS | `node -v` |
| npm | 9+ | 10+ | `npm -v` |
| MySQL | 8.0 | 8.0 | `mysql --version` |
| Redis | 7.0+ | 7.x | `redis-cli ping` → `PONG` |
| Git | 2.x | 最新 | `git --version` |

可选依赖（按需启动）：

| 依赖 | 用途 | 默认端口 |
| --- | --- | --- |
| Python 3.10+ | AI RAG sidecar | 5000 |
| go-judge | OJ 判题 | 5050 / 8974 |
| MinIO / S3 | 对象存储（本地可用本地文件系统） | 9000 |

## 第 1 步：克隆仓库

```powershell
git clone https://github.com/your-org/Code-Nest.git
cd Code-Nest
```

确认当前分支：

```powershell
git branch
# 应该看到 * v2.3.0 或对应版本分支
```

## 第 2 步：初始化数据库

### 创建数据库

```sql
CREATE DATABASE code_nest DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```

### 导入表结构

```powershell
mysql -u root -p code_nest < sql/MySql/code_nest.sql
```

如果 `code_nest.sql` 不在预期位置，用以下命令确认：

```powershell
dir /s /b sql\MySql\code_nest.sql
```

### 导入测试数据（可选）

如果仓库里有测试数据 SQL，在导入表结构之后执行。

### 确认表数量

```sql
USE code_nest;
SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'code_nest';
-- 预期：142 张表左右（视版本可能有差异）
```

## 第 3 步：配置后端

后端配置文件位于 `xiaou-application/src/main/resources/`：

| 文件 | 用途 |
| --- | --- |
| `application.yml` | 主配置（激活 profile、公共配置） |
| `application-dev.yml` | 本地开发（默认激活） |
| `application-docker.yml` | Docker 部署 |
| `application-prod.yml` | 生产环境（占位） |
| `application-sec.yml` | 敏感配置（密钥、Token） |

### 数据库连接

编辑 `application-dev.yml`，确认数据库连接信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/code_nest?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver
```

如果你使用 P6Spy（开发环境默认启用），实际驱动是 `com.p6spy.engine.spy.P6SpyDriver`，URL 前缀是 `jdbc:p6spy:mysql://`。P6Spy 会打印完整 SQL 日志，方便本地调试。如果不需要，将 `application-dev.yml` 中的 P6Spy 配置注释掉。

### Redis 连接

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password:        # 本地开发通常为空
      database: 0
```

Sa-Token 双登录域使用独立的 Redis 连接：

```yaml
sa-token:
  token-name: satoken
  timeout: 86400        # Token 有效期 24 小时
  active-timeout: -1    # 活跃超时（-1 表示不限）
  is-concurrent: true   # 允许多端同时登录
  is-share: false       # 不共享 Token

  # 用户端 Redis（alone-redis）
  alone-redis:
    database: 1          # DB 1
    host: localhost
    port: 6379

  # 管理端 Redis 通过 StpAdminUtil 的 loginType="admin" 隔离
```

### 文件存储

```yaml
xiaou:
  file:
    storage-type: local  # 本地开发用 local，生产用 minio 或 s3
    local:
      upload-dir: ./uploads
      base-url: http://localhost:9999
```

### AI 配置（可选）

```yaml
xiaou:
  ai:
    enabled: false  # 本地开发默认关闭
    # 如果需要 AI 功能，改为 true 并配置 API Key
```

如果 AI 关闭，涉及 AI 的接口会返回降级响应，不影响其他功能。

## 第 4 步：启动后端

```powershell
mvn -pl xiaou-application -am clean package -DskipTests
```

启动成功后，后端默认监听 `http://localhost:9999`：

```text
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/

 :: Spring Boot ::                (v3.4.4)

... Application started on port 9999
```

验证后端启动：

```powershell
curl http://localhost:9999/captcha/image
# 应返回 JSON 包含 captcha
```

### 常见启动报错

| 报错 | 原因 | 解决 |
| --- | --- | --- |
| `Communications link failure` | MySQL 未启动或连接信息错误 | 检查 MySQL 是否运行、端口和密码 |
| `Unable to connect to Redis` | Redis 未启动 | `redis-cli ping` 确认 Redis 可用 |
| `Table 'code_nest.xxx' doesn't exist` | SQL 未导入或版本不匹配 | 重新导入 `code_nest.sql` |
| `Port 9999 already in use` | 端口被占用 | 改 `application-dev.yml` 中的 `server.port`，或关闭占用进程 |
| `P6Spy ClassNotFoundException` | P6Spy 依赖缺失 | 确认 pom 里 p6spy 依赖未注释，或关闭 P6Spy 配置 |

## 第 5 步：启动用户端前端

```powershell
cd vue3-user-front
npm install
npm run dev
```

用户端默认监听 `http://localhost:3001`（由 `vite.config.js` 中 `server.port` 指定）。

Vite 代理配置（`vite.config.js`）：

```javascript
// vue3-user-front/vite.config.js
export default defineConfig({
  server: {
    port: 3001,
    proxy: {
      '/api': {
        target: 'http://localhost:9999',  // 后端地址
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '')
      }
    }
  }
})
```

如果后端不在 9999，改 `target` 即可。

验证用户端启动：

1. 打开 `http://localhost:3001`。
2. 看到登录或首页。
3. 注册一个用户。
4. 登录成功后看到主页面。

## 第 6 步：启动管理端前端

```powershell
cd vue3-admin-front
npm install
npm run dev
```

管理端默认监听 `http://localhost:3000`（或 Vite 分配的端口）。

Vite 代理配置（`vite.config.js`）：

```javascript
// vue3-admin-front/vite.config.js
export default defineConfig({
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:9999',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '')
      }
    }
  }
})
```

验证管理端启动：

1. 打开管理端地址。
2. 使用管理员账号登录（默认账号在数据库 `admin` 表中，或查看 SQL 初始化脚本）。
3. 登录成功后看到后台首页。

## 第 7 步：启动文档站（可选）

```powershell
cd docs-site
npm install
npm run dev
```

文档站默认监听 `http://localhost:5175`。

## 常见启动问题

### 后端问题

#### 数据库连接失败

```text
Communications link failure
The last packet sent successfully to the server was 0 milliseconds ago.
```

排查步骤：

1. 确认 MySQL 正在运行：`mysql -u root -p -e "SELECT 1"`
2. 确认端口正确：`netstat -an | findstr 3306`
3. 确认 `application-dev.yml` 中 URL、用户名和密码正确
4. 确认 MySQL 允许本地连接：检查 `bind-address` 配置
5. 如果用了 P6Spy，确认 URL 前缀是 `jdbc:p6spy:mysql://`

#### Redis 连接失败

```text
Unable to connect to Redis; nested exception is io.lettuce.core.RedisConnectionException
```

排查步骤：

1. 确认 Redis 正在运行：`redis-cli ping` → 应返回 `PONG`
2. 如果 Redis 设了密码，在 `application-dev.yml` 中补 `password`
3. 如果 Redis 端口不是 6379，修改 `spring.data.redis.port`
4. 确认 Sa-Token 的 alone-redis 配置与主 Redis 一致（除了 database 编号）

#### Maven 构建失败

```text
Could not resolve dependencies / Failed to execute goal on project xiaou-application
```

排查步骤：

1. 确认根目录 `pom.xml` 中的 `<revision>` 版本号与子模块一致
2. 先在根目录执行 `mvn clean install -DskipTests` 安装公共依赖
3. 如果某个模块下载失败，检查 Maven 仓库设置（`~/.m2/settings.xml`）
4. 清理本地缓存：`mvn dependency:purge-local-repository`

#### 后端启动成功但接口返回 701

这通常意味着 Sa-Token 路径拦截规则正确拦截了需要认证的接口。确认：

1. 公开接口（如注册、验证码）已在 `SaTokenConfig` 的 `.notMatch()` 中放行
2. 测试时先调用登录接口获取 Token
3. 后续请求在 Header 中携带 `satoken: <your-token>`

### 前端问题

#### npm install 失败

```text
npm ERR! code ERESOLVE / ECONNREFUSED
```

排查步骤：

1. 切换 npm 源：`npm config set registry https://registry.npmmirror.com`
2. 清理缓存：`npm cache clean --force`
3. 删除 `node_modules` 和 `package-lock.json`，重新 `npm install`
4. 如果使用 Node 22+，可能需要 `--legacy-peer-deps`：`npm install --legacy-peer-deps`

#### 前端代理 502 / 接口返回网络错误

```text
Proxy error: Could not proxy request /api/... from localhost:3001 to http://localhost:9999
```

排查步骤：

1. 确认后端正在运行：`curl http://localhost:9999/captcha/image`
2. 确认后端端口与 Vite 代理 `target` 一致
3. 如果后端刚启动不久，等待 Spring Boot 完全初始化
4. 检查防火墙是否阻止了本地端口通信

#### 前端页面空白

排查步骤：

1. 打开浏览器 DevTools Console，看是否有 JS 报错
2. 确认 `npm run build` 能成功（排除语法错误）
3. 如果用了路由懒加载，确认路由路径正确
4. 确认后端接口返回数据格式与前端期望一致

#### 登录后跳转异常

排查步骤：

1. 确认后端登录接口返回了 Token
2. 确认前端正确存储了 Token（检查 localStorage 或 Pinia store）
3. 确认后续请求的 Header 包含 `satoken: <token>`
4. 如果 Token 过期很快，检查 `sa-token.timeout` 配置

### 构建问题

#### 用户端 build 失败

```powershell
cd vue3-user-front && npm run build
```

常见原因：

| 报错 | 原因 | 解决 |
| --- | --- | --- |
| `SyntaxError` | 语法错误 | 按报错行号修复 |
| `Module not found` | 依赖缺失 | `npm install` |
| `Type error` | 类型不匹配 | 检查 import 路径和组件 props |
| `Chunk size limit` | 包体积超限 | 检查是否有大依赖未拆分 |

#### 管理端 build 失败

同上，在 `vue3-admin-front` 目录执行 `npm run build`。

#### 文档站 build 失败

```powershell
cd docs-site && npm run build
```

常见原因：

| 报错 | 原因 | 解决 |
| --- | --- | --- |
| `Unknown anchor` | 内部链接断链 | 修复 `[text](/path#anchor)` |
| `Invalid markdown` | VitePress 语法错误 | 检查 Vue template 语法 |
| `404 page` | sidebar 引用了不存在的页面 | 确认 .md 文件存在 |

## 启动成功后的下一步

| 你想做什么 | 去哪里 |
| --- | --- |
| 了解整体架构 | [后端架构](/architecture/backend-modules)、[前端架构](/architecture/frontend-apps) |
| 写第一个功能 | [功能开发流程](/guide/feature-development)、[首个真实任务接入指南](/guide/first-real-task) |
| 扩展已有模块 | [常见二开场景](/guide/extension-scenarios) |
| 了解权限和安全 | [权限与安全边界](/guide/security-boundaries) |
| 本地开发细节 | [本地开发环境](/guide/local-dev) |
| 部署到服务器 | [Docker 与服务部署](/operations/docker) |
| 查看所有模块 | [模块总览](/modules/) |


## 相关文档

| 文档 | 说明 |
| --- | --- |
| [本地开发](/guide/local-dev) | 详细开发环境配置 |
| [本地完整启动剧本](/guide/startup-playbook) | 完整启动步骤 |
| [架构总览](/architecture/overview) | 了解整体架构 |
| [模块总览](/modules/) | 各业务模块介绍 |
| [Docker 与服务部署](/operations/docker) | 生产环境部署 |
