---
artifact: project_index
status: active
---

# 项目总索引

本页只负责按模块导航，帮助定位“代码改在哪里、依据和验证到哪里看”。工作项阶段、批准、任务进度和验证结果仍以对应工件及 `project-lifecycle.ps1 status` 输出为准，不在这里重复维护。状态、恢复和严格校验脚本属于已安装 Skill 的内部实现，不在目标项目 `.agent/scripts/` 中查找。

## 内容在哪里

| 内容 | 位置 |
| --- | --- |
| 项目常驻规范 | `.agent/rules/always.md` |
| 当前稳定规格 | `.agent/specs/` |
| 需求与变更依据 | `.agent/changes/WORK-编号-中文名/` |
| 决策说明与共享资料 | `.agent/notes/`、`.agent/references/` |
| 长期记忆 | `.agent/memory.md` |
| 代码审查报告 | `.agent/reviews/` |
| 项目理解型 HTML | `.agent/html/` |
| 更新历史与 Git 历史视图 | `.agent/history/updates.md`、`.agent/history/core-components.md` |

## 模块索引

只登记已经从仓库确认的模块和路径；发现新模块或路径发生实质变化时更新对应行。

| 模块 | 源码或配置 | 测试 | 稳定规格 | 相关工作项 |
| --- | --- | --- | --- | --- |

| 后端聚合工程 | `backend/pom.xml` | — | — | WORK-001 |
| 通用契约 `paideia-platform` | `backend/paideia-platform/` | `src/test/java/io/github/xiaou61/platform/PlatformContractTest.java` | — | WORK-001 |
| Web 基础设施 `paideia-web` | `backend/paideia-web/` | `src/test/java/io/github/xiaou61/web/` | — | WORK-001 |
| 数据访问 `paideia-persistence` | `backend/paideia-persistence/` | `src/test/java/io/github/xiaou61/persistence/` | — | WORK-001 |
| 认证授权 `paideia-security` | `backend/paideia-security/` | `src/test/java/io/github/xiaou61/security/` | — | WORK-001、WORK-003 |
| 账号与认证 `paideia-account` | `backend/paideia-account/`（账号表与注册登录、图形与邮箱验证码、refresh 轮换；对外契约在包根 `AccountApi`） | `src/test/java/io/github/xiaou61/account/`；集成测试在 `paideia-app`（`AccountAuthIntegrationTest`） | — | WORK-003 |
| 启动模块 `paideia-app` | `backend/paideia-app/` | `src/test/java/io/github/xiaou61/`（含授权隔离、认证集成与模块边界校验） | — | WORK-001、WORK-003 |
| 前端工作区 | `frontend/pnpm-workspace.yaml`、`frontend/package.json` | — | — | WORK-001、WORK-002 |
| 设计系统 `@paideia/ui` | `frontend/packages/ui/`（`styles/globals.css` 是令牌唯一来源，`components/` 是基元，`theme/` 是主题与首屏脚本，`layout/` 是外壳） | `src/tokens.test.ts`（令牌单一来源）、`src/theme/contrast.test.ts`（WCAG 对比度）、`src/theme/theme-context.test.tsx` | — | WORK-001、WORK-002 |
| 端口与 API 客户端 `@paideia/core` | `frontend/packages/core/`（`session.ts` 是角色判断的唯一入口） | `src/api.test.ts`、`src/workspace-boundaries.test.ts`、`src/session.test.ts` | — | WORK-001、WORK-002 |
| 共享认证 `@paideia/auth` | `frontend/packages/auth/`（令牌存储、401 刷新重放、登录/注册表单、登录门） | `src/auth.test.ts` | — | WORK-003 |
| Web 平台适配 `@paideia/platform-web` | `frontend/packages/platform-web/` | — | — | WORK-001 |
| 桌面平台适配 `@paideia/platform-desktop` | `frontend/packages/platform-desktop/`（`createBridgeCache` 由启动快照初始化同步镜像） | `src/index.test.ts` | — | WORK-001、WORK-002 |
| 学习者端 `@paideia/app` | `frontend/apps/app/`（Web 与桌面共用产物，端口 5173） | `e2e/home.spec.ts` | — | WORK-001、WORK-002 |
| 管理端 `@paideia/admin` | `frontend/apps/admin/`（仅 Web，端口 5174，不含路由——只有一屏） | `e2e/admin.spec.ts` | — | WORK-002 |
| 组件展览 `@paideia/ui-kit` | `frontend/apps/ui-kit/`（仅开发与内审，端口 5175，不进任何产品产物） | `e2e/ui-kit.spec.ts` | — | WORK-002 |
| 桌面壳 `@paideia/desktop` | `frontend/apps/desktop/`（Electron 主进程与 preload 在 `electron/`） | `e2e/desktop.spec.ts`（本机环境未运行） | — | WORK-001、WORK-002 |
| 公开页 `@paideia/public` | `frontend/apps/public/` | — | — | WORK-001、WORK-002 |

## HTML 理解材料

将用于解释架构、流程、状态机、数据流或交互的独立 HTML 放在 `.agent/html/`，但必须先取得用户明确同意。初始化器只创建空目录；新增保留文件后，在这里补充名称、用途和相对路径；这类材料不替代源码、测试或批准工件。

当前暂无 HTML 理解材料。

## 查看一次变更

| 想确认什么 | 查看位置 |
| --- | --- |
| 为什么要改、验收什么 | 对应工作项的 `requirements.md` |
| 为什么选择这种方案 | `proposal.md`、`design.md` |
| 实际改哪些步骤 | `tasks.md` |
| 当前未提交文件属于谁 | `workspace.md` 与 `git diff` |
| 如何证明改对 | `testing/plan.md`、`testing/report.md` |
| 交付后项目应保持什么行为 | `.agent/specs/` |
| 这批改动审出什么问题 | `.agent/reviews/`（按 ref 区间归档） |
