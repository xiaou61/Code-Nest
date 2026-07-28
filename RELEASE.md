# 发布流程

## v2.5.0

`v2.5.0` 将 24x7 告警、事故聚合、证据留存、只读 RCA、人工反馈和离线质量评测收敛为一条可恢复、可审计的 SRE 工作流。它不会执行自动修复，也不代表已经完全对齐 OpenSRE。

### Highlights

- 部署 Prometheus、Alertmanager、Blackbox Exporter 与 Grafana 监控栈，通过独立 token 保护的私网 Webhook 接收告警。
- 使用事故、告警关系、证据和事务 Outbox 持久化告警处理；Worker 支持原子领取、重试和失败记录。
- 管理端 `/sre/incidents` 提供事故摘要、状态操作、时间线、RCA 历史、阶段轨迹、回放来源和反馈修订。
- RCA 只读取服务端固定的 Prometheus/Loki 查询，最多调查 5 轮；每条新证据先持久化，再生成一次最终结构化报告。
- 不可变评测用例和版本化套件通过持久化队列异步运行，具备心跳、租约恢复、指数退避、deadline、质量门禁和构建溯源。
- SRE 自身指标覆盖积压、最老任务年龄、运行耗时、调查轮数、重试、租约恢复、deadline 与终态失败。

### Scope And Safety Boundary

- 模型只能选择后端注册的固定 tool key；后端再次执行白名单、去重和最多 5 轮截断。
- 不接受模型生成的 PromQL、LogQL、Shell 或 SQL，不执行自动修复或其他系统写操作。
- 没有金额、Token 或模型成本预算闸门；资源边界由客户端超时、响应/上下文上限、轮数上限和查询指纹幂等控制。
- Prometheus/Loki 不可用时记录受控的 unavailable 证据，不阻塞 Alertmanager 原有 QQ 告警链路。

### Database Migration

新环境：

- 直接使用 `sql/MySql/code_nest.sql`，其中已经包含最终结构的 16 张 SRE 表；权限种子使用最新 `sql/MySql/code_nest_data.sql`。
- 不执行面向早期 v2.5.0 预览环境的增量 `ALTER TABLE` 脚本。

已部署且尚无 SRE 表的环境：

1. 保持 `XIAOU_SRE_OUTBOX_ENABLED=false`、`XIAOU_SRE_METRICS_ENABLED=false` 和 `XIAOU_SRE_EVALUATION_ENABLED=false`。
2. 执行 `sql/v2.5.0/sre_incident.sql`。
3. 执行 `sql/v2.5.0/sre_investigation_run.sql`，创建运行、轨迹、反馈和回放来源表。
4. 依次执行 `sql/v2.5.0/sre_rca_evaluation.sql`、`sre_rca_evaluation_suite.sql`、`sre_rca_evaluation_queue.sql`。
5. 执行 `sql/v2.5.0/sre_agent_permissions.sql`，为 `SUPER_ADMIN` 增加只读 Agent 调查权限。

从早期 v2.5.0 预览表结构升级：

1. 先执行当前 `sre_incident_evidence.sql` 和 `sre_investigation_run.sql`，补齐缺失表；二者使用 `CREATE TABLE IF NOT EXISTS`。
2. 停止 RCA 流量，执行一次 `sre_investigation_loop.sql`，为旧证据表增加调查运行 ID 与查询指纹。最终主 schema 或已包含这些列的环境不能重复执行。
3. 在评测 Worker 关闭时依次执行 evaluation、suite、queue 三个脚本。suite 与 queue 含不可重复的 `ALTER TABLE`，必须按顺序各执行一次；queue 会将没有可信租约的旧 `RUNNING` 记录标记为失败。
4. 执行 `sre_agent_permissions.sql` 后再恢复流量。

### Configuration Enablement

1. 先验证 Prometheus/Alertmanager 配置和目标可达性；Prometheus API 与后续 Loki API 只允许监控网络访问。
2. 创建独立 `XIAOU_SRE_WEBHOOK_TOKEN`，让 Alertmanager 私网直连 `/api/internal/sre/alertmanager/v1/alerts`，再开启 `XIAOU_SRE_WEBHOOK_ENABLED=true`。该路径不能发布到公网 Nginx。
3. 证据表验证完成后开启 `XIAOU_SRE_OUTBOX_ENABLED=true`；只在对应 API 和保留策略验证后开启 `XIAOU_SRE_PROMETHEUS_ENABLED` / `XIAOU_SRE_LOKI_ENABLED`。
4. 所有事故、调查和评测表存在后开启 `XIAOU_SRE_METRICS_ENABLED=true`。
5. 注入不可变的 `XIAOU_SRE_EVALUATION_SOURCE_REVISION`、`XIAOU_SRE_EVALUATION_BUILD_ID` 和 `XIAOU_SRE_EVALUATION_BUILD_VERSION=2.5.0`，最后开启 `XIAOU_SRE_EVALUATION_ENABLED=true`。未开启时创建评测运行返回业务码 `503`，不会遗留无人消费的任务。

### Verification

- `mvn -pl xiaou-sre,xiaou-system -am test`：`xiaou-ai` 93 个、`xiaou-system` 298 个测试通过（3 个 opt-in 用例跳过），`xiaou-sre` 测试通过。
- `scripts/code-nest-eval.ps1 -Tier sre`：固定脱敏样本的 SRE 回归门禁通过，不读取生产数据或调用在线模型。
- 管理端 30 个契约测试、ESLint 与生产构建通过；文档站 129 页面审计和 VitePress 构建通过。
- `scripts/code-nest-eval.ps1 -Tier release` 已通过：30 模块后端打包、双前端生产构建、129 页文档审计/VitePress 构建和部署脚本语法检查成功，产出 `xiaou-application-v2.5.0.jar`。
- `scripts/code-nest-eval.ps1 -Tier hygiene` 已通过：空白、敏感信息占位符和文本 NUL 字节检查无阻断项。

### Risks And Rollback

- Webhook、Outbox、数据库 gauge、Prometheus/Loki 采集和评测 Worker 均有独立开关；出现异常时先关闭对应开关，保留原 QQ 告警链路。
- 数据库变更以新增表、列和索引为主。应用回滚时保留 SRE 表和队列记录，不执行破坏性降级；恢复旧应用构建物后再评估离线清理。
- suite/queue 和早期调查轮次迁移不是可重复脚本，生产执行前必须备份数据库并记录已执行版本。
- 评测队列异常时关闭 Worker，保留 `QUEUED/RUNNING` 记录和构建溯源，修复后通过租约恢复继续处理。
- 合并发布 PR 后再创建并推送 `v2.5.0` tag；tag 会触发生产部署，不能在 PR 合并前创建。

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
