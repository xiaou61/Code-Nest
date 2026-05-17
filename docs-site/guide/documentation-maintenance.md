# 文档维护规范

这套 VitePress 文档站是 v2.2.0 的独立模块。之后每次新增功能、改接口、改表、改页面，都应该同步更新文档，避免代码和说明分叉。

## 新功能文档检查清单

| 检查项 | 要更新的位置 |
| --- | --- |
| 新用户页面 | [前端路由索引](/reference/frontend-routes)、对应模块页、用户端操作手册 |
| 新管理页面 | [前端路由索引](/reference/frontend-routes)、对应模块页、管理端操作手册 |
| 新 Controller 或路由前缀 | [API 路由索引](/reference/api-routes) |
| 新数据表或字段 | [数据表索引](/reference/database-tables)、模块页 |
| 新 WebSocket 事件 | [WebSocket 协议](/reference/websocket) |
| 新错误码 | [响应体与错误码](/reference/response-errors) |
| 新 AI Prompt、RAG、Schema 或回归 | [AI Schema 与治理](/reference/ai-schemas)、[AI Runtime](/modules/ai-runtime) |
| 新 Markdown、富文本或 `v-html` 展示 | [前端渲染安全](/reference/frontend-rendering-security)、对应模块页 |
| 新业务链路、部署配置或高风险修复 | [发布前验证](/guide/release-verification)、[验证记录与已知问题](/manuals/verified-scenarios) |
| 新截图或补测结果 | 对应操作手册、[验证记录与已知问题](/manuals/verified-scenarios) |
| 新模块 | [模块总览](/modules/)、[全功能覆盖矩阵](/reference/feature-coverage)、VitePress sidebar |

如果不是单纯改文档，而是要从代码层面新增一个完整功能，先按 [功能开发流程](/guide/feature-development) 走一遍，再回到本页检查文档同步位置。

## 模块页模板

每个模块页都应该逐步补齐这些部分：

1. 功能定位。
2. 用户端入口。
3. 管理端入口。
4. API 前缀和 Controller。
5. Service、Mapper、核心表。
6. 核心流程。
7. 状态机或关键枚举。
8. 权限、安全和风控边界。
9. 已知问题和验证方式。

首版可以先写 1 到 6，后续每次改功能时补齐 7 到 9。

## 写作规则

| 规则 | 说明 |
| --- | --- |
| 先读源码 | 不凭记忆写接口和表名 |
| 用源码路径 | 关键结论尽量给出 `xiaou-*`、`vue3-*` 或 `sql/*` 路径 |
| 少写口号 | 文档面向维护和交付，不写营销文案 |
| 保持中文术语一致 | 同一个功能不要在不同页面混用多个名字 |
| 不嵌入密钥 | 示例配置不得出现真实 token、API key、数据库密码 |
| 审查 `v-html` | 新增内容渲染前先确认是否走 `renderMarkdown`、`sanitizeHtml` 或 `escapeHtml` |
| 构建验证 | 每批文档变更都运行 `npm run build` |

## 提交前验证

```powershell
cd docs-site
npm run build
```

如果这次只改 `docs-site/**`，上面的构建通常就是最低要求。如果同时改了后端、用户端、管理端、AI、OJ、聊天、文件上传或部署配置，要先按 [发布前验证](/guide/release-verification) 选择对应验证项，再提交。

建议在提交前再执行：

```powershell
git diff --check
git status --short
```

## 推荐提交粒度

| 类型 | 提交示例 |
| --- | --- |
| 新文档站骨架 | `docs: add standalone VitePress site` |
| 模块覆盖 | `docs: expand feature module coverage` |
| 参考索引 | `docs: add api and data reference indexes` |
| 操作手册 | `docs: add operation manuals and workflow guides` |
| 模块深化 | `docs: deepen oj and mock interview docs` |

## 文档站本地开发

```powershell
cd docs-site
npm install
npm run docs:dev
```

如果端口被占用，可以使用 VitePress 的端口参数：

```powershell
npm run docs:dev -- --port 5178
```

## 版本节奏

1. 功能开发分支先改代码和文档。
2. 合并前按 [发布前验证](/guide/release-verification) 跑后端、前端、文档和关键业务烟测。
3. 发布版本时更新 [版本历史](/modules/version-history) 和 [v2.2.0 文档计划](/roadmap/v2.2.0-docs-plan)。
4. 手册截图有变更时，先放入 `docs/manual-assets/<date>`，再更新文档站索引。
