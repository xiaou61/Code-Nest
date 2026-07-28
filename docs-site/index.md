---
layout: home

hero:
  name: Code Nest
  text: v2.4.3 工程文档中心
  tagline: 按角色、任务和系统边界组织的开发、接口、操作与运维手册。
  image:
    src: /code-nest-mark.svg
    alt: Code Nest 标志
  actions:
    - theme: brand
      text: 开始使用
      link: /guide/
    - theme: alt
      text: 系统架构
      link: /architecture/
    - theme: alt
      text: 运维入口
      link: /operations/

features:
  - title: 开始与开发
    details: 环境搭建、学习路线、模块接手、功能开发、测试与发布流程。
    link: /guide/
  - title: 系统架构
    details: 单体模块化边界、28 个 Maven 子模块、双前端和数据演进方式。
    link: /architecture/
  - title: 功能模块
    details: 按学习成长、内容社交、平台能力和工具运营定位源码与影响面。
    link: /modules/
  - title: API
    details: 用户端与管理端接口分组、认证方式、协议和错误语义。
    link: /api/
  - title: 操作手册
    details: 面向用户端、管理端和端到端业务链路的实际操作说明。
    link: /manuals/
  - title: 运维与参考
    details: 部署配置、监控告警、事故响应、源码索引和质量矩阵。
    link: /operations/
---

## 按角色进入

| 你是谁 | 第一入口 | 接下来 |
| --- | --- | --- |
| 第一次运行项目 | [开始与开发指南](/guide/) | [本地完整启动剧本](/guide/startup-playbook) |
| 接手一个模块 | [模块总览](/modules/) | [模块接手检查清单](/guide/module-takeover-checklist) |
| 开发或联调接口 | [API 导航](/api/) | [响应体与错误码](/reference/response-errors) |
| 使用用户端或管理端 | [操作手册](/manuals/) | [核心链路教程](/manuals/core-workflows) |
| 部署或处理线上问题 | [部署与运维导航](/operations/) | [问题定位流程](/operations/diagnosis-flow) |
| 查代码、表或配置 | [参考索引](/reference/) | [源码地图](/reference/source-map) |

## 当前系统基线

| 维度 | 当前值 | 权威来源 |
| --- | --- | --- |
| 后端 | Spring Boot 3.4.4、JDK 17、28 个 Maven 子模块 | 根 `pom.xml` |
| 数据库 | MySQL 8.0+、主库基线 161 张表 | `sql/MySql/code_nest.sql` |
| 缓存与会话 | Redis 7.x + Redisson，业务缓存和 Sa-Token 分库 | 应用配置 |
| 前端 | Vue 3 用户端、管理端、共享设计系统、Electron 构建 | 双前端工程 |
| AI | LangChain4j + LangGraph4j + LlamaIndex Sidecar | `xiaou-ai`、`llamaindex-service` |
| 文档 | VitePress 2.x，120+ Markdown 页面 | `docs-site` |

实时分支、提交和统计见 [文档同步基线](/reference/docs-sync-baseline)。

## 常用任务

| 任务 | 文档 |
| --- | --- |
| 启动后端和双前端 | [快速开始](/guide/quick-start) |
| 理解请求如何穿过系统 | [整体架构](/architecture/overview) |
| 找到某个业务模块的源码入口 | [模块总览](/modules/) |
| 查看接口前缀和认证域 | [API 导航](/api/) |
| 查数据库表和字段 | [数据表索引](/reference/database-tables) |
| 修改环境变量和密钥 | [环境变量总表](/operations/env-vars) |
| 选择最低回归范围 | [模块最小回归矩阵](/reference/module-regression-matrix) |
| 发布前做统一验证 | [发布前验证](/guide/release-verification) |

## 关键端口

| 组件 | 默认端口 | 说明 |
| --- | --- | --- |
| 后端 API | 9999 | context-path `/api` |
| 用户端开发服务 | 3001 | `vue3-user-front` |
| 管理端开发服务 | 3000 | `vue3-admin-front` |
| 文档站开发服务 | 5175 | `docs-site` |
| MySQL | 3306 | 数据库 `code_nest` |
| Redis | 6379 | 业务缓存与会话分库 |
| go-judge | 5050 | OJ 判题沙箱 |
| RAG Sidecar | 18080 | 可选知识库服务 |

## 文档边界

- `docs-site`：当前代码对应的公开工程文档，是开发和运维的默认入口。
- `AI-DOCS`：PRD、技术专题、测试材料和历史归档，不替代现行模块/API 文档。
- `README.md`：项目简介和最短启动入口，不承载完整技术细节。
