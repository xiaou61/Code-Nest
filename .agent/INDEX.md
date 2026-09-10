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
| 项目理解型 HTML | `.agent/html/` |
| 更新历史与 Git 历史视图 | `.agent/history/updates.md`、`.agent/history/core-components.md` |

## 模块索引

只登记已经从仓库确认的模块和路径；发现新模块或路径发生实质变化时更新对应行。

共 36 个后端 Maven 模块、2 套前端应用、1 个 Python AI 服务，以及部署运维资产。测试列统计该模块 `src/test/`（前端与 Python 为 `tests/`）下的文件数；「无」表示不存在测试目录，不代表没有其他形式的验证。

### 后端：架构与公共平台层

| 模块 | 源码或配置 | 测试 | 稳定规格 | 相关工作项 |
| --- | --- | --- | --- | --- |
| `xiaou-bootstrap` | `xiaou-bootstrap/` | `src/test/`（1） | `.agent/specs/architecture.md` | — |
| `xiaou-application` | `xiaou-application/` | `src/test/`（22） | `.agent/specs/architecture.md` | — |
| `xiaou-common` | `xiaou-common/` | 无 | `.agent/specs/architecture.md` | — |
| `xiaou-common-core` | `xiaou-common-core/` | 无 | `.agent/specs/architecture.md` | — |
| `xiaou-common-web` | `xiaou-common-web/` | `src/test/`（1） | `.agent/specs/architecture.md` | — |
| `xiaou-common-security` | `xiaou-common-security/` | 无 | `.agent/specs/architecture.md` | — |
| `xiaou-common-cache` | `xiaou-common-cache/` | `src/test/`（2） | `.agent/specs/architecture.md` | — |
| `xiaou-common-persistence` | `xiaou-common-persistence/` | 无 | `.agent/specs/architecture.md` | — |
| `xiaou-resilience` | `xiaou-resilience/` | `src/test/`（1） | `.agent/specs/architecture.md` | — |
| `xiaou-user-api` | `xiaou-user-api/` | 无 | `.agent/specs/architecture.md` | — |
| `xiaou-sensitive-api` | `xiaou-sensitive-api/` | 无 | `.agent/specs/architecture.md` | — |

### 后端：业务模块

| 模块 | 源码或配置 | 测试 | 稳定规格 | 相关工作项 |
| --- | --- | --- | --- | --- |
| `xiaou-system` | `xiaou-system/` | `src/test/`（52） | `.agent/specs/system-and-access.md` | — |
| `xiaou-user` | `xiaou-user/` | `src/test/`（1） | `.agent/specs/system-and-access.md` | — |
| `xiaou-ai` | `xiaou-ai/` | `src/test/`（50） | `.agent/specs/ai-services.md` | — |
| `xiaou-community` | `xiaou-community/` | `src/test/`（2） | `.agent/specs/content-community.md` | — |
| `xiaou-moment` | `xiaou-moment/` | `src/test/`（1） | `.agent/specs/content-community.md` | — |
| `xiaou-blog` | `xiaou-blog/` | 无 | `.agent/specs/content-community.md` | — |
| `xiaou-codepen` | `xiaou-codepen/` | 无 | `.agent/specs/content-community.md` | — |
| `xiaou-chat` | `xiaou-chat/` | `src/test/`（2） | `.agent/specs/content-community.md` | — |
| `xiaou-notification` | `xiaou-notification/` | `src/test/`（2） | `.agent/specs/content-community.md` | — |
| `xiaou-interview` | `xiaou-interview/` | 无 | `.agent/specs/learning-and-interview.md` | — |
| `xiaou-mock-interview` | `xiaou-mock-interview/` | `src/test/`（5） | `.agent/specs/learning-and-interview.md` | — |
| `xiaou-flashcard` | `xiaou-flashcard/` | 无 | `.agent/specs/learning-and-interview.md` | — |
| `xiaou-learning-asset` | `xiaou-learning-asset/` | `src/test/`（4） | `.agent/specs/learning-and-interview.md` | — |
| `xiaou-resume` | `xiaou-resume/` | 无 | `.agent/specs/learning-and-interview.md` | — |
| `xiaou-plan` | `xiaou-plan/` | `src/test/`（3） | `.agent/specs/learning-and-interview.md` | — |
| `xiaou-oj` | `xiaou-oj/`；Mapper XML 特例在 `src/main/java/com/xiaou/oj/mapper/` | `src/test/`（7） | `.agent/specs/online-judge-and-tools.md` | — |
| `xiaou-sql-optimizer` | `xiaou-sql-optimizer/` | `src/test/`（1） | `.agent/specs/online-judge-and-tools.md` | — |
| `xiaou-knowledge` | `xiaou-knowledge/` | 无 | `.agent/specs/online-judge-and-tools.md` | — |
| `xiaou-version` | `xiaou-version/` | 无 | `.agent/specs/online-judge-and-tools.md` | — |
| `xiaou-moyu` | `xiaou-moyu/` | `src/test/`（1） | `.agent/specs/online-judge-and-tools.md` | — |
| `xiaou-points` | `xiaou-points/` | `src/test/`（2） | `.agent/specs/engagement-and-collab.md` | — |
| `xiaou-team` | `xiaou-team/` | 无 | `.agent/specs/engagement-and-collab.md` | — |
| `xiaou-filestorage` | `xiaou-filestorage/` | `src/test/`（3） | `.agent/specs/engagement-and-collab.md` | — |
| `xiaou-sensitive` | `xiaou-sensitive/` | `src/test/`（2） | `.agent/specs/engagement-and-collab.md` | — |
| `xiaou-sre` | `xiaou-sre/` | `src/test/`（36） | `.agent/specs/operations-sre.md` | — |

### 前端、AI 服务与运维资产

| 对象 | 源码或配置 | 测试 | 稳定规格 | 相关工作项 |
| --- | --- | --- | --- | --- |
| `vue3-admin-front`（管理端，端口 3000） | `vue3-admin-front/` | `tests/`（11） | `.agent/specs/frontend.md` | — |
| `vue3-user-front`（用户端，端口 3001） | `vue3-user-front/` | `tests/`（19） | `.agent/specs/frontend.md` | — |
| `code-nest-api-contract` | `code-nest-api-contract/` | `tests/` | `.agent/specs/frontend.md` | — |
| `code-nest-design-system` | `code-nest-design-system/` | 无 | `.agent/specs/frontend.md` | — |
| `llamaindex-service` | `llamaindex-service/` | `tests/` | `.agent/specs/ai-services.md` | — |
| 部署与运维资产 | `deploy/`、`docker/`、`scripts/` | `scripts/*.test.sh`、`scripts/test_*.py` | `.agent/specs/operations-sre.md` | — |
| 数据库脚本 | `sql/MySql/`（基线）、`sql/vX.Y.Z/`（增量） | — | `.agent/specs/architecture.md` | — |

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
