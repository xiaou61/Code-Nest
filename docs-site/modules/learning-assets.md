# 学习资产

学习资产模块负责把平台内容转化为可复习、可练习、可沉淀的学习材料。

## 功能入口

| 端 | 页面 |
| --- | --- |
| 用户端 | `/learning-assets` |
| 管理端 | `/learning-assets/review`、`/learning-assets/statistics` |
| 后端模块 | `xiaou-learning-asset` |

## 推荐学习顺序

学习资产是 Code Nest 很关键的“内容再利用”模块。它把博客、社区、CodePen、模拟面试这些内容沉淀成闪卡、知识节点、练习计划和面试题。建议按下面顺序读：

1. 先读 `LearningAssetSourceServiceImpl`，理解来源内容如何被拉成统一快照。
2. 再读 `LearningAssetTransformEngineImpl`，理解 MVP 转化引擎如何生成候选项。
3. 接着读 `LearningAssetServiceImpl`，理解转化记录、候选编辑、确认和丢弃。
4. 最后读 `LearningAssetPublishServiceImpl`，理解哪些资产直接发布，哪些资产进入后台审核。

## 源码地图

| 关注点 | 源码位置 |
| --- | --- |
| 用户端接口 | `xiaou-learning-asset/src/main/java/com/xiaou/learningasset/controller/user/UserLearningAssetController.java` |
| 管理端接口 | `xiaou-learning-asset/src/main/java/com/xiaou/learningasset/controller/admin/AdminLearningAssetController.java` |
| 转化记录和候选主流程 | `xiaou-learning-asset/src/main/java/com/xiaou/learningasset/service/impl/LearningAssetServiceImpl.java` |
| 来源快照加载 | `xiaou-learning-asset/src/main/java/com/xiaou/learningasset/service/impl/LearningAssetSourceServiceImpl.java` |
| MVP 转化引擎 | `xiaou-learning-asset/src/main/java/com/xiaou/learningasset/service/impl/LearningAssetTransformEngineImpl.java` |
| 发布和审核 | `xiaou-learning-asset/src/main/java/com/xiaou/learningasset/service/impl/LearningAssetPublishServiceImpl.java` |
| 状态枚举 | `xiaou-learning-asset/src/main/java/com/xiaou/learningasset/enums` |
| 初始化 SQL | `sql/v1.8.4/learning_asset.sql` |

## 用户侧接口域

| 接口 | 能力 |
| --- | --- |
| `/user/learning-assets/convert` | 发起内容转资产 |
| `/user/learning-assets/records/list` | 转化记录列表 |
| `/user/learning-assets/records/{id}` | 转化记录详情 |
| `/user/learning-assets/candidates/{id}` | 编辑候选资产 |
| `/user/learning-assets/records/{id}/confirm` | 确认候选 |
| `/user/learning-assets/candidates/{id}/discard` | 丢弃候选 |
| `/user/learning-assets/records/{id}/publish` | 发布资产 |
| `/user/learning-assets/records/{id}/retry` | 失败重试 |

## 管理侧接口域

| 接口 | 能力 |
| --- | --- |
| `/admin/learning-assets/candidates/list` | 候选资产列表 |
| `/admin/learning-assets/candidates/{id}` | 候选详情和编辑 |
| `/admin/learning-assets/candidates/{id}/approve` | 审核通过 |
| `/admin/learning-assets/candidates/{id}/merge` | 合并候选 |
| `/admin/learning-assets/candidates/{id}/reject` | 拒绝候选 |
| `/admin/learning-assets/statistics` | 统计数据 |

## 可转化来源

当前源码支持的来源类型是：

| `sourceType` | 来源 | 快照内容 |
| --- | --- | --- |
| `blog` | 博客文章 | 标题、摘要、正文、文章标签 |
| `community` | 社区帖子 | 标题、正文、标签，可复用 AI 摘要 |
| `codepen` | CodePen 作品 | 描述、HTML/CSS/JS 截断片段、标签 |
| `mock_interview` | 模拟面试报告 | AI 总结、建议、薄弱点 |

文档或产品上如果要扩展 SQL 优化记录、OJ 题解等来源，需要先在 `LearningAssetSourceServiceImpl.loadSnapshot` 增加新的 `sourceType` 分支。

