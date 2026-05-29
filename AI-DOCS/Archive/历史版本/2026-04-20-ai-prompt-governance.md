# AI Prompt 治理规范

## 目标

为 `xiaou-ai` 内所有 AI 场景建立统一的 Prompt 工程规范，解决过去“系统提示词散落在 scene / service 中、变量拼接方式不统一、缺乏版本与测试约束”的问题，让 Prompt 能够像普通业务代码一样被评审、测试、观测和演进。

## 官方实践依据

本规范优先参考官方一手资料，总结为项目内可执行约束：

1. LangSmith Prompt Engineering Quickstart
   - 链接：<https://docs.langchain.com/langsmith/prompt-engineering-quickstart>
   - 要点：把角色、任务、输出约束写清楚；把上下文和用户输入用清晰边界分隔；优先使用模板化 Prompt，而不是动态字符串拼接。
2. LangChain4j PromptTemplate
   - 链接：<https://docs.langchain4j.dev/apidocs/dev/langchain4j/model/input/PromptTemplate.html>
   - 要点：使用命名变量模板渲染 Prompt，减少位置参数和 `formatted()` 带来的维护成本。
3. LlamaIndex Prompt / RichPromptTemplate
   - 链接：<https://developers.llamaindex.ai/python/framework/module_guides/models/prompts/>
   - 要点：Prompt 中的上下文片段、用户问题、检索内容应通过显式变量注入，便于 RAG 场景复用。
4. OpenAI Prompting Guide
   - 链接：<https://platform.openai.com/docs/guides/prompting>
   - 要点：指令前置、上下文分块、输出格式明确、避免混合多种不稳定要求。
5. OpenAI Structured Outputs
   - 链接：<https://platform.openai.com/docs/guides/structured-outputs>
   - 要点：当结果需要给程序消费时，必须把输出 schema 显式化，并保留解析失败时的降级路径。

## 项目内规范

### 1. system prompt 与 user template 必须分离

- system prompt 只承载角色、任务、输出约束、评分规则、JSON 结构说明。
- user template 只承载业务变量与上下文数据，不再内联业务逻辑规则。
- 禁止在 scene / service 里直接定义长段 `SYSTEM_PROMPT` 字符串。

### 2. Prompt 必须显式命名并版本化

- 统一使用 `AiPromptSpec` 描述 Prompt。
- `key` 采用业务域前缀命名，例如：
  - `mock_interview.evaluate_answer`
  - `job_battle.plan_generate`
  - `sql_optimize.rewrite`
- `version` 当前显式保留 `v1 / v2`，后续版本升级必须保留旧版本痕迹，避免静默改行为。

### 3. 变量注入必须使用命名模板

- 统一使用 `PromptTemplate` 渲染 user template。
- 所有变量通过 `Map<String, Object>` 显式传递。
- 禁止继续使用长段 `"""...""".formatted(...)` 组织主 Prompt。
- 可选上下文统一通过公共片段注入，例如 `AiPromptSections.ragSection(...)`。

### 4. Scene 只负责变量装配与结果解析

- Scene 的职责：
  - 组装 Prompt 变量
  - 调用 `AiExecutionSupport`
  - 解析结构化结果
  - 处理 fallback
- Prompt 文案本身统一沉淀在 `xiaou-ai/src/main/java/com/xiaou/ai/prompt/`

### 5. RAG 检索 query 也必须模板化治理

- LlamaIndex 检索 query 本质上也是 Prompt，不能继续散落在 graph runner 的 `formatted()` 字符串里。
- 统一使用独立的 `AiRagQuerySpec` 定义：
  - `key`
  - `version`
  - `template`
- graph runner 只负责装配检索变量和调用 `LlamaIndexClient`，不再内联长段检索描述文案。
- 这样可以保证：
  - 知识库召回 query 可版本化
  - query 可回归测试
  - 后续做召回调优时可以准确定位是哪条 query 在变化

### 5.1 检索上下文必须显式分隔

- 参考 OpenAI 对“使用明确分隔符区分上下文块”的建议，RAG 召回内容必须通过统一片段工具注入，而不是直接拼在问题后面。
- 当前统一使用 `AiPromptSections.ragSection(...)`，并将召回内容包装在 `<knowledge_context>...</knowledge_context>` 中。
- Prompt 中如果引用了知识库片段，必须同时告诉模型：
  - 这是补充参考，不是绝对事实
  - 如果与当前任务无关，应当忽略
