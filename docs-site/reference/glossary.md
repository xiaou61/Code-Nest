# 术语表

这页统一解释 Code Nest 文档里反复出现的项目术语。它适合在读模块页时随手查，不要求一次背完。

如果一个词同时出现在后端源码、前端页面和文档里，优先使用本页的中文叫法，避免同一个概念在不同页面里被写成多个名字。

## 项目结构

| 术语 | 含义 | 先看 |
| --- | --- | --- |
| 用户端 | 面向普通用户的 Vue 3 应用，目录是 `vue3-user-front` | [前端应用](/architecture/frontend-apps) |
| 管理端 | 面向管理员和运营人员的 Vue 3 应用，目录是 `vue3-admin-front` | [系统运营后台](/modules/system-ops) |
| 后端聚合应用 | Spring Boot 启动入口，目录是 `xiaou-application` | [后端模块](/architecture/backend-modules) |
| 业务模块 | 一个 `xiaou-*` Maven 子模块，例如 `xiaou-oj`、`xiaou-chat` | [源码地图](/reference/source-map) |
| API 契约模块 | 给其他模块调用的接口或 DTO 模块，常见后缀是 `-api` | [后端模块](/architecture/backend-modules) |
| 文档站 | v2.2.0 新增的 VitePress 站点，目录是 `docs-site` | [快速开始](/guide/quick-start) |
| 原始资料库 | 历史 PRD、截图、计划和归档资料，目录是 `docs` | [源码地图](/reference/source-map) |

## 账号与权限

| 术语 | 含义 | 先看 |
| --- | --- | --- |
| Sa-Token | 后端登录、会话和权限控制框架 | [鉴权与用户体系](/modules/auth) |
| 用户登录域 | 普通用户使用的登录态，与管理端隔离 | [用户账户与个人中心](/modules/user-account) |
| 管理登录域 | 管理员使用的登录态，与用户端隔离 | [鉴权与用户体系](/modules/auth) |
| 当前登录用户 | 后端从 Token 中解析出的用户 ID，不应由前端手动传入 | [用户账户与个人中心](/modules/user-account) |
| `@RequireAdmin` | 管理端权限校验注解，用于保护后台接口 | [权限注解与角色边界索引](/reference/permission-boundaries)、[系统运营后台](/modules/system-ops) |
| 逻辑删除 | 数据不物理删除，而是用字段标记不可见或失效 | [数据库与脚本](/architecture/database) |
| `StpUserUtil` | Sa-Token 用户登录域操作工具类 | [鉴权与用户体系](/modules/auth) |
| `StpAdminUtil` | Sa-Token 管理登录域操作工具类 | [鉴权与用户体系](/modules/auth) |
| `AdminAuthAspect` | 管理端权限 AOP 切面，组合 `checkLogin` + `checkRole("admin")` | [鉴权与用户体系](/modules/auth) |

## 平台能力

| 术语 | 含义 | 先看 |
| --- | --- | --- |
| 横切能力 | 多个业务模块都会复用的能力，例如鉴权、文件、通知、积分、敏感词 | [功能开发流程](/guide/feature-development) |
| 文件存储策略 | 本地、云存储等不同文件落盘方式的抽象 | [文件存储](/modules/file-storage) |
| `is_public` | 文件是否允许匿名读取的标记，不等于上传权限 | [文件存储](/modules/file-storage) |
| 通知模板 | 用变量生成站内信或公告内容的后台配置 | [通知中心](/modules/notification) |
| 积分流水 | 积分奖励、扣减、抽奖消费的可追溯记录 | [积分与抽奖](/modules/points) |
| 敏感词策略 | 对命中文本执行放行、替换、拒绝或记录的规则 | [敏感词风控](/modules/sensitive) |
| 操作日志 | 理理端关键动作的审计记录 | [仪表盘与日志](/modules/dashboard-logs) |
| `@Log` | 操作日志注解，标记需要记录的管理端方法 | [仪表盘与日志](/modules/dashboard-logs) |
| `safeCall` | 仪表盘聚合查询的兜底模式，失败时返回默认值并标记模块危险 | [仪表盘与日志](/modules/dashboard-logs) |

## AI Runtime

