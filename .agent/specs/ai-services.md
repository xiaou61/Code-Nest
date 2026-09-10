---
artifact: spec
status: active
scope: AI 能力
---

# AI 能力

Code-Nest 的 AI 能力由三部分组成：Java 侧统一 AI 运行时模块 `xiaou-ai`（模型接入、Prompt 目录、结构化输出契约、RAG 网关、LangGraph4j 图编排、运行观测、治理总览、黄金样例回归），独立 Python 检索服务 `llamaindex-service`（FastAPI，RAG 唯一外部依赖），以及 `docker/ai` 一键联调编排（MySQL + Redis + RAG 服务 + 后端）。业务模块（模拟面试、求职作战台、慢 SQL 优化、社区摘要、代码评审、成长教练、SRE 根因分析）通过 `xiaou-ai` 的 Service 接口或 `AiExecutionSupport` 复用同一套模型运行时；`xiaou-ai` 自身没有任何 Controller 业务路由、没有 Mapper、没有数据表。全文结论均可指回下方文件路径，未读到的部分标注"未确认"。

## 模块总览

| 模块/目录 | 职责一句话 | 关键入口（包/类/文件） |
| --- | --- | --- |
| xiaou-ai | 统一 AI 运行时门面：模型接入 + Prompt 治理 + 结构化契约 + RAG 网关 + 图编排 + 观测 + 回归 | `xiaou-ai/pom.xml`、`com.xiaou.ai.service.*`（6 接口/6 实现）、`graph/{interview,jobbattle,sql}`、`prompt/*`、`structured/*`、`rag/LlamaIndexClient`、`support/AiExecutionSupport`、`com.xiaou.common.config.AiProperties`、`controller/AiGovernanceController` |
| llamaindex-service | FastAPI 轻量检索 + 知识文档 CRUD 服务，被 Java 侧以 HTTP 调用 | `llamaindex-service/app/main.py`、`app/service.py`、`app/models.py`、`app/config.py`、`app/auth.py`、`Dockerfile`、`requirements.txt` |
| docker/ai | 联调编排：MySQL/Redis/RAG 服务/后端四容器 | `docker/ai/docker-compose.yml`、`docker/ai/.env.example`、`docker/ai/README.md` |

## 模块详情

### xiaou-ai（xiaou-ai）

**职责与边界**
- 单一 AI 运行时：`AiModelFactory` 负责模型构建，`AiExecutionSupport` 负责并发准入/降级/指标，`XxxSceneSupport` 负责 Prompt 组装与结果解析，`XxxGraphRunner` 负责图编排，`LlamaIndexClient` 负责检索。
- 文件构成：`src/main` 125 个 Java + `src/test` 49 个 = 174（glob 清单）。整模块只有 1 个 Controller；`src/main/resources` 下仅 `ai-evals/scene-regression-cases.json`，无 mapper XML、无 SQL。
- 不落库：主代码 grep `@TableName|@Mapper|Mapper|JdbcTemplate` 无业务表命中，唯一外部状态是 Redis（`RedisAiRuntimeMetricsPersistence` → `com.xiaou.common.cache.TextStateStore`）。
- 图编排无条件边：grep `addConditionalEdges` 零命中，13 张图均为静态线性 DAG（面试 4 / 求职 5 / SQL 4）。

**核心领域对象**
| 分组 | 关键类型 | 说明 |
| --- | --- | --- |
| Prompt | `prompt/AiPromptSpec`（record: key/version/systemPrompt/userTemplate/maxCompletionTokens）、`AiPromptCatalog`、`AiPromptGovernance`、`AiPromptSections`、`AiRagQuerySpec(.Catalog)` | 目录汇总 9 个 Specs 持有类、18 个 `AiPromptSpec.of(...)` |
| 结构化输出 | `structured/AiStructuredOutputSpec(.Catalog)`、`AiStructuredOutputValidator`(293 行)、`AiStructuredJsonSchemaBuilder` | 类型/取值域校验，失败即降级 |
| 图编排 | `graph/{interview,jobbattle,sql}/{XxxState,XxxTaskType,XxxGraphRunner}` | LangGraph4j `StateGraph` + `Channels.base` + `AsyncNodeAction` |
| RAG | `rag/LlamaIndexClient`(254 行)、`rag/AiRagRetrievalProfile`、11 个画像常量 | 检索 scene + topK + metadataFilters |
| 运行时 | `support/AiExecutionSupport`、`AiExecutionResult`、`client/AiModelFactory`、`AiChatResult` | 并发、降级、成本估算、模型来源回放 |
| 观测与治理 | `metrics/AiRuntimeMetricsCollector`(781 行, 全模块最大)、`AiMetricsRecorder`、`AiRuntimeMetricsSnapshot`、`dto/governance/AiGovernanceOverviewResponse` | micrometer + Redis 快照 + 质量分/风险项 |
| 回归 | `regression/AiRegressionServiceImpl`(548 行)、`AiRegressionFixtureLoader` | 固定样例手动回归，14 个 scenario / 25 个 case |

