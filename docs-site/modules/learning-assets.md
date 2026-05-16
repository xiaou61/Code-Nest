# 学习资产

学习资产模块负责把平台内容转化为可复习、可练习、可沉淀的学习材料。

## 功能入口

| 端 | 页面 |
| --- | --- |
| 用户端 | `/learning-assets` |
| 管理端 | `/learning-assets/review`、`/learning-assets/statistics` |
| 后端模块 | `xiaou-learning-asset` |

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

- 社区帖子。
- 博客文章。
- CodePen 作品。
- AI 模拟面试报告。
- SQL 优化记录。
- 其他学习过程产物。

## 转化模式和资产类型

转化模式定义在 `TransformMode`：`QUICK`、`STUDY`、`INTERVIEW`、`PRACTICE`。当前 MVP 引擎会从来源快照标题、摘要、内容和标签中抽取高亮信息，并生成候选资产。

后续可以持续沉淀：

| 资产类型 | code | 目标模块 | 发布方式 |
| --- | --- | --- | --- |
| 闪卡 | `flashcard` | `xiaou-flashcard` | 用户发布时直接创建私有卡组和卡片 |
| 知识节点 | `knowledge_node` | `xiaou-knowledge` | 进入管理端审核，通过后创建或合并知识节点 |
| 练习计划 | `practice_plan` | `xiaou-plan` | 用户发布时直接创建计划 |
| 面试题 | `interview_question` | `xiaou-interview` | 进入管理端审核，通过后创建或合并题单题目 |

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

## 发布规则

`LearningAssetPublishServiceImpl.publish` 会把候选拆成两类：

| 类型 | 行为 |
| --- | --- |
| 直接发布 | 闪卡和练习计划由用户直接写入目标模块 |
| 审核发布 | 知识节点和面试题进入后台审核，管理员通过后写入目标模块 |

每次发布、审核、合并或拒绝都会写入 `learning_asset_publish_log`。发布结果还会通过通知中心发送给用户，方便用户知道哪些内容已沉淀成功、哪些还在审核。

## 数据完整性要求

- `sourceType`、`sourceId`、`sourceSnapshot` 和 `sourceHash` 必须保留，便于追溯生成来源。
- `targetModule` 和 `targetId` 是发布后定位目标内容的关键字段。
- 候选内容的 `contentJson` 在管理端编辑时会先做 JSON 解析校验。
- 用户只能编辑 `DRAFT` 和 `SELECTED` 候选；管理员只能审核 `REVIEWING` 候选。

## 开发注意

- 转化记录和候选资产要保留来源 ID，避免资产失去上下文。
- 用户确认和管理端审核是两个不同阶段。
- AI 生成内容需要人工可编辑。
- 发布后应和目标模块建立映射关系。
