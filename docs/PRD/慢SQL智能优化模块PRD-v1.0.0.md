# 慢SQL智能优化模块 PRD v1.0.0

> 历史说明：本文档中的 Coze 调用描述对应项目早期 AI 架构。当前主干已经迁移到 `LangChain4j + LangGraph4j + LlamaIndex` 统一 AI Runtime，现行实现请以 `docs/plans/2026-04-20-ai-runtime-replatform-design.md` 为准。

## 📋 项目概述

### 🎯 项目背景
在日常开发和运维过程中，SQL性能优化是一个常见且重要的工作。然而，不是所有开发者都精通SQL调优，分析EXPLAIN执行计划需要一定的专业知识。本模块旨在通过AI能力，帮助开发者快速识别SQL性能问题并获取优化建议，降低SQL调优的门槛。

### 💡 核心价值
- **降低门槛**：无需深厚的DBA经验，即可获得专业的SQL优化建议
- **提升效率**：AI自动分析EXPLAIN结果，快速定位性能瓶颈
- **学习成长**：每次优化建议都包含原理说明，帮助开发者积累经验
- **标准化输出**：统一的分析报告格式，便于团队沟通和文档沉淀

### 🗄️ 支持范围
- **数据库类型**：MySQL（5.7+、8.0+）
- **分析内容**：SELECT/UPDATE/DELETE/INSERT语句

## 🚀 功能需求

### 1. 核心功能

#### 1.1 SQL输入
- **SQL语句输入**：支持用户输入待优化的SQL语句
- **语法高亮**：提供SQL语法高亮显示
- **格式化**：支持SQL自动格式化美化
- **历史记录**：保存用户的分析历史，便于回顾

#### 1.2 EXPLAIN结果输入
- **文本输入**：支持粘贴EXPLAIN命令的输出结果
- **JSON格式**：支持EXPLAIN FORMAT=JSON的结果解析
- **表格展示**：将EXPLAIN结果以表格形式直观展示

#### 1.3 表结构输入（必填）
- **DDL输入**：必须输入相关表的CREATE TABLE语句
- **索引信息**：自动解析并展示现有索引
- **多表支持**：支持输入多个相关联表的结构

#### 1.4 AI智能分析
- **Coze工作流调用**：调用Coze平台的SQL优化工作流
- **执行计划解读**：解释EXPLAIN各字段含义和问题点
- **性能问题识别**：自动识别全表扫描、索引失效、临时表等问题
- **优化建议输出**：提供具体可执行的优化方案

### 2. 分析输出内容

#### 2.1 问题诊断
- **性能评分**：对SQL性能给出综合评分（0-100）
- **问题标签**：快速标注问题类型（如：全表扫描、索引失效、排序优化等）
- **严重程度**：问题严重程度分级（高/中/低）

#### 2.2 EXPLAIN解读
- **逐行解析**：对EXPLAIN结果的每一行进行解读
- **关键指标说明**：解释type、key、rows、Extra等关键字段
- **问题定位**：指出具体哪些环节存在性能问题

#### 2.3 优化建议
- **索引建议**：推荐添加或修改的索引（包含完整的DDL语句）
- **SQL重写**：提供优化后的SQL写法
- **查询拆分**：对于复杂查询，建议拆分方案
- **原理说明**：解释每条建议的原因和预期效果

#### 2.4 学习资料
- **知识点链接**：相关MySQL优化知识点
- **最佳实践**：SQL编写最佳实践提示

### 3. 增强功能

#### 3.1 历史记录管理
- **分析历史**：保存所有分析记录
- **收藏功能**：收藏有价值的优化案例
- **搜索筛选**：按时间、问题类型等筛选历史记录

#### 3.2 优化效果对比
- **Before/After对比**：展示优化前后的EXPLAIN对比
- **预估提升**：预估性能提升幅度

#### 3.3 团队协作
- **分享功能**：将分析结果分享给团队成员
- **评论讨论**：支持对优化方案进行讨论

## 🔧 技术实现方案

### 1. 架构设计

#### 1.1 技术栈
- **前端**：Vue3 + TypeScript + Monaco Editor（SQL编辑器）
- **后端**：Spring Boot + MyBatis-Plus
- **AI服务**：Coze工作流API
- **存储**：MySQL（分析记录）+ Redis（缓存）

#### 1.2 模块结构
```
xiaou-modules/
└── xiaou-sql-optimizer/
    ├── controller/
    │   └── SqlOptimizerController.java
    ├── service/
    │   ├── SqlOptimizerService.java
    │   └── impl/
    │       └── SqlOptimizerServiceImpl.java
    ├── domain/
    │   ├── dto/
    │   │   ├── SqlAnalyzeRequest.java
    │   │   └── SqlAnalyzeResponse.java
    │   ├── entity/
    │   │   └── SqlOptimizeRecord.java
    │   └── vo/
    │       └── SqlOptimizeResultVO.java
    └── mapper/
        └── SqlOptimizeRecordMapper.java
```

### 2. 接口设计

