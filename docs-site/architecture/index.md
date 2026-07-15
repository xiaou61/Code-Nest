# 架构导航

架构文档回答四个问题：系统如何部署、后端如何拆分、前端如何组织、数据如何演进。模块内部细节放在 [模块总览](/modules/)，接口明细放在 [API 导航](/api/)。

## 架构阅读顺序

1. [整体架构](/architecture/overview)：理解单体模块化边界、运行形态和基础设施。
2. [后端模块](/architecture/backend-modules)：查看 28 个 Maven 子模块的职责与依赖。
3. [前端应用](/architecture/frontend-apps)：理解用户端、管理端、共享设计系统和 Electron 构建。
4. [数据库与脚本](/architecture/database)：理解主库基线、版本脚本和迁移原则。

## 按问题定位

| 问题 | 先看 | 再看 |
| --- | --- | --- |
| 一个请求经过哪些层 | [整体架构](/architecture/overview) | [源码地图](/reference/source-map) |
| 修改公共模块会影响谁 | [后端模块](/architecture/backend-modules) | [模块依赖地图](/reference/module-dependencies) |
| 双前端如何共享组件 | [前端应用](/architecture/frontend-apps) | [前端路由索引](/reference/frontend-routes) |
| 新表应该放在哪里 | [数据库与脚本](/architecture/database) | [数据表索引](/reference/database-tables) |
| AI、WebSocket 或缓存的边界 | [整体架构](/architecture/overview) | 对应模块页和协议参考 |

## 架构约束

- 后端保持单体模块化部署，由 `xiaou-application` 聚合启动。
- 模块间通过明确依赖和 API 契约协作，避免循环依赖。
- 用户端与管理端共享设计系统，但保留独立路由、权限和构建产物。
- 数据库以 `sql/MySql/code_nest.sql` 为当前基线，增量脚本用于版本演进。
- 基础设施失败策略由调用方明确决定，不由通用工具静默吞错。