| 术语 | 含义 | 先看 |
| --- | --- | --- |
| AI Runtime | Code Nest 的 AI 运行底座，负责 Prompt、Schema、RAG、Graph、回归和指标 | [AI Runtime](/modules/ai-runtime) |
| Prompt | 发送给模型的提示词模板 | [AI Schema 与治理](/reference/ai-schemas) |
| Schema | 结构化输出约束，用于要求 AI 返回稳定字段 | [AI Schema 与治理](/reference/ai-schemas) |
| RAG | 检索增强生成，先查知识库，再把召回内容交给模型 | [AI Runtime](/modules/ai-runtime) |
| Graph | 多步骤 AI 编排流程，例如模拟面试、SQL 优化 | [AI Runtime](/modules/ai-runtime) |
| Eval | AI 场景回归评估，用样例检查输出是否稳定 | [AI Schema 与治理](/reference/ai-schemas) |
| 治理评分 | 从 Prompt、Schema、RAG、回归等维度衡量 AI 场景健康度 | [AI Runtime](/modules/ai-runtime) |
| RAG sidecar | 独立运行的知识库召回服务，不是 Spring Boot 主应用的一部分 | [独立部署](/guide/deploy) |
| `langchain4j` | AI 模块使用的 LLM 调用框架 | [AI Runtime](/modules/ai-runtime) |
| `langgraph4j` | AI 多步骤编排框架 | [AI Runtime](/modules/ai-runtime) |

## OJ 与实时通信

| 术语 | 含义 | 先看 |
| --- | --- | --- |
| OJ | Online Judge，在线判题系统 | [OJ 判题系统](/modules/oj) |
| go-judge | OJ 运行代码的沙箱服务，独立于主后端 | [OJ 判题系统](/modules/oj) |
| 提交状态机 | OJ 提交从等待、运行到 AC/WA/CE/TLE 等结果的流转 | [OJ 判题系统](/modules/oj) |
| ACM 排名 | 赛事中按过题数、罚时等规则计算的排名 | [OJ 判题系统](/modules/oj) |
| WebSocket | 聊天室实时通信通道 | [WebSocket 协议](/reference/websocket) |
| ws-ticket | 聊天连接前申请的一次性 WebSocket 票据 | [WebSocket 协议](/reference/websocket)、[IM 聊天室](/modules/chat) |
| ACK | 服务端确认消息已被接收或落库的事件 | [WebSocket 协议](/reference/websocket) |
| tempId | 前端乐观消息的临时 ID，用来把失败态和真实消息关联起来 | [IM 聊天室](/modules/chat) |

## 学习与内容

| 术语 | 含义 | 先看 |
| --- | --- | --- |
| 题单 | 一组面试题的集合，用于结构化刷题 | [面试题库](/modules/interview) |
| 掌握度 | 用户对题目的学习程度和复习状态 | [题库与成长闭环](/modules/interview-and-growth) |
| 学习资产 | 从面试、社区、博客等来源沉淀出的可复用学习内容 | [学习资产](/modules/learning-assets) |
| 候选资产 | 等待用户确认或管理员审核的学习资产草稿 | [学习资产](/modules/learning-assets) |
| 成长自动驾驶 | 根据目标自动生成学习任务和节奏的能力 | [计划与学习小组](/modules/plan-team) |
| SM-2 | 闪卡间隔重复算法，用评分决定下次复习时间 | [闪卡](/modules/flashcard) |
| 内容矩阵 | 社区、动态、博客、代码工坊等内容发布能力的统称 | [社区与内容矩阵](/modules/community-content) |
| 热度口径 | 浏览、点赞、评论、收藏等指标如何合成排序分数 | [社区与内容矩阵](/modules/community-content) |
| CodePen | 代码工坊模块，支持在线写代码和分享 | [代码工坊](/modules/codepen) |
| 求职作战台 | JD 解析、简历匹配和计划生成的求职辅助能力 | [模拟面试与求职作战](/modules/mock-interview-job-battle) |

## 运维与交付

| 术语 | 含义 | 先看 |
| --- | --- | --- |
| `/api` 上下文 | 后端统一上下文路径，本地前端代理也围绕它配置 | [本地开发](/guide/local-dev) |
| CORS Origin | 浏览器页面来源地址，后端和 WebSocket 都需要正确放行 | [常见问题排查](/operations/troubleshooting) |
| Actuator | Spring Boot 暴露健康检查和指标的能力 | [独立部署](/guide/deploy) |
| Prometheus | 拉取和存储监控指标的组件 | [独立部署](/guide/deploy) |
| Grafana | 展示监控面板和图表的组件 | [独立部署](/guide/deploy) |
| 发布前验证 | 合并或发布前按变更范围执行构建、测试和烟测 | [发布前验证](/guide/release-verification) |
| 验证记录 | 对真实验证结果、已知问题和补测优先级的沉淀 | [验证记录与已知问题](/manuals/verified-scenarios) |
| Spring Profile | 后端配置分组：dev、docker、prod、sec | [独立部署](/guide/deploy) |
| `application-sec.yml` | 密钥覆盖配置文件，不提交仓库，通过 `optional:import` 加载 | [鉴权与用户体系](/modules/auth) |

