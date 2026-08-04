# Code Nest v2.5.0：AI Growth Coach 调研与落地方案

> 状态：Proposed
>
> 调研日期：2026-07-17
>
> 仓库基线：`master@0e1e8750e8eb`（v2.4.3）
>
> 目标读者：产品负责人、技术负责人、后端、前端、测试
>
> 配套技术设计：[AI 成长教练技术架构与技术选型](../Technical/14-AI成长教练技术架构与技术选型.md)

## 1. 决策摘要

建议正式将 v2.5.0 锁定为：

> **AI Growth Coach：程序员成长与求职教练。AI 不是 Code Nest 的又一个聊天入口，而是产品背后的行动引擎。**

第一个垂直切片仍应是：

> **用户用一句自然语言调整目标与本周计划，系统返回可解释的变更预览；用户确认后，系统以幂等、可审计的方式更新计划与今日行动。**

这个方向成立，原因不是“AI 热门”，而是 Code Nest 已经拥有完成该闭环所需的大部分部件：

1. Growth Autopilot 已有周目标、任务生成、完成、顺延、重排和事件记录。
2. Career Loop 已有阶段、快照、动作清单和阶段日志，而且 Job Battle、模拟面试已经在局部推进该状态机。
3. 管理员 Agent 已验证 `Planner -> Tool -> Policy -> Preview -> Confirm -> Execute -> Audit` 的受控动作协议。
4. `xiaou-ai` 已有 Prompt `key/version`、结构化输出契约、模型降级、场景指标和回归评测，不需要再建设一套 Dify 式 AI 平台。
5. 用户首页已经有“今天的行动”，学习驾驶舱也已有带理由、预计耗时的候选动作；P0 更接近“升级决策质量并打通执行”，不是从零造一个页面。

但在进入开发前，必须先解决三个阻断问题：

- **时间预算目前不是硬约束。** 现有六类任务模板的最小分钟数合计为 330 分钟，即使用户选择每周 3 小时，也会至少生成 5.5 小时任务。
- **计划变更会破坏历史。** 重新生成会删除目标下全部任务；当前重排保留 missed 任务、又按旧目标分补齐任务，可能使目标分在重复重排后膨胀。
- **管理员运行时不能直接开放给用户。** 当前 Controller、Operator、权限、会话命名和审计表都明确绑定管理员语义。应复用协议和内核，不应伪造管理员身份或把管理员工具目录暴露给用户。

因此，v2.5.0 的正确交付不是“增加 AI 对话框”，而是完成以下产品闭环：

```text
用户约束/目标
  -> 有来源的成长证据
  -> 混合规划与硬约束校验
  -> 计划变更预览
  -> 用户确认
  -> 版本化、幂等执行
  -> 今日唯一优先动作
  -> 新证据与下一轮决策
```

## 2. 调研范围与结论口径

本调研包含：

- 当前仓库的 Growth Autopilot、Career Loop、管理员 Agent、统一 AI 运行时和用户首页实现。
- OJ、题库、闪卡、计划打卡、模拟面试、简历、求职作战台、CodePen 等潜在证据源。
- LangGraph、OpenHands、Dify、RAGFlow、Mem0、Zep 的官方机制。
- v2.5.0 的产品边界、技术架构、数据模型、接口、验收标准、指标和风险。

文中口径：

- **事实**：来自当前仓库或官方项目文档。
- **推断**：根据当前代码行为得出的影响判断。
- **建议**：v2.5.0 的产品或技术决策。

## 3. 当前仓库能力盘点

### 3.1 总体成熟度

| 能力 | 当前事实 | 可复用程度 | v2.5.0 缺口 |
| --- | --- | ---: | --- |
| 周计划 | 已有目标、任务、完成、顺延、重排、事件 | 高 | 预算硬约束、版本、资源绑定、预览、幂等 |
| 今日行动 | 首页已有行动区；驾驶舱已有候选排序、理由和预计耗时 | 高 | 选择具体任务、证据引用、统一唯一动作 |
| 求职闭环 | 8 阶段、快照、动作、时间线；Job Battle/模拟面试已推送事件 | 高 | 通用证据模型、可靠事件、跨域动作联动 |
| Agent 动作协议 | 工具注册、Schema、权限、预览、确认、审计、幂等、恢复 | 高 | 去管理员化、用户域隔离、领域工具目录 |
| AI 工程 | Prompt 版本、结构化输出、指标、成本、降级、回归夹具 | 高 | Growth Coach 场景 Prompt 与结果评测 |
| 成长证据 | 各模块有真实业务记录 | 中 | 统一索引、质量等级、时间有效性、引用协议 |
| 主动式教练 | 有计划提醒 Scheduler | 低 | 周复盘、漂移检测、主动建议与通知策略 |

### 3.2 Growth Autopilot：已有执行骨架，但还不是约束规划器

现有服务的优点：

- 生成、完成、批量完成、顺延和重排均位于事务中。
- 所有任务都归属 `userId` 和周目标，Controller 从登录态读取用户 ID。
- 任务已有 `plannedMinutes`、`priority`、`source` 和 `routePath`，可以演进为可执行动作。
- Dashboard 已提供今日任务、风险、模块进度、快速动作和事件日志。

关键代码：

