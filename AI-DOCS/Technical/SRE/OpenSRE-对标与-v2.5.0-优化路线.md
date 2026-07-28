# OpenSRE 对标与 Code-Nest v2.5.0 优化路线

> 调研日期：2026-07-27
>
> 对标对象：`Tracer-Cloud/opensre` 官方仓库 `main`，核对提交
> `a449d548e0a3282c76c74b0cc2c44432ee176e8d`。
>
> 适用约束：一台服务器、QQ 邮件作为紧急通知、AI 只读、暂不自动执行动作。

## 1. 执行结论

继续使用 Java，不新建 TS SRE 后端，也不把 OpenSRE 整体嵌入 Code-Nest。

OpenSRE 当前最值得借鉴的不是知识图谱，而是四个工程闭环：

1. 调查有明确阶段、上限和停止条件；
2. 会话和调查结果可以恢复、追溯；
3. 工具声明证据类型及副作用等级；
4. 用户反馈可以转成回归评测用例。

Code-Nest 的优势是事故、证据、权限、事务 Outbox 和管理后台已经在同一个 Java
业务边界内。单服务器阶段把这套主链拆成 TS/Python 服务，会增加部署、鉴权、数据一致性和
故障恢复成本，却不会提高告警可靠性。Python 以后可以作为离线评测或故障模拟器，但不应成为
告警接收和事故状态机的必经路径。

## 2. OpenSRE 当前源码事实

以下均为官方仓库当前代码或官方文档中的事实，不采用第三方宣传材料。

| 能力 | 源码事实 | 对比意义 |
| --- | --- | --- |
| 项目成熟度 | README 标记为 `Public Alpha`，包版本为 `0.1`，要求 Python 3.12+ | 可借鉴机制，不应直接承担 Code-Nest 的核心告警热路径 |
| 调查管线 | 调查分为连接解析、告警提取、计划、ReAct 取证、诊断、交付六阶段 | Code-Nest 当前是固定证据快照 + 单次结构化 RCA，阶段粒度较少 |
| 有界 Agent 循环 | 工具 schema 最多 32 个、循环最多 20 次；重复调用复用缓存，连续两次停滞后移除工具并强制收敛 | 未来做动态只读取证时必须先具备同类预算和停滞保护 |
| 会话持久化 | JSONL Session Repo 支持最近会话、调查历史、按前缀恢复调查；REPL 暴露 `/sessions` 和 `/resume` | Code-Nest 页面刷新后应继续看到原 RCA 和调查轨迹 |
| 工具元数据 | `ToolMetadata` 声明 evidence type 和 `none/read_only/mutating/external` 副作用等级 | 与 Code-Nest 已有 Agent 风险模型方向一致 |
| 反馈闭环 | `partial/inaccurate` 结果会按 retrieval/reasoning/tool/routing 等 taxonomy 记录为 miss，并可导出为 benchmark case | Code-Nest v2.5.0 已补齐评价、缺口分类、追加审计，并支持将指定反馈修订人工提升为不可变评测用例 |
| 评测体系 | benchmark 保存报告、单 case 原始产物和代码/配置/模型 provenance；官方文档当前提到 452 个 CloudOpsBench 场景 | v2.5.0 已实现手动离线回放、逐用例产物、模型 provenance 和固定规则评分；场景规模、代码提交 provenance 与 CI 门禁仍未对齐 |
| 数据安全 | README 声明可逆标识符掩码、结构化审计 Prompt、本地 transcript；遥测默认 opt-out | Code-Nest 已有双层脱敏，但仍需持续检查持久化报告和日志边界 |

### 已核对的反向证据

- 当前 README 明确说明旧 graph/chain framework layers 已移除。
- 当前仓库对 `neo4j`、`episodic memory`、`knowledge graph` 的源码全文检索均无结果。
- 因此，现阶段以 Neo4j 为核心对标 OpenSRE 属于过时判断，不能作为引入新基础设施的依据。

## 3. Code-Nest 当前位置

### 已具备

- Prometheus、Alertmanager、Grafana、node-exporter、blackbox-exporter；
- Alertmanager Webhook 幂等入库、事故聚合、状态机、事务 Outbox；
- Prometheus/Loki 固定白名单证据采集和失败降级；
- 受限调查上下文、双层脱敏、只读 AI RCA、有效证据引用校验；
- 管理端事故队列、时间线、证据、RCA、确认和关闭；
- 管理员权限、Agent 工具权限和禁止自动执行的硬边界。

### 本次 v2.5.0 已补齐

