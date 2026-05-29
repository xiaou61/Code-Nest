# 管理端 API - AI Runtime

本文档详细说明管理端 AI Runtime 模块的 API 接口，包括 AI 配置、治理、RAG、回归等。

## 权限要求

所有管理端接口都需要管理员登录态，使用 `@RequireAdmin` 注解保护。

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {adminToken} |

---

## 一、AI 配置（AiConfigController）

**模块**：`xiaou-system`
**路由前缀**：`/admin/ai/config`

### 1.1 获取运行时配置

```
GET /admin/ai/config/runtime
```

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "enabled": true,
    "provider": "openai-compatible",
    "baseUrl": "https://api.openai.com",
    "apiKey": "sk-***",
    "model": {
      "chat": "gpt-4o-mini",
      "embedding": "text-embedding-3-small"
    },
    "timeout": {
      "connectMs": 10000,
      "readMs": 60000
    },
    "retry": {
      "maxRetries": 3,
      "backoffMs": 1000
    },
    "rag": {
      "enabled": true,
      "endpoint": "http://localhost:18080",
      "defaultTopK": 5
    },
    "metrics": {
      "persistence": {
        "enabled": true,
        "redisKey": "xiaou:ai:runtime:metrics"
      }
    }
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl http://localhost:9999/api/admin/ai/config/runtime \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

### 1.2 获取 Schema 清单

```
GET /admin/ai/config/schema-catalog
```

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "prompts": [
      {
        "promptId": "mock_interview_evaluate",
        "key": "evaluate",
        "version": "1.0",
        "systemPrompt": "你是一个专业的面试官...",
        "userTemplate": "请评价以下回答...",
        "hasSchema": true,
        "hasRagQuery": false
      }
    ],
    "schemas": [
      {
        "promptId": "mock_interview_evaluate",
        "outputClass": "AIEvaluationResult",
        "schema": "{...}"
      }
    ],
    "ragQueries": [
      {
        "queryId": "sql_optimize_context",
        "promptId": "sql_optimize_analyze",
        "template": "查找相关的 SQL 优化知识..."
      }
    ],
    "ragProfiles": [
      {
        "profileId": "sql_optimize_profile",
        "topK": 5,
        "threshold": 0.7,
        "scenario": "SQL优化"
      }
    ],
    "coverage": {
      "totalPrompts": 13,
      "schemaCovered": 13,
      "ragCovered": 4,
      "schemaCoverageRate": 100.0,
      "ragCoverageRate": 30.8
    }
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl http://localhost:9999/api/admin/ai/config/schema-catalog \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

### 1.3 测试模型配置

```
POST /admin/ai/config/test
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| baseUrl | String | 否 | 测试 URL（空使用当前配置） |
| apiKey | String | 否 | 测试 API Key（空使用当前配置） |
| model | String | 否 | 测试模型（空使用当前配置） |

**请求示例**：

```json
{
  "baseUrl": "https://api.openai.com",
  "apiKey": "sk-test",
  "model": "gpt-4o-mini"
}
```

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "success": true,
    "model": "gpt-4o-mini",
    "latency": 500,
    "response": "Hello!",
    "error": null
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl -X POST http://localhost:9999/api/admin/ai/config/test \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"model":"gpt-4o-mini"}'
```

### 1.4 Prompt 调试

```
POST /admin/ai/config/prompt-debug
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| promptId | String | 是 | Prompt ID |
| variables | Object | 否 | 变量键值对 |

**请求示例**：

```json
{
  "promptId": "mock_interview_evaluate",
  "variables": {
    "question": "什么是多态？",
    "answer": "多态是指...",
    "reference": "多态的定义..."
  }
}
```

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "promptId": "mock_interview_evaluate",
    "renderedSystemPrompt": "你是一个专业的面试官...",
    "renderedUserPrompt": "请评价以下回答：...",
    "model": "gpt-4o-mini",
    "response": "这个回答很好地解释了多态的概念...",
    "latency": 1000,
    "tokens": {
      "input": 500,
      "output": 200,
      "total": 700
    },
    "estimatedCost": 0.001,
    "error": null
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl -X POST http://localhost:9999/api/admin/ai/config/prompt-debug \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"promptId":"mock_interview_evaluate","variables":{"question":"什么是多态？","answer":"多态是指..."}}'
```

### 1.5 RAG 调试

```
POST /admin/ai/config/rag-debug
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| query | String | 是 | 查询文本 |
| profileId | String | 否 | RAG Profile ID（空使用默认） |

**请求示例**：

```json
{
  "query": "Java 多态的实现原理",
  "profileId": "sql_optimize_profile"
}
```

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "query": "Java 多态的实现原理",
    "profileId": "sql_optimize_profile",
    "results": [
      {
        "nodeId": "node_1",
        "content": "Java 多态的实现原理...",
        "score": 0.85,
        "metadata": {
          "source": "java_basics.md",
          "page": 10
        }
      }
    ],
    "totalResults": 1,
    "latency": 200,
    "error": null
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl -X POST http://localhost:9999/api/admin/ai/config/rag-debug \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"query":"Java 多态的实现原理"}'
```

