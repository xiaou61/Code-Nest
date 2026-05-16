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

## 管理端

```bash
cd vue3-admin-front
npm install
npm run dev
```

管理端默认面向运营和管理员，包含用户、题库、OJ、内容审核、AI 配置、文件、积分、日志和系统管理等页面。

## 用户端

```bash
cd vue3-user-front
npm install
npm run dev2
```

用户端面向开发者，包含刷题、OJ、模拟面试、求职闭环、学习驾驶舱、社区、动态、博客、代码工坊、简历、闪卡、计划、团队、摸鱼工具等功能。

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

