# 成长能力图谱

成长能力图谱把 Growth Coach 已经采集并落库的成长证据，整理成用户可以理解和复核的能力结构。它是一个只读投影：用于解释当前状态和补强方向，不负责生成任务，也不修改既有计划。

## 能力边界

| 能力节点 | 主要证据 | 解释 |
| --- | --- | --- |
| 问题解决 | OJ 判题、题目掌握、SQL 优化 | 反映分析、定位和解决技术问题的证据 |
| 面试表达 | 模拟面试、题目掌握 | 反映表达、理解和回答问题的证据 |
| 项目交付 | 公开作品、代码审查、SQL 优化 | 反映可展示、可评审的交付结果 |
| 持续执行 | 计划任务、求职阶段推进 | 反映连续行动和完成闭环的能力 |
| 求职准备 | 求职阶段、投递状态、模拟面试 | 反映当前求职准备度和阶段进展 |

每个节点返回：

- `score`：基于证据的 0–100 分，不对没有证据的节点强行估分。
- `confidence`：已验证证据、主动记录和证据数量共同形成的可信度。
- `trend`：比较最近两个 14 天窗口，返回上升、形成、放缓或稳定。
- `evidenceRefs`：最多四条最近证据引用，支持从能力结论回溯到原始动作。

## API

```http
GET /api/user/growth-coach/capability-graph
Authorization: Bearer <token>
```

响应包含 `nodes`、`edges` 和 `gaps`：

| 字段 | 说明 |
| --- | --- |
| `overallScore` | 只对有证据的能力节点求平均 |
| `evidenceCount` / `verifiedEvidenceCount` | 本次投影读取到的证据数量 |
| `nodes` | 能力节点、分数、趋势、可信度和证据引用 |
| `edges` | 确定性能力关系，例如持续执行支撑项目交付 |
| `gaps` | 复用既有短板洞察的补强项，不生成新的模型事实 |

## 用户端入口

- 页面：`/growth-capabilities`
- 成长自动驾驶：点击“查看能力图谱”
- 学习导航和命令菜单：选择“能力图谱”

页面明确展示证据数量、已验证证据、节点趋势和补强入口；没有足够证据时会显示空状态，而不是用模型建议填充能力事实。

## 实现与验证

后端实现位于 `xiaou-application`：

- `GrowthCapabilityGraphService`：读取一次 `GrowthEvidenceQueryService` 的有界快照，再交给 `GrowthSkillInsightService` 复用，执行有界、只读的确定性计算。
- `GrowthCapabilityGraphResponse`：稳定的用户端响应契约。
- `GrowthCapabilityGraphServiceTest`：覆盖无证据降级、节点计算、关系和短板转换。

该功能不新增数据库表、迁移脚本或环境变量；后续若需要能力分数历史，应另行设计带版本和证据快照的持久化模型。