---

## 二、RAG 管理

### 2.1 RAG 服务健康检查

```
GET /admin/ai/config/rag-service/health
```

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "status": "healthy",
    "version": "1.0.0",
    "documentsCount": 100,
    "latency": 50,
    "uptime": "2d 3h 30m"
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl http://localhost:9999/api/admin/ai/config/rag-service/health \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

### 2.2 RAG 文档列表

```
GET /admin/ai/config/rag-service/documents
```

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": "doc_1",
      "title": "Java 基础知识",
      "content": "Java 是一种面向对象的编程语言...",
      "metadata": {
        "source": "java_basics.md",
        "category": "Java",
        "chunkCount": 10
      },
      "createdAt": "2024-01-01 12:00:00"
    }
  ],
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl http://localhost:9999/api/admin/ai/config/rag-service/documents \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

### 2.3 导入样例知识

```
POST /admin/ai/config/rag-service/sample-import
```

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "imported": 10,
    "skipped": 2,
    "failed": 0
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl -X POST http://localhost:9999/api/admin/ai/config/rag-service/sample-import \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

### 2.4 导入自定义知识

```
POST /admin/ai/config/rag-service/documents/import
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| documents | List<Object> | 是 | 文档列表 |

**请求示例**：

```json
{
  "documents": [
    {
      "title": "自定义知识",
      "content": "这是自定义知识内容...",
      "metadata": {
        "source": "custom",
        "category": "Java"
      }
    }
  ]
}
```

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "imported": 1,
    "skipped": 0,
    "failed": 0
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl -X POST http://localhost:9999/api/admin/ai/config/rag-service/documents/import \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"documents":[{"title":"自定义知识","content":"这是自定义知识内容..."}]}'
```

### 2.5 删除 RAG 文档

```
DELETE /admin/ai/config/rag-service/documents/{documentId}
```

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| documentId | String | 是 | 文档 ID |

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": null,
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl -X DELETE http://localhost:9999/api/admin/ai/config/rag-service/documents/doc_1 \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

---

## 三、回归管理

### 3.1 获取回归用例

```
GET /admin/ai/config/regression/cases
```

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": "case_1",
      "scenario": "mock_interview_evaluate",
      "description": "模拟面试评价 - 正常回答",
      "input": {
        "question": "什么是多态？",
        "answer": "多态是指..."
      },
      "expected": {
        "score": 85,
        "feedback": "回答很好地解释了多态的概念"
      }
    }
  ],
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl http://localhost:9999/api/admin/ai/config/regression/cases \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

### 3.2 执行回归测试

```
POST /admin/ai/config/regression/run
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| scenario | String | 否 | 场景筛选（空表示全部） |
| caseIds | List<String> | 否 | 用例 ID 列表（空表示全部） |

**请求示例**：

```json
{
  "scenario": "mock_interview_evaluate"
}
```

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "runId": "run_1",
    "scenario": "mock_interview_evaluate",
    "totalCases": 5,
    "passed": 4,
    "failed": 1,
    "skipped": 0,
    "duration": 5000,
    "results": [
      {
        "caseId": "case_1",
        "status": "passed",
        "actual": {
          "score": 85,
          "feedback": "回答很好地解释了多态的概念"
        },
        "expected": {
          "score": 85,
          "feedback": "回答很好地解释了多态的概念"
        },
        "latency": 1000
      }
    ],
    "startTime": "2024-01-15 12:00:00",
    "endTime": "2024-01-15 12:00:05"
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl -X POST http://localhost:9999/api/admin/ai/config/regression/run \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"scenario":"mock_interview_evaluate"}'
```

### 3.3 获取最新回归结果

```
GET /admin/ai/config/regression/latest
```

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "runId": "run_1",
    "scenario": "all",
    "totalCases": 13,
    "passed": 12,
    "failed": 1,
    "skipped": 0,
    "duration": 15000,
    "startTime": "2024-01-15 12:00:00",
    "endTime": "2024-01-15 12:00:15",
    "healthStatus": "healthy",
    "failedCases": [
      {
        "caseId": "case_5",
        "scenario": "sql_optimize_analyze",
        "error": "解析失败"
      }
    ]
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl http://localhost:9999/api/admin/ai/config/regression/latest \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

### 3.4 获取回归历史

