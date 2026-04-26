# 慢SQL优化 Coze工作流配置指南

本文档介绍慢SQL智能优化模块所需的Coze工作流配置。

## 概述

慢SQL优化模块需要配置1个Coze工作流：

| 工作流 | 枚举名 | 用途 |
|--------|--------|------|
| SQL优化分析 | `SQL_OPTIMIZE_ANALYZE` | AI分析SQL执行计划并给出优化建议 |

## 配置位置

工作流ID配置在 `xiaou-common` 模块的枚举类中：

```
xiaou-common/src/main/java/com/xiaou/common/enums/CozeWorkflowEnum.java
```

配置好Coze工作流后，将真实的工作流ID替换到对应的枚举值中。

---

## 工作流：SQL优化分析 (SQL_OPTIMIZE_ANALYZE)

### 功能说明
分析用户提交的SQL语句和EXPLAIN执行计划，识别性能问题并给出优化建议。

### 输入参数

| 参数名 | 类型 | 必填 | 说明 | 示例 |
|--------|------|------|------|------|
| sql | String | 是 | 待优化的SQL语句 | "SELECT * FROM users WHERE name = 'test'" |
| explainResult | String | 是 | EXPLAIN命令的输出结果 | 见下方示例 |
| explainFormat | String | 是 | EXPLAIN格式 | "TABLE" / "JSON" |
| tableStructures | String | 是 | 表结构DDL（JSON数组） | 见下方示例 |
| mysqlVersion | String | 否 | MySQL版本 | "8.0" |

**explainResult 示例（TABLE格式）：**
```
+----+-------------+-------+------+---------------+------+---------+------+------+-------------+
| id | select_type | table | type | possible_keys | key  | key_len | ref  | rows | Extra       |
+----+-------------+-------+------+---------------+------+---------+------+------+-------------+
|  1 | SIMPLE      | users | ALL  | NULL          | NULL | NULL    | NULL | 1000 | Using where |
+----+-------------+-------+------+---------------+------+---------+------+------+-------------+
```

**tableStructures 示例：**
```json
[
  {
    "tableName": "users",
    "ddl": "CREATE TABLE users (id BIGINT PRIMARY KEY, name VARCHAR(100), email VARCHAR(200), created_at DATETIME, INDEX idx_created_at(created_at)) ENGINE=InnoDB;"
  }
]
```

### Coze工作流提示词模板

```
# 角色设定
你是一位资深MySQL DBA，拥有10年数据库性能优化经验，精通SQL调优和索引设计。

# 任务
分析用户提供的SQL语句和EXPLAIN执行计划，识别性能问题并给出专业的优化建议。

# 输入信息
## 待优化SQL
{{sql}}

## EXPLAIN结果（格式：{{explainFormat}}）
{{explainResult}}

## 表结构DDL
{{tableStructures}}

## MySQL版本
{{mysqlVersion}}

# 分析要点
请重点关注以下性能问题：

1. **全表扫描**：type=ALL，无索引使用
2. **索引失效**：possible_keys有值但key为NULL
3. **文件排序**：Extra包含Using filesort
4. **临时表**：Extra包含Using temporary
5. **回表查询**：非覆盖索引导致的回表
6. **索引选择不当**：使用了低效索引
7. **JOIN优化**：驱动表选择、连接条件索引
8. **子查询问题**：相关子查询效率低
9. **LIKE前缀%**：导致索引失效
10. **隐式类型转换**：字段类型不匹配

# 评分标准（0-100分）
- 90-100：执行计划优秀，无明显问题
- 70-89：执行计划良好，有小问题可优化
- 50-69：存在明显性能问题，需要优化
- 30-49：性能问题严重，必须优化
- 0-29：执行计划极差，SQL需要重写

# 输出要求
请严格按以下JSON格式输出，不要输出其他内容：

{
  "score": 评分数字(0-100),
  "problems": [
    {
      "type": "问题类型枚举",
      "severity": "HIGH/MEDIUM/LOW",
      "description": "问题描述",
      "location": "问题位置（表名或字段）"
    }
  ],
  "explainAnalysis": [
    {
      "table": "表名",
      "type": "访问类型",
      "typeExplain": "访问类型解释",
      "key": "使用的索引",
      "keyExplain": "索引使用说明",
      "rows": 扫描行数,
      "extra": "Extra信息",
      "extraExplain": "Extra解释",
      "issue": "该行存在的问题（无问题则为null）"
    }
  ],
  "suggestions": [
    {
      "type": "ADD_INDEX/MODIFY_INDEX/REWRITE_SQL/SPLIT_QUERY/OTHER",
      "title": "建议标题",
      "ddl": "DDL语句（如果是索引建议）",
      "sql": "优化后的SQL（如果是SQL重写）",
      "reason": "建议原因",
      "expectedImprovement": "预期提升效果"
    }
  ],
  "optimizedSql": "优化后的完整SQL语句",
  "knowledgePoints": ["相关知识点1", "相关知识点2"]
}

# 问题类型枚举
- FULL_TABLE_SCAN：全表扫描
- INDEX_NOT_USED：索引失效
- FILE_SORT：文件排序
- TEMP_TABLE：临时表
- TABLE_BACK：回表查询
- BAD_INDEX_CHOICE：索引选择不当
- JOIN_ISSUE：JOIN问题
- SUBQUERY_ISSUE：子查询问题
- LIKE_PREFIX：LIKE前缀问题
- TYPE_CONVERSION：隐式类型转换
- SELECT_STAR：SELECT * 问题
- OTHER：其他问题
```

