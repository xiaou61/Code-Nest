---
artifact: tasks
work_id: WORK-001
work: 架构选型与项目骨架
status: approved
created: 2026-10-09
approved_by: xiaou61
approved_at: 2026-10-09 10:27:19 +0800
approver_role: CTO
---

# 架构选型与项目骨架实施任务

## 依据

- 已批准需求：`requirements.md`（REQ-001..REQ-008，AC-001..AC-008）
- 已批准提案：`proposal.md`
- 本工作项设计：`design.md`
- 技术选型裁决记录：`.agent/references/technology-options.md`、`.agent/rules/always.md`

## 全局约束

- 只建架构与技术底座，**不引入任何业务模块、业务实体或业务功能**。
- 后端模块间依赖必须在 `pom.xml` 声明；跨模块访问对方内部实现包由 Modulith 校验拦截。
- 一切连接信息、密钥只从环境变量或被忽略的本地配置读取；仓库内只放 `.env.example` 占位。本仓库为公开仓库。
- 数据库只面向 MySQL，可用其专有语法；主键为自增；分页由 MySQL 分页组件产生。
- 前端共享包（`packages/ui`、`packages/core`）不得引用桌面壳专有 API；资源路径必须相对。
- 每项任务的完成以「验证」列出的命令实际执行为准；未运行的检查在报告中如实写为未运行。

## 任务

### TASK-001 | pending | 后端 Maven 多模块工程与模块边界强制

- 对应：`REQ-005`、`AC-002`、`AC-004`
- 依赖：无
- 修改（计划）：`backend/pom.xml`、`backend/paideia-platform/pom.xml`、`backend/paideia-platform/src/main/java/io/github/xiaou61/platform/**`、`backend/paideia-app/pom.xml`、`backend/paideia-app/src/main/java/io/github/xiaou61/PaideiaApplication.java`、`backend/paideia-app/src/test/java/io/github/xiaou61/ModularityTest.java`
- 测试（计划）：`backend/paideia-app/src/test/java/io/github/xiaou61/ModularityTest.java`
- 步骤：
  1. 建聚合父 POM：JDK 25、Boot 4.1.1 依赖管理、模块清单。
  2. 建 `paideia-platform`：`ApiResponse<T>`、`ErrorCode`、`PageQuery`、`PageResult<T>`、`BizException`，并标注 `@ApplicationModule(type = OPEN)`。
  3. 建 `paideia-app`：主类 `PaideiaApplication` 于基础包根、Actuator 依赖、`GET /actuator/health` 可访问。
  4. 接入 Spring Modulith 与 `ApplicationModules.of(PaideiaApplication.class).verify()` 测试。
  5. 人为在 `paideia-platform` 中反向引用 `paideia-app` 的类型（或删除 pom 中已声明的依赖），确认构建失败，记录输出后回退。
- 验证：`cd backend && mvn -q verify` 通过；人为制造非法跨模块依赖时 `mvn -q verify` 失败（附记录）

### TASK-002 | pending | Web 基础设施：统一响应、全局异常、请求上下文

- 对应：`REQ-004`、`AC-002`
- 依赖：TASK-001
- 修改（计划）：`backend/paideia-web/**`、`backend/paideia-app/pom.xml`
- 测试（计划）：`backend/paideia-web/src/test/java/io/github/xiaou61/web/**`
- 步骤：
  1. 建 `paideia-web` 模块并在 `paideia-app` 中依赖它。
  2. 全局异常处理器：把 `BizException`、参数校验错误、未捕获异常映射为 `ApiResponse` 与对应 HTTP 状态，未捕获异常带 `traceId`。
  3. 请求上下文：入口过滤器生成追踪标识，写入 MDC 与响应体的 `traceId`。
  4. CORS 配置从配置项读取允许来源（骨架期留空并给出配置键名）。
- 验证：`cd backend && mvn -q test`；其中异常映射与响应包装的单测通过

### TASK-003 | pending | 前端 workspace 与主体 SPA 骨架

- 对应：`REQ-004`、`AC-003`
- 依赖：TASK-002
- 修改（计划）：`frontend/package.json`、`frontend/pnpm-workspace.yaml`、`frontend/packages/ui/**`、`frontend/packages/core/**`、`frontend/packages/platform-web/**`、`frontend/apps/app/**`
- 测试（计划）：`frontend/packages/core/src/**/*.test.ts`
- 步骤：
  1. 初始化 pnpm workspace 与根脚本；Vite 配置 `base: './'`。
  2. `packages/core`：定义 `Platform` 端口（本期只需运行环境标识与本地缓存读写）与 API 客户端。
  3. `packages/platform-web`：实现 `Platform` 端口的浏览器版本。
  4. `apps/app`：React Router SPA 骨架，一个页面调用后端接口并渲染真实返回数据（联调目标为 `GET /api/v1/me`，在 TASK-005 之前可先用健康检查端点）。
- 验证：`cd frontend && pnpm install && pnpm -w build` 通过；`pnpm --filter app dev` 启动后页面展示的是接口真实返回数据（附截图或控制台记录）

### TASK-004 | pending | 持久层底座：Flyway、MyBatis 装配与 MySQL 分页组件