## 转化模式和资产类型

转化模式定义在 `TransformMode`：`QUICK`、`STUDY`、`INTERVIEW`、`PRACTICE`。当前 MVP 引擎会从来源快照标题、摘要、内容和标签中抽取高亮信息，并生成候选资产。

不传 `transformMode` 时默认是 `STUDY`。`QUICK` 模式生成的闪卡数量更少，当前最多 1 张；其它模式最多 2 张闪卡。摘要会优先取来源摘要，没有摘要时取内容，最长保留 180 字。

后续可以持续沉淀：

| 资产类型 | code | 目标模块 | 发布方式 |
| --- | --- | --- | --- |
| 闪卡 | `flashcard` | `xiaou-flashcard` | 用户发布时直接创建私有卡组和卡片 |
| 知识节点 | `knowledge_node` | `xiaou-knowledge` | 进入管理端审核，通过后创建或合并知识节点 |
| 练习计划 | `practice_plan` | `xiaou-plan` | 用户发布时直接创建计划 |
| 面试题 | `interview_question` | `xiaou-interview` | 进入管理端审核，通过后创建或合并题单题目 |

## 端到端流程

一次完整转化通常是：

1. 用户在来源内容上点击“转为学习资产”。
2. 前端提交 `sourceType`、`sourceId`、`targetTypes`、`transformMode` 和可选 `extraTags`。
3. 后端加载来源快照，并计算 `sourceHash`，用于追踪来源内容。
4. 转化引擎生成候选项，写入 `learning_asset_candidate`，状态为 `DRAFT`。
5. 用户可以编辑候选标题、JSON 内容、标签、难度。
6. 用户确认候选，选中的变为 `SELECTED`，未选中的变为 `DISCARDED`。
7. 用户点击发布。闪卡和练习计划直接写入目标模块；知识节点和面试题变为 `REVIEWING`。
8. 管理员审核 `REVIEWING` 候选，选择通过、合并或拒绝。
9. 每次发布、提交审核、通过、合并、拒绝都会写 `learning_asset_publish_log`。
10. 系统通过通知中心告诉用户发布或审核结果。

## 状态流转

转化记录状态定义在 `LearningAssetRecordStatus`：

| 状态 | 说明 |
| --- | --- |
| `PENDING_CONFIRM` | 用户还需要确认候选项 |
| `REVIEWING` | 至少一个候选项进入管理员审核，且还没有发布项 |
| `PUBLISHED` | 所有候选项都已发布 |
| `PARTIAL_PUBLISHED` | 部分发布，或同时存在发布和审核中候选 |
| `FAILED` | 未生成候选，或候选全部被拒绝且没有待处理项 |

候选项状态定义在 `LearningAssetCandidateStatus`：

| 状态 | 允许动作 |
| --- | --- |
| `DRAFT` | 用户编辑、选择、丢弃 |
| `SELECTED` | 用户发布 |
| `REVIEWING` | 管理员编辑、通过、合并、拒绝 |
| `PUBLISHED` | 只读，带目标模块和目标 ID |
| `DISCARDED` | 用户丢弃后不再发布 |
| `REJECTED` | 管理员拒绝，记录审核备注 |

用户只能编辑 `DRAFT` 和 `SELECTED`。发布时如果显式传 `candidateIds`，这些候选也必须是 `SELECTED`；否则会返回“仅支持发布已确认候选项”。

## 发布规则

`LearningAssetPublishServiceImpl.publish` 会把候选拆成两类：

| 类型 | 行为 |
| --- | --- |
| 直接发布 | 闪卡和练习计划由用户直接写入目标模块 |
| 审核发布 | 知识节点和面试题进入后台审核，管理员通过后写入目标模块 |

每次发布、审核、合并或拒绝都会写入 `learning_asset_publish_log`。发布结果还会通过通知中心发送给用户，方便用户知道哪些内容已沉淀成功、哪些还在审核。

直接发布时的写入规则：

| 资产 | 写入方式 |
| --- | --- |
| 闪卡 | 创建一个私有卡组，名称为“来源标题 · 学习闪卡”，再批量创建卡片 |
| 练习计划 | 创建计划，`planType = 2`、`targetType = 3`、从当天开始、默认每天重复、不启用提醒 |