- 这样做的目的：
  - 降低用户输入、业务变量、召回片段三者混在一起造成的歧义
  - 降低 prompt injection 和“把检索内容误当系统指令”的风险

### 6. 输出格式必须结构化且可回退

- 当前项目仍以 JSON-only 输出约束为主。
- JSON 解析成功后，仍需通过统一结构化校验器验证字段、类型、枚举与取值范围，不能把“能 parse”视为“结构正确”。
- Scene 在解析失败时必须：
  - 记录结构化解析失败指标
  - 返回明确 fallback 结果
- 后续若升级到更强的原生 structured output 能力，也必须保留 fallback 语义。

### 7. Prompt 与 RAG query 需要最小测试保护

- 至少补一层模板渲染测试，验证：
  - 关键变量能够注入
  - 可选变量缺失时不会渲染出 `null`
  - Prompt key / version 可追踪
- 对 RAG query 同样补渲染测试，验证：
  - queryId 唯一
  - 模板变量完整
  - graph 场景缺省字段不会把 `null` 带进检索请求
- 多阶段图场景测试继续覆盖 scene -> graph 调用链，确保 Prompt 抽离后外部行为不回退。

### 8. Prompt 必须进入观测体系

- AI 调用指标必须带上：
  - `scene`
  - `prompt_key`
  - `prompt_version`
- 至少记录：
  - 调用次数
  - 调用耗时
  - fallback 次数
  - 结构化解析失败次数

## 当前落地结构

```text
xiaou-ai/src/main/java/com/xiaou/ai/
├── metrics/
│   └── AiMetricsRecorder.java
├── prompt/
│   ├── AiPromptSpec.java
│   ├── AiPromptSections.java
│   ├── AiRagQuerySpec.java
│   ├── community/CommunityPromptSpecs.java
│   ├── interview/InterviewPromptSpecs.java
│   ├── interview/InterviewRagQuerySpecs.java
│   ├── jobbattle/JobBattlePromptSpecs.java
│   ├── jobbattle/JobBattleRagQuerySpecs.java
│   ├── sql/SqlOptimizePromptSpecs.java
│   └── sql/SqlOptimizeRagQuerySpecs.java
├── rag/
│   ├── AiRagRetrievalProfile.java
│   ├── interview/InterviewRagRetrievalProfiles.java
│   ├── jobbattle/JobBattleRagRetrievalProfiles.java
│   └── sql/SqlRagRetrievalProfiles.java
├── structured/
│   ├── AiStructuredOutputValidator.java
│   ├── AiStructuredOutputSpec.java
│   ├── AiStructuredOutputSchemaExporter.java
│   ├── community/CommunityStructuredOutputSpecs.java
│   ├── interview/InterviewStructuredOutputSpecs.java
│   ├── jobbattle/JobBattleStructuredOutputSpecs.java
│   └── sql/SqlStructuredOutputSpecs.java
├── scene/
└── support/AiExecutionSupport.java
```

## 新增治理闭环

1. Prompt 契约：
   `AiPromptSpec` 负责 Prompt 命名、版本、模板变量治理。
2. 检索契约：
   `AiRagQuerySpec` 负责检索 query 模板，`AiRagRetrievalProfile` 负责 scene、topK 和 metadata filters。
   检索结果进入 Prompt 时，统一通过 `<knowledge_context>` 分隔符注入，避免上下文边界漂移。
3. 输出契约：
   `AiStructuredOutputSpec` 负责把 Prompt 与结构化 schema 一一绑定。
   当前同一份契约既可以做运行时校验，也可以导出 JSON Schema。
4. 回归保护：
   Prompt、RAG query、Structured Output 三层都已补 catalog 与 fixture 测试，减少“文案改了但解析没跟上”的风险。

## 后续建议

1. 若后面引入 LangChain4j 更原生的 system/user message 调用方式，可直接复用现有 `AiPromptSpec`，无需再次把 Prompt 从业务代码里拆一遍。
2. 若后面引入更严格的 structured outputs，可把 `AiPromptSpec` 与 JSON schema 一起治理。
3. 若后面继续提升知识库召回质量，可把 `AiRagQuerySpec` 与 metadata filter 策略一起版本化。
4. 若后面要做 Prompt 回归评测，可基于当前 `prompt_key + prompt_version` 直接建立样本集。
