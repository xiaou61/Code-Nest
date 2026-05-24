# 贡献指南

感谢你愿意参与 Code Nest 的建设。本项目是一个多模块、前后端分离的开发者成长平台，贡献时请优先保证变更边界清晰、验证充分、文档同步，避免一次提交混入无关改动。

## 参与方式

- 提交 Bug：请使用 GitHub Issue 中的 Bug Report 模板，并附上复现步骤、期望结果、实际结果和环境信息。
- 提交需求：请使用 Feature Request 模板，说明用户场景、业务价值、验收标准和兼容性影响。
- 修复或开发：请先从 Issue 或明确需求开始，确保改动范围可 review、可验证、可回滚。
- 文档改进：请说明文档读者、更新位置和是否影响 README、docs-site 或发布说明。

## 开发环境

建议环境：

- JDK 17
- Maven 3.8+
- Node.js 18+
- MySQL 8+
- Redis 6+

常用命令：

```bash
# 后端编译
mvn -pl xiaou-application -am -DskipTests compile

# 单模块编译示例
mvn -pl xiaou-team -am -DskipTests compile

# 用户端
cd vue3-user-front
npm install
npm run build

# 管理端
cd vue3-admin-front
npm install
npm run build
```

如需启动完整环境，请结合 `README.md`、`docs-site/guide/local-dev.md` 和数据库脚本 `sql/` 使用。

## 分支规范

推荐分支命名：

- `feature/<scope>-<summary>`：新增功能。
- `fix/<scope>-<summary>`：缺陷修复。
- `refactor/<scope>-<summary>`：重构，不改变外部行为。
- `docs/<scope>-<summary>`：文档变更。
- `release/vX.Y.Z`：版本发布准备。
- `hotfix/vX.Y.Z-<summary>`：线上紧急修复。

示例：

```text
feature/team-checkin-calendar
fix/chat-rate-limit-error-state
docs/release-verification-guide
```

## 提交信息规范

提交信息建议采用 Conventional Commits 风格：

```text
<type>(<scope>): <summary>
```

常用类型：

- `feat`：新增功能。
- `fix`：修复缺陷。
- `refactor`：重构。
- `perf`：性能优化。
- `docs`：文档更新。
- `test`：测试相关。
- `build`：构建、依赖或工程配置。
- `ci`：持续集成。
- `chore`：非业务杂项。

示例：

```text
fix(team): align checkin payload with backend contract
docs(release): add v2.2.1 verification checklist
build(frontend): split vendor chunks for user app
```

## 代码规范

通用要求：

- 单个 PR 聚焦一个目标，避免混入无关格式化、生成文件或历史改动。
- 不提交本地密钥、Token、数据库密码、真实用户数据和临时调试文件。
- 不提交 `target/`、`dist/`、日志文件、IDE 缓存和非必要生成产物。
- 修改公共 DTO、Mapper、SQL、API 路由时必须同步检查前后端调用方。
- 涉及数据库表结构或字段语义变化时，必须同步更新 SQL 脚本和相关文档。

后端要求：

- Controller 保持轻量，业务逻辑放入 Service。
- 统一使用全局异常处理，不在 Controller 中重复吞异常。
- 请求 DTO 应尽量补齐 `jakarta.validation` 校验。
- Mapper XML 避免生产查询中的无边界 `select *`。
- 新增跨模块调用时优先依赖 API 模块或明确的服务接口，避免循环依赖。

前端要求：

- 页面状态、接口参数、后端 DTO 字段保持一致。
- 用户可见失败态必须有明确提示，避免静默失败。
- 新增页面或路由时同步检查菜单、权限、路由文档和构建结果。
- 避免无意义的大范围格式化，减少 review 噪声。

## 文档同步要求

以下改动需要同步文档：

- 新增或修改用户端页面：更新 `docs-site/reference/frontend-routes.md` 及对应模块文档。
- 新增或修改管理端页面：更新路由、权限和管理端说明。
- 新增 Controller、接口前缀或响应结构：更新 `docs-site/reference/api-routes.md`。
- 新增表、字段、索引或数据迁移：更新 `docs-site/reference/database-tables.md` 和 SQL 脚本。
- 新增 WebSocket 事件：更新 `docs-site/reference/websocket.md`。
- 新增错误码或异常语义：更新 `docs-site/reference/response-errors.md`。
- 新增 AI Prompt、RAG、Schema 或回归样例：更新 `docs-site/reference/ai-schemas.md` 和 AI 模块文档。

## Pull Request 流程

提交 PR 前请确认：

- 已描述变更背景、影响范围和风险。
- 已列出执行过的验证命令和结果。
- 已说明未验证项及原因。
- 已同步必要文档、SQL、接口说明或截图。
- 已确认 PR 中不包含无关改动。

维护者 review 时重点关注：

- 行为是否符合需求和兼容性预期。
- 接口契约、数据库字段、前后端字段名是否一致。
- 是否存在权限绕过、越权访问、敏感信息泄露或 XSS/注入风险。
- 是否具备必要的回滚方案。

## 安全与隐私

请不要通过公开 Issue 披露安全漏洞。安全问题请按 `SECURITY.md` 中的方式私下报告。

任何贡献都不应包含：

- 真实用户隐私数据。
- 生产环境密钥。
- 内网地址、账号、密码、Cookie、Token。
- 未脱敏的日志、截图或数据库导出。

## 版本发布

版本发布请参考 `RELEASE.md`。发布前至少完成后端编译、前端构建、数据库脚本检查和核心功能冒烟验证。
