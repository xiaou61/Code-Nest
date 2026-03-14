# SQL 智能优化工作台 2.0 Coze 工作流配置指南

本文档描述 SQL 智能优化工作台 2.0 的 Coze 工作流配置细节（输入、输出、提示词、编排与异常处理）。

## 1. 总览

当前建议采用 **3 个主工作流 + 1 个兜底工作流**：

| 类型 | 工作流 | 建议枚举名 | 作用 |
|---|---|---|---|
| 兜底 | 兼容诊断 | `SQL_OPTIMIZE_ANALYZE` | 当 V2 诊断不可用时回退使用 |
| 主流程 | 基础诊断 | `SQL_OPTIMIZE_ANALYZE_V2` | 识别问题、评分、输出结构化诊断 |
| 主流程 | 重写建议 | `SQL_OPTIMIZE_REWRITE_V2` | 输出优化 SQL、索引 DDL、风险提示 |
| 主流程 | 收益对比 | `SQL_OPTIMIZE_COMPARE_V2` | 对比优化前后 EXPLAIN，输出收益评估 |

## 2. 配置位置

### 2.1 枚举管理

建议在：

`xiaou-common/src/main/java/com/xiaou/common/enums/CozeWorkflowEnum.java`

配置如下 4 个枚举（示例）：

```java
SQL_OPTIMIZE_ANALYZE("你的workflowId", "慢SQL优化分析", "分析SQL执行计划并给出优化建议"),
SQL_OPTIMIZE_ANALYZE_V2("你的workflowId", "慢SQL优化分析2.0", "分析SQL执行计划并输出结构化诊断结果"),
SQL_OPTIMIZE_REWRITE_V2("你的workflowId", "慢SQL重写建议2.0", "生成优化SQL和索引建议"),
SQL_OPTIMIZE_COMPARE_V2("你的workflowId", "慢SQL收益对比2.0", "对比优化前后收益并输出评估"),
```

### 2.2 服务编排入口

当前项目已在以下位置编排调用：

- `xiaou-ai/src/main/java/com/xiaou/ai/service/impl/AiSqlOptimizeServiceImpl.java`
- `xiaou-sql-optimizer/src/main/java/com/xiaou/sqloptimizer/service/impl/SqlOptimizerServiceImpl.java`

## 3. 兜底工作流：SQL_OPTIMIZE_ANALYZE（必须补）

> 说明：`analyzeSqlV2` 失败时会自动回退到该工作流，所以必须配置提示词与输出结构。

### 3.1 输入参数

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|
| sql | String | 是 | 待分析SQL |
| explainResult | String | 是 | EXPLAIN原始结果 |
| explainFormat | String | 是 | TABLE/JSON |
| tableStructures | String | 是 | DDL数组JSON字符串 |
| mysqlVersion | String | 否 | 默认8.0 |

### 3.2 输出格式（严格 JSON）

> 为了对齐后端 `parseAnalyzeResult`，建议输出以下字段结构（可选字段可为空）。

```json
{
  "score": 42,
  "optimizedSql": "SELECT id, name FROM users WHERE name = ?",
  "knowledgePoints": [
    "索引优化",
    "EXPLAIN分析"
  ],
  "problems": [
    {
      "type": "FULL_TABLE_SCAN",
      "severity": "HIGH",
      "description": "users 表出现全表扫描",
      "location": "users"
    }
  ],
  "explainAnalysis": [
    {
      "table": "users",
      "type": "ALL",
      "typeExplain": "全表扫描",
      "key": null,
      "keyExplain": "未命中索引",
      "rows": 120000,
      "extra": "Using where",
      "extraExplain": "先扫描后过滤",
      "issue": "name字段缺少索引"
    }
  ],
  "suggestions": [
    {
      "type": "ADD_INDEX",
      "title": "补充过滤列索引",
      "ddl": "ALTER TABLE users ADD INDEX idx_users_name(name);",
      "sql": null,
      "reason": "避免全表扫描",
      "expectedImprovement": "扫描行数预计显著下降"
    }
  ]
}
```

### 3.3 提示词模板（可直接使用）