### 输出格式

Coze工作流返回格式为 `{"output": "实际JSON字符串"}`，output字段内容如下：

```json
{
  "score": 35,
  "problems": [
    {
      "type": "FULL_TABLE_SCAN",
      "severity": "HIGH",
      "description": "全表扫描，未使用任何索引，扫描行数1000行",
      "location": "users表"
    },
    {
      "type": "SELECT_STAR",
      "severity": "LOW",
      "description": "使用SELECT *，建议明确指定需要的字段",
      "location": "SELECT子句"
    }
  ],
  "explainAnalysis": [
    {
      "table": "users",
      "type": "ALL",
      "typeExplain": "全表扫描，效率最低的访问类型",
      "key": null,
      "keyExplain": "未使用任何索引",
      "rows": 1000,
      "extra": "Using where",
      "extraExplain": "在存储引擎层过滤数据",
      "issue": "name字段缺少索引，导致全表扫描"
    }
  ],
  "suggestions": [
    {
      "type": "ADD_INDEX",
      "title": "为name字段添加索引",
      "ddl": "ALTER TABLE users ADD INDEX idx_name(name);",
      "sql": null,
      "reason": "name字段作为WHERE条件的等值查询，添加索引可将全表扫描优化为索引查找",
      "expectedImprovement": "查询性能预计提升95%以上，扫描行数从1000降至1"
    },
    {
      "type": "REWRITE_SQL",
      "title": "避免SELECT *",
      "ddl": null,
      "sql": "SELECT id, name, email FROM users WHERE name = 'test'",
      "reason": "明确指定需要的字段可以减少数据传输量，如果字段都在索引中还能实现覆盖索引",
      "expectedImprovement": "减少不必要的字段传输，提升查询效率"
    }
  ],
  "optimizedSql": "SELECT id, name, email FROM users WHERE name = 'test'",
  "knowledgePoints": [
    "索引设计原则",
    "EXPLAIN执行计划分析",
    "覆盖索引优化",
    "SELECT * 的性能影响"
  ]
}
```

### 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| score | Integer | 性能评分（0-100） |
| problems | Array | 识别出的问题列表 |
| problems[].type | String | 问题类型枚举值 |
| problems[].severity | String | 严重程度：HIGH/MEDIUM/LOW |
| problems[].description | String | 问题描述 |
| problems[].location | String | 问题位置 |
| explainAnalysis | Array | EXPLAIN逐行解析 |
| explainAnalysis[].table | String | 表名 |
| explainAnalysis[].type | String | 访问类型 |
| explainAnalysis[].typeExplain | String | 访问类型解释 |
| explainAnalysis[].key | String | 使用的索引 |
| explainAnalysis[].keyExplain | String | 索引使用说明 |
| explainAnalysis[].rows | Integer | 扫描行数 |
| explainAnalysis[].extra | String | Extra信息 |
| explainAnalysis[].extraExplain | String | Extra解释 |
| explainAnalysis[].issue | String | 该行存在的问题 |
| suggestions | Array | 优化建议列表 |
| suggestions[].type | String | 建议类型 |
| suggestions[].title | String | 建议标题 |
| suggestions[].ddl | String | DDL语句（索引建议时） |
| suggestions[].sql | String | 优化后SQL（SQL重写时） |
| suggestions[].reason | String | 建议原因 |
| suggestions[].expectedImprovement | String | 预期提升效果 |
| optimizedSql | String | 优化后的完整SQL |
| knowledgePoints | Array | 相关知识点列表 |

