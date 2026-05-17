# 功能开发流程

这页给后续开发者一个“从 0 到可合并”的功能开发流程。它不是替代具体模块文档，而是告诉你新增或改造一个功能时，应该按什么顺序查资料、写代码、补文档和做验证。

如果只修一个小文案或样式，可以不完整走完这页。只要变更涉及接口、表、权限、AI、文件、通知、积分、WebSocket 或部署配置，就建议按这页过一遍。

## 一句话流程

先定边界，再找落点，然后写后端和前端，接入横切能力，最后同步文档和验证记录。

```text
需求边界 -> 源码落点 -> 数据模型 -> 后端接口 -> 前端页面 -> 横切能力 -> 文档同步 -> 发布前验证
```

## 1. 定功能边界

开写代码前，先把功能放进现有业务地图里。

| 问题 | 去哪里查 |
| --- | --- |
| 这是已有模块增强，还是新模块 | [模块总览](/modules/)、[全功能覆盖矩阵](/reference/feature-coverage) |
| 用户从哪个页面进入 | [前端路由索引](/reference/frontend-routes) |
| 管理端是否要运营入口 | [系统运营后台](/modules/system-ops)、对应模块页 |
| 后端应该放在哪个 Maven 模块 | [后端模块](/architecture/backend-modules)、[源码地图](/reference/source-map) |
| 是否会新增表或字段 | [数据库与脚本](/architecture/database)、[数据表索引](/reference/database-tables) |

这一阶段最重要的是避免“功能写完了才发现落错模块”。例如文件上传不要散落在业务模块里自己实现存储，应该优先复用 [文件存储](/modules/file-storage)；内容发布不要自己写一套敏感词逻辑，应该接入 [敏感词风控](/modules/sensitive)。

## 2. 先设计数据和状态

Code Nest 很多模块不是简单 CRUD，而是有状态流转、计数、积分、通知或审核。写接口前先把数据模型想清楚。

| 设计项 | 检查点 |
| --- | --- |
| 主表 | 是否有 `id`、创建人、状态、逻辑删除、创建/更新时间 |
| 明细表 | 是否需要评论、附件、标签、记录、历史版本或统计表 |
| 状态字段 | 是否需要枚举，前后端是否都能读懂 |
| 唯一约束 | 是否要防重复，例如版本号、收藏关系、用户每日签到 |
| 软删除 | 删除后前端列表、管理端列表、统计口径怎么处理 |
| 版本脚本 | 新表和字段是否有独立 SQL，并同步主库基线 |

如果状态会跨多个步骤推进，要优先在模块页写成状态机，而不是只写“保存成功”。OJ 提交、学习资产审核、模拟面试会话、版本发布、文件迁移都属于这类功能。

## 3. 写后端接口

后端开发建议按这个顺序：

1. 在对应 `xiaou-*` 模块里新增或修改 Controller、Service、Mapper、domain、DTO/VO。
2. 使用已有统一返回体和异常处理，不要在 Controller 里拼临时 JSON。
3. 当前登录用户从 Sa-Token 工具取，不要相信前端传来的 `userId`。
4. 管理端接口加管理鉴权，用户端接口加用户登录边界。
5. 写操作要考虑事务、幂等、重复提交、逻辑删除和统计回写。
6. 新增路由前缀后同步 [API 路由索引](/reference/api-routes)。

常见落点：

| 代码类型 | 常见位置 |
| --- | --- |
| 用户端业务接口 | `xiaou-*/src/main/java/com/xiaou/*/controller` |
| 管理端接口 | `xiaou-*/src/main/java/com/xiaou/*/controller/admin` 或模块内 admin Controller |
| 业务逻辑 | `xiaou-*/src/main/java/com/xiaou/*/service/impl` |
| 数据访问 | `xiaou-*/src/main/java/com/xiaou/*/mapper` |
| 跨模块公共能力 | 对应 `*-api` 模块或已有工具类 |
| 启动聚合 | `xiaou-application` 只负责启动和配置聚合，不应承载业务逻辑 |

## 4. 写前端页面

前端开发先判断入口属于用户端还是管理端：

| 类型 | 目录 | 重点 |
| --- | --- | --- |
| 用户端页面 | `vue3-user-front/src/views` | 用户体验、登录态、空态、失败态、上传、WebSocket |
| 用户端 API | `vue3-user-front/src/api` | 接口前缀、错误提示、Token 处理 |
| 管理端页面 | `vue3-admin-front/src/views` | 权限菜单、表格筛选、批量操作、高风险确认 |
| 管理端 API | `vue3-admin-front/src/api` | 后台接口前缀、分页、审核、状态切换 |

新增页面时至少同步三件事：

1. 路由和菜单入口。
2. API 封装和错误态。
3. [前端路由索引](/reference/frontend-routes) 和对应模块页。

如果页面使用 Markdown、富文本、代码高亮、Diff、RAG 命中片段或任何 `v-html`，先按 [前端渲染安全](/reference/frontend-rendering-security) 处理，不要直接把后端字符串塞进页面。

