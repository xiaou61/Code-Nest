# 知识图谱

知识图谱用于把技术知识组织成可视化结构，帮助用户从孤立内容进入体系化学习。

## 功能入口

| 端 | 页面 |
| --- | --- |
| 用户端 | `/knowledge`、`/knowledge/maps/:id` |
| 管理端 | `/knowledge/maps`、`/knowledge/maps/:id/edit` |
| 后端模块 | `xiaou-knowledge` |

## 用户侧接口

| 接口域 | 能力 |
| --- | --- |
| `/pub/knowledge/maps` | 图谱列表、详情、节点列表、节点搜索、浏览记录 |

## 管理侧接口

| 接口域 | 能力 |
| --- | --- |
| `/admin/knowledge/maps` | 图谱列表、详情、新增、更新、发布、隐藏、删除、批量删除 |
| `/admin/knowledge/maps/{mapId}/nodes` | 节点列表和新增 |
| `/admin/knowledge/nodes/{id}` | 节点详情、更新、删除 |
| `/admin/knowledge/maps/{mapId}/nodes/sort` | 节点排序 |
| `/admin/knowledge/maps/{mapId}/nodes/search` | 节点搜索 |

## 核心模型

知识图谱至少包含：

- 图谱基础信息。
- 节点。
- 节点层级或关系。
- 排序。
- 发布状态。
- 浏览统计。

## 与学习资产的关系

学习资产模块可以生成知识节点候选。管理端审核后，候选知识点可以进入指定图谱，形成“内容生产到知识网络”的闭环。

## 文档深化点

- 图谱节点字段。
- 节点关系表达方式。
- 前端 G6 渲染数据结构。
- 发布、隐藏和删除的状态约束。

