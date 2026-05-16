# 架构总览

Code Nest 是一个面向开发者成长的多模块全栈平台。整体采用 Spring Boot 3 + Vue 3 + Vite 的前后端分离架构，并引入 AI Runtime、OJ 判题、内容社区、学习资产、IM、积分和运维观测等能力。

## 系统分层

| 层级 | 目录 | 职责 |
| --- | --- | --- |
| 用户端 | `vue3-user-front` | 面向开发者用户，承载学习、求职、社区、创作与个人中心 |
| 管理端 | `vue3-admin-front` | 面向运营和管理员，承载配置、审核、数据管理与观测 |
| API 聚合层 | `xiaou-application` | Spring Boot 启动入口，聚合所有业务模块并暴露统一 API |
| 业务模块 | `xiaou-*` | 用户、题库、社区、OJ、AI、积分等领域能力 |
| 公共能力 | `xiaou-common` | 统一返回体、异常、鉴权配置、工具类和通用配置 |
| API 契约 | `xiaou-user-api`、`xiaou-sensitive-api` | 跨模块调用接口与 DTO |
| AI sidecar | `llamaindex-service` | 知识库导入、检索、召回解释与文档管理 |
| 基础设施 | `docker/`、`sql/`、`logs/` | 部署脚本、数据库脚本、监控和运行日志 |

## 运行主线

前端请求通过 Axios 进入后端 `/api`，后端由 `xiaou-application` 聚合业务模块。鉴权由 Sa-Token 负责，用户端和管理端使用不同登录域。业务模块通过 MyBatis 访问 MySQL，通过 Redis/Redisson 承载缓存、限流、锁和实时状态。

AI 能力由 `xiaou-ai` 统一编排，模型调用、Prompt、Schema、RAG 和回归样例在同一套治理体系里沉淀。知识库检索通过 `llamaindex-service` 解耦，避免 Python 检索服务与 Java 主工程强耦合。

## 文档站定位

`docs-site/` 是 v2.2.0 新增的独立文档模块。它的职责不是替代源码注释，也不是简单搬运旧 Markdown，而是把项目能力整理成稳定的信息架构：

- 新人如何启动项目。
- 开发者如何定位模块。
- 运维如何部署和排障。
- 面试或复盘时如何讲清楚核心亮点。
- 后续新增功能时如何持续补文档。

