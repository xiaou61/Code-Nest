# SQL 优化工作台

SQL 优化工作台把 SQL 分析、重写、批量分析、优化前后对比、案例库和历史记录整合到用户端，并通过 `xiaou-ai` 调用 AI SQL 优化能力。它是学习“AI 能力如何落到业务工作台”的好例子。

## 功能入口

| 端 | 页面 | 后端模块 |
| --- | --- | --- |
| 用户端 | `/sql-optimizer/workbench` | `xiaou-sql-optimizer`、`xiaou-ai` |

源码位置：

| 层级 | 文件或目录 |
| --- | --- |
| 前端页面 | `vue3-user-front/src/views/sql-optimizer/Workbench.vue` |
| 前端 API | `vue3-user-front/src/api/sqlOptimizer.js` |
| Controller | `xiaou-sql-optimizer/src/main/java/com/xiaou/sqloptimizer/controller/SqlOptimizerController.java` |
| Service | `xiaou-sql-optimizer/src/main/java/com/xiaou/sqloptimizer/service/impl/SqlOptimizerServiceImpl.java` |
| AI 服务接口 | `xiaou-ai` 中的 `AiSqlOptimizeService` |
| 记录实体 | `xiaou-sql-optimizer/src/main/java/com/xiaou/sqloptimizer/domain/SqlOptimizeRecord.java` |
| SQL 脚本 | `sql/v1.8.2/sql_optimize_record.sql` |

注意：`sql_optimize_record` 不在主库基线 `sql/MySql/code_nest.sql`，而是在 `sql/v1.8.2/sql_optimize_record.sql`。

## 接口分组

| 接口 | 方法 | 说明 |
| --- | --- | --- |
| `/user/sql-optimizer/analyze` | `POST` | 旧版或基础 SQL 分析 |
| `/user/sql-optimizer/workbench/analyze` | `POST` | 工作台 v2 分析 |
| `/user/sql-optimizer/workbench/rewrite` | `POST` | 分析并重写 SQL |
| `/user/sql-optimizer/workbench/batch-analyze` | `POST` | 批量分析 |
| `/user/sql-optimizer/workbench/compare` | `POST` | 优化前后对比 |
| `/user/sql-optimizer/workbench/cases` | `GET` | 案例列表 |
| `/user/sql-optimizer/workbench/cases/{id}` | `GET` | 案例详情 |
| `/user/sql-optimizer/history` | `GET` | 历史记录 |
| `/user/sql-optimizer/{id}` | `GET` | 历史详情 |
| `/user/sql-optimizer/favorite/{id}` | `POST` | 收藏或取消收藏 |
| `/user/sql-optimizer/history/{id}` | `DELETE` | 删除历史记录 |

所有用户记录都按 `userId` 隔离，详情、收藏和删除都会先校验记录归属。

## 输入模型

一次 SQL 分析通常需要：

| 字段 | 说明 |
| --- | --- |
| `sql` | 原始 SQL |
| `explainResult` | EXPLAIN 结果 |
| `explainFormat` | `TABLE` 或 `JSON` |
| `tableStructures` | 表结构数组 |
| `mysqlVersion` | MySQL 版本，默认可按 8.0 理解 |

SQL 文本、EXPLAIN 和表结构都属于用户输入。展示时必须转义，保存时必须按用户隔离，传给 AI 前也要做好长度和空值校验。

## 工作台 v2 流程

### 分析

`analyzeWorkbench` 的流程：

1. 把 `tableStructures` 转成 JSON 字符串。
2. 调用 `AiSqlOptimizeService#analyzeSqlV2`。
3. 组装 `SqlWorkbenchRecordPayload`，其中包含 `workflowVersion = v2`、`fallback` 和 `analysis`。
4. 保存到 `sql_optimize_record.analysis_result`。
5. 返回 `recordId`、`workflowVersion`、`fallback` 和分析结果。

### 重写

`rewriteWorkbench` 的流程：

1. 调用 `AiSqlOptimizeService#analyzeAndRewriteSqlV2`。
2. 得到分析结果和重写结果。
3. 如果重写结果包含优化 SQL，则回填到分析结果。
4. 如果重写结果包含索引 DDL，则追加为 `ADD_INDEX` 建议。
5. 保存带 `analysis + rewrite` 的 v2 payload。

### 对比

`compareWorkbench` 的流程：

1. 调用 `AiSqlOptimizeService#compareSqlV2`。
2. 如果请求里传入 `recordId`，把 compare 结果追加到已有记录的 v2 payload。
3. 返回 `workflowVersion = v2`、`fallback` 和对比结果。

## 历史记录和案例库

普通历史记录返回 `SqlOptimizeRecord` 原始数据。案例库会解析 `analysis_result`，把它整理成适合工作台展示的摘要。

