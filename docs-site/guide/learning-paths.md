# 学习路线

Code Nest 的文档已经按模块拆得很细，但第一次接手时不要从目录第一项一路读到最后。更好的方式是先按目标选择路线，再在需要时回到模块页查细节。

本页给你一组可执行的学习路径。每条路线都控制在“先建立全局认识，再读关键模块，最后做一次小验证”的节奏里。

## 所有人先读这 5 页

无论你是后端、前端、运维还是文档维护者，建议先用 30 到 60 分钟读完这几页：

| 顺序 | 文档 | 读完要知道什么 |
| --- | --- | --- |
| 1 | [架构总览](/architecture/overview) | 项目由后端多模块、两个前端、文档站、AI sidecar、OJ 沙箱和监控组成 |
| 2 | [源码地图](/reference/source-map) | 想查页面、Controller、Service、SQL、脚本时先去哪里 |
| 3 | [本地开发](/guide/local-dev) | 本地后端、用户端、管理端、文档站分别怎么启动 |
| 4 | [模块总览](/modules/) | 功能如何按学习、内容、平台能力、工具和后台分组 |
| 5 | [功能开发流程](/guide/feature-development) | 新增功能时怎么从需求边界走到验证交接 |
| 6 | [发布前验证](/guide/release-verification) | 改不同范围时应该跑哪些构建、接口和页面烟测 |

如果这几页读完后你能说清楚“我要改某个功能时从哪个前端路由、哪个 Controller、哪个表开始查”，后面就可以按角色深入。

## 后端开发路线

适合要改 Controller、Service、Mapper、权限、数据库或业务状态机的人。

| 顺序 | 文档 | 重点 |
| --- | --- | --- |
| 1 | [后端模块](/architecture/backend-modules) | Maven 多模块边界，业务代码应该落在哪个 `xiaou-*` 模块 |
| 2 | [鉴权与用户体系](/modules/auth) | 用户端和管理端两套登录域、拦截器、`@RequireAdmin` |
| 3 | [用户账户与个人中心](/modules/user-account) | 当前登录用户、验证码、资料、头像和用户状态 |
| 4 | [API 路由索引](/reference/api-routes) | Controller 前缀和业务域对应关系 |
| 5 | [数据表索引](/reference/database-tables) | 核心表和 SQL 脚本入口 |
| 6 | [响应体与错误码](/reference/response-errors) | 统一返回、登录失效、权限错误和文件错误 |

建议练习：

1. 任选一个模块页，例如 [OJ 判题系统](/modules/oj)。
2. 找到它的用户入口、管理入口、Controller、Service、Mapper 和核心表。
3. 用 [发布前验证](/guide/release-verification) 判断如果改这个模块要跑哪些验证。

## 前端开发路线

适合要改用户端页面、管理端页面、API 封装、Markdown 渲染、上传和 WebSocket 的人。

| 顺序 | 文档 | 重点 |
| --- | --- | --- |
| 1 | [前端应用](/architecture/frontend-apps) | 用户端、管理端、Vite 代理、Electron 打包边界 |
| 2 | [前端路由索引](/reference/frontend-routes) | 页面路径、模块文档和业务入口映射 |
| 3 | [鉴权与用户体系](/modules/auth) | Token 存储、登录态失效、管理端权限 |
| 4 | [前端渲染安全](/reference/frontend-rendering-security) | Markdown、`v-html`、代码高亮、Diff 和 RAG 片段怎么安全展示 |
| 5 | [文件存储](/modules/file-storage) | 头像、聊天图片、简历导出、内容附件的上传和访问 |
| 6 | [WebSocket 协议](/reference/websocket) | 聊天 ws-ticket、事件类型、失败态和客户端行为 |

建议练习：

1. 在 [前端路由索引](/reference/frontend-routes) 找一个用户端页面。
2. 回到对应模块页，确认它调用哪个 API 前缀。
3. 检查页面有没有 Markdown、富文本、上传、WebSocket 或权限边界。

## AI、OJ 与高风险链路路线

适合要改 Prompt、Schema、RAG、Graph、OJ 判题、模拟面试、SQL 优化或结构化输出的人。