1. `sre_investigation_run` 持久化每次调查的来源、操作者、状态、输入计数、结论和报告；
2. `sre_investigation_step` 持久化上下文加载、模型上下文构建、只读分析、报告校验；
3. AI 或确定性降级报告都可恢复，异常只保存受控失败码，不保存异常正文；
4. 新增管理员历史和详情查询，路径参数及数量有边界；
5. 管理端打开事故时恢复最近报告，可切换历史版本并查看调查轨迹；
6. 原 `POST /admin/sre/incidents/{id}/rca` 契约不变，Admin Agent 仍可兼容调用。
7. `sre_investigation_feedback` 以追加修订保存准确度、缺口分类、管理员备注、期望结论、
   评价人和评价时间，不覆盖旧评价；
8. 管理端可恢复和编辑当前反馈，`PARTIAL/INACCURATE` 必须给出缺口与期望结论；
9. 每次运行可导出 `code-nest.sre.rca-eval.v1` 样本。样本只包含结构化报告与评价结论，
   不包含模型输入、原始日志、异常正文、管理员备注或身份。
10. `sre_investigation_artifact` 以“一次 run 一份”的唯一约束固化模型实际接收的脱敏上下文，
    上下文继续受 60,000 字符上限约束，并保存 SHA-256、裁剪状态和创建时间；
11. artifact 同时记录 Prompt ID、结构化输出 Schema ID、provider、配置模型、上游返回的实际
    模型以及 `SUCCESS / MODEL_UNAVAILABLE / EMPTY_RESPONSE / INVOCATION_EXCEPTION /
    PARSER_FAILURE` 调用结果；
12. RCA 详情接口和管理端只展示 provenance 摘要，不返回 `context_json`；明显未脱敏的 Bearer、
    常见云密钥和敏感 JSON 字段会被持久化边界拒绝。
13. 新增 `sre_rca_evaluation_case`，管理员只能从指定终态 run、对应 artifact 和指定反馈修订
    显式提升；`source_feedback_id` 唯一，重复提升返回同一不可变用例；
14. 新增 `sre_rca_evaluation_run` 与 `sre_rca_evaluation_result`，保存单用例或当前全部用例的
    手动回放、候选报告、Prompt/Schema、provider、配置/实际模型、调用结果及聚合得分；
15. 线上 RCA 与离线回放共用 `SreRcaAnalyzer`，保证 Prompt、结构化契约、证据引用白名单、
    危险建议降级和确定性 fallback 语义一致；
16. 透明评分不调用模型裁判：期望结论 Dice 相似度 50、证据引用召回 25、严重度一致 15、
    只读安全 10。总分至少 70、模型成功返回 AI 报告且只读安全通过时才算通过；
17. 管理端支持提升当前反馈修订、查看用例目录、单用例/全量手动回放、历史聚合和逐项评分。
    API DTO 不包含冻结上下文或基准报告 JSON，失败记录不保存异常正文；
18. artifact 提升和实际回放都会重新执行大小、哈希和未脱敏凭据校验，防止数据库内容被改写
    并重算哈希后进入模型。

## 4. 差距和优先级

这是结合当前单机约束得出的工程推断，不是 OpenSRE 官方结论。

| 优先级 | 差距 | 当前风险 | 建议 |
| --- | --- | --- | --- |
| P0 | RCA 运行不可恢复、不可审计 | 刷新即丢、无法判断 AI/降级过程 | 本次已完成 |
| 已完成 | 没有准确/部分准确/不准确反馈 | 无法知道模型是否真的帮助定位 | v2.5.0 已增加评价、缺口分类、追加审计和期望结论 |
| 已完成 | 缺少可控离线回放与可比较评分 | 改 Prompt/模型后无法量化差异 | v2.5.0 已实现人工提升、单用例/全量手动回放、逐用例透明评分和聚合得分；不从生产事故自动调用模型 |
| P1 | 缺少发布和 Runbook 证据 | 根因容易停留在指标/日志层 | 先接入最近发布、版本和只读 Runbook，仍走证据表 |
| P1 | SRE 自身指标不完整 | 无法发现调查失败率、降级率和积压 | 暴露 run duration、status、generation mode、outbox backlog 指标 |
| P2 | 固定查询不能按假设追加取证 | 复杂事故证据覆盖有限 | 只在白名单工具上实现最多 3-5 轮的有界调查，不开放任意命令或查询 |
| P2 | 单机监控无法发现整机失联 | 服务器掉电时同机告警一起失效 | 增加异地 HTTP 探针，优先级高于自动修复 |
| P3 | 服务依赖/影响面仍靠标签推断 | 跨模块事故难评估影响 | 先用静态服务目录和依赖表；规模证明需要后再评估图数据库 |

