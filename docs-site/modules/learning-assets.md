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

## 资产类型

后续可以持续沉淀：

- 闪卡。
- 练习清单。
- 面试题草稿。
- 知识图谱节点候选。
- 学习计划任务。

## 开发注意

- 转化记录和候选资产要保留来源 ID，避免资产失去上下文。
- 用户确认和管理端审核是两个不同阶段。
- AI 生成内容需要人工可编辑。
- 发布后应和目标模块建立映射关系。

