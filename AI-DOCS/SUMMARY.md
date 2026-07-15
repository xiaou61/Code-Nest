# AI-DOCS 分类索引

`AI-DOCS` 保存产品需求、技术专题、测试证据、部署材料和历史归档。与当前代码直接对应的架构、模块、API、操作和运维文档统一维护在 `docs-site`。

## 当前规模

截至 2026-07-14，`AI-DOCS` 共 177 个 Markdown 文件：

| 分类 | 文件数 | 定位 |
| --- | ---: | --- |
| `PRD` | 32 | 产品目标、范围、流程和验收标准 |
| `Technical` | 22 | 技术专题、面试亮点和设计说明 |
| `Development` | 8 | 环境、规范和贡献材料 |
| `Testing` | 5 | 测试计划、用例和报告 |
| `User` | 4 | 用户材料和 FAQ |
| `Deployment` | 3 | 部署专题材料 |
| `assets` | 7 | 模板和文档资源 |
| `Archive` | 95 | 历史版本、废弃方案和原始文档快照 |
| 根索引 | 1 | 本页 |

## 分类入口

- [PRD](./PRD/README.md)：需求和验收边界。
- [技术专题](./Technical/README.md)：架构专题、技术亮点和专项分析。
- [开发材料](./Development/README.md)：环境、规范和贡献流程。
- [测试材料](./Testing/README.md)：测试计划、用例和结果证据。
- [用户材料](./User/README.md)：用户手册与 FAQ 原始资料。
- [部署材料](./Deployment/README.md)：部署专题和交付说明。
- [历史归档](./Archive/README.md)：不再作为当前实现依据的文档。
- [资源与模板](./assets/README.md)：PRD、技术、测试和交付模板。

## 与 VitePress 的关系

| 内容 | 默认入口 |
| --- | --- |
| 当前系统架构、模块、API 和运维事实 | `docs-site` |
| 产品需求和设计背景 | `AI-DOCS/PRD`、`AI-DOCS/Technical` |
| 测试过程和交付证据 | `AI-DOCS/Testing` |
| 历史方案和旧版本副本 | `AI-DOCS/Archive` |

当一份专题材料影响当前开发或运维流程时，在 VitePress 中写一份简洁、可执行的现行说明，并链接到这里的背景材料。不要直接把归档文档加入主导航。

## 维护规则

1. 新文档进入最具体的分类目录，不放在 `AI-DOCS` 根目录。
2. 文件名说明主题；需要独立版本管理的 PRD 使用语义化版本号。
3. 当前实现发生变化时，优先更新 `docs-site`，再更新相关专题材料。
4. 废弃方案移动到 `Archive`，并在标题或开头标明状态。
5. 每个分类的 `README.md` 维护入口和新增规则。
6. 不在两处维护相同的接口表、模块数、数据库表数或环境变量清单。

VitePress 的分类和校验规则见 `docs-site/guide/documentation-maintenance.md`。
