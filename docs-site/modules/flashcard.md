# 闪卡

闪卡模块用于碎片知识复习，和学习资产转化、题库复习、知识图谱存在联动空间。

## 功能入口

| 端 | 页面 |
| --- | --- |
| 用户端 | `/flashcard`、`/flashcard/deck/:id`、`/flashcard/study`、`/flashcard/study/:deckId`、`/flashcard/my`、`/flashcard/deck/create`、`/flashcard/deck/:id/edit`、`/flashcard/deck/:deckId/cards` |
| 后端模块 | `xiaou-flashcard` |

## 接口域

| 接口域 | 能力 |
| --- | --- |
| `/pub/flashcard/deck` | 公开卡组列表、卡组详情、公开卡片 |
| `/flashcard/deck` | 创建、更新、删除、我的卡组、Fork |
| `/flashcard/card` | 新增、批量新增、更新、删除、按卡组查询、导入 |
| `/flashcard/study` | 今日学习、下一张卡、提交学习结果、统计、热力图 |

## 核心流程

1. 用户创建卡组。
2. 用户添加或导入卡片。
3. 卡组可以公开给其他用户查看或 Fork。
4. 用户进入学习模式，系统给出下一张卡片。
5. 用户提交掌握结果，系统更新学习统计。
6. 学习结果进入热力图和成长数据。

## 文档深化点

- 卡组公开状态。
- Fork 的归属和收益规则。
- 学习算法和下一张卡选择规则。
- 卡片导入格式。
- 与学习驾驶舱的复习任务联动。