- 对应：`REQ-003`、`AC-005`
- 依赖：TASK-001
- 修改（计划）：`backend/paideia-persistence/**`、`backend/paideia-app/src/main/resources/db/migration/**`、`backend/paideia-app/src/test/resources/db/test-migration/**`
- 测试（计划）：`backend/paideia-persistence/src/test/java/io/github/xiaou61/persistence/MySQLPageInterceptorTest.java`、`backend/paideia-persistence/src/test/java/io/github/xiaou61/persistence/RepositoryIntegrationTest.java`
- 步骤：
  1. 建 `paideia-persistence`：MyBatis 装配、`MapperScan` 配置点、审计字段填充、TypeHandler 注册点。
  2. 实现 `MySQLPageInterceptor`：按显式传入的 `PageQuery` 追加 `LIMIT` 并生成 `COUNT` 查询，不使用 `RowBounds`、不引入 `ThreadLocal` 分页状态。
  3. 接入 Flyway，建立迁移目录；写一条迁移创建测试作用域的示例表（含 `owner_id`，仅供 TASK-005 的隔离验证使用），**不进入生产 schema**。
  4. 写一条仓储用例走分页组件。
- 验证：`cd backend && mvn -q test`；分页组件单测与仓储集成测试通过（含总数与页大小断言）

### TASK-005 | pending | 认证与授权：JWT、AuthPort、越权拒绝

- 对应：`REQ-006`、`AC-006`
- 依赖：TASK-002、TASK-004
- 修改（计划）：`backend/paideia-security/**`、`backend/paideia-app/src/main/java/**`
- 测试（计划）：`backend/paideia-security/src/test/java/io/github/xiaou61/security/**`
- 步骤：
  1. 建 `paideia-security`：`AuthPort`（签发、校验、当前主体）、`JwtTokenService`（HS256，密钥来自配置，长度不低于 32 字节）、`CurrentUser` 上下文。
  2. Spring Security 装配为无状态资源服务：关闭 CSRF、不建会话、JWT 过滤器解析令牌。
  3. 暴露 `POST /api/v1/auth/token`（仅开发 profile）与 `GET /api/v1/me`；身份只取自令牌，接口不接受客户端传入的用户标识。
  4. 写授权隔离测试：用测试作用域示例表，断言用户 A 的令牌读不到用户 B 的行；并断言无令牌为 401、越权为 403。
- 验证：`cd backend && mvn -q test`；隔离与 401/403 测试通过

### TASK-006 | pending | Tauri 桌面壳与平台适配器双实现

- 对应：`REQ-007`、`AC-007`
- 依赖：TASK-003
- 修改（计划）：`frontend/packages/platform-desktop/**`、`frontend/apps/desktop/**`、`frontend/package.json`
- 测试（计划）：`frontend/apps/desktop/**` 启动验证脚本
- 步骤：
  1. 建 `packages/platform-desktop`：实现 `Platform` 端口的 Tauri 版本。
  2. 建 `apps/desktop`：Tauri 工程，加载 `apps/app` 构建产物。
  3. 配置 CORS 允许来源，使桌面壳能调用后端（桌面壳来源不是 `http(s)` 域，必须显式加入允许列表）。
  4. 加静态检查：`packages/ui`、`packages/core` 不得引用 `@tauri-apps/*`。
- 验证：`cd frontend && pnpm -w build:desktop` 产出 Windows 安装包；桌面版启动后成功调用后端接口；静态检查通过
- 备注：需要构建机具备 Rust 工具链与 MSVC C++ 生成工具；缺失则在验证报告中记为未运行并说明原因

### TASK-007 | pending | 公开页与 SEO 接入路径验证

- 对应：`REQ-008`、`AC-008`
- 依赖：TASK-003
- 修改（计划）：`frontend/apps/public/**`、`frontend/pnpm-workspace.yaml`
- 步骤：
  1. 建 `apps/public`：一个公开页面，构建产出预渲染的静态 HTML，与 `apps/app` 共享 `ui`/`core`。
  2. 确认该 surface 不进入桌面构建产物。
  3. 在设计文档的对应位置写明后续接入 SSR 的路径（保留说明，不实现）。
- 验证：`cd frontend && pnpm --filter public build` 产出含内容的静态 HTML（非空壳）；`pnpm -w build:desktop` 产物中不含公开页
- 备注：本期只验证路径可行，不实现完整 SEO

### TASK-008 | pending | 验证报告与交付说明

- 对应：`AC-001`..`AC-008`
- 依赖：TASK-001..TASK-007
- 修改（计划）：`.agent/changes/WORK-001-架构选型与项目骨架/testing/plan.md`、`testing/report.md`、`.agent/rules/always.md`（补齐实际构建与检查命令）、`.agent/INDEX.md`（登记模块与路径）
- 步骤：
  1. 逐条 AC 记录实际执行的命令与结果；未运行的项如实写为未运行并说明原因。
  2. 把真实的构建与检查命令写进项目常驻规范，替换当前的占位说明。
  3. 在 `.agent/INDEX.md` 登记前端与后端模块的位置与用途。
- 验证：`project-lifecycle.ps1 validate` 通过；报告中的每条命令可复现

## 完成条件

- TASK-001..TASK-008 全部为 `done`。
- AC-001..AC-008 均有可复现的验证记录；未运行的检查明确标注。
- 项目的构建与检查命令已写入 `.agent/rules/always.md`。
- 未引入任何业务模块或业务功能；`events`、`ai`、`storage` 仍未创建。
