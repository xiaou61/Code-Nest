# 发布流程

## v2.5.10

`v2.5.10` 完成管理员持久任务的产品工作台、工作流事件控制面和租约可靠性加固。该版本保留原有单工具聊天契约，并把长耗时任务的监督、人工干预和恢复边界纳入同一套后端状态机。

### Highlights

- 管理端全屏智能体新增“对话 / 持久任务”模式，切换模式不会丢失已有会话。
- 任务工作台支持创建、状态筛选、列表/详情刷新、生命周期进度、持久步骤、trace/audit 信息和终态原因。
- 游标事件时间线只在活跃的非终态任务上增量轮询，事件有界、排序稳定且不会重复展示。
- `WAITING_INPUT`、`PAUSED`、恢复和取消操作均按任务所有者隔离，并在同一事务中记录不可变事件。
- 每次领取使用唯一 lease token；规划和工具执行期间续约，旧 lease 的迟到结果无法推进任务。
- 陈旧恢复以扫描 heartbeat cutoff 原子 fencing，并按任务独立事务执行，单个冲突或异常不会回滚同批成功恢复。
- 写风险步骤继续复用实时权限、policy、预览、审计与完全匹配的强确认，不在浏览器执行或推断破坏性动作。

### Verification

```bash
python scripts/release_manifest.py validate
python scripts/test_release_manifest.py -v
openspec validate --all --strict
mvn -pl xiaou-system -am test
npm --prefix vue3-admin-front run test:contracts
npm --prefix vue3-admin-front run build
npm --prefix vue3-user-front run test:contracts
npm --prefix vue3-user-front run build
npm --prefix docs-site run build
```

### Migration And Enablement

1. 备份生产数据库，并在所有实例保持 `XIAOU_ADMIN_AGENT_TASK_ENABLED=false`。
2. 从远端 `v2.5.8` 升级时，先执行 `sql/v2.5.9/admin_agent_tasks.sql`，再执行 `sql/v2.5.10/admin_agent_workflow_control_plane.sql`；推荐先运行 `python scripts/db-migrate.py --dry-run`，确认后再 `--apply`。
3. 验证 `sys_agent_task`、`sys_agent_task_step`、`sys_agent_task_event` 以及 `workflow_context_json` 字段存在，确认原有 `/admin/agent/chat` 正常。
4. 先以 Worker 关闭状态部署应用，观察任务 API、事件写入和 Micrometer 指标；随后仅在一个实例设置 `XIAOU_ADMIN_AGENT_TASK_ENABLED=true`。
5. 验证 lease renewal、recovery conflict、任务失败、确认等待和人工复核指标稳定后，再逐实例灰度开启。

生产 AI Base URL、API Key、模型与推理等级继续只由服务器环境文件提供。

### Rollback

- 首先在全部实例关闭任务 Worker 并重启，阻止领取新任务；保留任务、步骤、事件和审计记录供人工复核。
- 回滚应用制品时不删除 v2.5.9/v2.5.10 新增表或字段，旧版本不会主动消费这些记录。
- `REQUIRES_REVIEW`、已确认但终态不明的写步骤和 lease ownership 冲突必须人工核对，禁止通过重新排队重放。

## v2.5.9

`v2.5.9` 将管理端统一智能体升级为全屏工作台，并增加持久化、顺序执行、可确认/取消/恢复的管理员任务运行时。原有单工具聊天入口保持兼容。

### Highlights

- 全屏三栏布局：会话列表、主对话区、执行检查器在同一个界面中协同。
- 会话有界持久化：最多 20 个会话、每个会话 100 条消息，支持搜索、重试、复制和 Markdown 导出。
- 运维结果结构化：数组产物自动渲染为表格，计划、差异、trace、风险与审计信息分页展示。
- 统一后端边界不变：前端仍只调用 `POST /admin/agent/chat`，不实现 planner、policy 或 audit 分支。
- 新增 `/admin/agent/tasks` 创建、列表、详情、确认和取消接口；所有读取和状态变更按当前管理员 ID 隔离。
- MySQL 是任务与步骤状态的唯一事实源；Worker 每次只推进一个图周期，默认最多 5 个顺序步骤且不并行执行。
- 写风险步骤继续复用现有预览、强确认和审计链路；确认后结果不明的写操作只进入 `REQUIRES_REVIEW`，不会自动重放。
- Micrometer 新增 `xiaou.agent.task.*` 队列深度、任务/步骤结果、端到端耗时、确认等待、取消、租约恢复和 Worker 指标。

### Verification

```bash
python scripts/release_manifest.py validate
openspec validate add-durable-admin-agent-tasks --strict
mvn -pl xiaou-system -am test
node --test vue3-admin-front/tests/admin-agent-workspace-state.test.js vue3-admin-front/tests/admin-agent-chat-ui.test.js
npm --prefix vue3-admin-front run test:contracts
npm --prefix vue3-admin-front run build
```