| 顺序 | 文档 | 重点 |
| --- | --- | --- |
| 1 | [AI Runtime](/modules/ai-runtime) | Prompt、Schema、RAG、Graph、Eval、Metrics 分层 |
| 2 | [AI Schema 与治理](/reference/ai-schemas) | 场景注册、结构化输出、回归样例和治理评分 |
| 3 | [模拟面试与求职作战台](/modules/mock-interview-job-battle) | AI 会话、追问、报告、Job Battle 阶段事件 |
| 4 | [SQL 优化工作台](/modules/sql-optimizer) | SQL 分析、重写、对比、历史记录和安全边界 |
| 5 | [OJ 判题系统](/modules/oj) | 提交状态机、go-judge、ACM 排名和首次 AC 积分 |
| 6 | [验证记录与已知问题](/manuals/verified-scenarios) | RAG sidecar、go-judge、历史验证缺口和补测优先级 |

这一类变更不要只看接口是否返回 `200`。要额外检查结构化字段、状态推进、失败态、外部依赖和回归样例。

## 运维与发布路线

适合要部署、联调、排障、发版本或维护 CI 的人。

| 顺序 | 文档 | 重点 |
| --- | --- | --- |
| 1 | [本地开发](/guide/local-dev) | 本地服务端口、`/api` 上下文、CORS、文件访问 |
| 2 | [Docker 与服务部署](/operations/docker) | 后端镜像、前端部署、AI compose、go-judge、Nginx 和环境变量 |
| 3 | [监控与观测](/operations/monitoring) | Actuator、Prometheus、Grafana、AI Runtime 指标和告警 |
| 4 | [常见问题排查](/operations/troubleshooting) | 后端启动、401、CORS、WebSocket、文件 URL 等问题 |
| 5 | [发布前验证](/guide/release-verification) | 发布前最终清单和 CI 覆盖边界 |
| 6 | [版本历史](/modules/version-history) | 版本记录、发布状态和用户可见更新说明 |

建议练习：

1. 本地跑一次文档站构建。
2. 找到一个已知问题，判断它属于部署、依赖、接口、前端缓存还是业务逻辑。
3. 把验证结论写成能放进 [验证记录与已知问题](/manuals/verified-scenarios) 的一句话。

## 文档维护路线

适合要继续补 v2.2.0 文档、维护模块页、写操作手册或同步新功能的人。

| 顺序 | 文档 | 重点 |
| --- | --- | --- |
| 1 | [文档维护规范](/guide/documentation-maintenance) | 新页面、新接口、新表、新错误码、新 AI 场景应该同步哪里 |
| 2 | [功能开发流程](/guide/feature-development) | 代码、文档、验证和交接如何连起来 |
| 3 | [全功能覆盖矩阵](/reference/feature-coverage) | 检查功能是否有用户入口、管理入口、API、表和模块页 |
| 4 | [模块总览](/modules/) | 模块页统一模板和依赖关系 |
| 5 | [核心链路教程](/manuals/core-workflows) | 用户视角的端到端流程 |
| 6 | [验证记录与已知问题](/manuals/verified-scenarios) | 真实验证结果和已知缺口 |
| 7 | [v2.2.0 文档计划](/roadmap/v2.2.0-docs-plan) | 已完成阶段和后续补强节奏 |

新增文档时，优先回答三个问题：

1. 读者为什么要看这一页。
2. 读完后能定位哪些源码、页面、接口或表。
3. 改这个功能时要怎么验证。

## 读完后的验收标准

你不需要记住所有文档内容，但应该能做到：

| 能力 | 自测问题 |
| --- | --- |
| 定位功能 | 一个页面出问题时，能找到路由、模块页、Controller 和表 |
| 判断风险 | 知道改动会影响鉴权、文件、通知、积分、AI、OJ 还是 WebSocket |
| 选择验证 | 能按改动范围列出最低构建和烟测项 |
| 更新文档 | 新增功能后知道同步哪些索引、模块页和验证记录 |
| 交接结论 | 能把“已验证”和“未验证原因”写清楚，方便下一个人继续 |

如果你已经能做到这些，再去读单个模块的源码会快很多。
