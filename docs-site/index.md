---
layout: home

hero:
  name: Code Nest
  text: v2.3.0 项目文档
  tagline: 面向开发者成长社区的全量开发与运维手册 — 后端 15 个模块、前端双端、文档站独立部署。
  image:
    src: /code-nest-mark.svg
    alt: Code Nest
  actions:
    - theme: brand
      text: 快速开始
      link: /guide/quick-start
    - theme: alt
      text: 架构总览
      link: /architecture/overview
    - theme: alt
      text: 查看模块
      link: /modules/

features:
  - title: 学习与成长
    details: 面试题库 + 掌握度追踪、计划打卡、学习小组（排行/贡献/讨论）、闪卡、知识图谱、成长自动驾驶
  - title: OJ 判题
    details: 多语言支持、go-judge 沙箱隔离、ACM/IOI 赛制、竞赛排名、首次 AC 积分奖励
  - title: AI Runtime
    details: 结构化 AI 对话、RAG 知识库增强、多模型切换、Schema 校验回归、Token 用量与成本监控
  - title: 内容与创作
    details: Markdown 博客、社区帖子、动态广场、代码工坊（Fork/收益/模板）、简历系统
  - title: 积分与抽奖
    details: 多来源积分发放与流水、概率可调抽奖、库存与限制规则、熔断降级、风险检测与黑名单
  - title: 平台运营
    details: Sa-Token 双登录域隔离、@RequireAdmin 权限、操作/登录日志审计、管理端仪表盘、文件存储、敏感词风控
---

## 项目技术栈

| 层 | 技术 | 版本 |
| --- | --- | --- |
| 后端框架 | Spring Boot | 3.4.4 |
| 运行时 | JDK | 17 |
| ORM | MyBatis-Plus | 3.5.x |
| 鉴权 | Sa-Token | 1.39.x |
| 缓存 | Redis 7.x + Redisson | — |
| 数据库 | MySQL | 8.0+ |
| 前端 | Vue 3 + Vite + Element Plus | — |
| 文档站 | VitePress 2.x | — |
| 判题沙箱 | go-judge | 独立 Docker 容器 |
| 监控 | Prometheus + Grafana | Docker Compose |

## 文档导航

| 你想做什么 | 去哪里 |
| --- | --- |
| 从零搭建本地开发环境 | [快速开始](/guide/quick-start) |
| 了解整体架构和模块划分 | [架构总览](/architecture/overview) |
| 按模块深入学习 | [模块总览](/modules/) |
| 理解双登录域和权限模型 | [鉴权与用户体系](/modules/auth) |
| 查看 API 接口清单 | [API 路由索引](/reference/api-routes) |
| 查看数据库表结构 | [数据表索引](/reference/database-tables) |
| 排查线上或本地问题 | [问题定位流程](/operations/diagnosis-flow) |
| 处理线上事故 | [事故响应](/operations/incident-response) |
| 理解安全边界 | [权限与安全边界](/guide/security-boundaries) |
| 查看环境变量清单 | [环境变量](/operations/env-vars) |
| 查看用户端操作接口 | [用户端操作手册](/manuals/user-operations) |
| 查看管理端操作接口 | [管理端操作手册](/manuals/admin-operations) |
| 走一遍核心业务链路 | [核心链路教程](/manuals/core-workflows) |
| 部署到生产环境 | [独立部署](/guide/deploy) |
| 开发一个新功能 | [功能开发流程](/guide/feature-development) |

## 关键端口速查

| 组件 | 端口 | 说明 |
| --- | --- | --- |
| 后端 API | 9999 | `server.port`，context-path `/api` |
| 用户端 dev | 3001 | `vue3-user-front` 开发服务器 |
| 管理端 dev | 3000 | `vue3-admin-front` 开发服务器 |
| 文档站 dev | 5175 | `docs-site` VitePress 开发服务器 |
| MySQL | 3306 | 数据库 `code_nest`，142 张表 |
| Redis | 6379 | db3 业务缓存 + db4 Sa-Token 会话 |
| go-judge | 5050 | OJ 判题沙箱（Docker 容器） |
| RAG Sidecar | 18080 | AI 知识库检索（可选） |

## 使用检查

第一次打开文档站时，建议先确认以下 5 个入口能正常使用：

1. [快速开始](/guide/quick-start) — 能启动并构建文档站。
2. [模块总览](/modules/) — 能看到所有业务模块并点击进入。
3. [API 路由索引](/reference/api-routes) — 能找到核心模块的接口清单。
4. [问题定位流程](/operations/diagnosis-flow) — 遇到问题能找到排查入口。
5. [文档同步基线](/reference/docs-sync-baseline) — 能看出当前文档站同步到了哪个 commit。