案例摘要字段：

| 字段 | 说明 |
| --- | --- |
| `score` | 性能评分 |
| `workflowVersion` | `v1` 或 `v2` |
| `fallback` | AI 是否走了兜底 |
| `hasRewrite` | 是否包含重写结果 |
| `hasCompare` | 是否包含对比结果 |
| `favorite` | 是否收藏 |
| `problemCount` | 问题数量 |
| `highestSeverity` | 最高严重程度 |
| `originalSqlPreview` | SQL 预览，最长 120 字符 |
| `createTime` | 创建时间 |

案例库支持过滤：

| 参数 | 说明 |
| --- | --- |
| `favorite` | 是否收藏 |
| `hasRewrite` | 是否有重写结果 |
| `hasCompare` | 是否有对比结果 |
| `highestSeverity` | `HIGH`、`MEDIUM`、`LOW` |
| `sortBy` | `createTime`、`score`、`severity` |
| `sortOrder` | `asc` 或 `desc` |

分页大小最大限制为 50。

## 批量分析

批量分析最多支持 20 条 SQL。服务会逐条调用工作台分析流程，并统计：

| 字段 | 说明 |
| --- | --- |
| `successCount` | 成功数量 |
| `fallbackCount` | AI 兜底数量 |
| `failedCount` | 失败数量 |
| `items` | 每条 SQL 的结果 |

批量模式适合“先筛出高风险 SQL”，不适合一次性塞入几百条 SQL。大批量应该做后台任务、队列和进度查询。

## 记录表

`sql_optimize_record` 字段：

| 字段 | 说明 |
| --- | --- |
| `user_id` | 用户 ID |
| `original_sql` | 原始 SQL |
| `explain_result` | EXPLAIN 结果 |
| `explain_format` | EXPLAIN 格式 |
| `table_structures` | 表结构 JSON |
| `mysql_version` | MySQL 版本 |
| `analysis_result` | 分析结果 JSON，v2 时保存工作台 payload |
| `score` | 性能评分，0 到 100 |
| `is_favorite` | 是否收藏 |
| `deleted` | 逻辑删除 |

索引包括 `user_id`、`create_time` 和 `score`，适合按用户查历史、按时间排序和按评分筛选。

## v1 和 v2 兼容

`parseRecordPayload` 会兼容旧记录：

| 记录格式 | 处理方式 |
| --- | --- |
| 旧版 `SqlAnalyzeResult` JSON | 转成 `workflowVersion = v1` 的 payload |
| 新版工作台 payload | 读取 `analysis`、`rewrite`、`compare` |
| JSON 解析失败 | 按 v1 兜底，`fallback = true` |

这个兼容层很重要，因为它允许升级工作台时保留旧历史记录。

## 安全边界

| 风险 | 建议 |
| --- | --- |
| SQL 文本含敏感数据 | 前端提示不要粘贴生产密钥、手机号、身份证等 |
| HTML 注入 | 展示 SQL、EXPLAIN、AI 输出时转义 |
| 越权查看记录 | 所有详情、收藏、删除都校验 `userId` |
| AI 输出不稳定 | 使用结构化 DTO 和 fallback 标记 |
| 批量滥用 | 限制最多 20 条，后续可加积分或频率限制 |
| DDL 建议误执行 | 文档和 UI 都要提示先评估，再在测试环境验证 |

## 验证清单

- 工作台分析后会生成一条 `sql_optimize_record`。
- 重写接口返回 `rewrite`，并把优化 SQL/索引建议合并到分析建议里。
- 对比接口传入 `recordId` 时，历史详情能看到 compare 结果。
- 批量分析超过 20 条会被拒绝。
- 只能提现当前用户自己的历史记录、案例、收藏和删除。
- 收藏接口重复调用会在收藏和取消收藏之间切换。
- 旧版历史 JSON 能被案例库解析为 v1 payload。
- 解析失败的记录不会导致案例库崩溃，而是 fallback。

## 常见坑

| 问题 | 原因 | 处理 |
| --- | --- | --- |
| 建表后历史为空 | 只执行了主 SQL，没有执行 v1.8.2 脚本 | 执行 `sql/v1.8.2/sql_optimize_record.sql` |
| 案例筛选不准 | `analysis_result` 不是工作台 payload 或严重程度不规范 | 检查 v1/v2 解析和 severity 归一化 |
| 对比结果没有保存 | `compare` 请求没有传 `recordId` | 前端把分析记录 ID 带上 |
| 批量分析失败 | 超过 20 条或某条输入为空 | 分批提交并做前端校验 |
| AI 建议无法直接落地 | DDL 或重写 SQL 未经过执行计划验证 | 在测试库执行 EXPLAIN 对比 |
