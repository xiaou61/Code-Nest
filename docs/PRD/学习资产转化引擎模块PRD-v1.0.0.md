# 学习资产转化引擎模块 PRD v1.0.0

## 📋 项目概述

### 🎯 项目背景

Code Nest 已经具备较强的内容生产与学习能力，包括：

- **内容源**：博客文章、社区帖子、CodePen 作品、AI 模拟面试报告
- **学习承接**：闪卡、知识图谱、面试题库、计划打卡、学习驾驶舱、成长闭环自动驾驶

当前平台的问题不是“没有内容”或“没有学习工具”，而是二者之间缺少一条低摩擦、高复用、可持续沉淀的连接链路：

- 用户阅读完一篇优质博客后，无法一键沉淀为复习卡片
- 用户在社区看到高价值经验帖后，难以转成结构化知识点
- 用户看完一个 CodePen 作品后，理解停留在浏览层，没有自动形成“概念 - 实现 - 练习”的学习链路
- 用户完成一次 AI 模拟面试后，报告虽然详细，但薄弱点没有自动进入后续复习系统

这导致平台内部形成了“内容消费”和“学习执行”两套相对割裂的体验，内容价值无法被长期复利。

### 💡 核心价值

- **把内容变成资产**：把一次浏览、一次阅读、一次面试，转化为可复习、可训练、可追踪的学习资产
- **形成平台闭环**：打通内容模块与学习模块，让博客、社区、CodePen、面试报告都能反哺成长体系
- **降低学习启动成本**：从“我看完了但不知道接下来怎么学”变成“我一键生成下一步学习动作”
- **沉淀结构化知识**：把原本非结构化内容提炼为闪卡、知识点、练习清单、题目草稿
- **提升内容复用率**：优质内容不只被阅读一次，而是不断转化为学习资源，持续发挥价值

### 🏠 模块定位

新增 `学习资产转化引擎`，建议作为一个新的业务模块存在，模块名建议为：

- **推荐方案**：`xiaou-learning-asset`

职责定位如下：

- **内容聚合层**：统一接入博客、社区、CodePen、AI 模拟面试报告等内容源
- **转化编排层**：负责 AI 提炼、结构化输出、去重、候选资产管理、发布状态管理
- **资产分发层**：将转化结果投递到闪卡、知识图谱、面试题库、计划中心等目标模块

同时复用现有 `xiaou-ai` 模块作为 AI 能力提供方，不建议将“转化记录、候选资产、发布状态”直接塞进 `xiaou-ai`，以避免 AI 能力层与业务状态层耦合。

### 🔗 模块关联

- **xiaou-blog**：博客文章内容源
- **xiaou-community**：社区帖子内容源，复用已有 AI 摘要能力
- **xiaou-codepen**：作品详情内容源
- **xiaou-mock-interview**：面试报告内容源
- **xiaou-flashcard**：闪卡卡组与卡片落地
- **xiaou-knowledge**：知识图谱节点候选与发布
- **xiaou-interview**：练习题草稿、题单候选落地
- **xiaou-plan**：练习清单/行动项同步
- **xiaou-notification**：转化完成、发布成功、审核反馈通知
- **xiaou-ai**：AI 提炼、标签抽取、结构化生成、摘要补全
- **vue3-user-front**：用户端一键转化与我的学习资产
- **vue3-admin-front**：管理端质量审核、候选发布、规则监控

## 🚀 一期目标与边界

### 一期目标

一期目标是先把“内容详情页一键转化 -> 候选资产预览 -> 用户确认 -> 发布到学习系统”这条链路跑通。

### 一期范围

- 支持 4 类内容源：
  - 博客文章
  - 社区帖子
  - CodePen 作品
  - AI 模拟面试报告
- 支持 4 类学习资产：
  - 闪卡卡组
  - 知识图谱节点候选
  - 练习清单
  - 面试题草稿