## 5. 下一步推荐顺序

### 5.1 RCA 反馈与回归评测

反馈、精确输入、不可变用例与手动回放已经落地：

- `ACCURATE / PARTIAL / INACCURATE` 评价；
- `RETRIEVAL_GAP / REASONING_GAP / TOOL_FAILURE / ROUTING_GAP / UNKNOWN` 分类；
- 管理员备注、期望结论、评价人、评价时间和不可变修订；
- 只导出结构化报告和评价结论，不导出原始模型输入、日志、备注、身份或异常正文。
- 服务端按 run 保存模型实际接收的脱敏上下文、SHA-256、Prompt/Schema ID、配置与实际模型、
  调用结果；管理端不回显上下文正文。
- 管理员通过显式动作将指定 run、artifact 和反馈修订冻结为不可变 case；前端不能提交或
  修改上下文，重复提升同一反馈修订保持幂等；
- 离线 runner 只读取这些已审核 case，支持单用例或当前最多 100 个用例的手动回放；
- 每个结果保存结构化候选报告、Prompt/Schema、provider、配置/实际模型、调用结果、分项分数、
  总分和受控失败码，运行保存通过数、失败数和平均分；
- 固定权重评分可由人工复核，不使用模型 judge，也不会依据得分自动切换或发布模型。

这完成了“反馈 -> 审核用例 -> 回放 -> 可比较结果”的最小闭环，但不等于完全对齐 OpenSRE：
当前没有 CloudOpsBench 同等规模的场景集、代码提交 provenance、版本化套件和 CI 自动门禁。
后续扩展这些能力时仍应读取隔离的审核用例集，不能让定时任务扫描生产事故并直接调用模型。

### 5.2 发布和 Runbook 证据

沿用 `SreIncidentEvidence`，新增受控 `DEPLOYMENT_SNAPSHOT` 与 `RUNBOOK_SNAPSHOT`，不要让
模型直接访问 Git、Shell 或数据库。报告中继续要求每个观察和假设引用证据 ID。

### 5.3 有界只读调查循环

满足以下门槛后再实现：

- 工具只有固定白名单参数；
- 单 run 最大轮次、工具调用数、总时长、上下文长度和模型成本均有限额；
- 相同工具 + 相同参数只执行一次；
- 连续无新证据时强制停止；
- 每次调用和结果摘要写入调查步骤；
- 离线回归集证明结果优于当前单次 RCA。

## 6. 明确不做

- 不因为 OpenSRE 使用 Python 就重写 Java 主链；
- 不新建 TS 服务复制事故状态机；
- 不引入 Neo4j 作为当前版本依赖；
- 不追求一次接入 60+ 工具，只接当前服务器真实存在的数据源；
- 不允许模型执行 Shell、Docker、任意 PromQL/LogQL 或修复动作；
- 不设定时任务从生产事故自动调用模型；
- 不使用模型裁判替代可复核评分规则；
- 不根据评测结果自动切换、发布或回滚模型；
- 不通过管理 API 返回冻结上下文或基准报告 JSON；
- 不让 AI、Loki、Grafana 或 MySQL 故障阻塞 Alertmanager 的 QQ 邮件链路。

## 7. 官方来源

- [OpenSRE README](https://github.com/Tracer-Cloud/opensre/blob/main/README.md)
- [OpenSRE 调查流程](https://github.com/Tracer-Cloud/opensre/blob/main/docs/how-investigations-work.mdx)
- [调查管线与有界循环](https://github.com/Tracer-Cloud/opensre/blob/main/docs/investigation-pipeline-architecture.md)
- [会话 JSONL 仓库](https://github.com/Tracer-Cloud/opensre/blob/main/core/agent_harness/session/persistence/jsonl_repo.py)
- [工具元数据契约](https://github.com/Tracer-Cloud/opensre/blob/main/core/tool_framework/metadata.py)
- [Closed-Loop Learning](https://github.com/Tracer-Cloud/opensre/blob/main/docs/closed-loop-learning.mdx)
- [Benchmark 运行与 provenance](https://github.com/Tracer-Cloud/opensre/blob/main/tests/benchmarks/README.md)
- [OpenSRE pyproject](https://github.com/Tracer-Cloud/opensre/blob/main/pyproject.toml)