**对外接口**
| 入口 | 类型 | 说明 |
| --- | --- | --- |
| `GET /admin/ai/governance/overview` | HTTP（`AiGovernanceController`） | `@RequireAdmin`，返回 `Result<AiGovernanceOverviewResponse>`：工作流清单、覆盖率矩阵、运行洞察、风险项、质量分 |
| `AiInterviewService` | Spring Bean | evaluateAnswer / generateSummary / generateQuestions（含 specializedFocus 重载）/ generateFollowUpQuestion |
| `AiSqlOptimizeService` | Spring Bean | analyzeSql / analyzeSqlV2 / analyzeAndRewriteSqlV2 / rewriteSqlV2 / compareSqlV2 |
| `AiJobBattleService` | Spring Bean | parseJd / matchResume / generatePlan / analyzeTarget / reviewInterview |
| `AiCommunityService`、`AiCodeReviewService` | Spring Bean | 帖子摘要；CodePen 代码评审 |
| `SreInvestigationPlanner`、`SreRcaAnalyzer` | Spring Bean | `xiaou-ai/sre/*Impl` 实现 `xiaou-sre` 端口（pom 依赖 `xiaou-sre`） |
| `AiExecutionSupport.chat / chatResult / chatWithFallback(Result)` | Spring Bean | 供 `xiaou-system`、`xiaou-application` 直连的底层入口 |
| `AiRegressionService` | Spring Bean | listCases / run（由 `xiaou-system` 的 `/admin/ai/config/regression/**` 暴露） |

**数据表**：无自有表。观测快照存 Redis key `xiaou:ai:runtime:metrics`（`AiProperties.metrics.persistence.redis-key`）。

**关键流程**
- 对话/推理（统一 5 段式）：
  `业务模块 Service` → `AiXxxService` → `XxxGraphRunner.invokeGraph`
  → 节点 `retrieve_context`（`LlamaIndexClient.isAvailable()` 为假或检索异常时写入空 RAG_CONTEXT，不中断）
  → 节点业务动作（`XxxSceneSupport` 组装 Prompt → `AiExecutionSupport.chatWithFallback`）
  → `Semaphore.tryAcquire(permitAcquireTimeoutMs)` 准入 → `AiModelFactory.chat`（系统消息 + 用户消息 → OpenAI 兼容模型）
  → `AiJsonResponseParser` 解析 → `AiStructuredOutputValidator` 校验 → DTO / 本地 fallback → `AiMetricsRecorder`（micrometer 计时/计数/Token/成本 + Redis 快照）。
- 上下文：上下文只来自 RAG 检索片段 `LlamaIndexRetrieveResponse.toContextSnippet()`，经 `AiPromptSections.ragSection(...)` 注入 user template；用户可控文本走 `AiPromptSections.text/untrustedFocusSection` 隔离。无多轮会话记忆对象（`未确认`是否有其它记忆组件，grep 未见）。
- 流式：**未实现**。全仓库 grep `SseEmitter|Flux<|StreamingChatModel|text/event-stream` 零命中，全部为阻塞式 `ChatModel.chat`。
- Agent 编排：`xiaou-ai` 内没有 LLM 自主 Agent；LangGraph4j 只做固定管线（如 求职 `parse_jd → match_resume`，SQL `analyze → rewrite → risk_check`）。真正的"Agent"在 `xiaou-system`（`LlmAgentPlanResolver` 直连 `AiExecutionSupport`，路由 `/admin/agent`）。
- 治理总览：读取 `AiRuntimeMetricsCollector.snapshot()`，与 Prompt/Schema/RAG 三个 Catalog 求交集，产出 `qualityScore`（Prompt 25% + Schema 25% + RAG 15% + 运行覆盖 10% + 运行质量 25% − 风险罚分 ≤28）、风险队列（失败率 ≥20%/≥8%、兜底率 ≥20%、解析失败 >0、平均耗时 ≥8000ms）。