审核发布时的写入规则：

| 资产 | 通过审核 | 合并 |
| --- | --- | --- |
| 知识节点 | 必须指定知识图谱 `mapId`，可指定 `parentId`，创建节点 | 必须指定已有知识节点 ID |
| 面试题 | 必须指定题单 `questionSetId`，创建题目 | 必须指定已有面试题 ID |

拒绝候选会把候选状态设为 `REJECTED`，记录审核备注；如果所有候选都被拒绝且没有待处理项，记录状态会变成 `FAILED`，失败原因是“候选资产未通过审核”。

## 数据完整性要求

- `sourceType`、`sourceId`、`sourceSnapshot` 和 `sourceHash` 必须保留，便于追溯生成来源。
- `targetModule` 和 `targetId` 是发布后定位目标内容的关键字段。
- 候选内容的 `contentJson` 在管理端编辑时会先做 JSON 解析校验。
- 用户只能编辑 `DRAFT` 和 `SELECTED` 候选；管理员只能审核 `REVIEWING` 候选。

## 数据表理解

| 表 | 说明 |
| --- | --- |
| `learning_asset_record` | 一次转化任务，保存来源、模式、目标类型、摘要、候选数量和记录状态 |
| `learning_asset_candidate` | 生成出来的候选学习资产，保存资产类型、内容 JSON、状态、目标模块和目标 ID |
| `learning_asset_publish_log` | 发布、提交审核、管理员编辑、通过、合并、拒绝等操作日志 |

## 常见坑

| 问题 | 原因 | 建议 |
| --- | --- | --- |
| 提交发布时报“当前没有可发布的候选项” | 没有 `SELECTED` 候选，或传入 ID 不正确 | 先调用确认接口选中候选 |
| 管理端编辑失败 | `contentJson` 不是合法 JSON | 前端提供 JSON 校验和格式化 |
| CodePen 转化内容为空 | 付费作品未 Fork 时拿不到源码 | 先确认当前用户有源码查看权限 |
| 知识节点审核通过失败 | 未指定图谱或图谱不存在 | 审核表单必须选择 `mapId` |
| 面试题审核通过失败 | 未指定题单或题单不存在 | 审核表单必须选择 `questionSetId` |
| 来源类型扩展后无效 | `loadSnapshot` 没有对应分支 | 新来源要补服务分支、DTO、测试和文档 |

## 开发注意

- 转化记录和候选资产要保留来源 ID，避免资产失去上下文。
- 用户确认和管理端审核是两个不同阶段。
- AI 生成内容需要人工可编辑。
- 发布后应和目标模块建立映射关系。

## 验证清单

| 场景 | 预期 |
| --- | --- |
| 不传转化模式 | 默认使用 `STUDY` |
| 传入未知 `targetTypes` | 返回未解析到有效资产类型 |
| 转化结果为空 | 记录状态为 `FAILED`，写入失败原因 |
| 用户编辑 `REVIEWING` 候选 | 返回当前状态不允许编辑 |
| 用户发布闪卡 | 创建私有卡组和卡片，候选变为 `PUBLISHED` |
| 用户发布知识节点 | 候选进入 `REVIEWING`，等待管理员 |
| 管理员通过知识节点 | 创建知识节点，候选变为 `PUBLISHED` |
| 管理员拒绝候选 | 候选变为 `REJECTED`，用户收到通知 |
| 所有候选都发布完成 | 转化记录变为 `PUBLISHED` |


---

## 深度拆解

### 一、转化流程深度分析

`LearningAssetServiceImpl.convert` 完整流程：

