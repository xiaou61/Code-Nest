# LlamaIndex Service

该目录用于承载独立部署的 LlamaIndex 检索服务。

## 目标

- 与 Java 主工程解耦部署
- 统一提供检索增强能力
- 为 SQL 优化、模拟面试、求职作战台等复杂场景提供知识片段召回
- 在真实向量库接入前，先提供一个可导入知识、可检索、可鉴权的最小可用服务

## 当前接口

- `GET /health`
- `POST /api/v1/retrieve`
- `POST /api/v1/admin/documents/import`
- `GET /api/v1/admin/documents`

## 环境变量

| 变量名 | 说明 | 默认值 |
| --- | --- | --- |
| `LLAMAINDEX_SERVICE_API_KEY` | 服务鉴权 Key，配置后所有检索和管理接口都要求 `Bearer` 鉴权 | 空 |
| `LLAMAINDEX_DATA_FILE` | 知识库 JSON 文件路径 | `data/knowledge-base.json` |
| `LLAMAINDEX_MAX_TOP_K` | 检索接口允许的最大 `topK` | `20` |

建议：

- 本地联调直接放到 shell 环境变量，不要把明文 Key 写进仓库。
- Java 端 `xiaou.ai.rag.api-key` 与这里的 `LLAMAINDEX_SERVICE_API_KEY` 保持一致。

请求示例：

```json
{
  "query": "MySQL explain using filesort optimize",
  "scene": "sql_optimize",
  "topK": 5,
  "metadataFilters": {
    "domain": "mysql"
  }
}
```

响应示例：

```json
{
  "query": "MySQL explain using filesort optimize",
  "nodes": [
    {
      "id": "doc-1",
      "score": 0.92,
      "text": "Avoid filesort by creating a composite index...",
      "metadata": {
        "source": "mysql-manual"
      },
      "matchedTerms": ["filesort", "optimize"],
      "scoreBreakdown": {
        "textTermScore": 2.73,
        "termCoverage": 1.2
      },
      "bestMatchField": "text"
    }
  ],
  "fallback": false
}
```

### 导入知识文档

```bash
curl -X POST "http://localhost:18080/api/v1/admin/documents/import" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $LLAMAINDEX_SERVICE_API_KEY" \
  -d '{
    "documents": [
      {
        "id": "doc-cache-1",
        "scene": "mock_interview",
        "text": "缓存一致性常见方案包括旁路缓存、延迟双删和消息最终一致性。",
        "metadata": {
          "source": "interview-handbook",
          "level": "senior"
        }
      },
      {
        "id": "doc-sql-1",
        "scene": "sql_optimize",
        "text": "Using filesort 往往意味着排序阶段没有完全命中索引，需要优先检查过滤列与排序列的联合索引设计。",
        "metadata": {
          "source": "mysql-manual",
          "topic": "index"
        }
      }
    ]
  }'
```

### 查看已导入文档

```bash
curl "http://localhost:18080/api/v1/admin/documents?limit=20" \
  -H "Authorization: Bearer $LLAMAINDEX_SERVICE_API_KEY"
```

## 本地启动

推荐直接从仓库根目录执行脚本：

```powershell
.\scripts\ai\start-llamaindex-service.ps1 -ApiKey "your-rag-key"
```

如果需要导入一份可直接联调的知识样例：

```powershell
.\scripts\ai\import-sample-knowledge.ps1 -ApiKey "your-rag-key" -Replace
```

也可以手动启动：

```bash
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 18080 --reload
```

## Docker 启动

如果你希望把 sidecar 容器化运行，可以直接构建：

```bash
docker build -t code-nest-llamaindex:latest llamaindex-service
docker run -d \
  --name code-nest-llamaindex \
  -p 18080:18080 \
  -e LLAMAINDEX_SERVICE_API_KEY=your-rag-key \
  code-nest-llamaindex:latest
```

如果要和 Java 主系统一起联调，优先使用仓库内置的 Compose：

```bash
docker compose -f docker/ai/docker-compose.yml --env-file docker/ai/.env up -d --build
```

## 本地测试

```bash
python -m unittest discover -s tests -v
```

## 当前实现说明

- 当前版本采用轻量级多字段加权检索，综合正文、Scene、Metadata、ID、精确短语命中和覆盖率进行排序。
- 已导入文档会持久化到 `LLAMAINDEX_DATA_FILE` 指定的 JSON 文件。
- 检索时会同时考虑 `query`、`scene`、`metadataFilters`，命中后返回文本原文、metadata、命中词、打分拆解和主命中字段，便于后台解释为什么召回这条知识。
- 后续可以在不改 Java 调用契约的前提下，替换成真实的 LlamaIndex 索引、Embedding 和向量库实现。

## 后续待办

- 接入真实 LlamaIndex 索引与向量库
- 增加文档分块、Embedding、重建索引能力
- 增加 rerank、引用追踪与召回解释
- 增加更细粒度的观测、缓存与管理后台
