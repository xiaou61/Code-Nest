---
artifact: reference
status: active
updated: 2026-10-09
---

# AgentScope Java 与 DeepSeek 接入事实（2026-10-09 实测）

本文记录**实测与官方文档核实过**的接入事实，供 AI 相关工作项直接引用。未经核实的内容不写进来。

## 一、DeepSeek 侧（实测）

### 模型与能力

`GET https://api.deepseek.com/models` 返回的当前可用模型 id（2026-10-09）：

| 模型 id | 名称 | 上下文 | 最大输出 | 输入模态 |
| --- | --- | --- | --- | --- |
| `deepseek-flash` | DeepSeek-V4.1-Flash | 1,048,576 | 393,216 | 文本 + 图像 |
| `deepseek-v4-pro` | DeepSeek-V4-Pro | 1,048,576 | 393,216 | 文本 |

- `deepseek-chat` / `deepseek-reasoner` **已不是当前 id**；旧 id `deepseek-v4-flash` 仍被接受但路由到 V4.1-Flash。
- `deepseek-flash` 的 `effort` 支持 `low` / `high` / `max`，**默认 `high`**。
- `deepseek-flash` 是唯一支持图像输入的模型。
- 认证：`Authorization: Bearer $DEEPSEEK_API_KEY`；基址 `https://api.deepseek.com`，与 OpenAI 格式兼容（另有 `/anthropic` 兼容端点）。

### 实测结论

1. **思考模式默认开启，且推理 token 计入 `max_tokens`。** 用 `max_tokens: 100` 请求一句普通问答时，`completion_tokens` 为 100、其中 `reasoning_tokens` 为 100、**`content` 为空字符串**；同一请求把 `max_tokens` 提到 3000 后正常返回（推理 74 token，正文 46 字）。
   - **后果**：`max_tokens` 给小了会拿到"成功但内容为空"的响应。这不会报错，只会静默产出空结果。
   - **必须做的检查**：调用后校验内容非空；`max_tokens` 要给足推理开销（判分与出题这类任务建议不低于 2000）。
2. **`response_format: {"type": "json_object"}` 可用且有效。** 实测按"为知识点生成一道选择题，返回含 stem/options/answer_index/difficulty 的 json"的提示词请求，返回内容 `json.loads` 通过，四个字段齐全、内容合理（推理 271 token，正文 140 字）。
   - **限制**：DeepSeek 只支持 `json_object`，**不支持 `json_schema`，也没有 strict 模式**；官方指南要求提示词里出现 "json" 字样，并明确警告"可能偶尔返回空内容"。

## 二、AgentScope Java 侧（官方文档核实）

### 没有 DeepSeek 专用 starter

Maven Central 上 **不存在任何 `io.agentscope:*deepseek*` 构件**（目录列表、search.maven.org 查询、三个可能的构件名探测均为 404）。DeepSeek 由 **OpenAI 扩展**承载：

- `io.agentscope:agentscope-extensions-model-openai`（模型扩展）
- `io.agentscope:agentscope-openai-spring-boot-starter`（Spring Boot starter）

文档给出的接入方式是 `ModelRegistry` 的 id 前缀写法（非 Spring 路径）：

```java
ReActAgent agent = ReActAgent.builder()
    .name("assistant")
    .model("deepseek:deepseek-flash")
    .build();
```

该路径读取环境变量 `DEEPSEEK_API_KEY`，默认基址 `https://api.deepseek.com`，并把 `deepseek:` 前缀剥掉作为真实 model 名。**没有文档化的 `agentscope.deepseek.*` 配置命名空间**——用 Spring starter 时要按 OpenAI 提供方配置并显式设 `baseUrl("https://api.deepseek.com")` 与 `DeepSeekFormatter`。

### 结构化输出走回退路径

AgentScope 的提供方能力表明确写着：OpenAI 系（含 DeepSeek/GLM 用其 formatter）`supportsNativeStructuredOutput()` 为 **`false`**，标注"不支持，自动回退"。也就是说结构化输出由框架**注入一个合成工具、靠强制工具调用**带回数据，而不是走 provider 原生 schema。DeepSeek 页面结论一致。

### 一个会打到架构上的限制

DeepSeek 页面明确提示：当**同时**存在工具与结构化输出时，DeepSeek 会"优先满足 `response_format` 约束而跳过工具调用"，因此需要把 `nativeStructuredOutputWithTools(false)`。

**后果**：我们原先设想的「智能体一边调工具（查掌握度、前置图）一边返回结构化结果（学习路径）」这个组合，在 DeepSeek 上是**不成立的**。要么先调工具拿到数据、再用第二次调用产出结构化结果；要么学习路径的决策留在确定性代码里（这与之前的判断一致）。

### 思考模式的另一处影响

推理是同一模型上的开关（`enableThinking(true)`），流式输出会把 `ThinkingBlockDeltaEvent` 与 `TextBlockDeltaEvent` 分开推送。**前端的流式渲染必须能分别处理这两类事件**，否则思考内容与正式回答会混在一起。

## 三、密钥与配置边界

- DeepSeek API 密钥存于本机 Windows 凭据管理器（`Codex/quick-server/paideia-deepseek`），**不进仓库、不写进任何被跟踪的文件**。
- 应用侧计划通过环境变量 `DEEPSEEK_API_KEY` 注入；本地开发配置放被 `.gitignore` 忽略的 `backend/config/application-local.yml`。
- 密钥曾在会话中以明文出现过，若认为会话记录敏感，应轮换。

## 来源

- DeepSeek 官方 API 文档：`https://api-docs.deepseek.com/api/create-chat-completion`、`https://api-docs.deepseek.com/quick_start/pricing`、`https://api-docs.deepseek.com/updates`、`https://api-docs.deepseek.com/guides/json_mode`
- AgentScope Java 文档：`https://java.agentscope.io/v2/en/docs/building-blocks/model`、`https://java.agentscope.io/v2/en/integration/model/deepseek`、`https://java.agentscope.io/v1/en/docs/task/model`
- Maven Central：`https://repo1.maven.org/maven2/io/agentscope/`
- 实测：`GET /models`、两次 `POST /chat/completions`（普通与 `json_object`），2026-10-09。