**依赖关系**
- 上游：`xiaou-common-core/web/security/cache`、`xiaou-sre`、`micrometer-core`、`langchain4j 1.13.0`（`langchain4j` + `langchain4j-open-ai` + `langchain4j-http-client-jdk`）、`langgraph4j-core 1.8.13`、`hutool-all`、`spring-boot-starter-web`（根 pom 属性 `langchain4j.version=1.13.0`、`langgraph4j.version=1.8.13`）。
- 下游（pom 显式依赖 `xiaou-ai`）：`xiaou-application`、`xiaou-system`、`xiaou-mock-interview`、`xiaou-sql-optimizer`、`xiaou-community`；根 pom `<modules>` 已登记。
- LangChain4j 实际触点：仅 `AiModelFactory`（`OpenAiChatModel`、`ChatRequest/Response`、`SystemMessage/UserMessage`、`JdkHttpClientBuilder` 代理、`OpenAiChatRequestParameters`）与 `AiExecutionSupport`（`TokenUsage`）；**未使用** embedding、tools、memory、retriever 等其它 API。
- LangGraph4j 实际触点：仅 3 个 `*GraphRunner`。

**约束与注意事项**
- 模型接入：provider 仅接受 `openai` / `openai-compatible`，否则 `AiConfigurationException`；`reasoningEffort` 白名单 `none|minimal|low|medium|high|xhigh|max|ultra`；`proxy-url` 仅允许 `http://host:port`（无 userInfo/path/query）；配置缺失（apiKey/baseUrl/chat 模型）时 `isChatAvailable()` 为假，直接走降级。
- 超时/重试：`timeout.connect-ms=10000`、`timeout.read-ms=60000` 传入模型与 JDK HTTP client；`retry.max-attempts=2` 映射为 `maxRetries`。`retry.backoff-ms` 与 `model.embedding` 在代码中 grep 无引用（死配置）。
- 限流：`AiExecutionSupport` 构造时以 `max-concurrent-calls=8` 建 `Semaphore`（运行期改配置不生效），`permit-acquire-timeout-ms=1000` 超时抛 `AiInvocationException("场景 X 当前请求过多")` 并计数 `xiaou.ai.chat.concurrency.rejections`。
- Token 上限：`promptSpec.maxCompletionTokens()` 优先，其次 `AiProperties.model.max-completion-tokens=2048`。
- 降级优先：`chatWithFallback` 在 模型不可用 / 空响应 / 调用异常 / 解析失败 四种情况返回本地兜底并记录 `recordFallback(reason)`；上层业务还会二次判断（如 `AIInterviewerServiceImpl` 用 `isFallback()` 转本地评估）。
- Prompt 治理：key 必须匹配 `^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$`，version 必须 `^v\d+$`，system prompt 禁止模板变量，user template 至少 1 个命名变量（构造期 Assert 失败即启动失败）。
- RAG：`xiaou.ai.rag.enabled` 默认 `false`，`endpoint` 默认 `http://localhost:18080`，`default-top-k=5`，画像内固定 `topK=3`（面试）与 metadata 场景标签。
- 观测：指标前缀 `xiaou.ai.chat.*`（duration/invocations/tokens/cost/fallbacks/concurrency.rejections）与 `xiaou.ai.structured.parse.failures`。
- 密钥位置：全部走环境变量占位（`XIAOU_AI_API_KEY`、`XIAOU_AI_PROXY_URL`、`XIAOU_AI_RAG_API_KEY`），YAML 无明文；`application-sec.yml` 按规则未读取（`未确认`是否含 `xiaou.ai` 覆盖）。