- 支持 2 个主入口：
  - 内容详情页的 `转为学习资产`
  - 用户端 `我的学习资产`
- 支持管理员审核与发布：
  - 知识图谱节点候选
  - 面试题草稿

### 一期非目标

- 不做全自动公开发布，所有平台级资产必须保留确认与审核链路
- 不做完整批量导入工坊，作为二期扩展
- 不做自动生成复杂图谱边关系，先生成节点候选与关联建议
- 不做跨内容多源混合转化，先以单个内容源为单位
- 不做通用文件导入（PDF、Word、外链网页），一期仅支持站内内容

## 👤 目标用户与使用场景

### 用户角色

- **学习型用户**：希望把阅读和浏览转成可执行的复习材料
- **求职型用户**：希望把面试反馈和项目经验沉淀成后续训练资产
- **内容创作者**：希望让自己的文章、作品被更多人转成学习资源
- **平台运营/审核人员**：希望从优质内容中筛出可平台化的知识节点和题目草稿

### 典型场景

#### 场景 1：博客文章转闪卡

用户读完一篇“Redis 缓存击穿实战”博客后，点击 `转为学习资产`，系统自动提炼：

- 关键概念
- 高频面试问答
- 易错点
- 延伸练习项

用户确认后，生成一个个人卡组，后续进入闪卡复习。

#### 场景 2：社区经验帖转练习清单

用户看到一篇“秋招二面挂掉复盘”的社区帖子，点击转化后生成：

- 经验总结卡
- 薄弱知识点列表
- 后续专项练习清单

随后一键同步到计划打卡或成长自动驾驶。

#### 场景 3：CodePen 作品转知识点

用户浏览一个关于“虚拟列表 + IntersectionObserver”的作品，点击转化后生成：

- 实现步骤卡
- 核心 API 卡
- 性能注意事项
- 可提交到知识图谱的节点候选

#### 场景 4：面试报告转复盘资产

用户完成一次 AI 模拟面试，进入报告页后点击转化，系统自动生成：

- 薄弱点闪卡
- 错因复盘卡
- 下一轮专项练习清单
- 面试题草稿候选

## 🧭 产品方案总览

### 1. 双入口策略

#### 1.1 内容详情页一键转化（一期主入口）

在以下页面增加统一入口按钮 `转为学习资产`：

- 博客详情页
- 社区帖子详情页
- CodePen 作品详情页
- AI 模拟面试报告页

特点：

- 路径最短
- 转化意图最强
- 最适合拉起首轮使用量

#### 1.2 我的学习资产（一期结果承接页）

用户端新增 `我的学习资产` 页面，用于统一查看：

- 转化记录
- 待确认候选资产
- 已发布的个人资产
- 审核中的平台候选资产

#### 1.3 学习资产工坊（二期）

二期新增统一导入工坊，支持：

- 从我的博客/帖子/作品/报告中批量选择内容
- 多内容对比转化
- 批量生成资产
- 批量发布与归类

### 2. 核心链路

```text
内容详情页点击转化
→ 拉取源内容快照
→ AI 结构化提炼
→ 生成候选资产
→ 用户勾选/编辑/去重
→ 发布到对应学习模块
→ 同步学习驾驶舱/计划系统/通知中心
```

### 3. 资产类型设计

#### 3.1 闪卡卡组

适合直接落地到 `xiaou-flashcard`：

- 概念卡：定义、原理、关键点
- 对比卡：区别、适用场景、优缺点
- 易错卡：误区、边界条件、注意事项
- 面试问答卡：问题 + 精炼回答

#### 3.2 知识图谱节点候选

适合沉淀到 `xiaou-knowledge`，但一期不直接公开发布：

- 节点名称
- 节点摘要
- 标签/分类
- 推荐父节点
- 关联来源
- 推荐前置知识

说明：

- 一期作为 **候选节点** 保存，提交管理员审核后再进入正式图谱
- 二期再支持图谱边关系自动建议