```text
1. sourceService.loadSnapshot(userId, request) → 按sourceType加载来源快照
2. parseTransformMode(request.transformMode) → 空则默认STUDY
3. parseTargetTypes(request.targetTypes) → 逐个匹配TargetAssetType，空则抛"未解析到有效资产类型"
4. transformEngine.transform(snapshot, mode, targetTypes) → 生成候选列表
5. 构建 LearningAssetRecord:
   ├─ sourceSnapshot = JSONUtil.toJsonStr(snapshot) (完整快照JSON)
   ├─ sourceHash = DigestUtil.sha256Hex(snapshot JSON) (内容指纹)
   ├─ status = candidates为空 ? FAILED : PENDING_CONFIRM
   ├─ failReason = candidates为空 ? "未生成任何候选学习资产" : null
   ├─ totalCandidates = candidates.size()
   └─ publishedCandidates = 0
6. recordMapper.insert(record)
7. 逐个插入 LearningAssetCandidate:
   ├─ dedupeKey = MD5(assetType + ":" + title) (去重键)
   ├─ status = DRAFT
   └─ sortOrder = 递增
8. return getRecordDetail(userId, record.getId())
```

**sourceHash 追踪来源变化**：`sha256Hex(snapshot JSON)` 对完整快照做哈希，如果来源内容没变，重试时 hash 相同。但 **重试不会检查 hash 是否重复**——每次 retry 都会创建新的 record 和 candidates，不会复用已有记录。

**dedupeKey 去重键未被使用**：`MD5(assetType + ":" + title)` 计算了去重键，但 Mapper 中没有 `selectByDedupeKey` 方法，也没有唯一约束。**这个字段目前只存储不查询**——同一来源重复转化会产生相同 dedupeKey 的多条候选。

**重试 retry 创建全新记录**：`retry` 方法从旧 record 读取 sourceType/sourceId/transformMode/targetTypes，然后调用 `convert`。这会创建一条全新的 record 和全新的 candidates——**旧记录和旧候选不会被清理或标记**。

### 二、来源快照加载深度分析

`LearningAssetSourceServiceImpl.loadSnapshot`：

```text
switch(sourceType):
├─ "blog" → blogArticleService.getArticleDetail(sourceId)
│   └─ title + summary + content + tags
├─ "community" → communityPostService.getPostDetail(sourceId)
│   ├─ summary优先取aiSummary
│   ├─ useExistingSummary=true且aiSummary为空 → 调用AI摘要服务
│   └─ tags从CommunityTag.getName()提取
├─ "codepen" → codePenService.getDetail(sourceId, userId)
│   ├─ content = description + html(截断400字) + css(截断400字) + js(截断400字)
│   └─ 付费作品未Fork时拿不到源码
├─ "mock_interview" → mockInterviewService.getReport(userId, sourceId)
│   ├─ content = aiSummary + aiSuggestion + weakPoints
│   └─ tags = weakPoints + extraTags
└─ default → 抛"暂不支持的来源类型"
```

**跨模块服务依赖**：SourceServiceImpl 直接注入了 BlogArticleService、CommunityPostService、CodePenService、MockInterviewService 四个外部模块服务。**没有降级或熔断**——如果博客服务异常，整个转化接口会失败。

**社区 AI 摘要触发条件**：`useExistingSummary=true` 且 `aiSummary` 为空时才调用 `communityAiSummaryService.getSummary`。但 `retry` 请求硬编码 `useExistingSummary=TRUE`——这意味着重试时如果社区帖子已有 AI 摘要，不会重新生成。

**CodePen 源码截断**：HTML/CSS/JS 各截断 400 字符，总内容约 1200 字符。对于大型作品，截断后可能丢失关键逻辑。

**mock_interview 来源 ID 映射**：`sourceId` 是面试会话 ID，但 `report.getSessionId()` 被用作快照的 `sourceId`——如果前端传的 `sourceId` 和 `sessionId` 不一致，快照中的 `sourceId` 会被覆盖为 `sessionId`。

### 三、MVP 转化引擎深度分析

`LearningAssetTransformEngineImpl.transform`：

```text
1. summary = buildSummary(snapshot)
   ├─ 优先取summary，空则取content
   ├─ 都空则取title，都空则"未命名内容"
   └─ maxLength(180)
2. highlights = extractHighlights(snapshot, summary)
   ├─ 先加所有tags
   ├─ 再按[，。；]分句，取>=4字的句子
   └─ 最多4个高亮点
3. 按targetTypes逐个生成:
   ├─ FLASHCARD → buildFlashcards (QUICK最多1张，其他最多2张)
   ├─ PRACTICE_PLAN → buildPracticePlans (按highlights生成任务)
   ├─ KNOWLEDGE_NODE → buildKnowledgeNode (取第一个tag作标题)
   └─ INTERVIEW_QUESTION → buildInterviewQuestion (生成"请说明X的核心机制与适用场景")
```

