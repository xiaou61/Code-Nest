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

## 学习和验证清单

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
