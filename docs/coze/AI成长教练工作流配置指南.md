# AI成长教练 Coze工作流配置指南

本文档用于指导 `AI成长教练` 功能所需的 Coze 工作流搭建、输出格式约束与项目落地方式。

## 概述

AI成长教练建议先配置 3 个核心工作流，覆盖一期的完整链路：

| 工作流 | 建议枚举名 | 用途 |
|---|---|---|
| 诊断快照生成 | `GROWTH_COACH_SNAPSHOT` | 根据学习/求职聚合数据生成结构化诊断快照 |
| 行动重排规划 | `GROWTH_COACH_ACTION_REPLAN` | 根据时间预算、目标优先级和当前风险重新生成行动清单 |
| 追问式教练对话 | `GROWTH_COACH_CHAT` | 基于当前诊断快照回答用户追问，并输出引用依据与后续建议 |

## 配置位置

工作流 ID 后续建议回填到：

`xiaou-common/src/main/java/com/xiaou/common/enums/CozeWorkflowEnum.java`

与当前项目一致，Coze 工作流输出请保持可被后端解析的 JSON，支持以下包装形式：

```json
{
  "output": "{...真正的JSON字符串...}"
}
```

也支持直接输出严格 JSON 对象，但不建议输出 Markdown、代码块或解释性前缀。

---

## 工作流1：诊断快照生成（GROWTH_COACH_SNAPSHOT）

### 功能说明

将平台内多个模块聚合后的学习与求职数据，统一生成一个结构化的 AI 教练诊断快照，供：

- 学习驾驶舱展示完整教练面板
- 求职闭环展示求职摘要
- 对话追问继承上下文

### 输入参数

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|
| scene | String | 是 | 场景范围：`learning` / `career` / `hybrid` |
| targetRole | String | 否 | 用户目标岗位 |
| weeklyHours | Integer | 否 | 每周可投入时长 |
| learningStatsJson | String | 是 | 学习侧聚合 JSON |
| careerStatsJson | String | 是 | 求职侧聚合 JSON |
| sourceDigestJson | String | 否 | 关键来源摘要，如面试报告、岗位匹配摘要、学习资产摘要 |

### 输入 JSON 示例

`learningStatsJson` 示例：

```json
{
  "interviewSolvedCount": 38,
  "wrongCount": 11,
  "masteryWeakTopics": ["Redis", "系统设计", "并发集合"],
  "flashcardCompletionRate": 62,
  "planCompletionRate": 54,
  "learningAssetPublishedCount": 8,
  "recentActiveDays": 5
}
```

`careerStatsJson` 示例：

```json
{
  "jobMatchBestScore": 72,
  "jobMatchTopGaps": [
    { "skill": "Redis", "priority": "P0" },
    { "skill": "系统设计", "priority": "P1" }
  ],
  "careerLoopStage": "PLAN_EXECUTING",
  "careerLoopHealthScore": 68,
  "mockInterviewScore": 64,
  "mockWeakPoints": ["项目表达", "系统设计"]
}
```

### 输出格式（严格 JSON）

```json
{
  "scene": "hybrid",
  "headline": "本周先补 Redis 与系统设计，优先提升岗位匹配和模拟面试稳定性",
  "scores": {
    "learningScore": 66,
    "careerScore": 61,
    "executionScore": 58,
    "overallScore": 62
  },
  "riskLevel": "medium",
  "learningWeakPoints": [
    {
      "name": "Redis",
      "impact": "dual",
      "reason": "错题率较高且岗位匹配缺失关键词",
      "priority": "P0"
    }
  ],
  "careerRisks": [
    "岗位匹配分仍未达到80+",
    "模拟面试中的系统设计表达不稳定"
  ],
  "topActions": [
    {
      "title": "完成 Redis 高频题专项训练",
      "priority": "P0",
      "route": "/interview",
      "reason": "同时影响刷题掌握度与岗位匹配分",
      "expectedGain": "提升学习稳定性与岗位匹配表现",
      "estimatedMinutes": 90
    },
    {
      "title": "进行一次系统设计模拟面试",
      "priority": "P1",
      "route": "/mock-interview",
      "reason": "复盘显示项目表达和架构拆解偏弱",
      "expectedGain": "提升真实面试回答结构",
      "estimatedMinutes": 60
    }
  ],
  "recommendedResources": [
    {
      "type": "blog",
      "title": "Redis缓存击穿实战",
      "route": "/blog/1/article/12",
      "why": "与当前 P0 薄弱点高度相关"
    }
  ],
  "suggestedQuestions": [
    "为什么本周优先补 Redis？",
    "如果我只有 4 小时怎么重排计划？",
    "我现在应该先刷题还是先做模拟面试？"
  ]
}
```