#### 3.3 练习清单

适合沉淀到 `xiaou-plan` 或学习驾驶舱待办体系：

- 学习动作
- 预计时长
- 推荐优先级
- 推荐执行顺序
- 关联模块跳转路径

示例：

- 刷 3 道 Redis 缓存一致性相关题
- 复习 10 张 JVM 内存模型闪卡
- 阅读 1 个虚拟列表源码案例

#### 3.4 面试题草稿

适合沉淀到 `xiaou-interview`，一期作为候选题进入审核态：

- 题目标题
- 题目描述
- 参考答案摘要
- 考察知识点
- 难度建议
- 来源链接与来源类型

### 4. 平台资产与个人资产拆分

为了兼顾当前模块权限边界，一期将转化结果分成两类：

#### 4.1 个人资产

用户确认后可直接落地：

- 闪卡卡组
- 练习清单

#### 4.2 平台候选资产

用户确认后进入候选池，由运营/管理员审核后再发布：

- 知识图谱节点候选
- 面试题草稿

这样既能让用户立刻获得收益，又不会把 AI 生成内容直接灌入平台公共内容池。

## 🧩 各内容源连接方式

### 1. 博客文章

#### 输入信息

- 标题
- 摘要
- 正文 Markdown/HTML
- 标签
- 分类
- 作者信息

#### 推荐转化结果

- 闪卡卡组
- 知识图谱节点候选
- 面试题草稿
- 练习清单

#### 转化重点

- 提取文章中的知识主线
- 提炼高密度概念与结论
- 识别适合做题或提问的知识点
- 识别文中的经验结论、注意事项

### 2. 社区帖子

#### 输入信息

- 标题
- 正文
- 标签
- 已有 AI 摘要
- 评论热词（可选）

#### 推荐转化结果

- 闪卡卡组
- 练习清单
- 知识节点候选

#### 转化重点

- 从经验帖中抽取“经验卡”和“踩坑卡”
- 从讨论帖中抽取“争议点”和“关键结论”
- 对问题求助帖优先生成“排查步骤”类清单

### 3. CodePen 作品

#### 输入信息

- 作品标题
- 作品描述
- HTML/CSS/JS 代码
- 标签/分类
- 作者说明

#### 推荐转化结果

- 闪卡卡组
- 知识节点候选
- 练习清单
- 面试题草稿

#### 转化重点

- 提炼关键 API、设计思路、核心实现步骤
- 提炼性能点、边界点、兼容性注意事项
- 生成“看懂后建议继续练什么”的实践清单

### 4. AI 模拟面试报告

#### 输入信息

- 总评
- 题目详情
- 追问记录
- 改进建议
- 知识点得分/薄弱点

#### 推荐转化结果

- 薄弱点闪卡
- 错因复盘卡
- 练习清单
- 面试题草稿

#### 转化重点

- 把“面试表现差”翻译成“后续应该练什么”
- 按薄弱点优先级生成复习卡和行动项
- 将高质量追问沉淀为题目草稿

## 🖥️ 用户端功能需求

### 1. 内容详情页转化入口

#### 1.1 统一按钮

- 按钮文案：`转为学习资产`
- 展示位置：详情页头部操作区或右侧工具栏
- 登录后可用，未登录点击跳转登录

#### 1.2 转化弹窗/抽屉

用户点击后打开统一转化面板，包含：

- 来源信息确认
- 资产类型勾选
- 转化风格选择
- 标签和目标方向补充
- 是否使用已有 AI 摘要

#### 1.3 转化风格

- **快速提炼**：更短、更偏总结
- **学习复习**：更适合闪卡和知识点
- **面试训练**：更偏问答和考点
- **实战拆解**：更偏步骤、练习和实现逻辑

### 2. 候选资产预览页

转化完成后，系统展示候选资产列表，支持：