**confidenceScore 硬编码**：闪卡 0.86、练习计划 0.82、知识节点 0.74、面试题 0.71。这些分数不反映实际内容质量——**无论来源内容多差，同类型候选的置信度都相同**。

**TransformMode 只影响闪卡数量**：`QUICK` 模式最多 1 张闪卡，其他模式最多 2 张。但 `INTERVIEW` 和 `PRACTICE` 模式与 `STUDY` 模式在知识节点、面试题和练习计划的生成上**完全相同**——模式名称暗示差异，但实际没有。

**高亮提取依赖中文标点**：`summary.split("[，。；]")` 按中文逗号、句号、分号分句。英文内容用逗号句号分隔时，整段内容会被当作一个"句子"，可能超过 4 字但无法拆分出更多高亮点。

**知识节点标题取第一个 tag**：`firstNonBlankTag(tags, title)` 优先取第一个非空 tag 作为标题。如果 tags 为空，fallback 到来源标题。这意味着一个"Java并发"标签会生成标题为"Java并发"的知识节点——**标签粒度可能不适合做节点标题**。

### 四、候选确认与丢弃深度分析

**确认候选** `confirmCandidates`：

```text
1. requireOwnedRecord → 校验归属
2. selectByRecordId → 所有候选
3. for each candidate:
   ├─ candidateIds.contains(id) → SELECTED
   └─ 否则 → DISCARDED
4. record.status = PENDING_CONFIRM (注意：不根据候选状态更新)
5. recordMapper.update
```

**确认后记录状态仍为 PENDING_CONFIRM**：`confirmCandidates` 无论选了多少候选，都把 record 状态设为 `PENDING_CONFIRM`。这意味着确认操作不会推进记录状态——**需要再调用 publish 才会推进**。

**丢弃候选** `discardCandidate`：

```text
1. 校验候选存在且属于当前用户
2. 已DISCARDED → 幂等返回详情
3. 非DRAFT且非SELECTED → 抛"当前状态不允许丢弃"
4. updateStatus → DISCARDED
5. record.status = PENDING_CONFIRM (同样不推进)
```

**丢弃已发布候选不被阻止**：丢弃只检查 DRAFT 和 SELECTED，但 PUBLISHED 和 REVIEWING 状态也不允许丢弃——这个逻辑是正确的。但 **丢弃操作不检查候选是否已被其他流程引用**（如闪卡已创建）。

### 五、发布机制深度分析

`LearningAssetPublishServiceImpl.publish`：

```text
1. requireOwnedRecord → 校验归属
2. selectByRecordId → 所有候选
3. filterPublishableCandidates → 只保留SELECTED的
4. publishFlashcards → 闪卡直接发布
5. publishPlans → 练习计划直接发布
6. submitReviewCandidates → 知识节点和面试题提交审核
7. updateRecordStatus → 根据候选状态推算记录状态
8. sendPublishSummaryNotification → 通知用户
```

**闪卡发布** `publishFlashcards`：

```text
1. 创建私有卡组: name = sourceTitle + " · 学习闪卡" (maxLength 100)
2. 逐个解析contentJson → frontContent + backContent + contentType
3. 逐个 updateStatus(candidate, PUBLISHED, deckId)
4. 逐个 logPublish("direct", ...)
5. batchCreateCards → 批量创建卡片
```

**闪卡 targetId 指向 deckId**：所有闪卡候选的 `targetId` 都设为卡组 ID，而不是卡片 ID。如果需要从候选定位到具体卡片，无法通过 `targetId` 实现——**卡片和候选之间没有一一映射**。

**练习计划发布** `publishPlans`：每个练习计划候选创建一个独立计划，`planType=2`、`targetType=3`、从当天开始、每天重复、不启用提醒。这些参数硬编码，不可配置。

**审核提交** `submitReviewCandidates`：知识节点和面试题候选状态改为 `REVIEWING`，`targetId=null`。提交审核时 **不校验目标模块服务是否可用**——如果知识模块服务异常，审核通过时才会失败。

