# 发布流程

## v2.5.4

`v2.5.4` 是 master 整合与发布治理补丁版本：合并 v2.5.1 生产 SRE 治理能力，并保留 v2.5.3 的成长闭环、数据库迁移和发布安全边界。

### Highlights

- SRE 评测队列、只读 RCA、容量治理、外部探针、Dashboard 和运维资产统一进入主线。
- 告警接收、Incident、Evidence、Outbox worker 和 RCA 评测共享低基数指标；Outbox 继续暴露积压、处理中、耗时和租约恢复指标。
- 发布包同时包含完整 `sql/`、`VERSION`、迁移执行器、运维资产和发布烟测；数据库迁移仍需显式设置 `CODE_NEST_RUN_MIGRATIONS=true`。

### Verification

- 版本一致性与迁移清单 dry-run。
- Maven 后端构建与定向测试、双前端契约测试、文档构建、SRE contract、发布脚本 syntax/smoke 验证。

### Migration

- v2.5.3 已应用的生产 ledger 可继续复用；master 新增的 SRE 评测/运维 SQL 由同一迁移执行器按 checksum 顺序补齐。
- 生产执行前必须先备份数据库；失败迁移保留 `FAILED` 记录，只有人工核验后才允许 `--retry-failed`。

## v2.5.3

`v2.5.3` 是 Growth Coach 与生产交付治理版本：在 v2.5.0/v2.5.2 的成长闭环能力上，补齐版本、迁移、发布、SSRF 和 AI 运行时的生产边界。

### Highlights

- 统一 `VERSION` 基线，构建前检查 Maven、双前端、设计系统、文档站和 lockfile 版本一致性。
- 发布包包含 `VERSION`、`RELEASE`、完整 `sql/`、`scripts/db-migrate.py` 和服务器部署脚本，可追溯、可复核。
- 数据库迁移使用 checksum ledger，支持 dry-run、baseline、apply；生产默认不自动执行迁移。
- Growth Coach 使用有界并发、来源状态/耗时指标和短缓存；成长事件带 schema/client/entry 元数据与 allow-list。
- SRE Outbox 暴露 `xiaou_sre_outbox_pending`、处理中数量、事件耗时、结果计数和租约恢复计数，便于值守告警。
- 远程敏感词来源拒绝内网和元数据地址、重定向及超大响应；AI 调用受并发 permit 和等待超时保护。

### Migration

发布前先在备份和测试环境执行：

```bash
python scripts/db-migrate.py --dry-run
python scripts/db-migrate.py --apply
```

对于已经手工完成历史迁移、但没有 ledger 的数据库：

```bash
python scripts/db-migrate.py --baseline --baseline-to v2.5.2
python scripts/db-migrate.py --apply
```

服务器端部署只有在显式设置 `CODE_NEST_RUN_MIGRATIONS=true` 时才会调用迁移器。迁移器使用 MySQL advisory lock 串行化执行，失败记录会保留并阻断后续发布；运维核对部分 DDL 状态后，才允许显式设置 `CODE_NEST_RETRY_FAILED=true`（等价于 `--retry-failed`）重试。应用发布失败会恢复本次发布前的 Jar 和双端静态资源。数据库迁移不做破坏性回滚，需按数据库备份和新增回滚脚本处理。

### Verification

- `python scripts/check-version-consistency.py`
- `python scripts/db-migrate.py --dry-run`
- `python -m py_compile scripts/check-version-consistency.py scripts/db-migrate.py scripts/deploy-production.py scripts/release-smoke-test.py`
- `bash -n scripts/ci-server-build-deploy.sh scripts/ci-server-build-deploy.test.sh scripts/deploy-release.sh`
- 后端定向测试、双前端契约测试、用户端/管理端/文档站构建

### Risks And Rollback

- 生产发布必须从干净工作树构建；本地诊断才允许使用 `--allow-dirty` 或 `CODE_NEST_ALLOW_DIRTY_BUILD=true`。
- 服务器端会在解包前再次检查 tar 路径、版本元数据、脚本和归档成员类型，发布包不能只依赖本地 smoke test。
- 发布前确认 `/opt/code-nest` 有足够磁盘空间；备份目录按数量和容量自动清理。
- 健康检查失败会自动恢复应用产物；迁移执行失败会停止发布并保留 checksum/失败原因，禁止继续覆盖应用。

## v2.4.3

`v2.4.3` 在 `v2.4.2` 的移动端与行动优先修复基础上，补齐首次登录到首周任务的闭环，并把首页浏览器侧的多请求收敛为可部分降级的聚合接口。

### Highlights

- 首次登录收集目标岗位、学习阶段和每周投入时间，立即生成本周自动驾驶任务。
- 新增 `/user/home/overview`，后端并行汇总首页所需数据，单个来源失败只影响对应区块。
- 求职闭环默认展示自动同步状态，恢复同步收进异常恢复菜单。
- 首页先呈现“今天的行动”，版本信息降为次级内容；Element Plus 改为按需组件解析。

### Migration

- 已部署数据库执行 `sql/v2.4.3/growth_autopilot_onboarding.sql`。
- 不新增环境变量；既有页面和 API 保持兼容。

### Verification

- 用户端契约测试与生产构建。
- `UserHomeOverviewServiceTest`，覆盖首页部分数据源不可用时的分区降级。
- 后端多模块测试编译与聚合服务测试。

### Risks And Rollback

- 数据库迁移只新增带默认值的列；回滚应用前可保留该列，不影响旧版计划读取。
- 首页聚合服务的单点超时只会降低单个页面区块，无法使用时可临时切回既有模块接口。

## v2.4.2

`v2.4.2` 是用户端 UX 修复补丁版本，聚焦移动端导航与认证首屏、用户目标导向的信息架构，以及求职和面试工作台的行动优先顺序。

### Highlights

- 移动端导航抽屉覆盖完整视口并支持滚动；窄屏登录/注册页优先展示表单。
- 主导航收敛为“今天、学习、求职、社区”，次级入口按“练习与工具、创作、更多”组织。
- 求职闭环先展示下一步行动，面试题库先展示可进入的题单列表。
- 首页聚合请求支持静默失败，避免服务故障时重复弹出全局提示，同时保留登录过期处理。

### Migration

- 无数据库迁移、环境变量变更或对外 API 兼容性变更。

### Verification

- 用户端契约测试、生产构建、文档审计与桌面/移动端浏览器冒烟验证。

### Risks And Rollback

- 主要风险集中在导航响应式断点与 Element Plus 抽屉的 Teleport 行为；回滚时恢复用户端和共享设计系统构建产物即可。

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