- 勾选需要保留的条目
- 单条编辑标题/内容/标签
- 删除明显重复或低质量条目
- 对闪卡设置所属卡组
- 对练习清单设置预计时长和优先级
- 对平台候选资产提交审核说明

### 3. 我的学习资产

#### 3.1 页面模块

- 我发起的转化记录
- 待确认候选资产
- 已发布到个人学习系统的资产
- 已提交审核的公共候选资产
- 失败记录与重试入口

#### 3.2 列表筛选

- 按来源类型筛选
- 按资产类型筛选
- 按状态筛选
- 按创建时间排序

#### 3.3 状态定义

- `待解析`
- `解析中`
- `待确认`
- `已发布`
- `审核中`
- `部分发布`
- `失败`

### 4. 学习承接联动

用户完成发布后，系统可执行以下联动：

- 闪卡卡组创建成功后，给出“立即学习”按钮
- 练习清单创建成功后，给出“加入今日计划”按钮
- 面试题草稿提交后，显示审核状态
- 知识节点候选提交后，显示来源追踪和审核进度

## 🛠️ 管理端功能需求

### 1. 候选资产审核台

管理员新增 `学习资产审核` 页面，查看：

- 知识图谱节点候选
- 面试题草稿候选

支持操作：

- 查看来源快照
- 查看 AI 提炼结果
- 编辑内容
- 驳回
- 通过并发布
- 合并到已有节点/已有题目

### 2. 质量监控台

管理员可查看：

- 各来源类型转化成功率
- 各资产类型发布率
- 用户编辑率
- 驳回率
- 常见失败原因
- 高质量来源排行

### 3. 规则配置（二期可扩展）

- 不同来源的默认转化模板
- 不同资产类型的生成上限
- 敏感内容拦截
- 去重阈值配置
- 审核策略配置

## 🔄 状态机设计

### 1. 转化记录状态机

```text
待解析 → 解析中 → 待确认 → 已发布
                  ├→ 审核中 → 已发布
                  ├→ 部分发布
                  └→ 失败
```

### 2. 候选资产状态机

```text
草稿 → 已选中 → 已发布
      ├→ 审核中 → 已发布
      └→ 已丢弃
```

## 📐 关键业务规则

### 1. 去重规则

- 同一用户对同一来源重复发起转化时，默认提示使用最近一次结果
- 对候选资产生成 `dedupeKey`，避免同一来源多次生成完全相同的闪卡或题目草稿
- 闪卡发布前，优先检查用户同名卡组中是否已存在高相似内容
- 知识节点候选和题目草稿支持管理员合并到已存在内容

### 2. 质量保护

- 平台级资产一律不自动公开发布
- AI 置信度低的条目默认不勾选
- 对过短帖子、无结构内容、纯灌水内容限制转化能力
- 敏感词模块继续参与内容风控

### 3. 转化额度建议

- 普通用户每日免费转化次数可限制为 5 次
- 高级用户/VIP/活动期可放宽额度
- 对超长内容采用摘要 + 分段提炼策略，避免一次请求过重

### 4. 来源可见性

- 仅允许转化当前用户有权限访问的内容
- 已删除、已下架、被封禁内容不可继续发起转化
- 面试报告仅限本人转化

## 🗃️ 数据结构设计

### 1. 转化记录表 `learning_asset_record`

```sql
CREATE TABLE learning_asset_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '发起用户ID',
    source_type VARCHAR(32) NOT NULL COMMENT '来源类型：blog/community/codepen/mock_interview',
    source_id BIGINT NOT NULL COMMENT '来源内容ID',
    source_title VARCHAR(255) COMMENT '来源标题',
    source_author_id BIGINT COMMENT '来源作者ID',
    source_snapshot JSON COMMENT '来源快照',
    transform_mode VARCHAR(32) COMMENT '转化模式：quick/study/interview/practice',
    target_types VARCHAR(255) COMMENT '目标资产类型，逗号分隔',
    status VARCHAR(32) NOT NULL COMMENT '记录状态',
    source_hash VARCHAR(64) COMMENT '来源内容哈希',
    ai_provider VARCHAR(64) COMMENT 'AI服务提供方',
    ai_model VARCHAR(128) COMMENT '模型名称',
    summary_text TEXT COMMENT '转化摘要',
    fail_reason VARCHAR(500) COMMENT '失败原因',
    total_candidates INT DEFAULT 0,
    published_candidates INT DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);
```