```text
你是资深 MySQL DBA。请分析 SQL 与 EXPLAIN，输出严格 JSON（不要输出 Markdown、不要解释文字）。

输入：
- sql: {{sql}}
- explainResult: {{explainResult}}
- explainFormat: {{explainFormat}}
- tableStructures: {{tableStructures}}
- mysqlVersion: {{mysqlVersion}}

输出要求：
1. 输出字段必须包含：
   score, optimizedSql, knowledgePoints, problems, explainAnalysis, suggestions
2. score 范围 0-100，整数。
3. problems 每项字段：
   type, severity(HIGH/MEDIUM/LOW), description, location
4. explainAnalysis 每项字段：
   table, type, typeExplain, key, keyExplain, rows, extra, extraExplain, issue
5. suggestions 每项字段：
   type, title, ddl, sql, reason, expectedImprovement
6. 若某字段无法判断，返回 null 或空数组，不要省略字段。
7. 仅输出 JSON。
```

## 4. 主工作流 A：SQL_OPTIMIZE_ANALYZE_V2

### 4.1 输入参数

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|
| sql | String | 是 | 待分析SQL |
| explainResult | String | 是 | EXPLAIN原始结果 |
| explainFormat | String | 是 | TABLE/JSON |
| tableStructures | String | 是 | DDL数组JSON字符串 |
| mysqlVersion | String | 否 | 默认8.0 |

### 4.2 输出格式（严格 JSON）

> 为避免后端字段丢失，建议与 `SQL_OPTIMIZE_ANALYZE` 保持同结构。

```json
{
  "score": 42,
  "optimizedSql": "SELECT id, name FROM users WHERE name = ?",
  "knowledgePoints": ["索引优化", "EXPLAIN分析"],
  "problems": [
    {
      "type": "FULL_TABLE_SCAN",
      "severity": "HIGH",
      "description": "users 表出现全表扫描",
      "location": "users"
    }
  ],
  "explainAnalysis": [
    {
      "table": "users",
      "type": "ALL",
      "typeExplain": "全表扫描",
      "key": null,
      "keyExplain": "未命中索引",
      "rows": 120000,
      "extra": "Using where",
      "extraExplain": "先扫描后过滤",
      "issue": "name字段缺少索引"
    }
  ],
  "suggestions": [
    {
      "type": "ADD_INDEX",
      "title": "补充过滤列索引",
      "ddl": "ALTER TABLE users ADD INDEX idx_users_name(name);",
      "sql": null,
      "reason": "避免全表扫描",
      "expectedImprovement": "扫描行数预计显著下降"
    }
  ]
}
```

### 4.3 提示词模板

```text
你是资深 MySQL DBA。请分析 SQL 与 EXPLAIN，输出严格 JSON（仅 JSON）。

输入：
- sql: {{sql}}
- explainResult: {{explainResult}}
- explainFormat: {{explainFormat}}
- tableStructures: {{tableStructures}}
- mysqlVersion: {{mysqlVersion}}

要求：
1. 评分 0-100。
2. 输出 problems（type/severity/description/location）。
3. 输出 explainAnalysis（table/type/typeExplain/key/keyExplain/rows/extra/extraExplain/issue）。
4. 输出 suggestions（type/title/ddl/sql/reason/expectedImprovement）。
5. 输出 optimizedSql 和 knowledgePoints。
6. 只输出 JSON，不要额外文本。
```

## 5. 主工作流 B：SQL_OPTIMIZE_REWRITE_V2

### 5.1 输入参数

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|
| originalSql | String | 是 | 原始SQL |
| diagnoseJson | String | 是 | 工作流A输出JSON |
| tableStructures | String | 是 | DDL数组JSON字符串 |
| mysqlVersion | String | 否 | 默认8.0 |

### 5.2 输出格式（严格 JSON）

```json
{
  "optimizedSql": "SELECT id, name FROM users WHERE name = ?",
  "indexDdls": [
    "ALTER TABLE users ADD INDEX idx_users_name(name);"
  ],
  "rewriteReason": "消除全表扫描并减少回表成本",
  "riskWarnings": [
    "新增索引会增加写入开销",
    "请先在测试环境验证执行计划"
  ],
  "expectedImprovement": "预计扫描行数下降 95%+"
}
```