#### 2.1 SQL分析接口
```
POST /api/sql-optimizer/analyze
```
**请求参数：**
```json
{
  "sql": "SELECT * FROM users WHERE name = 'test'",
  "explainResult": "EXPLAIN结果文本或JSON",
  "explainFormat": "TABLE|JSON",
  "tableStructures": [
    {
      "tableName": "users",
      "ddl": "CREATE TABLE users (...)"
    }
  ],
  "mysqlVersion": "8.0"
}
```
**响应结果：**
```json
{
  "code": 200,
  "data": {
    "score": 45,
    "problems": [
      {
        "type": "FULL_TABLE_SCAN",
        "severity": "HIGH",
        "description": "全表扫描，未使用索引",
        "location": "users表"
      }
    ],
    "explainAnalysis": [...],
    "suggestions": [
      {
        "type": "ADD_INDEX",
        "title": "添加name字段索引",
        "ddl": "ALTER TABLE users ADD INDEX idx_name(name);",
        "reason": "name字段作为WHERE条件，添加索引可避免全表扫描",
        "expectedImprovement": "预计查询性能提升90%+"
      }
    ],
    "optimizedSql": "SELECT id, name, email FROM users WHERE name = 'test'"
  }
}
```

#### 2.2 历史记录接口
```
GET /api/sql-optimizer/history
POST /api/sql-optimizer/favorite/{id}
DELETE /api/sql-optimizer/history/{id}
```

### 3. Coze工作流设计

#### 3.1 工作流输入
- 原始SQL语句
- EXPLAIN执行计划结果
- 表结构DDL（必填）
- MySQL版本信息

#### 3.2 工作流处理
1. 解析EXPLAIN结果，提取关键指标
2. 识别常见性能问题模式
3. 结合表结构分析索引使用情况
4. 生成优化建议和重写SQL

#### 3.3 工作流输出
- 结构化的分析报告（JSON格式）
- 问题列表及严重程度
- 优化建议列表
- 优化后的SQL语句

### 4. 数据库设计

#### 4.1 分析记录表
```sql
CREATE TABLE sql_optimize_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  original_sql TEXT NOT NULL COMMENT '原始SQL',
  explain_result TEXT COMMENT 'EXPLAIN结果',
  table_structures TEXT COMMENT '表结构JSON',
  analysis_result TEXT COMMENT '分析结果JSON',
  score INT COMMENT '性能评分',
  is_favorite TINYINT DEFAULT 0 COMMENT '是否收藏',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
  INDEX idx_user_id (user_id),
  INDEX idx_create_time (create_time)
) COMMENT '慢SQL优化分析记录表';
```

## 📱 用户流程

### 1. 基本使用流程
1. 用户进入「慢SQL优化」页面
2. 在SQL输入框中粘贴待优化的SQL语句
3. 在EXPLAIN输入框中粘贴执行计划结果
4. 输入相关表的DDL
5. 点击「开始分析」按钮
6. 等待AI分析完成，查看优化报告
7. 根据建议进行SQL优化
8. （可选）保存或分享分析结果

### 2. 获取EXPLAIN结果指引
页面提供获取EXPLAIN结果的说明：
```sql
-- 基础用法
EXPLAIN SELECT * FROM your_table WHERE condition;

-- JSON格式（推荐，信息更详细）
EXPLAIN FORMAT=JSON SELECT * FROM your_table WHERE condition;

-- 包含实际执行统计（MySQL 8.0+）
EXPLAIN ANALYZE SELECT * FROM your_table WHERE condition;
```

## ✅ 实现计划

### 第一期：MVP版本
- 🔲 前端页面基础布局
- 🔲 SQL/EXPLAIN输入组件
- 🔲 Coze工作流创建与对接
- 🔲 基础分析结果展示
- 🔲 后端接口实现

### 第二期：功能完善
- 🔲 表结构DDL输入支持
- 🔲 历史记录功能
- 🔲 收藏功能
- 🔲 分析结果优化展示（标签、评分等）

### 第三期：体验优化
- 🔲 Monaco Editor集成（语法高亮、自动补全）
- 🔲 SQL格式化功能
- 🔲 优化前后对比展示
- 🔲 分享功能

### 第四期：高级功能
- 🔲 批量SQL分析
- 🔲 定期扫描项目SQL
- 🔲 团队协作与讨论
- 🔲 优化案例库

## 📊 常见问题识别清单

AI将重点识别以下MySQL性能问题：

| 问题类型 | 识别特征 | 严重程度 |
|---------|---------|---------|
| 全表扫描 | type=ALL, 无key | 高 |
| 索引失效 | possible_keys有值但key为NULL | 高 |
| 文件排序 | Extra包含Using filesort | 中 |
| 临时表 | Extra包含Using temporary | 中 |
| 回表查询 | 非覆盖索引，需要回表 | 中 |
| 索引选择不当 | 使用了低效索引 | 中 |
| JOIN优化 | 驱动表选择不当 | 中 |
| 子查询问题 | 相关子查询效率低 | 中 |
| LIKE前缀% | 导致索引失效 | 低 |
| 隐式类型转换 | 字段类型不匹配 | 低 |

## 🔒 安全考虑

### 1. 数据安全
- SQL内容仅用于分析，不执行
- 用户数据隔离，仅可见自己的记录
- 敏感信息脱敏提示

### 2. 接口安全
- 登录用户才可使用
- 接口限流，防止滥用
- SQL长度限制（建议10KB以内）

## 📈 成功指标

### 1. 功能指标
- 问题识别准确率 > 85%
- 优化建议采纳率 > 60%
- 分析响应时间 < 10秒

### 2. 用户指标
- 日活用户数
- 人均分析次数
- 用户满意度评分

---

*文档版本：v1.0.0*
*创建日期：2026-01-20*
*作者：Code-Nest Team*
