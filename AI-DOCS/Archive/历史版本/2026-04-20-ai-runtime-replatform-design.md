# AI Runtime Replatform Design

**目标**

将项目内现有基于 Coze 工作流的 AI 能力，彻底重构为基于 `LangChain4j + LangGraph4j + LlamaIndex` 的统一 AI 架构，覆盖模拟面试、求职作战台、SQL 优化、社区摘要等现有场景，并为后续知识库增强、可观测性治理、模型切换、多阶段 Agent 编排提供稳定底座。

**范围**

- 完整移除 Coze SDK、Coze 配置、Coze 工作流枚举、Coze 调用工具类。
- 保留 `xiaou-ai` 作为统一 AI 门面模块，但彻底重写内部实现。
- 业务模块继续依赖 `xiaou-ai` 场景服务接口，不直接依赖底层模型 SDK。
- 引入独立的知识库检索能力，优先以独立服务方式接入 LlamaIndex。

**非目标**

- 本次不做前端页面改版。
- 本次不引入多租户模型配置平台。
- 本次不实现可视化 Prompt Studio 或工作流编辑器。

---

## 1. 现状问题

当前 AI 链路为：

`业务模块 -> xiaou-ai 场景服务 -> CozeUtils -> CozeWorkflowEnum -> Coze SDK`

存在以下问题：

1. AI 场景与 Coze 工作流 ID 强耦合，无法灵活切换模型和执行策略。
2. `xiaou-common` 直接承载 Coze SDK 与配置，公共层被第三方 AI 平台污染。
3. 场景服务以“参数 Map + JSON 手工解析”为主，类型约束弱，输出稳定性依赖外部工作流格式。
4. 多阶段场景没有真正的状态机与图编排抽象，复杂流程都被压在 service 方法里。
5. 当前知识库能力几乎缺位，无法统一为面试、求职作战台、SQL 优化等场景提供检索增强。
6. `application.yml` 中存在明文 Coze Key，存在明显安全风险。

---

## 2. 目标架构

目标架构分为 4 层：

### 2.1 场景门面层

位置：`xiaou-ai`

职责：

- 对外保留稳定的场景接口，例如：
  - `AiInterviewService`
  - `AiJobBattleService`
  - `AiSqlOptimizeService`
  - `AiCommunityService`
- 对业务模块隐藏底层框架细节。
- 负责将业务 DTO 映射到统一 AI 请求对象。

### 2.2 模型与提示词层

位置：`xiaou-ai` 内部新包，例如：

- `com.xiaou.ai.model`
- `com.xiaou.ai.prompt`
- `com.xiaou.ai.support`

职责：

- 使用 `LangChain4j` 封装：
  - `ChatModel`
  - `StreamingChatModel`
  - `EmbeddingModel`
  - `AiServices`
  - 结构化输出
  - 工具调用
- 统一管理模型提供商、超时、重试、温度、最大 token 等参数。
- 统一封装 Prompt 模板与结构化结果解析。
- Prompt 采用 `AiPromptSpec + PromptTemplate` 组织，显式区分 `system prompt` 与 `user template`。
- 所有 Prompt 必须具备 `key + version` 元信息，便于评审、测试、观测与后续回归评测。
- scene/service 不再直接内联长 Prompt 文本，改为只负责变量装配与结果解析。

### 2.3 图编排层

位置：`xiaou-ai` 内部新包，例如：

- `com.xiaou.ai.graph`
- `com.xiaou.ai.graph.interview`
- `com.xiaou.ai.graph.jobbattle`
- `com.xiaou.ai.graph.sql`

职责：

- 使用 `LangGraph4j` 承载多阶段、有状态、有分支的 AI 流程。
- 负责节点状态传递、条件分支、失败重试、人工兜底、checkpoint。
- 仅在复杂链路使用图编排，避免简单场景过度工程化。

### 2.4 知识检索层

位置：独立服务，建议新增目录：

- `llamaindex-service/`

职责：

- 使用 `LlamaIndex` 承担：
  - 文档切分
  - 索引构建
  - 检索与召回
  - 可选 rerank
  - 引用片段返回
- Java 主项目通过 HTTP/gRPC SDK 方式调用，不将 LlamaIndex 作为 Java 本地实现强塞入 `xiaou-ai`。

原因：

- LlamaIndex 当前主生态仍是 Python / TS，独立服务更符合官方生态与后续演进路径。
- 文档导入、重建索引、异步解析、向量存储迁移更适合独立部署。

---

## 3. 模块拆分方案

### 3.1 `xiaou-common`

删除：

- `CozeConfig`
- `CozeUtils`
- `CozeWorkflowEnum`
- Coze SDK Maven 依赖

新增：

- `AiProviderProperties`
- `AiRetryProperties`
- `AiSecurityProperties`
- 与 AI 相关但不包含业务语义的基础异常、请求上下文、trace 封装

原则：

- 公共模块只保留“通用 AI 基础能力配置”，不保留“特定平台调用代码”。

### 3.2 `xiaou-ai`

保留：

- 现有场景 service 接口与主要 DTO

重构：

- 替换全部 `CozeResponseParser`
- 替换全部 `Map<String, Object>` 风格工作流调用
- 新增：
  - `config`
  - `client`
  - `prompt`
  - `structured`
  - `graph`
  - `rag`
  - `scene`
  - `metrics`

### 3.3 业务模块

涉及模块：