```
GET /admin/ai/config/regression/history
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | Integer | 否 | 页码（默认 1） |
| pageSize | Integer | 否 | 每页大小（默认 10） |

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "pageNum": 1,
    "pageSize": 10,
    "total": 20,
    "totalPages": 2,
    "records": [
      {
        "runId": "run_1",
        "scenario": "all",
        "totalCases": 13,
        "passed": 12,
        "failed": 1,
        "duration": 15000,
        "startTime": "2024-01-15 12:00:00",
        "endTime": "2024-01-15 12:00:15",
        "healthStatus": "healthy"
      }
    ],
    "hasNext": true,
    "hasPrevious": false
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl "http://localhost:9999/api/admin/ai/config/regression/history?pageNum=1&pageSize=10" \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

### 3.5 获取场景健康度

```
GET /admin/ai/config/regression/scenario-health
```

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "scenario": "mock_interview_evaluate",
      "totalCases": 5,
      "passed": 5,
      "failed": 0,
      "passRate": 100.0,
      "healthStatus": "healthy",
      "lastRunTime": "2024-01-15 12:00:00"
    },
    {
      "scenario": "sql_optimize_analyze",
      "totalCases": 3,
      "passed": 2,
      "failed": 1,
      "passRate": 66.7,
      "healthStatus": "warning",
      "lastRunTime": "2024-01-15 12:00:00"
    }
  ],
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl http://localhost:9999/api/admin/ai/config/regression/scenario-health \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

---

## 四、运行指标

### 4.1 获取运行指标

```
GET /admin/ai/config/metrics
```

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "overview": {
      "totalInvocations": 1000,
      "successCount": 950,
      "errorCount": 50,
      "fallbackCount": 20,
      "structuredParseFailureCount": 5,
      "totalInputTokens": 500000,
      "totalOutputTokens": 200000,
      "totalTokens": 700000,
      "estimatedCost": 1.5,
      "averageLatencyMs": 2000,
      "lastInvocationAt": "2024-01-15 12:00:00"
    },
    "sceneStats": [
      {
        "scene": "mock_interview_evaluate",
        "promptKey": "evaluate",
        "promptVersion": "1.0",
        "invocations": 500,
        "successCount": 480,
        "errorCount": 20,
        "fallbackCount": 10,
        "averageLatencyMs": 1500,
        "lastInvocationAt": "2024-01-15 12:00:00"
      }
    ],
    "recentCalls": [
      {
        "scene": "mock_interview_evaluate",
        "promptKey": "evaluate",
        "promptVersion": "1.0",
        "modelName": "gpt-4o-mini",
        "outcome": "success",
        "latencyMs": 1200,
        "inputTokens": 500,
        "outputTokens": 200,
        "totalTokens": 700,
        "estimatedCost": 0.001,
        "timestamp": "2024-01-15 12:00:00"
      }
    ]
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl http://localhost:9999/api/admin/ai/config/metrics \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

### 4.2 清空运行指标

```
DELETE /admin/ai/config/metrics
```

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": null,
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl -X DELETE http://localhost:9999/api/admin/ai/config/metrics \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

---

## 五、AI 治理（AiGovernanceController）

**模块**：`xiaou-ai`
**路由前缀**：`/admin/ai/governance`

### 5.1 获取治理概览

```
GET /admin/ai/governance/overview
```

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "totalWorkflows": 13,
    "configuredWorkflows": 13,
    "fallbackCoveredWorkflows": 10,
    "fallbackCoverageRate": 76.9,
    "healthy": true,
    "healthLevel": "STABLE",
    "qualityScore": 85,
    "qualityGrade": "B",
    "summary": "AI Runtime 运行稳定，Prompt 覆盖率 100%，Schema 覆盖率 100%，RAG 覆盖率 30.8%",
    "generatedAt": "2024-01-15 12:00:00",
    "workflows": [
      {
        "promptId": "mock_interview_evaluate",
        "promptKey": "evaluate",
        "promptVersion": "1.0",
        "configured": true,
        "fallbackCovered": true,
        "schemaCovered": true,
        "ragCovered": false,
        "runtimeObserved": true,
        "riskLevel": "LOW",
        "riskText": null
      }
    ],
    "qualityRules": [
      {
        "name": "Prompt 完整性",
        "weight": 25,
        "score": 25,
        "status": "PASS"
      },
      {
        "name": "Schema 覆盖",
        "weight": 25,
        "score": 25,
        "status": "PASS"
      },
      {
        "name": "RAG 覆盖",
        "weight": 15,
        "score": 4.6,
        "status": "WARNING"
      },
      {
        "name": "运行覆盖",
        "weight": 10,
        "score": 10,
        "status": "PASS"
      },
      {
        "name": "运行质量",
        "weight": 25,
        "score": 20,
        "status": "PASS"
      }
    ],
    "improvementSuggestions": [
      "建议为 mock_interview_generate 和 job_battle_parse_jd 场景添加 RAG 知识库"
    ],
    "runtimeInsight": {
      "totalInvocations": 1000,
      "successRate": 95.0,
      "fallbackRate": 2.0,
      "parseFailureRate": 0.5,
      "averageLatencyMs": 2000
    },
    "riskItems": [
      {
        "severity": "MEDIUM",
        "score": 62,
        "message": "RAG 覆盖率较低（30.8%）",
        "promptId": "mock_interview_generate"
      }
    ]
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl http://localhost:9999/api/admin/ai/governance/overview \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

---

## 相关文档

| 文档 | 说明 |
| --- | --- |
| [AI Runtime](/modules/ai-runtime) | AI Runtime 模块详解 |
| [AI Schema 与治理](/reference/ai-schemas) | 结构化输出契约和治理规范 |
| [响应体与错误码](/reference/response-errors) | 完整错误码列表 |
| [API 路由索引](/reference/api-routes) | 完整接口清单 |
