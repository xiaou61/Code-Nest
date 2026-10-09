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
| 认证授权 `paideia-security` | `backend/paideia-security/` | `src/test/java/io/github/xiaou61/security/` | — | WORK-001 |
| 启动模块 `paideia-app` | `backend/paideia-app/` | `src/test/java/io/github/xiaou61/`（含授权隔离与模块边界校验） | — | WORK-001 |
| 前端工作区 | `frontend/pnpm-workspace.yaml`、`frontend/package.json` | — | — | WORK-001 |
| 展示组件 `@paideia/ui` | `frontend/packages/ui/` | — | — | WORK-001 |
| 端口与 API 客户端 `@paideia/core` | `frontend/packages/core/` | `src/api.test.ts`、`src/workspace-boundaries.test.ts` | — | WORK-001 |
| Web 平台适配 `@paideia/platform-web` | `frontend/packages/platform-web/` | — | — | WORK-001 |
| 桌面平台适配 `@paideia/platform-desktop` | `frontend/packages/platform-desktop/` | — | — | WORK-001 |
| 主体 SPA `@paideia/app` | `frontend/apps/app/` | `e2e/home.spec.ts` | — | WORK-001 |
| 桌面壳 `@paideia/desktop` | `frontend/apps/desktop/`（Electron 主进程与 preload 在 `electron/`） | `e2e/desktop.spec.ts`（本机环境未运行） | — | WORK-001 |
| 公开页 `@paideia/public` | `frontend/apps/public/` | — | — | WORK-001 |

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