### Coze提示词模板（可直接用）

```text
你是一位面向开发者成长与求职辅导的 AI 成长教练。

你的任务是根据学习数据、求职数据和关键来源摘要，生成结构化诊断快照。

输入：
- scene: {{scene}}
- targetRole: {{targetRole}}
- weeklyHours: {{weeklyHours}}
- learningStatsJson: {{learningStatsJson}}
- careerStatsJson: {{careerStatsJson}}
- sourceDigestJson: {{sourceDigestJson}}

要求：
1. 统一输出学习与求职双维度诊断。
2. 必须生成 scores、headline、riskLevel、learningWeakPoints、careerRisks、topActions、recommendedResources、suggestedQuestions。
3. 动作建议必须可执行，且优先级只能是 P0/P1/P2。
4. 推荐资源优先引用平台内已有内容形态：blog/community/codepen/knowledge/learningAsset。
5. suggestedQuestions 要适合作为“继续追问”的快捷问题。
6. 只输出严格 JSON，不要输出 Markdown，不要输出额外解释。
```

---

## 工作流2：行动重排规划（GROWTH_COACH_ACTION_REPLAN）

### 功能说明

当用户继续追问“如果我这周只有 3 小时怎么办”“如果我只想优先冲面试怎么办”时，系统需要对当前行动清单做二次重排。  
这个工作流负责把现有快照中的动作清单，按新的限制条件重排并压缩。

### 输入参数

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|
| snapshotJson | String | 是 | 当前诊断快照 JSON |
| currentActionsJson | String | 是 | 当前行动清单 JSON |
| weeklyHours | Integer | 否 | 重新限定后的每周时长 |
| focusMode | String | 否 | `learning_first` / `career_first` / `balanced` |
| specialConstraint | String | 否 | 用户附加限制，如“3天后要面试” |

### 输出格式（严格 JSON）

```json
{
  "strategyHeadline": "在时间受限下，优先保留 1 个学习动作和 1 个求职动作",
  "replannedActions": [
    {
      "title": "完成 Redis 高频题 10 题",
      "priority": "P0",
      "route": "/interview",
      "estimatedMinutes": 80,
      "reason": "在有限时间内收益最高"
    },
    {
      "title": "完成 1 次系统设计模拟面试",
      "priority": "P1",
      "route": "/mock-interview",
      "estimatedMinutes": 60,
      "reason": "可快速验证表达与拆解能力"
    }
  ],
  "droppedActions": [
    "延后本周知识图谱整理",
    "延后低优先级博客阅读"
  ],
  "executionAdvice": [
    "先做题后做模拟面试",
    "每个动作完成后立即记录复盘"
  ]
}
```

### Coze提示词模板（可直接用）

```text
你是 AI 成长教练的行动规划器。

输入：
- snapshotJson: {{snapshotJson}}
- currentActionsJson: {{currentActionsJson}}
- weeklyHours: {{weeklyHours}}
- focusMode: {{focusMode}}
- specialConstraint: {{specialConstraint}}

要求：
1. 根据当前限制条件重排行动清单。
2. 优先保留高收益动作，删除或延后低收益动作。
3. 输出 strategyHeadline、replannedActions、droppedActions、executionAdvice。
4. replannedActions 必须包含 title、priority、route、estimatedMinutes、reason。
5. 只输出严格 JSON。
```

---

## 工作流3：追问式教练对话（GROWTH_COACH_CHAT）

### 功能说明

用于回答用户围绕当前诊断快照的解释型追问。  
它不是开放聊天，而是必须基于：

- 当前快照
- 当前行动清单
- 来源摘要
- 用户提问

生成一条可解释、可承接的回答。

### 输入参数

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|
| scene | String | 是 | 当前对话场景：`learning` / `career` / `hybrid` |
| snapshotJson | String | 是 | 当前诊断快照 |
| actionsJson | String | 是 | 当前行动清单 |
| sourceDigestJson | String | 否 | 来源摘要 |
| userQuestion | String | 是 | 用户问题 |

### 输出格式（严格 JSON）

