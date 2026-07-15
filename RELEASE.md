# 发布流程

## v2.4.1

`v2.4.1` 是架构与文档治理补丁版本，重点收敛共享前端基础设施、缓存与线程池边界，优化热点业务链路，并把现行 Markdown 与 VitePress 文档纳入可持续审计。

### Highlights

- 双前端统一复用 `code-nest-design-system`，移除长期漂移的重复组件、主题和设计令牌。
- 使用 `RedisValueStore`、Spring 托管执行器和模块专用 Redisson 能力替代静态缓存与并发工具。
- 学习驾驶舱、社区热点与摘要、朋友圈、摸鱼工具、抽奖和验证码等热点链路减少重复查询与缓存往返。
- 为学习驾驶舱、WebSocket 票据、Redis 值存储和朋友圈核心服务补充聚焦回归测试。
- 129 个 VitePress 页面完成分类重组，新增响应式主题、暗色模式、动态同步基线和导航完整性审计。

### Migration

- 本版本不新增数据库脚本，不改变对外 API，也不新增生产环境变量。
- 双前端通过仓库内 `file:../code-nest-design-system` 解析共享包，部署流程继续执行各前端的 `npm ci` 与生产构建即可。

### Verification

- 本地 `scripts/code-nest-eval.ps1 -Tier release` 已通过：生成 `xiaou-application-v2.4.1.jar`，双前端与文档站构建成功，发布脚本语法检查通过。
- 本地 `scripts/code-nest-eval.ps1 -Tier hygiene` 已通过：空白、密钥占位符和文本 NUL 字节检查无阻断项。
- GitHub PR 必须通过后端测试与打包、双前端契约测试与构建、文档构建、RAG 测试和仓库卫生检查。
- VitePress 已覆盖桌面、390px 移动端、暗色模式、搜索、长表格和键盘焦点浏览器验收。

### Risks And Rollback

- 主要风险是共享设计系统本地包解析、缓存 key 行为和异步执行时序；发布门禁覆盖双前端构建及相关后端测试。
- 本版本无数据库回滚动作；如需回退，使用既有发布备份恢复 `v2.4.0` 后端 Jar 和双前端静态资源。

## v2.4.0

`v2.4.0` 发布管理员端统一后端 Agent 运行时。本版本的范围是单一聊天入口和当前 26 个已注册生产工具，不表示尚未注册的所有业务接口都已支持自然语言操作。

### Highlights

- `POST /admin/agent/chat` 统一承载自然语言请求、上下文续接和高风险动作确认。
- 后端通过 `LLM Planner -> AgentToolRegistry -> AgentPolicyEngine -> AgentTool -> Audit` 完成规划和执行，前端不维护动作目录或业务分组。
- 新工具通过新增带 definition/schema 的 `AgentTool` Bean 扩展，不需要修改前端编排逻辑。
- 写入和破坏性工具统一执行 `PREVIEW -> CONFIRMED -> EXECUTED`，并具备权限、审计、幂等与失败记录。
- 会话上下文按管理员 ID 隔离，HTTP 请求长度在进入 LLM 和编排器前完成校验。

### Migration

已部署数据库按顺序执行：

```text
sql/v2.4.0/admin_agent_audit.sql
sql/v2.4.0/admin_agent_audit_idempotency.sql
sql/v2.4.0/admin_agent_session_context.sql
sql/v2.4.0/admin_agent_permissions.sql
```

生产环境继续通过 `XIAOU_AI_BASE_URL`、`XIAOU_AI_API_KEY` 和 `XIAOU_AI_CHAT_MODEL` 注入模型配置；可通过 `XIAOU_AI_MAX_COMPLETION_TOKENS` 调整全局 completion 上限。密钥不得写入仓库配置。

### Verification

- `scripts/code-nest-eval.ps1 -Tier release`：后端 package、双前端 build、文档站 build 与脚本语法检查。
- 后端离线统一回归：215 个 Agent 测试通过，3 个 opt-in live 用例默认跳过。
- 真实 `gpt-5.5` 验收：当前 26/26 个注册工具通过；两个写工具完成隔离 MySQL、强确认、业务回查和审计终态验证。
- 完整证据见 `AI-DOCS/Technical/13-Code-Nest-full-site-test-results.md`。

### Risks And Rollback

