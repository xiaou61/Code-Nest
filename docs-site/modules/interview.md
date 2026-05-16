# 面试题库

面试题库是 Code Nest 的基础学习模块，提供分类、题单、题目、答案、收藏、掌握度、学习记录、随机刷题和复习能力。

## 功能入口

| 端 | 页面 |
| --- | --- |
| 用户端 | `/interview`、`/interview/random`、`/interview/question-sets/:id`、`/interview/questions/:setId/:questionId`、`/interview/favorites`、`/interview/review` |
| 管理端 | `/interview/categories`、`/interview/question-sets`、`/interview/questions` |
| 后端模块 | `xiaou-interview` |

## 用户侧接口域

| 接口域 | 能力 |
| --- | --- |
| `/interview/categories` | 分类列表和详情 |
| `/interview/question-sets` | 题单列表、题单详情、题单下题目、上下题、搜索、随机题 |
| `/interview/favorites` | 收藏、取消收藏、收藏状态、我的收藏 |
| `/interview/mastery` | 掌握度标记、批量查询、复习统计、复习列表、热力图 |
| `/interview/learn` | 学习记录、题单进度、学习总量 |

## 管理侧接口域

| 接口域 | 能力 |
| --- | --- |
| `/admin/interview/categories` | 分类增删改查 |
| `/admin/interview/question-sets` | 题单增删改查、分页、导入、浏览量 |
| `/admin/interview/questions` | 题目增删改查、题单内题目、搜索、批量删除 |

## 核心流程

1. 管理员维护分类。
2. 管理员创建题单并导入或维护题目。
3. 用户进入题库首页选择题单。
4. 用户刷题、切换上下题、标记掌握度。
5. 用户收藏重点题目。
6. 学习记录沉淀为进度、复习列表和热力图。

## 文档深化点

- 题目难度、分类和题单关系。
- 掌握度枚举和复习推荐规则。
- 收藏状态和学习记录的用户隔离。
- 导入题单的数据格式。
- 与学习驾驶舱的指标映射。