## 文档维护

| 术语 | 含义 | 先看 |
| --- | --- | --- |
| 模块页 | `docs-site/modules` 下按功能拆分的教程页 | [模块总览](/modules/) |
| 参考索引 | API、路由、数据表、源码地图、错误码等可查询页面 | [源码地图](/reference/source-map) |
| 操作手册 | 基于真实截图和核心链路整理的使用手册 | [核心链路教程](/manuals/core-workflows) |
| 覆盖矩阵 | 用来检查功能是否有入口、API、表和文档的总表 | [全功能覆盖矩阵](/reference/feature-coverage) |
| 文档同步 | 代码变更后同步模块页、索引、手册和验证记录 | [文档维护规范](/guide/documentation-maintenance) |
| `sync:baseline` | 文档站基线自动刷新脚本，在 dev/build/preview 前自动执行 | [文档同步基线](/reference/docs-sync-baseline) |

## 前端技术

| 术语 | 含义 | 先看 |
| --- | --- | --- |
| `renderMarkdown` | 用户端和管理端的统一 Markdown 渲染函数 | [前端渲染安全](/reference/frontend-rendering-security) |
| `sanitizeHtml` | DOMPurify 净化函数，禁止脚本、iframe 等危险标签 | [前端渲染安全](/reference/frontend-rendering-security) |
| `escapeHtml` | 纯文本 HTML 转义函数，用 `textContent` 实现 | [前端渲染安全](/reference/frontend-rendering-security) |
| `manualChunks` | Vite 代码分割策略，按依赖类型拆分 chunk | [独立部署](/guide/deploy) |
| `keepAlive` | Vue 路由缓存配置，开发工具页需要此属性 | [前端路由索引](/reference/frontend-routes) |
| `requiresAuth` | 路由元信息，标记页面是否需要登录 | [前端路由索引](/reference/frontend-routes) |

## 状态值速查

本节列出源码中定义的关键状态值。完整定义见对应模块页。

### 通用状态（StatusEnum）

| code | 名称 | 用途 |
| --- | --- | --- |
| 0 | 正常 | 通用启用状态 |
| 1 | 禁用 | 通用停用状态 |
| 2 | 已删除 | 逻辑删除 |

### 计划状态（PlanStatus）

| code | 名称 | 说明 |
| --- | --- | --- |
| 0 | 已删除 | 逻辑删除 |
| 1 | 进行中 | 用户正在执行 |
| 2 | 已暂停 | 用户主动暂停 |
| 3 | 已完成 | 所有打卡完成 |
| 4 | 已过期 | 超过截止日期 |

### 小组状态（TeamStatus）

| code | 名称 | 说明 |
| --- | --- | --- |
| 0 | 已解散 | 组长解散或到期 |
| 1 | 正常 | 可以加入 |
| 2 | 已满员 | 达到最大人数 |

### 成员角色（MemberRole）

| code | 名称 | 说明 |
| --- | --- | --- |
| 1 | 组长 | 创建者，最高权限 |
| 2 | 管理员 | 组长任命的管理角色 |
| 3 | 成员 | 普通成员 |

### OJ 提交状态（SubmissionStatus）

| value | 名称 | 说明 |
| --- | --- | --- |
| pending | 等待判题 | 提交后等待沙箱处理 |
| judging | 判题中 | go-judge 正在执行 |
| accepted | 通过 | 全部测试点通过 |
| wrong_answer | 答案错误 | 输出与期望不符 |
| time_limit_exceeded | 超时 | 执行超过时间限制 |
| memory_limit_exceeded | 超内存 | 使用超过内存限制 |
| runtime_error | 运行错误 | 执行过程中崩溃 |
| compile_error | 编译错误 | 编译阶段失败 |
| system_error | 系统错误 | 后端或沙箱异常 |

### 积分类型（PointsType）

| code | 名称 | 说明 |
| --- | --- | --- |
| 1 | 后台发放 | 管理员手动发放 |
| 2 | 打卡积分 | 每日打卡获得 |
| 3 | 抽奖消耗 | 参与抽奖扣减 |
| 4 | 抽奖奖励 | 抽奖中奖获得 |
| 5 | OJ通过 | 首次通过题目获得 |

### 动态状态（MomentStatus）

| code | 名称 | 说明 |
| --- | --- | --- |
| 0 | 已删除 | 逻辑删除 |
| 1 | 正常 | 正常可见 |
| 2 | 审核中 | 等待管理审核 |