### Migration And Enablement

1. 备份生产数据库，保持 `XIAOU_ADMIN_AGENT_TASK_ENABLED=false`。
2. 先执行 `python scripts/db-migrate.py --dry-run`，再按发布流程执行 `python scripts/db-migrate.py --apply`；本版本新增 `sql/v2.5.9/admin_agent_tasks.sql`。
3. 部署应用但暂不启用 Worker，验证 `sys_agent_task`、`sys_agent_task_step` 可读写，原有 `/admin/agent/chat` 行为不变。
4. 通过 Prometheus 检查 `xiaou_agent_task_queue_depth`、`xiaou_agent_task_worker_runs_total` 和 `xiaou_agent_task_stale_recoveries_total`，随后仅在一个实例设置 `XIAOU_ADMIN_AGENT_TASK_ENABLED=true`。
5. 确认队列、确认等待、失败和人工复核指标稳定后，再逐实例开启；租约默认 300 秒，应大于 AI 与工具调用超时。

生产 AI Base URL、API Key、模型与推理等级继续只由服务器环境文件提供。

### Rollback

- 先在所有实例设置 `XIAOU_ADMIN_AGENT_TASK_ENABLED=false` 并重启，停止领取新任务；已落库任务和审计记录保留供复核。
- 回滚应用制品时不删除两张任务表；旧版不读取它们。需要恢复时重新部署本版后继续处理安全可恢复的只读任务。
- `REQUIRES_REVIEW` 或确认后终态不明的写步骤必须人工核对审计与外部系统，禁止通过重新排队来重放。

## v2.5.6

`v2.5.6` 是纯架构治理版本，不增加产品功能，也不修改数据库结构。目标是让启动、基础设施、领域能力、跨模块读取、前后端契约和发布元数据各自只有一个清晰所有者。

### Highlights

- `xiaou-bootstrap` 成为唯一启动壳和可执行 JAR；`xiaou-application` 只负责跨领域应用编排。
- `xiaou-common` 的实现按 core、web、security、cache、persistence 拆分，旧模块作为兼容聚合逐步退出依赖图。
- Growth Coach、首页和学习驾驶舱通过端口与适配器读取领域数据；聚合调用统一走 `xiaou-resilience`。
- SRE RCA/评测完整归位 `xiaou-sre`，通知持久化与发布能力完整归位 `xiaou-notification`。
- 双前端共享 `@code-nest/api-contract`，后端统一业务码到 HTTP 状态映射，通知 DTO 不再暴露持久化实体。
- 用户端六个、管理端六个路由切片替代超大入口路由文件，路径和路由名保持兼容。
- `release/manifest.json` 统一 `v2.5.6`、schema `v2.5.3`、Docker 标签和制品路径，CI/CD 不再从分支名猜测发布版本。

### Verification

```bash
python scripts/release_manifest.py validate
python scripts/test_release_manifest.py -v
python scripts/check-architecture.py
mvn -B -pl xiaou-bootstrap -am test
npm --prefix code-nest-api-contract test
npm --prefix vue3-user-front run test:contracts
npm --prefix vue3-admin-front run test:contracts
npm --prefix vue3-user-front run build
npm --prefix vue3-admin-front run build
npm --prefix docs-site run build
```

### Migration

- 无数据库迁移；`RELEASE.schema_version` 固定记录最新真实数据库基线 `v2.5.3`。
- 部署仍默认不写数据库，`CODE_NEST_RUN_MIGRATIONS` 的行为不变。
- 运维脚本和 systemd 继续使用 `/opt/code-nest/app/app.jar`，只改变仓库内构建制品来源。

## v2.5.5

`v2.5.5` 是成长证据到能力结构的纵向切片版本：在既有 Growth Coach 证据链上提供可解释、可回溯的用户侧能力图谱。

### Highlights

- 新增 `GET /user/growth-coach/capability-graph`，按用户已落库证据计算能力节点、分数、可信度、趋势、证据引用和关系边。
- 新增用户端 `/growth-capabilities` 页面，并从成长自动驾驶和学习导航提供入口。
- 能力图谱复用 `GrowthEvidenceQueryService` 与 `GrowthSkillInsightService`，只读计算，不自动改计划、不新增数据库表、不把模型建议写成事实。
- 图谱与短板洞察复用同一份 30 条有界证据快照，减少单次请求的重复投影和查询。

### Verification

- `python scripts/check-version-consistency.py`
- 用户端与管理端契约测试、用户端生产构建、文档站构建
- `mvn -B -pl xiaou-application -am test`（含能力图谱单元测试）

### Migration

- 无数据库迁移、无新增环境变量；现有成长证据和短板洞察可直接使用。

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
- `python scripts/release_manifest.py validate` 已确认清单与所有版本投影一致。

### 后端验证

至少执行：

```bash
mvn -pl xiaou-bootstrap -am -DskipTests compile
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
