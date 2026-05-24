# SQL 优化工作台

SQL 优化工作台把 SQL 分析、重写、批量分析、优化前后对比、案例库和历史记录整合到用户端，并通过 `xiaou-ai` 调用 AI SQL 优化能力。它是学习“AI 能力如何落到业务工作台”的好例子。

## 功能入口

| 端 | 页面 | 后端模块 |
| --- | --- | --- |
| 用户端 | `/sql-optimizer/workbench` | `xiaou-sql-optimizer`、`xiaou-ai` |

## 推荐学习顺序

SQL 优化工作台适合拿来学习“AI 场景如何落成产品功能”。它不是只把 SQL 发给模型，而是把输入、AI 分析、历史记录、案例库和用户隔离都串起来。

1. 先看前端工作台，理解用户要填写 SQL、EXPLAIN、表结构和 MySQL 版本。
2. 再看 `SqlOptimizerController`，知道哪些接口是 v2 工作台能力，哪些是历史兼容能力。
3. 接着看 `SqlOptimizerServiceImpl`，把分析、重写、批量分析、对比和记录保存串起来。
4. 然后看 `xiaou-ai` 里的 SQL Prompt、Schema 和 Graph Runner，理解 AI 返回如何变成结构化结果。
5. 最后看案例库和历史记录，确认用户只能看到自己的记录，旧记录也能被兼容解析。

最小学习案例可以是一条慢查询：先填 SQL 和 EXPLAIN 做分析，再点重写，最后用 compare 把优化前后结果挂回同一条记录。

## 源码地图

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


---

## 深度拆解

### 一、工作台分析流程深度分析

`SqlOptimizerServiceImpl.analyzeWorkbenchInternal` 完整流程：

```text
1. JSONUtil.toJsonStr(request.getTableStructures()) → 表结构JSON
2. if withRewrite:
   ├─ aiSqlOptimizeService.analyzeAndRewriteSqlV2 → SqlAnalyzeRewriteResult
   ├─ result = bundle.getAnalysis()
   ├─ rewrite = bundle.getRewrite()
   └─ applyRewriteToAnalyzeResult(result, rewrite)
   else:
   └─ aiSqlOptimizeService.analyzeSqlV2 → SqlAnalyzeResult
3. fallback = result.isFallback() || (rewrite != null && rewrite.isFallback())
4. 构建 SqlWorkbenchRecordPayload:
   ├─ workflowVersion = "v2"
   ├─ fallback = fallback
   ├─ analysis = result
   ├─ rewrite = rewrite (或 null)
   └─ compare = null
5. saveRecord(userId, request, tableStructuresJson, payloadJson, score)
6. return SqlWorkbenchAnalyzeResponse(recordId, workflowVersion, fallback, analysis, rewrite)
```

**v1 和 v2 分析共用同一个 saveRecord**：`saveRecord` 方法不管 v1 还是 v2，都把 analysisResult 存为 JSON 字符串。v1 存的是 `SqlAnalyzeResult` JSON，v2 存的是 `SqlWorkbenchRecordPayload` JSON。两者格式完全不同，但放在同一个字段 `analysis_result` 里——**解析时靠 `parseRecordPayload` 做兼容**。

**重写建议回填到分析结果**：`applyRewriteToAnalyzeResult` 会把 `rewrite.optimizedSql` 设到 `analysis.optimizedSql`，同时把重写原因和索引 DDL 作为 `REWRITE_SQL` 和 `ADD_INDEX` 建议追加到 `analysis.suggestions`。这意味着**分析结果被重写结果修改了**——保存的 analysis 字段既包含纯分析，也包含重写建议，两者混合在一起。

### 二、对比结果追加深度分析

`appendCompareToRecord`：

```text
1. getById(userId, recordId) → 校验归属
2. parseRecordPayload(record.analysisResult) → 解析已有payload
3. payload.compare = compare → 追加对比结果
4. payload.fallback = 原fallback || compare.isFallback() → 合并fallback标记
5. recordMapper.updateAnalysisResult(recordId, userId, JSONUtil.toJsonStr(payload))
```

**对比结果追加而非替换**：`appendCompareToRecord` 只更新 `payload.compare` 字段和 `fallback` 标记，不修改 `analysis` 和 `rewrite`。这意味着一条记录可以经历 分析→重写→对比 的完整链路，最终 payload 包含三者。

**如果 recordId 为空，对比结果不保存**：`compareWorkbench` 中 `request.getRecordId() != null` 才调用 `appendCompareToRecord`。如果前端不传 recordId，对比结果只返回给前端，**不写入数据库**——下次查看案例详情时没有对比数据。

### 三、v1/v2 兼容解析深度分析

`parseRecordPayload`：