### 闪卡掌握度（MasteryLevel）

| code | 名称 | 说明 |
| --- | --- | --- |
| 1 | 新卡 | 还未学习过 |
| 2 | 学习中 | 正在复习 |
| 3 | 已掌握 | 完全掌握 |

### 模拟面试会话状态（SessionStatusEnum）

| code | 名称 | 说明 |
| --- | --- | --- |
| 0 | 进行中 | AI 问答还在继续 |
| 1 | 已完成 | 正常结束 |
| 2 | 已中断 | 异常或手动中断 |

### 通知状态（NotificationStatusEnum）

| code | 名称 | 说明 |
| --- | --- | --- |
| UNREAD | 未读 | 新消息，未查看 |
| READ | 已读 | 用户已查看 |
| DELETED | 已删除 | 用户已删除 |

> 通知状态使用字符串 code（非整数），流转方向为 UNREAD → READ → DELETED。

### 响应状态码（ResultCode）

| code | 名称 | 说明 |
| --- | --- | --- |
| 200 | 操作成功 | 正常返回 |
| 400 | 请求参数错误 | 客户端参数校验失败 |
| 401 | 未授权访问 | Token 缺失或无效 |
| 403 | 访问被禁止 | 权限不足 |
| 404 | 资源不存在 | 请求资源未找到 |
| 405 | 请求方法不支持 | HTTP 方法不被允许 |
| 408 | 请求超时 | 客户端请求超时 |
| 409 | 数据冲突 | 请求与当前状态冲突 |
| 415 | 不支持的媒体类型 | Content-Type 不匹配 |
| 429 | 请求过于频繁 | 触发限流 |
| 500 | 系统内部错误 | 后端异常 |
| 503 | 服务不可用 | 服务暂时不可用 |
| 600 | 业务处理失败 | 通用业务错误 |
| 601 | 参数校验失败 | 业务参数不合法 |
| 602 | 数据不存在 | 查询的目标数据缺失 |
| 603 | 数据已存在 | 重复创建 |
| 604 | 操作不被允许 | 当前状态不允许此操作 |
| 701 | Token无效 | Sa-Token 校验失败 |
| 702 | Token已过期 | 超过有效期 |
| 703 | 权限不足 | 角色或权限不满足 |
| 704 | 账户已被禁用 | 账户被管理员禁用 |
| 705 | 登录失败 | 用户名或密码错误 |
| 801 | 文件上传失败 | 文件模块异常 |
| 802 | 文件下载失败 | 文件模块异常 |
| 803 | 文件不存在 | 请求的文件未找到 |
| 804 | 文件类型不支持 | 上传文件格式不被允许 |
| 805 | 文件大小超出限制 | 上传文件超过大小限制 |

### 学习资产候选状态（LearningAssetCandidateStatus）

| value | 名称 | 说明 |
| --- | --- | --- |
| DRAFT | 待确认 | 用户还未决定是否保留 |
| SELECTED | 已选中 | 用户确认保留 |
| REVIEWING | 审核中 | 等待管理员审核 |
| PUBLISHED | 已发布 | 审核通过并发布 |
| DISCARDED | 已丢弃 | 用户主动丢弃 |
| REJECTED | 已驳回 | 管理员驳回 |

## 缩写对照

| 缩写 | 全称 | 用途 |
| --- | --- | --- |
| DTO | Data Transfer Object | 接层间数据传输对象，见各模块 `dto/` 目录 |
| VO | Value Object | 视图对象，用于前端展示响应 |
| PO | Persistent Object | 持久化对象，对应数据库表，见 `domain/` 目录 |
| QO | Query Object | 查询参数对象，封装分页、筛选条件 |
| BO | Business Object | 业务逻辑层对象 |
| Mapper | MyBatis Mapper | 数据访问层，见 `mapper/` 目录和 XML 映射文件 |
| Service | 业务服务层 | 核心业务逻辑，见 `service/impl/` 目录 |
| Controller | 控制器层 | HTTP 入口，见 `controller/` 目录 |
| AOP | Aspect-Oriented Programming | 切面编程，`AdminAuthAspect` 和 `@Log` 是例子 |


## 相关文档

| 文档 | 说明 |
| --- | --- |
| [模块总览](/modules/) | 各模块功能说明 |
| [架构总览](/architecture/overview) | 技术栈说明 |
| [数据库字段阅读指南](/reference/database-field-guide) | 字段命名规范 |
| [源码地图](/reference/source-map) | 代码位置索引 |