### 2. 候选资产表 `learning_asset_candidate`

```sql
CREATE TABLE learning_asset_candidate (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    record_id BIGINT NOT NULL COMMENT '所属转化记录ID',
    user_id BIGINT NOT NULL COMMENT '所属用户ID',
    asset_type VARCHAR(32) NOT NULL COMMENT 'flashcard/knowledge_node/practice_plan/interview_question',
    title VARCHAR(255) NOT NULL,
    content_json JSON NOT NULL COMMENT '结构化内容',
    tags VARCHAR(255) COMMENT '标签',
    difficulty VARCHAR(32) COMMENT '难度建议',
    confidence_score DECIMAL(5,2) COMMENT 'AI置信分',
    dedupe_key VARCHAR(128) COMMENT '去重Key',
    target_module VARCHAR(64) COMMENT '目标模块',
    target_id BIGINT COMMENT '发布后的目标ID',
    status VARCHAR(32) NOT NULL COMMENT 'draft/selected/reviewing/published/discarded',
    sort_order INT DEFAULT 0,
    review_note VARCHAR(500) COMMENT '审核说明',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);
```

### 3. 发布日志表 `learning_asset_publish_log`

```sql
CREATE TABLE learning_asset_publish_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    candidate_id BIGINT NOT NULL,
    publish_type VARCHAR(32) NOT NULL COMMENT 'direct/review_submit/admin_publish',
    target_module VARCHAR(64) NOT NULL,
    target_id BIGINT COMMENT '目标记录ID',
    operator_id BIGINT COMMENT '操作人',
    publish_result VARCHAR(32) NOT NULL COMMENT 'success/failed',
    message VARCHAR(500) COMMENT '结果说明',
    created_at DATETIME NOT NULL
);
```

## 🔌 接口设计

### 1. 用户端接口

#### 1.1 发起转化

```http
POST /user/learning-assets/convert
```

请求体示例：

```json
{
  "sourceType": "blog",
  "sourceId": 123,
  "transformMode": "study",
  "targetTypes": ["flashcard", "knowledge_node", "practice_plan"],
  "useExistingSummary": true,
  "extraTags": ["Redis", "缓存"]
}
```

#### 1.2 查询转化详情

```http
GET /user/learning-assets/records/{id}
```

#### 1.3 查询我的转化记录

```http
POST /user/learning-assets/records/list
```

#### 1.4 更新候选资产

```http
PUT /user/learning-assets/candidates/{id}
```

#### 1.5 批量确认候选资产

```http
POST /user/learning-assets/records/{id}/confirm
```

#### 1.6 发布个人资产

```http
POST /user/learning-assets/records/{id}/publish
```

说明：

- 闪卡与练习清单可直接发布
- 知识节点候选与面试题草稿则进入审核态

#### 1.7 重试失败记录

```http
POST /user/learning-assets/records/{id}/retry
```

### 2. 管理端接口

#### 2.1 候选资产列表

```http
POST /admin/learning-assets/candidates/list
```

#### 2.2 获取候选资产详情

```http
GET /admin/learning-assets/candidates/{id}
```

#### 2.3 审核通过并发布

```http
POST /admin/learning-assets/candidates/{id}/approve
```

#### 2.4 驳回候选资产

```http
POST /admin/learning-assets/candidates/{id}/reject
```

#### 2.5 合并到已有内容

```http
POST /admin/learning-assets/candidates/{id}/merge
```