### 5.3 提示词模板

```text
你是 SQL 优化顾问。基于诊断结果生成优化 SQL 与索引建议。仅输出严格 JSON。

输入：
- originalSql: {{originalSql}}
- diagnoseJson: {{diagnoseJson}}
- tableStructures: {{tableStructures}}
- mysqlVersion: {{mysqlVersion}}

要求：
1. 输出 optimizedSql、indexDdls、rewriteReason、riskWarnings、expectedImprovement。
2. 保持语义等价优先，必要时明确风险。
3. indexDdls 和 riskWarnings 必须为数组（可为空数组）。
4. 仅输出 JSON。
```

## 6. 主工作流 C：SQL_OPTIMIZE_COMPARE_V2

### 6.1 输入参数

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|
| beforeSql | String | 是 | 优化前SQL |
| afterSql | String | 是 | 优化后SQL |
| beforeExplain | String | 是 | 优化前EXPLAIN |
| afterExplain | String | 是 | 优化后EXPLAIN |
| explainFormat | String | 是 | TABLE/JSON |

### 6.2 输出格式（严格 JSON）

```json
{
  "improvementScore": 88,
  "deltaRows": "120000 -> 800",
  "deltaType": "ALL -> ref",
  "deltaExtra": "Using temporary, Using filesort -> Using where",
  "summary": "主风险已消除，查询性能显著提升",
  "attention": [
    "关注索引维护成本",
    "建议压测验证慢查询比例"
  ]
}
```

### 6.3 提示词模板

```text
你是数据库性能评估专家。请评估优化前后收益并输出严格 JSON。

输入：
- beforeSql: {{beforeSql}}
- afterSql: {{afterSql}}
- beforeExplain: {{beforeExplain}}
- afterExplain: {{afterExplain}}
- explainFormat: {{explainFormat}}

输出字段：
improvementScore, deltaRows, deltaType, deltaExtra, summary, attention

要求：
1. improvementScore 为 0-100 整数。
2. attention 必须是数组（可为空）。
3. 仅输出 JSON。
```

## 7. 编排与错误处理

### 7.1 推荐编排

1. 先调 `SQL_OPTIMIZE_ANALYZE_V2`。
2. 若 V2 失败，自动回退 `SQL_OPTIMIZE_ANALYZE`。
3. 有诊断结果后调用 `SQL_OPTIMIZE_REWRITE_V2`。
4. 用户提供 afterExplain 时再调用 `SQL_OPTIMIZE_COMPARE_V2`。

### 7.2 异常策略

1. Analyze V2 失败：回退 Analyze（v1）。
2. Analyze（v1）也失败：走本地 `fallbackResult`。
3. Rewrite 失败：保留诊断结果并提示可重试。
4. Compare 失败：保留诊断/重写结果并提示稍后补充重试。

### 7.3 输出校验

1. 每个工作流结果都做 JSON 结构校验。
2. 字段缺失时使用默认值兜底，避免前端崩溃。
3. 必须保证数组字段输出为数组（不是字符串）。

## 8. 调试用例

### 8.1 输入样例

```json
{
  "sql": "SELECT * FROM users WHERE name = 'test'",
  "explainResult": "| id | select_type | table | type | possible_keys | key | rows | Extra |\n| 1 | SIMPLE | users | ALL | NULL | NULL | 120000 | Using where |",
  "explainFormat": "TABLE",
  "tableStructures": "[{\"tableName\":\"users\",\"ddl\":\"CREATE TABLE users (id BIGINT PRIMARY KEY, name VARCHAR(100), email VARCHAR(200));\"}]",
  "mysqlVersion": "8.0"
}
```

### 8.2 验证点

1. 输出必须是可反序列化 JSON。
2. Analyze 输出必须包含：`score/problems/explainAnalysis/suggestions`。
3. Rewrite 输出必须包含：`optimizedSql/indexDdls/riskWarnings`。
4. Compare 输出必须包含：`improvementScore/summary/attention`。
5. 极端输入（长 SQL、复杂多表 join）不返回空对象。

---

*文档版本：v2.0.1*  
*更新日期：2026-03-05*  
*归档目录：docs/coze*