### 六、管理端审核深度分析

**审核通过** `approve`：

```text
1. requireReviewingCandidate → 校验REVIEWING状态
2. switch(assetType):
   ├─ KNOWLEDGE_NODE → approveKnowledgeNode
   │   ├─ mapId == null → 抛"必须指定图谱"
   │   ├─ knowledgeMapService.getById(mapId) → null → 抛"图谱不存在"
   │   └─ knowledgeNodeService.createNode(mapId, request)
   └─ INTERVIEW_QUESTION → approveInterviewQuestion
       ├─ questionSetId == null → 抛"必须指定题单"
       ├─ interviewQuestionSetService.getQuestionSetById → null → 抛"题单不存在"
       └─ interviewQuestionService.createQuestion(question)
3. updateStatus(candidate, PUBLISHED, targetId)
4. logPublish("admin_publish", ...)
5. updateRecordStatus → 重算记录状态
6. sendAuditResultNotification → 通知用户审核通过
```

**合并** `merge`：

```text
1. requireReviewingCandidate
2. switch(assetType):
   ├─ KNOWLEDGE_NODE → mergeKnowledgeNode(existingTargetId)
   │   ├─ existingTargetId == null → 抛"必须指定目标节点"
   │   └─ knowledgeNodeService.getById → null → 抛"节点不存在"
   └─ INTERVIEW_QUESTION → mergeInterviewQuestion(existingTargetId)
       ├─ existingTargetId == null → 抛"必须指定目标题目"
       └─ interviewQuestionService.getQuestionById → null → 抛"题目不存在"
3. updateStatus(candidate, PUBLISHED, existingTargetId)
4. logPublish("admin_merge", ...)
5. updateRecordStatus
6. sendAuditResultNotification → 通知用户已合并
```

**合并不更新目标内容**：`mergeKnowledgeNode` 和 `mergeInterviewQuestion` 只校验目标存在，**不把候选内容合并到目标节点/题目中**。合并操作只是把候选标记为 PUBLISHED 并关联到目标 ID——实际的知识融合需要人工完成。

**拒绝** `reject`：

```text
1. requireReviewingCandidate
2. updateStatus(candidate, REJECTED)
3. logPublish("admin_reject", ...)
4. updateRecordStatus → 如果全部REJECTED且无待处理 → FAILED
5. sendAuditResultNotification → 通知用户未通过
```

### 七、记录状态推算深度分析

`updateRecordStatus` 逻辑：

```text
publishedCount = count(PUBLISHED)
hasReviewing = any(REVIEWING)
hasPending = any(DRAFT || SELECTED)
hasRejected = any(REJECTED)

if (publishedCount == allCandidates.size() && publishedCount > 0):
    status = PUBLISHED
elif (hasReviewing && publishedCount > 0):
    status = PARTIAL_PUBLISHED
elif (hasReviewing):
    status = REVIEWING
elif (publishedCount > 0):
    status = PARTIAL_PUBLISHED
elif (hasRejected && !hasPending):
    status = FAILED (failReason = "候选资产未通过审核")
else:
    status = PENDING_CONFIRM
```

**DISCARDED 候选不影响状态推算**：丢弃的候选既不算 published 也不算 rejected，如果所有候选都被丢弃（没有 published、没有 reviewing、没有 rejected、没有 pending），记录状态会变成 `PENDING_CONFIRM`——**但此时已经没有可确认的候选了**，记录处于一个无意义的状态。

**部分发布判定**：只要 publishedCount > 0 且还有 reviewing，就是 PARTIAL_PUBLISHED。但如果 publishedCount > 0 且只有 discarded 剩余（没有 reviewing），也是 PARTIAL_PUBLISHED——**即使剩余候选永远不会发布**。

### 八、统计查询深度分析

`getStatistics` 聚合逻辑：