- `xiaou-mock-interview`
- `xiaou-sql-optimizer`
- `xiaou-community`
- `xiaou-application`

改造原则：

- 尽量不改 controller 对外协议
- 尽量不改数据库结构
- 优先替换 service 内部依赖与调用路径

---

## 4. 场景落地映射

### 4.1 社区摘要

类型：单步场景

实现策略：

- 直接使用 `LangChain4j AiServices + 结构化输出`
- 可选接入 `LlamaIndex` 检索帖子历史上下文、标签解释、社区术语库

不需要 `LangGraph4j`。

### 4.2 模拟面试

类型：典型多阶段场景

图节点建议：

1. 题目生成
2. 上下文检索
3. 回答评价
4. 追问决策
5. 追问生成
6. 总结生成

需要 `LangGraph4j` 管理状态：

- 面试方向
- 难度
- 风格
- 已回答轮次
- 追问次数
- 历史 QA
- 总分

`LlamaIndex` 用于：

- 面试题知识语料
- 参考答案片段召回
- 方向知识点定义

### 4.3 求职作战台

类型：多阶段场景

图节点建议：

1. JD 解析
2. 目标能力提取
3. 简历匹配
4. 差距归因
5. 行动计划生成
6. 面试复盘

`LlamaIndex` 用于：

- 岗位能力画像知识库
- 常见 JD 模板
- 简历优化范式库
- 面试复盘建议知识库

### 4.4 SQL 优化

类型：多阶段场景

图节点建议：

1. SQL/EXPLAIN 诊断
2. 优化建议生成
3. 重写 SQL
4. 收益对比
5. 风险检查

`LlamaIndex` 用于：

- MySQL 优化知识库
- 索引设计规范
- 常见慢 SQL 模板
- EXPLAIN 字段释义

---

## 5. 配置与密钥策略

统一配置前缀建议改为：

```yaml
xiaou:
  ai:
    provider: openai
    base-url: ${AI_BASE_URL:}
    api-key: ${AI_API_KEY:}
    model:
      chat: ${AI_CHAT_MODEL:gpt-4o-mini}
      embedding: ${AI_EMBEDDING_MODEL:text-embedding-3-small}
    timeout:
      connect-ms: 10000
      read-ms: 60000
    retry:
      max-attempts: 2
      backoff-ms: 1000
    rag:
      enabled: true
      endpoint: ${LLAMAINDEX_BASE_URL:http://localhost:18080}
      api-key: ${LLAMAINDEX_API_KEY:}
```

必须落实：

1. 删除明文密钥。
2. 全部密钥走环境变量。
3. 生产环境支持 provider 切换。
4. 检索服务与模型服务分别配置。

---

## 6. 错误处理与降级

### 6.1 分层错误

- `AiConfigurationException`
- `AiInvocationException`
- `AiStructuredOutputException`
- `AiRetrievalException`
- `AiGraphExecutionException`

### 6.2 降级原则

- 模型不可用：返回本地规则降级结果
- 检索不可用：退化为纯模型生成，但记录降级标记
- 图执行中节点失败：允许部分节点 fallback，不直接中断全链路
- 结构化解析失败：允许一次修复性重试，再降级

### 6.3 响应语义

保留现有 DTO 中的 `fallback` 语义，但将原因写清楚，例如：

- `MODEL_UNAVAILABLE`
- `RAG_UNAVAILABLE`
- `GRAPH_INTERRUPTED`
- `OUTPUT_INVALID`

---

## 7. 可观测性

新增埋点：

- 模型调用耗时
- token 使用量
- 图节点执行耗时
- 检索命中数
- fallback 比例
- 结构化解析失败率

接入点：

- `Micrometer`
- 结构化日志
- 业务 traceId

日志要求：

- 不记录明文 API Key
- 不记录完整敏感输入
- 长文本只打摘要与长度
- AI 指标需要补充 `prompt_key`、`prompt_version` 标签，支持按 Prompt 版本观察波动。

---

## 8. 测试策略

测试分层：

1. 单元测试
   - Prompt 变量装配
   - Prompt 模板渲染
   - 输出解析
   - 节点状态转换
2. 集成测试
   - 场景 service 到 graph 的调用链
   - RAG client 与检索接口交互
3. 契约测试
   - 结构化输出 schema
   - LlamaIndex 检索响应格式
4. 回归测试
   - 模拟面试
   - SQL 优化工作台
   - 社区摘要
   - 求职作战台

---

## 9. 分阶段实施顺序

### Phase 1：底座替换

- 移除 Coze 相关配置和依赖
- 建立新的 AI 配置、模型客户端、基础异常和结构化输出设施

### Phase 2：单步场景迁移

- 社区摘要

### Phase 3：复杂场景迁移

- 模拟面试
- SQL 优化
- 求职作战台

### Phase 4：知识库接入

- 建立 LlamaIndex 检索服务
- 将复杂场景逐步接入 RAG

### Phase 5：清理与治理

- 删除旧测试、旧文档、旧配置
- 补齐观测指标与回归测试

---

## 10. 验收标准

1. 仓库中不再存在 Coze SDK、Coze 配置、Coze 工具类、Coze 测试。
2. 所有现有 AI 场景仍可通过原有业务 API 使用。
3. `xiaou-ai` 内部实现完成 `LangChain4j + LangGraph4j + LlamaIndex` 分层。
4. 配置中不存在明文 AI 密钥。
5. 关键场景具备可运行测试与基础观测指标。