```text
1. analysisResult 为空 → 返回 v1 payload + fallback=true + analysis=null
2. 解析 JSON:
   ├─ 判断是否工作台payload: 包含"analysis"/"rewrite"/"compare"/"workflowVersion"
   ├─ 非工作台payload → 按v1解析为SqlAnalyzeResult
   └─ 工作台payload → 按v2解析各字段
3. fallback合并逻辑:
   ├─ root.fallback || analysis.isFallback() || rewrite.isFallback() || compare.isFallback()
   └─ 任一环节fallback则整体标记
4. JSON解析失败 → 二次尝试按v1解析
5. 二次解析也失败 → 返回 v1 payload + fallback=true + analysis=null
```

**兼容层双次解析**：如果 JSON 不是合法的工作台 payload，先尝试当作 `SqlAnalyzeResult` 解析。如果整个 JSON 解析失败，再尝试一次 `SqlAnalyzeResult` 解析。两次都失败则返回 fallback payload。

**isWorkbenchPayload 判断不够精确**：只要 JSON 包含 `"analysis"`、`"rewrite"`、`"compare"` 或 `"workflowVersion"` 任何一个 key，就认为是工作台 payload。但如果一个 v1 的 `SqlAnalyzeResult` 恰好有一个 `"analysis"` 属性（虽然 `SqlAnalyzeResult` 本身没有这个属性），会被误判为 v2——**实际上 `SqlAnalyzeResult` 没有 `analysis` 字段，误判概率低**。

### 四、案例库全量内存分页深度分析

`getWorkbenchCases`：

```text
1. pageNum = max(pageNum, 1), pageSize = max(min(pageSize, 50), 1)
2. selectAllByUserId(userId) → 查出该用户的所有记录（无分页）
3. 逐个 toCaseSummary → 每条记录都调用parseRecordPayload解析analysis_result
4. 内存过滤: favorite/hasRewrite/hasCompare/highestSeverity
5. 内存排序: sortBy(createTime/score/severity) + sortOrder
6. 内存分页: subList(fromIndex, toIndex)
7. return PageResult
```

**全量内存分页是最大性能问题**：`selectAllByUserId` 不分页，查出用户所有记录。每条记录的 `analysis_result` 可能很大（包含完整分析结果 JSON），全部加载到内存。如果一个用户有数百条记录，每次查案例库都要：
- 查询数百行数据
- 解析数百个 JSON
- 在内存中做过滤和排序

**parseRecordPayload 调用 N 次**：每条记录都调用 `parseRecordPayload`，其中包含 `JSONUtil.parseObj` 和 `toBean`。对于 100 条记录，等于 100 次 JSON 解析 + 100 次 Java Bean 映射。

**严重程度过滤在内存中完成**：`resolveHighestSeverity` 需要遍历 `analysis.problems` 才能确定最高严重程度。这个信息没有在数据库中索引——**每次查询都要实时解析**。

### 五、批量分析深度分析

`batchAnalyzeWorkbench`：

```text
1. 校验: requests != null && !empty && size <= 20
2. for each request:
   ├─ try: analyzeWorkbenchInternal(userId, request, false)
   │   ├─ success → successCount++
   │   └─ fallback → fallbackCount++
   └─ catch: failedCount++, log.warn
3. return BatchAnalyzeResponse(total, successCount, fallbackCount, failedCount, items)
```

**批量分析串行执行**：逐条调用 `analyzeWorkbenchInternal`，每条都调用 AI 服务。20 条 SQL = 20 次串行 AI 调用——**没有并行或批处理优化**。

**单条失败不阻断整体**：catch 只记录 warn 日志，不影响后续条目。这是合理的设计，但 **没有记录失败的具体 SQL 内容**——日志中只有序号，排查时需要对照前端提交顺序。

**批量分析整条事务包裹**：`@Transactional(rollbackFor = Exception.class)` 覆盖整个批量方法。但 catch 了内部异常，只有外部异常才会触发回滚——这意味着如果前 10 条成功保存但第 11 条抛出非 catch 的异常，前 10 条的记录也会被回滚。

### 六、收藏与删除深度分析

**收藏切换** `toggleFavorite`：

```text
1. getById(userId, id) → 校验归属
2. newFavorite = record.isFavorite == 1 ? 0 : 1 → 翻转
3. updateFavorite(id, userId, newFavorite)
4. return newFavorite == 1
```

**收藏翻转不是原子操作**：先 SELECT 再 UPDATE，并发收藏操作可能导致多次翻转。不过收藏翻转本身不要求强一致性，多次翻转的结果可能不确定。

**逻辑删除** `delete`：

```text
1. getById(userId, id) → 校验归属（但不使用返回值）
2. deleteById(id, userId) → UPDATE SET deleted=1
```

**软删除而非物理删除**：SQL 优化器是唯一使用软删除的模块（`deleted=1`），其他模块都是物理删除。但 **getById 不检查 deleted 字段**——`selectById` SQL 有 `AND deleted = 0`，所以 getById 对已删除记录返回 null，抛"记录不存在"。软删除后记录不再可见。

### 七、深度发现与坑点

#### 7.1 确认 BUG