```text
1. totalTransforms = countAll
2. successTransforms = countSuccess (status != FAILED)
3. reviewingTransforms = countStatus(REVIEWING)
4. publishedTransforms = countStatus(PUBLISHED) + countStatus(PARTIAL_PUBLISHED)
5. totalCandidates = countAll
6. editedCandidates = countEdited (update_time > create_time)
7. submittedCandidates = countByStatuses(REVIEWING, PUBLISHED)
8. rejectedCandidates = countByStatuses(REJECTED)
9. transformSuccessRate = percent(success, total)
10. editRate = percent(edited, total)
11. rejectRate = percent(rejected, submitted)
12. sourceStats = GROUP BY source_type
13. assetStats = GROUP BY asset_type
14. failReasonStats = GROUP BY fail_reason (LIMIT 10)
15. topSourceStats = GROUP BY source_type, source_id (HAVING SUM(published) > 0, LIMIT 10)
```

**7 次独立 COUNT 查询**：`getStatistics` 调用了 7 次独立的 COUNT/SUM 查询（countAll、countSuccess、countStatus×2、countAll、countEdited、countByStatuses×2），没有合并为一条 SQL。当数据量大时，7 次全表扫描性能差。

**topSourceStats 聚合粒度问题**：`GROUP BY source_type, source_id, source_title` 按 source_title 分组——如果同一篇博客被修改标题后再次转化，会产生两条统计记录。

### 九、深度发现与坑点

#### 9.1 确认 BUG

| 编号 | BUG | 位置 | 说明 |
| --- | --- | --- | --- |
| BUG-1 | dedupeKey 存储但不查询 | `convert:92` | 计算了去重键但Mapper无查询方法，无唯一约束，重复转化产生相同dedupeKey |
| BUG-2 | retry 创建全新记录不清理旧数据 | `retry:203-212` | 重试产生新record+新candidates，旧记录和旧候选残留 |
| BUG-3 | 确认后记录状态不推进 | `confirmCandidates:169` | 无论选了多少候选，record.status始终设为PENDING_CONFIRM |
| BUG-4 | 闪卡targetId指向deckId非cardId | `publishFlashcards:268` | 无法从候选定位到具体卡片，候选和卡片无一一映射 |
| BUG-5 | 合并不更新目标内容 | `mergeKnowledgeNode/mergeInterviewQuestion` | 只校验目标存在并关联ID，不把候选内容融合到目标 |
| BUG-6 | 全部丢弃后记录状态无意义 | `updateRecordStatus` | 所有候选DISCARDED时记录变为PENDING_CONFIRM，但无可确认候选 |
| BUG-7 | TransformMode只影响闪卡数量 | `buildFlashcards:55` | INTERVIEW/PRACTICE模式与STUDY模式在非闪卡资产生成上完全相同 |
| BUG-8 | confidenceScore硬编码 | `buildFlashcards:67` 等 | 无论来源内容质量，同类型候选置信度相同 |

#### 9.2 设计风险

| 编号 | 风险 | 说明 |
| --- | --- | --- |
| RISK-1 | 跨模块服务无降级 | SourceServiceImpl直接注入4个外部服务，任一异常导致整个转化失败 |
| RISK-2 | 高亮提取依赖中文标点 | split("[，。；]")对英文内容效果差，可能整段当作一个高亮点 |
| RISK-3 | 统计7次独立COUNT | getStatistics调用7次全表COUNT，数据量大时慢 |
| RISK-4 | CodePen源码截断丢失关键逻辑 | HTML/CSS/JS各截断400字符，大型作品关键代码可能丢失 |
| RISK-5 | 知识节点标题取第一个tag | tag粒度可能不适合做节点标题，如"Java"太宽泛 |
| RISK-6 | sourceSnapshot完整JSON存储 | 每次转化都存完整快照JSON，博客正文可能很大 |

#### 9.3 架构设计亮点

| 编号 | 亮点 | 说明 |
| --- | --- | --- |
| H-1 | sourceHash 内容指纹 | SHA256追踪来源变化，可判断内容是否更新 |
| H-2 | 双轨发布机制 | 闪卡/计划直接发布，知识节点/面试题走审核，兼顾效率和质量 |
| H-3 | 候选可编辑 | 用户和管理员都能编辑候选内容，AI生成内容不锁死 |
| H-4 | 审核三动作 | 通过、合并、拒绝覆盖所有审核场景 |
| H-5 | 发布日志全记录 | 每次发布/审核/合并/拒绝都写publish_log，完整审计链 |
| H-6 | 通知闭环 | 发布结果和审核结果都通过通知中心告知用户 |
| H-7 | 记录状态自动推算 | updateRecordStatus根据候选状态自动推算记录状态 |
| H-8 | 标签合并去重 | mergeTags用LinkedHashSet保留顺序且去重 |
| H-9 | 管理端JSON校验 | updateReviewCandidate先JSONUtil.parse校验contentJson合法性 |
| H-10 | 统计维度丰富 | 来源统计、资产统计、失败原因统计、Top来源统计四维度 |