```json
{
  "answer": "当前诊断优先推荐 Redis，是因为它同时出现在错题高频主题、岗位匹配缺失关键词和模拟面试弱项中，属于双影响短板。",
  "references": [
    "学习诊断：Redis 错题率较高",
    "岗位匹配：Redis 为目标岗位核心关键词",
    "模拟面试：系统设计与缓存设计回答不稳定"
  ],
  "suggestedFollowUps": [
    "如果我优先冲面试，这个顺序会变吗？",
    "我应该先刷题还是先看博客？",
    "如果我这周只有 3 小时怎么办？"
  ],
  "recommendedAction": {
    "title": "完成 Redis 高频题专项训练",
    "route": "/interview"
  }
}
```

### Coze提示词模板（可直接用）

```text
你是一个解释型 AI 成长教练，只能围绕当前成长诊断快照回答问题。

输入：
- scene: {{scene}}
- snapshotJson: {{snapshotJson}}
- actionsJson: {{actionsJson}}
- sourceDigestJson: {{sourceDigestJson}}
- userQuestion: {{userQuestion}}

要求：
1. 回答必须基于当前快照和来源摘要，不要泛泛而谈。
2. 必须说明推荐背后的依据。
3. 输出字段必须包含：answer、references、suggestedFollowUps、recommendedAction。
4. 若问题超出成长/求职范围，请温和拉回当前诊断上下文。
5. 只输出严格 JSON，不要输出 Markdown，不要输出额外解释。
```

---

## 后端接入建议

### 推荐落点

- 模块：`xiaou-ai`
- 适配方式：
  - 先由后端规则层聚合多源数据
  - 再把聚合结果发送给 Coze 工作流
  - 最后对结构化 JSON 做解析、持久化和回显

### 推荐调用顺序

#### 快照生成

```text
聚合学习/求职数据
→ 调用 GROWTH_COACH_SNAPSHOT
→ 解析结构化输出
→ 持久化 snapshot + action
→ 返回给前端
```

#### 行动重排

```text
读取当前 snapshot + action
→ 用户提出时间预算或目标偏好
→ 调用 GROWTH_COACH_ACTION_REPLAN
→ 回写新的行动建议（可选）
→ 返回前端展示
```

#### 对话追问

```text
读取 snapshot + action + sourceDigest
→ 调用 GROWTH_COACH_CHAT
→ 解析 answer / references / followUps
→ 存储消息记录
→ 返回前端
```

---

## 枚举扩展建议

建议后续在 `CozeWorkflowEnum` 中追加以下枚举：

```java
GROWTH_COACH_SNAPSHOT("待配置", "AI成长教练诊断快照", "根据学习与求职聚合数据生成结构化诊断快照"),
GROWTH_COACH_ACTION_REPLAN("待配置", "AI成长教练行动重排", "根据时间预算和目标变化重排行动清单"),
GROWTH_COACH_CHAT("待配置", "AI成长教练追问对话", "围绕当前诊断快照回答用户追问");
```

## 调试建议

### 1. 必须保证输出可解析

建议所有工作流都只输出严格 JSON。  
如果 Coze 外层必须包裹 `output` 字段，则由后端统一解析：

```json
{
  "output": "{\"answer\":\"...\"}"
}
```

### 2. 控制回答风格

AI成长教练要避免：

- 套话式建议
- 过于宽泛的鸡汤话术
- 和当前快照无关的泛聊天

建议提示词中明确强调：

- 必须引用当前快照
- 必须给出依据
- 必须输出可执行动作

### 3. 推荐加入兜底逻辑

即使 Coze 工作流失败，后端也建议保留：

- 规则化 fallback 快照
- 模板化对话回答

这样可以避免页面完全空白。

## 与现有项目的关系

### 依赖的数据来源

- `xiaou-interview`
- `xiaou-flashcard`
- `xiaou-plan`
- `xiaou-learning-asset`
- `xiaou-mock-interview`
- `career-loop`
- `job-match-engine`

### 前端承载页面

- `/learning-cockpit`
- `/career-loop`

### 文档联动

- PRD：`docs/PRD/AI成长教练模块PRD-v1.0.0.md`
- 实现计划：`docs/plans/2026-03-14-ai-growth-coach-design.md`

## 总结

AI成长教练一期最推荐的 Coze 接入方式是：

- **一个快照工作流**
- **一个行动重排工作流**
- **一个追问式对话工作流**

这样既能覆盖结构化诊断、策略调整和可解释追问三大核心能力，又不会在一期把工作流体系做得过重。