#### 2.6 转化质量统计

```http
GET /admin/learning-assets/statistics
```

## 🧱 前端页面建议

### 用户端 `vue3-user-front`

- `src/views/learning-assets/MyAssets.vue`
  - 我的学习资产首页
- `src/views/learning-assets/RecordDetail.vue`
  - 转化记录详情页
- `src/components/learning-assets/TransformDialog.vue`
  - 详情页统一转化弹窗
- `src/components/learning-assets/CandidateEditor.vue`
  - 候选资产编辑器

建议新增入口挂载位置：

- `src/views/blog/ArticleDetail.vue`
- `src/views/community/PostDetail.vue`
- `src/views/codepen/Detail.vue`
- `src/views/mock-interview/Report.vue`

### 管理端 `vue3-admin-front`

- `src/views/learning-assets/review/index.vue`
  - 候选资产审核台
- `src/views/learning-assets/statistics/index.vue`
  - 转化质量看板

## 📊 成功指标

### 核心指标

- 内容详情页转化点击率
- 转化成功率
- 候选资产确认率
- 个人资产发布率
- 平台候选资产审核通过率
- 发布后 7 日内学习行为触发率

### 结果指标

- 由内容转化生成的闪卡学习次数
- 由面试报告转化生成的复习计划完成率
- 高质量博客/帖子/作品的二次学习利用率
- 学习驾驶舱中“由内容转化产生的学习动作占比”

## 🗓️ 版本规划

### v1.0.0 一期

- 内容详情页一键转化
- 我的学习资产页
- 4 类内容源接入
- 4 类资产类型输出
- 候选资产编辑、去重、确认、发布
- 管理端审核台

### v1.1.0 二期

- 学习资产工坊
- 批量转化
- 多来源合并转化
- 图谱关系建议
- 用户自定义转化模板

### v1.2.0 三期

- 团队共享学习资产
- 公共卡组推荐
- 高质量内容自动推荐转化
- 成长自动驾驶直接消费学习资产结果

## ✅ 验收标准

- 用户可在博客、社区帖子、CodePen、模拟面试报告详情页看到统一的转化入口
- 用户发起转化后，可生成至少 1 种候选学习资产并进入预览流程
- 用户可编辑、勾选、删除候选资产，并成功发布个人资产
- 知识节点候选和面试题草稿可进入管理端审核流程
- 已发布的学习资产能够追溯来源内容
- 转化失败时有明确错误提示，并支持重试
- 所有转化记录可在“我的学习资产”中统一查看

## ⚠️ 风险与应对

### 1. AI 结果质量不稳定

应对：

- 保留候选编辑态，不做强自动发布
- 引入置信度与默认勾选策略
- 对不同来源使用不同 Prompt 模板

### 2. 公共内容池被低质量资产污染

应对：

- 平台级资产必须审核
- 支持管理员合并、编辑、驳回
- 记录来源与转化链路，便于回溯

### 3. 与现有模块边界冲突

应对：

- 新建独立业务模块管理编排状态
- 既复用 `xiaou-ai`，又不把业务态写死在 AI 模块中
- 对平台资产采用“候选池”模式，避免直接入库冲突

### 4. 用户转化后不继续学习

应对：

- 发布成功后提供“立即学习/加入计划”快捷入口
- 在学习驾驶舱中展示“来自内容转化”的任务与卡组
- 配合通知系统做转化后提醒

## 🎯 总结

学习资产转化引擎不是一个孤立新模块，而是 Code Nest 从“内容平台”走向“内容驱动学习平台”的关键中台能力。

它的真正价值不在于多一个 AI 功能按钮，而在于把：

- 博客的深度
- 社区的经验
- CodePen 的实践
- 面试报告的反馈

统一转化为可以持续复习、持续训练、持续追踪的学习资产，最终反哺学习驾驶舱、成长闭环和平台知识体系，形成真正的长期复利。