- 外部 OpenAI 兼容网关可能出现超时或 `502/524`；运行时保留传输重试和 deterministic fallback，live 验收则关闭 fallback 以避免伪成功。
- 回滚应用前先停止 Agent 写入流量；数据库新增表和权限种子均向后兼容，可保留，不需要破坏性回滚。
- 如需撤销 Agent 能力，回滚到上一版本应用和双前端构建产物，并按既有发布脚本恢复备份。

## v2.3.1

- 新增统一 CI 工作流，覆盖后端、双前端、文档站和脚本语法检查。
- 新增生产部署 GitHub Actions，支持版本分支/tag 推送或手动触发后构建 release bundle 并同步服务器。
- 新增服务器端发布脚本，内置备份、替换、健康检查和失败回滚。
- 新增生产 CI/CD 文档，说明 Secrets、目录、触发方式和回滚命令。

本文档定义 Code Nest 的版本发布流程，适用于正式版本、补丁版本和紧急修复版本。

## 版本号

版本号格式：

```text
vMAJOR.MINOR.PATCH
```

- `MAJOR`：架构级重构、不兼容变更或版本主题升级。
- `MINOR`：新增功能、模块增强或较大范围体验优化。
- `PATCH`：缺陷修复、安全修复、工程治理或兼容性优化。

示例：

```text
v2.2.1
v2.3.0
v3.0.0
```

## 发布分支

推荐分支：

- `release/vX.Y.Z`：常规发布准备分支。
- `hotfix/vX.Y.Z-summary`：紧急修复分支。
- `vX.Y.Z`：如需要保留版本分支，可在发布完成后推送到远端。

## 发布前检查

### 代码状态

- 工作区不包含无关改动。
- PR 已完成 review。
- 数据库脚本、接口文档、前端路由文档已同步。
- 版本号已在 Maven、前端 package、README 或部署脚本中保持一致。

### 后端验证

至少执行：

```bash
mvn -pl xiaou-application -am -DskipTests compile
```

如发布只涉及单模块，可额外执行单模块编译：

```bash
mvn -pl xiaou-team -am -DskipTests compile
```

涉及高风险逻辑时，应补充单元测试、集成测试或接口冒烟验证。

### 前端验证

用户端：

```bash
cd vue3-user-front
npm run build
```

管理端：

```bash
cd vue3-admin-front
npm run build
```

如改动涉及页面交互，应补充浏览器冒烟验证或截图说明。

### 数据库验证

- 新增表、字段、索引、约束时，确认 SQL 可重复评审。
- 避免破坏旧数据。
- 明确是否需要迁移脚本、默认值或灰度步骤。
- 更新 `sql/` 和 `docs-site/reference/database-tables.md`。

### 安全验证

涉及登录、权限、文件、WebSocket、富文本、AI 外部调用、积分和后台高权限操作时，必须说明：

- 权限边界。
- 输入校验。
- 失败态。
- 回滚方案。
- 是否涉及敏感信息。

## 发布步骤

1. 从目标基线创建发布分支。
2. 完成代码、文档、SQL 和版本号调整。
3. 执行必要的构建、测试和冒烟验证。
4. 更新 `CHANGELOG.md`。
5. 提交 Pull Request，并使用 PR 模板填写验证结果。
6. 合并后创建 Git tag。
7. 推送 tag 和发布分支。
8. 如使用 GitHub Release，复制 `CHANGELOG.md` 中对应版本内容作为 Release Notes。

## Git 命令示例

```bash
git checkout -b release/v2.2.1
git status
git add <changed-files>
git commit -m "chore(release): prepare v2.2.1"
git push -u origin release/v2.2.1
```

创建标签：

```bash
git tag -a v2.2.1 -m "Code Nest v2.2.1"
git push origin v2.2.1
```

## 回滚策略

发布前必须明确：

- 是否可以直接回滚代码。
- 数据库变更是否向后兼容。
- 是否需要保留旧配置。
- 是否需要关闭某个功能开关。
- 回滚后前端资源、缓存、定时任务和消息队列如何处理。

## 发布说明模板

```markdown
## Summary

- N/A

## Highlights

- N/A

## Fixes

- N/A

## Migration

- Database:
- Config:
- Data:

## Verification

- Backend:
- User frontend:
- Admin frontend:
- Smoke test:

## Risks

- N/A

## Rollback

- N/A
```
