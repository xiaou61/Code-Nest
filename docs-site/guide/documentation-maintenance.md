# 文档维护规范

本页说明 Code Nest 文档站（`docs-site`）的日常维护流程，包括目录结构、各类型页面的定位和更新原则、校验方法，以及变更时必须同步检查的关联页面。

文档站的目标是让新人快速定位代码入口、让开发者在改功能时顺便补文档、让页面之间的导航链路不会断。

## 目录结构

```text
docs-site/
├── .vitepress/
│   └── config.mts        # 侧栏和导航配置
├── guide/                 # 使用和流程指南
│   ├── quick-start.md     # 新人快速开始
│   ├── local-dev.md       # 本地开发环境搭建
│   ├── deploy.md          # 独立部署
│   ├── documentation-maintenance.md  # 本页
│   ├── learning-paths.md  # 学习路径推荐
│   ├── onboarding-tasks.md # 入职打卡任务
│   ├── feature-development.md # 功能开发流程
│   ├── release-verification.md  # 发布前验证
│   ├── testing-regression.md    # 测试回归
│   └── git-log-release-notes.md # Git 日志和发布说明
├── modules/               # 按功能拆分的模块教程
│   ├── index.md           # 模块总览与导航
│   ├── auth.md            # 鉴权
│   ├── blog.md            # 博客
│   ├── chat.md            # 聊天
│   ├── codepen.md         # 代码工坊
│   ├── community-content.md  # 社区内容矩阵
│   ├── community.md       # 社区互动
│   ├── dashboard-logs.md  # 仪表盘与日志
│   ├── dev-tools.md       # 开发工具
│   ├── interview-and-growth.md # 面试与成长
│   ├── interview.md       # 面试题库
│   ├── flashcard.md       # 闪卡
│   ├── knowledge.md       # 知识库
│   ├── learning-assets.md # 学习资产
│   ├── mock-interview-job-battle.md # 模拟面试与求职
│   ├── moments.md         # 动态
│   ├── moyu.md            # 摸鱼
│   ├── notification.md    # 通知
│   ├── oj.md              # OJ 判题
│   ├── plan-team.md       # 计划与学习小组
│   ├── points.md          # 积分与抽奖
│   ├── resume.md          # 简历
│   ├── sensitive.md       # 敏感词风控
│   ├── sql-optimizer.md   # SQL 优化
│   ├── system-ops.md      # 系统运营后台
│   ├── tools-moyu-version.md  # 开发工具/摸鱼/版本
│   ├── user-account.md    # 用户账户
│   ├── file-storage.md    # 文件存储
│   └── ai-runtime.md      # AI Runtime
├── reference/             # 可查询的索引和参考
│   ├── api-routes.md      # API 路由
│   ├── database-tables.md # 数据表
│   ├── frontend-routes.md # 前端路由
│   ├── source-map.md      # 源码地图
│   ├── ai-schemas.md      # AI Schema
│   ├── websocket.md       # WebSocket 协议
│   ├── response-errors.md # 响应码与错误
│   ├── permission-boundaries.md # 权限边界
│   ├── feature-coverage.md     # 功能覆盖矩阵
│   ├── module-regression-matrix.md # 回归验证矩阵
│   ├── module-state-machines.md # 状态机索引
│   ├── statistics-ranking-counts.md # 统计与排名口径
│   ├── docs-sync-baseline.md    # 文档同步基线
│   ├── frontend-rendering-security.md # 前端渲染安全
│   └── glossary.md        # 术语表
├── architecture/          # 架构总览
│   ├── overview.md
│   ├── backend-modules.md
│   ├── frontend-apps.md
│   └── database.md
├── operations/            # 运维操作
│   ├── incident-response.md
│   └── troubleshooting.md
├── roadmap/               # 版本计划
└── index.md               # 首页
```

## 页面类型和定位

| 类型 | 目录 | 写给谁看 | 更新时机 |
| --- | --- | --- | --- |
| 指南页 | `guide/` | 新人或操作者 | 流程变更时 |
| 模块页 | `modules/` | 开发者 | 功能新增或修改时 |
| 参考页 | `reference/` | 查表用 | API/路由/表结构变更时 |
| 架构页 | `architecture/` | 架构师或新人 | 大范围重构时 |
| 运维页 | `operations/` | 值班和运维 | 故障复盘或流程调整时 |
| 路线图 | `roadmap/` | 团队 | 版本迭代时 |

## 变更检查矩阵