#### 9.4 源码导航速查

| 想了解 | 读什么 |
| --- | --- |
| 发起转化 | `LearningAssetServiceImpl.java` — convert + sourceHash + dedupeKey |
| 候选确认 | `LearningAssetServiceImpl.java` — confirmCandidates + 选中/丢弃分流 |
| 候选丢弃 | `LearningAssetServiceImpl.java` — discardCandidate + 幂等 |
| 失败重试 | `LearningAssetServiceImpl.java` — retry + 创建全新记录 |
| 来源快照 | `LearningAssetSourceServiceImpl.java` — loadSnapshot + 四来源分支 |
| 博客快照 | `LearningAssetSourceServiceImpl.java` — loadBlogSnapshot |
| 社区快照 | `LearningAssetSourceServiceImpl.java` — loadCommunitySnapshot + AI摘要 |
| CodePen快照 | `LearningAssetSourceServiceImpl.java` — loadCodePenSnapshot + 截断 |
| 面试快照 | `LearningAssetSourceServiceImpl.java` — loadMockInterviewSnapshot |
| 转化引擎 | `LearningAssetTransformEngineImpl.java` — transform + 四资产类型 |
| 闪卡生成 | `LearningAssetTransformEngineImpl.java` — buildFlashcards + QUICK/其他 |
| 练习计划生成 | `LearningAssetTransformEngineImpl.java` — buildPracticePlans |
| 知识节点生成 | `LearningAssetTransformEngineImpl.java` — buildKnowledgeNode |
| 面试题生成 | `LearningAssetTransformEngineImpl.java` — buildInterviewQuestion |
| 发布 | `LearningAssetPublishServiceImpl.java` — publish + 双轨分流 |
| 闪卡发布 | `LearningAssetPublishServiceImpl.java` — publishFlashcards + 卡组+卡片 |
| 计划发布 | `LearningAssetPublishServiceImpl.java` — publishPlans + planType=2 |
| 审核提交 | `LearningAssetPublishServiceImpl.java` — submitReviewCandidates |
| 审核通过 | `LearningAssetPublishServiceImpl.java` — approve + 创建目标内容 |
| 合并 | `LearningAssetPublishServiceImpl.java` — merge + 校验目标存在 |
| 拒绝 | `LearningAssetPublishServiceImpl.java` — reject + 通知用户 |
| 状态推算 | `LearningAssetPublishServiceImpl.java` — updateRecordStatus |
| 统计 | `LearningAssetPublishServiceImpl.java` — getStatistics + 7次COUNT |
| 转化记录域 | `LearningAssetRecord.java` — sourceHash + summaryText + failReason |
| 候选域 | `LearningAssetCandidate.java` — dedupeKey + confidenceScore + contentJson |
| 发布日志域 | `LearningAssetPublishLog.java` — publishType + publishResult + message |


## 相关模块

| 模块 | 关系 | 说明 |
| --- | --- | --- |
| [公共底座](/modules/common) | 强依赖 | 学习资产模块依赖公共底座的统一响应、分页和异常处理 |
| [鉴权与用户体系](/modules/auth) | 强依赖 | 学习资产创建和管理需要用户登录态 |
| [用户账户与个人中心](/modules/user-account) | 强依赖 | 用户学习资产依赖用户信息 |
| [积分与抽奖](/modules/points) | 间接关联 | 学习资产可能触发积分奖励 |
| [题库与成长闭环](/modules/interview-and-growth) | 强依赖 | 学习资产与成长闭环紧密关联 |
| [系统运营后台](/modules/system-ops) | 被依赖 | 学习资产管理界面在管理端 |
