# 模块总览

Code Nest 的功能可以按读者视角分成五组：成长学习、内容社区、AI 能力、运营后台、基础设施。

如果你不是想“查某个模块”，而是想把整个项目按业务模块系统学一遍，建议先读 [按模块学习路线](/guide/module-learning-paths)。那一页会把账号、安全、学习、OJ、AI、内容、平台能力和后台运营串成连续学习顺序。

## 推荐学习顺序

如果你是第一次接手 Code Nest，不建议按目录从上到下硬读。更高效的顺序是：

1. 先读 [架构总览](/architecture/overview) 和 [源码地图](/reference/source-map)，知道后端多模块、用户端、管理端和文档站分别在哪里。
2. 再读 [鉴权与用户体系](/modules/auth) 和 [用户账户与个人中心](/modules/user-account)，理解普通用户和管理员为什么是两套登录域。
3. 接着读一个完整业务链路，比如 [OJ 判题系统](/modules/oj) 或 [模拟面试与求职作战台](/modules/mock-interview-job-battle)，练习从页面、Controller、Service、Mapper 到表结构的阅读方法。
4. 然后读 [通知中心](/modules/notification)、[文件存储](/modules/file-storage)、[敏感词风控](/modules/sensitive) 这些平台能力，因为很多业务模块都会调用它们。
5. 最后读 [全功能覆盖矩阵](/reference/feature-coverage)，用它检查某个功能有没有用户入口、管理入口、API 前缀、核心表和文档页。

## 按任务找文档

| 我想做的事 | 先看 |
| --- | --- |
| 不知道按什么顺序读 | [学习路线](/guide/learning-paths) |
| 想按业务模块一块一块系统学习 | [按模块学习路线](/guide/module-learning-paths) |
| 想按模块学习并留下可验收作业 | [模块学习任务模板](/guide/module-learning-workbook) |
| 想看跨模块综合示范答案 | [模块结业案例](/guide/module-learning-capstone) |
| 想统一评估模块学习作业质量 | [模块学习评审表](/guide/module-learning-rubric) |
| 想把学习流程排成带教节奏 | [带教执行剧本](/guide/onboarding-execution-playbook) |
| 想把上手变成练习任务 | [角色上手任务包](/guide/onboarding-tasks) |
| 看不懂某个项目名词 | [术语表](/reference/glossary) |
| 本地把项目跑起来 | [本地完整启动剧本](/guide/startup-playbook)、[本地开发](/guide/local-dev) |
| 理解端到端业务链路 | [端到端业务链路图](/manuals/business-flow-map)、[核心链路教程](/manuals/core-workflows) |
| 从 0 开发一个新功能 | [功能开发流程](/guide/feature-development)、[常见二开场景](/guide/extension-scenarios) |
| 新增一个用户端功能 | [前端路由索引](/reference/frontend-routes)、对应模块页、[API 分组索引](/reference/api-routes) |
| 调试一个后端接口 | [API 调用示例](/reference/api-examples)、[响应体与错误码](/reference/response-errors) |
| 新增一个后台管理页 | [鉴权与用户体系](/modules/auth)、[系统运营后台](/modules/system-ops)、对应业务模块页 |
| 新增一张表或字段 | [数据库与脚本](/architecture/database)、[数据表索引](/reference/database-tables)、[数据库字段阅读指南](/reference/database-field-guide)、对应模块页 |
| 接入 AI 能力 | [AI Runtime](/modules/ai-runtime)、[AI Schema 与治理](/reference/ai-schemas) |
| 检查安全边界 | [权限与安全边界](/guide/security-boundaries)、[前端渲染安全](/reference/frontend-rendering-security) |
| 选择测试和回归 | [测试与回归](/guide/testing-regression)、[发布前验证](/guide/release-verification) |
| 排查线上问题 | [问题定位流程](/operations/diagnosis-flow)、[监控与观测](/operations/monitoring)、[仪表盘与日志](/modules/dashboard-logs) |
| 整理部署配置 | [环境变量总表](/operations/env-vars)、[Docker 与服务部署](/operations/docker) |
| 验收一个版本功能 | [核心链路教程](/manuals/core-workflows)、[验证记录与已知问题](/manuals/verified-scenarios) |