- [GrowthAutopilotServiceImpl.java：生成计划](../../xiaou-plan/src/main/java/com/xiaou/plan/service/impl/GrowthAutopilotServiceImpl.java#L62)
- [GrowthAutopilotServiceImpl.java：当前重排](../../xiaou-plan/src/main/java/com/xiaou/plan/service/impl/GrowthAutopilotServiceImpl.java#L192)
- [GrowthAutopilotServiceImpl.java：模板任务生成](../../xiaou-plan/src/main/java/com/xiaou/plan/service/impl/GrowthAutopilotServiceImpl.java#L405)
- [GrowthAutopilotServiceImpl.java：岗位/阶段权重](../../xiaou-plan/src/main/java/com/xiaou/plan/service/impl/GrowthAutopilotServiceImpl.java#L566)

必须处理的缺口：

1. **预算会被模板最小值突破。** 六个模块的 `minMinutes` 为 `80 + 70 + 50 + 45 + 50 + 35 = 330`。生成逻辑对每个模块使用 `max(minMinutes, weeklyMinutes * weight)`，所以 3 小时预算无法成立。
2. **重新生成会删除已完成任务。** 对已有周目标执行 `deleteByGoalId`，会丢失任务级历史和完成证据。自然语言调整不能调用这条写路径。
3. **重排目标可能膨胀。** 重排保留 missed 任务，再按旧 `totalScoreTarget - completedScore` 生成补位任务；随后指标刷新又把 missed 与新任务一起计入目标总分。重复重排可能不断增加分母。
4. **没有计划版本。** 预览后如果用户在另一端完成或修改了任务，确认仍可能覆盖新状态。
5. **没有幂等键。** 重复确认、网络重试和崩溃恢复没有业务级去重依据。
6. **任务只绑定路由，不绑定资源。** “做两道题”没有 `problemId/questionSetId/cardId/sessionId`，无法证明计划来自真实题库或验证是否完成。
7. **没有自动化服务测试。** `xiaou-plan/src/test` 当前没有 Growth Autopilot 覆盖，不能把现有行为当作稳定契约直接扩展。

结论：保留现有 Controller/Dashboard 和任务实体的产品语义，但将生成与重排内核升级为“候选资源 + 硬约束调度 + 版本化应用”。

### 3.3 Career Loop：不是空壳，已经形成局部闭环

Career Loop 当前提供启动、当前状态、时间线、动作清单、完成动作、目标更新、同步和事件上报接口：

- [CareerLoopController.java](../../xiaou-mock-interview/src/main/java/com/xiaou/mockinterview/controller/CareerLoopController.java#L33)
- [CareerLoopServiceImpl.java：事件处理](../../xiaou-mock-interview/src/main/java/com/xiaou/mockinterview/service/impl/CareerLoopServiceImpl.java#L152)
- [CareerLoopServiceImpl.java：阶段动作模板](../../xiaou-mock-interview/src/main/java/com/xiaou/mockinterview/service/impl/CareerLoopServiceImpl.java#L294)
- [CareerLoopStateMachine.java](../../xiaou-mock-interview/src/main/java/com/xiaou/mockinterview/service/CareerLoopStateMachine.java#L15)

求职作战台已在 JD 解析、简历匹配、计划生成、批量岗位匹配和复盘后推进 Career Loop：

- [JobBattleServiceImpl.java](../../xiaou-mock-interview/src/main/java/com/xiaou/mockinterview/service/impl/JobBattleServiceImpl.java#L84)

模拟面试结束或生成总结后也会推送 `INTERVIEW_DONE`：

- [MockInterviewServiceImpl.java](../../xiaou-mock-interview/src/main/java/com/xiaou/mockinterview/service/impl/MockInterviewServiceImpl.java#L383)
- [MockInterviewServiceImpl.java：闭环事件](../../xiaou-mock-interview/src/main/java/com/xiaou/mockinterview/service/impl/MockInterviewServiceImpl.java#L852)

因此，准确结论是：**Career Loop 已与 Job Battle/模拟面试局部连接，但还没有成为全站统一的成长状态与证据消费者。**

现有连接的局限：

- 业务服务直接同步调用 `CareerLoopService`，异常被捕获后仅记录日志，缺少可靠重试和事件幂等。
- `CareerLoopEventRequest` 传递计数、分数和自由文本建议，缺少统一 `evidenceId`。
- 状态机只禁止回退，允许直接跳到任意更高阶段；阶段推进没有统一的证据门槛。
- `markActionDone` 只更新动作状态，不自动校验结果证据。
- 快照是汇总字段和 JSON 文本，无法回答“这个判断来自哪一次提交/面试/简历版本”。

### 3.4 管理员 Agent：应复用运行协议，不应复用管理员边界

当前运行时已经完成：

```text
自然语言
 -> LLM/确定性 Planner
 -> 后端 Tool Registry
 -> Schema 与权限策略
 -> Preview/Diff
 -> 强确认
 -> Execute
 -> Audit/Idempotency/Recovery
```

关键代码：

- [AgentChatOrchestrator.java：统一入口](../../xiaou-system/src/main/java/com/xiaou/system/agent/AgentChatOrchestrator.java#L98)
- [AgentChatOrchestrator.java：策略与执行](../../xiaou-system/src/main/java/com/xiaou/system/agent/AgentChatOrchestrator.java#L185)
- [AgentChatOrchestrator.java：预览与确认](../../xiaou-system/src/main/java/com/xiaou/system/agent/AgentChatOrchestrator.java#L227)
- [AgentChatOrchestrator.java：审计续接](../../xiaou-system/src/main/java/com/xiaou/system/agent/AgentChatOrchestrator.java#L309)
- [LlmAgentPlanResolver.java](../../xiaou-system/src/main/java/com/xiaou/system/agent/LlmAgentPlanResolver.java#L25)
- [AgentTool.java](../../xiaou-system/src/main/java/com/xiaou/system/agent/AgentTool.java#L10)
- [AgentPolicyEngine.java](../../xiaou-system/src/main/java/com/xiaou/system/agent/AgentPolicyEngine.java#L27)

但当前边界明确是管理员：

- HTTP 入口为 `/admin/agent/chat`，并使用 `@RequireAdmin`。
- `AgentOperator` 的角色和权限来自管理员服务。
- 会话 Key 使用 `operator:<adminId>`，Redis 前缀也包含 admin 语义。
- `sys_agent_audit` 的消息、操作者和注释均是管理员语义。
- Planner 当前看到管理员工具目录，不能直接让普通用户共享同一个 Registry。

用户侧应复用：状态协议、工具 Schema、策略评估、预览、幂等、审计和恢复模式。

用户侧不应复用：管理员 Controller、管理员 Operator、管理员权限种子、管理员工具目录和 `sys_agent_audit` 表。

### 3.5 统一 AI 基础：无需再造平台

当前 `xiaou-ai` 已有：

- Prompt `key/version` 与最大输出 Token 契约。
- 结构化 JSON Schema 和运行时校验。
- 场景、Prompt 版本、模型、延迟、Token、成本、降级和解析失败指标。
- 离线评测场景、回归夹具和管理员 Planner 回归用例。

示例：

- [AdminAgentPromptSpecs.java](../../xiaou-ai/src/main/java/com/xiaou/ai/prompt/admin/AdminAgentPromptSpecs.java#L12)
- [AiPromptSpec.java](../../xiaou-ai/src/main/java/com/xiaou/ai/prompt/AiPromptSpec.java#L16)
- [AiExecutionSupport.java](../../xiaou-ai/src/main/java/com/xiaou/ai/support/AiExecutionSupport.java#L38)
- [AiMetricsRecorder.java](../../xiaou-ai/src/main/java/com/xiaou/ai/metrics/AiMetricsRecorder.java#L29)
- [AiSceneRegressionEvalTest.java](../../xiaou-ai/src/test/java/com/xiaou/ai/eval/AiSceneRegressionEvalTest.java#L1)

建议新增 Growth Coach 的 Prompt、结构化契约、fixture 和 outcome 指标，不引入 Dify 服务或新的模型网关。

### 3.6 用户端已有可复用界面，但“今天的行动”还不够具体

首页当前已经把“今天的行动”放在主要内容中，但主卡只表达“还有几项计划”，CTA 跳转 `/plan`，旁边还并列一个 OJ 每日一题；它没有指向具体最优任务，也没有预计耗时和选择原因：

- [HomeRevamp.vue](../../vue3-user-front/src/views/HomeRevamp.vue#L58)

学习驾驶舱已经生成候选动作的 `reason`、`expectedGain` 和预计耗时，并取 `nextActions[0]` 作为 `topAction`：

- [LearningCockpitService.java：候选动作排序](../../xiaou-application/src/main/java/com/xiaou/web/learning/service/LearningCockpitService.java#L732)
- [LearningCockpitService.java：今日任务](../../xiaou-application/src/main/java/com/xiaou/web/learning/service/LearningCockpitService.java#L473)
- [learning-cockpit/Index.vue](../../vue3-user-front/src/views/learning-cockpit/Index.vue#L176)

但这些候选仍是规则生成的模块级动作，不绑定具体业务资源，也没有证据引用。P0 应升级并复用这条链路，将唯一 `TodayAction` 直接返回首页，而不是新增 AI 页面。

## 4. 产品定义

### 4.1 用户价值

AI Growth Coach 每天只需清楚回答三个问题：

1. **我今天最应该做什么？**
2. **为什么是这件事？**
3. **做完以后，下一步会发生什么变化？**

产品输出不是一段建议，而是一个可以开始、完成、验证和继续推进的动作。

### 4.2 交互原则

1. **行动优先。** 首页默认只展示一个最优动作，其余任务在计划详情中展开。
2. **解释必须有来源。** “为什么”至少引用一个用户约束或业务证据。
3. **先预览后写入。** AI 只能生成结构化意图和候选计划；数据库写入由确定性工具执行。
4. **可逆操作轻确认。** 用户修改自己的本周计划使用按钮确认，不要求输入强确认文本。
5. **高风险操作强确认。** 删除证据、跨用户操作或不可逆动作不进入 P0；未来仍使用强确认。
6. **不确定就澄清。** “这周 3 小时”究竟是剩余总时长还是每周长期偏好，无法确定时必须在预览前询问，不能静默猜测。
7. **不伪造资源。** 没有题目、题单、卡组、项目、岗位或面试记录支撑时，不生成对应任务。
8. **稳定而非抖动。** 今日动作只在计划、截止日期或新证据发生变化时重新选择，避免每次刷新都换任务。

### 4.3 AI 今日行动卡

首屏只保留一个主动作：

| 字段 | 含义 |
| --- | --- |
| `title` | 可交付、可验证的动作，不是宽泛建议 |
| `estimatedMinutes` | 由资源或规则确定的预计耗时 |
| `reason` | 选择原因，包含截止日期、能力缺口或连续拖延等依据 |
| `evidenceRefs` | 可点击查看的证据来源 |
| `expectedChange` | 完成后计划、能力信号或求职阶段会如何变化 |
| `startRoute` | 直接进入具体资源的开始入口 |
| `completionRule` | 系统如何判定完成 |

没有合格资源时，卡片应显示“补全目标/JD/计划”等设置动作，而不是让模型编一个学习任务。

## 5. P0 垂直切片：自然语言调整本周计划

### 5.1 标准流程

```mermaid
flowchart LR
    A["用户输入约束与目标"] --> B["读取当前计划、Career Loop 与证据摘要"]
    B --> C["LLM 提取结构化意图"]
    C --> D["规则引擎生成可行候选"]
    D --> E["LLM 排序并解释"]
    E --> F["硬约束最终校验"]
    F --> G["保存变更预览与基线版本"]
    G --> H{"用户确认?"}
    H -->|取消| I["记录取消，不改计划"]
    H -->|确认| J["幂等、事务化应用新版本"]
    J --> K["刷新今日行动与证据索引"]
```

### 5.2 示例

用户输入：

> 这周临时加班，只剩 3 小时，下周要面试 Java 后端岗位。

系统不应直接回复一篇建议，而应返回预览：

- 理解到的约束：当前周剩余 180 分钟；目标岗位调整为 Java 后端；下周存在面试截止压力。
- 保留：已完成任务、与 Java 高频知识点直接相关且有真实题单/题目绑定的任务。
- 降低：积分打卡、泛化闪卡整理等与近期面试目标关联较低的任务。
- 新计划总时长：不超过 180 分钟。
- 每一项任务显示真实 `resourceRef`、预计耗时、保留/移除原因。
- 用户确认后才写入；取消或预览过期均不改变计划。

注意：示例中的具体题目、题单和面试任务必须在运行时从 Code Nest 真实资源中选择，文档不预设虚构内容。

### 5.3 P0 支持的自然语言范围

支持：

- 调整当前周可用总分钟数。
- 调整目标岗位和当前阶段。
- 声明面试日期、加班、请假等短期约束。
- 表达“优先模拟面试/算法/八股/项目”等排序偏好。
- 请求减少、延后、重排当前周未完成任务。

不支持：

- 泛用问答、开放式职业咨询。
- 自动修改简历、投递岗位或发送消息。
- 跨多周自由生成长期路线。
- 修改或删除历史完成证据。
- 多 Agent 自主协商。

## 6. 推荐技术架构

### 6.1 模块职责

| 模块 | v2.5.0 职责 |
| --- | --- |
| `xiaou-ai` | 中立 Action Runtime 内核、Growth Coach Prompt、结构化意图契约、模型调用与指标 |
| `xiaou-application` | Growth Coach 应用编排；聚合计划、Career Loop、证据和候选资源 |
| `xiaou-plan` | 纯确定性的预算校验、任务调度、计划版本和事务化写入 |
| `xiaou-mock-interview` | Career Loop、JD/简历/面试证据与求职动作 |
| OJ/题库/闪卡/简历等模块 | 提供只读证据源和具体资源候选，不感知 LLM |
| `xiaou-system` | 管理员 Agent 适配器继续使用现有管理员鉴权和审计，不承载用户入口 |
| `vue3-user-front` | 首页行动卡、计划调整输入、Diff 预览、确认/取消和执行结果 |

`xiaou-application` 已经依赖业务模块和 `xiaou-ai`，适合作为跨域编排层。不要让 `xiaou-plan` 依赖 `xiaou-system`，也不要让 OJ、简历等模块反向依赖 Growth Coach。

### 6.2 Action Runtime 中立化

从现有管理员 Agent 抽取的是窄内核，而不是重写全部运行时：

- `ActionDefinition`：名称、领域、风险、Schema、权限/所有权策略。
- `ActionTool`：`preview()` 与 `execute()`。
- `ActionPolicyEngine`：只读、可逆自有写入、高风险写入三档策略。
- `ActionRunStore`：预览、确认、执行、取消、过期、失败和恢复。
- `ActionContext`：`actorType/actorId/domain/sessionId/traceId`。
- `ActionRegistry`：按 `domain + actorType` 隔离工具目录。

管理员 `/admin/agent/chat` 通过 Adapter 保持现有 API 和测试契约；用户 Growth Coach 使用独立 Registry 与审计存储。不得让用户 Planner 看到管理员工具定义。

P0 不必引入新的 LangGraph 服务。两步确认流用数据库状态机即可；借鉴 LangGraph 的 checkpoint、interrupt 和幂等原则，而不是复制其运行时。

### 6.3 用户 Action Run 状态

```text
PREVIEW -> CONFIRMED -> EXECUTING -> EXECUTED
   |           |             |
   |           |             +-> FAILED
   |           +-> FAILED
   +-> CANCELLED
   +-> EXPIRED
```

执行记录必须保存：

- `actor_type=user` 与登录用户 ID。
- 原始指令、结构化意图、Prompt ID/版本、模型名。
- 当前计划 `basePlanVersion`。
- 计划 Diff、解释、证据引用和警告。
- 幂等键、执行结果、新计划版本和错误恢复信息。

## 7. 混合规划器

### 7.1 LLM 负责什么

- 从自然语言提取岗位、周范围、可用分钟、截止日期、优先方向和禁止事项。
- 在规则引擎给出的可行候选中排序。
- 解释保留、删除、移动任务的原因。
- 信息不足时生成结构化澄清字段。

LLM 不负责：

- 计算最终时间预算。
- 决定用户是否有权限。
- 生成不存在的资源 ID。
- 写数据库或宣称执行成功。
- 修改已完成任务和历史证据。

### 7.2 规则引擎负责什么

| 硬约束 | 规则 |
| --- | --- |
| 所有权 | 目标、任务、预览和证据都必须属于当前登录用户 |
| 时间 | 活跃任务 `plannedMinutes` 总和不得超过本周剩余预算 |
| 日期 | 任务必须落在有效周和可用日期内 |
| 历史 | 已完成任务不可删除、降级或改写 |
| 资源 | 每个执行任务必须有可访问的 `resourceType + resourceId + startRoute` |
| 截止期 | 面试前任务必须在截止日期前完成，无法满足时明确提示冲突 |
| 幂等 | 同一 Action Run 重复确认最多产生一个计划版本 |
| 并发 | `basePlanVersion` 不一致时拒绝应用并重新生成预览 |
| 可执行性 | 任务完成条件必须能由系统事件或明确打卡验证 |

### 7.3 推荐规划顺序

1. 解析并标准化用户意图。
2. 读取当前周计划和已完成任务。
3. 从真实资源与证据中生成候选动作。
4. 先按所有权、资源存在性、日期和预算过滤。
5. 规则分数形成短名单：紧迫度、能力缺口、预计收益、上下文匹配、拖延惩罚、切换成本。
6. LLM 只在短名单内重排并生成解释。
7. 最终校验器再次验证全部硬约束。
8. 生成 Diff，不执行写入。

模型不可用时，支持范围内的意图使用确定性解析和排序；无法可靠解析时返回“需要重新说明”，不能用 fallback 伪装成 AI 已理解。

## 8. 用户成长证据库

### 8.1 证据不是聊天记忆

证据库的目标是回答：

- 用户做过什么？
- 结果如何？
- 发生在什么时候？
- 这个判断能否回到原始业务记录？

必须区分：

| 类型 | 示例 | 是否可证明能力 |
| --- | --- | ---: |
| 用户约束 | “本周只剩 3 小时” | 否 |
| 用户偏好 | “优先 Java 后端” | 否 |
| 系统证据 | OJ AC、面试得分、复习记录、简历版本 | 是 |
| 模型推断 | “并发基础较弱” | 仅在引用证据且标注置信度后可作为信号 |

周可用时间是带有效期的约束，不能被提升为永久偏好；旧岗位、旧目标和被新事实替代的偏好应保留历史，但退出当前有效上下文。

### 8.2 现有证据源

| 证据 | 当前记录 | 可用字段 |
| --- | --- | --- |
| OJ | [OjSubmission.java](../../xiaou-oj/src/main/java/com/xiaou/oj/domain/OjSubmission.java#L16) | problem、status、pass/total、语言、耗时、发生时间 |
| 面试题学习 | [InterviewMasteryRecord.java](../../xiaou-interview/src/main/java/com/xiaou/interview/domain/InterviewMasteryRecord.java#L16) | 掌握度、复习次数、下次复习时间 |
| 闪卡 | [FlashcardStudyRecord.java](../../xiaou-flashcard/src/main/java/com/xiaou/flashcard/domain/FlashcardStudyRecord.java#L15) | 掌握度、正确率、间隔、复习时间 |
| 计划打卡 | [PlanCheckinRecord.java](../../xiaou-plan/src/main/java/com/xiaou/plan/domain/PlanCheckinRecord.java#L15) | 完成量、内容、心得、积分、时间 |
| 自动驾驶任务 | [GrowthAutopilotTask.java](../../xiaou-plan/src/main/java/com/xiaou/plan/domain/GrowthAutopilotTask.java#L14) | 模块、分钟、分值、状态、完成时间 |
| 模拟面试 | [MockInterviewSession.java](../../xiaou-mock-interview/src/main/java/com/xiaou/mockinterview/domain/MockInterviewSession.java#L16) | 总分、分项分、总结、建议、时间 |
| 面试问答 | [MockInterviewQA.java](../../xiaou-mock-interview/src/main/java/com/xiaou/mockinterview/domain/MockInterviewQA.java#L16) | 题目、回答、得分、知识点、AI 反馈 |
| 简历变化 | [ResumeVersion.java](../../xiaou-resume/src/main/java/com/xiaou/resume/domain/ResumeVersion.java#L14) | 版本号、快照、changeLog、时间 |
| 岗位匹配 | [JobBattleMatchRecord.java](../../xiaou-mock-interview/src/main/java/com/xiaou/mockinterview/domain/JobBattleMatchRecord.java#L16) | 最佳/平均匹配分、岗位、结果、时间 |
| 求职计划 | [JobBattlePlanRecord.java](../../xiaou-mock-interview/src/main/java/com/xiaou/mockinterview/domain/JobBattlePlanRecord.java#L16) | 差距、计划、面试日期、投入时间 |
| 求职推进 | [CareerLoopStageLog.java](../../xiaou-mock-interview/src/main/java/com/xiaou/mockinterview/domain/CareerLoopStageLog.java#L13) | 前后阶段、来源、引用 ID、时间 |
| 代码作品 | [CodePen.java](../../xiaou-codepen/src/main/java/com/xiaou/codepen/domain/CodePen.java#L13) | 作品、版本时间、发布状态、互动结果 |

### 8.3 最小证据模型

建议新增 append-only `growth_evidence`，只保存索引、指标和脱敏摘要，不复制完整简历、源代码、回答正文或 JD：

| 字段 | 说明 |
| --- | --- |
| `id` | 证据 ID |
| `user_id` | 所属用户 |
| `evidence_type` | `oj_submission/mock_interview/resume_version/...` |
| `source_module` | 来源模块 |
| `source_type/source_id` | 回到原始记录的稳定引用 |
| `occurred_at` | 业务发生时间，不使用投影写入时间代替 |
| `result_status` | accepted/completed/failed/updated 等 |
| `metrics_json` | 分数、正确率、耗时等结构化指标 |
| `capability_tags_json` | Java、并发、算法、表达等标签；区分规则标签和模型标签 |
| `summary` | 脱敏、可展示摘要 |
| `route_path` | 用户可访问的证据详情路由 |
| `quality` | verified/self_reported/inferred |
| `content_hash` | 去重与变更检测 |
| `source_revision` | 来源业务版本；不可变记录固定为 1，简历等使用真实版本号 |
| `schema_version` | 证据载荷版本 |
| `supersedes_evidence_id` | 新版本替代的旧证据；旧记录仍保留 |
| `valid_from/valid_to` | 仅对偏好、约束和可失效事实使用 |
| `created_time` | 投影时间 |

唯一约束建议为：

```text
(user_id, evidence_type, source_type, source_id, source_revision)
```

`schema_version` 只描述载荷契约，不能用于判断同一来源是否产生了新业务版本。

### 8.4 P0 采集策略

当前除少数 Career Loop 直连外，没有全站统一业务事件总线。为降低 P0 对所有写链路的侵入：

1. 在 `xiaou-application` 定义 `EvidenceSourceAdapter`，从真实业务表增量、幂等投影。
2. 今日行动或计划预览前按用户触发一次轻量增量同步，后台任务做批量补偿。
3. 使用来源唯一键去重，允许重复扫描。
4. P1 再将高价值写链路升级为事务 Outbox，实现近实时可靠采集。

不建议在 P0 给每个模块增加不受控的同步双写，也不建议直接用 Spring 内存事件作为唯一证据通道；进程崩溃会丢事件。

### 8.5 记忆分层

| 层 | 生命周期 | Code Nest 内容 |
| --- | --- | --- |
| 请求上下文 | 单次预览 | 当前自然语言、当前页面、候选工具结果 |
| Action Run | 预览至终态 | 结构化意图、Diff、确认、执行结果 |
| 周期上下文 | 当前周/求职会话 | 本周时间、目标、活动计划、Career Loop 状态 |
| 长期偏好 | 跨会话但可失效 | 目标岗位、学习偏好、通知偏好 |
| 成长证据 | 长期、append-only | 提交、复习、面试、简历版本、求职结果 |

这比保存无限聊天历史更符合 Growth Coach 的任务，也更容易解释和删除。

## 9. API 与数据契约

### 9.1 用户接口

不新增 `/user/ai/chat`。建议使用意图明确的资源接口：

| 方法 | 路径 | 作用 |
| --- | --- | --- |
| `GET` | `/user/growth-coach/today-action` | 获取唯一今日动作 |
| `POST` | `/user/growth-coach/plan-adjustments/preview` | 解析自然语言并生成变更预览 |
| `POST` | `/user/growth-coach/plan-adjustments/{runId}/confirm` | 确认并幂等执行 |
| `POST` | `/user/growth-coach/plan-adjustments/{runId}/cancel` | 取消预览 |
| `GET` | `/user/growth-coach/plan-adjustments/{runId}` | 查询终态与恢复结果 |

首页聚合接口应直接包含 `todayAction`，避免首页再增加一个串行请求。

### 9.2 预览请求

```json
{
  "message": "这周临时加班，只剩 3 小时，下周要面试 Java 后端岗位。",
  "weekStart": "2026-07-13",
  "clientRequestId": "7e730c91-5bd9-4f28-9339-d15e69d7f2c8"
}
```

`userId` 不得由请求传入，只能来自用户登录态。

### 9.3 预览响应核心字段

```json
{
  "runId": "growth-run-...",
  "status": "PREVIEW",
  "basePlanVersion": 4,
  "intent": {
    "targetRole": "Java 后端",
    "availableMinutes": 180,
    "scope": "CURRENT_WEEK",
    "deadlineType": "INTERVIEW_NEXT_WEEK"
  },
  "constraintSummary": {
    "plannedMinutes": 180,
    "budgetMinutes": 180,
    "completedTasksPreserved": true,
    "allTasksResourceBacked": true
  },
  "diff": {
    "kept": [],
    "moved": [],
    "removed": [],
    "added": []
  },
  "explanations": [],
  "evidenceRefs": [],
  "warnings": [],
  "expiresAt": "2026-07-17 18:30:00"
}
```

具体任务对象必须包含 `resourceType`、`resourceId`、`plannedMinutes`、`startRoute`、`completionRule` 和 `reason`。

## 10. 数据演进

### 10.1 Growth Autopilot

建议增量字段：

- `growth_autopilot_goal.plan_version`：乐观锁版本。
- `growth_autopilot_goal.weekly_minutes`：精确分钟预算；保留 `weekly_hours` 兼容旧接口。
- `growth_autopilot_task.plan_version`：所属计划版本。
- `growth_autopilot_task.status` 新增 `superseded`，计划调整不物理删除历史任务。
- `growth_autopilot_task.resource_type/resource_id`：具体业务资源。
- `growth_autopilot_task.selection_reason`：可解释原因。
- `growth_autopilot_task.completion_rule_json`：完成验证规则。
- `growth_autopilot_task_evidence`：任务与完成证据的关联表，支持多次尝试和多份证据。

P0 任务应尽量原子化：一个任务绑定一个可开始的具体资源；需要完成多个资源时拆成多个任务，不用一个模糊任务包隐藏资源明细。

可选新增 `growth_autopilot_revision` 保存每次计划版本的约束摘要、Diff 和 Action Run ID。

### 10.2 用户动作审计

新增独立的 `growth_coach_action_run`，复用管理员 Agent 的状态协议，但不复用 `sys_agent_audit`：

- 用户域可使用更合适的隐私、保留期和确认策略。
- 避免把普通用户 ID 混入管理员操作者字段。
- 避免管理员审计查询误展示用户私有简历、JD 或学习内容。
- 后续稳定后，再评估是否迁移到统一 `ai_action_run`。

## 11. 版本路线

### 11.1 P0：v2.5.0 必须完成

#### A. 自然语言计划调整

- 当前周、目标岗位、可用分钟和面试截止约束解析。
- Preview/Diff、确认、取消、过期、幂等和恢复。
- 计划版本与并发冲突处理。

#### B. 最小成长证据库

- 统一证据表、来源 Adapter、幂等投影和证据链接。
- 首批覆盖计划/OJ/题库/闪卡/模拟面试/简历与求职记录。
- 不复制敏感正文。

#### C. AI 今日行动卡

- 首页只展示一个具体动作。
- 显示耗时、原因、证据、完成后变化和开始按钮。
- 没有真实资源时退化为设置动作。

#### D. 混合规划器

- LLM 只做意图、可行候选排序和解释。
- 规则引擎控制预算、日期、资源、历史和所有权。
- 最终约束校验器有独立测试。

### 11.2 P1：形成三类结果闭环

1. `JD -> 简历差距 -> 学习计划 -> 模拟面试 -> 投递/Offer`。
2. `错题/低掌握度 -> 薄弱点 -> 针对性练习 -> 再测验证`。
3. `CodePen/项目代码 -> AI Review -> 改进任务 -> 新版本/提交证据 -> 能力档案`。

P1 的重点不是增加更多 Agent，而是让每个动作产生可验证证据，并让证据改变下一步决策。

当前实现补充了这条求职准备路径的用户侧阶段机：最近一次岗位匹配、其后的补短板计划、其后的已完成模拟面试和用户自报投递摘要会形成一个只读的“下一步”判断。它不会把计划创建误作计划完成，不会自动把投递归因到某份 JD，也不会自动创建面试或投递；只有能明确映射到现有方向的岗位，才会预填专项模拟面试的短技术主题。

### 11.3 P2：主动能力

- 每周自动复盘和下周调整建议。
- 连续拖延、任务过重、目标漂移检测。
- 岗位市场和能力变化驱动的路线调整。
- 多领域内部 Agent；用户侧仍只有统一行动入口。

## 12. 推荐实施顺序

| 顺序 | 工作包 | 依赖 | 相对复杂度 |
| ---: | --- | --- | ---: |
| 1 | 为现有 Growth Autopilot 补契约测试；修复预算、历史删除和目标膨胀 | 无 | M |
| 2 | 增加计划版本、superseded 状态和资源绑定 | 1 | M |
| 3 | 抽取中立 Action Runtime 协议，保持管理员回归全绿 | 现有 Agent 测试 | L |
| 4 | Growth Coach 意图 Schema、Prompt、fallback 和回归 fixture | 3 | M |
| 5 | Preview/Confirm/Cancel/Recover API 与审计 | 2、3、4 | M |
| 6 | 证据表、来源 Adapter 和增量投影 | 2 | L |
| 7 | 首页今日行动卡与计划 Diff 交互 | 5、6 | M |
| 8 | 指标、灰度、E2E 和发布门禁 | 全部 | M |

团队容量未知，因此这里给依赖顺序和相对复杂度，不给缺乏依据的人天承诺。

建议先以“只生成预览、不允许确认”的 shadow 模式验证意图解析与约束，再开放真实写入。

## 13. 验收标准

### 13.1 功能验收

1. 用户输入“本周只剩 3 小时”后，活跃任务分钟总和不得超过 180。
2. 已完成任务和对应证据在任何重排后保持不变。
3. 预览阶段除 Action Run 审计外，不修改目标与任务。
4. 取消、过期预览不修改计划。
5. 同一 `runId` 重复确认最多生成一个新计划版本。
6. `basePlanVersion` 过期时拒绝写入，并提示重新预览。
7. 每个新增任务都绑定真实、当前用户可访问的业务资源。
8. 模型不可用时只执行确定性支持范围；不支持的意图明确失败。
9. 用户不能读取、确认或取消其他用户的 Action Run。
10. 确认成功后，首页今日行动立即反映新计划。

### 13.2 测试门禁

- `GrowthAutopilotServiceImplTest`：生成、完成、重排、重复重排、预算和历史保留。
- 规划器属性测试：任意合法输入均满足预算、日期、资源和所有权不变量。
- Action Runtime 测试：状态迁移、确认策略、幂等、崩溃恢复、过期和并发冲突。
- Planner fixture：常见中文时间表达、歧义、Prompt 注入、缺字段和低置信度。
- 证据投影测试：重复扫描、源记录更新、敏感字段不落库、路由有效。
- API 安全测试：越权、伪造用户 ID、重放、超长输入和日志脱敏。
- 前端 E2E：输入 -> 预览 -> 取消；输入 -> 预览 -> 确认 -> 今日行动更新。
- 管理员 Agent 现有回归必须保持通过，证明中立化未改变 v2.4.0 行为。

## 14. 衡量指标

不使用聊天轮数、消息数或“AI 使用时长”作为核心价值指标。

| 指标 | 计算方式 | 目的 |
| --- | --- | --- |
| 计划采纳率 | `EXECUTED 计划预览 / 有效 PREVIEW` | 建议是否值得执行 |
| 任务完成率 | `完成任务 / 到期活跃任务` | 计划是否可执行 |
| 重排后完成率 | `重排版本完成任务 / 重排版本到期任务` | 重排是否改善执行 |
| 证据增长 | 新增 verified 证据数、覆盖领域和质量变化 | 是否真实成长 |
| 能力验证率 | 薄弱点经后续再测改善的比例 | 是否形成学习闭环 |
| 求职推进率 | 活跃用户在观察窗内推进 Career Loop 阶段的比例 | 是否改善求职结果 |
| 今日动作启动率 | `start 点击 / 展示` | 首要动作是否明确 |
| 今日动作完成率 | `产生完成证据 / 已启动动作` | 动作是否匹配用户状态 |

必须同时监控的护栏：

- 预算/日期/资源硬约束违规数：目标为 0。
- 无来源任务数：目标为 0。
- 重复确认造成的重复写入数：目标为 0。
- 计划应用失败率、fallback 率、预览过期率、用户取消率。
- Preview P50/P95 延迟、模型 Token 和单次已采纳计划成本。
- 用户撤销或短期再次重排率，用于识别低质量计划。

采纳率和完成率的提升目标应在 v2.5.0 先采集基线后设定，不建议现在写一个没有历史数据支撑的百分比。

## 15. 风险与反证

| 风险 | 影响 | 缓解 |
| --- | --- | --- |
| 3 小时仍生成超预算计划 | 核心承诺失真 | 预算作为最终校验不变量，违规则禁止预览 |
| 计划调整删除历史 | 证据与信任丢失 | superseded/版本化，不物理删除完成记录 |
| 直接开放管理员 Agent | 权限越界与工具泄露 | 用户独立 Registry、Actor、审计表和 Controller |
| LLM 生成虚假任务 | 用户无法执行 | 候选只能来自真实资源 Adapter |
| 预览后状态变化 | 覆盖用户最新进度 | `basePlanVersion` 乐观锁 |
| 重试造成重复写入 | 计划重复或数据膨胀 | `runId + idempotencyKey + planVersion` |
| 简历、代码、回答进入日志 | 隐私泄露 | 证据只存指针/指标/脱敏摘要，Prompt 与日志限字段 |
| JD/代码中的 Prompt 注入 | 规划器越权选工具 | 内容作为不可信数据；领域 Registry 与最终策略隔离 |
| 同步直连 Career Loop 丢事件 | 阶段与证据不一致 | P0 幂等投影补偿，P1 事务 Outbox |
| 每次刷新更换今日动作 | 用户失去稳定感 | 仅在计划/证据变化时重算，并记录选择版本 |
| 过早引入工作流平台/图数据库 | 运维复杂度超过收益 | 先用现有 Java/SQL/AI 运行时验证闭环 |

反证条件：如果 shadow 数据显示自然语言重排的有效预览率极低，或确认后的完成率不高于现有“一键重排”，应暂停扩展 P1/P2，优先改善资源质量和规则规划，而不是增加更多模型或 Agent。

## 16. 明确不做

- 不新增泛用“问问 AI”页面。
- 不把多轮聊天作为默认交互。
- 不让 LLM 直接写数据库。
- 不把工作流画布暴露给普通用户。
- 不让普通用户共享管理员工具目录。
- 不生成没有真实资源引用的计划。
- 不把用户自述当成能力证据。
- 不以聊天次数衡量 AI 价值。
- 不在 P0 引入独立 Python Agent 服务、Dify、RAGFlow、Mem0 或 Zep 基础设施。
- 不为了宣传提前做多 Agent。

## 17. 开源机制对照

| 项目 | 官方机制事实 | Code Nest 应借鉴 | 不应复制 |
| --- | --- | --- | --- |
| LangGraph | Checkpointer 保存线程状态；Store 保存跨线程长期数据；Interrupt 可暂停并等待外部输入，恢复前副作用需幂等 | Action Run checkpoint、预览确认、短期状态与长期证据分离 | 为两步确认流引入新的 Python 图运行时 |
| OpenHands | Conversation 管理 Agent 生命周期与状态；事件日志 append-only；工具遵循类型化 Action -> Observation；确认策略可按风险配置 | 类型化工具、不可变执行事件、可逆自有写入轻确认 | 编码沙箱、通用软件工程 Agent 循环 |
| Dify | 工作流有 draft/latest/history/restore；日志记录输入输出、模型、Token、延迟、错误与反馈；模型配置集中管理 | 计划/Prompt 版本、运行日志、反馈与灰度回滚 | 面向普通用户的工作流画布；另建模型配置平台 |
| RAGFlow | 强调可追溯引用和 grounded answer，并允许查看关键引用 | 每个理由和动作带 `evidenceRefs` | 为结构化业务证据部署完整文档 RAG 平台 |
| Mem0 | 区分 conversation、session、user、organization memory，并区分短期与长期记忆 | 请求、Action Run、周上下文、长期偏好、证据五层模型 | 把所有聊天自动提升为长期事实 |
| Zep | Temporal Context Graph 保存实体、关系和事实的有效/失效时间，并保留历史 | 偏好/约束的 `valid_from/valid_to` 与事实替代关系 | P0 引入图数据库或把能力档案图谱化 |

这些项目证明“状态、工具、证据、确认、版本和记忆分层”是成熟机制，但没有证明 Code Nest 需要复制它们的产品界面或基础设施。

## 18. 备选方案与取舍

### 方案 A：新增 AI 聊天页

拒绝。它最容易开发，也最容易与现有聊天、求职和学习页面割裂；用户得到建议，却仍需手动操作多个模块，价值指标会滑向消息量。

### 方案 B：直接让现有管理员 Agent 服务普通用户

拒绝。身份、权限、工具目录、会话和审计都是管理员边界，复用会引入严重越权面。

### 方案 C：先做完整证据平台，再做行动

不建议。数据平台范围会吞掉版本目标。P0 只建支持首个垂直切片的最小证据索引和 Adapter，边交付边扩源。

### 方案 D：直接引入 LangGraph/Dify/RAGFlow/Mem0/Zep

拒绝。当前 Java 运行时已拥有主要机制，新增平台会带来部署、数据同步、权限和可观测性双轨问题。先借鉴机制并扩展现有基础。

### 方案 E：混合规划器 + 受控 Action Runtime

推荐。它最大化复用当前代码，同时把 LLM 限制在最擅长的意图理解、排序和解释，把不可妥协的执行约束留给后端。

## 19. 最终建议

批准 v2.5.0 “AI Growth Coach”，但将发布定义写得更严格：

> v2.5.0 不是发布一个 AI 入口，而是发布第一条“自然语言约束 -> 有证据计划 -> 预览确认 -> 可验证行动”的用户闭环。

开工顺序应为：

1. 先补测试并修正 Growth Autopilot 的预算、历史和重排目标问题。
2. 增加计划版本、资源绑定和用户 Action Run。
3. 在 shadow 模式上线自然语言预览。
4. 通过离线 fixture 和真实预览数据后开放确认写入。
5. 将执行结果接回首页唯一今日行动与成长证据。

只有这条闭环达到“硬约束违规为 0、无来源任务为 0、重复写入为 0”，才进入 P1 三类成长/求职闭环和 P2 主动式能力。

## 20. 外部一手来源

以下资料均于 2026-07-17 访问：

1. LangGraph, [Persistence](https://docs.langchain.com/oss/python/langgraph/durable-execution)：Checkpointer、Store、短期/长期状态边界及保留风险。
2. LangGraph, [Interrupts](https://docs.langchain.com/oss/python/langgraph/interrupts)：暂停、恢复、人类确认与副作用幂等要求。
3. OpenHands, [Conversation Architecture](https://docs.openhands.dev/sdk/arch/conversation)：生命周期、状态、事件日志、持久化和服务边界。
4. OpenHands, [Events Architecture](https://docs.openhands.dev/sdk/arch/events)：append-only 事件、Action/Observation 与观察者模式。
5. OpenHands, [Tool System & MCP](https://docs.openhands.dev/sdk/arch/tool-system)：类型化工具、Schema、Registry 和 Action -> Observation 契约。
6. OpenHands, [Set Conversation Confirmation Policy](https://docs.openhands.dev/sdk/guides/agent-server/api-reference/conversations/set-conversation-confirmation-policy)：AlwaysConfirm、ConfirmRisky、NeverConfirm 风险策略。
7. Dify, [Version Control](https://docs.dify.ai/en/cloud/use-dify/build/version-control)：draft、latest、历史版本与恢复。
8. Dify, [Logs](https://docs.dify.ai/en/guides/monitoring/logs)：输入输出、模型、Token、耗时、错误、反馈和隐私提醒。
9. Dify, [Model Providers](https://docs.dify.ai/en/cloud/use-dify/workspace/model-providers)：工作区模型配置、默认模型、凭据与负载均衡。
10. RAGFlow, [Official README](https://github.com/infiniflow/ragflow/blob/main/README.md)：grounded citations、可追溯引用和部署要求。
11. Mem0, [Memory Types](https://docs.mem0.ai/core-concepts/memory-types)：conversation/session/user/organization 与短期/长期记忆分层。
12. Zep, [Key Concepts](https://help.getzep.com/concepts)：Temporal Context Graph、事实失效、历史保留和上下文类型。