当你改了一类页面，要同步检查的关联页面：

| 改动位置 | 必须检查 |
| --- | --- |
| 模块页 | 源码地图、API 路由、前端路由、数据库表、术语表、模块总览 |
| 新增 API | API 路由、权限边界、对应模块页 |
| 新增前端路由 | 前端路由、对应模块页 |
| 新增数据表 | 数据库表、对应模块页 |
| 新增状态值 | 术语表、状态机索引 |
| 新增权限 | 权限边界、鉴权模块页 |
| 修改部署配置 | 部署指南、本地开发 |
| 修改 WebSocket 协议 | WebSocket 协议页、聊天模块页 |
| 修改 AI Schema | AI Schema 页、AI Runtime 模块页 |
| 新增 v-html 使用 | 前端渲染安全 |

## VitePress 构建和校验

### 构建

```bash
cd docs-site
npm ci
npm run build
```

构建会检查：

1. Markdown 链接是否存在目标文件。
2. Vue 模板语法是否正确。
3. 侧栏配置中引用的页面是否存在。

### 常见构建错误

| 错误 | 原因 | 修复 |
| --- | --- | --- |
| 死链接 | 链接指向不存在的页或标题 | 检查路径和文件名 |
| Vue 模板编译失败 | 正文里有未转义尖括号 | 用 `&lt;` 和 `&gt;` 替换 |
| 侧栏找不到文件 | config.mts 引用的路径与实际文件不一致 | 对齐路径 |

### 尖括号转义

VitePress 用 Vue 编译 Markdown，尖括号会被当作 HTML 标签。以下内容必须转义：

| 场景 | 错误写法 | 正确写法 |
| --- | --- | --- |
| Java 泛型 | `List&lt;String&gt;` | `List&lt;String&gt;` |
| 路由参数 | `/dev-tools/&lt;toolName&gt;` | `/dev-tools/&lt;toolName&gt;` |
| HTML 标签提及 | `&lt;script&gt;` | 用中文"脚本标签"或 `&lt;script&gt;` |
| Vue 组件名 | `&lt;KeepAlive&gt;` | `&lt;KeepAlive&gt;` |

### localhost 链接

VitePress 会把 Markdown 代码块中的 localhost URL 也当作链接去验证。如果写示例命令，用环境变量替代：

| 不推荐 | 推荐 |
| --- | --- |
| `curl http://localhost:9999/api/health` | `curl ${BACKEND_HOST}/api/health`，再在注释中说明默认值 |

## 写作约定

### 文件名和标题

- 文件名使用 kebab-case，例如 `plan-team.md`。
- 页面标题用中文，首行 `#` 后面直接跟标题，不加编号。

### 模块页模板

每个模块页建议包含以下段落：

1. **定位** — 一段话说清模块做什么、给谁用。
2. **源码入口** — 控制器、服务、Mapper、前端组件的路径表。
3. **API 一览** — 用户端和管理端的主要接口。
4. **核心流程** — 关键业务的状态流转或调用链。
5. **数据表** — 主要表名和字段概要。
6. **常见坑** — 开发时容易踩的雷。
7. **验证清单** — 改完代码后怎么验证。

### 表格约定

- 状态值表格至少列出 code 和含义。
- API 表格至少列出方法、路径和用途。
- 状态机图用文本表格或缩进列表，不用 Mermaid。

### 交叉链接

- 模块页之间用相对路径，例如 `[鉴权](/modules/auth)`。
- 同一页面的标题链接用 `[标题](#标题锚点)`。
- 不使用绝对 URL，避免 VitePress 死链接检查误报。

## 同步脚本

`sync:baseline` 脚本在 dev/build/preview 前自动执行，它会刷新：

- 源码地图中的文件列表
- API 路由索引
- 前端路由索引
- 数据库表索引

如果脚本执行失败，构建也会失败。常见原因是 Java 源码路径变更后脚本中的 glob 模式没有更新。

## 新增页面检查清单

当你新增一个文档页面时：

1. 确认页面放在正确的目录下（guide/modules/reference/architecture/operations）。
2. 在 `.vitepress/config.mts` 的侧栏配置中添加条目。
3. 在 `modules/index.md` 的总览表或对应索引页添加一行。
4. 构建通过且无死链接。
5. 搜索相关术语表，确保新术语已收录。
6. 如果涉及新 API、新路由或新表，同步更新对应的参考页。
7. 如果涉及新状态值，同步更新术语表的状态速查。