| 编号 | BUG | 位置 | 说明 |
| --- | --- | --- | --- |
| BUG-1 | 案例库全量内存分页 | `getWorkbenchCases:174` | selectAllByUserId查出所有记录再内存分页，数据量大时性能极差 |
| BUG-2 | 对比结果不传recordId时不保存 | `compareWorkbench:151` | 不传recordId则对比结果只返回前端不写入数据库 |
| BUG-3 | 重写建议回填修改了分析结果 | `applyRewriteToAnalyzeResult` | analysis.optimizedSql和suggestions被rewrite修改，保存后analysis不再纯粹 |
| BUG-4 | 批量分析串行无并行 | `batchAnalyzeWorkbench:107` | 20条SQL逐条串行调用AI，无并行或批处理 |
| BUG-5 | 收藏翻转非原子 | `toggleFavorite:278` | SELECT再UPDATE，并发操作可能导致状态不确定 |
| BUG-6 | isWorkbenchPayload判断不精确 | `parseRecordPayload:374` | 只要JSON包含任意一个特定key就判断为v2，理论上可误判 |

#### 7.2 设计风险

| 编号 | 风险 | 说明 |
| --- | --- | --- |
| RISK-1 | 全量加载所有记录到内存 | selectAllByUserId不分页，analysis_result JSON大时内存占用高 |
| RISK-2 | parseRecordPayload调用N次 | 每次查案例库都逐条解析JSON，100条=100次JSON解析 |
| RISK-3 | 严重程度无数据库索引 | highestSeverity通过解析analysis.problems实时计算，无法走索引 |
| RISK-4 | AI服务无超时控制 | aiSqlOptimizeService调用无显式超时，AI慢会阻塞整个分析流程 |
| RISK-5 | analysis_result混合v1/v2 | 同一个字段存两种格式的JSON，兼容解析增加维护成本 |
| RISK-6 | SQL文本未做敏感词检测 | 用户粘贴的SQL可能包含生产数据（手机号、身份证等） |

#### 7.3 架构设计亮点

| 编号 | 亮点 | 说明 |
| --- | --- | --- |
| H-1 | v1/v2兼容解析 | parseRecordPayload支持旧版和新版两种格式，升级不丢历史 |
| H-2 | 双层fallback标记 | result.isFallback || rewrite.isFallback合并标记，任一环节降级即标注 |
| H-3 | 对比结果追加而非替换 | appendCompareToRecord保留analysis和rewrite，只追加compare |
| H-4 | 重写建议合并到分析结果 | REWRITE_SQL + ADD_INDEX建议追加到suggestions，统一展示 |
| H-5 | 逻辑删除 | 软删除(deleted=1)而非物理删除，可恢复 |
| H-6 | 批量分析单条失败不阻断 | catch只记录warn，不影响后续条目 |
| H-7 | 归属校验统一入口 | getById校验userId，所有操作都走这个校验 |
| H-8 | severity归一化 | normalizeSeverity统一HIGH/MEDIUM/LOW/NONE四种 |
| H-9 | SQL预览截断 | buildSqlPreview最长120字符，防止长SQL撑爆列表 |
| H-10 | 注解式Mapper | SqlOptimizeRecordMapper用@Select/@Update注解而非XML，简洁 |

#### 7.4 源码导航速查

| 想了解 | 读什么 |
| --- | --- |
| v1分析 | `SqlOptimizerServiceImpl.java` — analyze + v1保存 |
| v2分析 | `SqlOptimizerServiceImpl.java` — analyzeWorkbenchInternal + payload |
| v2重写 | `SqlOptimizerServiceImpl.java` — rewriteWorkbench + applyRewriteToAnalyzeResult |
| 重写建议合并 | `SqlOptimizerServiceImpl.java` — applyRewriteToAnalyzeResult + REWRITE_SQL/ADD_INDEX |
| v2对比 | `SqlOptimizerServiceImpl.java` — compareWorkbench + appendCompareToRecord |
| 对比追加 | `SqlOptimizerServiceImpl.java` — appendCompareToRecord + payload.compare |
| 批量分析 | `SqlOptimizerServiceImpl.java` — batchAnalyzeWorkbench + 串行循环 |
| 案例库 | `SqlOptimizerServiceImpl.java` — getWorkbenchCases + 全量内存分页 |
| 案例详情 | `SqlOptimizerServiceImpl.java` — getWorkbenchCaseById + toCaseDetail |
| v1/v2兼容 | `SqlOptimizerServiceImpl.java` — parseRecordPayload + 双次解析 |
| 严重程度 | `SqlOptimizerServiceImpl.java` — resolveHighestSeverity + normalizeSeverity |
| 收藏切换 | `SqlOptimizerServiceImpl.java` — toggleFavorite + 翻转逻辑 |
| 删除 | `SqlOptimizerServiceImpl.java` — delete + 逻辑删除 |
| 记录保存 | `SqlOptimizerServiceImpl.java` — saveRecord + v1/v2共用 |
| Controller | `SqlOptimizerController.java` — 11个接口 + @Operation注解 |
| Mapper | `SqlOptimizeRecordMapper.java` — @Select/@Update注解式SQL |
| 记录域 | `SqlOptimizeRecord.java` — analysis_result(JSON) + score + deleted |
