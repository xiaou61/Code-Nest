# 本地开发

本页描述 Code Nest 全项目的本地启动顺序。文档站本身可以独立运行，但理解业务功能时通常需要后端、数据库、Redis 和两个前端同时准备。

## 推荐启动顺序

1. 启动 MySQL 和 Redis。
2. 导入数据库基线脚本与必要增量脚本。
3. 启动后端聚合工程 `xiaou-application`。
4. 启动管理端 `vue3-admin-front`。
5. 启动用户端 `vue3-user-front`。
6. 按需启动 AI 知识库 sidecar `llamaindex-service`。
7. 启动文档站 `docs-site`。

## 后端

后端是 Maven 多模块工程，根目录 `pom.xml` 管理所有 `xiaou-*` 模块。聚合启动入口在：

```text
xiaou-application/src/main/java/com/xiaou/CodeNestApplication.java
```

常用命令：

```bash
mvn -pl xiaou-application -am spring-boot:run
```

打包验证：

```bash
mvn -pl xiaou-application -am clean package -DskipTests
```

默认后端地址是：

```text
http://localhost:9999/api
```

这来自 `xiaou-application/src/main/resources/application.yml` 的 `server.port=9999` 和 `server.servlet.context-path=/api`。本地开发时前端不需要手动拼完整后端域名，两个 Vite 应用都会把 `/api` 代理到 `http://localhost:9999`。

## 管理端

```bash
cd vue3-admin-front
npm install
npm run dev
```

管理端默认面向运营和管理员，包含用户、题库、OJ、内容审核、AI 配置、文件、积分、日志和系统管理等页面。

管理端默认端口是 `3000`，`vite.config.js` 中的 `/api` 代理不做 rewrite，因为后端本身已经带 `/api` 上下文。

## 用户端

```bash
cd vue3-user-front
npm install
npm run dev2
```

用户端面向开发者，包含刷题、OJ、模拟面试、求职闭环、学习驾驶舱、社区、动态、博客、代码工坊、简历、闪卡、计划、团队、摸鱼工具等功能。

用户端默认端口是 `3001`。聊天室默认 WebSocket 地址为 `ws://localhost:9999/api`，连接前会先调用 `POST /user/chat/ws-ticket` 获取一次性票据，再连接 `/ws/chat?ticket=...`。

## 本地跨域和静态文件

后端 CORS 和 WebSocket Origin 使用同一个白名单配置：

```text
xiaou.cors.allowed-origin-patterns
```

默认已包含 `localhost:3000`、`localhost:3001` 和 `localhost:5173` 等本地开发地址。如果你临时改了前端端口，需要同步改这个配置。

本地上传文件会保存在后端工作目录的 `uploads/` 下，外部访问一般是：

```text
http://localhost:9999/api/files/...
```

## AI sidecar

`llamaindex-service/` 是 Python 知识库 sidecar，服务于 AI Runtime 的 RAG 导入、检索和文档管理。

常见入口：

```text
llamaindex-service/README.md
docker/ai/README.md
scripts/ai/
```

## 文档站

```bash
cd docs-site
npm install
npm run dev
```

文档站只依赖 Node.js，不要求后端服务在线。