### llamaindex-service（llamaindex-service）

**职责与边界**
- FastAPI（`fastapi==*`、`uvicorn`）+ `pydantic` 模型；对外提供检索与知识文档管理；内部维护单进程文档集合；**不做向量检索**（见下）。
- 代码规模：`app/main.py`(131) + `app/service.py`(545) + `app/models.py`(90) + `app/config.py`(36) + `app/auth.py`(39) + `tests/test_app.py`（未读） + `data/sample-documents.json`。

**核心领域对象**：`RetrieveRequest{query,scene,topK,metadataFilters}`、`RetrieveNode{id,score,text,metadata,matchedTerms,scoreBreakdown,bestMatchField,bestSnippet}`、`RetrieveResponse{query,nodes,fallback}`、`KnowledgeDocument{id,text,scene,metadata}`、`Document*Request/Response`、`HealthResponse{status,authEnabled,documentCount,sceneCount,dataFile}`；内部 `DocumentStore`（`RLock` + 内存 list + JSON 全量持久化）。

**对外接口**（除 `/health` 外均需 `Authorization: Bearer <key>`，且仅在密钥非空时校验）
| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/health` | 免鉴权；返回 authEnabled / documentCount / sceneCount / dataFile |
| POST | `/api/v1/retrieve` | 检索，`topK` 被夹到 `[1, MAX_TOP_K=20]`；无命中原样返回 `fallback=true` |
| POST | `/api/v1/admin/documents/import` | 批量导入，`replace` 控制是否清空 |
| GET | `/api/v1/admin/documents` | 列表（scene 过滤，limit 1..500，默认 100，仅返回 160 字预览） |
| GET | `/api/v1/admin/documents/export` | 导出全文 |
| DELETE | `/api/v1/admin/documents?documentId=` | 单删（不存在 → 404，空 → 400） |
| POST | `/api/v1/admin/documents/batch-delete` | 批删，返回 deleted/missing 两组 id |

**数据表**：无数据库。知识库为单个 JSON 文件（`LLAMAINDEX_DATA_FILE`，默认 `data/knowledge-base.json`；compose 内为 `/app/data/knowledge-base.json`，挂载 `rag_data` 卷）；`data/sample-documents.json` 只读挂载供导入样例。

**关键流程**
- 检索：`query` 去空格 → 按 `scene` 与 `metadataFilters` 过滤候选 → 每个文档建索引（text/scene/metadata/id 四路词项 + 字段级权重）→ 打分 = 精确匹配加分（text 3.6 / scene 2.1 / metadata 1.8 / id 1.2）+ Σ 词项权重×idf（text 1.9 / scene 1.6 / metadata 1.25 / id 0.85，字段细分为 title/question 1.75、keyword 1.7、tag 1.65、summary 1.45、category 1.2）+ 覆盖度 2.4 / 密度 0.8 + metadata filter 0.3×N → 排序取 topK，附 `bestSnippet`。
- 写入：`_ensure_loaded()` 懒加载 JSON → 全量重建 → `_persist()` 覆盖整文件；全程 `RLock` 串行化。
- 分词：ASCII 正则 `[a-z0-9_+#.\-]{2,}` 去停用词；CJK `{2,}` 取 2–4 gram。

**依赖关系**：`requirements.txt` = fastapi / uvicorn / llama-index / httpx；`Dockerfile` = python:3.11-slim，`EXPOSE 18080`，`CMD uvicorn app.main:app --host 0.0.0.0 --port 18080`；被 `docker/ai/docker-compose.yml` 以 context `../../llamaindex-service` 构建，被 Java 侧 `LlamaIndexClient` 以 HTTP 调用。

**约束与注意事项**
- **名实不符**：`llama-index` 已列入 `requirements.txt`，但源码全局 grep `llama_index|import llama|VectorStore|embedding|SentenceSplitter` 零命中；实际检索为自研词法打分（类 BM25），无 embedding、无向量库、无 rerank。`httpx` 同样未见 import（`未确认`其用途）。
- 鉴权可关闭：`LLAMAINDEX_SERVICE_API_KEY` 为空时 `auth_enabled=False`，全部接口（含 admin 文档增删）完全放行；`docker/ai` 用 `XIAOU_AI_RAG_API_KEY` 注入该变量，未设置即无鉴权。
- 无 CORS、无限流、无分页（list 上限 500）、无并发上限。
- 单进程内存态：`RLock` 仅进程内有效，JSON 文件全量覆盖写，多副本/多进程部署会互相覆盖（水平扩展受阻）。
- `LLAMAINDEX_MAX_TOP_K` 默认 20 且 `max_top_k = max(1, ...)`，可上调；召回质量完全依赖词法匹配。

### docker/ai（docker/ai）

**职责与边界**：以 `docker compose -f docker/ai/docker-compose.yml --env-file docker/ai/.env up -d --build` 一键拉起联调栈（README 步骤 3）。

**核心领域对象（compose 服务）**
| 服务 | 镜像/构建 | 端口 | 要点 |
| --- | --- | --- | --- |
| mysql | mysql:8.0 | 3306 | 首次启动导入 `sql/MySql/code_nest.sql`、`code_nest_data.sql`；`mysql_data` 卷；healthcheck mysqladmin ping |
| redis | redis:7-alpine | 6379 | `--appendonly yes`；`redis_data` 卷；healthcheck redis-cli ping |
| llamaindex-service | context `../../llamaindex-service` | 18080 | `LLAMAINDEX_SERVICE_API_KEY=${XIAOU_AI_RAG_API_KEY}`、`LLAMAINDEX_DATA_FILE=/app/data/knowledge-base.json`；`rag_data` 卷 + sample 只读挂载；healthcheck 请求 `/health` |
| code-nest | context `../..`（`docker/Dockerfile`） | 9999 | `SPRING_PROFILES_ACTIVE=docker`；注入 `XIAOU_AI_*`；`XIAOU_AI_RAG_ENDPOINT=http://llamaindex-service:18080`；`depends_on` 三者 `service_healthy` |

**对外接口**：`http://127.0.0.1:9999/api`（后端）、`http://127.0.0.1:18080/health`（RAG）（README「常用地址」）。

**数据表**：不适用（由 mysql 服务承载，脚本来自 `sql/MySql/`）。卷：`mysql_data`、`redis_data`、`rag_data`。

**关键流程**：`compose up` → mysql/redis/llamaindex 通过 healthcheck → code-nest 启动（docker profile）→ 后端以 `XIAOU_AI_RAG_ENDPOINT` 指向容器名 `llamaindex-service` 完成 RAG 联调；样例知识可在后台"导入样例知识"（`/admin/ai/config/rag-service/sample-import`）。

**依赖关系**：依赖仓库内 `llamaindex-service/` 与 `docker/Dockerfile`；`docker/ai/.env.example` 只列 `MYSQL_*`、`XIAOU_AI_PROVIDER/BASE_URL/API_KEY/CHAT_MODEL`、`XIAOU_AI_RAG_ENABLED/RAG_API_KEY`。

**约束与注意事项**
- 密钥：仅通过 `.env` 环境变量传入，容器不挂载 `application-sec.yml`；示例默认模型 `gpt-5.4`、`max-completion-tokens=2048`。
- `docker/ai/.env.example` 未列 `XIAOU_AI_MAX_CONCURRENT_CALLS`、`XIAOU_AI_PROXY_URL`、`XIAOU_AI_METRICS_*`，需自行补充。
- `MYSQL_ROOT_PASSWORD` 在 compose 内带硬编码兜底弱口令（联调用途，值见该文件，此处不复制）。
- `docker-compose.yml` 未设置容器资源限制、未启用 `read_only`、未定义日志轮转。

## 跨模块观察

**共性模式**
- 五段式一致：`业务模块 → Ai*Service → GraphRunner(线性DAG) → SceneSupport(Prompt+解析) → AiExecutionSupport(准入/降级/观测)`；六类场景（面试、求职、SQL、社区、代码评审、成长教练）全部复用，未出现第二个模型客户端。
- 治理双门禁：Prompt 目录 + 结构化输出契约必须同时登记，`AiGovernanceServiceImpl` 用两者交集计算覆盖率和风险分；新增场景不登记即被治理页面判为 HIGH 风险。
- 兜底优先于报错：模型不可用/输出不合法时返回 `fallbackResult`，业务侧再叠加本地规则（如面试分数按风格 `scoreAdjustment` 修正），保证接口不 500。

**跨模块调用关系**
| 调用方模块 | 入口类 | 使用的 AI 能力 |
| --- | --- | --- |
| xiaou-mock-interview | `AIInterviewerServiceImpl`、`QuestionSelectorServiceImpl` | `AiInterviewService`（评价/总结/生成题目/追问） |
| xiaou-mock-interview | `JobBattleServiceImpl` | `AiJobBattleService`（parseJd/matchResume/generatePlan/analyzeTarget/reviewInterview） |
| xiaou-sql-optimizer | `SqlOptimizerServiceImpl` | `AiSqlOptimizeService`（analyzeSql/工作台 V2 全链路） |
| xiaou-community | `CommunityAiSummaryServiceImpl` | `AiCommunityService`（帖子摘要） |
| xiaou-application | `GrowthCodeReviewService` | `AiCodeReviewService`（CodePen 评审） |
| xiaou-application | `LlmGrowthCoachIntentResolver`、`GrowthCoachApplicationService` | 直连 `AiExecutionSupport` + `GrowthCoachPromptSpecs` + `AiStructuredOutputValidator` |
| xiaou-system | `SysAiConfigServiceImpl`、`LlmAgentPlanResolver` | 直连 `AiExecutionSupport`；`AiRegressionService`；`LlamaIndexClient`；三个 Catalog |
| 路由暴露 | `/user/mock-interview`、`/user/job-battle`、`/user/sql-optimizer`、`/community/posts`、`/admin/ai/config/**`、`/admin/ai/governance/overview`、`/admin/agent` | AI 能力无独立业务路由，均挂在业务模块路由下 |

**潜在风险**
1. `llamaindex-service` 引入 `llama-index` 但零使用，检索为自研词法打分；依赖声明与实际能力不符，容易被误读为向量 RAG。
2. 全链路无流式输出（grep 零命中），长回答只能整段等待；`read-ms=60000` 上限下用户体验与连接占用风险并存。
3. `AiExecutionSupport` 的 `Semaphore` 在构造时固定，`retry.backoff-ms`、`model.embedding` 为死配置；运行期调参不会生效。
4. 图编排无条件边，"Agent 编排"实为固定管线；`/admin/agent` 的自主体在 `xiaou-system` 而非 `xiaou-ai`，模块职责描述与实际边界存在偏差。
5. `xiaou.ai.rag.enabled` 默认 `false`，而 `docker/ai` 默认 `true`：环境间 RAG 行为不一致，未确认 `application-prod.yml` 的取值。
6. RAG 服务无鉴权时会放行全部 admin 文档接口，且单 JSON 文件 + 进程内锁难以水平扩展与多实例一致性。
7. 两处 AI 管理入口（`/admin/ai/governance` 在 `xiaou-ai`，`/admin/ai/config` 在 `xiaou-system`）职责重叠，需明确归属。

**与 `.agent/rules/always.md` 的对照**
- 一致：AI 逻辑落入独立模块 `xiaou-ai`，根 pom `<modules>` 已登记、`xiaou-application` 已加依赖；治理接口统一返回 `Result<T>` 并使用 `@RequireAdmin`；统一上下文 `/api`、端口 9999、Java 17、Spring Boot 3.4.4 与 `langchain4j.version=1.13.0` / `langgraph4j.version=1.8.13` 属性一致（根 `pom.xml` 52/57/63/64 行，`revision=v2.5.8`）。
- 需注意：rules 未提及 `llamaindex-service` 与 `docker/ai` 的运维约束（鉴权默认关闭、单文件持久化、弱口令兜底），建议补入常驻规范。
- `未确认`：`application-sec.yml` 是否覆盖 `xiaou.ai`；`application-prod.yml` 的 `xiaou.ai.rag.enabled` 取值；`llamaindex-service/tests/test_app.py` 覆盖面；`AiRuntimeMetricsCollector` 的 781 行实现细节（仅读其接口使用点）。