## 源码地图

模块总览不展开每个类的源码细节，而是告诉你从哪里进入全项目索引。真正查代码时，建议先用这些索引定位目录，再跳到具体模块页。

| 我需要定位 | 先看 |
| --- | --- |
| 后端多模块、前端应用、文档站目录 | [源码地图](/reference/source-map) |
| 模块之间的影响面和平台依赖 | [模块依赖地图](/reference/module-dependencies) |
| 用户端和管理端页面路由 | [前端路由索引](/reference/frontend-routes) |
| Controller 路由和接口分组 | [API 分组索引](/reference/api-routes) |
| 表结构和版本脚本 | [数据表索引](/reference/database-tables) |
| 统一返回体、登录失效、权限错误 | [返回体与错误码](/reference/response-errors) |
| Markdown、富文本和 `v-html` 风险 | [前端渲染安全](/reference/frontend-rendering-security) |
| AI Prompt、Schema、RAG、回归 | [AI Schema 与治理](/reference/ai-schemas) |
| 项目名词和统一叫法 | [术语表](/reference/glossary) |

## 模块依赖关系

很多页面不是孤立功能。读源码时可以用下面的依赖关系定位影响范围：

| 基础能力 | 被哪些模块调用 | 典型场景 |
| --- | --- | --- |
| 鉴权 | 几乎所有用户端和管理端接口 | 登录、角色校验、当前用户 ID |
| 用户账户 | 积分、通知、社区、聊天、学习统计 | 注册、资料、头像、状态 |
| 文件存储 | 头像、简历导出、聊天图片、内容附件 | 上传、访问 URL、迁移 |
| 通知中心 | 注册、资料变更、社区互动、审核结果 | 站内信、公告、模板消息 |
| 积分 | OJ、签到、博客、抽奖、CodePen | 奖励、扣减、流水 |
| 敏感词 | 社区、动态、评论、内容发布 | 拦截、替换、记录 |
| AI Runtime | 模拟面试、求职作战台、SQL 优化、社区摘要 | Prompt、Schema、RAG、回归 |
| 前端渲染安全 | 社区、博客、题库、OJ、闪卡、通知、开发者工具 | Markdown、`v-html`、高亮和 Diff 展示 |

## 成长学习

| 功能 | 用户端 | 管理端 | 后端模块 |
| --- | --- | --- | --- |
| 面试题库 | 题单、刷题、收藏、复习 | 分类、题单、题目管理 | `xiaou-interview` |
| OJ | 题目、提交、赛事、排行 | 题目、赛事、标签管理 | `xiaou-oj` |
| AI 模拟面试 | 配置、会话、报告、历史 | 会话和方向管理 | `xiaou-mock-interview`、`xiaou-ai` |
| 求职闭环 | 求职作战台、岗位匹配、成长驾驶舱 | AI 治理与配置 | `xiaou-mock-interview`、`xiaou-ai` |
| 闪卡 | 卡组、学习、编辑 | 后续可扩展审核 | `xiaou-flashcard` |
| 计划打卡 | 计划、每日打卡 | 后续可扩展统计 | `xiaou-plan` |
| 学习小组 | 小组、任务、讨论 | 后续可扩展运营 | `xiaou-team` |
| 知识图谱 | 图谱查看 | 图谱管理 | `xiaou-knowledge` |

## 内容社区

