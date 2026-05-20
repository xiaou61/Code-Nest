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
| `@RequireAdmin` | 管理端权限校验注解，用于保护后台接口 | [系统运营后台](/modules/system-ops) |
| 逻辑删除 | 数据不物理删除，而是用字段标记不可见或失效 | [数据库与脚本](/architecture/database) |

## 平台能力

| 术语 | 含义 | 先看 |
| --- | --- | --- |
| 横切能力 | 多个业务模块都会复用的能力，例如鉴权、文件、通知、积分、敏感词 | [功能开发流程](/guide/feature-development) |
| 文件存储策略 | 本地、云存储等不同文件落盘方式的抽象 | [文件存储](/modules/file-storage) |
| `is_public` | 文件是否允许匿名读取的标记，不等于上传权限 | [文件存储](/modules/file-storage) |
| 通知模板 | 用变量生成站内信或公告内容的后台配置 | [通知中心](/modules/notification) |
| 积分流水 | 积分奖励、扣减、抽奖消费的可追溯记录 | [积分与抽奖](/modules/points) |
| 敏感词策略 | 对命中文本执行放行、替换、拒绝或记录的规则 | [敏感词风控](/modules/sensitive) |
| 操作日志 | 管理端关键动作的审计记录 | [仪表盘与日志](/modules/dashboard-logs) |

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
| RAG sidecar | 独立运行的知识库召回服务，不是 Spring Boot 主应用的一部分 | [Docker 与服务部署](/operations/docker) |

## OJ 与实时通信

| 术语 | 含义 | 先看 |
| --- | --- | --- |
| OJ | Online Judge，在线判题系统 | [OJ 判题系统](/modules/oj) |
| go-judge | OJ 运行代码的沙箱服务，独立于主后端 | [OJ 判题系统](/modules/oj) |
| 提交状态机 | OJ 提交从等待、运行到 AC/WA/CE/TLE 等结果的流转 | [OJ 判题系统](/modules/oj) |
| ACM 排名 | 赛事中按过题数、罚时等规则计算的排名 | [OJ 判题系统](/modules/oj) |
| WebSocket | 聊天室实时通信通道 | [WebSocket 协议](/reference/websocket) |
| ws-ticket | 聊天连接前申请的一次性 WebSocket 票据 | [IM 聊天室](/modules/chat) |
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

## 运维与交付

| 术语 | 含义 | 先看 |
| --- | --- | --- |
| `/api` 上下文 | 后端统一上下文路径，本地前端代理也围绕它配置 | [本地开发](/guide/local-dev) |
| CORS Origin | 浏览器页面来源地址，后端和 WebSocket 都需要正确放行 | [常见问题排查](/operations/troubleshooting) |
| Actuator | Spring Boot 暴露健康检查和指标的能力 | [监控与观测](/operations/monitoring) |
| Prometheus | 拉取和存储监控指标的组件 | [监控与观测](/operations/monitoring) |
| Grafana | 展示监控面板和图表的组件 | [监控与观测](/operations/monitoring) |
| 发布前验证 | 合并或发布前按变更范围执行构建、测试和烟测 | [发布前验证](/guide/release-verification) |
| 验证记录 | 对真实验证结果、已知问题和补测优先级的沉淀 | [验证记录与已知问题](/manuals/verified-scenarios) |

## 文档维护

| 术语 | 含义 | 先看 |
| --- | --- | --- |
| 模块页 | `docs-site/modules` 下按功能拆分的教程页 | [模块总览](/modules/) |
| 参考索引 | API、路由、数据表、源码地图、错误码等可查询页面 | [源码地图](/reference/source-map) |
| 操作手册 | 基于真实截图和核心链路整理的使用手册 | [核心链路教程](/manuals/core-workflows) |
| 覆盖矩阵 | 用来检查功能是否有入口、API、表和文档的总表 | [全功能覆盖矩阵](/reference/feature-coverage) |
| 文档同步 | 代码变更后同步模块页、索引、手册和验证记录 | [文档维护规范](/guide/documentation-maintenance) |
