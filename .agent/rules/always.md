---
artifact: project-rules
status: active
configured: true
schema_version: 1
---

# Code-Nest 项目常驻规范

本文件保存经用户确认、跨工作项长期有效的项目事实与约束。仓库根 `SKILL.md`（`code-nest-project-spec`）是更详细的项目规范；两者冲突时以本文件为准，并回修 `SKILL.md`。

## 项目事实

- 版本：`VERSION` = 2.5.8；根 `pom.xml` 的 `<revision>` = v2.5.8；`vue3-admin-front` 与 `vue3-user-front` 的 `package.json` version = 2.5.8。
- 后端：Maven 多模块，Java 17，Spring Boot 3.4.4，端口 9999，统一上下文路径 `/api`。
- 关键依赖：Sa-Token 1.44.0、MyBatis-Spring-Boot 3.0.3、Druid 1.2.20、Redisson 3.24.3、PageHelper 6.1、LangChain4j 1.13.0、LangGraph4j 1.8.13。
- 分层：`xiaou-bootstrap` 为启动层（唯一启用 `spring-boot-maven-plugin`，`mainClass = com.xiaou.bootstrap.CodeNestApplication`）；`xiaou-application` 为业务聚合层，持有 34 个业务模块依赖、`web` 包与 `resources/mapper`。
- 两套前端均支持 Electron 打包，入口在各自 `electron/`；`sql/` 采用“基线 + 版本增量”结构。

## MUST

### 路径与配置

- 启动主类：`xiaou-bootstrap/src/main/java/com/xiaou/bootstrap/CodeNestApplication.java`
- 主配置：`xiaou-bootstrap/src/main/resources/application.yml`；另有 `application-dev.yml`、`application-docker.yml`、`application-prod.yml`
- 私密配置：`xiaou-bootstrap/src/main/resources/application-sec.yml`（已被 `.gitignore` 排除，不得提交，不得写入工件）
- 统一返回体：`xiaou-common-core/src/main/java/com/xiaou/common/core/domain/Result.java`、`ResultCode.java`
- 鉴权：`xiaou-common-security/src/main/java/com/xiaou/common/config/SaTokenConfig.java`、`satoken/StpAdminUtil.java`、`satoken/StpUserUtil.java`、`annotation/RequireAdmin.java`
- 前端请求入口：`vue3-admin-front/src/utils/request.js`、`vue3-user-front/src/utils/request.js`
- 文档根目录：`AI-DOCS/`；仓库无 `docs/`

### 启动与验证

- 后端编译：`mvn -pl xiaou-bootstrap -am clean package -DskipTests`
- 后端启动：`mvn -pl xiaou-bootstrap -am spring-boot:run`
- 管理端：`cd vue3-admin-front && npm run dev`（端口 3000，代理至 `http://localhost:9999`）
- 用户端：`cd vue3-user-front && npm run dev2`（端口 3001，代理至 `http://localhost:9999`）
- 前端 lint：两端 `npm run lint`
- 禁止对 `xiaou-application` 使用 `spring-boot:run`：该模块未启用 `spring-boot-maven-plugin`，命令必然失败。

### 接口与返回体

- 所有后端接口统一返回 `Result<T>`；业务码 200 成功、701 Token 无效、702 Token 过期、703 权限不足、704 账号禁用；前端拦截器依赖这些语义，不得改动。
- 路由分区：管理端 `/auth/**`、`/admin/**`；用户端 `/user/**`；公共 `/oj/**`、`/community/**`、`/version/**`；免登录 `/captcha/**`、`/v3/api-docs/**`、`/swagger-ui/**`。
- 鉴权复用 `StpAdminUtil` / `StpUserUtil`；管理端方法优先 `@RequireAdmin`。
- 前端 token key：用户端 `user_token`，管理端 `token`。

### 落点与生成物

- 业务逻辑落在对应业务模块，不得塞进 `xiaou-bootstrap` 或 `xiaou-application`。
- 新增业务模块必须同步根 `pom.xml` 的 `<modules>` 与 `xiaou-application/pom.xml` 依赖。
- OJ 特例：Mapper XML 位于 `xiaou-oj/src/main/java/com/xiaou/oj/mapper/`（该模块无 `resources/mapper`）。
- SQL：基线 `sql/MySql/code_nest.sql`、`code_nest_data.sql`；结构变更写增量到 `sql/vX.Y.Z/`，避免破坏性 `DROP`。
- 禁止手改：`pom-xml-flattened`、`target/`、`dist/`、`out/`、`node_modules/`。

### 版本同步

- 改版本必须同步 `VERSION`、根 `pom.xml` 的 `<revision>`、两端 `package.json` version，并运行 `scripts/check-version-consistency.py` 与 `scripts/check-architecture.py`。

### 工具链边界

- 本仓库的 git 传输需经本地代理 `http://127.0.0.1:7897`（已写入 `.git/config` 的 `http.proxy`/`https.proxy`）；直连会被限速到 15–50 KiB/s 并频繁断流。