| 功能 | 用户端 | 管理端 | 后端模块 |
| --- | --- | --- | --- |
| 社区 | 发帖、评论、收藏、个人主页 | 分类、标签、帖子、评论、用户 | `xiaou-community` |
| 动态广场 | 动态、互动、收藏 | 动态、评论、统计 | `xiaou-moment` |
| 博客 | 个人博客、编辑器、文章详情 | 文章、分类、标签 | `xiaou-blog` |
| 代码工坊 | 代码广场、编辑器、作品详情 | 作品、模板、标签、统计 | `xiaou-codepen` |
| 学习资产 | 内容沉淀、发布记录 | 审核、统计 | `xiaou-learning-asset` |

## 平台能力

| 功能 | 说明 | 后端模块 |
| --- | --- | --- |
| 双端鉴权 | 管理端和用户端隔离登录域 | `xiaou-common`、`xiaou-system`、`xiaou-user` |
| AI Runtime | 模型、Prompt、Graph、RAG、Schema、回归 | `xiaou-ai` |
| 文件存储 | 上传、访问控制、存储配置、迁移 | `xiaou-filestorage` |
| 通知 | 系统通知、私信和消息状态 | `xiaou-notification` |
| IM 聊天室 | WebSocket、心跳、撤回、禁言、踢人 | `xiaou-chat` |
| 积分抽奖 | 积分账户、规则、明细、抽奖 | `xiaou-points` |
| 敏感词风控 | 词库、白名单、策略、统计 | `xiaou-sensitive` |
| 版本与摸鱼工具 | 版本墙、程序员日历、热榜、薪资计算、Bug 商店 | `xiaou-version`、`xiaou-moyu` |

## 已拆分文档页

| 分组 | 文档页 |
| --- | --- |
| 账号与安全 | [鉴权与用户体系](/modules/auth)、[用户账户与个人中心](/modules/user-account)、[敏感词风控](/modules/sensitive) |
| 学习成长 | [面试题库](/modules/interview)、[模拟面试与求职作战台](/modules/mock-interview-job-battle)、[学习资产](/modules/learning-assets)、[闪卡](/modules/flashcard)、[计划与学习小组](/modules/plan-team)、[知识图谱](/modules/knowledge)、[SQL 优化工作台](/modules/sql-optimizer) |
| 内容社区 | [社区帖子](/modules/community)、[动态广场](/modules/moments)、[博客](/modules/blog)、[代码工坊](/modules/codepen) |
| 平台运营 | [IM 聊天室](/modules/chat)、[简历系统](/modules/resume)、[积分与抽奖](/modules/points)、[文件存储](/modules/file-storage)、[通知中心](/modules/notification)、[系统运营后台](/modules/system-ops)、[仪表盘与日志](/modules/dashboard-logs) |
| 轻工具与版本 | [开发者工具](/modules/dev-tools)、[摸鱼工具](/modules/moyu)、[版本历史](/modules/version-history) |

## 验证清单

用模块总览验收文档覆盖时，可以按下面顺序扫一遍：

1. 新增功能是否能在“已拆分文档页”里找到对应模块页。
2. 模块页是否有 `推荐学习顺序`、`源码地图`、核心流程、数据表、常见坑和 `验证清单`。
3. 用户端或管理端新增页面后，是否同步更新 [前端路由索引](/reference/frontend-routes)。
4. 新增 Controller 或接口前缀后，是否同步更新 [API 分组索引](/reference/api-routes)。
5. 新增表、字段或版本 SQL 后，是否同步更新 [数据表索引](/reference/database-tables)。
6. 新增 AI 场景后，是否同步更新 [AI Runtime](/modules/ai-runtime) 和 [AI Schema 与治理](/reference/ai-schemas)。
7. 新增可截图验证的用户流程后，是否补到操作手册或验证记录。

## 文档补齐策略

每个模块后续都按同一模板补齐：

1. 功能定位。
2. 用户端入口。
3. 管理端入口。
4. 后端源码位置。
5. 数据表和关键字段。
6. 核心流程。
7. 权限和安全边界。
8. 常见问题。
9. 验证方式。
