# 快速开始

本页帮助你从零启动 Code-Nest 的**后端**、**前端**和**文档站**。

## 环境要求

| 工具 | 版本 | 说明 |
|------|------|------|
| Java | 17+ | 后端运行时 |
| Maven | 3.8+ | 后端构建 |
| MySQL | 8.0+ | 数据库 |
| Redis | 7.x | 缓存/会话 |
| Node.js | 18+ | 前端/文档站 |
| npm | 9+ | 包管理 |
| Git | 2.30+ | 版本管理 |

## 后端启动

### 1. 初始化数据库

```bash
# 导入基线结构
mysql -u root -p code_nest < sql/MySql/code_nest.sql

# 导入初始化数据
mysql -u root -p code_nest < sql/MySql/code_nest_data.sql
```

### 2. 配置开发环境

编辑 `xiaou-application/src/main/resources/application-dev.yml`，确认：

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `spring.datasource.url` | `jdbc:p6spy:mysql://localhost:3306/code_nest` | 数据库连接 |
| `spring.datasource.username` | `root` | 数据库用户名 |
| `spring.datasource.password` | `1234` | 数据库密码 |
| `spring.data.redis.redisson.config` | `redis://127.0.0.1:6379` | Redis 地址 |
| `sa-token.alone-redis.host` | `127.0.0.1` | Sa-Token Redis |

**敏感配置**：如需 AI 功能，在 `application-sec.yml`（不提交 Git）中配置：

```yaml
xiaou:
  ai:
    base-url: <your-api-base-url>
    api-key: <your-api-key>
```

或通过环境变量 `XIAOU_AI_BASE_URL` / `XIAOU_AI_API_KEY` 注入。

### 3. 启动后端

```bash
# 在项目根目录
mvn clean install -DskipTests

# 启动应用
cd xiaou-application
mvn spring-boot:run
```

启动成功后，终端会打印：

```text
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🎉 Code-Nest 启动成功！
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

🌐 运行环境:
环境: dev
端口: 9999
路径: /api

🌍 访问地址:
首页: http://localhost:9999/api
API文档: http://localhost:9999/api/swagger-ui.html
```

### 4. 验证后端

| 检查项 | 方法 |
|--------|------|
| 健康检查 | `curl http://localhost:9999/api/actuator/health` |
| Swagger 文档 | 浏览器打开 `http://localhost:9999/api/swagger-ui.html` |
| Prometheus 指标 | `curl http://localhost:9999/api/actuator/prometheus` |

## 前端启动

### 用户端

```bash
cd vue3-user-front
npm install
npm run dev2
```

默认访问地址：`http://localhost:5173`

### 管理端

```bash
cd vue3-admin-front
npm install
npm run dev
```

默认访问地址：`http://localhost:5174`

### 前端 API 代理

两个前端项目的 `vite.config.js` 已配置开发代理，将 `/api` 请求转发到 `http://localhost:9999`。如果后端端口不是 9999，需要修改对应配置。

## 文档站启动

```bash
cd docs-site
npm install
npm run dev
```

默认访问地址：`http://localhost:5175`

### 构建文档站

```bash
cd docs-site
npm run build
```

构建产物位于 `docs-site/.vitepress/dist`，可部署到 Nginx、Vercel、GitHub Pages 等静态托管平台。

## 完整启动顺序

```text
1. MySQL    → 确保服务运行，code_nest 库已导入
2. Redis    → 确保服务运行
3. 后端     → mvn spring-boot:run (端口 9999)
4. 用户端   → npm run dev2 (端口 5173)
5. 管理端   → npm run dev  (端口 5174)
6. 文档站   → npm run dev  (端口 5175)
```

## 常见启动问题

| 问题 | 检查方向 |
|------|----------|
| 后端启动报 MySQL 连接失败 | MySQL 是否启动、端口/用户名/密码是否正确 |
| 后端启动报 Redis 连接失败 | Redis 是否启动、端口是否正确 |
| 前端接口 401 | 是否已登录、Token 是否正确 |
| 前端接口 CORS 报错 | 后端 CorsConfig 白名单是否包含前端 Origin |
| AI 功能不可用 | application-sec.yml 或环境变量中的 API Key 是否配置 |
| OJ 判题超时 | go-judge 沙箱服务是否可达 |

更多排查见 [常见问题排查](/operations/troubleshooting)。

## 与旧文档的关系

现有 `docs/` 目录继续作为资料库保留，包括 PRD、截图手册、技术方案、升级计划和归档文档。

`docs-site/` 负责对这些资料重新组织成"读者能顺着看"的文档站。后续迁移或引用旧资料时，优先保持原始文件不动，再在文档站里写结构化说明。

## 验证清单

第一次启动时，确认：

1. `mvn clean install -DskipTests` 能在项目根目录完成。
2. 后端启动后 `/api/actuator/health` 返回 `{"status":"UP"}`。
3. 用户端 `npm run dev2` 能打开登录页面。
4. 管理端 `npm run dev` 能打开登录页面。
5. 文档站 `npm run dev` 能打开首页。
6. 用户端登录后能看到面试题库页面。
7. 管理端登录后能看到仪表盘。
8. 修改任意 Markdown 后，文档站开发服务能热更新。