## 5. 接入横切能力

很多新功能不只是一个页面和一个接口。下面这些横切能力要主动判断是否需要接入：

| 能力 | 什么时候需要 | 先看 |
| --- | --- | --- |
| 鉴权 | 几乎所有用户端和管理端接口 | [鉴权与用户体系](/modules/auth) |
| 用户资料 | 当前用户、头像、昵称、状态 | [用户账户与个人中心](/modules/user-account) |
| 文件存储 | 头像、图片、附件、导出文件 | [文件存储](/modules/file-storage) |
| 通知 | 审核结果、互动提醒、系统消息 | [通知中心](/modules/notification) |
| 积分 | 奖励、扣减、抽奖、消费 | [积分与抽奖](/modules/points) |
| 敏感词 | 帖子、动态、评论、标题、简介 | [敏感词风控](/modules/sensitive) |
| WebSocket | 实时消息、在线态、ACK、失败态 | [WebSocket 协议](/reference/websocket) |
| AI Runtime | Prompt、Schema、RAG、Graph、回归 | [AI Runtime](/modules/ai-runtime) |
| 操作日志 | 管理端高风险写操作 | [仪表盘与日志](/modules/dashboard-logs) |

一个实用判断：如果这个功能会被用户感知、被管理员运营、被统计报表展示，或者失败后需要追责，就不要只写主流程，还要补状态、日志、通知和验证。

## 6. 同步文档

代码改完后，不要只补模块页。按变更范围同步：

| 变更 | 必补文档 |
| --- | --- |
| 新页面 | [前端路由索引](/reference/frontend-routes)、模块页、操作手册 |
| 新 Controller 或前缀 | [API 路由索引](/reference/api-routes) |
| 新表或字段 | [数据表索引](/reference/database-tables)、模块页 |
| 新错误码 | [响应体与错误码](/reference/response-errors) |
| 新 WebSocket 事件 | [WebSocket 协议](/reference/websocket) |
| 新 AI 场景 | [AI Runtime](/modules/ai-runtime)、[AI Schema 与治理](/reference/ai-schemas) |
| 新高风险链路 | [发布前验证](/guide/release-verification)、[验证记录与已知问题](/manuals/verified-scenarios) |
| 新完整模块 | [模块总览](/modules/)、[全功能覆盖矩阵](/reference/feature-coverage)、VitePress sidebar |

模块页建议至少补齐：

1. 功能定位。
2. 推荐学习顺序。
3. 源码地图。
4. 用户端和管理端入口。
5. API 分组。
6. 核心表。
7. 主流程。
8. 权限、安全和常见坑。
9. 验证清单。

## 7. 做发布前验证

验证不要凭感觉。先按 [发布前验证](/guide/release-verification) 判断变更类型，再选命令和烟测。

最低要求通常是：

| 改动范围 | 最低验证 |
| --- | --- |
| 后端业务 | 后端聚合构建，相关接口或页面烟测 |
| 用户端页面 | 用户端构建，打开真实路由验证 |
| 管理端页面 | 管理端构建，打开真实后台路由验证 |
| 文档 | `docs-site npm run build` |
| AI、OJ、聊天、文件上传 | 对应依赖可用性、失败态和模块验证清单 |

如果依赖没有启动，要写清楚“构建已通过，但 RAG/go-judge/Redis/MySQL 等依赖未完整联通”，不要把未跑过的链路写成已验证。

## 8. 合并前交接

合并前建议留下这几类信息：

| 信息 | 示例 |
| --- | --- |
| 改了什么 | 新增学习资产批量审核入口，补后台列表和审核接口 |
| 影响哪里 | 管理端学习资产页、`xiaou-learning-asset`、通知中心 |
| 怎么验证 | 后端构建、管理端构建、审核通过/驳回、通知回流 |
| 没验证什么 | 未启动真实消息推送，只验证站内通知记录 |
| 文档同步 | 模块页、API 索引、前端路由索引、验证记录已更新 |

这段交接可以放在 PR 描述、版本记录或验证记录里。重点不是写得长，而是让下一个人知道哪些结论是真的跑过，哪些还需要补测。

## 常见误区

| 误区 | 结果 | 更稳的做法 |
| --- | --- | --- |
| 先写页面，再临时补接口 | 字段、状态和分页口径容易反复改 | 先定 DTO/VO 和状态，再写页面 |
| 新增功能不更新索引 | 后来的人找不到入口 | 同步前端路由、API、表和覆盖矩阵 |
| 只验证成功路径 | 线上失败态不可控 | 至少补登录失效、权限不足、空态、外部依赖失败 |
| 自己实现横切能力 | 文件、通知、敏感词、积分逻辑分叉 | 优先复用平台能力 |
| 文档只写功能介绍 | 无法帮助维护 | 写源码位置、流程、表、常见坑和验证清单 |
