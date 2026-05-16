# SQL 优化工作台

SQL 优化工作台把 SQL 分析、重写、批量分析、对比、案例库和历史记录整合到用户端，并接入 AI Runtime。

## 功能入口

| 端 | 页面 |
| --- | --- |
| 用户端 | `/sql-optimizer/workbench` |
| 后端模块 | `xiaou-sql-optimizer`、`xiaou-ai` |

## 接口域

| 接口 | 能力 |
| --- | --- |
| `/user/sql-optimizer/analyze` | 旧版或基础 SQL 分析 |
| `/user/sql-optimizer/workbench/analyze` | 工作台分析 |
| `/user/sql-optimizer/workbench/rewrite` | SQL 重写 |
| `/user/sql-optimizer/workbench/batch-analyze` | 批量分析 |
| `/user/sql-optimizer/workbench/compare` | 优化前后对比 |
| `/user/sql-optimizer/workbench/cases` | 案例列表 |
| `/user/sql-optimizer/workbench/cases/{id}` | 案例详情 |
| `/user/sql-optimizer/history` | 历史记录 |
| `/user/sql-optimizer/{id}` | 记录详情 |
| `/user/sql-optimizer/favorite/{id}` | 收藏记录 |
| `/user/sql-optimizer/history/{id}` | 删除记录 |

## AI 场景

SQL 优化相关 AI 场景包括：

- SQL 分析。
- SQL 重写。
- SQL 分析 + 重写组合。
- 优化前后比较。
- 结合知识库的解释增强。

## 开发注意

- SQL 文本属于用户输入，展示时必须转义。
- AI 输出要经过结构化校验和失败兜底。
- 历史记录要按用户隔离。
- 收藏、删除和详情访问要校验归属。

