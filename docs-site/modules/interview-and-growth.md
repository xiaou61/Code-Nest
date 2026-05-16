# 题库与成长闭环

题库与成长闭环把面试题、OJ、模拟面试、求职计划、学习资产、闪卡、打卡和知识图谱串在一起，目标是让用户从内容消费走向可追踪的能力提升。

## 功能构成

| 功能 | 用户端入口 | 管理端入口 | 后端模块 |
| --- | --- | --- | --- |
| 面试题库 | `/interview` | `/interview/*` | `xiaou-interview` |
| AI 模拟面试 | `/mock-interview` | `/mock-interview/*` | `xiaou-mock-interview`、`xiaou-ai` |
| 求职作战台 | `/job-battle` | `/system/ai-config` | `xiaou-mock-interview`、`xiaou-ai` |
| 岗位匹配 | `/job-match-engine` | `/system/ai-governance` | `xiaou-mock-interview`、`xiaou-ai` |
| 求职闭环 | `/career-loop` | AI 治理相关页面 | `xiaou-mock-interview` |
| 学习驾驶舱 | `/learning-cockpit` | AI 治理相关页面 | `xiaou-application`、`xiaou-ai` |
| 学习资产 | `/learning-assets` | `/learning-assets/*` | `xiaou-learning-asset` |
| 闪卡 | `/flashcard` | 后续扩展 | `xiaou-flashcard` |
| 计划打卡 | `/plan` | 后续扩展 | `xiaou-plan` |
| 学习小组 | `/team` | 后续扩展 | `xiaou-team` |
| 知识图谱 | `/knowledge` | `/knowledge/maps` | `xiaou-knowledge` |

## 面试题库

面试题库提供题目分类、题单、题目详情、答案展示、收藏、随机刷题和复习能力。管理端负责分类、题单和题目维护。

后续文档需要补齐：

- 题目分类与题单模型。
- 收藏、掌握度和复习记录。
- 用户刷题路径。
- 管理端题目维护流程。

## 学习资产转化

学习资产转化引擎负责把博客、社区帖子、CodePen、模拟面试报告等内容转成可审核、可发布的学习资产，例如闪卡、练习清单、题目草稿和知识节点候选。

核心价值是把平台内容沉淀为二次学习材料，减少“看完就丢”的损耗。

## 成长驾驶舱

成长驾驶舱聚合 OJ、题库、闪卡、计划、积分、排名和求职目标，提供成长分、能力雷达、短板诊断、今日任务和 AI 复盘。

这部分是 v2.1.0 之后的重点体验，需要持续补齐指标口径和任务生成规则。