---

## Coze工作流搭建步骤

### 1. 创建工作流

1. 登录 [Coze平台](https://www.coze.cn)
2. 进入「工作流」页面，点击「创建工作流」
3. 命名为「慢SQL优化分析」

### 2. 配置输入节点

添加「开始」节点，配置以下输入参数：

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| sql | String | 是 | 待优化的SQL语句 |
| explainResult | String | 是 | EXPLAIN结果 |
| explainFormat | String | 是 | EXPLAIN格式（TABLE/JSON） |
| tableStructures | String | 是 | 表结构DDL |
| mysqlVersion | String | 否 | MySQL版本 |

### 3. 添加大模型节点

1. 添加「大模型」节点
2. 选择模型（推荐：豆包/通义千问/GPT-4）
3. 将上方的提示词模板粘贴到「系统提示词」或「用户提示词」
4. 配置参数映射：
   - `{{sql}}` → 输入参数 sql
   - `{{explainResult}}` → 输入参数 explainResult
   - `{{explainFormat}}` → 输入参数 explainFormat
   - `{{tableStructures}}` → 输入参数 tableStructures
   - `{{mysqlVersion}}` → 输入参数 mysqlVersion

### 4. 配置输出节点

1. 添加「结束」节点
2. 输出类型选择「JSON」
3. 将大模型输出映射到结束节点

### 5. 工作流调试

1. 使用测试用例进行调试：
   ```json
   {
     "sql": "SELECT * FROM users WHERE name = 'test'",
     "explainResult": "| id | select_type | table | type | possible_keys | key | key_len | ref | rows | Extra |\n| 1 | SIMPLE | users | ALL | NULL | NULL | NULL | NULL | 1000 | Using where |",
     "explainFormat": "TABLE",
     "tableStructures": "[{\"tableName\":\"users\",\"ddl\":\"CREATE TABLE users (id BIGINT PRIMARY KEY, name VARCHAR(100), email VARCHAR(200));\"}]",
     "mysqlVersion": "8.0"
   }
   ```
2. 检查输出是否符合预期JSON格式
3. 调整提示词直到输出稳定

### 6. 发布工作流

1. 测试通过后，点击「发布」
2. 复制工作流ID

### 7. 更新代码配置

修改 `CozeWorkflowEnum.java` 添加新的枚举值：

```java
SQL_OPTIMIZE_ANALYZE("你的工作流ID", "慢SQL优化分析", "分析SQL执行计划并给出优化建议"),
```

---

## 高级配置建议

### 1. 模型选择

- **推荐模型**：豆包大模型、通义千问、GPT-4
- **Token限制**：建议设置max_tokens为4096以保证完整输出
- **温度参数**：建议设置为0.3，保证输出稳定性

### 2. 超时配置

- SQL分析可能涉及复杂推理，建议超时时间设置为60秒
- 在代码中配置Coze调用超时：
  ```yaml
  coze:
    api:
      timeout: 60000
  ```

### 3. 错误处理

工作流应处理以下异常情况：
- SQL语法错误
- EXPLAIN结果格式错误
- 表结构DDL解析失败

建议在大模型节点后添加「条件判断」节点，检查输出格式是否正确。

### 4. 缓存优化

- 相同的SQL+EXPLAIN+DDL组合可以缓存分析结果
- 建议缓存时间：24小时
- 缓存Key：`sql:optimize:${md5(sql+explain+ddl)}`

---

## 降级方案

当Coze服务不可用时，系统启用本地降级方案：

1. **基础规则检测**：
   - 检测type=ALL全表扫描
   - 检测possible_keys有值但key为NULL
   - 检测Extra中的filesort和temporary

2. **简单评分**：
   - 根据检测到的问题数量和严重程度给出基础评分

3. **固定建议**：
   - 提供通用的SQL优化建议

---

## 注意事项

1. **输出格式**：确保大模型输出严格符合JSON格式
2. **output包装**：系统会自动处理Coze返回的 `{"output": "..."}` 格式
3. **特殊字符**：SQL和DDL中的特殊字符需要正确转义
4. **长文本处理**：复杂SQL和DDL可能较长，确保模型能处理完整输入
5. **版本差异**：MySQL 5.7和8.0的EXPLAIN输出格式略有不同，提示词中已考虑

---

*文档版本：v1.0.0*
*创建日期：2026-01-20*
